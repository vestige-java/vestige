Name:           vestige
Version:        0.0.1
Release:        1
Summary:        Java Application Manager

License:        GPLv3+
URL:            http://gaellalire.fr/vestige/

Source0:        vestige.tar.gz

BuildArchitectures: noarch

Requires:  bash java >= 1.6.0

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

%files
%{_datadir}/vestige

%config(noreplace) %{_sysconfdir}/vestige/logback.xml
%config(noreplace) %{_sysconfdir}/vestige/cacerts.p12

%doc



%changelog
