#import <Foundation/Foundation.h>
#import <UIKit/UIKit.h>

@interface MpvPlayerBridge : UIView

- (void)loadURLSimple:(NSString *)url;
- (void)loadURL:(NSString *)url headers:(NSDictionary<NSString *, NSString *> *)headers;
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
- (void)setFrameCallbackWithBlock:(void (^)(NSDictionary * _Nonnull))block;

@end
