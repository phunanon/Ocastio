# Ocastio
An experimental website for online organisational democracy.  
~A website running a similar copy to this repository is currently hosted at [ocastio.uk](https://ocastio.uk)~  
Shut-down on the 25th of April, 2021.

## Prerequisites

You will need [Leiningen][] 2.0.0 or above. You will need to create a blank database using `createdb.clj`, and provide a [Telegram] bot token if you want that feature.

[leiningen]: https://github.com/technomancy/leiningen
[telegram]: https://core.telegram.org/bots

## Running

Start a web server with `lein ring server-headless`.

To browse the database use this command with [version] replaced for your circumstances:

    java -jar ~/.m2/repository/com/h2database/h2/[version]/h2-[version].jar

With `jdbc:h2:/absolute/path/ocastio-db`, without the `.mv.db` extension, as the uri.

## Deploying

    lein with-profile prod ring uberjar

If not on Linux, or without access to `/tmp`, you'll have to tweak `project.clj` -  Morse was causing a "file name too long" error in any deeper work directories, at least on my machine.
