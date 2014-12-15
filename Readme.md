#ID
The ID-class is a wrapper around a URI. This allows us to make subclasses of an ID. (URI is a final class and cannot be extended.)
One subclass is the RedisID, which holds functionalities for communication with the Redis-server:

 - It holds a version-field, constructed using the current time of the System. Redis.LAST_VERSION or Redis.NO_VERSION should be used form last version or unversioned id.
 - It can return the name of the list with all the versions of the object corresponding with that ID (= id.getUnversionedId())
 - It can transform a URL into an id suited for actually saving an object to the db
 
Two ID's are **equal** when their string-representation is equal (for RedisID's this includes timestamp-versioning)
 
#Elements
 - Template is a super-name for entity-templates, entity-class-templates and page-templates. 
 - Two templates are **equal** when their content, meta-data (application version, creator, ...) and id (this doesn't include timestamp-versioning!) are equal
  
#Parsing
Is done in 4 visiting-lines:
 - From files to entity-classes in cacher (ON SERVER START UP)
 - From entity-classes in cacher to new stored entity-instances in db (ON ENTITY CREATION)
 - From stored entity-templates in db to html (ON READ)
 - From html received from client to updated instances in db (ON UPDATE)

#Redis
 - Only Storables can be saved to db
 - The method 'toHash()' in Storable decides which info is saved to db in hash-form. This method should be overwritten manually. (It is saved under "[templateId]:[version]")


#Exceptions
 - The only exceptions Redis.class throws (publicly) are RedisExceptions. Their cause can of course be all sorts of other exceptions.
 - The only exceptions TemplateParser.class throws (publicly) are ParseExceptions. Their cause can of course be all sorts of other exceptions.
 - The only exceptions AbstractTemplatesCache.class throws (publicly) are CacheExceptions. Their cause can of course be all sorts of other exceptions.
 - The only exceptions RedisID.class throws (publicly) are IDExceptions. Their cause can of course be all sorts of other exceptions.
 
#Page-templates
 - Should be defined using "template=..."- and "template-content"-attributes
 - The last found "default"-template (a html-file starting with \<html template="default"\>) is used as the default-template. If no such html-file is found in the templates-directory, we use: 
  \<!DOCTYPE\>
  \<html\>
  \<head\> \</head\>
  \<body\>
     \<div template-content reference-to="entity\> \</div\>
  \</body\>
  \</html\>