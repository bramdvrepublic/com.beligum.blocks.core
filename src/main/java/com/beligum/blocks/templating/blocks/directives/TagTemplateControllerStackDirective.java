package com.beligum.blocks.templating.blocks.directives;

import com.beligum.base.templating.velocity.directives.VelocityDirective;
import com.beligum.blocks.templating.blocks.TagTemplateContextMap;
import com.beligum.blocks.templating.blocks.TagTemplateController;
import org.apache.velocity.context.InternalContextAdapter;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.runtime.parser.node.Node;

import java.io.IOException;
import java.io.Writer;
import java.util.Deque;
import java.util.LinkedList;

/**
 * Created by bram on 5/28/15.
 */
@VelocityDirective(TagTemplateControllerStackDirective.NAME)
public class TagTemplateControllerStackDirective extends AbstractTagTemplateDirective
{
    //-----CONSTANTS-----
    //blocksTemplateControllerStack
    public static final String NAME = "btcs";

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
        Deque<TagTemplateController> controllerStack = (Deque<TagTemplateController>) context.get(TagTemplateContextMap.TAG_TEMPLATE_CONTROLLER_STACK_VARIABLE);
        if (controllerStack==null) {
            //note: a linked list is the only deque that allows null values
            context.put(TagTemplateContextMap.TAG_TEMPLATE_CONTROLLER_STACK_VARIABLE, controllerStack = new LinkedList<TagTemplateController>());
        }

        boolean push = (boolean) AbstractTagTemplateDirective.readArg(context, node, 0);
        if (push) {
            TagTemplateController controller = (TagTemplateController) AbstractTagTemplateDirective.readArg(context, node, 1);
            controller.resetConfig();
            if (node.jjtGetNumChildren()>2) {
                for (int i=2;i<node.jjtGetNumChildren();i+=2) {
                    String key = (String) node.jjtGetChild(i).value(context);
                    String value = (String) node.jjtGetChild(i+1).value(context);
                    controller.putConfig(key, value);
                }
            }

            controllerStack.push(controller);
            context.put(TagTemplateContextMap.TAG_TEMPLATE_CONTROLLER_VARIABLE, controller);
            //callback the controller and let know we're all set now
            controller.created();
        }
        else {
            controllerStack.pop();
            if (!controllerStack.isEmpty()) {
                context.put(TagTemplateContextMap.TAG_TEMPLATE_CONTROLLER_VARIABLE, controllerStack.peek());
            }
            else {
                context.remove(TagTemplateContextMap.TAG_TEMPLATE_CONTROLLER_VARIABLE);
            }
        }

        return true;
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}
