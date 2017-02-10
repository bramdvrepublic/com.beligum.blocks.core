package com.beligum.blocks.templating.blocks.directives;

import com.beligum.base.security.PermissionRole;
import com.beligum.base.server.R;
import com.beligum.base.templating.velocity.directives.VelocityDirective;
import com.beligum.blocks.templating.blocks.HtmlTemplate;
import org.apache.velocity.context.InternalContextAdapter;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.runtime.directive.Directive;
import org.apache.velocity.runtime.parser.node.Node;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;

/**
 * Created by bram on 4/25/15.
 */
@VelocityDirective(TagTemplateResourceDirective.NAME)
public class TagTemplateResourceDirective extends Directive
{
    //-----CONSTANTS-----
    //blocksTemplateResourceDirective
    public static final String NAME = "btrd";

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
        TemplateResourcesDirective.Argument type = TemplateResourcesDirective.Argument.values()[((int) TagTemplateDirectiveUtils.readArg(context, node, 0))];
        boolean print = (boolean) TagTemplateDirectiveUtils.readArg(context, node, 1);
        String urlArgument = (String) TagTemplateDirectiveUtils.readArg(context, node, 2);
        boolean fingerprinted = (boolean) TagTemplateDirectiveUtils.readArg(context, node, 3);
        boolean isLocalResource = (boolean) TagTemplateDirectiveUtils.readArg(context, node, 4);
        PermissionRole roleScope = R.configuration().getSecurityConfig().lookupPermissionRole((String) TagTemplateDirectiveUtils.readArg(context, node, 5));
        HtmlTemplate.ResourceScopeMode mode = HtmlTemplate.ResourceScopeMode.values()[(int) TagTemplateDirectiveUtils.readArg(context, node, 6)];
        HtmlTemplate.ResourceJoinHint joinHint = HtmlTemplate.ResourceJoinHint.values()[(int) TagTemplateDirectiveUtils.readArg(context, node, 7)];

        if (HtmlTemplate.testResourceRoleScope(roleScope) && HtmlTemplate.testResourceModeScope(mode)) {
            if (writer instanceof StringWriter) {
                boolean added = false;
                String element = TagTemplateDirectiveUtils.readValue(context, node);

                switch (type) {
                    case inlineStyles:
                        added = TemplateResourcesDirective.getContextResources(context).addInlineStyle(element, (StringWriter) writer, print, joinHint, fingerprinted);
                        break;
                    case externalStyles:
                        added = TemplateResourcesDirective.getContextResources(context).addExternalStyle(element, (StringWriter) writer, urlArgument, print, joinHint, fingerprinted, isLocalResource);
                        break;
                    case inlineScripts:
                        added = TemplateResourcesDirective.getContextResources(context).addInlineScript(element, (StringWriter) writer, print, joinHint, fingerprinted);
                        break;
                    case externalScripts:
                        added = TemplateResourcesDirective.getContextResources(context).addExternalScript(element, (StringWriter) writer, urlArgument, print, joinHint, fingerprinted, isLocalResource);
                        break;
                    default:
                        throw new ParseErrorException("Encountered unsupported resource type in directive #" + NAME + " of type " + type + "; this shouldn't happen");
                }
            }
            else {
                throw new ClassCastException("I was expecting a StringWriter (because all writers are wrapped in a StringWriter, see PageTemplateWrapperDirective), but I got a " +
                                             writer.getClass().getCanonicalName() + " instead; can't proceed and this should be fixed.");
            }
        }

        return true;
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}
