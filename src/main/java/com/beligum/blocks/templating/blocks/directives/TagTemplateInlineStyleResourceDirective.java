package com.beligum.blocks.templating.blocks.directives;

import com.beligum.base.templating.velocity.directives.VelocityDirective;
import com.beligum.blocks.templating.blocks.TemplateResourcesDirective;
import org.apache.velocity.context.InternalContextAdapter;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.runtime.directive.Directive;
import org.apache.velocity.runtime.parser.node.Node;

import java.io.IOException;
import java.io.Writer;

/**
 * Created by bram on 4/25/15.
 */
@VelocityDirective(TagTemplateInlineStyleResourceDirective.NAME)
public class TagTemplateInlineStyleResourceDirective extends Directive
{
    //-----CONSTANTS-----
    //blocksTemplateInlineStyle
    public static final String NAME = "btist";

    //-----VARIABLES-----

    //-----CONSTRUCTORS-----

    //-----PUBLIC METHODS-----
    @Override
    public int getType()
    {
        return BLOCK;
    }
    @Override
    public String getName()
    {
        return NAME;
    }
    @Override
    public boolean render(InternalContextAdapter context, Writer writer, Node node) throws IOException, ResourceNotFoundException, ParseErrorException, MethodInvocationException
    {
        boolean print = (boolean) TagTemplateDirectiveUtils.readArg(context, node, 0);
        String element = TagTemplateDirectiveUtils.readValue(context, node);
        TemplateResourcesDirective.getContextResources(context).addInlineStyle(print, element);

        if (print) {
            writer.write(element);
        }

        return true;
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}
