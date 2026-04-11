//
//  Bz2Wrapper.h
//  iosApp
//
//  Created by Денис Хамидуллин on 12.04.2026.
//

#import <Foundation/Foundation.h>

NS_ASSUME_NONNULL_BEGIN

@interface Bz2Wrapper : NSObject
+ (NSData * _Nullable)decompressBz2File:(NSString *)path;
@end

NS_ASSUME_NONNULL_END
