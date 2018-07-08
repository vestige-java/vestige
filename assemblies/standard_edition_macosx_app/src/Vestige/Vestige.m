#import "Vestige.h"
#include <CoreFoundation/CoreFoundation.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <signal.h>

@implementation Vestige

static void handleConnect(CFSocketRef socket,
                                    CFSocketCallBackType type,
                                    CFDataRef address,
                                    const void *data,
                                    void *info) {
    Vestige * vapp = (Vestige *) info;
    if (kCFSocketAcceptCallBack == type) {
        CFSocketNativeHandle nativeSocketHandle = *(CFSocketNativeHandle *)data;
        
        CFReadStreamRef readStream = NULL;
        CFStreamCreatePairWithSocket(kCFAllocatorDefault, nativeSocketHandle, &readStream, NULL);
        
        if (readStream) {
            CFReadStreamSetProperty(readStream, kCFStreamPropertyShouldCloseNativeSocket, kCFBooleanTrue);
            NSInputStream * istream = (NSInputStream *)readStream;
            
            [istream setDelegate:(id) vapp];
            [istream scheduleInRunLoop:[NSRunLoop currentRunLoop] forMode:(id) kCFRunLoopCommonModes];
            [istream open];
        } else {
            close(nativeSocketHandle);
        }
        
        if (readStream)
            CFRelease(readStream);
        
    }
}

- (void)stream:(NSStream *)stream handleEvent:(NSStreamEvent)streamEvent {
    
    NSInputStream * istream = (NSInputStream *) stream;
    switch(streamEvent) {
        case NSStreamEventEndEncountered:;
        case NSStreamEventHasBytesAvailable:;
            if (bufferSizeBytes != 4) {
                bufferSizeBytes += [istream read:((uint8_t *)&bufferSize)+bufferSizeBytes maxLength:4 - bufferSizeBytes];
                if (bufferSizeBytes != 4) {
                    return;
                }
                bufferSize = ntohl(bufferSize);
                bufferRemain = bufferSize;
                return;
            }
            
            bufferRemain -= [istream read:buffer + (bufferSize - bufferRemain) maxLength:bufferRemain];
            if (bufferRemain != 0) {
                return;
            }
            bufferSizeBytes = 0;
            
            NSString * command = [[NSString alloc] initWithBytesNoCopy:buffer length:bufferSize encoding:NSUTF8StringEncoding freeWhenDone:false];

            int webLen = strlen("Web ");
            int baseLen = strlen("Base ");
            if ([command hasPrefix:@"Web "]) {
                url = [[NSString alloc] initWithString:[command substringFromIndex:webLen]];
                [openWebAdminItem setEnabled:TRUE];
                [statusMenu update];
            } else if ([command hasPrefix:@"Base "]) {
                base = [[NSString alloc] initWithString:[command substringFromIndex:baseLen]];
                [openBaseFolderItem setEnabled:TRUE];
                [statusMenu update];
            } else if ([command compare:@"Starting"] == 0) {
                procState = 1;
            } else if ([command compare:@"Started"] == 0) {
                procState = 2;
            } else if ([command compare:@"Stopping"] == 0) {
                procState = 3;
            } else if ([command compare:@"Stopped"] == 0) {
                procState = 4;
            }
            
            break;
        default:
            break;
    }
}


- (void)dataAvailable:(NSNotification *)aNotification {
    NSData* data = [[aNotification userInfo] objectForKey:NSFileHandleNotificationDataItem];
    if ([data length] == 0) {
        return;
    }
    
    NSTextStorage *buffer_text = [textView textStorage];
    NSString *s = [[NSString alloc] initWithData: data encoding:NSUTF8StringEncoding];
    NSAttributedString *str = [[NSAttributedString alloc] initWithString: s];
    
    [buffer_text beginEditing];
    [buffer_text appendAttributedString: str];
    [buffer_text endEditing];
    
    [fileHandle readInBackgroundAndNotify];
}


