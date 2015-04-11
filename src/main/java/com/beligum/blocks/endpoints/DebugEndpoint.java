package com.beligum.blocks.endpoints;

import com.beligum.base.templating.ifaces.Template;
import com.beligum.base.utils.Logger;
import com.beligum.blocks.base.Blocks;
import com.beligum.blocks.exceptions.CacheException;
import com.beligum.blocks.exceptions.UrlIdMappingException;
import com.beligum.blocks.models.Blueprint;
import com.beligum.blocks.models.PageTemplate;
import com.beligum.blocks.renderer.BlocksTemplateRenderer;
import com.beligum.blocks.urlmapping.BlocksUrlDispatcher;
import com.beligum.blocks.usermanagement.Permissions;
import gen.com.beligum.blocks.core.fs.html.views.admin.*;
import gen.com.beligum.blocks.endpoints.DebugEndpointRoutes;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.annotation.RequiresRoles;
import org.apache.velocity.tools.generic.DateTool;
import org.apache.velocity.tools.generic.EscapeTool;
import org.joda.time.LocalDateTime;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;

/**
 * Created by bas on 27.01.15.
 */
@Path("debug")
@RequiresRoles(Permissions.ADMIN_ROLE_NAME)
public class DebugEndpoint
{
    public static final String ENTTIY_INSTANCE_TYPE = "instance";
    public static final String BLUEPRINT_TYPE = "blueprint";
    public static final String PAGE_TEMPLATE_TYPE = "template";
    public static final String XML_TEMPLATE_TYPE = "xml";

    @GET
    public Response debugIndex()
    {
        return Response.seeOther(URI.create(DebugEndpointRoutes.getBlueprintsPage().getPath())).build();
    }

    @GET
    @Path("/flush")
    public Response flushEntities() throws Exception
    {
        Logger.warn("Database has been flushed by user '" + SecurityUtils.getSubject().getPrincipal() + "' at " + LocalDateTime.now().toString() + " .");

        Logger.warn("Url-id mapping has been reset by user '" + SecurityUtils.getSubject().getPrincipal() + "' at " + LocalDateTime.now().toString() + " .");
        Blocks.templateCache().reset();
        return Response.ok("<ul><li>Database emptied</li><li>Cache reset</li><li>Url-id mapping reset</li></ul>").build();
    }

    @GET
    @Path("/reset")
    @Produces(MediaType.TEXT_PLAIN)
    public Response resetCache() throws Exception
    {
        try {
//            BlueprintsCache.getInstance().reset();
            //            PageTemplateCache.getInstance().reset();
            //            BlueprintsCache.getInstance();
            //            PageTemplateCache.getInstance();
            Blocks.templateCache().reset();
            Logger.warn("Cache has been reset by user '" + SecurityUtils.getSubject().getPrincipal() + "' at " + LocalDateTime.now().toString() + " .");
            return Response.ok("Cache reset").build();
        }
        catch(CacheException e){
            String errorMessage = "Error while resetting: \n";
            errorMessage += e.getMessage();
            Logger.error(errorMessage, e.getCause());
            //TODO: if parse errors occurred, display a log file to user
            return Response.ok(errorMessage).build();
        }
    }

    @GET
    @Path("/pagetemplates")
    public Response getPageTemplatesPage() throws Exception
    {
        Template template = pagetemplates.instance.getNewTemplate();
        template.set("pageTemplates",  Blocks.templateCache().getPagetemplates(Blocks.config().getDefaultLanguage()));
        return Response.ok(template).build();
    }

    @GET
    @Path("/pagetemplates/{pageTemplateName}")
    public Response getPageTemplatePage(
                    @PathParam("pageTemplateName")
                    String pageTemplateName,
                    @QueryParam("lang")
                    String language) throws Exception
    {
        if(StringUtils.isEmpty(language)){
            language = Blocks.config().getDefaultLanguage();
        }
        PageTemplate pageTemplate = Blocks.templateCache().getPagetemplate(pageTemplateName, language);
        Template template = pagetemplate.instance.getNewTemplate();
        template.set("DateTool", new DateTool());
        template.set("EscapeTool", new EscapeTool());
        template.set("pageTemplate", pageTemplate);
        template.set("activeLanguage", language);
        //TODO: rendering should include links ands scripts for full view of blueprint
//        String resourcePath = XMLUrlIdMapper.getInstance().getUrl(pageTemplate.getId()).getPath().substring(1);
//        template.set("src", DebugEndpointRoutes.showTemplate(resourcePath, null, PAGE_TEMPLATE_TYPE).getAbsoluteUrl());
        return Response.ok(template).build();
    }

