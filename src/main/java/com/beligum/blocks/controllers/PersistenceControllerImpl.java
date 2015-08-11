package com.beligum.blocks.controllers;


import com.beligum.base.server.RequestContext;
import com.beligum.base.utils.Logger;
import com.beligum.blocks.config.BlocksConfig;
import com.beligum.blocks.config.ParserConstants;
import com.beligum.blocks.controllers.interfaces.PersistenceController;
import com.beligum.blocks.models.WebPageImpl;
import com.beligum.blocks.models.interfaces.WebPage;
import com.beligum.blocks.models.interfaces.Resource;
import com.beligum.blocks.models.sql.DBPage;
import com.beligum.blocks.models.sql.DBPath;
import com.beligum.blocks.models.sql.DBResource;
import com.beligum.blocks.models.interfaces.WebPath;
import com.beligum.blocks.search.ElasticSearch;
import com.beligum.blocks.security.Permissions;
import com.beligum.blocks.utils.RdfTools;
import org.apache.shiro.SecurityUtils;
import org.elasticsearch.action.index.IndexResponse;
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
public class PersistenceControllerImpl  implements PersistenceController
{



    private static PersistenceControllerImpl instance;

    private PersistenceControllerImpl() {

    }

    public static PersistenceControllerImpl instance() {
        if (PersistenceControllerImpl.instance == null) {
            PersistenceControllerImpl.instance = new PersistenceControllerImpl();
        }
        return PersistenceControllerImpl.instance;
    }

    public WebPage getWebPage(URI masterWebPage, Locale locale) throws IOException
    {
        WebPage retVal = null;
        // When user can not modify a page we get the page from our ES cluster
        // Because ES only updates every second, we have to take the page from the real DB
        // when user can modify the page
        if (!SecurityUtils.getSubject().isPermitted(Permissions.ENTITY_MODIFY)) {
            QueryBuilder query = QueryBuilders
                            .boolQuery().must(QueryBuilders.matchQuery("master_page", masterWebPage.toString())).must(QueryBuilders.matchQuery(ParserConstants.JSONLD_LANGUAGE, locale.getLanguage()));
            SearchResponse searchResponse = ElasticSearch.instance().getClient().prepareSearch(ElasticSearch.instance().getPageIndexName(locale)).setQuery(query).execute().actionGet();

            if (searchResponse.getHits().getHits().length > 0) {
                retVal = WebPageImpl.pageMapper.readValue(searchResponse.getHits().getHits()[0].getSourceAsString(), WebPage.class);
            }
        }

        // If page is not found, double check in DB
        if (retVal == null) {
            DBPage page = findPageInDB(masterWebPage, locale);
            if (page != null) {
                retVal = page.getWebPage();
            }
        }

        return retVal;
    }



    public WebPage saveWebPage(WebPage webPage, boolean doVersion) throws IOException
    {
        DBPage dbPage = findPageInDB(webPage.getMasterpageId(), webPage.getLanguage());

        if (dbPage == null) {
            dbPage = new DBPage(webPage);
        } else {
            dbPage.setWebPage(webPage);
        }
        RequestContext.getEntityManager().persist(dbPage);

        addPageToLucene(dbPage.getId().toString(), webPage.toJson(), webPage.getLanguage());
        return webPage;
    }

    public void deleteWebPage(URI masterPage)
    {
        // Do not remove the page but mark the path as not found
        List<DBPath> paths = RequestContext.getEntityManager().createQuery("select p FROM DBPath p where p.masterPage = :master", DBPath.class).setParameter("master", masterPage.toString()).getResultList();
        RequestContext.getEntityManager().createQuery("update DBPath p SET p.statusCode = 404 where p.masterPage = :master").setParameter("master", masterPage.toString())
                      .executeUpdate();
        List<Long> pages = RequestContext.getEntityManager().createQuery("select p.id FROM DBPage p where p.masterPage = :master", Long.class).setParameter("master", masterPage.toString()).getResultList();

        for (DBPath path: paths) {
            removePathFromLucene(path.getId().toString());
        }

        for (Long id: pages) {
            removePageFromLucene(id.toString());
        }
    }


    public WebPath getPath(Path path, Locale locale) {
        // find the path for this language
        DBPath webpath = null;
        try {
            webpath = RequestContext.getEntityManager().createQuery("Select p from DBPath p where p.url = :path and p.language = :language", DBPath.class).setParameter("language", locale.getLanguage()).setParameter(
                            "path", path.toString()).getSingleResult();
        } catch (NoResultException e) {
            Logger.debug("Searching for path but path not found");
        } catch (Exception e) {
            Logger.error(e);
        }

        return webpath;
    }

