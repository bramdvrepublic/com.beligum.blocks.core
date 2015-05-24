package com.beligum.blocks.caching;

import com.beligum.base.resources.ResourceSearchResult;
import com.beligum.base.server.R;
import com.beligum.base.templating.ifaces.Template;
import com.beligum.base.utils.Logger;
import com.beligum.blocks.base.Blocks;
import com.beligum.blocks.exceptions.CacheException;
import com.beligum.blocks.exceptions.ParseException;
import com.beligum.blocks.models.Blueprint;
import com.beligum.blocks.models.PageTemplate;
import com.beligum.blocks.parsers.FileAnalyzer;
import com.beligum.blocks.parsers.Traversor;
import com.beligum.blocks.parsers.visitors.reset.BlocksScriptVisitor;
import com.beligum.blocks.utils.UrlTools;
import org.apache.commons.io.Charsets;
import org.apache.commons.io.IOUtils;
import org.apache.shiro.util.AntPathMatcher;
import org.jsoup.nodes.Document;

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
    private AntPathMatcher pathMatcher = new AntPathMatcher();
    private HashMap<String, Blueprint> blueprints = new HashMap<String, Blueprint>();
    private HashMap<String, PageTemplate> pagetemplates = new HashMap<String, PageTemplate>();
    private Set<String> pageblocks = new HashSet<String>();
    private Set<String> addableblocks = new HashSet<String>();
    private LinkedHashSet<String> blocksScripts = new LinkedHashSet<>();
    private LinkedHashSet<String> blocksLinks = new LinkedHashSet<>();

    public TemplateCache() throws CacheException
    {
        blueprints = new HashMap<String, Blueprint>();
        pagetemplates = new HashMap<String, PageTemplate>();

    }

    public void reset() throws CacheException
    {
        blueprints = new HashMap<String, Blueprint>();
        pagetemplates = new HashMap<String, PageTemplate>();

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
        if (!this.pagetemplates.containsKey(page.getName())) {
            this.pagetemplates.put(page.getName(), page);
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
        return this.pagetemplates.get(name);
    }

    public List<Blueprint> getBlueprints()
    {
        return new ArrayList<Blueprint>(this.blueprints.values());
    }

    public List<PageTemplate> getPagetemplates()
    {
        return new ArrayList<PageTemplate>(this.pagetemplates.values());
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

    public LinkedHashSet<String> getBlocksScripts()
    {
        return this.blocksScripts;
    }

    public LinkedHashSet<String> getBlocksLinks()
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

                for (ResourceSearchResult htmlFile : htmlFiles) {
                    Path relativeAbsolutedPath = Paths.get("/").resolve(htmlFile.getResourceFolder().relativize(htmlFile.getResource()));

                    try (Reader reader = Files.newBufferedReader(htmlFile.getResource(), Charset.forName(Charsets.UTF_8.name()))) {
                        Template template = R.templateEngine().getNewStringTemplate(IOUtils.toString(reader));
                        //TODO bram: we should incorporate the language here?
                        String html = template.render();
                        for (Locale language : Blocks.config().getLanguages().values()) {
                            FileAnalyzer.AnalyseHtmlFile(html, language);
                        }
                    }
                    catch (ParseException e) {
                        Logger.error("Parse error while fetching page-templates and blueprints from file '" + htmlFile + "'.", e);
                    }
                }

                // Parse all after finding all
                // Add missing blueprints to default language

                for (Blueprint blueprint : this.getBlueprints()) {
                    blueprint.parse();

                    if (blueprint.isAddableBlock())
                        this.addableblocks.add(blueprint.getName());
                    if (blueprint.isPageBlock())
                        this.pageblocks.add(blueprint.getName());

                    for (Locale language : Blocks.config().getLanguages().values()) {
                        blueprint.setBlockId(UrlTools.createLocalResourceId("Blueprint", blueprint.getName()));
//                        EntityRepository.instance().save(blueprint, language);
                    }
                }

                for (PageTemplate pageTemplate : this.getPagetemplates()) {
                    pageTemplate.parse();

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