    @GET
    @Path("/blueprints")
    public Response getBlueprintsPage() throws Exception
    {
        Template template = blueprints.instance.getNewTemplate();
        template.set("blueprints", Blocks.templateCache().getBlueprints(Blocks.config().getDefaultLanguage()));
        return Response.ok(template).build();
    }

    @GET
    @Path("/blueprints/{blueprintName}")
    public Response getBlueprintPage(@PathParam("blueprintName") String blueprintName, @QueryParam("lang") String language) throws Exception
    {
        if(StringUtils.isEmpty(language)){
            language = Blocks.config().getDefaultLanguage();
        }
        Blueprint blueprintObj = Blocks.templateCache().getBlueprint(blueprintName, language);
        Template template = blueprint.instance.getNewTemplate();
        template.set("DateTool", new DateTool());
        template.set("EscapeTool", new EscapeTool());
        template.set("blueprint", blueprintObj);
        template.set("activeLanguage", language);
        //TODO: rendering should include links ands scripts for full view of blueprint
        BlocksTemplateRenderer renderer = Blocks.factory().createTemplateRenderer();
        template.set("src", renderer.render(blueprintObj, null));
        return Response.ok(template).build();
    }

    @GET
    @Path("/sitemap")
    public Response viewSiteMap(@QueryParam("lang") String language) throws UrlIdMappingException
    {
        ArrayList<String> languages = Blocks.config().getLanguages();

        Template template = sitemap.instance.getNewTemplate();
        BlocksUrlDispatcher sitemap = Blocks.urlDispatcher();
        template.set("urlmap", sitemap);
        template.set("languages", languages);

        return Response.ok(template).build();
    }

    @GET
    @Path("src/blueprints")
    @Produces("text/plain")
    public Response getBlueprintsCache(@QueryParam("lang")String language) throws Exception
    {
        if (language == null) language = Blocks.config().getDefaultLanguage();

        String cache = "";
        for(Blueprint blueprint: Blocks.templateCache().getBlueprints(language)){
            cache += "----------------------------------" + blueprint.getBlueprintName() + "---------------------------------- \n\n" + blueprint.getValue() + "\n\n\n\n\n\n";
        }
        return Response.ok(cache).build();
    }

