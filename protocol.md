  
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

### Setup protocol:

    /* Server set nonblocking io for both input and output streams */
    
    >> server protocol version [byte array {4}] /* example [31, 0, 0, 0] for version 31 */
    
    << client protocol version [byte array {4}]
   
### Send compat flags
    /* Since protocol 31 
    Compat-flags are set of per-session options, encoded into single byte, including: 
    
    `[
    CF_INC_RECURSE(1),
    CF_SYMLINK_TIMES(2),
    CF_SYMLINK_ICONV(4),
    CF_SAFE_FLIST(8),
    CF_AVOID_XATTR_OPTIM(16),
    CF_CHKSUM_SEED_FIX(32)
    ]`
    */
    
    >> server compat flags [byte]
    
### Send checksum seed
    
    /* Seed for fast rolling checksum */
    
    >>  checksum seed [int]
    
    
    
      
     
   