    public Map<String, WebPath> getPaths(URI masterPage) {
        // find the path for this language
        // if this path has statuscode 404, search if this path for another language has code 200
        // use this masterpageid
        Map<String, WebPath> retVal = new HashMap<>();
        try {
            List<DBPath> paths = RequestContext.getEntityManager().createQuery("Select p from DBPath p where p.masterPage = :masterPage", DBPath.class).setParameter("masterPage", masterPage.toString()).getResultList();
            for (DBPath path: paths) {
                retVal.put(path.getLanguage().getLanguage(), path);
            }
        } catch (NoResultException e) {
            Logger.debug("Searching for path but path not found");
        } catch (Exception e) {
            Logger.error(e);
        }

        return retVal;
    }
    @Override
    public boolean pathExists(Path path, Locale locale)
    {
        boolean retVal = false;
        Long count = RequestContext.getEntityManager().createQuery("select count(p) from DBPath p where p.url = :path and p.language = :language", Long.class).setParameter(
                        "path", path.toString()).setParameter("language", locale.getLanguage()).getSingleResult();
        if (count > 0) {
            retVal = true;
        }
        return retVal;
    }

    public WebPath getActivePath(Path path) {
        WebPath retVal = null;
        // search all urls with code 200 and return the first active language code
        // in the config file

        List<DBPath> paths = RequestContext.getEntityManager().createQuery("Select p from DBPath p where p.url = :path and p.statusCode = 200", DBPath.class).setParameter("path", path.toString()).getResultList();
        Map<String, DBPath> pathMap = new HashMap<String, DBPath>();
        for (DBPath p: paths) {
            pathMap.put(p.getLanguage().getLanguage(), p);
        }
        // now find the best match for the page -> take the order of the languages in the config file
        for (Locale l: BlocksConfig.instance().getLanguages().values()) {
            if (pathMap.containsKey(l.getLanguage())) {
                retVal = pathMap.get(l.getLanguage());
                break;
            }
        }

        return retVal;
    }


    public WebPath savePath(WebPath path) throws Exception
    {
        RequestContext.getEntityManager().persist(path);
        addPathToLucene(path.getDBid(), path.toJson());
        return path;
    }



    public Resource getResource(URI id, Locale language) throws Exception
    {
        Resource retVal = null;
        DBResource dbResource = findResourceInDB(id);;
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
        } else {
            dbResource.setResource(resource);
        }

        RequestContext.getEntityManager().persist(dbResource);
        Set<URI> types = resource.getRdfType();
        String type = "resource";
        if (types.iterator().hasNext()) type = RdfTools.makeDbFieldFromUri(types.iterator().next());

        addResourceToLucene(dbResource.getId().toString(), type, resource.toJson(), resource.getLanguage());

        // if a root value was updated, update all localized resources in elasticsearch
        // TODO: unccommeny. Commented for batch upload resources
//        if (!dbResource.hasUpdatedRoot()) {
//            addResourceToLucene(dbResource.getId().toString(), type, resource.toJson(), resource.getLanguage());
//        } else {
//            for (Locale locale: dbResource.getLanguages()) {
//                addResourceToLucene(dbResource.getId().toString(), type, dbResource.getResource(locale).toJson(), locale);
//            }
//        }

        return resource;
    }


    public Resource deleteResource(Resource resource)
    {
        DBResource dbResource = findResourceInDB(resource.getBlockId());
        return null;
    }

    // ------- PRIVATE METHODS  ---------


    private DBResource findResourceInDB(URI id) {
        DBResource retVal = null;
        try {
            retVal = RequestContext.getEntityManager().createQuery("select r FROM DBResource r where r.blockId = :id", DBResource.class).setParameter("id", id.toString()).getSingleResult();
        } catch (NoResultException e) {
            Logger.debug("Searching for resource but resource not found");
        }
        return retVal;
    }

    private DBPage findPageInDB(URI masterWebPage, Locale locale) {
        DBPage retVal = null;
        try {
            retVal = RequestContext.getEntityManager().createQuery("select p FROM DBPage p where p.masterPage = :master AND p.language = :language", DBPage.class)
                                        .setParameter("master", masterWebPage.toString()).setParameter("language", locale.getLanguage()).getSingleResult();

        } catch (NoResultException e) {
            Logger.debug("Searching for resource but resource not found");
        }
        return retVal;
    }


    private void addResourceToLucene(String id, String type, String json, Locale locale) {
        String index = ElasticSearch.instance().getResourceIndexName(locale);

        ElasticSearch.instance().getBulk().add(ElasticSearch.instance().getClient().prepareIndex(index, type)
                                                            .setSource(json)
                                                            .setId(id).request());

    }

    private void addPageToLucene(String id, String json, Locale locale) {

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

    private void addPathToLucene(String id, String json) {

        String name = PersistenceController.PATH_CLASS;
        String index = "routing";
        ElasticSearch.instance().getClient().prepareIndex(index, name)
                     .setSource(json)
                     .setId(id).execute().actionGet();
    }

    private void removePathFromLucene(String id) {
        String name = PersistenceController.PATH_CLASS;
        String index = "routing";
        ElasticSearch.instance().getClient().prepareDelete(index, name, id).execute().actionGet();
    }

    private void removePageFromLucene(String id) {
        String name = PersistenceController.WEB_PAGE_CLASS;
        for (Locale locale: BlocksConfig.instance().getLanguages().values()) {
            String index = ElasticSearch.instance().getPageIndexName(locale);
            ElasticSearch.instance().getClient().prepareDelete(index, name, id).execute().actionGet();
        }

    }


}
