//
//  SherpaOnnxBridge.h
//  iosApp
//
//  Created by Денис Хамидуллин on 06.04.2026.
//

#import <Foundation/Foundation.h>

NS_ASSUME_NONNULL_BEGIN

@interface SherpaOnnxBridge : NSObject

- (void)loadModelWithOnnxPath:(NSString *)onnxPath
                   tokensPath:(NSString *)tokensPath
               espeakDataPath:(NSString *)espeakDataPath;

- (void)speakWithText:(NSString *)text speed:(float)speed;

- (void)stop;

@end

NS_ASSUME_NONNULL_END
