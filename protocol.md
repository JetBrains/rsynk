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

## Command `rsync --server --sender`

1. ### Setup protocol:

    *Server set nonblocking io for both input and output streams.*
    
    `S >> server protocol version [byte array {4}]` *example [31, 0, 0, 0] for version 31*
    
    `S << client protocol version [byte array {4}]`
   
2. ### Write compat flags
    *Since protocol 31.*
    *Compat-flags are set of per-session options, encoded into single byte, including:*
    
    `[
    CF_INC_RECURSE(1),
    CF_SYMLINK_TIMES(2),
    CF_SYMLINK_ICONV(4),
    CF_SAFE_FLIST(8),
    CF_AVOID_XATTR_OPTIM(16),
    CF_CHKSUM_SEED_FIX(32)
    ]`
    
    `S >> server compat flags [byte]`
  
        
    
      
     
   
