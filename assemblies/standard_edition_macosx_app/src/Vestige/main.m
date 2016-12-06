#import <Cocoa/Cocoa.h>
#include "Vestige.h"

Vestige * app = NULL;

void signalHandler(int signal) {
    if (app != NULL) {
        [app quitVestige];
    }
}

int main(int argc, char *argv[])
{
    signal(SIGTERM, &signalHandler);
    signal(SIGINT, &signalHandler);

    app = (Vestige *) [[Vestige class] sharedApplication];
    [app performSelectorOnMainThread:@selector(run) withObject:nil waitUntilDone:YES];
    return 0;
}
