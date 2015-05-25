#import <Cocoa/Cocoa.h>

@interface Vestige : NSApplication {
    NSWindow *win;
    NSMenu *statusMenu;
    NSMenuItem *openWebAdminItem;
    NSMenuItem *openBaseFolderItem;
    NSStatusItem * statusItem;
	NSTask *task;
    NSFileHandle * fileHandle;
    int procState;
    NSString * url;
    NSString * base;
    NSTextView * textView;
    bool consoleWinShown;
}

- (void)stream:(NSStream *)stream handleEvent:(NSStreamEvent)streamEvent;

- (void)processQuit;

- (void)openWebAdmin;

- (void)openBaseFolder;

- (void)showCommandLineOutput;

- (void)quitVestige;

@end
