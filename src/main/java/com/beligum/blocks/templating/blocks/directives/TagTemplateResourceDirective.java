/*
 * Copyright 2017 Republic of Reinvention bvba. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.beligum.blocks.templating.blocks.directives;

import com.beligum.base.security.PermissionRole;
import com.beligum.base.server.R;
import com.beligum.base.templating.velocity.directives.VelocityDirective;
import com.beligum.blocks.templating.blocks.HtmlTemplate;
import com.beligum.blocks.templating.blocks.TemplateResources;
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
        boolean enableDynamicFingerprinting = (boolean) TagTemplateDirectiveUtils.readArg(context, node, 3);
        PermissionRole roleScope = R.configuration().getSecurityConfig().getRole((String) TagTemplateDirectiveUtils.readArg(context, node, 4));
        HtmlTemplate.ResourceScopeMode mode = HtmlTemplate.ResourceScopeMode.values()[(int) TagTemplateDirectiveUtils.readArg(context, node, 5)];
        HtmlTemplate.ResourceJoinHint joinHint = HtmlTemplate.ResourceJoinHint.values()[(int) TagTemplateDirectiveUtils.readArg(context, node, 6)];

        if (HtmlTemplate.testResourceRoleScope(roleScope) && HtmlTemplate.testResourceModeScope(mode)) {
            if (writer instanceof StringWriter) {
                boolean added = false;
                String element = TagTemplateDirectiveUtils.readValue(context, node);

                TemplateResources contextResources = TemplateResourcesDirective.getContextResources(context);
                switch (type) {
                    case inlineStyles:
                        added = contextResources.addInlineStyle(element, (StringWriter) writer, print, joinHint, enableDynamicFingerprinting);
                        break;
                    case externalStyles:
                        added = contextResources.addExternalStyle(element, (StringWriter) writer, urlArgument, print, joinHint, enableDynamicFingerprinting);
                        break;
                    case inlineScripts:
                        added = contextResources.addInlineScript(element, (StringWriter) writer, print, joinHint, enableDynamicFingerprinting);
                        break;
                    case externalScripts:
                        added = contextResources.addExternalScript(element, (StringWriter) writer, urlArgument, print, joinHint, enableDynamicFingerprinting);
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
