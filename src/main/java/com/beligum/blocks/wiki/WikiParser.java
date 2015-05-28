package com.beligum.blocks.wiki;

import com.beligum.base.server.RequestContext;
import com.beligum.base.utils.Logger;
import com.beligum.blocks.base.Blocks;
import com.beligum.blocks.models.jsonld.OrientResourceFactory;
import com.beligum.blocks.models.jsonld.interfaces.Node;
import com.beligum.blocks.models.jsonld.interfaces.Resource;
import com.beligum.blocks.models.jsonld.jackson.ResourceJsonLDSerializer;
import com.beligum.blocks.search.SimpleIndexer;
import com.beligum.blocks.utils.UrlTools;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.orientechnologies.orient.core.db.document.ODatabaseDocument;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.tinkerpop.blueprints.impls.orient.OrientGraph;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.util.AntPathMatcher;
import org.joda.time.LocalDateTime;

import java.io.*;
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
    public static final Locale NL = Blocks.config().getLocaleForLanguage("nl");
    public static final Locale EN = Locale.ENGLISH;
    public static final Locale FR = Locale.FRENCH;

    public static final Locale[] LANGUAGES = new Locale[] {NL, FR, EN};

    protected SimpleIndexer indexer;
    protected AntPathMatcher pathMatcher;
    // Objects with fields and languages for fields
    protected HashMap<String, HashMap<String, HashMap<String,String>>> items = new HashMap<String, HashMap<String, HashMap<String,String>>>();
    protected HashMap<String, Resource> localizedResources;
    protected ArrayList<HashMap<String, Resource>> entities = new ArrayList<HashMap<String, Resource>>();
    protected ObjectMapper mapper;
    protected String blueprintId;


    protected Integer counter = 0;

    public WikiParser() throws IOException
    {

//        this.indexer = new SimpleIndexer(new File(Blocks.config().getLuceneIndex()));
        pathMatcher = new AntPathMatcher();
        try {
            indexer = new SimpleIndexer(new File(Blocks.config().getLuceneIndex()));
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        final SimpleModule module = new SimpleModule("customerSerializationModule", new Version(1, 0, 0, "static version"));
        module.addSerializer(Resource.class, new ResourceJsonLDSerializer());

        mapper = new ObjectMapper();
        mapper.registerModule(module);

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
        try {
            try {
                int count = 0;
                ODatabaseDocumentTx graph = OrientResourceFactory.instance().getGraph();
                for (String key : this.items.keySet()) {
                    HashMap<String, HashMap<String, String>> item = this.items.get(key);
                    // create a new id taken from a field of the item
                    // this makes that we dont have to assign a random number as id
                    String id = createId(item);
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

                    //Save to DB
//                    saveEntity(resource);
                    OrientResourceFactory.instance().save(resource);

                    count++;
                    if (count % 100 == 0) {
//                        OrientGraph graph = (OrientGraph)OrientResourceFactory.instance().getGraph();
//                        graph.commit();
//                        graph.begin();

//                        ProtectedLazyEntityManager em = (ProtectedLazyEntityManager)RequestContext.getEntityManager();
//                        em.getEntityManager().getTransaction().commit();

//                        em.getEntityManager().clear();
//                        em.getEntityManager().getTransaction().begin();
                        Logger.info("saving entity: " + count);
                        break;

                    }
                }

                graph.commit();
                Logger.debug("finish!");
                indexer.commit();
            }
            catch (Exception e) {
                Logger.error(e);
            } finally {
                if (indexer != null) indexer.close();
            }

        }
        catch (IOException e) {
            e.printStackTrace();
        }


    }

    public void addDocument(String id, Resource resource) throws IOException
    {
        String content = mapper.writeValueAsString(resource);
        indexer.addDocument(id, content);
    }

    public abstract String createId(HashMap<String, HashMap<String, String>> item);

    public abstract void makeEntity(String id);

    public abstract Resource addEntity();

    public abstract Resource changeEntity(Resource entity);

    public abstract String getSimpleTypeName();

    public void saveEntity(Resource resource)
    {



    }

    public abstract Resource fillEntity(HashMap<String, HashMap<String, String>> item, Locale lang);

    public void splitField(Resource entity, String field) {
        String name = UrlTools.createLocalType(field);
        Node property = entity.get(name);
        entity.remove(name);
        if (property != null && property.isIterable()) {
            for (Node value : property.getIterable()) {
                if (value.isString()) {
                    String[] splitValue = value.asString().split("\\|");
                    for (String v : splitValue) {
                        entity.add(field, OrientResourceFactory.instance().asNode(v.trim(), Blocks.config().getDefaultLanguage()));
                    }
                }
            }
        } else if (property != null && property.isString()) {
            String[] splitValue = property.asString().split("\\|");
            for (String v : splitValue) {
                entity.add(field, OrientResourceFactory.instance().asNode(v.trim(), property.getLanguage()));
            }
        }

    }

    public void prependField(Resource entity, String field, String prefix) {
        String name = UrlTools.createLocalType(field);
        Node property = entity.get(name);
        entity.remove(name);
        if (property != null && property.isIterable()) {
            for (Node node: property.getIterable()) {
                if (node.isString()) {
                    entity.add(field, OrientResourceFactory.instance().asNode(prefix + node.asString().trim(), property.getLanguage()));

                }
            }
        } else if (property.isString()) {
            entity.add(field, OrientResourceFactory.instance().asNode(prefix + property.asString().trim(), property.getLanguage()));
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

        if (!StringUtils.isEmpty(value) && langTo.equals(Locale.ROOT) && language != Blocks.config().getDefaultLanguage()) {
            Logger.warn("Text added to root note by translation available for field " + newFieldName);
        } else if (langTo !=Locale.ROOT  && language != Blocks.config().getDefaultLanguage() && StringUtils.isEmpty(value)) {
            addToEntityOR(newFieldName, oldFields, entity, Blocks.config().getDefaultLanguage(), item, langTo);
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

        if (!StringUtils.isEmpty(value) && langTo.equals(Locale.ROOT) && language != Blocks.config().getDefaultLanguage()) {
            Logger.warn("Text added to root note by translation available for field " + newFieldName);
        } else if (langTo !=Locale.ROOT  && language != Blocks.config().getDefaultLanguage() && StringUtils.isEmpty(value)) {
            addToEntityOR(newFieldName, oldFields, entity, Blocks.config().getDefaultLanguage(), item, langTo);
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

        if (!StringUtils.isEmpty(value) && langTo.equals(Locale.ROOT) && language != Blocks.config().getDefaultLanguage()) {
            Logger.warn("Text added to root note by translation available for field " + newFieldName);
        } else if (langTo !=Locale.ROOT  && language != Blocks.config().getDefaultLanguage() && StringUtils.isEmpty(value)) {
            addToEntityOR(newFieldName, oldFields, entity, Blocks.config().getDefaultLanguage(), item, langTo);
        } else {
            add(newFieldName, value, entity, langTo);
        }
    }


    public void add(String newFieldName, String value, Resource entity, Locale language) {
        if (!StringUtils.isEmpty(value)) {
            value = value.trim();
            if (newFieldName.equals("createdBy")) {
                entity.add("createdBy", OrientResourceFactory.instance().asNode(value, language));
            } else if (newFieldName.equals("createdAt")) {
                try {
                    Long millis = Long.parseLong(value);
                    LocalDateTime c = new LocalDateTime(millis*1000);
                    entity.add("createdAt", OrientResourceFactory.instance().asNode(c.toString(), language));
                } catch(Exception e) {
                    Logger.debug("Could not parse time", e);
                }
            } else {
                entity.add(newFieldName, OrientResourceFactory.instance().asNode(value, language));
            }
        }
    }


}
