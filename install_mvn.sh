

mvn -U install:install-file -Dfile=./libs/colloquial.jar  -DgroupId=com.colloquial -DartifactId=arithcode         -Dversion=1.1         -Dpackaging=jar
mvn -U install:install-file -Dfile=./libs/mdsj.jar -DgroupId=de.unikonstanz.inf -DartifactId=mdsj -Dversion=0.2 -Dpackaging=jar 
mvn -U install:install-file -Dfile=./libs/pal1.5.1.1.jar -DgroupId=nz.ac.auckland -DartifactId=pal -Dversion=1.5.1.1 -Dpackaging=jar 


