package com.beligum.blocks.templating.blocks;

import com.beligum.base.templating.velocity.directives.VelocityDirective;
import com.beligum.base.utils.Logger;
import org.apache.commons.io.output.NullWriter;
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
    private Writer dummyWriter = new NullWriter();

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
        // contrary to the superclass implementation, this renders (and caches) the result immediately,
        // because we need to analyze the children of thie #define right here, right now;
        // On top of putting the content block of the #define directive in the context,
        // it renders all content now to make sure the styles and scripts down below are in the context,
        // so they can be found by TagTemplateResourcesDirective.render(), even if they occur after that tag.
        context.put(key, new ImmediateReference(context, this));

        return true;
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

    //-----PRIVATE CLASSES-----
    public static class ImmediateReference extends Reference
    {
        private String cachedRender = null;

        public ImmediateReference(InternalContextAdapter context, Block parent)
        {
            super(context, parent);

            this.cachedRender = this.doRender(context);
        }

        public String doRender(InternalContextAdapter context)
        {
            String retVal = "";

            Writer writer = new StringWriter();
            if (super.render(context, writer)) {
                retVal = writer.toString();
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
