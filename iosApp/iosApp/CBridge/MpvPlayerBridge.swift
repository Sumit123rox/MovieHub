import UIKit
import Metal
import MediaPlayer

/// Thin ObjC-visible bridge between Kotlin/Native and MPVKit's libmpv.
/// Uses event-driven property observation instead of polling — mpv calls
/// mpv_set_wakeup_callback when properties change, and drainEvents()
/// processes MPV_EVENT_PROPERTY_CHANGE to build a cached state dictionary.
/// The resulting NSDictionary is dispatched to the main queue for Kotlin.
@objc(MpvPlayerBridge)
public class MpvPlayerBridge: UIView {

    private var mpv: OpaquePointer?
    private let metalLayer = CAMetalLayer()
    private var isActive = false
    private var stateCallback: ((NSDictionary) -> Void)?
    /// Accumulates property values from MPV_EVENT_PROPERTY_CHANGE events.
    private var cachedState: [String: Any] = [:]
    /// Prevents flooding the main queue when rapid events arrive (e.g. time-pos at 60fps).
    private var callbackPending = false
    private var consecutiveStalls = 0
    private var wasPlayingBeforeBackground = false
    private var cachedAudioTracks: [[String: Any]] = []
    private var cachedSubtitleTracks: [[String: Any]] = []
    private var cachedVideoTracks: [[String: Any]] = []
    private var cachedChapters: [[String: Any]] = []
    private var needsTrackRefresh = true
    private var pollingTimer: DispatchSourceTimer?

    /// Lock screen metadata
    private var lockScreenTitle: String = ""
    private var lockScreenArtist: String = ""

    /// All mpv API calls must happen on this queue (mpv is not thread-safe).
    /// Callbacks to Kotlin are dispatched back to the main queue.
    private let mpvQueue = DispatchQueue(label: "com.moviehub.mpv", qos: .userInteractive)

    // MARK: - Init

    public override init(frame: CGRect) {
        super.init(frame: frame)
        setupLayer()
    }

    required init?(coder: NSCoder) {
        super.init(coder: coder)
        setupLayer()
    }

    deinit {
        // Remove background/foreground observers (must be on main thread)
        removeLifecycleObservers()
        // mpv_terminate_destroy is documented as thread-safe — call synchronously
        // since the async [weak self] path in destroyMpv() would be nil during deinit.
        if let mpv = mpv {
            mpv_set_wakeup_callback(mpv, nil, nil)
            mpv_terminate_destroy(mpv)
            self.mpv = nil
        }
    }

    private func setupLayer() {
        metalLayer.device = MTLCreateSystemDefaultDevice()
        metalLayer.pixelFormat = .bgra8Unorm
        metalLayer.framebufferOnly = true
        metalLayer.contentsScale = UIScreen.main.scale
        layer.addSublayer(metalLayer)
    }

    public override func layoutSubviews() {
        super.layoutSubviews()
        metalLayer.frame = bounds
    }

    /// Disable touch on this view so touches pass through to Compose.
    public override func didMoveToSuperview() {
        super.didMoveToSuperview()
        isUserInteractionEnabled = false
    }

    // MARK: - mpv Setup (runs on mpvQueue)

