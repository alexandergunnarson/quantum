echo "Installing datomic-pro-0.9.5173 in local maven repository..."
mvn install:install-file -DgroupId="com.datomic" -DartifactId=datomic-pro -Dfile="datomic-pro-0.9.5173.jar" -DpomFile="pom.xml"
