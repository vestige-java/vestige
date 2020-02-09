#import <Cocoa/Cocoa.h>

@interface Vestige : NSApplication {
    NSWindow *win;
    NSMenu *statusMenu;
    NSMenuItem *openWebAdminItem;
    NSMenuItem *openBaseFolderItem;
    NSMenuItem *startAtLoginItem;
    NSMenuItem *quitItem;
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
    bool forceStop;
    bool quit;
    uint8_t buffer[32767];
    int32_t bufferRemain;
    int32_t bufferSize;
    int bufferSizeBytes;
}

- (void)stream:(NSStream *)stream handleEvent:(NSStreamEvent)streamEvent;

- (void)processQuit;

- (void)openWebAdmin;

- (void)openBaseFolder;

- (void)showCommandLineOutput;

- (void)quitVestige;

- (void)stopVestige;

- (void)toggleLoginStart;

- (void)addCA:(NSString *)path;

- (void)addP12:(NSString *)path;

@end
