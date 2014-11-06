package com.beligum.blocks.core.config;

/**
 * Created by bas on 03.11.14.
 * CSS-class-names used while parsing html to blocks
 */
public class CSSClasses
{
    /**
     * The css-class indicating that a certain <body>-tag is a entity.
     * (f.i. <body class="entity">)
     */
    public final static String ENTITY = "entity";
    /**
     * The prefix which a css-class indicating that a certain <body>-tag is of a certain entity-class must have to be recognized as such.
     * (f.i. 'entity-default' has the prefix 'entity-' added to it's entity-class name 'default')
     */
    public final static String ENTITY_CLASS_PREFIX = "entity-";
    /**
     * The prefix which a css-class indicating that a certain <div>-tag is of a certain block-class must have to be recognized as such.
     * (f.i. 'block-default' has the prefix 'block-' added to it's block-class name 'default')
     */
    public final static String BLOCK_CLASS_PREFIX = "block-";

    public final static String ROW = "row";
    public final static String BLOCK = "block";

    public final static String MODIFIABLE_ROW = "can-modify";
    public final static String LAYOUTABLE_ROW = "can-layout";
    public final static String CREATE_ENABLED_ROW = "can-create";
    public final static String EDITABLE_BLOCK = "can-edit";
}
