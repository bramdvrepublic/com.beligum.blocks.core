package com.beligum.blocks.core.endpoints;

import com.beligum.blocks.core.URLMapping.XMLUrlIdMapper;
import com.beligum.blocks.core.caching.BlueprintsCache;
import com.beligum.blocks.core.caching.PageTemplatesCache;
import com.beligum.blocks.core.config.BlocksConfig;
import com.beligum.blocks.core.config.ParserConstants;
import com.beligum.blocks.core.dbs.RedisDatabase;
import com.beligum.blocks.core.exceptions.*;
import com.beligum.blocks.core.identifiers.BlocksID;
import com.beligum.blocks.core.models.redis.templates.*;
import com.beligum.blocks.core.parsers.TemplateParser;
import com.beligum.blocks.core.usermanagement.Permissions;
import com.beligum.blocks.core.utils.Utils;
import com.beligum.core.framework.base.R;
import com.beligum.core.framework.i18n.I18n;
import com.beligum.core.framework.templating.ifaces.Template;
import com.beligum.core.framework.utils.Logger;
import gen.com.beligum.blocks.core.endpoints.DebugEndpointRoutes;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.annotation.RequiresRoles;
import org.apache.velocity.tools.generic.DateTool;
import org.apache.velocity.tools.generic.EscapeTool;
import org.joda.time.LocalDateTime;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

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
    @Path("/flush")
    public Response flushEntities() throws Exception
    {
        RedisDatabase.getInstance().flushDB();
        Logger.warn("Database has been flushed by user '" + SecurityUtils.getSubject().getPrincipal() + "' at " + LocalDateTime.now().toString() + " .");
        XMLUrlIdMapper.getInstance().reset();
        XMLUrlIdMapper.getInstance();
        Logger.warn("Url-id mapping has been reset by user '" + SecurityUtils.getSubject().getPrincipal() + "' at " + LocalDateTime.now().toString() + " .");
        this.resetCache();
        return Response.ok("<ul><li>Database emptied</li><li>Cache reset</li><li>Url-id mapping reset</li></ul>").build();
    }

    @GET
    @Path("/reset")
    @Produces(MediaType.TEXT_PLAIN)
    public Response resetCache() throws Exception
    {
        try {
            BlueprintsCache.getInstance().reset();
            PageTemplatesCache.getInstance().reset();
            BlueprintsCache.getInstance();
            PageTemplatesCache.getInstance();
            Logger.warn("Cache has been reset by user '" + SecurityUtils.getSubject().getPrincipal() + "' at " + LocalDateTime.now().toString() + " .");
            return Response.ok("Cache reset").build();
        }
        catch(ParseException e){
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
        Template template = R.templateEngine().getEmptyTemplate("/views/admin/pagetemplates.vm");
        template.set("pageTemplates", PageTemplatesCache.getInstance().values());
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
            language = BlocksConfig.getDefaultLanguage();
        }
        PageTemplate pageTemplate = PageTemplatesCache.getInstance().get(pageTemplateName);
        Template template = R.templateEngine().getEmptyTemplate("/views/admin/pagetemplate.vm");
        template.set("DateTool", new DateTool());
        template.set("EscapeTool", new EscapeTool());
        template.set("pageTemplate", pageTemplate);
        template.set("activeLanguage", language);
        //TODO: rendering should include links ands scripts for full view of blueprint
        String resourcePath = XMLUrlIdMapper.getInstance().getUrl(pageTemplate.getId()).getPath().substring(1);
        template.set("src", DebugEndpointRoutes.showTemplate(resourcePath, null, PAGE_TEMPLATE_TYPE).getAbsoluteUrl());
        return Response.ok(template).build();
    }

    @GET
    @Path("/blueprints")
    public Response getBlueprintsPage() throws Exception
    {
        Template template = R.templateEngine().getEmptyTemplate("/views/admin/blueprints.vm");
        template.set("blueprints", BlueprintsCache.getInstance().values());
        return Response.ok(template).build();
    }

    @GET
    @Path("/blueprints/{blueprintName}")
    public Response getBlueprintPage(@PathParam("blueprintName") String blueprintName, @QueryParam("lang") String language) throws Exception
    {
        if(StringUtils.isEmpty(language)){
            language = BlocksConfig.getDefaultLanguage();
        }
        Blueprint blueprint = BlueprintsCache.getInstance().get(blueprintName);
        Template template = R.templateEngine().getEmptyTemplate("/views/admin/blueprint.vm");
        template.set("DateTool", new DateTool());
        template.set("EscapeTool", new EscapeTool());
        template.set("blueprint", blueprint);
        template.set("activeLanguage", language);
        //TODO: rendering should include links ands scripts for full view of blueprint
        template.set("src", TemplateParser.renderTemplate(blueprint, language));
        return Response.ok(template).build();
    }


    @GET
    @Path("src/blueprints")
    @Produces("text/plain")
    public Response getBlueprintsCache() throws Exception
    {
        List<String> blueprintKeys = BlueprintsCache.getInstance().keys();
        List<Blueprint> blueprint = BlueprintsCache.getInstance().values();
        String cache = "";
        for(int i = 0; i< blueprint.size(); i++){
            cache += "----------------------------------" + blueprintKeys.get(i) + "---------------------------------- \n\n" + blueprint.get(i).toString() + "\n\n\n\n\n\n";
        }
        return Response.ok(cache).build();
    }

    @GET
    @Path("src/pagetemplates")
    @Produces("text/plain")
    public Response getPageTemplateCache() throws Exception
    {
        List<String> pageTemplateKeys = PageTemplatesCache.getInstance().keys();
        List<PageTemplate> pageTemplates = PageTemplatesCache.getInstance().values();
        String cache = "";
        for(int i = 0; i<pageTemplates.size(); i++){
            cache += "----------------------------------" + pageTemplateKeys.get(i) + "---------------------------------- \n\n" + pageTemplates.get(i).toString() + "\n\n\n\n\n\n";
        }
        return Response.ok(cache).build();
    }

    @GET
    @Path("/src/{resourcePath:.+}")
    @Produces("text/plain")
    public Response fetchEntityTemplateSrc(
                    @PathParam("resourcePath")
                    @DefaultValue("")
                    String resourcePath,
                    @QueryParam("fragment")
                    @DefaultValue("")
                    String fragment,
                    @QueryParam("type")
                    String typeName)
                    throws Exception
    {
        URL url = renderUrl(resourcePath,fragment);
        Class<? extends AbstractTemplate> type = determineType(typeName);
        BlocksID id = XMLUrlIdMapper.getInstance().getId(url);
        AbstractTemplate template = (AbstractTemplate) RedisDatabase.getInstance().fetchLastVersion(id, type);
        return Response.ok(template.toString()).build();
    }

    @GET
    @Path("/show/{resourcePath:.+}")
    @Produces("text/html")
    public Response showTemplate(
                    @PathParam("resourcePath")
                    @DefaultValue("")
                    String resourcePath,
                    @QueryParam("fragment")
                    @DefaultValue("")
                    String fragment,
                    @QueryParam("type")
                    String typeName)
                    throws Exception
    {
        URL url = renderUrl(resourcePath, fragment);
        Class<? extends AbstractTemplate> type = determineType(typeName);
        BlocksID id = XMLUrlIdMapper.getInstance().getId(url);
        AbstractTemplate template = (AbstractTemplate) RedisDatabase.getInstance().fetchLastVersion(id, type);
        if(template instanceof EntityTemplate) {
            return Response.ok(((EntityTemplate) template).renderEntityInPageTemplate(template.getLanguage())).build();
        }
        else if(template instanceof PageTemplate){
            Blueprint defaultBlueprint = BlueprintsCache.getInstance().get(ParserConstants.DEFAULT_BLUEPRINT);
            return Response.ok(TemplateParser.renderTemplate(TemplateParser.parse(template.getTemplate()), BlocksConfig.getSiteDomainUrl(), id.getLanguage(), template.getLinks(), template.getScripts()).outerHtml()).build();
        }
        else{
            return Response.ok(TemplateParser.renderTemplate(template, id.getLanguage())).build();
        }
    }

    @GET
    @Path("/hash/{resourcePath:.+}")
    @Produces("text/html")
    public Response getTemplateHash(
                    @PathParam("resourcePath")
                    @DefaultValue("")
                    String resourcePath,
                    @QueryParam("fragment")
                    @DefaultValue("")
                    String fragment,
                    @QueryParam("type")
                    String typeName)
                    throws Exception
    {
        URL url = renderUrl(resourcePath, fragment);
        Class<? extends AbstractTemplate> type = this.determineType(typeName);
        BlocksID id = XMLUrlIdMapper.getInstance().getId(url);
        AbstractTemplate template = (AbstractTemplate) RedisDatabase.getInstance().fetchLastVersion(id, type);
        String retVal = "";
        Map<String, String> hash = template.toHash();
        List<String> keys = new ArrayList<>(hash.keySet());
        Collections.sort(keys);
        for(String key : keys){
            String fieldContent = hash.get(key);
            fieldContent = fieldContent.replace("<", "&lt;");
            fieldContent = fieldContent.replace(">", "&gt;");
            retVal += key + "  ---->  " + fieldContent + "<br/><br/>";
        }
        return Response.ok(retVal).build();
    }

    @GET
    @Path("/hash/allversions/{resourcePath:.+}")
    @Produces("text/html")
    public Response getTemplateHashForAllVersions(@PathParam("resourcePath")
                                                  @DefaultValue("")
                                                  String resourcePath,
                                                  @QueryParam("fragment")
                                                  @DefaultValue("")
                                                  String fragment,
                                                  @QueryParam("type")
                                                  String typeName) throws Exception
    {
        Class<? extends AbstractTemplate> type = determineType(typeName);
        URL url = renderUrl(resourcePath, fragment);
        BlocksID id = XMLUrlIdMapper.getInstance().getId(url);
        List<AbstractTemplate> versions = RedisDatabase.getInstance().fetchVersionList(id, type);
        String retVal = "";
        for(AbstractTemplate template : versions) {
            if(template != null) {
                retVal += "----------------------------------" + template.getId() + "---------------------------------- <br/><br/>";
                Map<String, String> hash = template.toHash();
                List<String> keys = new ArrayList<>(hash.keySet());
                Collections.sort(keys);
                for (String key : keys) {
                    String fieldContent = hash.get(key);
                    fieldContent = fieldContent.replace("<", "&lt;");
                    fieldContent = fieldContent.replace(">", "&gt;");
                    retVal += key + "  ---->  " + fieldContent + "<br/><br/>";
                }
                retVal += "<br/><br/><br/>";
            }
            else{
                retVal += "----------------------------------FOUND NULL TEMPLATE----------------------------------";
                retVal += "<br/><br/><br/><br/><br/>";
            }
        }
        return Response.ok(retVal).build();
    }

    @GET
    @Path("/src/allversions/{resourcePath:.+}")
    @Produces("text/plain")
    public Response getTemplateSrcForAllVersions(@PathParam("resourcePath")
                                                 @DefaultValue("")
                                                 String resourcePath,
                                                 @QueryParam("fragment")
                                                 @DefaultValue("")
                                                 String fragment,
                                                 @QueryParam("type")
                                                 String typeName) throws Exception
    {
        Class<? extends AbstractTemplate> type = determineType(typeName);
        URL url = renderUrl(resourcePath, fragment);
        BlocksID id = XMLUrlIdMapper.getInstance().getId(url);
        List<AbstractTemplate> versions = RedisDatabase.getInstance().fetchVersionList(id, type);
        String retVal = "";
        for(AbstractTemplate template : versions) {
            if(template != null) {
                retVal += "----------------------------------" + template.getId() + "---------------------------------- \n \n";
                Map<BlocksID, String> languageTemplates = template.getTemplates();
                for(BlocksID languagedId : languageTemplates.keySet()){
                    retVal += "----------------------------------" + languagedId.getLanguage() + "----------------------------------  \n";
                    String toBeAdded = languageTemplates.get(languagedId);
                    retVal += toBeAdded;
                }

                retVal += "\n\n\n";
            }
            else{
                retVal += "----------------------------------FOUND NULL TEMPLATE----------------------------------";
                retVal += "\n\n\n\n\n";
            }
        }
        return Response.ok(retVal).build();
    }

    private URL renderUrl(String resourcePath, String fragment) throws MalformedURLException
    {
        if(!StringUtils.isEmpty(fragment)){
            resourcePath += "#" + fragment;
        }
        return new URL(BlocksConfig.getSiteDomain() + "/" + resourcePath);
    }

    private Class<? extends AbstractTemplate> determineType(String typeName){
        Class<? extends AbstractTemplate> type;
        if(!StringUtils.isEmpty(typeName)) {
            switch (typeName) {
                case ENTTIY_INSTANCE_TYPE:
                    type = EntityTemplate.class;
                    break;
                case BLUEPRINT_TYPE:
                    type = Blueprint.class;
                    break;
                case PAGE_TEMPLATE_TYPE:
                    type = PageTemplate.class;
                    break;
                case XML_TEMPLATE_TYPE:
                    type = UrlIdMapping.class;
                    break;
                default:
                    type = EntityTemplate.class;
                    break;
            }
        }
        else{
            type = EntityTemplate.class;
        }
        return type;
    }

}
