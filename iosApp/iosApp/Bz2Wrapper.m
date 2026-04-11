//
//  Bz2Wrapper.m
//  iosApp
//
//  Created by Денис Хамидуллин on 12.04.2026.
//

#import "Bz2Wrapper.h"
#import <bzlib.h>

@implementation Bz2Wrapper

+ (NSData * _Nullable)decompressBz2File:(NSString *)path {
    BZFILE *bzFile = BZ2_bzopen(path.UTF8String, "rb");
    if (!bzFile) return nil;
    
    NSMutableData *result = [NSMutableData data];
    char buf[65536];
    int bytesRead;
    
    while ((bytesRead = BZ2_bzread(bzFile, buf, sizeof(buf))) > 0) {
        [result appendBytes:buf length:bytesRead];
    }
    
    BZ2_bzclose(bzFile);
    return result;
}

@end
