package com.beligum.blocks.templating.blocks.directives;

import com.beligum.base.templating.velocity.directives.VelocityDirective;
import com.beligum.base.utils.Logger;
import com.beligum.blocks.templating.blocks.*;
import org.apache.velocity.context.InternalContextAdapter;
import org.apache.velocity.runtime.directive.Block;
import org.apache.velocity.runtime.parser.node.ASTStringLiteral;
import org.apache.velocity.runtime.parser.node.Node;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;

/**
 * Created by bram on 5/28/15.
 */
@VelocityDirective(TemplateInstanceStackDirective.NAME)
public class TemplateInstanceStackDirective extends Block
{
    //-----CONSTANTS-----
    //blocksTemplateInstanceDirective
    public static final String NAME = "btid";

    public enum Action
    {
        STACK,
        DEFINE
    }

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
    public boolean render(InternalContextAdapter context, Writer writer, Node node) throws IOException
    {
        //init the stack if it's not there yet
        Deque<TemplateStackFrame> stack = (Deque<TemplateStackFrame>) context.get(TemplateContextMap.TEMPLATE_STACK_VARIABLE);
        if (stack == null) {
            context.put(TemplateContextMap.TEMPLATE_STACK_VARIABLE, stack = new ArrayDeque<>());
        }

        Action action = Action.values()[(Integer) TagTemplateDirectiveUtils.readArg(context, node, 0)];
        switch (action) {
            case STACK:
                String templateName = (String) TagTemplateDirectiveUtils.readArg(context, node, 1);

                TemplateController controller = TemplateContextMap.getTemplateControllers().get(templateName);
                if (controller != null) {
                    //controller instances live for the duration of the entire request (per template name); make sure to reset them first
                    controller.resetConfig();
                    if (node.jjtGetNumChildren() > 2) {
                        for (int i = 2; i < node.jjtGetNumChildren(); i += 2) {
                            //we're in a block, so stop as soon as we reach the content of the block (not the arguments)
                            if (!(node.jjtGetChild(i) instanceof ASTStringLiteral)) {
                                break;
                            }
                            String key = (String) node.jjtGetChild(i).value(context);
                            String value = (String) node.jjtGetChild(i + 1).value(context);
                            controller.putConfig(key, value);
                        }
                    }

                    //callback the controller and let it know we're all set now
                    controller.created();
                }

                TemplateStackFrame frame = new TemplateStackFrame(TemplateCache.instance().getByTagName(templateName), controller, stack.size());

                stack.push(frame);

                //link the frame variables to context variables
                context.put(TemplateContextMap.TAG_TEMPLATE_TEMPLATE_VARIABLE, frame.getTemplate());
                context.put(TemplateContextMap.TAG_TEMPLATE_CONTROLLER_VARIABLE, frame.getController());
                context.put(TemplateContextMap.TAG_TEMPLATE_PROPERTIES_VARIABLE, frame.getProperties());

                super.render(context, writer);

                stack.pop();

                if (!stack.isEmpty()) {
                    context.put(TemplateContextMap.TAG_TEMPLATE_TEMPLATE_VARIABLE, stack.peek().getTemplate());
                    context.put(TemplateContextMap.TAG_TEMPLATE_CONTROLLER_VARIABLE, stack.peek().getController());
                    context.put(TemplateContextMap.TAG_TEMPLATE_PROPERTIES_VARIABLE, stack.peek().getProperties());
                }
                else {
                    context.remove(TemplateContextMap.TAG_TEMPLATE_TEMPLATE_VARIABLE);
                    context.remove(TemplateContextMap.TAG_TEMPLATE_CONTROLLER_VARIABLE);
                    context.remove(TemplateContextMap.TAG_TEMPLATE_PROPERTIES_VARIABLE);
                }

                break;

            case DEFINE:

                if (!stack.isEmpty()) {
                    String variable = (String) TagTemplateDirectiveUtils.readArg(context, node, 1);
                    Map<String, Object> properties = stack.peek().getProperties();
                    //this allows us to assign multiple tags to a single property key
                    // by only converting to a list when multiple mappings are present, we allow for natural coding in VTL syntax
                    // and also save the fact "there's only one"
                    PropertyArray propertyValues = (PropertyArray) properties.get(variable);
                    if (propertyValues == null) {
                        //specially crafted ArrayList with a modified toString()
                        properties.put(variable, propertyValues = new PropertyArray());
                    }

                    // this is like the Reference from the #define directive (that holds the value and context, to spit out later on, when needed, during toString())
                    propertyValues.add(new PropertyReference(context, this));
                }
                else {
                    throw new IOException("Can't use a define outside of a stack frame, this shouldn't happen; template: " + node.getTemplateName());
                }

                break;

            default:
                throw new IOException("Encountered unknown/unimplemented action; " + action);
        }

        return true;
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

    public static class PropertyReference
    {
        private InternalContextAdapter context;
        private Block parent;

        public PropertyReference(InternalContextAdapter context, Block parent)
        {
            this.context = context;
            this.parent = parent;
        }

        public String toString()
        {
            try (Writer writer = new StringWriter()) {
                if (parent.render(context, writer)) {
                    return writer.toString();
                }
            }
            catch (IOException e) {
                Logger.error("Error while writing out a property value", e);
            }

            return null;
        }
    }
}
