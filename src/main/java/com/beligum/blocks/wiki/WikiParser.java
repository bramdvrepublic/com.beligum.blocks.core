package com.beligum.blocks.wiki;

import com.beligum.base.cache.CacheKey;
import com.beligum.base.server.R;
import com.beligum.base.utils.Logger;
import com.beligum.blocks.config.BlocksConfig;
import com.beligum.blocks.config.ParserConstants;
import com.beligum.blocks.controllers.interfaces.PersistenceController;
import com.beligum.blocks.models.ResourceImpl;
import com.beligum.blocks.models.factories.ResourceFactoryImpl;
import com.beligum.blocks.models.interfaces.Node;
import com.beligum.blocks.models.interfaces.Resource;
import com.beligum.blocks.search.ElasticSearch;
import com.beligum.blocks.utils.RdfTools;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.util.AntPathMatcher;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.joda.time.LocalDateTime;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

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
    protected List<HashMap<String, HashMap<String,String>>> itemsList = new ArrayList<>();
    protected ObjectMapper mapper;
    protected int start;
    protected int length;

    BulkRequestBuilder bulkRequest;


    protected Integer counter = 0;

    public WikiParser(int start, int length) throws IOException
    {
        this.start = start;
        this.length = length;
        //        this.indexer = new SimpleIndexer(new File(BlocksConfig.instance().getLuceneIndex()));
        pathMatcher = new AntPathMatcher();

//        final SimpleModule module = new SimpleModule("customerSerializationModule", new Version(1, 0, 0, "static version"));
//        module.addSerializer(Resource.class, new ResourceJsonLDSerializer());
//
//        mapper = new ObjectMapper();
//        mapper.registerModule(module);

        R.cacheManager().getApplicationCache().put(getCacheKey(), new ArrayList<HashMap<String, Resource>>());
        this.bulkRequest = ElasticSearch.instance().getClient().prepareBulk();

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
                        itemsList.add(storedValue);
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
//        OBlocksDatabase.instance().getGraph().getRawGraph().declareIntent(new OIntentMassiveInsert());
        int count = 0;
        int listsize = itemsList.size();
        for (int index = start; index < start + length; index++) {
            // last item reached in list
            if (index >= listsize) break;

            HashMap<String, HashMap<String, String>> item = this.itemsList.get(index);
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
            HashMap<String, Resource> basicResources = changeEntity(resource);

            ((ArrayList<HashMap<String, Resource>>)R.cacheManager().getApplicationCache().get(getCacheKey())).add(basicResources);

//            for (Locale locale : LANGUAGES) {
//                createWebPages(basicResources.get(locale.getLanguage()), locale);
//            }


            count++;
            if (count % 100 == 0) {

                Logger.info("saving entity: " + count);
//                break;

            }


//            if (count > 200) break;

        }

        ElasticSearch.instance().saveBulk();
        Logger.info("finish!");

//


    }


    public abstract CacheKey getCacheKey();

    public abstract URI createId(HashMap<String, HashMap<String, String>> item);

    public abstract void makeEntity(URI id);

    public abstract Resource addEntity();

    public abstract HashMap<String, Resource> changeEntity(Resource entity);

    public abstract String getSimpleTypeName();

    public abstract String getTemplatename();

    public HashMap<String,Resource> createResources(HashMap<String,Resource> container, URI id, URI type)
    {
        container = new HashMap<>();
//                ODocument defaultVertex = OBlocksDatabase.instance().createDefaultVertex(id, type);
//                ODocument bnlVertex = OBlocksDatabase.instance().createLocalizedVertex(defaultVertex, BlocksConfig.instance().getLocaleForLanguage("nl"));
        //        ODocument bfrVertex = OBlocksDatabase.instance().createLocalizedVertex(defaultVertex, Locale.FRENCH);
        //        ODocument benVertex = OBlocksDatabase.instance().createLocalizedVertex(defaultVertex, Locale.ENGLISH);

        //        container.put("nl", new OrientResource(defaultVertex, bnlVertex));
        //        container.put("fr", new OrientResource(defaultVertex, bfrVertex));
        //        container.put("en", new OrientResource(defaultVertex, benVertex));

        HashMap<String, Object> defaultVertex = new HashMap<>();
        HashMap<String, Object> nlVertex = new HashMap<>();
        HashMap<String, Object> frVertex = new HashMap<>();
        HashMap<String, Object> enVertex = new HashMap<>();
        defaultVertex.put(ParserConstants.JSONLD_ID, id.toString());
        Set<String> types = new HashSet<String>();
        types.add(type.toString());
        defaultVertex.put(PersistenceController.RESOURCE_TYPE_FIELD, types);

        container.put("nl", new ResourceImpl(defaultVertex, nlVertex, BlocksConfig.instance().getDefaultLanguage()));
        container.put("fr", new ResourceImpl(defaultVertex, frVertex, Locale.FRENCH));
        container.put("en", new ResourceImpl(defaultVertex, enVertex, Locale.ENGLISH));

        return container;
    }


    public abstract Resource fillEntity(HashMap<String, HashMap<String, String>> item, Locale lang);

    public void splitField(Resource entity, String field, Locale locale) {
        URI name = RdfTools.createLocalType(field);
        Node property = entity.get(name);
        entity.remove(name);
        if (property != null && property.isIterable()) {
            for (Node value : property) {
                if (value.isString()) {
                    String[] splitValue = value.asString().split("\\|");
                    for (String v : splitValue) {
                        entity.add(name, ResourceFactoryImpl.instance().createNode(v.trim(), locale));
                    }
                }
            }
        } else if (property != null && property.isString()) {
            String[] splitValue = property.asString().split("\\|");
            for (String v : splitValue) {
                entity.add(name, ResourceFactoryImpl.instance().createNode(v.trim(), locale));
            }
        }

    }

    public void prependField(Resource entity, String field, String prefix, Locale locale) {
        URI name = RdfTools.createLocalType(field);
        Node property = entity.get(name);
        entity.remove(name);
        if (property != null && property.isIterable()) {
            for (Node node: property) {
                if (node.isString()) {
                    entity.add(name, ResourceFactoryImpl.instance().createNode(prefix + node.asString().trim(), locale));

                }
            }
        } else if (property.isString()) {
            entity.add(name, ResourceFactoryImpl.instance().createNode(prefix + property.asString().trim(), locale));
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
//            addToEntityOR(newFieldName, oldFields, entity, BlocksConfig.instance().getDefaultLanguage(), item, langTo);
        } else if (!StringUtils.isEmpty(value)) {
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
                    Date date = new Date(millis*1000);
                    Calendar cal = Calendar.getInstance();
                    cal.setTime(date);
                    entity.setCreatedAt(new LocalDateTime(cal.getTime()));
                } catch(Exception e) {
                    Logger.debug("Could not parse time", e);
                }
            } else {
                entity.add(RdfTools.makeLocalAbsolute(newFieldName), ResourceFactoryImpl.instance().createNode(value, language));
            }
        }
    }

    public abstract void createWebPages(Resource resource, Locale locale);

}
