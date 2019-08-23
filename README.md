# Ocastio
An experimental website for online organisational democracy.  
A website running a similar copy to this repository is currently hosted at [ocastio.uk](https://ocastio.uk)

## Prerequisites

You will need [Leiningen][] 2.0.0 or above. You will need to create a blank database using create-db.clj, and provide a [Telegram] bot token if you want that feature.

[leiningen]: https://github.com/technomancy/leiningen
[telegram]: https://core.telegram.org/bots

## Running

Start a web server with `lein ring server-headless`, or compile into an uberjar with `lein ring uberjar`

To browse the database use this command with [value] replaced for your circumstances:

    java -jar ~/.m2/repository/com/h2database/h2/[version]/h2-[version].jar

With `jdbc:h2:/absolute/path/ocastio-db`, without the .mv.db extension, as the uri.