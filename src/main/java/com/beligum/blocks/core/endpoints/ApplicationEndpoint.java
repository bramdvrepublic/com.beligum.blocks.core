package com.beligum.blocks.core.endpoints;

import com.beligum.blocks.core.config.ParserConstants;
import com.beligum.blocks.core.caching.EntityTemplateClassCache;
import com.beligum.blocks.core.dbs.Redis;
import com.beligum.blocks.core.identifiers.RedisID;
import com.beligum.blocks.core.models.templates.EntityTemplateClass;
import com.beligum.blocks.core.models.templates.EntityTemplate;
import com.beligum.blocks.core.parsers.TemplateParser;
import com.beligum.core.framework.base.R;
import com.beligum.core.framework.base.RequestContext;
import com.beligum.core.framework.templating.ifaces.Template;

import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ThreadFactory;

@Path("/")
public class ApplicationEndpoint
{
//    @Path("/ind")
//    @GET
//    public Response index()
//    {
//        Template indexTemplate = R.templateEngine().getEmptyTemplate("/views/index.html");
////        TypeCacher.instance().reset();
//        return Response.ok(indexTemplate).build();
//    }

    @Path("/finder")
    @GET
    public Response finder()
    {
        Template indexTemplate = R.templateEngine().getEmptyTemplate("/views/finder.html");
        //        TypeCacher.instance().reset();
        return Response.ok(indexTemplate).build();
    }

    @Path("/mot/{name}")
    @GET
    public Response mot(@PathParam("name") String name)
    {
        Template indexTemplate = R.templateEngine().getEmptyTemplate("/templates/mot/"+name+".html");
        //        TypeCacher.instance().reset();
        return Response.ok(indexTemplate).build();
    }

    @Path("/")
    @GET
    public Response overzicht() throws URISyntaxException
    {
//        Template indexTemplate = R.templateEngine().getEmptyTemplate("/views/overzicht.html");
        //        TypeCacher.instance().reset();
        return Response.seeOther(new URI("/index")).build();
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



    //using regular expression to let all requests to undefined paths end up here
    @Path("/{randomPage:.+}")
    @GET
    public Response getPageWithId(@PathParam("randomPage") String randomURLPath)
    {
        try{
            Redis redis = Redis.getInstance();
            URL url = new URL(RequestContext.getRequest().getRequestURL().toString());
            //if no language info is specified in the url, or if the specified language doesn't exist, the default language will still be shown
            RedisID id = new RedisID(url, RedisID.LAST_VERSION, false);
            //if no such page is present in db, ask if user wants to create a new page
            if(id.getVersion() == RedisID.NO_VERSION) {
                Template template = R.templateEngine().getEmptyTemplate("/views/new-page.html");
                //TODO BAS: should we use threading here?
                //the first time the server is started, we need to wait for the cache to be proparly filled, so all classes will be shown the very first time a new page is made.
                EntityTemplateClassCache entityTemplateClassCache = EntityTemplateClassCache.getInstance();
                while(entityTemplateClassCache.isFillingUp()){
                    Thread.sleep(1000);
                    entityTemplateClassCache = EntityTemplateClassCache.getInstance();
                }
                List<EntityTemplateClass> entityTemplateClasses = entityTemplateClassCache.values();
                //TODO BAS: find general way to split entity-classes to be shown when creating a new page and when creating a new block in frontend
                List<EntityTemplateClass> pageClasses = new ArrayList<>();
                for (EntityTemplateClass entityTemplateClass : entityTemplateClasses) {
                    if (entityTemplateClass.getName().contains("-page")) {
                        pageClasses.add(entityTemplateClass);
                    }
                }
                template.set(ParserConstants.ENTITY_URL, RequestContext.getRequest().getRequestURL().toString());
                template.set(ParserConstants.ENTITY_CLASSES, pageClasses);
                return Response.ok(template).build();
            }
            //if a version is present in db, try to fetch the page from db
            else if(!id.hasLanguage()) {
                RedisID primaryLanguageId = new RedisID(id, RedisID.PRIMARY_LANGUAGE);
                //if no primary language can be found in db, it means the page is not present in db
                if (!primaryLanguageId.hasLanguage()) {
                    throw new NotFoundException("Couldn't find " + primaryLanguageId.getUrl());
                }
                return Response.seeOther(primaryLanguageId.getLanguagedUrl().toURI()).build();

            }
            //if the url contains both version and language-information, try to render the entity
            else {
                EntityTemplate entityTemplate = Redis.getInstance().fetchEntityTemplate(id);
                //if no entity-template is returned from db, the specified language doesn't exist
                if(entityTemplate == null){
                    //since a last version was found, it must be present in db
                    EntityTemplate storedInstance = (EntityTemplate) Redis.getInstance().fetchLastVersion(id, EntityTemplate.class);
                    if(storedInstance == null){
                        throw new Exception("Received null from db, after asking for last version of '" + id +"'. This should not happen!");
                    }
                    //if the requested language already exists in db, render and show it
                    if(storedInstance.getLanguages().contains(id.getLanguage())){
                        String page = storedInstance.renderEntityInPageTemplate(id.getLanguage());
                        return Response.ok(page).build();
                    }
                    //save a new language (from url) to db, which is a copy of the last stored version
                    else {
                        String lastVersionHtml = TemplateParser.renderEntityInsidePageTemplate(storedInstance.getPageTemplate(), storedInstance);
                        TemplateParser.updateEntity(url, lastVersionHtml);
                        return Response.seeOther(url.toURI()).build();
                    }
                }
                else {
                    String page = entityTemplate.renderEntityInPageTemplate(entityTemplate.getLanguage());
                    return Response.ok(page).build();
                }
            }
        }
        catch(Exception e){
            throw new NotFoundException("The page '" + randomURLPath + "' could not be found.", e);
        }
    }

}