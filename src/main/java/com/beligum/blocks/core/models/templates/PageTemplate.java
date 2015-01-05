package com.beligum.blocks.core.models.templates;

import com.beligum.blocks.core.caching.EntityTemplateClassCache;
import com.beligum.blocks.core.config.DatabaseConstants;
import com.beligum.blocks.core.exceptions.CacheException;
import com.beligum.blocks.core.exceptions.DeserializationException;
import com.beligum.blocks.core.exceptions.IDException;
import com.beligum.blocks.core.exceptions.ParseException;
import com.beligum.blocks.core.identifiers.RedisID;
import com.beligum.blocks.core.parsers.TemplateParser;
import com.beligum.core.framework.utils.Logger;

import java.net.URISyntaxException;
import java.util.Map;

/**
 * Created by wouter on 20/11/14.
 */
public class PageTemplate extends AbstractTemplate
{
    private String name;

    public PageTemplate(String name, Map<String, String> templates) throws IDException
    {
        super(RedisID.renderNewPageTemplateID(name), templates);
        this.name = name;
        //TODO: should the creator of a page-template be the <author>-tag of the html file?, or else "server-start" or something?
    }

    private PageTemplate(RedisID id, Map<String, String> templates){
        super(id, templates);
    }

    public String getName() {
        return name;
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

    /**
     * The PageTemplate-class can be used as a factory, to construct page-templates from data found in a hash in the redis-db
     * @param hash a map, mapping field-names to field-values
     * @return an page-template or null if no page-template could be constructed from the specified hash
     */
    public static PageTemplate createInstanceFromHash(RedisID id, Map<String, String> hash) throws DeserializationException
    {
        try{
            if(hash != null && !hash.isEmpty() && hash.containsKey(DatabaseConstants.TEMPLATE)) {
                PageTemplate newInstance = new PageTemplate(id, hash.get(DatabaseConstants.TEMPLATE));
                newInstance.applicationVersion = hash.get(DatabaseConstants.APP_VERSION);
                newInstance.creator = hash.get(DatabaseConstants.CREATOR);
                String[] splitted = id.getUnversionedId().split("/");
                newInstance.name = splitted[splitted.length-1];
                return newInstance;
            }
            else{
                Logger.error("Could not construct a page-template from the specified hash: " + hash);
                throw new DeserializationException("Could not construct a page-template from the specified hash: " + hash);
            }
        }
        catch(DeserializationException e){
            throw e;
        }
        catch(Exception e){
            throw new DeserializationException("Could not construct an object of class '" + PageTemplate.class.getName() + "' from specified hash.", e);
        }
    }

    //________________OVERRIDE OF OBJECT_______________//

    /**
     * Two templates have the same hashCode when their template-content, url and meta-data are equal.
     * (thus equal through object-state, not object-address)
     * @return
     */
    @Override
    public int hashCode()
    {
        return super.hashCode();
    }

    /**
     * Two templates are equal when their template-content, url and meta-data are equal
     * (thus equal through object-state, not object-address).
     * @param obj
     * @return true if two templates are equal, false otherwise
     */
    @Override
    public boolean equals(Object obj)
    {
        if(obj instanceof PageTemplate) {
            return super.equals(obj);
        }
        else{
            return false;
        }
    }
}
