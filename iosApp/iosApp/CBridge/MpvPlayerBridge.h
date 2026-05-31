#import <UIKit/UIKit.h>

NS_ASSUME_NONNULL_BEGIN

/**
 Thin ObjC-visible bridge between Kotlin/Native and the Swift MpvPlayerBridge class.
 Kotlin calls these @objc methods via cinterop; the Swift class implements them.
 */
NS_SWIFT_NAME(MpvPlayerBridge)
@interface MpvPlayerBridge : UIView

/// Load a URL with optional headers
- (void)loadURL:(NSString *)url headers:(nullable NSDictionary<NSString *, NSString *> *)headers;
/// Load a URL without headers
- (void)loadURLSimple:(NSString *)url;
- (void)play;
- (void)pause;
- (void)seekTo:(double)seconds;
- (void)setSpeed:(double)speed;
- (void)setVolume:(double)volume;
- (void)selectAudioTrackWithTrackIndex:(int)trackIndex;
- (void)selectSubtitleTrackWithTrackIndex:(int)trackIndex;
- (void)selectVideoTrackWithTrackIndex:(int)trackIndex;
- (void)setVideoScale:(int)scale;
- (void)setSubtitleFontSize:(int)fontSize;
- (void)setSubtitleColor:(int)colorArgb;
- (void)setSubtitleBold:(BOOL)bold;
- (void)setSubtitleShadow:(BOOL)hasShadow;
- (void)setLockScreenMeta:(NSString *)title artist:(NSString *)artist;
- (void)setSubtitleMargin:(int)bottomMargin;
- (void)enterPip;
- (void)destroy;

/// Called from Kotlin with the bridge as argument to set up the state callback
- (void)setFrameCallbackWithBlock:(void (^)(NSDictionary * _Nonnull))block;

@end

NS_ASSUME_NONNULL_END
