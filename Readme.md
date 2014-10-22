#ID
The ID-class is a wrapper around a URI. This allows us to make subclasses of an ID. (URI is a final class and cannot be extended.)
One subclass is the RedisID, which holds some functionalities for communication with the Redis-server:

 - It holds a version-field, constructed using the current time of the System.
 - It can return the name of the list with all the versions of the object corresponding with that ID
 - It can transform a URL into a shorter form, suited for actually saving an object to the db
 
 #Parsing
 The PageParser needs quite a specific project-structure to run correctly for the moment. This must be generalised, or at least some error-checks should be done. (This will take some time...)