//package com.beligum.blocks.core.endpoints;
//
//import com.beligum.blocks.core.models.storables.Page;
//import com.beligum.core.framework.base.R;
//import com.beligum.core.framework.templating.ifaces.Template;
//import org.apache.velocity.VelocityContext;
//import org.apache.velocity.context.Context;
//import org.apache.velocity.runtime.RuntimeServices;
//import org.apache.velocity.runtime.RuntimeSingleton;
//import org.apache.velocity.tools.generic.RenderTool;
//import redis.clients.jedis.Jedis;
//
//import javax.ws.rs.GET;
//import javax.ws.rs.Path;
//import javax.ws.rs.PathParam;
//import javax.ws.rs.core.Response;
//import java.util.Set;
//
///**
//* Created by bas on 13.10.14.
//* Endpoint class for the pages of the default-class
//*/
//@Path("/default")
//public class PageDefaultEndpoint
//{
//    @Path("/{randomPageId}")
//    @GET
//    public Response getPageWithId(@PathParam("randomPageId") String randomURLPath){
//        //TODO BAS: HIER BEGINNEN, implementeren dat je eender welke pagina vanaf hier kunt bereiken http://wwW.mot.be/blocks/eenmooiepagina wordt hier opgehaald in de database (en geparsed), vandaag moet er ook eens goed gedacht worden over internationalisering in de db!!!
//        Template template = R.templateEngine().getEmptyTemplate(Page.getTemplatePath(pageClassName));
//        Context velocityContext = new VelocityContext();
//        //reads can be done from slave
//        Jedis redisClient = new Jedis("localhost", 6380);
//        Set<String> rowAndBlockIds = redisClient.smembers(pageClass);
//        for(String rowOrBlockId : rowAndBlockIds){
//            //rget most recent version (at position 1) from versioning-list stored at 'rowAndBlockId'
//            String UID = redisClient.lindex(rowOrBlockId, 1);
//            String content = redisClient.get(UID);
//            //add the content of the row or block to the velocity-context as a variable for parsing
//            velocityContext.put(rowOrBlockId, content);
//        }
//
//        /*
//         * We can use a velocity runtime service, so we only have to parse the template once an can merge the template multiple times
//         */
//        velocityContext.put("velocityTemplate", velocityTemplate.toString());
//        RuntimeServices runtimeServices = RuntimeSingleton.getRuntimeServices();
//
//
//        /*
//         * Parses the velocity template recursivly and returns a string with all variables in the velocityContext rendered
//         * Note: parse depth is default set to 20
//         * Note 2: renderTools.recurse() stops when encountering numbers, so no id may consist of only a number
//         */
//        RenderTool renderTool = new RenderTool();
//        String output = renderTool.recurse(velocityContext, velocityTemplate.toString());
//        return Response.ok(template.render()).build();
//    }
//
//
//    //TODO BAS: schrijf de ReadMe voor het parse-gedeelte
//
//}
