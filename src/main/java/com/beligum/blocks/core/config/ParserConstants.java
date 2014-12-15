package com.beligum.blocks.core.config;

/**
 * Created by bas on 03.11.14.
 * CSS-class-names used while parsing html to blocks
 */
public class ParserConstants
{
    public final static String CAN_LAYOUT = "can-layout";
    public final static String CAN_CREATE = "can-create";
    public final static String CAN_EDIT = "can-edit";

    public final static String DEFAULT_ENTITY_TEMPLATE_CLASS = "default";


    /**the attribute indicating a html-file defines a page-template*/
    public final static String PAGE_TEMPLATE_ATTR = "template";
    /**the attribute indicating the template-content should be pasted their*/
    public final static String PAGE_TEMPLATE_CONTENT_ATTR = "template-content";

    /**the name of the variable in the template containing the entity to be rendered inside*/
    public static final String PAGE_TEMPLATE_ENTITY_VARIABLE_NAME = "entity";

    /**the name of the variable in the new-page template, containing all possible entity-classes*/
    public static final String ENTITY_CLASSES = "entityClasses";
    /**the form a reference-node takes inside parsed templates*/
    public static final String REFERENCE_TO = "reference-to";
    /**the keyword (attribute) indicating a certain template-class should be used as bleuprint for all instances*/
    public static final String BLEUPRINT = "bleuprint";

    /**
     * RDFa-constants
     */
    public static final String RESOURCE = "resource";
    public static final String PROPERTY = "property";
    public static final String TYPE_OF = "typeof";

}
