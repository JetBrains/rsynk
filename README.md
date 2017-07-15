# Rsynk #

[![GitHub license](https://img.shields.io/hexpm/l/plug.svg)](http://www.apache.org/licenses/LICENSE-2.0.html)
![Repository Size](https://reposs.herokuapp.com/?path=JetBrains/rsynk)

Consider work in progress. It's not yet ready to use.

### What it is ###
An embeddable ssh server for [rsync clinet](https://rsync.samba.org).

### What it is not ###
This is notrsync re-implementation. Unlike server, client functionality is not an aim of project.

### Goals ###
The goal is to make rsync server that allows rich files and their content manipulations. That means rsynk supplied with an API to dynamically select which files are served and set the bounds on those files - offset and length, dynamically as well.

### Supported rsync versions ###
Required mimimal client version is 3.1.0 (released September 28th, 2013)

### Example ###
**Rsynk** is able to manage tracked files dynamically:

```kotlin
val rsynk = Rsynk.newBuilder().apply {
                  port = 22
                  idleConnectionTimeout = 30000
                  nioWorkersNumber = 1
                  commandWorkersNumber = 1
                  }.build()
                  
rsynk.addTrackingFiles( ... ) 
...
rsync.setTrackingFiles( ... )
```                

It is possible to track only a part of a file (i.e. a consistent part which is correct in terms of your application current state)

```kotlin
val logData = File("app/data/logs/users.log")
val rsynkFile = RsynkFile(logData, RsynkFileBoundaries {

  // this callback will be invoked for each request 
  // boundaries are as dynamic as it possible

  val lowerBound = 0                                   
  val upperBound = getLastWritePosition(logData) // an example of method you can provide         
  RsynkFilesBoundaries(lowerBound, upperBound)
})
rsynk.addTrackingFile(rsynkFile)
```

### Building from Source
[Gradle](http://www.gradle.org) is used to build and test. JDK 1.8 and [Kotlin](http://kotlinlang.org)
1.1.1 are required. To build the project, run:

    ./gradlew
