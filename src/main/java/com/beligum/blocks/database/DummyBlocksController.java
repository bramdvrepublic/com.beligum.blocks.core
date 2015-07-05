package com.beligum.blocks.database;


import com.beligum.base.server.RequestContext;
import com.beligum.base.utils.Logger;
import com.beligum.blocks.config.BlocksConfig;
import com.beligum.blocks.config.ParserConstants;
import com.beligum.blocks.database.interfaces.BlocksController;
import com.beligum.blocks.pages.WebPageImpl;
import com.beligum.blocks.pages.ifaces.WebPage;
import com.beligum.blocks.resources.dummy.DummyNode;
import com.beligum.blocks.resources.dummy.DummyResource;
import com.beligum.blocks.resources.interfaces.DocumentInfo;
import com.beligum.blocks.resources.interfaces.Node;
import com.beligum.blocks.resources.interfaces.Resource;
import com.beligum.blocks.resources.jackson.path.PathDeserializer;
import com.beligum.blocks.resources.jackson.path.PathSerializer;
import com.beligum.blocks.resources.sql.DBPage;
import com.beligum.blocks.resources.sql.DBPath;
import com.beligum.blocks.resources.sql.DBResource;
import com.beligum.blocks.routing.ifaces.WebPath;
import com.beligum.blocks.search.ElasticSearchClient;
import com.beligum.blocks.search.ElasticSearchServer;
import com.beligum.blocks.utils.RdfTools;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.QueryBuilder;
import org.joda.time.LocalDateTime;

import javax.persistence.NoResultException;
import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.*;

/**
 * Created by wouter on 22/06/15.
 */
public class DummyBlocksController implements BlocksController
{
    private static final TimeZone UTC = TimeZone.getTimeZone("UTC");
    private ObjectMapper pathMapper = new ObjectMapper().registerModule(new SimpleModule().addSerializer(DBPath.class, new PathSerializer()).addDeserializer(DBPath.class, new PathDeserializer()));


    private static DummyBlocksController instance;

    private DummyBlocksController() {

    }

    public static DummyBlocksController instance() {
        if (DummyBlocksController.instance == null) {
            DummyBlocksController.instance = new DummyBlocksController();
        }
        return DummyBlocksController.instance;
    }

    @Override
    public WebPage createWebPage(URI masterWebPage, URI id, Locale locale)
    {
        return new WebPageImpl(masterWebPage, id, locale);
    }

    @Override
    public WebPage getWebPage(URI masterWebPage, Locale locale) throws IOException
    {
        QueryBuilder query = QueryBuilders.boolQuery().must(QueryBuilders.matchQuery("master_page", masterWebPage.toString())).must(QueryBuilders.matchQuery(ParserConstants.JSONLD_LANGUAGE, locale.getLanguage()));
        SearchResponse searchResponse = ElasticSearchClient.instance().getClient().prepareSearch(ElasticSearchServer.instance().getPageIndexName(locale)).setQuery(query).execute().actionGet();

        WebPage retVal = null;
        if (searchResponse.getHits().getHits().length > 0) {
            retVal = WebPageImpl.pageMapper.readValue(searchResponse.getHits().getHits()[0].getSourceAsString(), WebPage.class);
        }

        return retVal;
    }

    public DBPage getWebPageDB(URI masterWebPage, Locale locale) throws IOException
    {
        DBPage page = null;
        try {
            page = RequestContext.getEntityManager().createQuery("select p FROM DBPage p where p.masterPage = :master AND p.language = :language", DBPage.class)
                                 .setParameter("master", masterWebPage.toString()).setParameter("language", locale.getLanguage()).getSingleResult();
        } catch (NoResultException e) {
            Logger.debug("No webpage found in DB");
        } catch (Exception e) {
            Logger.error(e);
        }

        return page;
    }

    @Override
    public WebPage getWebPage(URI id)
    {
        return null;
    }

    @Override
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

    @Override
    public WebPage saveWebPage(WebPage webPage, boolean doVersion) throws IOException
    {
        DBPage dbPage = null;
        dbPage = getWebPageDB(webPage.getMasterpageId(), webPage.getLanguage());

        this.touch(webPage);
        if (dbPage == null) {
            dbPage = new DBPage(webPage);
        } else {
            dbPage.setWebPage(webPage);
        }
        RequestContext.getEntityManager().persist(dbPage);

        addPageToLucene(dbPage.getId().toString(), webPage.toJson(), webPage.getLanguage());
        return webPage;
    }

    @Override
    public WebPath getPath(URI masterPage, Locale locale) {
        return null;
    }

