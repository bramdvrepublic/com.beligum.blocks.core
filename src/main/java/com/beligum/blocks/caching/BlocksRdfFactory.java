package com.beligum.blocks.caching;

import com.beligum.blocks.base.Blocks;
import com.beligum.blocks.utils.URLFactory;
import com.google.common.collect.HashBiMap;

import java.util.HashMap;

/**
 * Created by wouter on 15/04/15.
 */
public class BlocksRdfFactory
{

    private HashBiMap<String, String> prefixes = HashBiMap.create();

    public BlocksRdfFactory()
    {
        this.addDefaultPrefixes();
    }

    public void addPrefixes(HashMap<String, String> prefixes)
    {
        this.prefixes.putAll(prefixes);
    }

    public void addPrefix(String prefix, String namespace)
    {
        if (prefix != null && namespace != null) {
            if (!this.prefixes.containsValue(namespace)) {
                if (!this.prefixes.containsKey(prefix)) {
                    this.prefixes.put(prefix, namespace);
                }
                else {
                    // Todo prefix exists but with a different namespace
                    int nr = 1;
                    try {
                        nr = Integer.parseInt(prefix.substring(prefix.length() - 1));
                        prefix = prefix.substring(prefix.length() - 1) + nr;
                    }
                    catch (Exception e) {
                        prefix = prefix + 1;
                    }

                    this.prefixes.put(prefix + "1", namespace);
                }
            }
        }
    }

    public String getSchemaForUrl(String url)
    {
        String retVal = null;
        for (String schema : this.prefixes.values()) {
            if (url.startsWith(schema)) {
                retVal = schema;
                break;
            }
        }
        return retVal;
    }

    public String getPrefixForSchema(String schema)
    {
        return this.prefixes.inverse().get(schema);
    }

    public String getSchemaForPrefix(String prefix)
    {
        return this.prefixes.get(prefix);
    }

