package com.beligum.blocks.config;

import com.beligum.base.filesystem.ConstantsFileEntry;
import gen.com.beligum.blocks.core.constants.blocks.core;

import java.util.HashMap;
import java.util.Map;

/**
 * This represents a widget in the blocks sidebar. To be used to link RDF data types to input-widgets.
 * <p>
 * Created by bram on 2/26/16.
 */
public enum InputType
{
    //-----CONSTANTS-----
    //this mapping makes sure we can use them in JS/CSS/...
    Undefined(core.Entries.INPUT_TYPE_UNDEFINED),
    Editor(core.Entries.INPUT_TYPE_EDITOR),
    InlineEditor(core.Entries.INPUT_TYPE_INLINE_EDITOR),
    Enum(core.Entries.INPUT_TYPE_ENUM),
    Boolean(core.Entries.INPUT_TYPE_BOOLEAN),
    Number(core.Entries.INPUT_TYPE_NUMBER),
    Date(core.Entries.INPUT_TYPE_DATE),
    Time(core.Entries.INPUT_TYPE_TIME),
    DateTime(core.Entries.INPUT_TYPE_DATETIME),
    Color(core.Entries.INPUT_TYPE_COLOR),
    Uri(core.Entries.INPUT_TYPE_URI),
    Resource(core.Entries.INPUT_TYPE_RESOURCE),
    ;

    /**
     * this will allow us to map to this enum from a constant string value
     */
    private static final Map<String, InputType> constantValueMapping = new HashMap<>();

    static {
        for (InputType widget : InputType.values()) {
            constantValueMapping.put(widget.getConstant(), widget);
        }
    }

    //-----VARIABLES-----
    private ConstantsFileEntry constantEntry;

    //-----CONSTRUCTORS-----
    InputType(ConstantsFileEntry constantEntry)
    {
        this.constantEntry = constantEntry;
    }

    //-----STATIC METHODS-----
    public static InputType valueOfConstant(String constantValue)
    {
        return constantValueMapping.get(constantValue);
    }

    //-----PUBLIC METHODS-----
    public String getConstant()
    {
        return constantEntry.getValue();
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----
}
