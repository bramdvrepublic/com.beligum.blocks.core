package com.beligum.blocks.templating.blocks.directives;

import com.beligum.base.server.R;
import com.beligum.base.templating.velocity.directives.VelocityDirective;
import org.apache.velocity.context.InternalContextAdapter;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.runtime.directive.Directive;
import org.apache.velocity.runtime.parser.node.Node;

import java.io.IOException;
import java.io.Writer;

/**
 * This directive is a dynamic URL wrapper that fingerprints an URL it the last moment possible (during template rendering)
 *
 * Created by bram on 4/25/15.
 */
@VelocityDirective(ResourceUriDirective.NAME)
public class ResourceUriDirective extends Directive
{
    //-----CONSTANTS-----
    //blocksResourceUriDirective
    public static final String NAME = "brud";

    //-----VARIABLES-----

    //-----CONSTRUCTORS-----

    //-----PUBLIC METHODS-----
    @Override
    public int getType()
    {
        return LINE;
    }
    @Override
    public String getName()
    {
        return NAME;
    }
    @Override
    public boolean render(InternalContextAdapter context, Writer writer, Node node) throws IOException, ResourceNotFoundException, ParseErrorException, MethodInvocationException
    {
        Node uriArg = node.jjtGetNumChildren() > 0 ? node.jjtGetChild(0) : null;
        if (uriArg != null) {
            //Note that this directive is only activated when fingerprinting is enabled and the resource is local and is non-immutable,
            //if we need to post-parse URIs for other uses, please change the code in HtmlParser
            writer.write(R.resourceManager().getFingerprinter().fingerprintAllUris((String) uriArg.value(context)));
        }

        return true;
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}
