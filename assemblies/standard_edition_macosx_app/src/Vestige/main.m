#import <Cocoa/Cocoa.h>
#include "Vestige.h"

int main(int argc, char *argv[])
{
    NSApplication *applicationObject = [[Vestige class] sharedApplication];
    [applicationObject  performSelectorOnMainThread:@selector(run) withObject:nil waitUntilDone:YES];
    return 0;
}