    private func initMpv() {
        guard mpv == nil else { return }

        let handle = mpv_create()
        guard let handle = handle else { return }
        mpv = handle

        // Window ID: pass the Metal layer pointer
        let wid = unsafeBitCast(metalLayer, to: UnsafeMutableRawPointer.self)
        mpv_set_option_string(handle, "wid", "\(wid)")
        mpv_set_option_string(handle, "vo", "gpu-next")
        mpv_set_option_string(handle, "gpu-api", "vulkan")
        mpv_set_option_string(handle, "gpu-context", "moltenvk")
        mpv_set_option_string(handle, "hwdec", "videotoolbox")
        mpv_set_option_string(handle, "hwdec-codecs", "all")
        mpv_set_option_string(handle, "keep-open", "yes")
        mpv_set_option_string(handle, "audio-channels", "stereo")
        mpv_set_option_string(handle, "osc", "no")
        mpv_set_option_string(handle, "ytdl", "no")
        mpv_set_option_string(handle, "config", "no")
        mpv_set_option_string(handle, "load-scripts", "no")
        mpv_set_option_string(handle, "sub-auto", "all")
        mpv_set_option_string(handle, "sub-ass-override", "yes")  // Apply subtitle style overrides on top of ASS
        mpv_set_option_string(handle, "sub-use-margins", "yes")
        mpv_set_option_string(handle, "demuxer-max-bytes", "150M")
        mpv_set_option_string(handle, "demuxer-max-back-bytes", "50M")
        mpv_set_option_string(handle, "cache", "yes")
        mpv_set_option_string(handle, "cache-secs", "300")

        let initResult = mpv_initialize(handle)
        guard initResult >= 0 else {
            print("[MpvPlayerBridge] mpv_initialize failed: \(initResult)")
            mpv_terminate_destroy(handle)
            mpv = nil
            // Notify listeners of initialization failure
            DispatchQueue.main.async { [weak self] in
                guard let self else { return }
                let errState: NSDictionary = [
                    "isPlaying": false,
                    "isLoading": false,
                    "error": "MPV init failed: code \(initResult)",
                    "currentPositionMs": Int64(0),
                    "durationMs": Int64(0),
                    "bufferedPositionMs": Int64(0),
                    "playbackSpeed": Float(1.0),
                ]
                self.stateCallback?(errState)
            }
            return
        }

        // Observe all properties we need for the state NSDictionary.
        // Sequential reply_userdata IDs — only used for debugging.
        mpv_observe_property(handle,  1, "track-list",             MPV_FORMAT_NODE)
        mpv_observe_property(handle,  2, "chapter-list",           MPV_FORMAT_NODE)
        mpv_observe_property(handle,  3, "duration",               MPV_FORMAT_DOUBLE)
        mpv_observe_property(handle,  4, "pause",                  MPV_FORMAT_FLAG)
        mpv_observe_property(handle,  5, "time-pos",               MPV_FORMAT_DOUBLE)
        mpv_observe_property(handle,  6, "speed",                  MPV_FORMAT_DOUBLE)
        mpv_observe_property(handle,  7, "cache-buffering-state",  MPV_FORMAT_INT64)
        mpv_observe_property(handle,  8, "demuxer-cache-duration", MPV_FORMAT_DOUBLE)
        mpv_observe_property(handle,  9, "video-params/w",         MPV_FORMAT_INT64)
        mpv_observe_property(handle, 10, "video-params/h",         MPV_FORMAT_INT64)
        mpv_observe_property(handle, 11, "video-codec",            MPV_FORMAT_STRING)
        mpv_observe_property(handle, 12, "video-bitrate",          MPV_FORMAT_INT64)
        mpv_observe_property(handle, 13, "audio-codec",            MPV_FORMAT_STRING)
        mpv_observe_property(handle, 14, "audio-bitrate",          MPV_FORMAT_INT64)
        mpv_observe_property(handle, 15, "audio-params/samplerate",MPV_FORMAT_INT64)
        mpv_observe_property(handle, 16, "audio-params/channel-count", MPV_FORMAT_INT64)
        mpv_observe_property(handle, 17, "display-fps",            MPV_FORMAT_DOUBLE)
        mpv_observe_property(handle, 18, "frame-drop-count",       MPV_FORMAT_INT64)
        mpv_observe_property(handle, 19, "hwdec-current",          MPV_FORMAT_STRING)
        mpv_observe_property(handle, 20, "demuxer",                MPV_FORMAT_STRING)
        mpv_observe_property(handle, 21, "file-format",            MPV_FORMAT_STRING)
        mpv_observe_property(handle, 22, "video-params/primaries", MPV_FORMAT_STRING)

        // Register wakeup callback — mpv calls this from an internal thread
        // when events are available. Must NOT touch mpv directly.
        let ctx = Unmanaged.passUnretained(self).toOpaque()
        mpv_set_wakeup_callback(handle, Self.mpvWakeupCallback, ctx)

        // Setup lock screen / control center controls
        setupRemoteCommandCenter()

        // Start polling timer (1s) as fallback — ensures the Kotlin UI
        // always gets periodic state updates even when event-driven
        // dispatch is coalescing. This is especially important for
        // time-pos (seekbar timer) and isPlaying (play/pause icon).
        startPollingTimer()

        // Background/foreground lifecycle observers
        let nc = NotificationCenter.default
        nc.addObserver(self, selector: #selector(handleBackground),
                       name: UIApplication.didEnterBackgroundNotification, object: nil)
        nc.addObserver(self, selector: #selector(handleForeground),
                       name: UIApplication.willEnterForegroundNotification, object: nil)
    }

    @objc private func removeLifecycleObservers() {
        NotificationCenter.default.removeObserver(self,
            name: UIApplication.didEnterBackgroundNotification, object: nil)
        NotificationCenter.default.removeObserver(self,
            name: UIApplication.willEnterForegroundNotification, object: nil)
    }

    @objc private func handleBackground() {
        mpvQueue.async { [weak self] in
            guard let self, let mpv = self.mpv, self.isActive else { return }
            // Save whether we were playing so we can auto-resume on foreground
            let wasPlaying = !(self.cachedState["pause"] as? Bool ?? false)
            self.wasPlayingBeforeBackground = wasPlaying
            if wasPlaying {
                var pauseFlag: Int32 = 1
                mpv_set_property(mpv, "pause", MPV_FORMAT_FLAG, &pauseFlag)
            }
        }
    }