- (void)openWebAdmin {
    [[NSWorkspace sharedWorkspace] openURL:[NSURL URLWithString:url]];
}

- (void)openBaseFolder {
    [[NSWorkspace sharedWorkspace] openFile:base];
}

- (void)showCommandLineOutput {
    consoleWinShown = TRUE;
    [win makeKeyAndOrderFront:nil];
    [[NSApplication sharedApplication] activateIgnoringOtherApps:TRUE];
    [win orderFrontRegardless];
}

- (void)toggleLoginStart {
    NSURL *bundleURL = [NSURL fileURLWithPath:[[NSBundle mainBundle] bundlePath]];
    
    if (atLoginStarted) {
        CFArrayRef loginItemsRef = LSSharedFileListCopySnapshot(loginItemsListRef, NULL);
        NSArray *loginItems = CFBridgingRelease(loginItemsRef);
        for (id item in loginItems) {
            LSSharedFileListItemRef itemRef = (LSSharedFileListItemRef)item;
            CFURLRef itemURLRef;
            
            if (LSSharedFileListItemResolve(itemRef, 0, &itemURLRef, NULL) == noErr) {
                NSURL *itemURL = (NSURL *)[NSMakeCollectable(itemURLRef) autorelease];
                if ([itemURL isEqual:bundleURL]) {
                    LSSharedFileListItemRemove(loginItemsListRef, itemRef);
                    break;
                }
            }
        }
    } else {
        LSSharedFileListItemRef itemRef = LSSharedFileListInsertItemURL(loginItemsListRef,
                                                kLSSharedFileListItemLast,
                                                NULL,
                                                NULL,
                                                (CFURLRef)bundleURL,
                                                NULL,
                                                NULL);
        if (itemRef) {
            CFRelease(itemRef);
        }
    }
}

static void loginItemsChanged(LSSharedFileListRef listRef, void *context)
{
    Vestige * vapp = (Vestige *) context;
    bool atLoginStarted = NO;
    CFArrayRef loginItemsRef = LSSharedFileListCopySnapshot(listRef, NULL);
    NSArray *loginItems = CFBridgingRelease(loginItemsRef);
    
    NSURL *bundleURL = [NSURL fileURLWithPath:[[NSBundle mainBundle] bundlePath]];
    for (id item in loginItems) {
        LSSharedFileListItemRef itemRef = (LSSharedFileListItemRef)item;
        CFURLRef itemURLRef;
        
        if (LSSharedFileListItemResolve(itemRef, 0, &itemURLRef, NULL) == noErr) {
            NSURL *itemURL = (NSURL *)[NSMakeCollectable(itemURLRef) autorelease];
            if ([itemURL isEqual:bundleURL]) {
                atLoginStarted = true;
                break;
            }
        }
    }
    if (vapp->atLoginStarted != atLoginStarted) {
        if (atLoginStarted) {
            [vapp->startAtLoginItem setState:NSOnState];
        } else {
            [vapp->startAtLoginItem setState:NSOffState];
        }
        vapp->atLoginStarted = atLoginStarted;
    }
}

- (void)quitVestige {
    quit = true;
    if (procState == 5) {
        [NSApp terminate:nil];
    } else {
        [task terminate];
        [quitItem setTitle:@"Force stop"];
        forceStop = true;
    }
}


- (void)stopVestige {
    if (forceStop) {
        int pid = [task processIdentifier];
        kill(pid, SIGKILL);
    } else {
	    [task terminate];
        [quitItem setTitle:@"Force stop"];
        forceStop = true;
    }
}

- (void) applicationWillTerminate:(NSNotification *)notification {
	[task release];
}

- (void)hideWin {
    if (procState == 5)  {
        [NSApp terminate:nil];
    }
    consoleWinShown = false;
}

- (void)processQuit {
    if (!quit && (consoleWinShown || procState < 2))  {
        // user show console or starting failed
        procState = 5;
        NSStatusBar *bar = [NSStatusBar systemStatusBar];
        [bar removeStatusItem:statusItem];
        [self showCommandLineOutput];
    } else {
        [NSApp terminate:nil];
    }

}

