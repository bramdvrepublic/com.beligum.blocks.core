package com.beligum.blocks.core.config;

/**
 * Created by bas on 03.11.14.
 * CSS-class-names used while parsing html to blocks
 */
public class CSSClasses
{
    /**
     * The css-class indicating that a certain <body>-tag is a page.
     * (f.i. <body class="page">)
     */
    public final static String PAGE = "page";
    /**
     * The prefix which a css-class indicating that a certain <body>-tag is of a certain page-class must have to be recognized as such.
     * (f.i. 'page-default' has the prefix 'page-' added to it's page-class name 'default')
     */
    public final static String PAGECLASS_PREFIX = "page-";

    public final static String ROW = "row";
    public final static String BLOCK = "block";

    public final static String MODIFIABLE_ROW = "can-modify";
    public final static String LAYOUTABLE_ROW = "can-layout";
    public final static String CREATE_ENABLED_ROW = "can-create";
    public final static String EDITABLE_BLOCK = "can-edit";
}
