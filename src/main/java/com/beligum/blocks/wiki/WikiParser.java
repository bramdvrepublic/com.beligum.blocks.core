package com.beligum.blocks.wiki;

import com.beligum.blocks.base.Blocks;
import com.beligum.blocks.exceptions.DatabaseException;
import com.beligum.base.utils.Logger;
import com.beligum.blocks.models.jsonld.JsonLDGraph;
import com.beligum.blocks.models.jsonld.Node;
import com.beligum.blocks.models.jsonld.NodeFactory;
import com.beligum.blocks.models.jsonld.ResourceNode;
import com.beligum.blocks.models.jsonld.jackson.ResourceNodeSerializer;
import com.beligum.blocks.models.rdf.OrderedMemGraph;
import com.beligum.blocks.search.SimpleIndexer;
import com.beligum.blocks.utils.UrlFactory;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.util.AntPathMatcher;
import org.joda.time.LocalDateTime;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

/**
 * Created by wouter on 2/04/15.
 */
public abstract class WikiParser
{
    public static final String NL = "nl";
    public static final String EN = "en";
    public static final String FR = "fr";

    public static final String[] LANGUAGES = new String[] {NL, FR, EN};

    protected SimpleIndexer indexer;
    protected AntPathMatcher pathMatcher;
    // Objects with fields and languages for fields
    protected HashMap<String, HashMap<String, HashMap<String,String>>> items = new HashMap<String, HashMap<String, HashMap<String,String>>>();
    protected ArrayList<HashMap<String, ResourceNode>> entities = new ArrayList<HashMap<String, ResourceNode>>();
    protected ObjectMapper mapper;

    public WikiParser()
    {

        pathMatcher = new AntPathMatcher();
        try {
            indexer = new SimpleIndexer(new File(Blocks.config().getLuceneIndex()));
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        final SimpleModule module = new SimpleModule("customerSerializationModule", new Version(1, 0, 0, "static version"));
        module.addSerializer(ResourceNode.class, new ResourceNodeSerializer());

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
                        } else if (line_nr > 0) {
                            int index = line.indexOf("=");
                            if (index > -1) {
                                item.addField(line.substring(0, index), line.substring(index + 1));
                            } else if (!StringUtils.isEmpty(line)) {
                                Logger.error("Invalid line found in file: " + path + " ");
                            }

                        }

                        line_nr++;
                    }

