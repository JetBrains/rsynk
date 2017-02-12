# Roles:

    `S` for Server
    `C` for Client
    
# Actions:

    `<<` reading
    `>>` writing

# Transferring data types:

    [byte], 
    [byte array], 
    [int] (always 32 bit)
    
    
# Other:
*Italic* used for notes and comments
 
# Protocol from server side perspective (not in daemon mode)

1. ### Setup protocol:

    *server set nonblocking io for both input and output streams*
    
    `S >> server protocol version [byte]`
    
    `S << client protocol version [byte]`
    
    *only main part is written, so for `31.0` `31` will be sent*
    
      
     
   