    @GET
    @Path("src/pagetemplates")
    @Produces("text/plain")
    public Response getPageTemplateCache(@QueryParam("lang")String language) throws Exception
    {
        if (language == null) language = Blocks.config().getDefaultLanguage();

        String cache = "";
        for(PageTemplate pageTemplate: Blocks.templateCache().getPagetemplates(language)){
            cache += "----------------------------------" + pageTemplate.getBlueprintName() + "---------------------------------- \n\n" + pageTemplate.getValue() + "\n\n\n\n\n\n";
        }
        return Response.ok(cache).build();
    }

//
//    @GET
//    @Path("/show/{resourcePath:.+}")
//    @Produces("text/html")
//    public Response showTemplate(
//                    @PathParam("resourcePath")
//                    @DefaultValue("")
//                    String resourcePath,
//                    @QueryParam("fragment")
//                    @DefaultValue("")
//                    String fragment,
//                    @QueryParam("type")
//                    String typeName)
//                    throws Exception
//    {
//        URL url = renderUrl(resourcePath, fragment);
//        Class<? extends AbstractTemplate> type = determineType(typeName);
//        BlocksID id = XMLUrlIdMapper.getInstance().getId(url);
//        AbstractTemplate template = (AbstractTemplate) RedisDatabase.getInstance().fetchLastVersion(id, type);
//        if(template instanceof EntityTemplate) {
//            return Response.ok(((EntityTemplate) template).renderEntityInPageTemplate(template.getLanguage())).build();
//        }
//        else if(template instanceof PageTemplate){
//            Blueprint defaultBlueprint = BlueprintsCache.getInstance().get(ParserConstants.DEFAULT_BLUEPRINT);
//            return Response.ok(TemplateParser.renderTemplate(TemplateParser.parse(template.getTemplate()), Blocks.config().getSiteDomainUrl(), id.getLanguage(), template.getLinks(), template.getScripts()).outerHtml()).build();
//        }
//        else{
//            return Response.ok(TemplateParser.renderTemplate(template, id.getLanguage())).build();
//        }
//    }
//
//    @GET
//    @Path("/hash/{resourcePath:.+}")
//    @Produces("text/html")
//    public Response getTemplateHash(
//                    @PathParam("resourcePath")
//                    @DefaultValue("")
//                    String resourcePath,
//                    @QueryParam("fragment")
//                    @DefaultValue("")
//                    String fragment,
//                    @QueryParam("type")
//                    String typeName)
//                    throws Exception
//    {
//        URL url = renderUrl(resourcePath, fragment);
//        Class<? extends AbstractTemplate> type = this.determineType(typeName);
//        BlocksID id = XMLUrlIdMapper.getInstance().getId(url);
//        AbstractTemplate template = (AbstractTemplate) RedisDatabase.getInstance().fetchLastVersion(id, type);
//        String retVal = "";
//        Map<String, String> hash = template.toHash();
//        List<String> keys = new ArrayList<>(hash.keySet());
//        Collections.sort(keys);
//        for(String key : keys){
//            String fieldContent = hash.get(key);
//            fieldContent = fieldContent.replace("<", "&lt;");
//            fieldContent = fieldContent.replace(">", "&gt;");
//            retVal += key + "  ---->  " + fieldContent + "<br/><br/>";
//        }
//        return Response.ok(retVal).build();
//    }
//
//    @GET
//    @Path("/hash/allversions/{resourcePath:.+}")
//    @Produces("text/html")
//    public Response getTemplateHashForAllVersions(@PathParam("resourcePath")
//                                                  @DefaultValue("")
//                                                  String resourcePath,
//                                                  @QueryParam("fragment")
//                                                  @DefaultValue("")
//                                                  String fragment,
//                                                  @QueryParam("type")
//                                                  String typeName) throws Exception
//    {
//        Class<? extends AbstractTemplate> type = determineType(typeName);
//        URL url = renderUrl(resourcePath, fragment);
//        BlocksID id = XMLUrlIdMapper.getInstance().getId(url);
//        List<AbstractTemplate> versions = RedisDatabase.getInstance().fetchVersionList(id, type);
//        String retVal = "";
//        for(AbstractTemplate template : versions) {
//            if(template != null) {
//                retVal += "----------------------------------" + template.getId() + "---------------------------------- <br/><br/>";
//                Map<String, String> hash = template.toHash();
//                List<String> keys = new ArrayList<>(hash.keySet());
//                Collections.sort(keys);
//                for (String key : keys) {
//                    String fieldContent = hash.get(key);
//                    fieldContent = fieldContent.replace("<", "&lt;");
//                    fieldContent = fieldContent.replace(">", "&gt;");
//                    retVal += key + "  ---->  " + fieldContent + "<br/><br/>";
//                }
//                retVal += "<br/><br/><br/>";
//            }
//            else{
//                retVal += "----------------------------------FOUND NULL TEMPLATE----------------------------------";
//                retVal += "<br/><br/><br/><br/><br/>";
//            }
//        }
//        return Response.ok(retVal).build();
//    }
//
//    @GET
//    @Path("/src/allversions/{resourcePath:.+}")
//    @Produces("text/plain")
//    public Response getTemplateSrcForAllVersions(@PathParam("resourcePath")
//                                                 @DefaultValue("")
//                                                 String resourcePath,
//                                                 @QueryParam("fragment")
//                                                 @DefaultValue("")
//                                                 String fragment,
//                                                 @QueryParam("type")
//                                                 String typeName) throws Exception
//    {
//        Class<? extends AbstractTemplate> type = determineType(typeName);
//        URL url = renderUrl(resourcePath, fragment);
//        BlocksID id = XMLUrlIdMapper.getInstance().getId(url);
//        List<AbstractTemplate> versions = RedisDatabase.getInstance().fetchVersionList(id, type);
//        String retVal = "";
//        for(AbstractTemplate template : versions) {
//            if(template != null) {
//                retVal += "----------------------------------" + template.getId() + "---------------------------------- \n \n";
//                Map<BlocksID, String> languageTemplates = template.getTemplates();
//                for(BlocksID languagedId : languageTemplates.keySet()){
//                    retVal += "----------------------------------" + languagedId.getLanguage() + "----------------------------------  \n";
//                    String toBeAdded = languageTemplates.get(languagedId);
//                    retVal += toBeAdded;
//                }
//
//                retVal += "\n\n\n";
//            }
//            else{
//                retVal += "----------------------------------FOUND NULL TEMPLATE----------------------------------";
//                retVal += "\n\n\n\n\n";
//            }
//        }
//        return Response.ok(retVal).build();
//    }

    private URL renderUrl(String resourcePath, String fragment) throws MalformedURLException
    {
        if(!StringUtils.isEmpty(fragment)){
            resourcePath += "#" + fragment;
        }
        return new URL(Blocks.config().getSiteDomain() + "/" + resourcePath);
    }
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
