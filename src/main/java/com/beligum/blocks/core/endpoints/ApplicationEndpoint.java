package com.beligum.blocks.core.endpoints;

import com.beligum.blocks.core.dbs.Redis;
import com.beligum.blocks.core.models.storables.Entity;
import com.beligum.blocks.core.models.storables.Row;
import com.beligum.core.framework.base.R;
import com.beligum.core.framework.base.RequestContext;
import com.beligum.core.framework.templating.ifaces.Template;
import com.beligum.core.framework.templating.ifaces.TemplateEngine;
import com.beligum.core.framework.templating.velocity.VelocityTemplateEngine;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.tools.generic.RenderTool;

import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;
import java.net.URL;

@Path("/")
public class ApplicationEndpoint
{
    @Path("/")
    @GET
    public Response index()
    {
        Template indexTemplate = R.templateEngine().getEmptyTemplate("/views/index.html");
        return Response.ok(indexTemplate).build();
    }

    //using regular expression to let all requests to undefined paths end up here
    @Path("/{randomPage:.+}")
    @GET
    public Response getPageWithId(@PathParam("randomPage") String randomURLPath)
    {
        try{
            Redis redis = Redis.getInstance();
            URL url = new URL(RequestContext.getRequest().getRequestURL().toString());
            Entity page = redis.fetchPage(url);

            /*
             * Use the default template-engine of the application and the default template-context of this page-class for template-rendering
             */
            TemplateEngine templateEngine = R.templateEngine();
            //TODO: this cast should be avoided here, we need a more generic 'RenderTool' where this cast should be done properly
            if(templateEngine instanceof VelocityTemplateEngine) {
                /*
                 * Add all specific velocity-variables fetched from database to the context.
                 */
                VelocityContext context = new VelocityContext();
                for(Row element : page.getAllElements()){
                    context.put(element.getTemplateVariableName(), element.getTemplate());
                }

                /*
                 * Parse the velocity template recursively using the correct engine and context and return a string with all variables in the velocityContext rendered
                 * Note: parse depth is default set to 20
                 * Note 2: renderTools.recurse() stops when encountering numbers, so no element's-id may consist of only a number (this should not happen since element-ids are of the form "[db-alias]:///[pagePath]#[elementId]"
                 */
                RenderTool renderTool = new RenderTool();
                renderTool.setVelocityEngine(((VelocityTemplateEngine) templateEngine).getDelegateEngine());
                renderTool.setVelocityContext(context);
                String pageHtml = renderTool.recurse(page.getTemplate());
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


    //TODO BAS: schrijf de ReadMe voor het parse-gedeelte

}