    @objc private func handleForeground() {
        mpvQueue.async { [weak self] in
            guard let self, let mpv = self.mpv, self.isActive else { return }
            if self.wasPlayingBeforeBackground {
                var pauseFlag: Int32 = 0
                mpv_set_property(mpv, "pause", MPV_FORMAT_FLAG, &pauseFlag)
                self.wasPlayingBeforeBackground = false
            }
        }
    }

    /// C-compatible wakeup callback. Called from an arbitrary mpv internal thread.
    /// Retains self across the async hop to prevent dealloc during drain.
    /// Starts a 1-second polling timer on mpvQueue.
    /// This is a FALLBACK — the primary state dispatch is event-driven via
    /// mpv_set_wakeup_callback. The polling timer guarantees state sync
    /// even when rapid property change coalescing delays events.
    private func startPollingTimer() {
        let timer = DispatchSource.makeTimerSource(queue: mpvQueue)
        timer.schedule(deadline: .now() + 1.0, repeating: 1.0, leeway: .milliseconds(100))
        timer.setEventHandler { [weak self] in
            guard let self = self, self.isActive else { return }
            if let mpv = self.mpv {
                // Read ALL state properties to ensure the snapshot is complete.
                // Without this, the timer would dispatch state with duration=0
                // before the event-driven path populates it, causing the Kotlin
                // side to save invalid WatchProgress(durationMs=0).
                let timePos = self.readDouble(mpv, name: "time-pos")
                let duration = self.readDouble(mpv, name: "duration")
                let pause = self.readFlag(mpv, name: "pause")
                let cacheBuffering = self.readInt64(mpv, name: "cache-buffering-state")
                let cacheDuration = self.readDouble(mpv, name: "demuxer-cache-duration")
                if timePos > 0 { self.cachedState["time-pos"] = timePos }
                if duration > 0 { self.cachedState["duration"] = duration }
                self.cachedState["pause"] = pause
                if cacheBuffering >= 0 { self.cachedState["cache-buffering-state"] = cacheBuffering }
                if cacheDuration > 0 { self.cachedState["demuxer-cache-duration"] = cacheDuration }
                // Only dispatch when we have valid duration (file has loaded)
                if duration > 0 {
                    self.dispatchStateIfNeeded()
                }
            }
        }
        timer.resume()
        pollingTimer = timer
    }

    private func stopPollingTimer() {
        pollingTimer?.cancel()
        pollingTimer = nil
    }

    private static let mpvWakeupCallback: @convention(c) (UnsafeMutableRawPointer?) -> Void = { ctx in
        guard let ctx = ctx else { return }
        let handle = Unmanaged<MpvPlayerBridge>.fromOpaque(ctx).retain()
        let bridge = handle.takeUnretainedValue()
        bridge.mpvQueue.async {
            bridge.drainEvents()
            handle.release()
        }
    }

    // MARK: - Public @objc API (queues work onto mpvQueue)

    @objc public func loadURLSimple(_ url: String) {
        loadURL(url, headers: nil)
    }

    @objc public func loadURL(_ url: String, headers: [String: String]?) {
        mpvQueue.async { [weak self] in
            guard let self else { return }
            self.initMpv()
            guard let mpv = self.mpv else { return }

            if let headers, !headers.isEmpty {
                let joined = headers.map { "\($0.key): \($0.value)" }.joined(separator: "\r\n")
                mpv_set_property_string(mpv, "http-header-fields", joined)
            } else {
                mpv_set_property_string(mpv, "http-header-fields", "")
            }

            // Clear all caches — FILE_LOADED will set isActive = true and re-populate
            self.cachedState = [:]
            self.cachedAudioTracks = []
            self.cachedSubtitleTracks = []
            self.cachedVideoTracks = []
            self.cachedChapters = []
            self.needsTrackRefresh = true
            self.isActive = false

            // Escape any quotes in the URL to prevent mpv_command_string parsing issues
            let escapedUrl = url.replacingOccurrences(of: "\"", with: "\\\"")
            mpv_command_string(mpv, "loadfile \"\(escapedUrl)\" replace")
        }
    }

    @objc public func play() {
        mpvQueue.async { [weak self] in
            guard let self, let mpv = self.mpv else { return }
            mpv_set_property_string(mpv, "pause", "no")
            self.drainEvents()  // immediately process resulting PROPERTY_CHANGE
        }
    }

    @objc public func pause() {
        mpvQueue.async { [weak self] in
            guard let self, let mpv = self.mpv else { return }
            mpv_set_property_string(mpv, "pause", "yes")
            self.drainEvents()
        }
    }

