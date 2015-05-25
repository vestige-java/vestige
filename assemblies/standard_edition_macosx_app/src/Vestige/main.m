#import <Cocoa/Cocoa.h>

int main(int argc, char *argv[])
{
    NSDictionary *infoDictionary = [[NSBundle mainBundle] infoDictionary];
    Class principalClass = NSClassFromString([infoDictionary objectForKey:@"NSPrincipalClass"]);
    NSApplication *applicationObject = [principalClass sharedApplication];
    if ([applicationObject respondsToSelector:@selector(run)])
    {
        [applicationObject  performSelectorOnMainThread:@selector(run) withObject:nil waitUntilDone:YES];
    }
    
    return 0;
}
