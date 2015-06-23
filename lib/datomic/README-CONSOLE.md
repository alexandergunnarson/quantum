# Datomic Console

## Install

If you downloaded Console as a separate download (i.e.: not bundled with Datomic Pro), you will need to install it alongside Datomic:

Run this command from the directory you unzipped Console to:

    bin/install-console path-to-datomic-directory

## Running Datomic Console

Switch to your Datomic directory and run:

    bin/console -p 8080 alias transactor-uri-no-db

to start the Console, where:

* alias is a text name for a transactor

* transactor-uri-no-db identifies your transactor without a db name,

For example, if your transactor URI is datomic:dev://localhost:4334/ and you want to give it an alias of mbrainz:

    bin/console -p 8080 mbrainz datomic:dev://localhost:4334/

Once the Console is running, open this url:

    http://localhost:8080/browse

in your browser (Chrome recommended).
