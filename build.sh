export MAVEN_OPTS=-Xss32m
mvn clean install:install-file -Dfile=libs/soot-infoflow-android-classes.jar -DgroupId=de.tud.sse -DartifactId=soot-infoflow-android -Dversion=2.7.1 -Dpackaging=jar
mvn clean install:install-file -Dfile=libs/soot-infoflow-classes.jar -DgroupId=de.tud.sse -DartifactId=soot-infoflow -Dversion=2.7.1 -Dpackaging=jar
mvn clean install
