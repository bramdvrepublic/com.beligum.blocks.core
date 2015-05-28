package com.beligum.blocks.caching;

import com.beligum.base.resources.ResourceSearchResult;
import com.beligum.base.server.R;
import com.beligum.blocks.base.Blocks;
import com.beligum.blocks.exceptions.CacheException;
import com.beligum.blocks.exceptions.ParseException;
import com.beligum.blocks.models.Blueprint;
import com.beligum.blocks.models.PageTemplate;
import com.beligum.blocks.parsers.Traversor;
import com.beligum.blocks.parsers.visitors.reset.BlocksScriptVisitor;
import org.apache.commons.io.Charsets;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.parser.Parser;

import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Created by wouter on 17/03/15.
 */
public class TemplateCache implements BlocksTemplateCache
{
    private boolean runningTroughHtmlTemplates = false;
    private HashMap<String, Blueprint> blueprints = new HashMap<String, Blueprint>();
    private HashMap<String, PageTemplate> pageTemplates = new HashMap<String, PageTemplate>();
    private Set<String> pageBlocks = new HashSet<String>();
    private Set<String> addableBlocks = new HashSet<String>();
    private LinkedHashMap<String, String> blocksScripts = new LinkedHashMap<>();
    private LinkedHashMap<String, String> blocksLinks = new LinkedHashMap<>();

    public TemplateCache() throws CacheException
    {
        blueprints = new HashMap<String, Blueprint>();
        pageTemplates = new HashMap<String, PageTemplate>();
    }

    public void reset() throws CacheException
    {
        blueprints = new HashMap<String, Blueprint>();
        pageTemplates = new HashMap<String, PageTemplate>();

        this.fillCache();
    }

    public void addBlueprint(Blueprint blueprint)
    {
        if (!this.blueprints.containsKey(blueprint.getName())) {
            this.blueprints.put(blueprint.getName(), blueprint);
        }
    }

    public void addPageTemplate(PageTemplate page)
    {
        if (!this.pageTemplates.containsKey(page.getName())) {
            this.pageTemplates.put(page.getName(), page);
        }
    }

    public Blueprint getBlueprint(String name)
    {
        Blueprint retVal = null;
        if (name != null) {
            retVal = this.blueprints.get(name);

        }
        return retVal;
    }

    public PageTemplate getPageTemplate(String name)
    {
        return this.pageTemplates.get(name);
    }

    public List<Blueprint> getBlueprints()
    {
        return new ArrayList<Blueprint>(this.blueprints.values());
    }

    public List<PageTemplate> getPageTemplates()
    {
        return new ArrayList<PageTemplate>(this.pageTemplates.values());
    }

    public List<Blueprint> getPageBlocks()
    {
        ArrayList<Blueprint> list = new ArrayList<>();
        for (Blueprint bp : getBlueprints()) {
            if (bp.isPageBlock())
                list.add(bp);
        }

        return list;
    }

    public List<Blueprint> getAddableBlocks()
    {
        ArrayList<Blueprint> list = new ArrayList<>();
        for (Blueprint bp : getBlueprints()) {
            if (bp.isAddableBlock())
                list.add(bp);
        }

        return list;
    }

