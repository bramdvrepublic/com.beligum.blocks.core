package com.beligum.blocks.controllers;

import com.beligum.base.server.R;
import com.beligum.base.utils.Logger;
import com.beligum.blocks.config.ParserConstants;
import com.beligum.blocks.config.Settings;
import com.beligum.blocks.controllers.interfaces.PersistenceController;
import com.beligum.blocks.models.factories.ResourceFactoryImpl;
import com.beligum.blocks.models.interfaces.Resource;
import com.beligum.blocks.models.interfaces.WebPage;
import com.beligum.blocks.models.interfaces.WebPath;
import com.beligum.blocks.models.sql.DBPage;
import com.beligum.blocks.models.sql.DBPath;
import com.beligum.blocks.models.sql.DBResource;
import com.beligum.blocks.search.ElasticSearch;
import com.beligum.blocks.security.Permissions;
import com.beligum.blocks.utils.RdfTools;
import org.apache.shiro.SecurityUtils;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;

import javax.persistence.NoResultException;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.*;

/**
 * Created by wouter on 22/06/15.
 */
public class PersistenceControllerImpl implements PersistenceController
{

    private static PersistenceControllerImpl instance;

    private PersistenceControllerImpl()
    {

    }

    public static PersistenceControllerImpl instance()
    {
        if (PersistenceControllerImpl.instance == null) {
            PersistenceControllerImpl.instance = new PersistenceControllerImpl();
        }
        return PersistenceControllerImpl.instance;
    }

    public WebPage getWebPage(URI blockId, Locale locale) throws IOException
    {
        WebPage retVal = null;
        // When user can not modify a page we get the page from our ES cluster
        // Because ES only updates every second, we have to take the page from the real DB
        // when user can modify the page
        if (!SecurityUtils.getSubject().isPermitted(Permissions.Action.PAGE_MODIFY.getPermission())) {
            QueryBuilder
                            query =
                            QueryBuilders.boolQuery().must(QueryBuilders.matchQuery("master_page", blockId.toString()))
                                         .must(QueryBuilders.matchQuery(ParserConstants.JSONLD_LANGUAGE, locale.getLanguage()));
            SearchResponse searchResponse = ElasticSearch.instance().getClient().prepareSearch(ElasticSearch.instance().getPageIndexName(locale)).setQuery(query).execute().actionGet();

            if (searchResponse.getHits().getHits().length > 0) {
                retVal = ResourceFactoryImpl.instance().deserializeWebpage(searchResponse.getHits().getHits()[0].getSourceAsString().getBytes(), locale);
                //                retVal = WebPageImpl.pageMapper.readValue(searchResponse.getHits().getHits()[0].getSourceAsString(), WebPage.class);
            }
        }

        // If page is not found, double check in DB
        if (retVal == null) {
            DBPage page = findPageInDB(blockId);
            if (page != null) {
                retVal = page.getWebPage();
            }
        }

        if (retVal != null) {
            retVal.setLanguage(locale);
        }

        return retVal;
    }

    public WebPage saveWebPage(WebPage webPage, boolean doVersion) throws IOException
    {
        DBPage dbPage = findPageInDB(webPage.getBlockId());

        if (dbPage == null) {
            dbPage = new DBPage(webPage);
        }
        else {
            dbPage.setWebPage(webPage);
        }
        R.requestContext().getEntityManager().persist(dbPage);

        addPageToLucene(dbPage.getId().toString(), ResourceFactoryImpl.instance().serializeWebpage(webPage, false), Locale.ROOT);
        return webPage;
    }

