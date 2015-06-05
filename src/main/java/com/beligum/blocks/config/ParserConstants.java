package com.beligum.blocks.config;

/**
 * Created by bas on 03.11.14.
 * CSS-class-names used while parsing html to templates
 */
public class ParserConstants
{
    public enum ModificationLevel
    {
        NONE(0, ""),
        CAN_LAYOUT(1, ParserConstants.CAN_LAYOUT),
        CAN_EDIT(2, ParserConstants.CAN_EDIT_PROPERTY);

        private int permissionLevel;
        private String cssClass;
        ModificationLevel(int permissionLevel, String cssClass)
        {
            this.permissionLevel = permissionLevel;
            this.cssClass = cssClass;
        }
        @Override
        public String toString()
        {
            return cssClass;
        }
    }

    public final static String RESOURCE_ENDPOINT= "/v1/resource/";


    public final static String JSONLD_ID = "@id";
    public final static String JSONLD_TYPE = "@type";
    public final static String JSONLD_VALUE = "@value";
    public final static String JSONLD_LANGUAGE = "@language";
    public final static String JSONLD_GRAPH = "@graph";
    public final static String JSONLD_CONTEXT = "@context";

    public final static String CAN_LAYOUT = "can-layout";
    public final static String CAN_EDIT_PROPERTY = "can-edit";


    public static final String ENTITY_URL = "entityUrl";


}
