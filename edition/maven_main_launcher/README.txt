Add main class if you recreate a module-info.class :

jar --main-class fr.gaellalire.vestige.edition.maven_main_launcher.JPMSMavenMainLauncher --update --file target/vestige.edition.maven_main_launcher*.jar; \
pushd src/main/resources/ && jar xf ../../../target/vestige.edition.maven_main_launcher*.jar module-info.class; popd