    public void deleteWebPage(URI blockId)
    {
        // Do not remove the page but mark the path as not found
        List<DBPath> paths = R.requestContext().getEntityManager().createQuery("select p FROM DBPath p where p.blockId = :id", DBPath.class).setParameter("id", blockId.toString()).getResultList();
        R.requestContext().getEntityManager().createQuery("update DBPath p SET p.statusCode = 404 where p.blockId = :id").setParameter("id", blockId.toString())
                      .executeUpdate();
        List<Long> pages = R.requestContext().getEntityManager().createQuery("select p.id FROM DBPage p where p.blockId = :id", Long.class).setParameter("id", blockId.toString()).getResultList();

        for (DBPath path : paths) {
            removePathFromLucene(path.getId().toString());
        }

        for (Long id : pages) {
            removePageFromLucene(id.toString());
        }
    }

    public WebPath getPath(Path path, Locale locale)
    {
        // find the path for this language
        DBPath webpath = null;
        try {
            webpath = R.requestContext().getEntityManager().createQuery("Select p from DBPath p where p.url = :path and p.language = :language ORDER BY p.createdAt DESC", DBPath.class)
                                    .setParameter("language", locale.getLanguage())
                                    .setParameter("path", path.toString())
                                    .setMaxResults(1)
                                    .getSingleResult();
        }
        catch (NoResultException e) {
            Logger.debug("Searching for path but path not found");
        }
        catch (Exception e) {
            Logger.error(e);
        }

        return webpath;
    }

    public Map<String, WebPath> getPaths(URI masterPage)
    {
        // find the path for this language
        // if this path has statuscode 404, search if this path for another language has code 200
        // use this masterpageid
        Map<String, WebPath> retVal = new HashMap<>();
        try {
            List<DBPath> paths = R.requestContext().getEntityManager().createQuery("Select p from DBPath p where p.blockId = :id", DBPath.class).setParameter("id", masterPage.toString()).getResultList();
            for (DBPath path : paths) {
                retVal.put(path.getLanguage().getLanguage(), path);
            }
        }
        catch (NoResultException e) {
            Logger.debug("Searching for path but path not found");
        }
        catch (Exception e) {
            Logger.error(e);
        }

        return retVal;
    }

    @Override
    public Map<String, WebPath> getLanguagePaths(String pathName)
    {
        // find the path for this language
        // if this path has statuscode 404, search if this path for another language has code 200
        // use this masterpageid
        Map<String, WebPath> retVal = new HashMap<>();
        try {
            List<DBPath>
                            paths =
                            R.requestContext().getEntityManager().createQuery("Select p from DBPath p where p.localizedUrl = :pathName", DBPath.class).setParameter("pathName", pathName).getResultList();
            for (DBPath path : paths) {
                retVal.put(path.getLanguage().getLanguage(), path);
            }
        }
        catch (NoResultException e) {
            Logger.debug("Searching for path but path not found");
        }
        catch (Exception e) {
            Logger.error(e);
        }

        return retVal;
    }

    public WebPath getActivePath(Path path)
    {
        WebPath retVal = null;
        // search all urls with code 200 and return the first active language code
        // in the config file

        List<DBPath>
                        paths =
                        R.requestContext().getEntityManager().createQuery("Select p from DBPath p where p.url = :path and p.statusCode = 200", DBPath.class).setParameter("path", path.toString())
                                      .getResultList();
        Map<String, DBPath> pathMap = new HashMap<String, DBPath>();
        for (DBPath p : paths) {
            pathMap.put(p.getLanguage().getLanguage(), p);
        }
        // now find the best match for the page -> take the order of the languages in the config file
        for (Locale l : R.configuration().getLanguages().values()) {
            if (pathMap.containsKey(l.getLanguage())) {
                retVal = pathMap.get(l.getLanguage());
                break;
            }
        }

        return retVal;
    }

    public WebPath savePath(WebPath path) throws Exception
    {
        R.requestContext().getEntityManager().persist(path);
        addPathToLucene(path.getDBid(), path.toJson());
        return path;
    }

    public Resource getResource(URI id, Locale language) throws Exception
    {
        Resource retVal = null;
        DBResource dbResource = findResourceInDB(id);
        ;
        if (dbResource != null) {
            retVal = dbResource.getResource(language);
        }
        return retVal;

    }

