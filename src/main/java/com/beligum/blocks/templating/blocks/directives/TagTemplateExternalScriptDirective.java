package com.beligum.blocks.templating.blocks.directives;

import com.beligum.base.templating.velocity.directives.VelocityDirective;
import com.beligum.blocks.templating.blocks.TagTemplateResourcesDirective;
import org.apache.velocity.context.InternalContextAdapter;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.runtime.parser.node.Node;

import java.io.IOException;
import java.io.Writer;

/**
 * Created by bram on 4/25/15.
 */
@VelocityDirective(TagTemplateExternalScriptDirective.NAME)
public class TagTemplateExternalScriptDirective extends AbstractTagTemplateResourceDirective
{
    //-----CONSTANTS-----
    //blocksTemplateExternalScript
    public static final String NAME = "btesc";

    //-----VARIABLES-----

    //-----CONSTRUCTORS-----

    //-----PUBLIC METHODS-----
    @Override
    public String getName()
    {
        return NAME;
    }
    @Override
    public boolean render(InternalContextAdapter context, Writer writer, Node node) throws IOException, ResourceNotFoundException, ParseErrorException, MethodInvocationException
    {
        boolean print = (boolean) this.readArg(context, node, 0);
        String src = (String) this.readArg(context, node, 1);
        String element = this.readValue(context, node);
        TagTemplateResourcesDirective.getContextResources(context).addExternalScript(print, src, element);

        if (print) {
            writer.write(element);
        }

        return true;
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}
