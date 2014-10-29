package com.beligum.blocks.core.config;

/**
 * Created by bas on 27.10.14.
 * Class for managing field-names of (meta-)data in db
 */
public class DatabaseFieldNames
{
    /**name of the application-version-field in db*/
    public final static String APP_VERSION = "appVersion";
    /**name of the creator-field in db*/
    public final static String CREATOR = "creator";
    /**name of the content-field in db*/
    public final static String CONTENT = "content";
    /**name of the page-class-field in db*/
    public final static String PAGE_CLASS = "pageClass";
    /**name of the type-field in db*/
    public final static String ELEMENT_CLASS_TYPE = "type";
    /**the name of the set of all row-elements in the database*/
    public final static String ROW_SET_NAME = "rows";
    /**the name of the set of all block-elements in the database*/
    public final static String BLOCK_SET_NAME = "blocks";
    /**the suffix used to indicate a pages meta-data-hash in db, it is the suffix used to distinguish the page-info from the page-content (it's rows and blocks)*/
    public final static String PAGE_INFO_SUFFIX = "info";
}
