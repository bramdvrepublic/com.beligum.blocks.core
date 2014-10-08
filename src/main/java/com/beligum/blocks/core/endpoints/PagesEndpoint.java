package com.beligum.blocks.core.endpoints;

import com.beligum.blocks.core.caching.PageCache;
import com.beligum.blocks.core.models.AbstractIdentifiableElement;
import com.beligum.blocks.core.models.Page;
import com.beligum.blocks.core.parsing.ElementParser;
import com.beligum.blocks.core.parsing.ElementParserException;
import com.beligum.core.framework.base.R;
import com.beligum.core.framework.templating.ifaces.Template;
import org.apache.commons.io.FileUtils;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.context.Context;
import org.apache.velocity.runtime.RuntimeServices;
import org.apache.velocity.runtime.RuntimeSingleton;
import org.apache.velocity.runtime.parser.ParseException;
import org.apache.velocity.tools.generic.RenderTool;
import redis.clients.jedis.Jedis;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.Set;

/**
 * Created by bas on 07.10.14.
 */
@Path("/pages")
public class PagesEndpoint
{
    @GET
    public Response newPage(){
        Template template = R.templateEngine().getEmptyTemplate("/views/new.html");
        return Response.ok(template.render()).build();
    }

    @POST
    @Path("/{pageClass}")
    /**
     * Create a new page-instance of the page-class specified as a parameter
     */
    public Response createPage(@PathParam("pageClass") String pageClass) throws URISyntaxException, IOException, ElementParserException{
        // TODO BAS: how can be chosen which database-server is requested to read from? (random number?)
        Jedis redisClient = new Jedis("localhost", 6380);
        try {
            /*
             * Get the default page (='page-class') from the cache and use it to construct a new page filled with the same blocks and rows
             */
            Map<String, Page> cache = PageCache.getInstance().getPageCache();
            Page defaultPage = cache.get(pageClass);

            /*
             * Get a UID for a new page instance for db-representation
             * Check if this page-id (url) is not already present in db, if so, re-render a random page-id
             */
            String newPageId = Page.getNewUniqueID(pageClass);
            while(redisClient.get(newPageId) != null){
                newPageId = Page.getNewUniqueID(pageClass);
            }
            Page newPage = new Page(newPageId, defaultPage);

            /*
             * Save this page to the db, save it's block- and row-ids to a set with this page's name
             * Also it's rows and blocks content to db
             */
            for(AbstractIdentifiableElement element : newPage.getElements()) {
                redisClient.sadd(newPage.getUid(), element.getUid());
                redisClient.set(element.getUid(), element.getContent());
            }




            return Response.seeOther(new URI("/pages/" + pageClass)).build();
        }
        finally{
            try{
                if(redisClient != null){
                    redisClient.close();
                }
            }catch(Exception e){}
        }
    }

    @GET
    @Path("/{pageClass}/{pageId}")
    /*
     * return a page-instance of class 'pageClass', with id 'pageId'
     */
    public Response getPage(@PathParam("pageClass") String pageClass, @PathParam("pageId") String pageId){
        Template template = R.templateEngine().getEmptyTemplate(Page.getTemplatePath(pageClass));
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
//            /*
//             * We can use a velocity runtime service, so we only have to parse the template once an can merge the template multiple times
//             */
//        velocityContext.put("velocityTemplate", velocityTemplate.toString());
//        RuntimeServices runtimeServices = RuntimeSingleton.getRuntimeServices();
//
//
//
//
//
//            /*
//             * Parses the velocity template recursivly and returns a string with all variables in the velocityContext rendered
//             * Note: parse depth is default set to 20
//             * Note 2: renderTools.recurse() stops when encountering numbers, so no id may consist of only a number
//            //TODO BAS: implement url-id-rendering and re-rendering using RenderTool (since it seems to have problems with rendering "$bla/1", we could use ";" as seperator?)
//             */
//        RenderTool renderTool = new RenderTool();
//        String output = renderTool.recurse(velocityContext, velocityTemplate.toString());
        return Response.ok(template.render()).build();
    }