    @objc public func seekTo(_ positionMs: Int64) {
        mpvQueue.async { [weak self] in
            guard let self, let mpv = self.mpv else { return }
            let sec = Double(positionMs) / 1000.0
            mpv_command_string(mpv, "seek \(sec) absolute")
            self.drainEvents()
        }
    }

    @objc public func setSpeed(_ speed: Float) {
        mpvQueue.async { [weak self] in
            guard let self, let mpv = self.mpv else { return }
            var val = Double(speed)
            mpv_set_property(mpv, "speed", MPV_FORMAT_DOUBLE, &val)
        }
    }

    @objc public func setVolume(_ volume: Float) {
        mpvQueue.async { [weak self] in
            guard let self, let mpv = self.mpv else { return }
            var val = Double(max(0, min(100, volume * 100)))
            mpv_set_property(mpv, "volume", MPV_FORMAT_DOUBLE, &val)
        }
    }

    @objc public func selectAudioTrackWithTrackIndex(_ trackIndex: Int) {
        mpvQueue.async { [weak self] in
            guard let self, let mpv = self.mpv else { return }
            var val = Int64(trackIndex)
            mpv_set_property(mpv, "aid", MPV_FORMAT_INT64, &val)
        }
    }

    @objc public func selectSubtitleTrackWithTrackIndex(_ trackIndex: Int) {
        mpvQueue.async { [weak self] in
            guard let self, let mpv = self.mpv else { return }
            if trackIndex <= 0 {
                mpv_set_property_string(mpv, "sid", "no")
            } else {
                var val = Int64(trackIndex)
                mpv_set_property(mpv, "sid", MPV_FORMAT_INT64, &val)
            }
        }
    }

    @objc public func setVideoScale(_ scale: Int) {
        mpvQueue.async { [weak self] in
            guard let self, let mpv = self.mpv else { return }
            // Reset all scale-related props first
            mpv_set_property_string(mpv, "video-aspect-override", "no")
            mpv_set_property_string(mpv, "panscan", "0.0")
            mpv_set_property_string(mpv, "video-zoom", "0.0")
            mpv_set_property_string(mpv, "video-pan-x", "0.0")
            mpv_set_property_string(mpv, "video-pan-y", "0.0")

            switch scale {
            case 0: /* FIT — default, no override */ break
            case 1: mpv_set_property_string(mpv, "panscan", "1.0")        // FILL — crop to fill
            case 2: mpv_set_property_string(mpv, "video-zoom", "0.15")    // ZOOM — 15% zoom
            case 3: mpv_set_property_string(mpv, "video-aspect-override", "-1")  // STRETCH
            default: break
            }
        }
    }

    @objc public func setSubtitleColor(_ colorArgb: Int) {
        mpvQueue.async { [weak self] in
            guard let self, let mpv = self.mpv else { return }
            // Convert ARGB int to mpv hex color string: #AARRGGBB
            let a = (colorArgb >> 24) & 0xFF
            let r = (colorArgb >> 16) & 0xFF
            let g = (colorArgb >> 8) & 0xFF
            let b = colorArgb & 0xFF
            let colorStr = "#\(String(format: "%02X%02X%02X%02X", a, r, g, b))"
            mpv_set_property_string(mpv, "sub-color", colorStr)
        }
    }

    @objc public func setSubtitleBold(_ bold: Bool) {
        mpvQueue.async { [weak self] in
            guard let self, let mpv = self.mpv else { return }
            mpv_set_property_string(mpv, "sub-bold", bold ? "yes" : "no")
        }
    }

    @objc public func setSubtitleShadow(_ hasShadow: Bool) {
        mpvQueue.async { [weak self] in
            guard let self, let mpv = self.mpv else { return }
            mpv_set_property_string(mpv, "sub-shadow-offset", hasShadow ? "0.5" : "0")
        }
    }

    @objc public func setLockScreenMeta(_ title: String, artist: String) {
        lockScreenTitle = title
        lockScreenArtist = artist
    }

    private func updateNowPlaying(duration: Double, position: Double, rate: Float) {
        var info = [String: Any]()
        info[MPMediaItemPropertyTitle] = lockScreenTitle
        info[MPMediaItemPropertyArtist] = lockScreenArtist
        info[MPMediaItemPropertyPlaybackDuration] = duration.isFinite && duration > 0 ? duration : 0
        info[MPNowPlayingInfoPropertyElapsedPlaybackTime] = position.isFinite ? position : 0
        info[MPNowPlayingInfoPropertyPlaybackRate] = rate
        MPNowPlayingInfoCenter.default().nowPlayingInfo = info
    }

