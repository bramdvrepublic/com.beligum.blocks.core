package com.beligum.blocks.caching;

import com.beligum.blocks.base.Blocks;
import com.beligum.blocks.exceptions.CacheException;
import com.beligum.blocks.exceptions.ParseException;
import com.beligum.blocks.models.Blueprint;
import com.beligum.blocks.models.PageTemplate;
import com.beligum.blocks.parsers.FileAnalyzer;
import com.beligum.blocks.parsers.Traversor;
import com.beligum.blocks.parsers.visitors.reset.BlocksScriptVisitor;
import com.beligum.base.server.R;
import com.beligum.base.utils.Logger;
import com.google.common.collect.HashBiMap;
import org.apache.shiro.util.AntPathMatcher;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

/**
 * Created by wouter on 17/03/15.
 */
public class TemplateCache implements BlocksTemplateCache
{
    private boolean runningTroughHtmlTemplates = false;
    private AntPathMatcher pathMatcher = new AntPathMatcher();
    private HashMap<String, HashMap<String, Blueprint>> blueprints = new HashMap<String, HashMap<String, Blueprint>>();
    private HashMap<String, HashMap<String, PageTemplate>> pagetemplates = new HashMap<String, HashMap<String, PageTemplate>>();
    private HashBiMap<String, String> prefixes = HashBiMap.create();
    private Set<String> pageblocks = new HashSet<String>();
    private Set<String> addableblocks = new HashSet<String>();
    private LinkedHashSet<String> blocksScripts = new LinkedHashSet<>();
    private LinkedHashSet<String> blocksLinks = new LinkedHashSet<>();

    public TemplateCache() throws CacheException
    {
        blueprints = new HashMap<String, HashMap<String, Blueprint>>();
        pagetemplates = new HashMap<String, HashMap<String, PageTemplate>>();
        for (String lang: Blocks.config().getLanguages()) {
            blueprints.put(lang, new HashMap<String, Blueprint>());
            pagetemplates.put(lang, new HashMap<String, PageTemplate>());
        }
    }

    public void reset() throws CacheException
    {
        blueprints = new HashMap<String, HashMap<String, Blueprint>>();
        pagetemplates = new HashMap<String, HashMap<String, PageTemplate>>();
        for (String lang: Blocks.config().getLanguages()) {
            blueprints.put(lang, new HashMap<String, Blueprint>());
            pagetemplates.put(lang, new HashMap<String, PageTemplate>());
        }
        this.fillCache();
    }


    public void addBlueprint(Blueprint blueprint) {
        if (!this.blueprints.get(blueprint.getLanguage()).containsKey(blueprint.getBlueprintName())) {
            this.blueprints.get(blueprint.getLanguage()).put(blueprint.getName(), blueprint);
        }
    }

    public void addPageTemplate(PageTemplate page) {
        if (!this.pagetemplates.get(page.getLanguage()).containsKey(page.getBlueprintName())) {
            this.pagetemplates.get(page.getLanguage()).put(page.getName(), page);
        }
    }

    public void addBlueprint(Blueprint blueprint, String language) {
        if (!this.blueprints.get(language).containsKey(blueprint.getBlueprintName())) {
            this.blueprints.get(language).put(blueprint.getName(), blueprint);
        }
    }

    public void addPageTemplate(PageTemplate page, String language) {
        if (!this.pagetemplates.get(language).containsKey(page.getBlueprintName())) {
            this.pagetemplates.get(language).put(page.getName(), page);
        }
    }

    public Blueprint getBlueprint(String name, String language) {
        Blueprint retVal = null;
        if (name != null && language != null) {
            retVal = this.blueprints.get(language).get(name);
            if (retVal == null) {
                retVal = this.blueprints.get(Blocks.config().getDefaultLanguage()).get(name);
            }
        }
        return retVal;
    }

    public PageTemplate getPagetemplate(String name, String language) {
        return this.pagetemplates.get(language).get(name);
    }

    public List<Blueprint> getBlueprints(String language) {
        return new ArrayList<Blueprint>(this.blueprints.get(language).values());
    }

