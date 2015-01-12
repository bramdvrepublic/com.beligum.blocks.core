#ID
The ID-class is a wrapper around a URI. This allows us to make subclasses of an ID. (URI is a final class and cannot be extended.)

One subclass is the RedisID, which holds functionalities for communication with the Redis-server:

 - It holds a version-field, constructed using the current time of the System. Redis.LAST_VERSION or Redis.NO_VERSION should be used form last version or unversioned id.
 - It can return the name of the list with all the versions of the object corresponding with that ID (= id.getUnversionedId())
 - It can transform a URL into an id suited for actually saving an object to the db
 
Two ID's are **equal** when their string-representation is equal (for RedisID's this includes timestamp-versioning)


All ID's used in Redis, are URI's mapping one-to-one on the object in question:

 - The ID of the redis-hash representing an EntityTemplate looks like this: "blocks://[db-site-alias]/[entityId]:[version]"
 - The list of all versions of a certain EntityTemplate has an id looking like this: "blocks://[db-site-alias]/[entityId]"
 - The ID of the redis-hash representing an EntityTemplateClass looks like this= "blocks://[db-site-alias]/[entityClassName]:[version]"
 - The list of all versions of a certain EntityTemplateClass has an id looking like this: "blocks://[db-site-alias]/[entityClassName]"
 - The default property of a certain EntityTemplateClass is an EntityTemplate (instance), represented by a redis-hash and has an id looking like this: "blocks://[db-site-alias]/[entityClassName]#[propertyName]:[version]"
 - the list of all versions of a property of a certain EntityTemplateClass has an id lookin like this: "blocks://[db-site-alias]/[entityClassName]#[propertyName]"
 - The set with all instances of a certain EntityTemplateClass has an id looking like this: "blocks://[db-site-allias]/[entityClassName]Set"

 
#Elements
 - Template is a super-name for entity-templates, entity-class-templates and page-templates. 
 - Two templates are **equal** when their content, meta-data (application version, creator, ...) and id (this doesn't include timestamp-versioning!) are equal
  
#Parsing
Is done in 4 visiting-lines:
 - From files to entity-classes in cacher (ON SERVER START UP)
 - From entity-classes in cacher to new stored entity-instances in db (ON ENTITY CREATION)
 - From stored entity-templates in db to html (ON READ)
 - From html received from client to updated instances in db (ON UPDATE)

##Html- and CSS-'rules'
 - Only page-templates can hold bootstrap-containers (entities should never be containers, and no containers should ever be used in entities)
 - No bootstrap-layout should be added to a typeof- or property-tag (= entity-tag)
 - If a entity-tag has can-layout, then the first elementy inside the entity must be a row
 - CSS-id's should only be used in page-templates, never inside entities. All css-styling should be achieved without use of id's
 - The css-rules for a certain entity will probably be grouped inside a class with the same name as the entity-class. These rules should be able to properly render the entity independently of any other css-rules.


#Redis
 - Only Storables can be saved to db
 - The method 'toHash()' in Storable decides which info is saved to db in hash-form. This method should be overwritten manually. (It is saved under "[templateId]:[version]")


#Exceptions
 - The only exceptions Redis.class throws (publicly) are RedisExceptions. Their cause can of course be all sorts of other exceptions.
 - The only exceptions TemplateParser.class throws (publicly) are ParseExceptions. Their cause can of course be all sorts of other exceptions.
 - The only exceptions AbstractTemplatesCache.class throws (publicly) are CacheExceptions. Their cause can of course be all sorts of other exceptions.
 - The only exceptions RedisID.class throws (publicly) are IDExceptions. Their cause can of course be all sorts of other exceptions.
 - The toHash()-method in all Storables can throw SerializationExceptions, the createInstanceFromHash()-methods in all templates can throw DeserializationExceptions.
 
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

  #Internationalization
   - Preferred languages can be specified in the configuration-xml under "blocks.site.languages", as a list, in order from most preferred language to least preferred language.
   - Languages should always be represented as a (2 letter) ISO 639 language-code.