    private func setupRemoteCommandCenter() {
        let cmd = MPRemoteCommandCenter.shared()
        cmd.playCommand.addTarget { [weak self] _ in
            self?.play()
            return .success
        }
        cmd.pauseCommand.addTarget { [weak self] _ in
            self?.pause()
            return .success
        }
        cmd.togglePlayPauseCommand.addTarget { [weak self] _ in
            self?.play()  // toggle handled by Kotlin
            return .success
        }
        cmd.changePlaybackPositionCommand.addTarget { [weak self] event in
            guard let event = event as? MPChangePlaybackPositionCommandEvent else { return .commandFailed }
            self?.seekTo(Int64(event.positionTime * 1000))
            return .success
        }
    }

    @objc public func selectVideoTrackWithTrackIndex(_ trackIndex: Int) {
        mpvQueue.async { [weak self] in
            guard let self, let mpv = self.mpv else { return }
            var val = Int64(trackIndex)
            mpv_set_property(mpv, "vid", MPV_FORMAT_INT64, &val)
        }
    }

    @objc public func setSubtitleFontSize(_ fontSize: Int) {
        mpvQueue.async { [weak self] in
            guard let self, let mpv = self.mpv else { return }
            var val = Int64(fontSize)
            mpv_set_property(mpv, "sub-font-size", MPV_FORMAT_INT64, &val)
        }
    }

    @objc public func setSubtitleMargin(_ bottomMargin: Int) {
        mpvQueue.async { [weak self] in
            guard let self, let mpv = self.mpv else { return }
            var val = Int64(bottomMargin)
            mpv_set_property(mpv, "sub-margin-y", MPV_FORMAT_INT64, &val)
        }
    }

    @objc public func enterPip() {
        // PiP not supported with vulkan/Metal rendering
    }

    @objc public func destroy() {
        destroyMpv()
    }

    @objc public func setFrameCallbackWithBlock(_ block: @escaping (NSDictionary) -> Void) {
        stateCallback = block
    }

    // MARK: - Event-driven state (all on mpvQueue)

    private func drainEvents() {
        guard let mpv = mpv else { return }
        var didChange = false
        var iterations = 0

        while iterations < 500 {  // safety limit — accommodates bursty property change storms
            guard let event = mpv_wait_event(mpv, 0) else { break }
            iterations += 1

            switch event.pointee.event_id {
            case MPV_EVENT_SHUTDOWN:
                isActive = false

            case MPV_EVENT_END_FILE:
                if event.pointee.error != 0 {
                    isActive = false
                    if let cb = stateCallback {
                        let errState = buildErrorStateDict(code: event.pointee.error)
                        DispatchQueue.main.async { cb(errState) }
                    }
                }

            case MPV_EVENT_FILE_LOADED:
                isActive = true
                needsTrackRefresh = true
                populateInitialStateFromMpv()
                didChange = true

            case MPV_EVENT_PROPERTY_CHANGE:
                let prop = event.pointee.data.assumingMemoryBound(
                    to: mpv_event_property.self
                ).pointee
                processPropertyChange(prop)
                // track-list and chapter-list events trigger refresh eagerly
                if let name = prop.name {
                    let s = String(cString: name)
                    if s == "track-list" { refreshTrackCache() }
                    else if s == "chapter-list" { refreshChapterCache() }
                }
                didChange = true

            default:
                break
            }
        }

        if didChange {
            dispatchStateIfNeeded()
        }
    }

    private func processPropertyChange(_ prop: mpv_event_property) {
        let name = prop.name.map { String(cString: $0) } ?? ""
        guard !name.isEmpty, let data = prop.data else { return }

        switch prop.format {
        case MPV_FORMAT_FLAG:
            cachedState[name] = data.assumingMemoryBound(to: Int32.self).pointee != 0
        case MPV_FORMAT_INT64:
            cachedState[name] = data.assumingMemoryBound(to: Int64.self).pointee
        case MPV_FORMAT_DOUBLE:
            cachedState[name] = data.assumingMemoryBound(to: Double.self).pointee
        case MPV_FORMAT_STRING:
            let strPtr = data.assumingMemoryBound(to: UnsafePointer<CChar>?.self).pointee
            cachedState[name] = strPtr.map { String(cString: $0) } ?? ""
        default:
            break  // NODE handled via separate refreshTrackCache / refreshChapterCache
        }
    }