- (void)finishLaunching {
    [super finishLaunching];

    NSMenu * mainMenu = [[NSMenu alloc] init];
    NSMenuItem * mainMenuFirstItem = [mainMenu addItemWithTitle:@"" action:NULL keyEquivalent:@""];
    
    NSMenu * mainSubMenu = [[NSMenu alloc] init];
    [mainMenu setSubmenu:mainSubMenu forItem:mainMenuFirstItem];
    [mainSubMenu addItemWithTitle: @"Quit" action:@selector(quitVestige) keyEquivalent: @"q"];
    [mainSubMenu addItemWithTitle: @"Copy" action:@selector(copy:) keyEquivalent: @"c"];
    [mainSubMenu addItemWithTitle: @"Select All" action:@selector(selectAll:) keyEquivalent: @"a"];
    [mainSubMenu addItemWithTitle: @"Close" action:@selector(performClose:) keyEquivalent: @"w"];
    [mainSubMenu addItemWithTitle: @"Minimize" action:@selector(performMiniaturize:) keyEquivalent: @"m"];
    [self setMainMenu:mainMenu];
    
    url = NULL;
    procState = 0;
    forceStop = false;
    quit = false;
    bufferSizeBytes = 0;
    
    statusItem = [[[NSStatusBar systemStatusBar] statusItemWithLength:NSVariableStatusItemLength] retain];
    statusMenu = [[NSMenu alloc] init];
    [statusMenu setAutoenablesItems:FALSE];
    
    openWebAdminItem = [statusMenu addItemWithTitle:@"Open web administration" action:@selector(openWebAdmin) keyEquivalent:@""];
    [openWebAdminItem setEnabled:FALSE];

    openBaseFolderItem = [statusMenu addItemWithTitle:@"Open base folder" action:@selector(openBaseFolder) keyEquivalent:@""];
    [openBaseFolderItem setEnabled:FALSE];

    [statusMenu addItemWithTitle:@"Show command line output" action:@selector(showCommandLineOutput) keyEquivalent:@""];

    startAtLoginItem = [statusMenu addItemWithTitle:@"Start at login" action:@selector(toggleLoginStart) keyEquivalent:@""];
    
    loginItemsListRef = LSSharedFileListCreate(NULL,
                                               kLSSharedFileListSessionLoginItems,
                                               NULL);
    if (loginItemsListRef) {
        LSSharedFileListAddObserver(loginItemsListRef,
                                    CFRunLoopGetMain(),
                                    kCFRunLoopCommonModes,
                                    loginItemsChanged,
                                    self);
        loginItemsChanged(loginItemsListRef, self);
    }
    
    [statusMenu addItem:[NSMenuItem separatorItem]];
    
    quitItem = [statusMenu addItemWithTitle:@"Stop" action:@selector(stopVestige) keyEquivalent:@""];
    
    NSImage *statusItemImage = [NSImage imageNamed:@"vestige.png"];
    if (!statusItemImage) {
        [statusItem setTitle:@"Vestige"];
    } else {
        [statusItem setImage:statusItemImage];
    }
    
    NSRect frame = NSMakeRect(0, 0, 700, 500);
    NSUInteger windowStyleMask = NSTitledWindowMask|NSResizableWindowMask|NSClosableWindowMask|NSMiniaturizableWindowMask;
    NSRect rect = [NSWindow contentRectForFrameRect:frame styleMask:windowStyleMask];
    win =  [[NSWindow alloc] initWithContentRect:rect styleMask:windowStyleMask backing: NSBackingStoreBuffered    defer:false];
    win.title = @"Vestige: command line output";
    [win setReleasedWhenClosed:false];
    [[NSNotificationCenter defaultCenter] addObserver:self
                                             selector:@selector(hideWin)
                                                 name:NSWindowWillCloseNotification
                                               object:win];
    
    NSScrollView *scrollview = [[NSScrollView alloc] initWithFrame:[[win contentView] frame]];
    NSSize contentSize = [scrollview contentSize];
    
    [scrollview setBorderType:NSNoBorder];
    [scrollview setHasVerticalScroller:YES];
    [scrollview setHasHorizontalScroller:YES];
    [scrollview setAutoresizingMask:NSViewWidthSizable | NSViewHeightSizable];
    
    textView = [[NSTextView alloc] initWithFrame:NSMakeRect(0, 0, contentSize.width, contentSize.height)];
    [textView setMinSize:NSMakeSize(contentSize.width, contentSize.height)];
    [textView setMaxSize:NSMakeSize(FLT_MAX, FLT_MAX)];
    [textView setVerticallyResizable:YES];
    [textView setHorizontallyResizable:YES];
    [textView setAutoresizingMask:(NSViewWidthSizable | NSViewHeightSizable)];
    [textView setEditable:false];
    
    [[textView textContainer]
    setContainerSize:NSMakeSize(FLT_MAX, FLT_MAX)];
    [[textView textContainer] setWidthTracksTextView:NO];
    
    [scrollview setDocumentView:textView];
    [win setContentView:scrollview];
    [win makeFirstResponder:textView];
    [win center];
    
    NSPipe *pipe = [[NSPipe alloc] init];
    task = [[NSTask alloc] init];
    fileHandle = [pipe fileHandleForReading];
    [[NSNotificationCenter defaultCenter] addObserver:self
                                             selector:@selector(dataAvailable:)
                                                 name:NSFileHandleReadCompletionNotification
                                               object:fileHandle];
    [fileHandle readInBackgroundAndNotify];
    [task setStandardOutput: pipe];
    [task setStandardError: pipe];
    CFSocketContext socketCtxt = {0, self, NULL, NULL, NULL};
    CFSocketRef ipv4cfsock = CFSocketCreate(kCFAllocatorDefault, PF_INET, SOCK_STREAM, IPPROTO_TCP,
                                            kCFSocketAcceptCallBack, handleConnect, &socketCtxt);
    
    struct sockaddr_in sin;
    
    memset(&sin, 0, sizeof(sin));
    sin.sin_len = sizeof(sin);
    sin.sin_family = AF_INET;
    sin.sin_port = htons(0);
    sin.sin_addr.s_addr= INADDR_ANY;
    
    CFDataRef sincfd = CFDataCreate(kCFAllocatorDefault, (UInt8 *)&sin, sizeof(sin));
    
    CFSocketSetAddress(ipv4cfsock, sincfd);
    CFRelease(sincfd);
    
    NSData *addr = [(NSData *)CFSocketCopyAddress(ipv4cfsock) autorelease];
    memcpy(&sin, [addr bytes], [addr length]);
    int port = ntohs(sin.sin_port);
    NSMutableDictionary *env = [[NSMutableDictionary alloc] init];
    [env addEntriesFromDictionary:[[NSProcessInfo processInfo] environment]];
    [env setObject:[@(port) stringValue] forKey:@"VESTIGE_LISTENER_PORT"];
    [env setObject:@"UTF-8" forKey:@"VESTIGE_CONSOLE_ENCODING"];
    [task setEnvironment:env];
    
    CFRunLoopSourceRef socketsource = CFSocketCreateRunLoopSource(kCFAllocatorDefault, ipv4cfsock, 0);
    
    CFRunLoopAddSource(CFRunLoopGetCurrent(), socketsource, kCFRunLoopDefaultMode);
    
    NSString* vestigeScript = [[NSBundle mainBundle] pathForResource:@"vestige" ofType:nil inDirectory:@"vestige_home"];
    [task setLaunchPath: vestigeScript];
    [task launch];
    
    [[NSNotificationCenter defaultCenter] addObserver:self
                                             selector:@selector(processQuit)
                                                 name:NSTaskDidTerminateNotification
                                               object:nil];

    [statusItem setMenu:statusMenu];
    [statusItem setHighlightMode:YES];
}

@end
