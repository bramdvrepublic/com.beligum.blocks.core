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

import com.beligum.base.cache.CacheFunction;
import com.beligum.base.resources.MimeTypes;
import com.beligum.base.resources.ifaces.MimeType;
import com.beligum.base.server.R;
import com.beligum.base.templating.velocity.directives.VelocityDirective;
import com.beligum.blocks.templating.TemplateResources;
import org.apache.velocity.context.InternalContextAdapter;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.runtime.directive.Directive;
import org.apache.velocity.runtime.parser.node.Node;
import org.apache.velocity.util.StringUtils;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by bram on 4/25/15.
 */
@VelocityDirective(TemplateResourcesDirective.NAME)
public class TemplateResourcesDirective extends Directive
{
    //-----CONSTANTS-----
    public static final String NAME = "blocksTagResources";

    public static final String RESOURCES_INSERTS = "RESOURCES_INSERTS";

    //private static final String BLOCKS_TEMPLATE_RESOURCES = "BLOCKS_TEMPLATE_RES";
    enum CacheKey implements com.beligum.base.cache.CacheKey
    {
        BLOCKS_TEMPLATE_RES
    }

    public enum Argument
    {
        inlineStyles(MimeTypes.CSS, true, false, true),
        externalStyles(MimeTypes.CSS, true, false, false),
        styles(MimeTypes.CSS, true, false, false),
        inlineScripts(MimeTypes.JAVASCRIPT, false, true, true),
        externalScripts(MimeTypes.JAVASCRIPT, false, true, false),
        scripts(MimeTypes.JAVASCRIPT, false, true, false),
        all(null, false, false, false);

        private MimeType matchingMimeType;
        private boolean inline;
        private boolean script;
        private boolean style;
        Argument(MimeType matchingMimeType, boolean style, boolean script, boolean inline)
        {
            this.matchingMimeType = matchingMimeType;
            this.style = style;
            this.script = script;
            this.inline = inline;
        }
        public MimeType getMatchingMimeType()
        {
            return matchingMimeType;
        }
        public boolean isStyle()
        {
            return style;
        }
        public boolean isScript()
        {
            return script;
        }
        public boolean isInline()
        {
            return inline;
        }
    }

    //-----VARIABLES-----

    //-----CONSTRUCTORS-----

    //-----PUBLIC METHODS-----
    //Note: storing this in the context meant trouble (multiple factory calls during one request?)
    public static TemplateResources getContextResources(InternalContextAdapter context) throws IOException
    {
        return R.requestManager().getCurrentRequest().getRequestCache().getAndPutIfAbsent(CacheKey.BLOCKS_TEMPLATE_RES, new CacheFunction<com.beligum.base.cache.CacheKey, TemplateResources>()
        {
            @Override
            public TemplateResources apply(com.beligum.base.cache.CacheKey cacheKey) throws IOException
            {
                return new TemplateResources();
            }
        });
    }
    @Override
    public String getName()
    {
        return NAME;
    }
    @Override
    public int getType()
    {
        return LINE;
    }
    @Override
    public boolean render(InternalContextAdapter context, Writer writer, Node node) throws IOException, ResourceNotFoundException, ParseErrorException, MethodInvocationException
    {
        // Note: we stopped doing this. Instead, the #define directive is overloaded by OverloadedDefineDirective and adapted so it renders out it's content
        // to a dummy writer, to achieve the same thing as in the comment block below, but a lot faster when using template tags in loops
        //        Object[] contextKeys = context.getKeys();
        //        for (Object key : contextKeys) {
        //            Object value = context.get((String) key);
        //            if (value instanceof Block.Reference) {
        //                // this is a bit of a hack: it forces the references (actually the values of the #define, that are stored in the context)
        //                // to be rendered here instead of later on, when it's being used for the first time, making sure the right render-order is simulated.
        //                // (see comments of the Block and Block.Reference classes for details)
        //                // Note that by rendering this node out, the styles and scripts of the tagTemplates will
        //                // be put into the context, so they can be used below
        //                Block.Reference ref = (Block.Reference)value;
        //                ref.render(context, this.dummyWriter);
        //            }
        //        }

        TemplateResources resources = getContextResources(context);

        //if we have nothing to check, let's move on
        if (resources != null) {
            Argument arg = Argument.all;
            Node argNode = node.jjtGetNumChildren() > 0 ? node.jjtGetChild(0) : null;
            if (argNode != null) {
                //this validates the value; real switch() is in PageTemplateWrapperDirective.class
                arg = Argument.valueOf((String) argNode.value(context));
            }

            /**
             * Instead of writing the resource tags to the writer, we save the position of the buffer to insert them at this position later on.
             * This is because I couldn't find a way to run through the entire Velocity file, saving all resource references and then building the uniques list
             * any other way.
             */
            List<WriterBufferReference> inserts = (List<WriterBufferReference>) context.get(RESOURCES_INSERTS);
            if (inserts == null) {
                context.put(RESOURCES_INSERTS, inserts = new ArrayList<>());
            }

            if (writer instanceof StringWriter) {
                inserts.add(new WriterBufferReference(arg, (StringWriter) writer));
            }
            else {
                throw new IOException("Encountered a writer while processing #" + NAME + " directive that's not a StringWriter; this shouldn't happen and should be fixed in " +
                                      PageTemplateWrapperDirective.class.getCanonicalName() + "; template file was " +
                                      StringUtils.formatFileString(this));
            }
        }

        return true;
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

    //-----PRIVATE CLASSES-----

}
