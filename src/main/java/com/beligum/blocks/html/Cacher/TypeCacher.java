package com.beligum.blocks.html.Cacher;

import com.beligum.blocks.html.models.Content;
import com.beligum.blocks.html.models.types.DefaultValue;
import com.beligum.blocks.html.models.types.Identifiable;
import com.beligum.blocks.html.models.types.Template;
import com.beligum.blocks.html.parsers.AbstractParser;
import com.beligum.blocks.html.parsers.CachingNodeVisitor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;

/**
 * Created by wouter on 20/11/14.
 */
public class TypeCacher
{
    private static TypeCacher instance;
    private HashMap<String, Element> content = new HashMap<String, Element>();
    private HashMap<String, Element> entities = new HashMap<String, Element>();
    private HashMap<String, Template> templates = new HashMap<String, Template>();


    private TypeCacher() {
    }

    public static TypeCacher instance() {
        if (instance == null) {
            instance = new TypeCacher();
        }
        return instance;
    }


    public void addTemplate(Template template, boolean force) {
        if (force || !this.templates.containsKey(template.getName())) {
            this.templates.put(template.getName(), template);
        }
    }

    public void addDefault(DefaultValue defaultValue, boolean force) {
        // check if property -> save as checkunique field name
        //                   -> if no lang -> defaultlang, if lang as lang and check if default lang is present
        // check type /./ check if blueprint check if existing and blueprint.
        if (AbstractParser.isProperty(defaultValue.getParsedContent())) {
            this.content.put(defaultValue.getUniquePropertyName(), defaultValue.getParsedContent());
            if (defaultValue.getLanguage() != null) {
                this.putValueWithoutOverride(this.content, defaultValue.getUniquePropertyName() + "::" + defaultValue.getLanguage(), defaultValue.getParsedContent());
            }
            this.putValueWithoutOverride(this.content, defaultValue.getUniquePropertyName(), defaultValue.getParsedContent());
        }

        if (AbstractParser.isType(defaultValue.getParsedContent())) {
            if (defaultValue.getLanguage() != null) {
                this.entities.put(defaultValue.getType() + "::" + defaultValue.getLanguage(), defaultValue.getParsedContent());
            }
        }   this.entities.put(defaultValue.getType(), defaultValue.getParsedContent());
    }

    private <K, V> void putValueWithoutOverride(HashMap<K, V> map, K key, V value) {
        if (!map.containsKey(key)) {
            map.put(key, value);
        }
    }


    public Template getTemplate(String name) {
        return this.templates.get(name);
    }

    public Element getContent(DefaultValue content) {
        Element defaultValue = getPropertyContent(content);
        if (defaultValue == null) {
            defaultValue = getEntityContent(content);
        }
        return defaultValue;
    }

    public Element getEntityContent(DefaultValue defaultValue) {
        Element retVal = null;
        if (AbstractParser.isType(defaultValue.getParsedContent())) {
            if (defaultValue.getLanguage() != null) {
                retVal = this.entities.get(defaultValue.getType() + "::" + defaultValue.getLanguage());
            }
            if (retVal == null) {
                retVal = this.entities.get(defaultValue.getType());
            }
        }
        return retVal;
    }


    public Element getContent(String id) {
        return this.entities.get(id);
    }

    public Element getPropertyContent(DefaultValue defaultValue) {
        Element retVal = null;
        if (AbstractParser.isProperty(defaultValue.getParsedContent())) {
            if (defaultValue.getLanguage() != null) {
                retVal = this.content.get(defaultValue.getUniquePropertyName() + "::" + defaultValue.getLanguage());
            }
            if (retVal == null) {
                retVal = this.content.get(defaultValue.getUniquePropertyName());
            }
        }
        return retVal;
    }

    public void reset() {
        this.content = new HashMap<String, Element>();
        this.entities = new HashMap<String, Element>();
        this.templates = new HashMap<String, Template>();
//        URI classesRootFolderUri = FileFunctions.searchClasspath(this, "/templates");
        Path classesRootFolder = Paths.get("/Users/wouter/git/com.beligum.blocks.core/src/main/resources/templates");

        try {
            Files.walkFileTree(classesRootFolder, new SimpleFileVisitor<Path>()
            {
                @Override
                public FileVisitResult visitFile(Path filePath, BasicFileAttributes attrs)
                                throws IOException
                {
                    if (filePath.getFileName().toString().endsWith("html")) {
                        Document d = Jsoup.parse(new String(Files.readAllBytes(filePath)));
                        d.traverse(new CachingNodeVisitor());
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (Exception e) {
            int x = 0;
        }


    }


}
