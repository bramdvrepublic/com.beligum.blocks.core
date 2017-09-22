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
