# BlocksID
The BlocksID-class is a wrapper around a URI, which holds functionalities for communication with the database:

 - It holds a version-field, constructed using the current time of the System. Redis.LAST_VERSION or Redis.NO_VERSION should be used for last version or unversioned id.
 - It can return the name of the list with all the versions of the object corresponding with that ID (= id.getUnversionedId())
 - It can transform a URL into an id suited for actually saving an object to the db
 
Two ID's are **equal** when their string-representation is equal (for RedisID's this includes timestamp-versioning)

# Redis and Storable
 - Extends interface Database<Storable>
 - The method 'toHash()' in Storable decides which info is saved to db in hash-form. This method should be overwritten manually. (It is saved under "[storableId]:[version]")
 - A storable holds creation and update information
 - All fields that need to be saved to db, should be publicly accessible through public field declaration or get- and set-method.

All ID's used in Redis, are URI's mapping one-to-one on the object in question:

 - The id of the html-content in a certain language looks like this: "blocks://[db-site-alias]/[entityId]:[version]/[languageCode]"
 - The ID of the redis-hash representing an EntityTemplate looks like this: "blocks://[db-site-alias]/[entityId]:[version]"
 - The list of all versions of a certain EntityTemplate has an id looking like this: "blocks://[db-site-alias]/[entityId]"
 - The ID of the redis-hash representing an EntityTemplateClass looks like this= "blocks://[db-site-alias]/[entityClassName]:[version]"
 - The list of all versions of a certain EntityTemplateClass has an id looking like this: "blocks://[db-site-alias]/[entityClassName]"
 - The default property of a certain EntityTemplateClass is an EntityTemplate (instance), represented by a redis-hash and has an id looking like this: "blocks://[db-site-alias]/[entityClassName]#[propertyName]:[version]"
 - the list of all versions of a property of a certain EntityTemplateClass has an id lookin like this: "blocks://[db-site-alias]/[entityClassName]#[propertyName]"
 - The set with all instances of a certain EntityTemplateClass has an id looking like this: "blocks://[db-site-allias]/[entityClassName]Set"


# Templates
 - Template is a super-name for entity-templates, entity-class-templates and page-templates. 
 - Two templates are **equal** when their content, meta-data (application version and deletion-flag) and id (this doesn't include timestamp-versioning!) are equal. Creation- and update-info is not brought into the mix!
 - Creation- and update-info is being handled in the Storable constructor and Database.update(Storable storable) method.
  
# Parsing
Is done in 5 visiting-lines:
 - From files to a list of all entity-classes and all page-templates found (ON SERVER START UP)
 - That list is given to a second visitor which in turn makes default instances where necessary and caches all classes (ON SERVER START UP)
 - From entity-classes in cacher to new stored entity-instances in db (ON ENTITY CREATION)
 - From stored entity-templates in db to html (ON READ)
 - From html received from client to updated instances in db (ON UPDATE)

## Html- and CSS-'rules'
 - Only page-templates can hold bootstrap-containers (entities should never be containers, and no containers should ever be used in entities)
 - No bootstrap-layout should be added to a blueprint- or property-tag (= entity-tag)
 - If a entity-tag has can-layout, then the first element inside the entity must be a row
 - CSS-id's should only be used in page-templates, never inside entities. All css-styling should be achieved without use of id's
 - The css-rules for a certain entity will probably be grouped inside a class with the same name as the entity-class. These rules should be able to properly render the entity independently of any other css-rules. This also means no bootstrap-classes can be used to render good design.


# Exceptions
 - The only exceptions Redis.class throws (publicly) are RedisExceptions. Their cause can of course be all sorts of other exceptions.
 - The only exceptions TemplateParser.class throws (publicly) are ParseExceptions. Their cause can of course be all sorts of other exceptions.
 - The only exceptions AbstractTemplatesCache.class throws (publicly) are CacheExceptions. Their cause can of course be all sorts of other exceptions.
 - The only exceptions RedisID.class throws (publicly) are IDExceptions. Their cause can of course be all sorts of other exceptions.
 - The toHash()-method in all Storables can throw SerializationExceptions, the createInstanceFromHash()-methods in all templates can throw DeserializationExceptions.
 
# Page-templates
 - Should be defined using "template=..."- and "template-content"-attributes
 - The last found "default"-template (a html-file starting with \<html template="default"\>) is used as the default-template. If no such html-file is found in the templates-directory, we use: 
  \<!DOCTYPE\>
  \<html\>
  \<head\> \</head\>
  \<body\>
     \<div template-content reference-to="entity\> \</div\>
  \</body\>
  \</html\>

# Internationalization
 - Preferred languages can be specified in the configuration-xml under "blocks.site.languages", as a list, in order from most preferred language to least preferred language.
 - Languages should always be represented as a (2 letter) ISO 639 language-code.
 - If a url needs to be translated, it needs to be an absolute url of this site-domain or a relative url starting with a '/'. Relative urls without '/' will not be translated!

# Script- and style-injection
 - Scripts and links (f.i. css-files) can be added to page-templates, class blueprints and dynamic blocks
 - Scripts of page-templates and entity-classes are saved to db as a html-blob
 - When a page is send to the client, the scripts and links are injected in this order:
     1. links of page-template
     2. links of blueprints
     3. links of dynamic blocks
     4. scripts of page-template
     5. scripts of blueprints
     6. scripts of dynamic blocks
     (7. links of blocks core frontend)
     (8. scripts of blocks core frontend)

# Editable pages
- a page, when loaded, is automatically set to editable
- All blocks on the first level in the page are editable
    - EXCEPT if the blueprint of this block defines it as not editable with the attribute CAN_NOT_EDIT
- if a block is editable, then it's properties are editable too
    - EXCEPT if the property is marked with CAN-NOT-EDIT attribute
- If a property is editable but the blueprint is marked as not editable, then the property becomes not editable
- When a propertie is not editable, the all content (properties) on a deeper level becomes not editable

If we say a block is editable, it has a double meaning. If available, plugins will be loaded to make
the properies editable in the client. (e.g. a text editor). It also means that the content from the database is used
to show in the fields. If a property of a block is marked as not editable, then the content from the blueprint will be used
even if there is content available in the database.

This way we have full control of the shown content, based on the blueprints
