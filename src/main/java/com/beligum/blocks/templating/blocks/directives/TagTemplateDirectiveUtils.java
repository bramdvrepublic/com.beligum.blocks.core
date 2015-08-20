package com.beligum.blocks.templating.blocks.directives;

import org.apache.velocity.context.InternalContextAdapter;
import org.apache.velocity.runtime.parser.node.Node;

/**
 * Created by bram on 4/25/15.
 */
public class TagTemplateDirectiveUtils
{
    //-----CONSTANTS-----

    //-----VARIABLES-----

    //-----CONSTRUCTORS-----

    //-----PUBLIC METHODS-----

    //-----PROTECTED METHODS-----
    protected static Object readArg(InternalContextAdapter context, Node node, int index)
    {
        Object retVal = null;

        Node argNode = node.jjtGetNumChildren()>index ? node.jjtGetChild(index) : null;
        if (argNode!=null) {
            retVal = argNode.value(context);
        }

        return retVal;
    }
    protected static String readValue(InternalContextAdapter context, Node node)
    {
        Node contentNode = node.jjtGetChild(node.jjtGetNumChildren() - 1);
        if (contentNode.jjtGetNumChildren()>0) {
            return contentNode.literal();
        }
        else {
            return null;
        }
    }

    //-----PRIVATE METHODS-----

}
