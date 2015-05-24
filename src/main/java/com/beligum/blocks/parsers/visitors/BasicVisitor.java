package com.beligum.blocks.parsers.visitors;

import com.beligum.base.resources.ResourceDescriptor;
import com.beligum.base.server.R;
import com.beligum.base.utils.Logger;
import com.beligum.blocks.exceptions.ParseException;
import com.beligum.blocks.parsers.FileAnalyzer;
import com.google.common.base.Charsets;
import com.google.common.collect.HashBiMap;
import org.apache.commons.io.IOUtils;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;

/**
 * Created by wouter on 30/03/15.
 */
public abstract class BasicVisitor
{
    protected HashBiMap<String, String> localPrefixes = HashBiMap.create();

    public abstract Node head(Node node, int depth) throws ParseException;

    public abstract Node tail(Node node, int depth) throws ParseException;

    public Node includeSource(Element at, Document source) throws IOException, ParseException
    {
        if (source.childNodes().isEmpty()) {
            throw new ParseException("Cannot include an empty file.", at);
        }
        Node firstChild = source.childNode(0);
        Element parent = at.parent();
        if (parent == null) {
            throw new ParseException("Cannot use an include as a root-node.", at);
        }
        int siblingIndex = at.siblingIndex();
        parent.insertChildren(siblingIndex, source.childNodes());
        at.remove();
        return firstChild;
    }

    public Document getSource(String sourcePath) throws IOException
    {
        ResourceDescriptor resourceDescriptor = R.resourceLoader().getResource(sourcePath);
        try (Reader reader = Files.newBufferedReader(resourceDescriptor.getParsedPath(), Charsets.UTF_8)) {
            Document source = FileAnalyzer.parse(IOUtils.toString(reader));
            if (source.childNodes().isEmpty()) {
                Logger.warn("Found empty file at '" + sourcePath + "'.");
            }
            return source;
        }
    }

    public void parsePrefixes(String value)
    {
        if (value == null)
            return;
        String[] parts = value.split("\\s+");
        for (int i = 0; i < parts.length; i += 2) {
            String prefix = parts[i];
            if (i + 1 < parts.length && prefix.endsWith(":")) {
                String prefixFix = prefix.substring(0, prefix.length() - 1);
                localPrefixes.put(prefixFix, parts[i + 1]);
            }
        }
    }

    //    public String makeAbsoluteRdfValue(String value) {
    //        if (value != null && !value.startsWith("http://")) {
    //            if (value.contains(":")) {
    //                String prefix = value.split(":")[0];
    //                value = value.split(":")[1];
    //                String namespace = this.localPrefixes.get(prefix) == null ? Blocks.templateCache().getSchemaForPrefix(prefix): this.localPrefixes.get(prefix);
    //                if (namespace != null) value = URLFactory.makeAbsolute(namespace, value);
    //            } else {
    //                value = URLFactory.makeAbsoluteRdfValue(value).toString();
    //            }
    //        }
    //        return value;
    //    }
    //

}
