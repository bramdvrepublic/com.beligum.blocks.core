package com.beligum.blocks.wiki;

import com.beligum.base.server.R;
import com.beligum.base.utils.Logger;
import com.beligum.blocks.config.BlocksConfig;
import com.beligum.blocks.models.resources.orient.OrientResourceController;
import com.beligum.blocks.models.resources.interfaces.Node;
import com.beligum.blocks.models.resources.interfaces.Resource;
import com.beligum.blocks.models.resources.jackson.ResourceJsonLDSerializer;
import com.beligum.blocks.pages.DefaultWebPageController;
import com.beligum.blocks.pages.WebPageParser;
import com.beligum.blocks.pages.ifaces.WebPage;
import com.beligum.blocks.routing.ORouteController;
import com.beligum.blocks.search.ElasticSearchClient;
import com.beligum.blocks.search.ElasticSearchServer;
import com.beligum.blocks.utils.RdfTools;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.util.AntPathMatcher;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.joda.time.DateTime;
import org.joda.time.LocalDateTime;

import javax.ws.rs.core.UriBuilder;
import java.io.*;
import java.net.URI;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;

/**
 * Created by wouter on 2/04/15.
 */
public abstract class WikiParser
{
    public static final Locale NL = BlocksConfig.instance().getLocaleForLanguage("nl");
    public static final Locale EN = Locale.ENGLISH;
    public static final Locale FR = Locale.FRENCH;

    public static final Locale[] LANGUAGES = new Locale[] {NL, FR, EN};

    protected AntPathMatcher pathMatcher;
    // Objects with fields and languages for fields
    protected HashMap<String, HashMap<String, HashMap<String,String>>> items = new HashMap<String, HashMap<String, HashMap<String,String>>>();
    protected HashMap<String, Resource> localizedResources;
    protected ArrayList<HashMap<String, Resource>> entities = new ArrayList<HashMap<String, Resource>>();
    protected ObjectMapper mapper;
    protected String blueprintId;

    BulkRequestBuilder bulkRequest;


    protected Integer counter = 0;

    public WikiParser() throws IOException
    {

        //        this.indexer = new SimpleIndexer(new File(BlocksConfig.instance().getLuceneIndex()));
        pathMatcher = new AntPathMatcher();

        final SimpleModule module = new SimpleModule("customerSerializationModule", new Version(1, 0, 0, "static version"));
        module.addSerializer(Resource.class, new ResourceJsonLDSerializer());

        mapper = new ObjectMapper();
        mapper.registerModule(module);

        this.bulkRequest = ElasticSearchClient.instance().getClient().prepareBulk();

    }

    public void parse(Path path)
    {
        FileVisitor<Path> visitor = new SimpleFileVisitor<Path>()
        {
            @Override
            public FileVisitResult visitFile(Path filePath, BasicFileAttributes attrs)
                            throws IOException
            {
                String path = filePath.toString();
                WikiItem item = new WikiItem();

                try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(path), "8859_1"))) {
                    String line;
                    Integer line_nr = 0;
                    while ((line = br.readLine()) != null) {
                        if (line_nr == 0 && !line.contains("pmwiki")) {
                            Logger.error("This is not a pmwiki file: " + path + ". Skip!");
                            return FileVisitResult.CONTINUE;
                        }
                        else if (line_nr > 0) {
                            int index = line.indexOf("=");
                            if (index > -1) {
                                item.addField(line.substring(0, index), line.substring(index + 1));
                            }
                            else if (!StringUtils.isEmpty(line)) {
                                Logger.error("Invalid line found in file: " + path + " ");
                            }

                        }

                        line_nr++;
                    }

                    if (item.isValid()) {
                        HashMap<String, HashMap<String, String>> storedValue = new HashMap<String, HashMap<String, String>>();
                        if (items.containsKey(item.getId())) {
                            storedValue = items.get(item.getId());
                        }
                        storedValue = item.addToData(storedValue);
                        items.put(item.getId(), storedValue);
                    }
                    else {
                        Logger.error("Item is not valid and will not be added");
                    }

                }
                catch (Exception e) {
                    Logger.error("Error while reading file " + path, e);
                }

