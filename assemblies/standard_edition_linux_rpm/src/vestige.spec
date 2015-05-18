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

%description

%prep
%setup -q


%build
make


%install
rm -rf $RPM_BUILD_ROOT
%make_install


%files
%{_datadir}/applications/vestige.desktop
%{_datadir}/vestige/lib
%{_datadir}/vestige/repository
%{_datadir}/icons/hicolor/24x24/apps/vestige.png
%{_datadir}/icons/hicolor/32x32/apps/vestige.png
%{_datadir}/icons/hicolor/48x48/apps/vestige.png
%{_datadir}/icons/hicolor/128x128/apps/vestige.png
%{_datadir}/icons/hicolor/256x256/apps/vestige.png
%{_datadir}/vestige/vestige
%{_bindir}/vestige
%{_sysconfdir}/vestige/README
%{_sysconfdir}/vestige/template/logback.xml
%{_sysconfdir}/vestige/template/m2/vestige-se.xml


%doc



%changelog