    /// Seed cachedState with initial values on FILE_LOADED.
    /// After this, all updates arrive via PROPERTY_CHANGE events.
    private func populateInitialStateFromMpv() {
        guard let mpv = mpv else { return }

        cachedState["pause"]                  = readFlag(mpv, name: "pause")
        cachedState["time-pos"]               = readDouble(mpv, name: "time-pos")
        cachedState["duration"]               = readDouble(mpv, name: "duration")
        cachedState["speed"]                  = readDouble(mpv, name: "speed")
        cachedState["cache-buffering-state"]  = readInt64(mpv, name: "cache-buffering-state")
        cachedState["demuxer-cache-duration"] = readDouble(mpv, name: "demuxer-cache-duration")
        cachedState["video-params/w"]         = readInt64(mpv, name: "video-params/w")
        cachedState["video-params/h"]         = readInt64(mpv, name: "video-params/h")
        cachedState["video-codec"]            = readString(mpv, name: "video-codec") ?? ""
        cachedState["video-bitrate"]          = readInt64(mpv, name: "video-bitrate")
        cachedState["audio-codec"]            = readString(mpv, name: "audio-codec") ?? ""
        cachedState["audio-bitrate"]          = readInt64(mpv, name: "audio-bitrate")
        cachedState["audio-params/samplerate"]   = readInt64(mpv, name: "audio-params/samplerate")
        cachedState["audio-params/channel-count"] = readInt64(mpv, name: "audio-params/channel-count")
        cachedState["display-fps"]            = readDouble(mpv, name: "display-fps")
        cachedState["frame-drop-count"]       = readInt64(mpv, name: "frame-drop-count")
        cachedState["hwdec-current"]          = readString(mpv, name: "hwdec-current") ?? ""
        cachedState["demuxer"]                = readString(mpv, name: "demuxer") ?? ""
        cachedState["file-format"]            = readString(mpv, name: "file-format") ?? ""
        cachedState["video-params/primaries"] = readString(mpv, name: "video-params/primaries") ?? ""
    }

    private func dispatchStateIfNeeded() {
        guard let callback = stateCallback, isActive else { return }
        guard !callbackPending else { return }  // coalesce rapid updates
        callbackPending = true

        let snapshot = buildStateDict()
        DispatchQueue.main.async { [weak self] in
            guard self?.stateCallback != nil else { return }
            callback(snapshot)
            // Update lock screen info on main thread
            if let dur = snapshot["durationMs"] as? Int64,
               let pos = snapshot["currentPositionMs"] as? Int64,
               let rate = snapshot["playbackSpeed"] as? Float {
                self?.updateNowPlaying(duration: Double(dur) / 1000.0,
                                       position: Double(pos) / 1000.0,
                                       rate: rate)
            }
            // Clear the flag back on mpvQueue to serialize access
            self?.mpvQueue.async {
                self?.callbackPending = false
            }
        }

        // Watchdog: if main queue is heavily contended, force-clear after 2s.
        // Track consecutive stalls to detect persistent main queue starvation.
        mpvQueue.asyncAfter(deadline: .now() + 2.0) { [weak self] in
            guard let self, self.callbackPending else { return }
            self.consecutiveStalls += 1
            self.callbackPending = false
        }
    }

    private func buildStateDict() -> NSDictionary {
        let pauseFlag = cachedState["pause"] as? Bool ?? false
        let timePos   = cachedState["time-pos"] as? Double ?? 0
        let duration  = cachedState["duration"] as? Double ?? 0
        let speed     = cachedState["speed"] as? Double ?? 1.0
        let cacheBuffering = cachedState["cache-buffering-state"] as? Int64 ?? 0
        let cacheDuration  = cachedState["demuxer-cache-duration"] as? Double ?? 0

        // Refresh track cache on first valid duration after file load
        if needsTrackRefresh, duration.isFinite, duration > 0, cachedAudioTracks.isEmpty {
            refreshTrackCache()
            refreshChapterCache()
            needsTrackRefresh = false
        }

        let primaries = cachedState["video-params/primaries"] as? String ?? ""
        let hdrMode: String = {
            switch primaries {
            case "bt.2020":    return "HDR10"
            case "smpte2084":  return "Dolby Vision"
            default:           return "SDR"
            }
        }()

        let isPlaying = isActive && !pauseFlag
        let isLoading = cacheBuffering > 0
        let posMs     = (timePos.isFinite ? timePos : 0) * 1000
        let durMs     = (duration.isFinite ? duration : 0) * 1000
        let bufMs     = posMs + (cacheDuration.isFinite ? cacheDuration : 0) * 1000

        return [
            "isPlaying":         isPlaying,
            "isLoading":         isLoading,
            "error":             "",
            "currentPositionMs": Int64(posMs),
            "durationMs":        Int64(durMs),
            "bufferedPositionMs": Int64(bufMs),
            "playbackSpeed":     Float(speed),
            "audioTracks":       cachedAudioTracks,
            "subtitleTracks":    cachedSubtitleTracks,
            "videoTracks":       cachedVideoTracks,
            "videoWidth":        Int(cachedState["video-params/w"] as? Int64 ?? 0),
            "videoHeight":       Int(cachedState["video-params/h"] as? Int64 ?? 0),
            "videoCodec":        cachedState["video-codec"] as? String ?? "",
            "videoBitrate":      Int(cachedState["video-bitrate"] as? Int64 ?? 0),
            "audioBitrate":      Int(cachedState["audio-bitrate"] as? Int64 ?? 0),
            "audioCodec":        cachedState["audio-codec"] as? String ?? "",
            "currentCueCount":   0,
            "chapters":          cachedChapters,
            "droppedFrameCount": Int(cachedState["frame-drop-count"] as? Int64 ?? 0),
            "decoderName":       cachedState["hwdec-current"] as? String ?? "",
            "hardwareDecoding": {
                let hwdec = cachedState["hwdec-current"] as? String ?? ""
                return !hwdec.isEmpty && hwdec != "no"
            }(),
            "hdrMode":           hdrMode,
            "demuxerName":       cachedState["demuxer"] as? String ?? "",
            "containerFormat":   cachedState["file-format"] as? String ?? "",
            "audioSampleRate":   Int(cachedState["audio-params/samplerate"] as? Int64 ?? 0),
            "audioChannels":     Int(cachedState["audio-params/channel-count"] as? Int64 ?? 0),
            "displayFps":        Float(cachedState["display-fps"] as? Double ?? 0),
        ]
    }