    public List<PageTemplate> getPagetemplates(String language) {
        return new ArrayList<PageTemplate>(this.pagetemplates.get(language).values());
    }

    public List<Blueprint> getPageBlocks() {
        ArrayList<Blueprint> list = new ArrayList<>();
        for (Blueprint bp: getBlueprints(Blocks.config().getDefaultLanguage())) {
            if (bp.isPageBlock()) list.add(bp);
        }

        return list;
    }

    public List<Blueprint> getAddableBlocks() {
        ArrayList<Blueprint> list = new ArrayList<>();
        for (Blueprint bp: getBlueprints(Blocks.config().getDefaultLanguage())) {
            if (bp.isAddableBlock()) list.add(bp);
        }

        return list;
    }

    public LinkedHashSet<String> getBlocksScripts() {
        return this.blocksScripts;
    }

    public LinkedHashSet<String> getBlocksLinks() {
        return this.blocksLinks;
    }

    public void addPrefixes(HashMap<String, String> prefixes) {
        this.prefixes.putAll(prefixes);
    }



    public void addPrefix(String prefix, String namespace) {
        if (prefix != null && namespace != null) {
            if (!this.prefixes.containsValue(namespace)) {
                if (!this.prefixes.containsKey(prefix)) {
                    this.prefixes.put(prefix, namespace);
                } else {
                    // Todo prefix exists but with a different namespace
                    this.prefixes.put(prefix+"1", namespace);
                }
            }
        }
    }

    public String getPrefixForSchema(String schema) {
        return this.prefixes.inverse().get(schema);
    }

    public String getSchemaForPrefix(String prefix) {
        return this.prefixes.get(prefix);
    }


    private void addDefaultPrefixes() {
        this.addPrefix(Blocks.config().getDefaultRdfPrefix(), Blocks.config().getDefaultRdfSchema());
        this.addPrefix("cat","http://www.w3.org/ns/dcat#");
        this.addPrefix("qb","http://purl.org/linked-data/cube#");
        this.addPrefix("grddl","http://www.w3.org/2003/g/data-view#");
        this.addPrefix("ma","http://www.w3.org/ns/ma-ont#");
        this.addPrefix("owl","http://www.w3.org/2002/07/owl#");
        this.addPrefix("rdf","http://www.w3.org/1999/02/22-rdf-syntax-ns#");
        this.addPrefix("rdfa","http://www.w3.org/ns/rdfa#");
        this.addPrefix("rdfs","http://www.w3.org/2000/01/rdf-schema#");
        this.addPrefix("rif","http://www.w3.org/2007/rif#");
        this.addPrefix("rr","http://www.w3.org/ns/r2rml#");
        this.addPrefix("skos","http://www.w3.org/2004/02/skos/core#");
        this.addPrefix("skosxl","http://www.w3.org/2008/05/skos-xl#");
        this.addPrefix("wdr","http://www.w3.org/2007/05/powder#");
        this.addPrefix("void","http://rdfs.org/ns/void#");
        this.addPrefix("wdrs","http://www.w3.org/2007/05/powder-s#");
        this.addPrefix("xhv","http://www.w3.org/1999/xhtml/vocab#");
        this.addPrefix("xml","http://www.w3.org/XML/1998/namespace");
        this.addPrefix("xsd","http://www.w3.org/2001/XMLSchema#");
        this.addPrefix("prov","http://www.w3.org/ns/prov#");
        this.addPrefix("sd","http://www.w3.org/ns/sparql-service-description#");
        this.addPrefix("org","http://www.w3.org/ns/org#");
        this.addPrefix("gldp","http://www.w3.org/ns/people#");
        this.addPrefix("cnt","http://www.w3.org/2008/content#");
        this.addPrefix("dcat","http://www.w3.org/ns/dcat#");
        this.addPrefix("earl","http://www.w3.org/ns/earl#");
        this.addPrefix("ht","http://www.w3.org/2006/http#");
        this.addPrefix("ptr","http://www.w3.org/2009/pointers#");
        this.addPrefix("cc","http://creativecommons.org/ns#");
        this.addPrefix("ctag","http://commontag.org/ns#");
        this.addPrefix("dc","http://purl.org/dc/terms/");
        this.addPrefix("dc11","http://purl.org/dc/elements/1.1/");
        this.addPrefix("dcterms","http://purl.org/dc/terms/");
        this.addPrefix("foaf","http://xmlns.com/foaf/0.1/");
        this.addPrefix("gr","http://purl.org/goodrelations/v1#");
        this.addPrefix("ical","http://www.w3.org/2002/12/cal/icaltzd#");
        this.addPrefix("og","http://ogp.me/ns#");
        this.addPrefix("rev","http://purl.org/stuff/rev#");
        this.addPrefix("sioc","http://rdfs.org/sioc/ns#");
        this.addPrefix("v","http://rdf.data-vocabulary.org/#");
        this.addPrefix("vcard","http://www.w3.org/2006/vcard/ns#");
        this.addPrefix("schema","http://schema.org/");
        this.addPrefix("describedby","http://www.w3.org/2007/05/powder-s#describedby");
        this.addPrefix("license","http://www.w3.org/1999/xhtml/vocab#license");
        this.addPrefix("role","http://www.w3.org/1999/xhtml/vocab#role");
    }

