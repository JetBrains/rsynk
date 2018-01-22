# Rsynk #

[![JetBrains team project](http://jb.gg/badges/team-flat-square.svg)](https://confluence.jetbrains.com/display/ALL/JetBrains+on+GitHub)

### What it is ###
An embeddable ssh server for [rsync client](https://rsync.samba.org) with additional features.

### What it is not ###
Not an rsync re-implementation. Unlike server, client functionality is not an aim of the project.

### Goals ###
The goal is to make a server for rsync client and allows rich files content manipulations which are not implemented in vanilla rsync. Rsynk supplied with API to dynamically select which files are downloadable and set the bounds on those files - offset and length, dynamically as well.

### Compatible rsync clients ###
Minimal client version is 3.1.0 (released September 28th, 2013, see [versions](https://rsync.samba.org/)), newer versions of rsync can be used. If you're using another rsync protocol implementation - the version of protocol must be 31 or newer. 

### Building project
[Gradle](http://www.gradle.org) is used to build and test. JDK 1.8 and [Kotlin](http://kotlinlang.org)
1.1.1 are required. To build the project, run:

    ./gradlew
