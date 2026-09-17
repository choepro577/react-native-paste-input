#import "PasteInputSetup.h"
#import "PasteInputModule.h"

@implementation PasteInputSetup

+ (void)setup:(id)rootViewFactory
{
    [PasteInputModule setup:(RCTRootViewFactory *)rootViewFactory];
}

@end
