package com.beligum.blocks.endpoints;

import com.beligum.base.server.RequestContext;
import com.beligum.base.utils.Logger;
import com.beligum.blocks.config.BlocksConfig;
import com.beligum.blocks.controllers.interfaces.PersistenceController;
import com.beligum.blocks.search.ElasticSearch;
import com.beligum.blocks.security.Permissions;
import org.apache.commons.io.IOUtils;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.annotation.RequiresRoles;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.client.IndicesAdminClient;
import org.joda.time.LocalDateTime;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import java.util.Locale;

/**
 * Created by bas on 27.01.15.
 */
@Path("debug")
@RequiresRoles(Permissions.ADMIN_ROLE_NAME)
public class DebugEndpoint
{
    @GET
    @Path("/flush")
    public Response flushEntities() throws Exception
    {
        Logger.warn("Database has been flushed by user '" + SecurityUtils.getSubject().getPrincipal() + "' at " + LocalDateTime.now().toString() + " .");

        Logger.warn("Url-id mapping has been reset by user '" + SecurityUtils.getSubject().getPrincipal() + "' at " + LocalDateTime.now().toString() + " .");

        ElasticSearch.instance().getClient().admin().indices().delete(new DeleteIndexRequest("*")).actionGet();

        ClassLoader classLoader = getClass().getClassLoader();
        String resourceMapping = null;
        String pageMapping = null;
        String pathMapping = null;
        String settings = null;
        try {
            resourceMapping = IOUtils.toString(classLoader.getResourceAsStream("elastic/resource.json"));
            pageMapping = IOUtils.toString(classLoader.getResourceAsStream("elastic/page.json"));
            pathMapping = IOUtils.toString(classLoader.getResourceAsStream("elastic/path.json"));
            settings = IOUtils.toString(classLoader.getResourceAsStream("elastic/settings.json"));
        }
        catch (Exception e) {
            Logger.error("Could not read mappings for elastic search", e);
        }

        RequestContext.getEntityManager().createNativeQuery("delete from page where id > 0").executeUpdate();
        RequestContext.getEntityManager().createNativeQuery("delete from resource_language").executeUpdate();
        RequestContext.getEntityManager().createNativeQuery("delete from resource where id > 0").executeUpdate();
        RequestContext.getEntityManager().createNativeQuery("delete from path where id > 0").executeUpdate();

        IndicesAdminClient esIndicesClient = ElasticSearch.instance().getClient().admin().indices();
        for (Locale locale : BlocksConfig.instance().getLanguages().values()) {
            esIndicesClient.prepareCreate(ElasticSearch.instance().getPageIndexName(locale)).setSettings(settings).addMapping(PersistenceController.WEB_PAGE_CLASS,pageMapping).execute().actionGet();
            esIndicesClient.prepareCreate(ElasticSearch.instance().getResourceIndexName(locale)).setSettings(settings).addMapping("_default_",resourceMapping).execute().actionGet();
        }

        esIndicesClient.prepareCreate(PersistenceController.PATH_CLASS).setSettings(settings).addMapping(PersistenceController.PATH_CLASS, pathMapping).execute().actionGet();

        return Response.ok("<ul><li>Database emptied</li><li>Cache reset</li></ul>").build();
    }
    //
    //    @GET
    //    @Path("/pagetemplates/{pageTemplateName}")
    //    public Response getPageTemplatePage(
    //                    @PathParam("pageTemplateName")
    //                    String pageTemplateName,
    //                    @QueryParam("lang")
    //                    String getLanguage) throws Exception
    //    {
    //        if (StringUtils.isEmpty(getLanguage)) {
    //            getLanguage = BlocksConfig.instance().getDefaultLanguage();
    //        }
    //        PageTemplate pageTemplate = Blocks.templateCache().getPageTemplate(pageTemplateName);
    //        Template template = pagetemplate.get().getNewTemplate();
    //        template.set("DateTool", new DateTool());
    //        template.set("EscapeTool", new EscapeTool());
    //        template.set("pageTemplate", pageTemplate);
    //        template.set("activeLanguage", getLanguage);
    //        //TODO: rendering should include links ands scripts for full view of blueprint
    //        //        String resourcePath = XMLUrlIdMapper.getInstance().getUrl(pageTemplate.getId()).getSimplePath().substring(1);
    //        //        template.set("src", DebugEndpointRoutes.showTemplate(resourcePath, null, PAGE_TEMPLATE_TYPE).getAbsoluteUrl());
    //        return Response.ok(template).build();
    //    }
    //
    //    @GET
    //    @Path("/blueprints")
    //    public Response getBlueprintsPage() throws Exception
    //    {
    //        Template template = blueprints.get().getNewTemplate();
    //        template.set("blueprints", Blocks.templateCache().getBlueprints());
    //        return Response.ok(template).build();
    //    }
    //
    //    @GET
    //    @Path("/blueprints/{blueprintName}")
    //    public Response getBlueprintPage(@PathParam("blueprintName") String blueprintName, @QueryParam("lang") String getLanguage) throws Exception
    //    {
    //        if (StringUtils.isEmpty(getLanguage)) {
    //            getLanguage = BlocksConfig.instance().getDefaultLanguage();
    //        }
    //        Blueprint blueprintObj = Blocks.templateCache().getBlueprint(blueprintName);
    //        Template template = blueprint.get().getNewTemplate();
    //        template.set("DateTool", new DateTool());
    //        template.set("EscapeTool", new EscapeTool());
    //        template.set("blueprint", blueprintObj);
    //        template.set("activeLanguage", getLanguage);
    //        //TODO: rendering should include links ands scripts for full view of blueprint
    //        BlocksTemplateRenderer renderer = Blocks.factory().createTemplateRenderer();
    //        template.set("src", renderer.render(blueprintObj, null, BlocksConfig.instance().getDefaultLanguage()));
    //        return Response.ok(template).build();
    //    }
    //
    //    @GET
    //    @Path("/sitemap")
    //    public Response viewSiteMap(@QueryParam("lang") String getLanguage) throws UrlIdMappingException
    //    {
    //        ArrayList<String> languages = BlocksConfig.instance().getLanguages();
    //
    //        Template template = sitemap.get().getNewTemplate();
    //        BlocksUrlDispatcher sitemap = Blocks.urlDispatcher();
    //        template.set("urlmap", sitemap);
    //        template.set("languages", languages);
    //
    //        return Response.ok(template).build();
    //    }
    //
    //    @GET
    //    @Path("src/blueprints")
    //    @Produces("text/plain")
    //    public Response getBlueprintsCache(@QueryParam("lang") String getLanguage) throws Exception
    //    {
    //        if (getLanguage == null)
    //            getLanguage = BlocksConfig.instance().getDefaultLanguage();
    //
    //        String cache = "";
    //        for (Blueprint blueprint : Blocks.templateCache().getBlueprints()) {
    //            cache += "----------------------------------" + blueprint.getBlueprintName() + "---------------------------------- \n\n" + blueprint.getValue() + "\n\n\n\n\n\n";
    //        }
    //        return Response.ok(cache).build();
    //    }
    //
    //    @GET
    //    @Path("src/pagetemplates")
    //    @Produces("text/plain")
    //    public Response getPageTemplateCache(@QueryParam("lang") String getLanguage) throws Exception
    //    {
    //        if (getLanguage == null)
    //            getLanguage = BlocksConfig.instance().getDefaultLanguage();
    //
    //        String cache = "";
    //        for (PageTemplate pageTemplate : Blocks.templateCache().getPageTemplates()) {
    //            cache += "----------------------------------" + pageTemplate.getBlueprintName() + "---------------------------------- \n\n" + pageTemplate.getValue() + "\n\n\n\n\n\n";
    //        }
    //        return Response.ok(cache).build();
    //    }
    //
    //    //
    //    //    @GET
    //    //    @Path("/show/{resourcePath:.+}")
    //    //    @Produces("text/html")
    //    //    public Response showTemplate(
    //    //                    @PathParam("resourcePath")
    //    //                    @DefaultValue("")
    //    //                    String resourcePath,
    //    //                    @QueryParam("fragment")
    //    //                    @DefaultValue("")
    //    //                    String fragment,
    //    //                    @QueryParam("type")
    //    //                    String typeName)
    //    //                    throws Exception
    //    //    {
    //    //        URL url = renderUrl(resourcePath, fragment);
    //    //        Class<? extends AbstractTemplate> type = determineType(typeName);
    //    //        BlocksID id = XMLUrlIdMapper.getInstance().getId(url);
    //    //        AbstractTemplate template = (AbstractTemplate) RedisDatabase.getInstance().fetchLastVersion(id, type);
    //    //        if(template instanceof EntityTemplate) {
    //    //            return Response.ok(((EntityTemplate) template).renderEntityInPageTemplate(template.getLanguage())).build();
    //    //        }
    //    //        else if(template instanceof PageTemplate){
    //    //            Blueprint defaultBlueprint = BlueprintsCache.getInstance().get(ParserConstants.DEFAULT_BLUEPRINT);
    //    //            return Response.ok(TemplateParser.renderTemplate(TemplateParser.parse(template.getTemplate()), BlocksConfig.instance().getSiteDomainUrl(), id.getLanguage(), template.getLinks(), template.getScripts()).outerHtml()).build();
    //    //        }
    //    //        else{
    //    //            return Response.ok(TemplateParser.renderTemplate(template, id.getLanguage())).build();
    //    //        }
    //    //    }
    //    //
    //    //    @GET
    //    //    @Path("/hash/{resourcePath:.+}")
    //    //    @Produces("text/html")
    //    //    public Response getTemplateHash(
    //    //                    @PathParam("resourcePath")
    //    //                    @DefaultValue("")
    //    //                    String resourcePath,
    //    //                    @QueryParam("fragment")
    //    //                    @DefaultValue("")
    //    //                    String fragment,
    //    //                    @QueryParam("type")
    //    //                    String typeName)
    //    //                    throws Exception
    //    //    {
    //    //        URL url = renderUrl(resourcePath, fragment);
    //    //        Class<? extends AbstractTemplate> type = this.determineType(typeName);
    //    //        BlocksID id = XMLUrlIdMapper.getInstance().getId(url);
    //    //        AbstractTemplate template = (AbstractTemplate) RedisDatabase.getInstance().fetchLastVersion(id, type);
    //    //        String retVal = "";
    //    //        Map<String, String> hash = template.toHash();
    //    //        List<String> keys = new ArrayList<>(hash.keySet());
    //    //        Collections.sort(keys);
    //    //        for(String key : keys){
    //    //            String fieldContent = hash.get(key);
    //    //            fieldContent = fieldContent.replace("<", "&lt;");
    //    //            fieldContent = fieldContent.replace(">", "&gt;");
    //    //            retVal += key + "  ---->  " + fieldContent + "<br/><br/>";
    //    //        }
    //    //        return Response.ok(retVal).build();
    //    //    }
    //    //
    //    //    @GET
    //    //    @Path("/hash/allversions/{resourcePath:.+}")
    //    //    @Produces("text/html")
    //    //    public Response getTemplateHashForAllVersions(@PathParam("resourcePath")
    //    //                                                  @DefaultValue("")
    //    //                                                  String resourcePath,
    //    //                                                  @QueryParam("fragment")
    //    //                                                  @DefaultValue("")
    //    //                                                  String fragment,
    //    //                                                  @QueryParam("type")
    //    //                                                  String typeName) throws Exception
    //    //    {
    //    //        Class<? extends AbstractTemplate> type = determineType(typeName);
    //    //        URL url = renderUrl(resourcePath, fragment);
    //    //        BlocksID id = XMLUrlIdMapper.getInstance().getId(url);
    //    //        List<AbstractTemplate> versions = RedisDatabase.getInstance().fetchVersionList(id, type);
    //    //        String retVal = "";
    //    //        for(AbstractTemplate template : versions) {
    //    //            if(template != null) {
    //    //                retVal += "----------------------------------" + template.getId() + "---------------------------------- <br/><br/>";
    //    //                Map<String, String> hash = template.toHash();
    //    //                List<String> keys = new ArrayList<>(hash.keySet());
    //    //                Collections.sort(keys);
    //    //                for (String key : keys) {
    //    //                    String fieldContent = hash.get(key);
    //    //                    fieldContent = fieldContent.replace("<", "&lt;");
    //    //                    fieldContent = fieldContent.replace(">", "&gt;");
    //    //                    retVal += key + "  ---->  " + fieldContent + "<br/><br/>";
    //    //                }
    //    //                retVal += "<br/><br/><br/>";
    //    //            }
    //    //            else{
    //    //                retVal += "----------------------------------FOUND NULL TEMPLATE----------------------------------";
    //    //                retVal += "<br/><br/><br/><br/><br/>";
    //    //            }
    //    //        }
    //    //        return Response.ok(retVal).build();
    //    //    }
    //    //
    //    //    @GET
    //    //    @Path("/src/allversions/{resourcePath:.+}")
    //    //    @Produces("text/plain")
    //    //    public Response getTemplateSrcForAllVersions(@PathParam("resourcePath")
    //    //                                                 @DefaultValue("")
    //    //                                                 String resourcePath,
    //    //                                                 @QueryParam("fragment")
    //    //                                                 @DefaultValue("")
    //    //                                                 String fragment,
    //    //                                                 @QueryParam("type")
    //    //                                                 String typeName) throws Exception
    //    //    {
    //    //        Class<? extends AbstractTemplate> type = determineType(typeName);
    //    //        URL url = renderUrl(resourcePath, fragment);
    //    //        BlocksID id = XMLUrlIdMapper.getInstance().getId(url);
    //    //        List<AbstractTemplate> versions = RedisDatabase.getInstance().fetchVersionList(id, type);
    //    //        String retVal = "";
    //    //        for(AbstractTemplate template : versions) {
    //    //            if(template != null) {
    //    //                retVal += "----------------------------------" + template.getId() + "---------------------------------- \n \n";
    //    //                Map<BlocksID, String> languageTemplates = template.getTemplates();
    //    //                for(BlocksID languagedId : languageTemplates.keySet()){
    //    //                    retVal += "----------------------------------" + languagedId.getLanguage() + "----------------------------------  \n";
    //    //                    String toBeAdded = languageTemplates.get(languagedId);
    //    //                    retVal += toBeAdded;
    //    //                }
    //    //
    //    //                retVal += "\n\n\n";
    //    //            }
    //    //            else{
    //    //                retVal += "----------------------------------FOUND NULL TEMPLATE----------------------------------";
    //    //                retVal += "\n\n\n\n\n";
    //    //            }
    //    //        }
    //    //        return Response.ok(retVal).build();
    //    //    }
    //
    //    private URL renderUrl(String resourcePath, String fragment) throws MalformedURLException
    //    {
    //        if (!StringUtils.isEmpty(fragment)) {
    //            resourcePath += "#" + fragment;
    //        }
    //        return new URL(BlocksConfig.instance().getSiteDomain() + "/" + resourcePath);
    //    }
    //
    //    private Class<? extends AbstractTemplate> determineType(String typeName){
    //        Class<? extends AbstractTemplate> type;
    //        if(!StringUtils.isEmpty(typeName)) {
    //            switch (typeName) {
    //                case ENTTIY_INSTANCE_TYPE:
    //                    type = EntityTemplate.class;
    //                    break;
    //                case BLUEPRINT_TYPE:
    //                    type = Blueprint.class;
    //                    break;
    //                case PAGE_TEMPLATE_TYPE:
    //                    type = PageTemplate.class;
    //                    break;
    //                case XML_TEMPLATE_TYPE:
    //                    type = UrlIdMapping.class;
    //                    break;
    //                default:
    //                    type = EntityTemplate.class;
    //                    break;
    //            }
    //        }
    //        else{
    //            type = EntityTemplate.class;
    //        }
    //        return type;
    //    }

}