    @Override
    public WebPath getPath(Path path, Locale locale) {
        // find the path for this language
        // if this path has statuscode 404, search if this path for another language has code 200
        // use this masterpageid
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


    @Override
    public WebPath createPath(URI masterPage, Path path, Locale locale) throws JsonProcessingException
    {
        WebPath webPath = null;
        WebPath retVal = null;
        Long count = RequestContext.getEntityManager().createQuery("select count(p) from DBPath p where p.url = :path and p.language = :language", Long.class).setParameter(
                        "path", path.toString()).setParameter("language", locale.getLanguage()).getSingleResult();


        if (count.equals(0L)) {
            // this path does not yet exist for this getLanguage, so we can create it
            webPath = new DBPath(masterPage, path, locale);

            RequestContext.getEntityManager().persist(webPath);
            // save to lucene

            addPathToLucene(webPath.getDBid(), pathMapper.writeValueAsString(webPath));

            retVal = webPath;

            // now add paths for other languages
            for (Locale l: BlocksConfig.instance().getLanguages().values()) {
                if (!l.equals(locale)) {
                    //TODO: implement better localized paths
                    // find the subpath for this language based our current path
                    // check if this path exists, if not create

                    // TODO: this should be removed when feature above is implemented
                    // for now we add paths for all languages
                    count = RequestContext.getEntityManager().createQuery("select count(p) from DBPath p where p.url = :path and p.language = :language", Long.class).setParameter(
                                    "path", path.toString()).setParameter("language", l.getLanguage()).getSingleResult();
                    if (count.equals(0L)) {
                        webPath = new DBPath(masterPage, path, l);
                        RequestContext.getEntityManager().persist(webPath);
                        addPathToLucene(webPath.getDBid(), pathMapper.writeValueAsString(webPath));
                    }
                }
            }
        } else {
            retVal = webPath;
        }

        return retVal;
    }

    @Override
    public WebPath savePath(WebPath path) throws Exception
    {
        RequestContext.getEntityManager().persist(path);
        addPathToLucene(path.getDBid(), pathMapper.writeValueAsString(path));
        return path;
    }

    @Override
    public Resource createResource(URI id, URI rdfType, Locale language)
    {
        HashMap<String, Object> vertex = new HashMap<String, Object>();
        HashMap<String, Object> localized = new HashMap<String, Object>();
        Resource retVal = new DummyResource(vertex, localized, language);
        retVal.setBlockId(id);
        Set<URI> typeSet = new HashSet<URI>();
        typeSet.add(rdfType);
        retVal.setRdfType(typeSet);

        return retVal;
    }


    @Override
    public Resource getResource(URI id, Locale language) throws Exception
    {
        Resource retVal = null;
        DBResource dbResource = findResourceInDB(id);;
        if (dbResource != null) {
            retVal = dbResource.getResource(language);
        }
        return retVal;

    }

    @Override
    public Resource saveResource(Resource resource) throws Exception
    {
        // TODO: this shoul be removed. We don't want webpage resources here.

        if (resource.getRdfType().contains(new URI("http://www.mot.be/ontology/Webpage"))) return null;
        DBResource dbResource = null;

        dbResource = findResourceInDB(resource.getBlockId());

        if (dbResource == null) {
            dbResource = new DBResource(resource);
        } else {
            dbResource.setResource(resource);
        }
        this.touch(resource);
        RequestContext.getEntityManager().persist(dbResource);

        Set<URI> types = resource.getRdfType();
        String type = "resource";
        if (types.iterator().hasNext()) type = RdfTools.makeDbFieldFromUri(types.iterator().next());

        addResourceToLucene(dbResource.getId().toString(), type, resource.toJson(), resource.getLanguage());
        return resource;
    }

    @Override
    public Resource deleteResource(Resource resource)
    {
        return null;
    }

    @Override
    public Node createNode(Object value, Locale language)
    {
        Node retVal = null;
        if (value instanceof Resource || value instanceof Node) {
            retVal = (Node)value;
        } else {
            if (value instanceof List && ((List)value).size() == 2
                && ((List)value).get(0) instanceof HashMap
                && ((HashMap<String, Object>)((List)value).get(0)).containsKey(ParserConstants.JSONLD_ID)) {
                HashMap<String, Object> rootVector = (HashMap<String, Object>) ((List) value).get(0);
                HashMap<String, Object> localVector = (HashMap<String, Object>) ((List) value).get(1);

                retVal = new DummyResource(rootVector, localVector, language);

//            } else if (value instanceof List && ((List)value).size() == 1
//                       && ((List)value).get(0) instanceof List
//                       && ((HashMap<String, Object>)((List)((List)value).get(0)).get(0)).containsKey(ParserConstants.JSONLD_ID)) {
//
//                HashMap<String, Object> rootVector = (HashMap<String, Object>) ((List)((List) value).get(0)).get(0);
//                HashMap<String, Object> localVector = (HashMap<String, Object>) ((List)((List) value).get(0)).get(1);
//
//                if (localVector.isEmpty() && rootVector.values().size() == 2) {
//                    // get this resource from the db
//                    try {
//                        retVal = DummyBlocksController.instance().getResource(UriBuilder.fromUri((String) rootVector.get(ParserConstants.JSONLD_ID)).build(), language);
//                    }
//                    catch (Exception e) {
//                        Logger.error("Could not fetch resource as child of parent resource");
//                        retVal = new DummyResource(rootVector, localVector, language);
//                    }
//                } else {
//                    retVal = createNode(((List)value).get(0), language);
//                }

//            } else if (value instanceof List && ((List)value).size() == 1 && ((List)value).get(0) instanceof Map && ((Map)((List)value).get(0)).containsKey(ParserConstants.JSONLD_ID) && ((Map)((List)value).get(0)).values().size() == 1) {
//                try {
//                    retVal = DummyBlocksController.instance().getResource(UriBuilder.fromUri((String)((Map)((List)value).get(0)).get(ParserConstants.JSONLD_ID)).build(), language);
//                }
//                catch (Exception e) {
//                    Logger.error("Could not fetch resource as child of parent resource");
//                    // TODO how to catch this?
//                }
            } else if (value instanceof Map && ((Map)value).containsKey(ParserConstants.JSONLD_ID)) {
                try {
                    retVal = DummyBlocksController.instance().getResource(UriBuilder.fromUri((String)((Map)value).get(ParserConstants.JSONLD_ID)).build(), language);
                }
                catch (Exception e) {
                    Logger.error("Could not fetch resource as child of parent resource");
                    // TODO how to catch this?
                }
//                retVal = new DummyResource((Map)value, new HashMap<String, Object>(), language);
            } else {
                retVal = new DummyNode(value, language);
            }
        }

        return retVal;
    }


    private DocumentInfo touch(DocumentInfo resource) {
        Calendar c = Calendar.getInstance();
        c.setTimeZone(UTC);
        if (resource.getCreatedAt() == null) {
            resource.setCreatedAt(new LocalDateTime(c.getTime()));
        }

        if (resource.getCreatedBy() == null) {
            resource.setCreatedBy(BlocksConfig.instance().getCurrentUserName());
        }

        resource.setUpdatedAt(new LocalDateTime(c.getTime()));
        resource.setUpdatedBy(BlocksConfig.instance().getCurrentUserName());
        return resource;
    }


    private void addResourceToLucene(String id, String type, String json, Locale locale) {
        //        String index = ElasticSearchServer.instance().getResourceIndexName(locale);
        //
        //        ElasticSearchServer.instance().getBulk().add(ElasticSearchClient.instance().getClient().prepareIndex(index, type)
        //                                                                        .setSource(json)
        //                                                                        .setId(id).request());

    }

    private void addPageToLucene(String id, String json, Locale locale) {

        //        String name = BlocksController.webpage;
        //        String index = ElasticSearchServer.instance().getPageIndexName(locale);
        //        ElasticSearchServer.instance().getBulk().add(ElasticSearchClient.instance().getClient().prepareIndex(index, name)
        //                                                                        .setSource(json)
        //                                                                        .setId(id).request());
        //        IndexResponse is = ElasticSearchClient.instance().getClient().prepareIndex(index, name)
        //                           .setSource(json).setId(id)
        //                           .execute().actionGet();
        //        if (!is.isCreated()) {
        //            Logger.error("Webpage could not be added to Lucene");
        //        }
    }

    private void addPathToLucene(String id, String json) {

        //        String name = BlocksController.path;
        //        String index = "routing";
        //        ElasticSearchClient.instance().getClient().prepareIndex(index, name)
        //                           .setSource(json)
        //                           .setId(id).execute().actionGet();
    }

    private void removePathFromLucene(String id) {
        String name = BlocksController.path;
        String index = "routing";
        ElasticSearchClient.instance().getClient().prepareDelete(index, name, id).execute().actionGet();
    }

    private void removePageFromLucene(String id) {
        String name = BlocksController.webpage;
        for (Locale locale: BlocksConfig.instance().getLanguages().values()) {
            String index = ElasticSearchServer.instance().getPageIndexName(locale);
            ElasticSearchClient.instance().getClient().prepareDelete(index, name, id).execute().actionGet();
        }

    }


    private DBResource findResourceInDB(URI id) {
        DBResource retVal = null;
        try {
            retVal = RequestContext.getEntityManager().createQuery("select r FROM DBResource r where r.blockId = :id", DBResource.class).setParameter("id", id.toString()).getSingleResult();
        } catch (NoResultException e) {
            Logger.debug("Searching for resource but resource not found");
        } catch (Exception e) {
            Logger.error(e);
        }
        return retVal;
    }


}
