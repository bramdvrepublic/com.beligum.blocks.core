package com.beligum.blocks.endpoints;

import com.beligum.blocks.config.ParserConstants;
import com.beligum.blocks.base.Blocks;
import com.beligum.blocks.identifiers.BlockId;
import com.beligum.blocks.models.*;
import com.beligum.blocks.parsers.visitors.template.HtmlFromClientVisitor;
import com.beligum.blocks.parsers.Traversor;
import com.beligum.blocks.usermanagement.Permissions;
import com.beligum.base.i18n.I18nFactory;
import com.beligum.base.utils.Logger;
import org.apache.shiro.authz.annotation.RequiresRoles;
import org.hibernate.validator.constraints.NotBlank;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URL;
import java.util.*;

/**
 * Created by bas on 07.10.14.
 */
@Path("/entities")
@RequiresRoles(Permissions.ADMIN_ROLE_NAME)
public class EntitiesEndpoint
{

    @POST
    /**
     * Create a new page-instance of the page-class specified as a parameter
     */
    public Response createEntity(
                    @FormParam("page-url")
                    @NotBlank(message = "No url specified.")
                    String pageUrl,
                    @FormParam("page-class-name")
                    @NotBlank(message = "No entity-class specified.")
                    String entityClassName)
                    throws Exception

    {
        Blueprint blueprint = Blocks.templateCache().getBlueprint(entityClassName, Blocks.config().getDefaultLanguage());
        URL pageURL = new URL(pageUrl);
        BlockId existingId = Blocks.urlDispatcher().findId(pageURL);

        if (existingId != null) {
            // template exists
            throw new Exception("Cannot create already existing entity with url '" + pageURL + "'.");
        }

        Element element = blueprint.getRenderedTemplateAsElement();
        String language = Blocks.urlDispatcher().getLanguage(pageURL);
        StoredTemplate newPage = Blocks.factory().createStoredTemplate(element, language);
        Blocks.database().save(newPage);

        Blocks.urlDispatcher().addId(pageURL, newPage.getId(), language);
        /*
         * Redirect the client to the newly created entity's page
         */
        return Response.seeOther(pageURL.toURI()).build();
    }

