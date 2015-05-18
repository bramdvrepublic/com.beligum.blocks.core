package com.beligum.blocks.templating.blocks.directives;

import org.apache.velocity.context.InternalContextAdapter;
import org.apache.velocity.runtime.directive.Directive;
import org.apache.velocity.runtime.parser.node.Node;

/**
 * Created by bram on 4/25/15.
 */
public abstract class AbstractTagTemplateDirective extends Directive
{
    //-----CONSTANTS-----

    //-----VARIABLES-----

    //-----CONSTRUCTORS-----

    //-----PUBLIC METHODS-----
    @Override
    public int getType()
    {
        return BLOCK;
    }

    //-----PROTECTED METHODS-----
    protected Object readArg(InternalContextAdapter context, Node node, int index)
    {
        Object retVal = null;

        Node argNode = node.jjtGetNumChildren()>index ? node.jjtGetChild(index) : null;
        if (argNode!=null) {
            retVal = argNode.value(context);
        }

        return retVal;
    }
    protected String readValue(InternalContextAdapter context, Node node)
    {
        return node.jjtGetChild(node.jjtGetNumChildren()-1).literal();
    }

    //-----PRIVATE METHODS-----

}
