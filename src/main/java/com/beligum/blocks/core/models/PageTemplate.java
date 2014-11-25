package com.beligum.blocks.core.models;

import com.beligum.blocks.core.config.VelocityVariables;
import com.beligum.blocks.core.identifiers.ID;
import com.beligum.blocks.core.models.storables.Entity;
import com.beligum.core.framework.base.R;
import com.beligum.core.framework.templating.ifaces.TemplateEngine;
import com.beligum.core.framework.templating.velocity.VelocityTemplateEngine;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.tools.generic.RenderTool;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Created by wouter on 20/11/14.
 */
public class PageTemplate extends IdentifiableObject
{

    private String template;
    private boolean htmlSeen = false;


    public PageTemplate(String name, String template) throws URISyntaxException
    {
        super(new ID(new URI(name)));
        this.template = template;
    }

    public String getName() {
        if(this.id != null){
            return this.id.toString();
        }
        else{
            return null;
        }
    }

    public boolean isTemplate() {
        return this.getName() != null && !this.getName().isEmpty();
    }

    public String renderContent(Entity entity) throws Exception
    {
        /*
         * Use the default template-engine of the application and the default template-context of this page-template for template-rendering
         */
        TemplateEngine templateEngine = R.templateEngine();
        if(templateEngine instanceof VelocityTemplateEngine) {
            /*
             * Add all specific velocity-variables fetched from database to the context.
             */
            VelocityContext context = new VelocityContext();
            context.put(VelocityVariables.ENTITY_VARIABLE_NAME, entity.getTemplate());
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
