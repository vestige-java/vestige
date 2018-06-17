Remove .BUILDINFO

~/.config/pacman/makepkg.conf 
function write_buildinfo() {
	msg2 "Don't write .BUILDINFO"
}


Install package

pacman -U vestige-*-1-any.pkg.tar.xz