    public Resource saveResource(Resource resource) throws Exception
    {
        DBResource dbResource = findResourceInDB(resource.getBlockId());

        if (dbResource == null) {
            dbResource = new DBResource(resource);
        }
        else {
            dbResource.setResource(resource);
        }

        R.requestContext().getEntityManager().persist(dbResource);
        Set<URI> types = resource.getRdfType();
        String type = "resource";
        if (types.iterator().hasNext())
            type = RdfTools.makeDbFieldFromUri(types.iterator().next());

        addResourceToLucene(dbResource.getId().toString(), type, ResourceFactoryImpl.instance().serializeResource(resource, false), Locale.ROOT);

        return resource;
    }

    public Resource deleteResource(Resource resource)
    {
        DBResource dbResource = findResourceInDB(resource.getBlockId());
        return null;
    }

    // ------- PRIVATE METHODS  ---------

    private DBResource findResourceInDB(URI id)
    {
        DBResource retVal = null;
        try {
            retVal = R.requestContext().getEntityManager().createQuery("select r FROM DBResource r where r.blockId = :id", DBResource.class)
                                   .setParameter("id", id.toString())
                                   .getSingleResult();
        }
        catch (NoResultException e) {
            Logger.debug("Searching for resource but resource not found");
        }
        catch (StackOverflowError e) {
            Logger.debug("StackOverflow error !");
        }

        return retVal;
    }

    private DBPage findPageInDB(URI id)
    {
        DBPage retVal = null;

        try {
            retVal = R.requestContext().getEntityManager().createQuery("select p FROM DBPage p where p.blockId = :id ORDER BY p.createdAt DESC", DBPage.class)
                                   .setParameter("id", id.toString())
                                   .setMaxResults(1)
                                   .getSingleResult();

        }
        catch (NoResultException e) {
            Logger.debug("Searching for resource but resource not found");
        }

        return retVal;
    }

    private void addResourceToLucene(String id, String type, String json, Locale locale)
    {
        String index = ElasticSearch.instance().getResourceIndexName(locale);

        ElasticSearch.instance().getBulk().add(ElasticSearch.instance().getClient().prepareIndex(index, type)
                                                            .setSource(json)
                                                            .setId(id).request());
    }

    private void addPageToLucene(String id, String json, Locale locale)
    {
        String name = PersistenceController.WEB_PAGE_CLASS;
        String index = ElasticSearch.instance().getPageIndexName(locale);
        ElasticSearch.instance().getBulk().add(ElasticSearch.instance().getClient().prepareIndex(index, name)
                                                            .setSource(json)
                                                            .setId(id).request());
        //        IndexResponse is = ElasticSearch.instance().getClient().prepareIndex(index, name)
        //                                        .setSource(json).setId(id)
        //                                        .execute().actionGet();
        //        if (!is.isCreated()) {
        //            Logger.error("Webpage could not be added to Lucene");
        //        }
    }

    private void addPathToLucene(String id, String json)
    {
        String name = PersistenceController.PATH_CLASS;
        String index = "routing";
        ElasticSearch.instance().getBulk().add(ElasticSearch.instance().getClient().prepareIndex(index, name)
                                                            .setSource(json)
                                                            .setId(id).request());
    }

    private void removePathFromLucene(String id)
    {
        String name = PersistenceController.PATH_CLASS;
        String index = "routing";
        ElasticSearch.instance().getClient().prepareDelete(index, name, id).execute().actionGet();
    }

    private void removePageFromLucene(String id)
    {
        String name = PersistenceController.WEB_PAGE_CLASS;
        for (Locale locale : R.configuration().getLanguages().values()) {
            String index = ElasticSearch.instance().getPageIndexName(locale);
            ElasticSearch.instance().getClient().prepareDelete(index, name, id).execute().actionGet();
        }

    }

}
