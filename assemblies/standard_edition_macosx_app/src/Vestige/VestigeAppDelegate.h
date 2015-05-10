#import <Cocoa/Cocoa.h>

@interface VestigeAppDelegate : NSObject <NSApplicationDelegate> {
    NSWindow *window;
	IBOutlet NSMenu *statusMenu;
    NSStatusItem * statusItem;
	NSTask *task;
}

- (void)waitVestigeTermination;

- (IBAction)quitVestige:(id)pId;

@property (assign) IBOutlet NSWindow *window;

@end
