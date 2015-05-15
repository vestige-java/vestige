Name:           vestige
Version:        0.0.1
Release:        1
Summary:        Java Application Manager

License:        GPL
URL:            http://gaellalire.fr/vestige/

Source0:        vestige.tar.gz

BuildArchitectures: noarch

Requires:  java >= 1.6.0 pygtk2 >= 2.6.0

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
%{_datadir}/icons/hicolor/32x32/apps/vestige.png
%{_datadir}/icons/hicolor/48x48/apps/vestige.png
%{_datadir}/icons/hicolor/128x128/apps/vestige.png
%{_datadir}/icons/hicolor/256x256/apps/vestige.png
%{_bindir}/vestige
%{_bindir}/vestige_gtk_launcher.py
%{_sysconfdir}/vestige/README
%{_sysconfdir}/vestige/settings.xml
%{_sysconfdir}/vestige/logback.xml
%{_sysconfdir}/vestige/m2/vestige-se.xml


%doc



%changelog
