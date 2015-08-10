package com.beligum.blocks.templating.blocks.directives;

import com.beligum.base.templating.velocity.directives.VelocityDirective;
import com.beligum.base.utils.Logger;
import org.apache.velocity.context.InternalContextAdapter;
import org.apache.velocity.runtime.directive.Block;
import org.apache.velocity.runtime.directive.Define;
import org.apache.velocity.runtime.parser.node.Node;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;

/**
 * Created by bram on 4/25/15.
 */
@VelocityDirective(OverloadedDefineDirective.NAME)
public class OverloadedDefineDirective extends Define
{
    //-----CONSTANTS-----
    public static final String NAME = "define";

    //-----VARIABLES-----

    //-----CONSTRUCTORS-----

    //-----PUBLIC METHODS-----
    @Override
    public String getName()
    {
        return NAME;
    }
    @Override
    public boolean render(InternalContextAdapter context, Writer writer, Node node)
    {
        boolean retVal = false;

        // contrary to the superclass implementation, this renders (and caches) the result immediately,
        // because we need to analyze the children of the #define right here, right now;
        // On top of putting the content block of the #define directive in the context,
        // it renders all content now to make sure the styles and scripts down below are in the context,
        // so they can be found by TemplateResourcesDirective.render(), even if they occur after that tag.
        try {
            context.put(key, new ImmediateReference(context, this));
            retVal = true;
        }
        catch (IOException e) {
            Logger.error("Error while rendering the overloaded #define directive", e);
        }

        return retVal;
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

    //-----PRIVATE CLASSES-----
    public static class ImmediateReference extends Reference
    {
        private String cachedRender = null;

        public ImmediateReference(InternalContextAdapter context, Block parent) throws IOException
        {
            super(context, parent);

            this.cachedRender = this.doRender(context);
        }

        public String doRender(InternalContextAdapter context) throws IOException
        {
            String retVal = "";

            try (Writer writer = new StringWriter()) {
                if (super.render(context, writer)) {
                    retVal = writer.toString();
                }
            }

            return retVal;
        }

        @Override
        public boolean render(InternalContextAdapter context, Writer writer)
        {
            boolean retVal = false;

            try {
                writer.write(this.cachedRender);
                retVal = true;
            }
            catch (IOException e) {
                Logger.error("Error while rendering overloaded resource", e);
            }

            return retVal;
        }

        @Override
        public String toString()
        {
            return this.cachedRender;
        }
    }
}