                    if (item.isValid()) {
                        HashMap<String, HashMap<String,String>> storedValue = new HashMap<String, HashMap<String,String>>();
                        if (items.containsKey(item.getId())) {
                            storedValue = items.get(item.getId());
                        }
                        storedValue = item.addToData(storedValue);
                        items.put(item.getId(), storedValue);
                    } else {
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

    public void listFields() {
        HashSet<String> fieldNames = new HashSet<String>();
        for (String key: this.items.keySet()) {
            HashMap<String, HashMap<String,String>> item = this.items.get(key);
            for (String fieldKey: item.keySet()) {
                fieldNames.add(fieldKey);
            }
        }
        Logger.info("-----------Fields----------");
        for(Object field: fieldNames.toArray()) {
            if (!((String)field).contains("title")) {
                Logger.info((String) field);
            }
        }
    }

    public void createEntities()
    {
        // loop through items
        // loop through languages
        // create entity per language
        try {


            try {
                int count = 0;

                for (String key : this.items.keySet()) {
                    HashMap<String, HashMap<String, String>> item = this.items.get(key);

                    makeEntity();
                    HashMap entity = new HashMap<String, ResourceNode>();
                    for (String language : new String[] { NL, FR, EN }) {
                        fillEntity(item, language);
                        ResourceNode resource = addEntity(language);

                        if (resource.unwrap().keySet().size() > 0) {
                            entity.put(language, changeEntity(resource, language));
                            this.entities.add(entity);
                            count++;

                            Logger.info("saving entity: " + count);
                        }
                    }
                    saveEntity((ResourceNode) entity.get(NL));

                }
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

    public void addDocument(String id, ResourceNode resource) throws IOException
    {
        String content = mapper.writeValueAsString(resource);
        indexer.addDocument(id, content);
    }

    public abstract void makeEntity();

    public abstract ResourceNode addEntity(String language);

    public abstract ResourceNode changeEntity(ResourceNode entity, String language);

    public abstract void saveEntity(ResourceNode entity);

    public abstract ResourceNode fillEntity(HashMap<String, HashMap<String, String>> item, String lang);

    public void splitField(ResourceNode entity, String field, String language) {
        String name = UrlFactory.createLocalType(field);
        Node property = entity.get(name);
        entity.remove(name);
        if (property != null && property.isList()) {
            for (Node value : property.getList()) {
                if (value.isString()) {
                    String[] splitValue = value.getString().split("\\|");
                    for (String v : splitValue) {
                        entity.add(field, NodeFactory.createAndGuess(v.trim(), language));
                    }
                }
            }
        } else if (property != null && property.isString()) {
            String[] splitValue = property.getString().split("\\|");
            for (String v : splitValue) {
                entity.add(field, NodeFactory.createAndGuess(v.trim(), language));
            }
        }

    }

    public void prependField(ResourceNode entity, String field, String prefix, String language) {
        String name = UrlFactory.createLocalType(field);
        Node property = entity.get(name);
        entity.remove(name);
        if (property != null && property.isList()) {
            for (Node node: property.getList()) {
                if (node.isString()) {
                    entity.add(field, NodeFactory.createAndGuess(prefix + node.getString().trim(), language));

                }
            }
        } else if (property.isString()) {
            entity.add(field, NodeFactory.createAndGuess(prefix + property.getString().trim(), language));
        }



    }

    public void addToEntity(String newFieldName, String[] oldFields, ResourceNode entity, String language, HashMap<String, HashMap<String,String>> item) {
        String value = null;
        for (String field: oldFields) {
            if (item.containsKey(field) && item.get(field).containsKey(language)) {
                if (value == null) value = "";
                value += item.get(field).get(language) + " ";
            }
        }
        if (value != null) value = value.trim();

        add(newFieldName, value, entity, language);
    }

    // Add the first filled field
    public void addToEntityOR(String newFieldName, String[] oldFields, ResourceNode entity, String language, HashMap<String, HashMap<String,String>> item) {
        String value = null;
        for (String field: oldFields) {
            if (item.containsKey(field) && item.get(field).containsKey(language)) {
                value = item.get(field).get(language) + " ";
                break;
            }
        }

    }

    public void addToEntityJoined(String newFieldName, String[] oldFields, ResourceNode entity, String language, HashMap<String, HashMap<String,String>> item, String joint) {
        String value = null;
        for (String field: oldFields) {
            if (item.containsKey(field) && item.get(field).containsKey(language)) {
                if (value == null) value = ""; else value += joint + " ";
                value += item.get(field).get(language);
            }
        }

        add(newFieldName, value, entity, language);

    }


    public void add(String newFieldName, String value, ResourceNode entity, String language) {
        if (!StringUtils.isEmpty(value)) {
            value = value.trim();
            if (newFieldName.equals("createdBy")) {
                entity.add("createdBy", NodeFactory.createAndGuess(value));
            } else if (newFieldName.equals("createdAt")) {
                try {
                    Long millis = Long.parseLong(value);
                    LocalDateTime c = new LocalDateTime(millis*1000);
                    entity.add("createdAt", NodeFactory.createAndGuess(c.toString()));
                } catch(Exception e) {
                    Logger.debug("Could not parse time", e);
                }
            } else {
                entity.add(UrlFactory.createLocalType(newFieldName), NodeFactory.createAndGuess(value, language));
            }
        }
    }


}
