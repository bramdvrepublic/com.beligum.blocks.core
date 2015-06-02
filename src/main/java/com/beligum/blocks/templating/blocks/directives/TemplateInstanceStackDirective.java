package com.beligum.blocks.templating.blocks.directives;

import com.beligum.base.templating.velocity.directives.VelocityDirective;
import com.beligum.blocks.templating.blocks.TemplateContextMap;
import com.beligum.blocks.templating.blocks.TemplateController;
import com.beligum.blocks.templating.blocks.TemplateStackFrame;
import org.apache.velocity.context.InternalContextAdapter;
import org.apache.velocity.runtime.directive.Block;
import org.apache.velocity.runtime.parser.node.ASTStringLiteral;
import org.apache.velocity.runtime.parser.node.Node;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Created by bram on 5/28/15.
 */
@VelocityDirective(TemplateInstanceStackDirective.NAME)
public class TemplateInstanceStackDirective extends Block
{
    //-----CONSTANTS-----
    //blocksTemplateInstanceDirective
    public static final String NAME = "btid";

    public enum Action {
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
        if (stack==null) {
            context.put(TemplateContextMap.TEMPLATE_STACK_VARIABLE, stack = new ArrayDeque<>());
        }

        Action action = Action.values()[(Integer)TagTemplateDirectiveUtils.readArg(context, node, 0)];
        switch (action) {
            case STACK:
                TemplateStackFrame frame = new TemplateStackFrame();

                String controllerName = (String) TagTemplateDirectiveUtils.readArg(context, node, 1);
                if (controllerName != null) {
                    TemplateController controller = TemplateContextMap.getTemplateControllers().get(controllerName);
                    if (controller!=null) {
                        //controller instances live for the duration of the entire request; make sure to reset them first
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

                        frame.setController(controller);
                    }
                }

                stack.push(frame);

                //link the frame variables the context variables
                context.put(TemplateContextMap.TAG_TEMPLATE_CONTROLLER_VARIABLE, frame.getController());
                context.put(TemplateContextMap.TAG_TEMPLATE_PROPERTIES_VARIABLE, frame.getProperties());

                super.render(context, writer);

                stack.pop();

                if (!stack.isEmpty()) {
                    context.put(TemplateContextMap.TAG_TEMPLATE_CONTROLLER_VARIABLE, stack.peek().getController());
                    context.put(TemplateContextMap.TAG_TEMPLATE_PROPERTIES_VARIABLE, stack.peek().getProperties());
                }
                else {
                    context.remove(TemplateContextMap.TAG_TEMPLATE_CONTROLLER_VARIABLE);
                    context.remove(TemplateContextMap.TAG_TEMPLATE_PROPERTIES_VARIABLE);
                }

                break;

            case DEFINE:

                if (!stack.isEmpty()) {
                    try (StringWriter dummyWriter = new StringWriter()) {
                        super.render(context, dummyWriter);
                        String variable = (String) TagTemplateDirectiveUtils.readArg(context, node, 1);
                        stack.peek().getProperties().put(variable, dummyWriter.toString());
                    }
                }
                else {
                    throw new IOException("Can't use a define outside of a stack frame, this shouldn't happen; template: "+node.getTemplateName());
                }

                break;

            default:
                throw new IOException("Encountered unknown/unimplemented action; "+action);
        }

        return true;
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}