                return FileVisitResult.CONTINUE;
            }
        };

        try {
            Files.walkFileTree(path, visitor);
        }
        catch (Exception e) {
            Logger.error("Error while walking tree", e);
        }
    }

    public void listFields()
    {
        HashSet<String> fieldNames = new HashSet<String>();
        for (String key : this.items.keySet()) {
            HashMap<String, HashMap<String, String>> item = this.items.get(key);
            for (String fieldKey : item.keySet()) {
                fieldNames.add(fieldKey);
            }
        }
        Logger.info("-----------Fields----------");
        for (Object field : fieldNames.toArray()) {
            if (!((String) field).contains("title")) {
                Logger.info((String) field);
            }
        }
    }

    public void createEntities()
    {

        int count = 0;
        ODatabaseDocumentTx graph = com.beligum.blocks.controllers.OrientResourceController.instance().getDatabase();
        for (String key : this.items.keySet()) {
            HashMap<String, HashMap<String, String>> item = this.items.get(key);
            // create a new id taken from a field of the item
            // this makes that we dont have to assign a random number as id
            URI id = createId(item);
            if (id == null) {
                Logger.warn("Resource skipped because no id was available");
                continue;
            }
            makeEntity(id);

            // Convert the hashmaps to a Resource Object (created by makeEntity)
            for (Locale language : LANGUAGES) {
                fillEntity(item, language);
            }

            // When multiple resources are filled e.g person and his address, we connect them here
            Resource resource = addEntity();

            // fix some fields
            changeEntity(resource);
            createWebPages(resource);
            //Save to DB
            //                    saveEntity(resource);
            OrientResourceController.instance().save(resource);
            toLucene();


            count++;
            if (count % 100 == 0) {
                //                        OrientGraph graph = (OrientGraph)OrientResourceFactory.instance().getDatabase();
                //                        graph.commit();
                //                        graph.begin();

                //                        ProtectedLazyEntityManager em = (ProtectedLazyEntityManager)RequestContext.getEntityManager();
                //                        em.getEntityManager().getTransaction().commit();

                //                        em.getEntityManager().clear();
                //                        em.getEntityManager().getTransaction().begin();
                Logger.info("saving entity: " + count);
                //                        break;

            }
        }

        graph.commit();
        Logger.debug("finish!");

        BulkResponse bulkResponse = this.bulkRequest.execute().actionGet();




    }



    public abstract URI createId(HashMap<String, HashMap<String, String>> item);

    public abstract void makeEntity(URI id);

    public abstract Resource addEntity();

    public abstract Resource changeEntity(Resource entity);

    public abstract String getSimpleTypeName();

    public abstract void toLucene();

    public abstract String getTemplatename();

    protected void addToLuceneIndex(String id, String json, Locale locale)
    {
        this.bulkRequest.add(ElasticSearchClient.instance().getClient().prepareIndex(ElasticSearchServer.instance().getResourceIndexName(locale), "resource")
                                                .setSource(json)
                                                .setId(id));
    }

    public abstract Resource fillEntity(HashMap<String, HashMap<String, String>> item, Locale lang);

    public void splitField(Resource entity, String field) {
        URI name = RdfTools.createLocalType(field);
        Node property = entity.get(name);
        entity.remove(name);
        if (property != null && property.isIterable()) {
            for (Node value : property) {
                if (value.isString()) {
                    String[] splitValue = value.asString().split("\\|");
                    for (String v : splitValue) {
                        entity.add(name, OrientResourceController.instance().asNode(v.trim(), BlocksConfig.instance().getDefaultLanguage()));
                    }
                }
            }
        } else if (property != null && property.isString()) {
            String[] splitValue = property.asString().split("\\|");
            for (String v : splitValue) {
                entity.add(name, OrientResourceController.instance().asNode(v.trim(), property.getLanguage()));
            }
        }

    }

    public void prependField(Resource entity, String field, String prefix) {
        URI name = RdfTools.createLocalType(field);
        Node property = entity.get(name);
        entity.remove(name);
        if (property != null && property.isIterable()) {
            for (Node node: property) {
                if (node.isString()) {
                    entity.add(name, OrientResourceController.instance().asNode(prefix + node.asString().trim(), property.getLanguage()));

                }
            }
        } else if (property.isString()) {
            entity.add(name, OrientResourceController.instance().asNode(prefix + property.asString().trim(), property.getLanguage()));
        }



    }

    public void addToEntity(String newFieldName, String[] oldFields, Resource entity, Locale language, HashMap<String, HashMap<String,String>> item, Locale langTo) {
        String value = null;
        for (String field : oldFields) {
            if (item.containsKey(field) && item.get(field).containsKey(language.getLanguage())) {
                if (value == null)
                    value = "";
                value += item.get(field).get(language.getLanguage()) + " ";
            }
        }
        if (value != null) value = value.trim();

        if (!StringUtils.isEmpty(value) && langTo.equals(Locale.ROOT) && language != BlocksConfig.instance().getDefaultLanguage()) {
            Logger.warn("Text added to root note by translation available for field " + newFieldName);
        } else if (langTo !=Locale.ROOT  && language != BlocksConfig.instance().getDefaultLanguage() && StringUtils.isEmpty(value)) {
            addToEntityOR(newFieldName, oldFields, entity, BlocksConfig.instance().getDefaultLanguage(), item, langTo);
        } else {
            add(newFieldName, value, entity, langTo);
        }
    }

    // Add the first filled field
    public void addToEntityOR(String newFieldName, String[] oldFields, Resource entity, Locale language, HashMap<String, HashMap<String,String>> item, Locale langTo) {
        String value = null;
        for (String field : oldFields) {
            if (item.containsKey(field) && item.get(field).containsKey(language.getLanguage())) {
                value = item.get(field).get(language.getLanguage()) + " ";
                break;
            }
        }

        if (!StringUtils.isEmpty(value) && langTo.equals(Locale.ROOT) && language != BlocksConfig.instance().getDefaultLanguage()) {
            Logger.warn("Text added to root note by translation available for field " + newFieldName);
        } else if (langTo !=Locale.ROOT  && language != BlocksConfig.instance().getDefaultLanguage() && StringUtils.isEmpty(value)) {
            addToEntityOR(newFieldName, oldFields, entity, BlocksConfig.instance().getDefaultLanguage(), item, langTo);
        } else {
            add(newFieldName, value, entity, langTo);
        }
    }

    public void addToEntityJoined(String newFieldName, String[] oldFields, Resource entity, Locale language, HashMap<String, HashMap<String,String>> item, String joint, Locale langTo) {
        String value = null;
        for (String field : oldFields) {
            if (item.containsKey(field) && item.get(field).containsKey(language.getLanguage())) {
                if (value == null)
                    value = "";
                else
                    value += joint + " ";
                value += item.get(field).get(language.getLanguage());
            }
        }

        if (!StringUtils.isEmpty(value) && langTo.equals(Locale.ROOT) && language != BlocksConfig.instance().getDefaultLanguage()) {
            Logger.warn("Text added to root note by translation available for field " + newFieldName);
        } else if (langTo !=Locale.ROOT  && language != BlocksConfig.instance().getDefaultLanguage() && StringUtils.isEmpty(value)) {
            addToEntityOR(newFieldName, oldFields, entity, BlocksConfig.instance().getDefaultLanguage(), item, langTo);
        } else {
            add(newFieldName, value, entity, langTo);
        }
    }


    public void add(String newFieldName, String value, Resource entity, Locale language) {
        if (!StringUtils.isEmpty(value)) {
            value = value.trim();
            if (newFieldName.equals("createdBy")) {
                entity.setCreatedBy(value);
            } else if (newFieldName.equals("createdAt")) {
                try {
                    Long millis = Long.parseLong(value);
                    DateTime c = new LocalDateTime(millis*1000).toDateTime();
                    entity.setCreatedAt(c.toDate());
                } catch(Exception e) {
                    Logger.debug("Could not parse time", e);
                }
            } else {
                entity.add(RdfTools.makeAbsolute(newFieldName), OrientResourceController.instance().asNode(value, language));
            }
        }
    }

    public WebPage createWebPages(Resource resource) {
        StringBuilder sb = new StringBuilder();
        sb.append("<main-content><div property=\"body-html\">").append("<").append(getTemplatename()).append(" property=\"waterput\" resource='").append(resource.getBlockId()).append("' ></").append(getTemplatename()).append(">").append("</div></main-content>");
        String html = sb.toString();
        String source = R.templateEngine().getNewStringTemplate(html).render();

        WebPage page = DefaultWebPageController.instance().createPage(BlocksConfig.instance().getDefaultLanguage());
        URI url = UriBuilder.fromUri(BlocksConfig.instance().getSiteDomain()).path("/nl/resource/waterput/" + resource.get("http://www.mot.be/ontology/name")).build();
        WebPageParser parser = null;
        try {
            parser = new WebPageParser(page, url, source, ORouteController.instance());
        } catch (Exception e) {
            Logger.error("Problem creating Web page", e);
        }
        return page;
    }


}
