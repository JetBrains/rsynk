# Rsynk #
## SSH server that serves rsync reading request ##  

### What it is ###
It does work like rsync daemon on a remote computer: allows you to connect to remote machine and download files. Hovewer it works via ssh, not via rsync protocol 

### What it is not ###
This is not a jvm rsync re-implementation. You cannot use it as jvm rsync client, only as a server

**Rsynk** allows you to choose tracked files dynamically, providing a an API to your application:

```kotlin
val rsynk = Rsynk.newBuilder().apply {
                  port = 22
                  idleConnectionTimeout = 30000
                  nioWorkersNumber = 1
                  commandWorkersNumber = 1
                  }.build() //starts the server
                  
rsynk.addTrackingFiles(getNewlyCreatedUsersDataFiles())

...

rsync.setTrackingFiles(emptyList()) //delete all previously added files without having server downtime

...

rsynk.addTrackingFile(getLastUserDataFile()) //start over to serve a file

```                

Also **rsynk** makes possible to track only a part (currently continious) of a file (i.e. a consistent part which is correct in terms of current application state)

```kotlin
val logData = File("app/data/logs/users.log")
val rsynkFile = RsynkFile(logData, RsynkFileBoundaries { // this callback will be invoked for each request 
  val lowerBound = 0                                     // asking for users.log file
  val upperBound = getLastWritePosition(logData)         // making protection from partly serialized data possible
  RsynkFilesBoundaries(lowerBound, upperBound)
})
rsynk.addTrackingFile(rsynkFile)
```

Consider work in progress
