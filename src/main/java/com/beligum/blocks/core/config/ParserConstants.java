package com.beligum.blocks.core.config;

/**
 * Created by bas on 03.11.14.
 * CSS-class-names used while parsing html to blocks
 */
public class ParserConstants
{
    public enum ModificationLevel
    {
        NONE(0, ""),
        CAN_LAYOUT(1, ParserConstants.CAN_LAYOUT),
        CAN_EDIT(2, ParserConstants.CAN_EDIT);

        private int permissionLevel;
        private String cssClass;
        ModificationLevel(int permissionLevel, String cssClass){
            this.permissionLevel = permissionLevel;
            this.cssClass = cssClass;
        }
        @Override
        public String toString(){
            return cssClass;
        }
    }


    public final static String CAN_LAYOUT = "can-layout";
    public final static String CAN_EDIT = "can-edit";
    public final static String CAN_CHANGE = "can-change";

    public final static String INCLUDE = "include";

    public final static String DEFAULT_ENTITY_TEMPLATE_CLASS = "default";
    public final static String DEFAULT_PAGE_TEMPLATE = "default";


    /**the attribute indicating a html-file defines a page-template*/
    public final static String PAGE_TEMPLATE_ATTR = "template";
    /**the attribute indicating the template-content should be pasted their*/
    public final static String PAGE_TEMPLATE_CONTENT_ATTR = "template-content";

    /**the name of the variable in the template containing the entity to be rendered inside*/
    public static final String PAGE_TEMPLATE_ENTITY_VARIABLE_NAME = "entity";

    /**the name of the variable in the new-page template, containing all possible entity-classes*/
    public static final String ENTITY_CLASSES = "entityClasses";
    /**the name of the variable in the new-page template, containing the url of the new Entity*/
    public static final String ENTITY_URL = "entityUrl";


    /**the html-language attribute*/
    public static final String LANGUAGE = "lang";

    /**the form a reference-node takes inside parsed templates*/
    public static final String REFERENCE_TO = "reference-to";
    /**the keyword (attribute) indicating a certain template-class should be used as blueprint for all instances*/
    public static final String BLUEPRINT = "blueprint";
    /**the keyword indicating a certain entity is a copy of the entity-class (blueprint)*/
    public static final String USE_BLUEPRINT = "use-blueprint";

    public static final String PAGE_BLOCK = "page-block";
    public static final String ADDABLE_BLOCK = "addable-block";

    /**
     * Dynamic blocks
     */
    public class DynamicBlocks{
        /**the typeof indicating a block is a translation-block*/
        public static final String TRANSLATION_LIST = "translation-list";
    }


    /**
     * RDFa-constants
     */
    public static final String RESOURCE = "resource";
    public static final String PROPERTY = "property";
    public static final String PROPERTY_NAME = "name";
    public static final String TYPE_OF = "typeof";

}
