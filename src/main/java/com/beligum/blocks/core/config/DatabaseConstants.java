package com.beligum.blocks.core.config;

import com.beligum.blocks.core.models.redis.templates.EntityTemplate;

/**
 * Created by bas on 27.10.14.
 * Class for managing field-names of (meta-)data in db
 */
public class DatabaseConstants
{
    /**name of the scheme used for uri-identification of objects in the blocks-project*/
    public static final String SCHEME_NAME = "blocks";
    /**
     * name of the blueprint-field in db
     * Note: the value of this string should always be the name of the field of an {@link com.beligum.blocks.core.models.redis.templates.EntityTemplate} holding the type of that template
     */
    public final static String BLUEPRINT_TYPE = "blueprintType";
    /**the (javascript-)script-tags*/
    public final static String SCRIPTS = "scripts";
    /**the (css-)linked files-tags*/
    public final static String LINKS = "links";
    /**the suffix for the set of all entity-templates of a certain class*/
    public static final String ENTITY_TEMPLATE_CLASS_SET_SUFFIX = "Set";

    public static final String URL_ID_MAPPING = "urlidmapping";

    public static final String SERVER_USER_NAME = "blocksServer";
    public static final String SERVER_START_UP = "blocksServerStartUp";
}
