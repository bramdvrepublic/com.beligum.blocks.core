package com.beligum.blocks.core.models;

import com.beligum.blocks.core.config.ParserConstants;
import com.beligum.blocks.core.exceptions.ParserException;
import com.beligum.blocks.core.identifiers.ID;
import com.beligum.blocks.core.models.storables.Entity;
import com.beligum.blocks.core.parsers.AbstractParser;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

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

    public String getTemplate()
    {
        return template;
    }
    public boolean isTemplate() {
        return this.getName() != null && !this.getName().isEmpty();
    }

//    public String renderContent(Entity entity) throws Exception
//    {
//        /*
//         * Use the default template-engine of the application and the default template-context of this page-template for template-rendering
//         */
//        TemplateEngine templateEngine = R.templateEngine();
//        if(templateEngine instanceof VelocityTemplateEngine) {
//            /*
//             * Add all specific velocity-variables fetched from database to the context.
//             */
//            VelocityContext context = new VelocityContext();
//            context.put(ParserConstants.PAGE_TEMPLATE_ENTITY_VARIABLE_NAME, entity.getTemplate());
//            for(Entity child : entity.getChildren()){
//                context.put(child.getTemplateVariableName(), child.getTemplate());
//            }
//
//            /*
//             * Parse the velocity template recursively using the correct engine and context and return a string with all variables in the velocityContext rendered
//             * Note: parse depth is default set to 20
//             * Note 2: renderTools.recurse() stops when encountering numbers, so no element's-id may consist of only a number (this should not happen since element-ids are of the form "[db-alias]:///[pagePath]#[elementId]"
//             */
//            RenderTool renderTool = new RenderTool();
//            renderTool.setVelocityEngine(((VelocityTemplateEngine) templateEngine).getDelegateEngine());
//            renderTool.setVelocityContext(context);
//            String pageHtml = renderTool.recurse(this.template);
//            return pageHtml;
//        }
//        else{
//            throw new Exception("The only template engine supported is Velocity. No other template-structure can be used for now, so for now you cannot use '" + templateEngine.getClass().getName() + "'");
//        }
//    }

    public String renderContent(Entity entity) throws ParserException
    {
        return AbstractParser.renderEntitiesInsidePageTemplate(this, entity);
    }

    /**
     * Returns a string representation of the object. In general, the
     * {@code toString} method returns a string that
     * "textually represents" this object. The result should
     * be a concise but informative representation that is easy for a
     * person to read.
     * It is recommended that all subclasses override this method.
     * <p/>
     * The {@code toString} method for class {@code Object}
     * returns a string consisting of the name of the class of which the
     * object is an instance, the at-sign character `{@code @}', and
     * the unsigned hexadecimal representation of the hash code of the
     * object. In other words, this method returns a string equal to the
     * value of:
     * <blockquote>
     * <pre>
     * getClass().getName() + '@' + Integer.toHexString(hashCode())
     * </pre></blockquote>
     *
     * @return a string representation of the object.
     */
    @Override
    public String toString()
    {
        return this.getTemplate();
    }
}
