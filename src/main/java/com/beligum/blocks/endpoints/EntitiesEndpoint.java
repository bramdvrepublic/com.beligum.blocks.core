//package com.beligum.blocks.endpoints;
//
//import com.beligum.base.server.RequestContext;
//import com.beligum.blocks.base.Blocks;
//import com.beligum.blocks.config.ParserConstants;
//import com.beligum.blocks.models.*;
//import com.beligum.blocks.models.url.BlocksURL;
//import com.beligum.blocks.models.url.OkURL;
//import com.beligum.blocks.parsers.Traversor;
//import com.beligum.blocks.parsers.visitors.template.HtmlFromClientVisitor;
//import com.beligum.blocks.renderer.BlocksTemplateRenderer;
//import com.beligum.blocks.repositories.EntityRepository;
//import com.beligum.blocks.repositories.UrlRepository;
//import com.beligum.blocks.security.Permissions;
//import com.beligum.blocks.utils.UrlTools;
//import org.apache.shiro.authz.annotation.RequiresRoles;
//import org.hibernate.validator.constraints.NotBlank;
//import org.jsoup.Jsoup;
//import org.jsoup.nodes.Document;
//import org.jsoup.parser.Parser;
//
//import javax.ws.rs.*;
//import javax.ws.rs.core.MediaType;
//import javax.ws.rs.core.Response;
//import javax.ws.rs.core.UriBuilder;
//import java.net.URI;
//import java.nio.file.Paths;
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Locale;
//
///**
// * Created by bas on 07.10.14.
// */
//@Path("/entities")
//@RequiresRoles(Permissions.ADMIN_ROLE_NAME)
//public class EntitiesEndpoint
//{
//
//    @POST
//    /**
//     * Create a new page-instance of the page-class specified as a parameter
//     */
//    public Response createEntity(
//                    @FormParam("page-url")
//                    @NotBlank(message = "No url specified.")
//                    String pageUrl,
//                    @FormParam("page-class-name")
//                    @NotBlank(message = "No entity-class specified.")
//                    String entityClassName)
//                    throws Exception
//
//    {
//        Blueprint blueprint = Blocks.templateCache().getBlueprint(entityClassName);
//        URI pageURL = new URI(pageUrl);
//
//
//        Locale language = UrlTools.getLanguage(pageURL);
//        if (language == null) {
//            throw new Exception("Could not create entity because no language was specified in the url.");
//        }
//
//        // Create a new page based on the blueprint
//        StoredTemplate newPage = new StoredTemplate(blueprint, language);
//        ResourceContext context = new ResourceContext(newPage, language);
//        RequestContext.getEntityManager().persist(context);
//
//        // Create a rout to this new page for this url
//        URI urlWithoutLanguage = UriBuilder.fromUri(pageURL).replacePath(UrlTools.getPathWithoutLanguage(Paths.get(pageURL.getSimplePath())).toString()).build();
//        BlocksURL routing = new OkURL(urlWithoutLanguage, UriBuilder.fromUri(newPage.getBlockId()).build(), null, language);
//        RequestContext.getEntityManager().persist(routing);
//
//        /*
//         * Redirect the client to the newly created entity's page
//         */
//        return Response.seeOther(pageURL).build();
//    }
//
//    @GET
//    @Path("/class/{blueprintType}")
//    @Produces(MediaType.APPLICATION_JSON)
//    public Response getClassTemplate(@PathParam("blueprintType") String blueprintName) throws Exception
//    {
//        Blueprint blueprint = Blocks.templateCache().getBlueprint(blueprintName);
//        if (blueprint != null) {
//            BlocksTemplateRenderer renderer = Blocks.factory().createTemplateRenderer();
//            String classHtml = renderer.render(blueprint, null, BlocksConfig.instance().getDefaultLanguage()).toString();
//            HashMap<String, String> json = new HashMap<String, String>();
//            json.put("template", classHtml);
//            return Response.ok(json).build();
//        }
//        else {
//            throw new Exception("Blueprint not found with name: " + blueprintName);
//        }
//
//    }
//
//    @PUT
//    @Path("/{entityUrlPath:.+}")
//    @Consumes(MediaType.APPLICATION_JSON)
//    /*
//     * update a page-instance with id 'entityId' to be the html specified
//     */
//    public Response updateEntity(@PathParam("entityUrlPath") String pageUrlPath, @QueryParam("deleted") @DefaultValue("false") boolean fetchDeleted, String pageHtml) throws Exception
//    {
//
//        // analyze html,
//        // only properties should be a) singletons, b) 1 property that is not a singelton (with reference-to (or resource)) this will replace entity with id of url
//        // only other properties allowed in root are properties with typeof
//        URI pageUrl = RequestContext.getJaxRsRequest().getUriInfo().getRequestUri();
//        Locale language = UrlTools.getLanguage(pageUrl);
//
//        // Find the Routing url and the page context
//        BlocksURL routing = UrlRepository.instance().getUrlForURI(pageUrl.getAuthority(), pageUrl.getSimplePath(), language);
//        ResourceContext resourceContext = EntityRepository.instance().findContextByURI((((OkURL) routing).getViewUri()), language);
//
//        if (routing == null || resourceContext == null) {
//            throw new Exception("Cannot update entity which doesn't exist: '" + pageUrl + ".");
//        }
//
//        // Parse the page inside a new StoredTemplate
//        Document html = Jsoup.parse(pageHtml, BlocksConfig.instance().getSiteDomain().toString(), Parser.htmlParser());
//        HtmlFromClientVisitor htmlFromClientVisitor = new HtmlFromClientVisitor(pageUrl);
//        Traversor.traverseProperties(html, htmlFromClientVisitor);
//
//        StoredTemplate pageContent = htmlFromClientVisitor.getContent();
//
//        if (pageContent != null) {
//            // Save this new content inside the old ResourceContextOf the page
//            pageContent = Blocks.factory().createStoredTemplate(pageContent.getRenderedTemplateAsElement(), language);
//            pageContent.setBlockId(resourceContext.getBlockId());
//            resourceContext.setResource(pageContent);
//            RequestContext.getEntityManager().persist(resourceContext);
//
//        }
//
//        ArrayList<StoredTemplate> other = htmlFromClientVisitor.getOther();
//        ArrayList<StoredTemplate> otherWithoutSingletons = new ArrayList<>();
//        for (StoredTemplate singleton : other) {
//
//            if (singleton instanceof Singleton) {
////                Blocks.factory().createSingleton(singleton.getRenderedTemplateAsElement(), language);
//                //                    Blocks.database().save(singleton);
//
//
//                //                    List<Entity> entities = singleton.getRootEntities();
//                //
//                //                    for (Entity entity : entities) {
//                //                        Blocks.database().saveEntity(entity);
//                //                    }
//
//            }else {
//                otherWithoutSingletons.add(singleton);
//            }
//        }
//
//        for (StoredTemplate storedTemplate : otherWithoutSingletons) {
//            storedTemplate = Blocks.factory().createStoredTemplate(storedTemplate.getRenderedTemplateAsElement(), language);
//            //                    Blocks.database().save(storedTemplate);
//
//            //                List<Entity> entities = storedTemplate.getRootEntities();
//            //
//            //                for (Entity entity: entities) {
//            //                    Blocks.database().saveEntity(entity);
//            //                }
//        }
//
//        return Response.ok(pageUrl.getSimplePath()).build();
//
//    }
//
//    @POST
//    @Path("/delete")
//    public Response deletePage(String url) throws Exception
//    {
//        URI pageUrl = UriBuilder.fromUri(url).build();
//        Locale language = UrlTools.getLanguage(pageUrl);
//
//        BlocksURL id = UrlRepository.instance().getUrlForURI(pageUrl.getAuthority(), UrlTools.getPathWithoutLanguage(Paths.get(pageUrl.getSimplePath())).toString(), language);
//
//        if (id == null) {
//            throw new Exception("Cannot delete entity which doesn't exist: '" + pageUrl + ".");
//        }
//
//        id.setDeleted(true);
//        RequestContext.getEntityManager().merge(id);
//
//
//        return Response.ok(pageUrl.toString()).build();
//
//    }
//
//    @GET
//    @Path("/list")
//    @Consumes(MediaType.APPLICATION_JSON)
//    @Produces(MediaType.APPLICATION_JSON)
//        /*
//         * Return a list of strings of all available entities
//         */
//    public Response listEntities() throws Exception
//    {
//        List<String> entityNames = new ArrayList<String>();
//        List<Blueprint> addableClasses = Blocks.templateCache().getAddableBlocks();
//        for (Blueprint e : addableClasses) {
//            if (!e.getName().equals(ParserConstants.DEFAULT_BLUEPRINT)) {
//                entityNames.add(e.getBlueprintName());
//            }
//        }
//        return Response.ok(entityNames).build();
//    }
//
//    @GET
//    @Path("/template")
//    @Consumes(MediaType.APPLICATION_JSON)
//    @Produces(MediaType.APPLICATION_JSON)
//        /*
//         * Return a list of strings of all available page-templates
//         */
//    public Response listTemplates() throws Exception
//    {
//        List<String> templateNames = new ArrayList<String>();
//        for (PageTemplate e : Blocks.templateCache().getPageTemplates()) {
//            if (!e.getName().equals(ParserConstants.DEFAULT_PAGE_TEMPLATE)) {
//                templateNames.add(e.getName());
//            }
//        }
//        return Response.ok(templateNames).build();
//    }
//
//    @PUT
//    @Path("/template")
//    @Consumes(MediaType.APPLICATION_JSON)
//    @Produces(MediaType.APPLICATION_JSON)
//    public Response changeTemplate(@FormParam("template") String templateName, @FormParam("id") String id)
//                    throws Exception
//    {
//        URI url = new URI(id);
//        BlocksURL blockId = UrlRepository.instance().getId(url.toString());
//        Locale language = UrlTools.getLanguage(url);
//        if (blockId != null) {
//            PageTemplate pageTemplate = Blocks.templateCache().getPageTemplate(templateName);
//            if (pageTemplate == null) {
//                throw new Exception("Page template does not exist");
//            }
//            //            StoredTemplate storedTemplate = Blocks.database().fetchTemplate(blockId, language);
//            //            storedTemplate.setPageTemplateName(templateName);
//            //            Blocks.database().save(storedTemplate);
//        }
//
//        return Response.ok().build();
//    }
//
//    //        @POST
//    //        @Path("/deletedversion")
//    //        public Response showDeletedVersion(@FormParam("page-url") String pageUrl) throws Exception
//    //        {
//    //            BlocksID id = XMLUrlIdMapper.getInstance().getLastId(new URL(pageUrl));
//    //            List<AbstractTemplate> versionList = RedisDatabase.getInstance().fetchVersionList(id, EntityTemplate.class);
//    //            EntityTemplate lastAccessibleVersion = null;
//    //            Iterator<AbstractTemplate> it = versionList.iterator();
//    //            while(lastAccessibleVersion == null && it.hasNext()){
//    //                EntityTemplate version = (EntityTemplate) it.next();
//    //                if(version != null && !version.getDeleted()){
//    //                    lastAccessibleVersion = version;
//    //                }
//    //            }
//    //            if(lastAccessibleVersion != null) {
//    //                String pageUrlPath = new URL(pageUrl).getSimplePath().substring(1);
//    //                return Response.seeOther(URI.create(ApplicationEndpointRoutes.getPageWithId(pageUrlPath, new Long(lastAccessibleVersion.getVersion()), true).getAbsoluteUrl())).build();
//    //            }
//    //            else{
//    //                Logger.error("Bad request: cannot revive '" + pageUrl + "' to the state before it was deleted, since no version is present in db which is not deleted.");
//    //                return Response.status(Response.Status.BAD_REQUEST).build();
//    //            }
//    //        }
//}
