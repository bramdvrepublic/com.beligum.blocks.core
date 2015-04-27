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


    public void addBlueprint(Blueprint blueprint) {
        if (!this.blueprints.containsKey(blueprint.getBlueprintName())) {
            this.blueprints.put(blueprint.getBlueprintName(), blueprint);
        }
    }

    public void addPageTemplate(PageTemplate page) {
        if (!this.pagetemplates.containsKey(page.getBlueprintName())) {
            this.pagetemplates.put(page.getName(), page);
        }
    }

    public void addBlueprint(Blueprint blueprint, String language) {
        if (!this.blueprints.containsKey(blueprint.getBlueprintName())) {
            this.blueprints.put(blueprint.getName(), blueprint);
        }
    }

    public void addPageTemplate(PageTemplate page, String language) {
        if (!this.pagetemplates.containsKey(page.getBlueprintName())) {
            this.pagetemplates.put(page.getName(), page);
        }
    }

    public Blueprint getBlueprint(String name) {
        Blueprint retVal = null;
        if (name != null) {
            retVal = this.blueprints.get(name);

        }
        return retVal;
    }

    public PageTemplate getPagetemplate(String name) {
        return this.pagetemplates.get(name);
    }

    public List<Blueprint> getBlueprints() {
        return new ArrayList<Blueprint>(this.blueprints.values());
    }

    public List<PageTemplate> getPagetemplates() {
        return new ArrayList<PageTemplate>(this.pagetemplates.values());
    }

    public List<Blueprint> getPageBlocks() {
        ArrayList<Blueprint> list = new ArrayList<>();
        for (Blueprint bp: getBlueprints()) {
            if (bp.isPageBlock()) list.add(bp);
        }

        return list;
    }

    public List<Blueprint> getAddableBlocks() {
        ArrayList<Blueprint> list = new ArrayList<>();
        for (Blueprint bp: getBlueprints()) {
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

                for (Blueprint blueprint : this.getBlueprints()) {
                    blueprint.parse();

                    if (blueprint.isAddableBlock()) this.addableblocks.add(blueprint.getName());
                    if (blueprint.isPageBlock()) this.pageblocks.add(blueprint.getName());
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
