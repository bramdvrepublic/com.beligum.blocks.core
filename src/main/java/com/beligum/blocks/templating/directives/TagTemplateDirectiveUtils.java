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

package com.beligum.blocks.templating.directives;

import com.beligum.base.utils.Logger;
import org.apache.velocity.context.InternalContextAdapter;
import org.apache.velocity.runtime.parser.node.Node;

import java.io.IOException;
import java.io.StringWriter;

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

        Node argNode = node.jjtGetNumChildren() > index ? node.jjtGetChild(index) : null;
        if (argNode != null) {
            retVal = argNode.value(context);
        }

        return retVal;
    }
    protected static String renderValue(InternalContextAdapter context, Node node) throws IOException
    {
        String retVal = null;

        Node contentNode = node.jjtGetChild(node.jjtGetNumChildren() - 1);
        if (contentNode.jjtGetNumChildren() > 0) {

            // When upgrading to Velocity 2.0, this seemed to throw a NPE,
            // so we replaced it with the code below, I hope it basically does the same...
            // contentNode.literal();

            StringWriter blockContent = new StringWriter();
            contentNode.render(context, blockContent);
            retVal = blockContent.toString();
        }

        return retVal;
    }

    //-----PRIVATE METHODS-----

}
