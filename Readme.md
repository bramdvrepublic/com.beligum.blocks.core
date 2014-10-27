#ID
The ID-class is a wrapper around a URI. This allows us to make subclasses of an ID. (URI is a final class and cannot be extended.)
One subclass is the RedisID, which holds some functionalities for communication with the Redis-server:

 - It holds a version-field, constructed using the current time of the System.
 - It can return the name of the list with all the versions of the object corresponding with that ID
 - It can transform a URL into a shorter form, suited for actually saving an object to the db
 
Two ID's are **equal** when their string-representation is equal (for RedisID's this includes timestamp-versioning)
 
#Elements
 - Element is a super-name for blocks and rows. 
 - Two elements are **equal** when their content, meta-data (application version, creator, ...) and id (this doesn't include timestamp-versioning!) are equal
  
#Parsing

#Exceptions
 - The only exceptions Redis.class throws (publicly) are RedisExceptions. Their cause can of course be all sorts of other exceptions.
 - The only exceptions PageParser.class throws (publicly) are PageParserExceptions. Their cause can of course be all sorts of other exceptions.
 - The only exceptions PageClassCache.class throws (publicly) are PageClassCacheExceptions. Their cause can of course be all sorts of other exceptions.
 - ElementExceptions are thrown when something is wrong with a certain element (block or row)