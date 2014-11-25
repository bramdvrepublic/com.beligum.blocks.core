package com.beligum.blocks.core.endpoints;

import com.beligum.blocks.core.dbs.Redis;
import com.beligum.blocks.core.identifiers.RedisID;
import com.beligum.blocks.core.models.storables.Entity;
import com.beligum.core.framework.base.R;
import com.beligum.core.framework.base.RequestContext;
import com.beligum.core.framework.templating.ifaces.Template;
import com.beligum.core.framework.templating.ifaces.TemplateEngine;
import com.beligum.core.framework.templating.velocity.VelocityTemplateEngine;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.tools.generic.RenderTool;
import org.jsoup.nodes.Element;

import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;
import java.net.URL;

@Path("/")
public class ApplicationEndpoint
{
    @Path("/index")
    @GET
    public Response index()
    {
        Template indexTemplate = R.templateEngine().getEmptyTemplate("/views/index.html");
//        TypeCacher.instance().reset();
        return Response.ok(indexTemplate).build();
    }

//    @Path("/show")
//    @GET
//    public Response show()
//    {
//        TypeCacher.instance().reset();
//
//        com.beligum.blocks.html.Template template = TypeCacher.instance().getTemplate("default");
//        Element element = TypeCacher.instance().getContent("free");
//
//        return Response.ok(template.renderContent(element)).build();
//    }

    @Path("/reset")
    @GET
    public Response reset()
    {
//        TypeCacher.instance().reset();
        // TODO enqble reset of EntityClassCache
        return Response.ok("OK: all templates loaded").build();
    }

    //using regular expression to let all requests to undefined paths end up here
    @Path("/{randomPage:.+}")
    @GET
    public Response getPageWithId(@PathParam("randomPage") String randomURLPath)
    {
        try{
            Redis redis = Redis.getInstance();
            URL url = new URL(RequestContext.getRequest().getRequestURL().toString());
            RedisID id = new RedisID(url);
            Entity entity = redis.fetchEntity(id, true, true);

            /*
             * Use the default template-engine of the application and the default template-context of this page-class for template-rendering
             */
            TemplateEngine templateEngine = R.templateEngine();
            //TODO BAS SH: put this in PageTemplate: this cast should be avoided here, we need a more generic 'RenderTool' where this cast should be done properly
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
                return Response.ok(pageHtml).build();
            }
            else{
                throw new Exception("The only template engine supported is Velocity. No other template-structure can be used for now, so for now you cannot use '" + templateEngine.getClass().getName() + "'");
            }
        }
        catch(Exception e){
            throw new NotFoundException("The page '" + randomURLPath + "' could not be found.", e);
        }
    }

}