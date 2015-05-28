package com.beligum.blocks.templating.blocks.directives;

import com.beligum.base.templating.velocity.directives.VelocityDirective;
import com.beligum.blocks.templating.blocks.TagTemplateContextMap;
import org.apache.velocity.context.InternalContextAdapter;
import org.apache.velocity.runtime.directive.Define;
import org.apache.velocity.runtime.parser.node.Node;

import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;

/**
 * Created by bram on 5/28/15.
 */
@VelocityDirective(TagTemplateDefinePropertyDirective.NAME)
public class TagTemplateDefinePropertyDirective extends Define
{
    //-----CONSTANTS-----
    //blocksTemplateDefineProperty
    public static final String NAME = "btdp";

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
    public boolean render(InternalContextAdapter context, Writer writer, Node node)
    {
        // This class started out as the OverloadedDefineDirective class, see that too...
        // contrary to the superclass implementation, this renders (and caches) the result immediately,
        // because we need to analyze the children of the #define right here, right now;
        // On top of putting the content block of the #define directive in the context,
        // it renders all content now to make sure the styles and scripts down below are in the context,
        // so they can be found by TagTemplateResourcesDirective.render(), even if they occur after that tag.
        // Along the way, we hack the 'save to context' method to save the value to our special hashmap (so that the syntax reflects the html syntax)
        Writer sw = new StringWriter();
        if (super.render(context, sw)) {
            HashMap<String, Object> propertyMap = (HashMap<String, Object>) context.get(TagTemplateContextMap.TAG_TEMPLATE_PROPERTIES_VARIABLE);
            propertyMap.put(this.key, sw.toString());
        }

        return true;
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}
