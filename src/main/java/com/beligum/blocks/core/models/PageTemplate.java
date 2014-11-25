package com.beligum.blocks.core.models;

import com.beligum.blocks.core.config.BlocksConfig;
import com.beligum.blocks.core.models.storables.Entity;
import com.beligum.core.framework.base.R;
import com.beligum.core.framework.templating.ifaces.Template;
import com.beligum.core.framework.templating.ifaces.TemplateEngine;
import com.beligum.core.framework.templating.velocity.VelocityTemplateEngine;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.tools.generic.RenderTool;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.parser.Tag;

import javax.ws.rs.core.Response;
import java.util.List;

/**
 * Created by wouter on 20/11/14.
 */
public class PageTemplate
{
    private String template;
    private String name;
    private boolean htmlSeen = false;


    public PageTemplate(Element node) {
        super();
        Element parent = node.parent();
        while (parent.parent() != null) {
            if (parent.tagName().equals("html")) {
                name = parent.attr("template");
            }
            parent = parent.parent();
        }
        Node e = new TextNode("${" + BlocksConfig.TEMPLATE_ENTITY_VARIABLE + "}", BlocksConfig.getSiteDomain());
        node.replaceWith(e);

        this.template = parent.outerHtml();
    }



    public String getName() {
        return this.name;
    }

    public boolean isTemplate() {
        boolean retVal = false;
        if (this.name != null) retVal = true;
        return retVal;
    }

    public String renderContent(Entity entity) throws Exception
    {
        //        Element filledTemplate = this.tempContent.clone();
        //        List<Element> nodes = filledTemplate.select("div[template-content]");
        //        Element node = nodes.get(0);
        //        node.replaceWith(element);

        /*
         * Use the default template-engine of the application and the default template-context of this page-template for template-rendering
         */
        TemplateEngine templateEngine = R.templateEngine();
        if(templateEngine instanceof VelocityTemplateEngine) {
            /*
             * Add all specific velocity-variables fetched from database to the context.
             */
            VelocityContext context = new VelocityContext();
            for(Entity child : entity.getAllChildren()){
                context.put(child.getTemplateVariableName(), child.getTemplate());
            }

            /*
             * Parse the velocity template recursively using the correct engine and context and return a string with all variables in the velocityContext rendered
             * Note: parse depth is default set to 20
             * Note 2: renderTools.recurse() stops when encountering numbers, so no element's-id may consist of only a number (this should not happen since element-ids are of the form "[db-alias]:///[pagePath]#[elementId]"
             */
            RenderTool renderTool = new RenderTool();
            renderTool.setVelocityEngine(((VelocityTemplateEngine) templateEngine).getDelegateEngine());
            renderTool.setVelocityContext(context);
            String pageHtml = renderTool.recurse(entity.getTemplate());
            return pageHtml;
        }
        else{
            throw new Exception("The only template engine supported is Velocity. No other template-structure can be used for now, so for now you cannot use '" + templateEngine.getClass().getName() + "'");
        }
    }



}