    @POST
    @Path("/{pageClass}/{pageId}")
    /*
     * update a page-instance of class 'pageClass', with id 'pageId'
     */
    public Response updatePage(@PathParam("pageClass") String pageClass, @PathParam("pageId") String pageId, String html)
    {
        return Response.ok().build();
    }


    private static void parseProtoType(String filename){
        Jedis redisClient = new Jedis("localhost", 6379);
        try{
            /*_____________SAVE TO DB_______________*/
            File htmlFile = new File(filename);

            String pageName = htmlFile.getName();
            ElementParser parser = new ElementParser();
            StringBuilder velocityTemplate = new StringBuilder();
            //parse the file to retrieve a map with (id, row/block)-pairs and a string containing the velocity template of the page
            //parse the file to retrieve a set of rows and blocks and a string containing the velocity template of the page
            Set<AbstractIdentifiableElement> rowsAndBlocks = parser.toVelocity(htmlFile, velocityTemplate);
        /*
         * write all the row- and block-info to db
         */
            for(AbstractIdentifiableElement rowOrBlock : rowsAndBlocks){
                //for setting data-version
                long currentTime = System.currentTimeMillis();
                String dbId = rowOrBlock.getUid() + ":" + currentTime;
                //save this row or block in db
                redisClient.set(dbId, rowOrBlock.getContent());
                //add the id of this row or block at the beginning of a list representing all versions of this row or block (first element in list is most recent version)
                redisClient.lpush(rowOrBlock.getUid(), dbId);
                //add the set representing all versions of this row or block to a set representing all rows and blocks of this page
                redisClient.sadd(pageName, rowOrBlock.getUid());
                //add the rowOrBlockId to a set representing all rows or one representing all blocks
                redisClient.sadd(rowOrBlock.getDBSetName(), rowOrBlock.getUid());
            }



            /*_________________AND RETURN (starting from pageName)__________________*/

            Context velocityContext = new VelocityContext();
            //reads can be done from slave
            redisClient = new Jedis("localhost", 6380);
            Set<String> rowAndBlockIds = redisClient.smembers(pageName);
            for(String rowOrBlockId : rowAndBlockIds){
                //rget most recent version (at position 1) from versioning-list stored at 'rowAndBlockId'
                String UID = redisClient.lindex(rowOrBlockId, 1);
                String content = redisClient.get(UID);
                //add the content of the row or block to the velocity-context as a variable for parsing
                velocityContext.put(rowOrBlockId, content);
            }

            /*
             * We can use a velocity runtime service, so we only have to parse the template once an can merge the template multiple times
             */
            velocityContext.put("velocityTemplate", velocityTemplate.toString());
            RuntimeServices runtimeServices = RuntimeSingleton.getRuntimeServices();





            /*
             * Parses the velocity template recursivly and returns a string with all variables in the velocityContext rendered
             * Note: parse depth is default set to 20
             * Note 2: renderTools.recurse() stops when encountering numbers, so no id may consist of only a number
            //TODO BAS: implement url-id-rendering and re-rendering using RenderTool (since it seems to have problems with rendering "$bla/1", we could use ";" as seperator?)
             */
            RenderTool renderTool = new RenderTool();
            String output = renderTool.recurse(velocityContext, velocityTemplate.toString());
            FileUtils.writeStringToFile(new File("/home/bas/Projects/Workspace/idea/MongoDB Test/html/output.html"), output);
        }
        catch(UnknownHostException e){
            System.out.println("Problem while looking for hosts: " + e);
        }
        catch(IOException e){
            System.out.println("Problem while reading or parsing file '" + filename + "': " + e);
        }
        catch(ElementParserException e){
            System.out.println("Problem while parsing file '" + filename + "' to velocity: \n" + e);
        }
        catch (ParseException e) {
            System.out.println("Problem while parsing velocity template: " + e);
        }
        catch(Exception e){
            System.out.println("General problem:" + e);
        }
        finally{
            //safely close everything
            try{
                if(redisClient != null){
                    redisClient.close();
                }
            }catch(Exception e){}

        }
    }


}
