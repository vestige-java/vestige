Name:           vestige
Version:        0.0.1
Release:        1
Summary:        Java Application Manager

License:        GPL
URL:            http://gaellalire.fr/vestige/

Source0:        vestige.tar.gz

BuildArchitectures: noarch

Requires:  bash java >= 1.6.0 pygtk2 >= 2.6.0 python-appindicator

%define __jar_repack %{nil}
%define _binaries_in_noarch_packages_terminate_build 0

%description

%prep
%setup -q


%build
make


%install
rm -rf $RPM_BUILD_ROOT
%make_install

%posttrans
/usr/bin/gtk-update-icon-cache -q -t -f %{_datadir}/icons/hicolor || :

%files
%{_datadir}/applications/vestige.desktop
%{_datadir}/vestige
%{_datadir}/icons/hicolor/24x24/apps/vestige.png
%{_datadir}/icons/hicolor/32x32/apps/vestige.png
%{_datadir}/icons/hicolor/48x48/apps/vestige.png
%{_datadir}/icons/hicolor/128x128/apps/vestige.png
%{_datadir}/icons/hicolor/256x256/apps/vestige.png
%{_bindir}/vestige
%{_sysconfdir}/vestige


%doc



%changelog
