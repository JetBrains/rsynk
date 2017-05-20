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
                  }.build()
                  
rsynk.addTrackingFiles(getNewlyCreatedUsersDataFiles())
...
rsync.setTrackingFiles(emptyList()) //delete all previously added files without having server downtime
...
rsynk.addTrackingFile(getLastUserDataFile()) // track a new file, 0 downtime
```                

Also **rsynk** makes possible to track only a part (currently continious) of a file (i.e. a consistent part which is correct in terms of your application current state)

```kotlin
val logData = File("app/data/logs/users.log")
val rsynkFile = RsynkFile(logData, RsynkFileBoundaries {
  // this callback will be invoked for each request 
  // asking for users.log file
  // so you can define correct boundaries from your application
  val lowerBound = 0                                   
  val upperBound = getLastWritePosition(logData)         
  RsynkFilesBoundaries(lowerBound, upperBound)
})
rsynk.addTrackingFile(rsynkFile)
```

Consider work in progress
