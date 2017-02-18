# Roles:

    `S` for Server
    `C` for Client
    
# Actions:

    `<<` reading
    `>>` writing

# Transferring data types:

    [byte], 
    [byte array], 
    [int] (32 bit)
    
    
# Other:
*Italic* used for notes and comments
 
# Protocol from server side perspective (not in daemon mode)

1. ### Setup protocol:

    *server set nonblocking io for both input and output streams*
    
    `S >> server protocol version [byte array 4]` *example [31, 0, 0, 0] for version 31*
    
    `S << client protocol version [byte array 4]`
    
    *only main part is written, so for `31.0` `31` will be sent*
    
      
     
   