    private func buildErrorStateDict(code: Int32) -> NSDictionary {
        [
            "isPlaying": false, "isLoading": false,
            "error": "Playback ended with error \(code)",
            "currentPositionMs": Int64(0), "durationMs": Int64(0),
            "bufferedPositionMs": Int64(0), "playbackSpeed": Float(1.0),
            "audioTracks": [], "subtitleTracks": [], "videoTracks": [],
            "videoWidth": 0, "videoHeight": 0, "videoCodec": "",
            "videoBitrate": 0, "audioBitrate": 0, "audioCodec": "",
            "currentCueCount": 0, "chapters": [],
            "droppedFrameCount": 0, "decoderName": "",
            "hardwareDecoding": false, "hdrMode": "",
            "demuxerName": "", "containerFormat": "",
            "audioSampleRate": 0, "audioChannels": 0,
            "displayFps": Float(0),
        ]
    }

    // MARK: - Track / chapter cache (on mpvQueue)

    private func refreshTrackCache() {
        guard let mpv = mpv else { return }
        var node = mpv_node()
        guard mpv_get_property(mpv, "track-list", MPV_FORMAT_NODE, &node) >= 0 else { return }
        defer { mpv_free_node_contents(&node) }
        guard node.format == MPV_FORMAT_NODE_ARRAY, let list = node.u.list else { return }

        var audio: [[String: Any]] = []
        var subs: [[String: Any]] = []
        var video: [[String: Any]] = []

        for i in 0..<Int(list.pointee.num) {
            guard let track = nodeValue(at: i, in: list) else { continue }
            let type = stringFromNodeMap(track, key: "type") ?? ""
            switch type {
            case "audio":
                audio.append([
                    "id": stringFromNodeMap(track, key: "id") ?? "audio/\(i)",
                    "label": stringFromNodeMap(track, key: "title") ?? stringFromNodeMap(track, key: "lang") ?? "Track \(audio.count + 1)",
                    "language": stringFromNodeMap(track, key: "lang") ?? "",
                    "isSelected": boolFromNodeMap(track, key: "selected"),
                ])
            case "sub":
                subs.append([
                    "id": stringFromNodeMap(track, key: "id") ?? "sub/\(i)",
                    "label": stringFromNodeMap(track, key: "title") ?? stringFromNodeMap(track, key: "lang") ?? "Track \(subs.count + 1)",
                    "language": stringFromNodeMap(track, key: "lang") ?? "",
                    "isSelected": boolFromNodeMap(track, key: "selected"),
                    "isExternal": boolFromNodeMap(track, key: "external"),
                ])
            case "video":
                video.append([
                    "id": stringFromNodeMap(track, key: "id") ?? "video/\(i)",
                    "label": stringFromNodeMap(track, key: "title") ?? "Video \(video.count + 1)",
                    "width": intFromNodeMap(track, key: "demux-w"),
                    "height": intFromNodeMap(track, key: "demux-h"),
                    "codec": stringFromNodeMap(track, key: "codec") ?? "",
                    "bitrate": 0,
                    "isSelected": boolFromNodeMap(track, key: "selected"),
                ])
            default: break
            }
        }
        cachedAudioTracks = audio
        cachedSubtitleTracks = subs
        cachedVideoTracks = video
    }

