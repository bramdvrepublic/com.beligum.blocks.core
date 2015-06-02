package com.beligum.blocks.templating.blocks;

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
 * Created by bram on 4/25/15.
 */
@VelocityDirective(TemplateResourcesDirective.NAME)
public class TemplateResourcesDirective extends Directive
{
    //-----CONSTANTS-----
    public static final String NAME = "blocksTagResources";
    private static final String BLOCKS_TEMPLATE_RESOURCES = "BLOCKS_TEMPLATE_RES";

    public enum Argument
    {
        inlineStyles,
        externalStyles,
        styles,
        inlineScripts,
        externalScripts,
        scripts,
        all,
        ;
    }

    //-----VARIABLES-----

    //-----CONSTRUCTORS-----

    //-----PUBLIC METHODS-----
    public static TemplateResources getContextResources(InternalContextAdapter context)
    {
        TemplateResources retVal = (TemplateResources) context.get(TemplateResourcesDirective.BLOCKS_TEMPLATE_RESOURCES);
        if (retVal==null) {
            context.localPut(TemplateResourcesDirective.BLOCKS_TEMPLATE_RESOURCES, retVal = new TemplateResources());
        }

        return retVal;
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

        TemplateResources resources = (TemplateResources) context.get(BLOCKS_TEMPLATE_RESOURCES);
        //if we have nothing to check, let's move on
        if (resources!=null) {
            Argument arg = Argument.all;
            Node argNode = node.jjtGetNumChildren()>0 ? node.jjtGetChild(0) : null;
            if (argNode!=null) {
                arg = Argument.valueOf((String) argNode.value(context));
            }

            switch (arg) {
                case inlineStyles:
                    this.writeResources(resources.getInlineStyles(), writer);
                    break;
                case externalStyles:
                    this.writeResources(resources.getExternalStyles(), writer);
                    break;
                case styles:
                    this.writeResources(resources.getStyles(), writer);
                    break;
                case inlineScripts:
                    this.writeResources(resources.getInlineScripts(), writer);
                    break;
                case externalScripts:
                    this.writeResources(resources.getExternalScripts(), writer);
                    break;
                case scripts:
                    this.writeResources(resources.getScripts(), writer);
                    break;
                default:
                    // default is to write everything out
                    this.writeResources(resources.getStyles(), writer);
                    this.writeResources(resources.getScripts(), writer);
                    break;
            }
        }

        return true;
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----
    private void writeResources(Iterable<TemplateResources.Resource> resources, Writer writer) throws IOException
    {
        for (TemplateResources.Resource res : resources) {
            writer.write(res.getValue());
            writer.write("\n");
        }
    }
}