    /**
     * Fill up the page-cache with all template found in file-system
     *
     * @throws com.beligum.blocks.exceptions.CacheException
     */
    protected void fillCache() throws CacheException
    {
        if (!runningTroughHtmlTemplates) {
            runningTroughHtmlTemplates = true;

            this.prefixes.clear();
            this.addDefaultPrefixes();

            try {
                List<Path> allResourceFolders = R.resourceLoader().getResourceFolders();

                for (Path resourceFolder : allResourceFolders) {
                    Path templatesFolder = resourceFolder.resolve(Blocks.config().getTemplateFolder());

                    if (Files.exists(templatesFolder)) {
                        //set which will be filled up with all class-names found in all files in the templates-folder
                        final Set<String> foundEntityClassNames = new HashSet<>();

                        //first fetch all blueprints from all files
                        FileVisitor<Path> visitor = new SimpleFileVisitor<Path>()
                        {
                            @Override
                            public FileVisitResult visitFile(Path filePath, BasicFileAttributes attrs)
                                            throws IOException
                            {
                                String path = filePath.getFileName().toString();
                                if (pathMatcher.matches("*.html", path) || pathMatcher.match("*.htm", path)) {
                                    try {
                                        String html = new String(Files.readAllBytes(filePath));
                                        for (String language: Blocks.config().getLanguages()) {
                                            FileAnalyzer.AnalyseHtmlFile(html, language);
                                        }
                                    }
                                    catch (ParseException e) {
                                        Logger.error("Parse error while fetching page-templates and blueprints from file '" + filePath + "'.", e);
                                    }
                                }
                                return FileVisitResult.CONTINUE;
                            }
                        };
                        Files.walkFileTree(templatesFolder, visitor);

                    }
                }

                // Parse all after finding all
                // Add missing blueprints to default language
                for (String lang: Blocks.config().getLanguages()) {
                    for (Blueprint blueprint : this.getBlueprints(lang)) {
                        blueprint.parse();

                        if (blueprint.isAddableBlock()) this.addableblocks.add(blueprint.getName());
                        if (blueprint.isPageBlock()) this.pageblocks.add(blueprint.getName());
                    }

                    for (PageTemplate pageTemplate : this.getPagetemplates(lang)) {
                        pageTemplate.parse();

                    }
                }

                BlocksScriptVisitor visitor = new BlocksScriptVisitor();
                Document doc = visitor.getSource(Blocks.config().getFrontEndScripts());
                Traversor.traverseDeep(doc, visitor);
                this.blocksScripts = visitor.getScripts();
                this.blocksLinks = visitor.getLinks();


            }
            catch (Exception e) {
                throw new CacheException("Error while filling cache: " + this, e);
            }
            finally {
                runningTroughHtmlTemplates = false;
            }
        }
    }



}
