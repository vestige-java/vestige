#import "VestigeAppDelegate.h"

@implementation VestigeAppDelegate

@synthesize window;

- (void)applicationDidFinishLaunching:(NSNotification *)aNotification {
	task = [[NSTask alloc] init];
	NSString* vestigeScript = [[NSBundle mainBundle] pathForResource:@"vestige" ofType:nil inDirectory:@"vestige_home"];
	[task setLaunchPath: vestigeScript];
	[task launch];
	[self performSelectorInBackground:@selector(waitVestigeTermination) withObject:nil];
}

- (IBAction)quitVestige:(id)pId {
	[task terminate];
}

- (void) applicationWillTerminate:(NSNotification *)notification {
	[task release];
}

- (void)waitVestigeTermination {
	[task waitUntilExit];
	[NSApp terminate:nil];
}


-(void)awakeFromNib{
	statusItem = [[[NSStatusBar systemStatusBar] statusItemWithLength:NSVariableStatusItemLength] retain];
	[statusItem setMenu:statusMenu];
	[statusItem setHighlightMode:YES];
	NSImage *statusItemImage = [NSImage imageNamed:@"vestige.png"];
	if (!statusItemImage) {
		[statusItem setTitle:@"Vestige"];
	} else {
		[statusItem setImage:statusItemImage];
	}
}

@end