    private void addDefaultPrefixes()
    {
        this.addPrefix(Blocks.config().getDefaultRdfPrefix(), Blocks.config().getDefaultRdfSchema());
        this.addPrefix("cat", "http://www.w3.org/ns/dcat#");
        this.addPrefix("qb", "http://purl.org/linked-data/cube#");
        this.addPrefix("grddl", "http://www.w3.org/2003/g/data-view#");
        this.addPrefix("ma", "http://www.w3.org/ns/ma-ont#");
        this.addPrefix("owl", "http://www.w3.org/2002/07/owl#");
        this.addPrefix("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
        this.addPrefix("rdfa", "http://www.w3.org/ns/rdfa#");
        this.addPrefix("rdfs", "http://www.w3.org/2000/01/rdf-schema#");
        this.addPrefix("rif", "http://www.w3.org/2007/rif#");
        this.addPrefix("rr", "http://www.w3.org/ns/r2rml#");
        this.addPrefix("skos", "http://www.w3.org/2004/02/skos/core#");
        this.addPrefix("skosxl", "http://www.w3.org/2008/05/skos-xl#");
        this.addPrefix("wdr", "http://www.w3.org/2007/05/powder#");
        this.addPrefix("void", "http://rdfs.org/ns/void#");
        this.addPrefix("wdrs", "http://www.w3.org/2007/05/powder-s#");
        this.addPrefix("xhv", "http://www.w3.org/1999/xhtml/vocab#");
        this.addPrefix("xml", "http://www.w3.org/XML/1998/namespace");
        this.addPrefix("xsd", "http://www.w3.org/2001/XMLSchema#");
        this.addPrefix("prov", "http://www.w3.org/ns/prov#");
        this.addPrefix("sd", "http://www.w3.org/ns/sparql-service-description#");
        this.addPrefix("org", "http://www.w3.org/ns/org#");
        this.addPrefix("gldp", "http://www.w3.org/ns/people#");
        this.addPrefix("cnt", "http://www.w3.org/2008/content#");
        this.addPrefix("dcat", "http://www.w3.org/ns/dcat#");
        this.addPrefix("earl", "http://www.w3.org/ns/earl#");
        this.addPrefix("ht", "http://www.w3.org/2006/http#");
        this.addPrefix("ptr", "http://www.w3.org/2009/pointers#");
        this.addPrefix("cc", "http://creativecommons.org/ns#");
        this.addPrefix("ctag", "http://commontag.org/ns#");
        this.addPrefix("dc", "http://purl.org/dc/terms/");
        this.addPrefix("dc11", "http://purl.org/dc/elements/1.1/");
        this.addPrefix("dcterms", "http://purl.org/dc/terms/");
        this.addPrefix("foaf", "http://xmlns.com/foaf/0.1/");
        this.addPrefix("gr", "http://purl.org/goodrelations/v1#");
        this.addPrefix("ical", "http://www.w3.org/2002/12/cal/icaltzd#");
        this.addPrefix("og", "http://ogp.me/ns#");
        this.addPrefix("rev", "http://purl.org/stuff/rev#");
        this.addPrefix("sioc", "http://rdfs.org/sioc/ns#");
        this.addPrefix("v", "http://rdf.data-vocabulary.org/#");
        this.addPrefix("vcard", "http://www.w3.org/2006/vcard/ns#");
        this.addPrefix("schema", "http://schema.org/");
        this.addPrefix("describedby", "http://www.w3.org/2007/05/powder-s#describedby");
        this.addPrefix("license", "http://www.w3.org/1999/xhtml/vocab#license");
        this.addPrefix("role", "http://www.w3.org/1999/xhtml/vocab#role");
    }

    public String makeAbsoluteRdfValue(String relativePath)
    {
        return URLFactory.makeAbsolute(Blocks.config().getDefaultRdfSchema(), relativePath);
    }

    public String ensureAbsoluteRdfValue(String relativePath)
    {
        return ensureAbsoluteRdfValue(Blocks.config().getDefaultRdfSchema(), relativePath);
    }

    public String ensureAbsoluteRdfValue(String schema, String relativePath)
    {
        String retVal = null;
        if (relativePath.indexOf(":") > 0) {
            String[] paths = relativePath.split(":");
            if (paths[1].startsWith("//")) {
                retVal = relativePath;
            }
            else {
                String prefixSchema = getSchemaForPrefix(paths[0]);
                retVal = URLFactory.makeAbsolute(prefixSchema, paths[1]);
            }
        }

        if (retVal == null) {
            retVal = URLFactory.makeAbsolute(schema, relativePath);
        }
        return retVal;
    }

    //    public String ensurePrefixedRdfValue(String relativePath) {
    //        return ensurePrefixedRdfValue(Blocks.putConfig().getDefaultRdfPrefix(), relativePath);
    //    }

    //    public String ensurePrefixedRdfValue(String prefix, String relativePath) {
    //        String retVal = null;
    //        if (relativePath.indexOf(":") > 0) {
    //            String[] paths = relativePath.split(":");
    //            if (paths.length > 1 && paths[1].startsWith("//")) {
    //                String schema = getSchemaForUrl(relativePath);
    //
    //                if (schema != null) {
    //                    prefix = getPrefixForSchema(schema);
    //                    retVal = makePrefixed(prefix, relativePath.substring(schema.length()));
    //                } else {
    //                    // add prefix
    //                    int index = relativePath.indexOf("#");
    //                    if (relativePath.indexOf("#") == -1) index = relativePath.indexOf("/");
    //                    schema = relativePath.substring(0, index);
    //                    relativePath = relativePath.substring(index + 1);
    //                    prefix = createPrefixForSchema(schema);
    //                    retVal = makePrefixed(prefix, relativePath);
    //                }
    //            } else  {
    //                // already prefixed
    //                retVal = relativePath;
    //            }
    //        }
    //
    //        if (retVal == null) {
    //            retVal = makePrefixed(prefix, relativePath);
    //        }
    //        return retVal;
    //    }

    public static String makePrefixed(String prefix, String relativePath)
    {
        return prefix + ":" + relativePath;
    }

    public String createPrefixForSchema(String schema)
    {
        String retVal = schema;
        String[] paths = schema.split(".");
        if (paths.length > 1) {
            if (paths[0].endsWith("www")) {
                retVal = paths[1];
            }
            else {
                retVal = paths[0].substring(paths[0].lastIndexOf("/"));
            }
        }
        return retVal;
    }
}
