package com.beligum.blocks.wiki;

import com.beligum.base.utils.Logger;
import com.beligum.blocks.base.Blocks;
import com.beligum.blocks.models.Entity;
import com.beligum.blocks.wiki.search.SimpleIndexer;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.util.AntPathMatcher;
import org.joda.time.LocalDateTime;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

/**
 * Created by wouter on 2/04/15.
 */
public abstract class WikiParser
{
    protected AntPathMatcher pathMatcher;
    // Objects with fields and languages for fields
    protected HashMap<String, HashMap<String, HashMap<String, String>>> items = new HashMap<String, HashMap<String, HashMap<String, String>>>();
    protected ArrayList<Entity> entities = new ArrayList<Entity>();


    protected Integer counter = 0;
    protected SimpleIndexer indexer;

    public WikiParser() throws IOException
    {
        this.indexer = new SimpleIndexer(new File(Blocks.config().getLuceneIndex()));
        pathMatcher = new AntPathMatcher();
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

    public void createEntities() throws IOException
    {
        // loop through items
        // loop through languages
        // create entity per language
        Integer total = this.items.keySet().size();
        Integer count = 0;
        for (String key : this.items.keySet()) {
            HashMap<String, HashMap<String, String>> item = this.items.get(key);
            count++;
            Logger.info("Counter: "+count+"/"+total);
            makeEntity();
            fillEntity(item, "nl");
            fillEntity(item, "fr");
            fillEntity(item, "en");
            Entity entity = addEntity();

            if (entity.getProperties().size() > 0) {
                this.entities.add(changeEntity(entity));
                saveEntity(entity);
            }

        }

        this.indexer.commit();
        this.indexer.close();

    }

    public abstract void makeEntity();

    public abstract Entity addEntity();

    public abstract Entity changeEntity(Entity entity);

    public abstract void saveEntity(Entity entity);

    public abstract Entity fillEntity(HashMap<String, HashMap<String, String>> item, String lang);

    public void splitField(Entity entity, String field)
    {
        String name = Blocks.rdfFactory().ensureAbsoluteRdfValue(field);
        HashMap<String, ArrayList<String>> property = (HashMap<String, ArrayList<String>>) entity.getProperties().get(name);
        entity.getProperties().remove(name);
        for (String language : new String[] { "nl", "fr", "en" }) {
            if (property != null && property.containsKey(language)) {
                for (String value : property.get(language)) {
                    String[] splitValue = value.split("\\|");
                    for (String v : splitValue) {
                        entity.addProperty(field, v.trim(), language);
                    }
                }
            }
        }
    }

    public void prependField(Entity entity, String field, String prefix)
    {
        String name = Blocks.rdfFactory().ensureAbsoluteRdfValue(field);
        HashMap<String, ArrayList<String>> property = (HashMap<String, ArrayList<String>>) entity.getProperties().get(name);
        entity.getProperties().remove(name);
        for (String language : new String[] { "nl", "fr", "en" }) {
            if (property != null && property.containsKey(language)) {
                for (String value : property.get(language)) {
                    entity.addProperty(field, prefix + value.trim(), language);

                }
            }
        }
    }

    public void addToEntity(String newFieldName, String[] oldFields, Entity entity, String language, HashMap<String, HashMap<String, String>> item)
    {
        String value = null;
        for (String field : oldFields) {
            if (item.containsKey(field) && item.get(field).containsKey(language)) {
                if (value == null)
                    value = "";
                value += item.get(field).get(language) + " ";
            }
        }
        if (value != null)
            value = value.trim();
        if (!StringUtils.isEmpty(value)) {
            value = value.trim();
            if (newFieldName.equals("createdBy")) {
                entity.addProperty("createdBy", value);
            }
            else if (newFieldName.equals("createdAt")) {
                try {
                    Long millis = Long.parseLong(value);
                    LocalDateTime c = new LocalDateTime(millis * 1000);
                    entity.addProperty("createdAt", c.toString());
                }
                catch (Exception e) {
                    Logger.debug("Could not parse time", e);
                }
            }
            else {
                entity.addProperty(newFieldName, value, language);
            }
        }
    }

    // Add the first filled field
    public void addToEntityOR(String newFieldName, String[] oldFields, Entity entity, String language, HashMap<String, HashMap<String, String>> item)
    {
        String value = null;
        for (String field : oldFields) {
            if (item.containsKey(field) && item.get(field).containsKey(language)) {
                value = item.get(field).get(language) + " ";
                break;
            }
        }
        if (!StringUtils.isEmpty(value)) {
            value = value.trim();
            if (newFieldName.equals("createdBy")) {
                entity.addProperty("createdBy", value);
            }
            else if (newFieldName.equals("createdAt")) {
                try {
                    Long millis = Long.parseLong(value);
                    LocalDateTime c = new LocalDateTime(millis * 1000);
                    entity.addProperty("createdAt", c.toString());
                }
                catch (Exception e) {
                    Logger.debug("Could not parse time", e);
                }
            }
            else {
                entity.addProperty(newFieldName, value, language);
            }
        }
    }

    public void addToEntityJoined(String newFieldName, String[] oldFields, Entity entity, String language, HashMap<String, HashMap<String, String>> item, String joint)
    {
        String value = null;
        for (String field : oldFields) {
            if (item.containsKey(field) && item.get(field).containsKey(language)) {
                if (value == null)
                    value = "";
                else
                    value += joint + " ";
                value += item.get(field).get(language);
            }
        }
        if (!StringUtils.isEmpty(value)) {
            value = value.trim();
            if (newFieldName.equals("createdBy")) {
                entity.addProperty("createdBy", value);
            }
            else if (newFieldName.equals("createdAt")) {
                try {
                    Long millis = Long.parseLong(value);
                    LocalDateTime c = new LocalDateTime(millis * 1000);
                    entity.addProperty("createdAt", c.toString());
                }
                catch (Exception e) {
                    Logger.debug("Could not parse time", e);
                }
            }
            else {
                entity.addProperty(newFieldName, value, language);
            }
        }
    }

}
