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
import java.io.Writer;

/**
 * Created by bram on 4/25/15.
 */
@VelocityDirective(TagTemplateExternalScriptResourceDirective.NAME)
public class TagTemplateExternalScriptResourceDirective extends TagTemplateAbstractResourceDirective
{
    //-----CONSTANTS-----
    //blocksTemplateExternalScript
    public static final String NAME = "btesc";

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
        String src = (String) TagTemplateDirectiveUtils.readArg(context, node, 1);
        PermissionRole roleScope = R.configuration().getSecurityConfig().lookupPermissionRole((String)TagTemplateDirectiveUtils.readArg(context, node, 2));
        HtmlTemplate.ResourceScopeMode mode = HtmlTemplate.ResourceScopeMode.values()[(int)TagTemplateDirectiveUtils.readArg(context, node, 3)];
        String element = TagTemplateDirectiveUtils.readValue(context, node);

        if (HtmlTemplate.testResourceRoleScope(roleScope) && HtmlTemplate.testResourceModeScope(mode)) {
            TemplateResourcesDirective.getContextResources(context).addExternalScript(print, src, element);
            if (print) {
                writer.write(element);
            }
        }

        return true;
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}