    @Override
    public LinkedHashMap<String, String> getBlocksScripts()
    {
        return this.blocksScripts;
    }
    @Override
    public LinkedHashMap<String, String> getBlocksLinks()
    {
        return this.blocksLinks;
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

            try {
                List<ResourceSearchResult> htmlFiles = R.resourceLoader().searchResourceGlob("/templates/**.{html,htm}");
                htmlFiles.addAll(R.resourceLoader().searchResourceGlob("/views/**.{html,htm}"));
                htmlFiles.addAll(R.resourceLoader().searchResourceGlob("/assets/imports/**.{html,htm}"));
                htmlFiles.addAll(R.resourceLoader().searchResourceGlob("/imports/**.{html,htm}"));

                for (ResourceSearchResult htmlFile : htmlFiles) {
                    Path relativeAbsolutedPath = Paths.get("/").resolve(htmlFile.getResourceFolder().relativize(htmlFile.getResource()));

                    try (Reader reader = Files.newBufferedReader(htmlFile.getResource(), Charset.forName(Charsets.UTF_8.name()))) {
//                        //TODO bram: we should incorporate the language here?
////                        Template template = R.templateEngine().getNewStringTemplate(IOUtils.toString(reader));
////                        String html = template.renderContent();
//
//                        Document document = this.parseHtml(IOUtils.toString(reader));
//                        Html html = HtmlCodeFactory.create(document, relativeAbsolutedPath);
//                        if (html instanceof HtmlImportTemplate) {
//                            HtmlImportTemplate template = (HtmlImportTemplate)html;
//
//                            Logger.info("Found template <"+template.getTemplateTagName()+">: "+htmlFile);
//
//                            Iterable<HtmlScriptElement> scripts = template.getScripts();
//                            for (HtmlElement script : scripts) {
//                                Logger.info("script: "+script);
//                            }
//
//                            Iterable<HtmlStyleElement> styles = template.getStyles();
//                            for (HtmlElement style : styles) {
//                                Logger.info("style: "+style);
//                            }
//                        }

                        //Element htmlElement = new Source(html).getFirstElement("html");
//                        if (htmlElement!=null) {
//                            Logger.info("We found a page template; "+htmlFile.getResource());
//                        }
//                        else {
//                            Logger.info("We found a snippet; "+htmlFile.getResource());
//                        }

//                        for (String language : Blocks.putConfig().getLanguages()) {
//                            FileAnalyzer.AnalyseHtmlFile(html, language);
//                        }
                    }
                    catch (Exception e) {
                        throw new ParseException("Parse error while fetching page-templates and blueprints from file '" + htmlFile + "'.", e);
                    }
                }

                // Parse all after finding all
                // Add missing blueprints to default language

                for (Blueprint blueprint : this.getBlueprints()) {
                    blueprint.parse();

                    if (blueprint.isAddableBlock())
                        this.addableBlocks.add(blueprint.getName());
                    if (blueprint.isPageBlock())
                        this.pageBlocks.add(blueprint.getName());
                }

                for (PageTemplate pageTemplate : this.getPageTemplates()) {
                    pageTemplate.parse();
                }

                BlocksScriptVisitor visitor = new BlocksScriptVisitor();
                Document doc = visitor.getSource(Blocks.config().getFrontEndScripts());
                Traversor.traverseDeep(doc, visitor);
                this.blocksScripts = visitor.getScriptsLinksParser().getScripts();
                this.blocksLinks = visitor.getScriptsLinksParser().getLinks();
            }
            catch (Exception e) {
                throw new CacheException("Error while filling cache: " + this, e);
            }
            finally {
                runningTroughHtmlTemplates = false;
            }
        }
    }

    //-----PRIVATE METHODS-----
    /**
     * Parse html to jsoup-document.
     * Note: if the html received contains an empty head, only the body-html is returned.
     *
     * @param html
     */
    private Document parseHtml(String html)
    {
        Document retVal = new Document(Blocks.config().getSiteDomain());
        Document parsed = Jsoup.parse(html, Blocks.config().getSiteDomain(), Parser.htmlParser());
        /*
         * If only part of a html-file is being parsed (which starts f.i. with a <div>-tag), Jsoup will add <html>-, <head>- and <body>-tags, which is not what we want
         * Thus if the head (or body) is empty, but the body (or head) is not, we only want the info in the body (or head).
         */
        if (parsed.head().childNodes().isEmpty() && !parsed.body().childNodes().isEmpty()) {
            for (org.jsoup.nodes.Node child : parsed.body().childNodes()) {
                retVal.appendChild(child.clone());
            }
        }
        else if (parsed.body().childNodes().isEmpty() && !parsed.head().childNodes().isEmpty()) {
            for (org.jsoup.nodes.Node child : parsed.head().childNodes()) {
                retVal.appendChild(child.clone());
            }
        }
        else if (parsed.body().childNodes().isEmpty() && parsed.body().childNodes().isEmpty()) {
            //add nothing to the retVal so an empty document will be returned
        }
        else {
            retVal = parsed;
        }
        return retVal;
    }
}
