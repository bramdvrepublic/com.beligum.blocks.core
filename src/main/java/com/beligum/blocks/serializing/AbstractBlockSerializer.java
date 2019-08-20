package com.beligum.blocks.serializing;

import com.beligum.base.filesystem.ConstantsFileEntry;
import com.beligum.base.utils.Logger;
import com.beligum.blocks.templating.HtmlTemplate;
import com.beligum.blocks.templating.TagTemplate;
import com.beligum.blocks.templating.TemplateCache;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.Iterables;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Map;

/**
 * Created by bram on Aug 20, 2019
 */
public abstract class AbstractBlockSerializer implements BlockSerializer
{
    //-----CONSTANTS-----

    //-----VARIABLES-----

    //-----CONSTRUCTORS-----

    //-----PUBLIC METHODS-----

    //-----PROTECTED METHODS-----
    /**
     * Creates the opening tag for the specified block tag with an optional arguments list (excluding "class") and an optional list of classes
     */
    protected StringBuilder createBlockHtml(TagTemplate blockType, Map<String, String> arguments, ConstantsFileEntry[] classes)
    {
        StringBuilder retVal = new StringBuilder();

        retVal.append("<").append(blockType.getTemplateName());

        // first add the general arguments
        if (arguments != null && arguments.size() > 0) {
            retVal.append(" ");
            Joiner.on(" ").skipNulls().appendTo(retVal, Iterables.transform(arguments.entrySet(), new Function<Map.Entry<String, String>, StringBuilder>()
            {
                @Override
                public StringBuilder apply(Map.Entry<String, String> input)
                {
                    if (input.getKey().equalsIgnoreCase("class")) {
                        Logger.warn("Found a \"class\" argument in the general arguments list, skipping. Please use the designated classes parameter; " + input);
                        return null;
                    }
                    else {
                        return new StringBuilder().append(input.getKey()).append("=\"").append(input.getValue()).append("\"");
                    }
                }
            }));
        }

        // then the classes
        if (classes != null && classes.length > 0) {
            retVal.append(" class=\"");
            Joiner.on(" ").skipNulls().appendTo(retVal, classes);
            retVal.append("\"");
        }

        retVal.append(">");

        return retVal;
    }
    /**
     * Appends a closing tag for the specified block type to the string builder
     */
    protected StringBuilder closeBlockHtml(TagTemplate blockType, StringBuilder stringBuilder)
    {
        return stringBuilder.append("</").append(blockType.getTemplateName()).append(">");
    }

    //-----PRIVATE METHODS-----
}
