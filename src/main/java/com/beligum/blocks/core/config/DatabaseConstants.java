package com.beligum.blocks.core.config;

/**
 * Created by bas on 27.10.14.
 * Class for managing field-names of (meta-)data in db
 */
public class DatabaseConstants
{
    /**name of the scheme used for uri-identification of objects in the blocks-project*/
    public static final String SCHEME_NAME = "blocks";
    /**name of the application-version-field in db*/
    public final static String APP_VERSION = "appVersion";
    /**name of the creator-field in db*/
    public final static String CREATOR = "creator";
    /**name of the entity-template-class-field in db*/
    public final static String ENTITY_TEMPLATE_CLASS = "entityTemplateClass";
    /**the default page-template field-name for an entity-class*/
    public final static String PAGE_TEMPLATE = "pageTemplate";
    /**the (javascript-)script-tags*/
    public final static String SCRIPTS = "scripts";
    /**the (css-)linked files-tags*/
    public final static String LINKS = "links";
    /**the suffix for the set of all entity-templates of a certain class*/
    public static final String ENTITY_TEMPLATE_CLASS_SET_SUFFIX = "Set";
}
