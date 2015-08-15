package com.beligum.blocks.templating.blocks.directives;

import com.beligum.base.security.PermissionRole;
import com.beligum.base.server.R;
import com.beligum.base.templating.velocity.directives.VelocityDirective;
import com.beligum.blocks.templating.blocks.HtmlTemplate;
import org.apache.velocity.context.InternalContextAdapter;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.runtime.parser.node.Node;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;

/**
 * Created by bram on 4/25/15.
 */
@VelocityDirective(TagTemplateInlineScriptResourceDirective.NAME)
public class TagTemplateInlineScriptResourceDirective extends TagTemplateAbstractResourceDirective
{
    //-----CONSTANTS-----
    //blocksTemplateInlineScript
    public static final String NAME = "btisc";

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
        PermissionRole roleScope = R.configuration().getSecurityConfig().lookupPermissionRole((String)TagTemplateDirectiveUtils.readArg(context, node, 1));
        HtmlTemplate.ResourceScopeMode mode = HtmlTemplate.ResourceScopeMode.values()[(int)TagTemplateDirectiveUtils.readArg(context, node, 2)];
        String element = TagTemplateDirectiveUtils.readValue(context, node);

        if (HtmlTemplate.testResourceRoleScope(roleScope) && HtmlTemplate.testResourceModeScope(mode)) {
            boolean added = TemplateResourcesDirective.getContextResources(context).addInlineScript(print, element, (StringWriter) writer);
            if (print) {
                writer.write(element);
            }
        }

        return true;
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}
