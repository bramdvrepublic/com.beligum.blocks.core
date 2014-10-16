package com.beligum.blocks.core.endpoints;

import com.beligum.blocks.core.caching.PageCache;
import com.beligum.blocks.core.config.BlocksConfig;
import com.beligum.blocks.core.dbs.Redis;
import com.beligum.blocks.core.models.AbstractElement;
import com.beligum.blocks.core.models.storables.Page;
import com.beligum.blocks.core.models.PageClass;
import com.beligum.blocks.core.parsing.PageParser;
import com.beligum.blocks.core.parsing.PageParserException;
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
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.Set;

/**
 * Created by bas on 07.10.14.
 */
@Path("/page")
public class PageEndpoint
{
    @GET
    public Response newPage(){
        Template template = R.templateEngine().getEmptyTemplate("/views/new.html");
        return Response.ok(template.render()).build();
    }

    @POST
    @Path("/{pageClassName}")
    /**
     * Create a new page-instance of the page-class specified as a parameter
     */
    public Response createPage(@PathParam("pageClassName") String pageClassName) throws IOException, PageParserException, URISyntaxException
    {
        /*
         * Get the page-class (containing the default blocks and rows) from the cache and use it to construct a new page
         */
        Map<String, PageClass> cache = PageCache.getInstance().getPageCache();
        PageClass pageClass = cache.get(pageClassName);

        //this try-with-resource block should be set somewhere at the very beginning of the application, so the Redis-instance is closed (and it's connection-pool destroyed) at the end of the application
        try(Redis redis = Redis.getInstance()) {
            Page newPage = redis.getNewPage(pageClass);
            redis.save(newPage);
            /*
             * Redirect the client to the newly created page
             */
            return Response.seeOther(newPage.getUrl().toURI()).build();
        }
    }

//    @GET
//    @Path("/{pageClassName}/{pageId}")
//    /*
//     * return a page-instance of class 'pageClass', with id 'pageId'
//     */
//    public Response getPage(@PathParam("pageClassName") String pageClassName, @PathParam("pageId") String pageId){
//
//    }

    @POST
    @Path("/{pageClass}/{pageId}")
    /*
     * update a page-instance of class 'pageClass', with id 'pageId'
     */
    public Response updatePage(@PathParam("pageClass") String pageClass, @PathParam("pageId") String pageId, String html)
    {
        return Response.ok().build();
    }


//    private static void parseProtoType(String filename){
//        Jedis redisClient = new Jedis("localhost", 6379);
//        try{
//            /*_____________SAVE TO DB_______________*/
//            File htmlFile = new File(filename);
//
//            String pageName = htmlFile.getName();
//            URL siteDomain = new URL(BlocksConfig.getSiteDomain());
//            PageParser parser = new PageParser(siteDomain);
//            StringBuilder velocityTemplate = new StringBuilder();
//            //parse the file to retrieve a map with (id, row/block)-pairs and a string containing the velocity template of the page
//            //parse the file to retrieve a set of rows and blocks and a string containing the velocity template of the page
//            Set<AbstractElement> rowsAndBlocks = parser.toVelocity(htmlFile, velocityTemplate);
//        /*
//         * write all the row- and block-info to db
//         */
//            for(AbstractElement rowOrBlock : rowsAndBlocks){
//                //for setting data-version
//                long currentTime = System.currentTimeMillis();
//                String dbId = rowOrBlock.getId() + ":" + currentTime;
//                //save this row or block in db
//                redisClient.set(dbId, rowOrBlock.getContent());
//                //add the id of this row or block at the beginning of a list representing all versions of this row or block (first element in list is most recent version)
//                redisClient.lpush(rowOrBlock.getId().toString(), dbId);
//                //add the set representing all versions of this row or block to a set representing all rows and blocks of this page
//                redisClient.sadd(pageName, rowOrBlock.getId().toString());
//                //add the rowOrBlockId to a set representing all rows or one representing all blocks
//                redisClient.sadd(rowOrBlock.getDBSetName(), rowOrBlock.getId().toString());
//            }
//
//
//
//            /*_________________AND RETURN (starting from pageName)__________________*/
//
//            Context velocityContext = new VelocityContext();
//            //reads can be done from slave
//            redisClient = new Jedis("localhost", 6380);
//            Set<String> rowAndBlockIds = redisClient.smembers(pageName);
//            for(String rowOrBlockId : rowAndBlockIds){
//                //rget most recent version (at position 1) from versioning-list stored at 'rowAndBlockId'
//                String UID = redisClient.lindex(rowOrBlockId, 1);
//                String content = redisClient.get(UID);
//                //add the content of the row or block to the velocity-context as a variable for parsing
//                velocityContext.put(rowOrBlockId, content);
//            }
//
//            /*
//             * We can use a velocity runtime service, so we only have to parse the template once an can merge the template multiple times
//             */
//            velocityContext.put("velocityTemplate", velocityTemplate.toString());
//            RuntimeServices runtimeServices = RuntimeSingleton.getRuntimeServices();
//
//
//
//
//
//            /*
//             * Parses the velocity template recursivly and returns a string with all variables in the velocityContext rendered
//             * Note: parse depth is default set to 20
//             * Note 2: renderTools.recurse() stops when encountering numbers, so no id may consist of only a number
//             */
//            RenderTool renderTool = new RenderTool();
//            String output = renderTool.recurse(velocityContext, velocityTemplate.toString());
//            FileUtils.writeStringToFile(new File("/home/bas/Projects/Workspace/idea/MongoDB Test/html/output.html"), output);
//        }
//        catch(UnknownHostException e){
//            System.out.println("Problem while looking for hosts: " + e);
//        }
//        catch(IOException e){
//            System.out.println("Problem while reading or parsing file '" + filename + "': " + e);
//        }
//        catch(PageParserException e){
//            System.out.println("Problem while parsing file '" + filename + "' to velocity: \n" + e);
//        }
//        catch (ParseException e) {
//            System.out.println("Problem while parsing velocity template: " + e);
//        }
//        catch(Exception e){
//            System.out.println("General problem:" + e);
//        }
//        finally{
//            //safely close everything
//            try{
//                if(redisClient != null){
//                    redisClient.close();
//                }
//            }catch(Exception e){}
//
//        }
//    }


}
