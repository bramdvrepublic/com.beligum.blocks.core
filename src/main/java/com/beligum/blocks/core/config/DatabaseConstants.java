package com.beligum.blocks.core.config;

/**
 * Created by bas on 27.10.14.
 * Class for managing field-names of (meta-)data in db
 */
public class DatabaseConstants
{
    /**name of the application-version-field in db*/
    public final static String APP_VERSION = "appVersion";
    /**name of the creator-field in db*/
    public final static String CREATOR = "creator";
    /**name of the template-field in db*/
    public final static String TEMPLATE = "template";
    /**name of the page-class-field in db*/
    public final static String VIEWABLE_CLASS = "viewableClass";
    /**name of the type-field in db*/
    public final static String ROW_TYPE = "type";
    /**the suffix used to indicate a entities meta-data-hash in db, it is the suffix used to distinguish the page-info from the page-template (it's rows and blocks)*/
    public final static String HASH_SUFFIX = "hash";
}