    private func refreshChapterCache() {
        guard let mpv = mpv else { return }
        var node = mpv_node()
        guard mpv_get_property(mpv, "chapter-list", MPV_FORMAT_NODE, &node) >= 0 else { return }
        defer { mpv_free_node_contents(&node) }
        guard node.format == MPV_FORMAT_NODE_ARRAY, let list = node.u.list else { return }

        var chapters: [[String: Any]] = []
        for i in 0..<Int(list.pointee.num) {
            guard let ch = nodeValue(at: i, in: list) else { continue }
            let title = stringFromNodeMap(ch, key: "title") ?? "Chapter \(i)"
            let timeSec = doubleFromNodeMap(ch, key: "time")
            chapters.append(["title": title, "startMs": Int64(timeSec * 1000), "endMs": Int64(0)])
        }
        cachedChapters = chapters
    }

    // MARK: - mpv property helpers (call on mpvQueue only)

    private func readString(_ mpv: OpaquePointer, name: String) -> String? {
        guard let ptr = mpv_get_property_string(mpv, name) else { return nil }
        defer { mpv_free(ptr) }
        return String(cString: ptr)
    }

    private func readDouble(_ mpv: OpaquePointer, name: String) -> Double {
        var val: Double = 0
        guard mpv_get_property(mpv, name, MPV_FORMAT_DOUBLE, &val) >= 0 else { return .nan }
        return val
    }

    private func readInt64(_ mpv: OpaquePointer, name: String) -> Int64 {
        var val: Int64 = 0
        guard mpv_get_property(mpv, name, MPV_FORMAT_INT64, &val) >= 0 else { return 0 }
        return val
    }

    private func readFlag(_ mpv: OpaquePointer, name: String) -> Bool {
        var val: Int32 = 0
        guard mpv_get_property(mpv, name, MPV_FORMAT_FLAG, &val) >= 0 else { return false }
        return val != 0
    }

    // MARK: - mpv_node helpers

    private func nodeValue(at index: Int, in list: UnsafeMutablePointer<mpv_node_list>) -> UnsafeMutablePointer<mpv_node>? {
        guard index >= 0, index < Int(list.pointee.num) else { return nil }
        return list.pointee.values.advanced(by: index)
    }

    private func stringFromNodeMap(_ map: UnsafeMutablePointer<mpv_node>, key: String) -> String? {
        guard map.pointee.format == MPV_FORMAT_NODE_MAP, let list = map.pointee.u.list, let keys = list.pointee.keys else { return nil }
        for i in 0..<Int(list.pointee.num) {
            if let k = keys[i], String(cString: k) == key {
                let val = list.pointee.values.advanced(by: i)
                if val.pointee.format == MPV_FORMAT_STRING, let s = val.pointee.u.string { return String(cString: s) }
                return nil
            }
        }
        return nil
    }

    private func boolFromNodeMap(_ map: UnsafeMutablePointer<mpv_node>, key: String) -> Bool {
        guard map.pointee.format == MPV_FORMAT_NODE_MAP, let list = map.pointee.u.list, let keys = list.pointee.keys else { return false }
        for i in 0..<Int(list.pointee.num) {
            if let k = keys[i], String(cString: k) == key {
                let val = list.pointee.values.advanced(by: i)
                if val.pointee.format == MPV_FORMAT_FLAG { return val.pointee.u.flag != 0 }
                if val.pointee.format == MPV_FORMAT_INT64 { return val.pointee.u.int64 != 0 }
                return false
            }
        }
        return false
    }

    private func intFromNodeMap(_ map: UnsafeMutablePointer<mpv_node>, key: String) -> Int {
        guard map.pointee.format == MPV_FORMAT_NODE_MAP, let list = map.pointee.u.list, let keys = list.pointee.keys else { return 0 }
        for i in 0..<Int(list.pointee.num) {
            if let k = keys[i], String(cString: k) == key {
                let val = list.pointee.values.advanced(by: i)
                if val.pointee.format == MPV_FORMAT_INT64 { return Int(val.pointee.u.int64) }
                return 0
            }
        }
        return 0
    }

    private func doubleFromNodeMap(_ map: UnsafeMutablePointer<mpv_node>, key: String) -> Double {
        guard map.pointee.format == MPV_FORMAT_NODE_MAP, let list = map.pointee.u.list, let keys = list.pointee.keys else { return 0 }
        for i in 0..<Int(list.pointee.num) {
            if let k = keys[i], String(cString: k) == key {
                let val = list.pointee.values.advanced(by: i)
                if val.pointee.format == MPV_FORMAT_DOUBLE { return val.pointee.u.double_ }
                return 0
            }
        }
        return 0
    }

    // MARK: - Cleanup

    private func destroyMpv() {
        stopPollingTimer()
        stateCallback = nil
        guard let mpv = mpv else { return }
        mpv_set_wakeup_callback(mpv, nil, nil)
        isActive = false
        mpv_terminate_destroy(mpv)
        self.mpv = nil
    }
}
