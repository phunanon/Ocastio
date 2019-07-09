# Ocastio
An experimental website for online organisational democracy.

## Prerequisites

You will need [Leiningen][] 2.0.0 or above installed.

[leiningen]: https://github.com/technomancy/leiningen

## Running

To start a web server for the application, run:

    lein ring server-headless

To browse the database use this command with [value] replaced for your circumstances:

    java -jar ~/.m2/repository/com/h2database/h2/[version]/h2-[version].jar

With `jdbc:h2:/absolute/path/ocastio`, without the .mv.db extension, as the uri.