    @GET
    @Path("/class/{blueprintType}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getClassTemplate(@PathParam("blueprintType") String blueprintName) throws Exception
    {
        Blueprint blueprint = Blocks.templateCache().getBlueprint(blueprintName, Blocks.config().getDefaultLanguage());
        if (blueprint != null) {
            String classHtml = blueprint.getRenderedTemplate(false, true).toString();
            HashMap<String, String> json = new HashMap<String, String>();
            json.put("template", classHtml);
            return Response.ok(json).build();
        } else {
            throw new Exception("Blueprint not found with name: " + blueprintName);
        }

    }


    @PUT
    @Path("/{entityUrlPath:.+}")
    @Consumes(MediaType.APPLICATION_JSON)
    /*
     * update a page-instance with id 'entityId' to be the html specified
     */
    public Response updateEntity(@PathParam("entityUrlPath") String pageUrlPath, @QueryParam("deleted") @DefaultValue("false") boolean fetchDeleted, String pageHtml)
    {
        try{


            // analyze html,
            // only properties should be a) singletons, b) 1 property that is not a singelton (with reference-to (or resource)) this will replace entity with id of url
            // only other properties allowed in root are properties with typeof
            URL pageUrl = new URL(Blocks.config().getSiteDomainUrl(), pageUrlPath);
            BlockId id = Blocks.urlDispatcher().findId(pageUrl);

            if (id == null) {
                if (fetchDeleted) {
                    id = Blocks.urlDispatcher().findPreviousId(pageUrl);
                }
                if (id == null) {
                    throw new Exception("Cannot update entity which doesn't exist: '" + pageUrl + ".");
                }
            }

            String language = Blocks.urlDispatcher().getLanguage(pageUrl);
            Document html = Jsoup.parse(pageHtml, Blocks.config().getSiteDomain(), Parser.htmlParser());
            HtmlFromClientVisitor htmlFromClientVisitor = new HtmlFromClientVisitor(pageUrl);
            Traversor.traverseProperties(html, htmlFromClientVisitor);

//            RDFReaderFImpl.setBaseReaderClassName("RDFA", RDFaReader.class.getName());

            StoredTemplate pageContent = htmlFromClientVisitor.getContent();
//            Model m = ModelFactory.createDefaultModel();
//            JenaRdfaReader.inject();
//            String c = pageContent.getRenderedTemplate(false, false).toString();
////            c = c.replaceAll(System.lineSeparator(), "");
//            StringReader q = new StringReader(html.outerHtml());
//            Logger.warn(html.outerHtml());
//            m.read(q, "RDFA");

            if (pageContent != null) {
                // recreate this page. This way we prevent unwanted changes
                pageContent = Blocks.factory().createStoredTemplate(pageContent.getRenderedTemplateAsElement(), language);

//                All entities on this page without a parent (need to be saved)
                List<Entity> entities = pageContent.getRootEntities();

                for (Entity entity: entities) {
                    Blocks.database().saveEntity(entity);
                }

                Blocks.database().save(htmlFromClientVisitor.getContent());
            }

            ArrayList<StoredTemplate> other = htmlFromClientVisitor.getOther();
            ArrayList<StoredTemplate> otherWithoutSingletons = new ArrayList<>();
            for (StoredTemplate singleton: other) {

                if (singleton instanceof Singleton) {
                    Blocks.factory().createSingleton(singleton.getRenderedTemplateAsElement(), language);
                    Blocks.database().save(singleton);


                    List<Entity> entities = singleton.getRootEntities();

                    for (Entity entity : entities) {
                        Blocks.database().saveEntity(entity);
                    }

                }else {
                    otherWithoutSingletons.add(singleton);
                }
            }


            for (StoredTemplate storedTemplate: otherWithoutSingletons) {
                storedTemplate = Blocks.factory().createStoredTemplate(storedTemplate.getRenderedTemplateAsElement(), language);
//                    Blocks.database().save(storedTemplate);

                List<Entity> entities = storedTemplate.getRootEntities();

                for (Entity entity: entities) {
                    Blocks.database().saveEntity(entity);
                }
            }

            return Response.ok(pageUrl.getPath()).build();
        }catch (Exception e){
            Logger.error(e);
            return Response.status(Response.Status.BAD_REQUEST).entity(I18nFactory.instance().getDefaultResourceBundle().getMessage("entitySaveFailed")).build();
        }
    }

    @POST
    @Path("/delete")
    public Response deletePage(String url)
    {
        try {
            URL pageUrl = new URL(url);
            BlockId id = Blocks.urlDispatcher().findId(pageUrl);
            String language = Blocks.urlDispatcher().getLanguage(pageUrl);

            if (id == null) {
                throw new Exception("Cannot delete entity which doesn't exist: '" + pageUrl + ".");
            }

            StoredTemplate storedTemplate = Blocks.database().fetchTemplate(id, language);
            Blocks.urlDispatcher().removeId(pageUrl);
            Blocks.database().remove(storedTemplate);
            return Response.ok(pageUrl.toString()).build();
        }
        catch(Exception e){
            return Response.status(Response.Status.BAD_REQUEST).entity(I18nFactory.instance().getDefaultResourceBundle().getMessage("entityDeleteFailed")).build();
        }
    }


    @GET
    @Path("/list")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
        /*
         * Return a list of strings of all available entities
         */
    public Response listEntities() throws Exception
    {
        List<String> entityNames = new ArrayList<String>();
        List<Blueprint> addableClasses = Blocks.templateCache().getAddableBlocks();
        for (Blueprint e : addableClasses) {
            if(!e.getName().equals(ParserConstants.DEFAULT_BLUEPRINT)){
                entityNames.add(e.getBlueprintName());
            }
        }
        return Response.ok(entityNames).build();
    }

    @GET
    @Path("/template")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
        /*
         * Return a list of strings of all available page-templates
         */
    public Response listTemplates() throws Exception
    {
        List<String> templateNames = new ArrayList<String>();
        for (PageTemplate e : Blocks.templateCache().getPagetemplates(Blocks.config().getDefaultLanguage())) {
            if(!e.getName().equals(ParserConstants.DEFAULT_PAGE_TEMPLATE)){
                templateNames.add(e.getName());
            }
        }
        return Response.ok(templateNames).build();
    }

    @PUT
    @Path("/template")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response changeTemplate(@FormParam("template") String templateName, @FormParam("id") String id)
                    throws Exception
    {
        URL url = new URL(id);
        BlockId blockId = Blocks.urlDispatcher().findId(url);
        String language = Blocks.urlDispatcher().getLanguage(url);
        if (blockId != null) {
            PageTemplate pageTemplate = Blocks.templateCache().getPagetemplate(templateName, language);
            if (pageTemplate == null) {
                throw new Exception("Page template does not exist");
            }
            StoredTemplate storedTemplate = Blocks.database().fetchTemplate(blockId, language);
            storedTemplate.setPageTemplateName(templateName);
            Blocks.database().save(storedTemplate);
        }

        return Response.ok().build();
    }

    //    @POST
    //    @Path("/deletedversion")
    //    public Response showDeletedVersion(@FormParam("page-url") String pageUrl) throws Exception
    //    {
    //        BlocksID id = XMLUrlIdMapper.getInstance().getLastId(new URL(pageUrl));
    //        List<AbstractTemplate> versionList = RedisDatabase.getInstance().fetchVersionList(id, EntityTemplate.class);
    //        EntityTemplate lastAccessibleVersion = null;
    //        Iterator<AbstractTemplate> it = versionList.iterator();
    //        while(lastAccessibleVersion == null && it.hasNext()){
    //            EntityTemplate version = (EntityTemplate) it.next();
    //            if(version != null && !version.getDeleted()){
    //                lastAccessibleVersion = version;
    //            }
    //        }
    //        if(lastAccessibleVersion != null) {
    //            String pageUrlPath = new URL(pageUrl).getPath().substring(1);
    //            return Response.seeOther(URI.create(ApplicationEndpointRoutes.getPageWithId(pageUrlPath, new Long(lastAccessibleVersion.getVersion()), true).getAbsoluteUrl())).build();
    //        }
    //        else{
    //            Logger.error("Bad request: cannot revive '" + pageUrl + "' to the state before it was deleted, since no version is present in db which is not deleted.");
    //            return Response.status(Response.Status.BAD_REQUEST).build();
    //        }
    //    }
}
