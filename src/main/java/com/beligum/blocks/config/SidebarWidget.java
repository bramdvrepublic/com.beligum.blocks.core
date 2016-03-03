package com.beligum.blocks.config;

import com.beligum.base.filesystem.ConstantsFileEntry;
import gen.com.beligum.blocks.core.constants.blocks.core;

import java.util.HashMap;
import java.util.Map;

/**
 * This represents a widget in the blocks sidebar. To be used to link RDF data types to input-widgets.
 *
 * Created by bram on 2/26/16.
 */
public enum SidebarWidget
{
    //-----CONSTANTS-----
    //this mapping makes sure we can use them in JS/CSS/...
    Undefined(core.Entries.SIDEBAR_WIDGET_UNDEFINED),
    Editor(core.Entries.SIDEBAR_WIDGET_EDITOR),
    InlineEditor(core.Entries.SIDEBAR_WIDGET_INLINE_EDITOR),
    ToggleButton(core.Entries.SIDEBAR_WIDGET_TOGGLE),

    ;

    /**
     * this will allow us to map to this enum from a constant string value
     */
    private static final Map<String, SidebarWidget> constantValueMapping = new HashMap<>();
    static {
        for (SidebarWidget widget : SidebarWidget.values()) {
            constantValueMapping.put(widget.getConstant(), widget);
        }
    }

    //-----VARIABLES-----
    private ConstantsFileEntry constantEntry;

    //-----CONSTRUCTORS-----
    SidebarWidget(ConstantsFileEntry constantEntry)
    {
        this.constantEntry = constantEntry;
    }

    //-----STATIC METHODS-----
    public static SidebarWidget valueOfConstant(String constantValue)
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
