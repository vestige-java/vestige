#import <Cocoa/Cocoa.h>

@interface Vestige : NSApplication {
    NSWindow *win;
    NSMenu *statusMenu;
    NSMenuItem *openWebAdminItem;
    NSMenuItem *openBaseFolderItem;
    NSMenuItem *startsAtLoginItem;
    NSStatusItem * statusItem;
	NSTask *task;
    NSFileHandle * fileHandle;
    int procState;
    NSString * url;
    NSString * base;
    NSTextView * textView;
    bool consoleWinShown;
    LSSharedFileListRef loginItemsListRef;
    bool atLoginStarted;
}

- (void)stream:(NSStream *)stream handleEvent:(NSStreamEvent)streamEvent;

- (void)processQuit;

- (void)openWebAdmin;

- (void)openBaseFolder;

- (void)showCommandLineOutput;

- (void)quitVestige;

- (void)toggleLoginStart;

@end
