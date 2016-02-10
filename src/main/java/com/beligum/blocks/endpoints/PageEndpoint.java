package com.beligum.blocks.endpoints;

import com.beligum.base.auth.repositories.PersonRepository;
import com.beligum.base.security.Authentication;
import com.beligum.base.server.R;
import com.beligum.blocks.caching.CacheKeys;
import com.beligum.blocks.fs.indexes.JenaPageIndex;
import com.beligum.blocks.fs.indexes.ifaces.PageIndex;
import com.beligum.blocks.fs.pages.SimplePageStore;
import com.beligum.blocks.fs.pages.ifaces.Page;
import com.beligum.blocks.fs.pages.ifaces.PageStore;
import com.beligum.blocks.rdf.sources.HtmlSource;
import com.beligum.blocks.rdf.sources.HtmlStringSource;
import com.beligum.blocks.security.Permissions;
import com.beligum.blocks.templating.blocks.HtmlParser;
import com.beligum.blocks.templating.blocks.PageTemplate;
import gen.com.beligum.blocks.core.messages.blocks.core;
import org.apache.commons.lang.StringUtils;
import org.apache.shiro.authz.annotation.RequiresRoles;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Created by bram on 2/10/16.
 */
@Path("/blocks/admin/page")
@RequiresRoles(Permissions.ADMIN_ROLE_NAME)
public class PageEndpoint
{
    //-----CONSTANTS-----
    public static final String PAGE_TEMPLATE_NAME = "pageTemplateName";

    //-----VARIABLES-----

    //-----CONSTRUCTORS-----

    //-----PUBLIC METHODS-----
    /**
     * When a new page (a non-existing page) is requested, the (logged-in) user is presented with a list of page templates.
     * This endpoint is called when a page template is selected from that list.
     * Basically, we redirect back to the url where the page has to be created and put the name of the pagetemplate in the flashcache,
     * TODO where is it picked up?
     * @param pageUrl the url of the new page to be created
     * @param pageTemplateName the name of the page template to use for the new page
     */
    @GET
    @Path("/template")
    public Response getPageTemplate(@QueryParam("page_url") String pageUrl,
                                    @QueryParam("page_class_name") String pageTemplateName) throws URISyntaxException
    {
        if (StringUtils.isEmpty(pageUrl)) {
            throw new InternalServerErrorException(core.Entries.newPageNoUrlError.getI18nValue());
        }
        if (StringUtils.isEmpty(pageTemplateName)) {
            throw new InternalServerErrorException(core.Entries.newPageNoTemplatenameError.getI18nValue());
        }

        PageTemplate pageTemplate = (PageTemplate) HtmlParser.getTemplateCache().getByTagName(pageTemplateName);
        if (pageTemplate==null) {
            throw new InternalServerErrorException(core.Entries.newPageUnknownTemplateError.getI18nValue());
        }

        R.cacheManager().getFlashCache().put(PAGE_TEMPLATE_NAME, pageTemplateName);

        //redirect to the requested page with the flash cache filled in
        return Response.seeOther(new URI(pageUrl)).build();
    }

    @POST
    @javax.ws.rs.Path("/save/{url:.*}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response savePageNew(@PathParam("url") URI uri, String content) throws Exception
    {
        PersonRepository personRepository = new PersonRepository();

        //Note: the true flag: compacting helps minimizing the whitespace of the JSONLD properties
        HtmlSource source = new HtmlStringSource(content, uri, true);

        //save the file to disk and pull all the proxies etc
        //TODO this should probably honour our watch system and just write the HTML, no?
        Page savedPage = this.getPageStore().save(source, personRepository.get(Authentication.getCurrentPrincipal()));

        this.getTriplestorePageIndex().indexPage(savedPage);

        return Response.ok().build();
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----
    private PageStore getPageStore() throws IOException
    {
        if (!R.cacheManager().getApplicationCache().containsKey(CacheKeys.HDFS_PAGE_STORE)) {
            PageStore pageStore = new SimplePageStore();
            pageStore.init();

            R.cacheManager().getApplicationCache().put(CacheKeys.HDFS_PAGE_STORE, pageStore);
        }

        return (PageStore) R.cacheManager().getApplicationCache().get(CacheKeys.HDFS_PAGE_STORE);
    }
    private PageIndex getTriplestorePageIndex() throws IOException
    {
        if (!R.cacheManager().getApplicationCache().containsKey(CacheKeys.TRIPLESTORE_PAGE_INDEX)) {
            R.cacheManager().getApplicationCache().put(CacheKeys.TRIPLESTORE_PAGE_INDEX, new JenaPageIndex());
        }

        return (PageIndex) R.cacheManager().getApplicationCache().get(CacheKeys.TRIPLESTORE_PAGE_INDEX);
    }
}
