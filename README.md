# Rsynk #
## SSH server that serves rsync read request ##  

### What it is ###
It does work like rsync daemon on a remote computer: allows you to connect to remote machine and download files. Hovewer it works via ssh, not via rsync protocol 

### What it is not ###
This is not a jvm rsync re-implementation. You cannot use it as jvm rsync client, only as a server

** Rsynk ** allows you to choose files which will be served dynamically, providing a callback to your application

//code example

Also ** rsynk ** makes possible to serve only a part of a file (i.e. a consistent part which is correct in terms of current application state)

//code example


Consider work in progress
