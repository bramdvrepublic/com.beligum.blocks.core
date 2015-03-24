package com.beligum.blocks.core.mongocache;

import com.beligum.blocks.core.config.BlocksConfig;
import com.beligum.blocks.core.dbs.BlocksTemplateCache;
import com.beligum.blocks.core.exceptions.CacheException;
import com.beligum.blocks.core.exceptions.ParseException;
import com.beligum.blocks.core.models.nosql.Blueprint;
import com.beligum.blocks.core.models.nosql.PageTemplate;
import com.beligum.blocks.core.models.nosql.StoredTemplate;
import com.beligum.blocks.core.models.redis.templates.AbstractTemplate;
import com.beligum.blocks.core.parsers.FileAnalyzer;
import com.beligum.blocks.core.parsers.MongoVisitor.reset.BlocksScriptVisitor;
import com.beligum.blocks.core.parsers.Traversor;
import com.beligum.core.framework.base.R;
import com.beligum.core.framework.utils.Logger;
import com.beligum.core.framework.utils.toolkit.FileFunctions;
import org.apache.shiro.util.AntPathMatcher;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

/**
 * Created by wouter on 17/03/15.
 */
public class TemplateCache implements BlocksTemplateCache
{
    private static TemplateCache instance;
    private boolean runningTroughHtmlTemplates = false;
    private AntPathMatcher pathMatcher = new AntPathMatcher();
    private HashMap<String, HashMap<String, Blueprint>> blueprints = new HashMap<String, HashMap<String, Blueprint>>();
    private HashMap<String, HashMap<String, PageTemplate>> pagetemplates = new HashMap<String, HashMap<String, PageTemplate>>();
    private Set<String> pageblocks = new HashSet<String>();
    private Set<String> addableblocks = new HashSet<String>();
    private LinkedHashSet<String> blocksScripts = new LinkedHashSet<>();
    private LinkedHashSet<String> blocksLinks = new LinkedHashSet<>();


    public TemplateCache() throws CacheException
    {
        blueprints = new HashMap<String, HashMap<String, Blueprint>>();
        pagetemplates = new HashMap<String, HashMap<String, PageTemplate>>();
        for (String lang: BlocksConfig.getLanguages()) {
            blueprints.put(lang, new HashMap<String, Blueprint>());
            pagetemplates.put(lang, new HashMap<String, PageTemplate>());
        }
    }

    public void reset() throws CacheException
    {
        blueprints = new HashMap<String, HashMap<String, Blueprint>>();
        pagetemplates = new HashMap<String, HashMap<String, PageTemplate>>();
        for (String lang: BlocksConfig.getLanguages()) {
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
                retVal = this.blueprints.get(BlocksConfig.getDefaultLanguage()).get(name);
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
        for (Blueprint bp: getBlueprints(BlocksConfig.getDefaultLanguage())) {
            if (bp.isPageBlock()) list.add(bp);
        }

        return list;
    }

    public List<Blueprint> getAddableBlocks() {
        ArrayList<Blueprint> list = new ArrayList<>();
        for (Blueprint bp: getBlueprints(BlocksConfig.getDefaultLanguage())) {
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
     * @throws com.beligum.blocks.core.exceptions.CacheException
     */
    protected void fillCache() throws CacheException
    {
        if (!runningTroughHtmlTemplates) {
            runningTroughHtmlTemplates = true;


            try {
                List<Path> allResourceFolders = findAllResourceFolders();

                for (Path resourceFolder : allResourceFolders) {
                    Path templatesFolder = resourceFolder.resolve(BlocksConfig.getTemplateFolder());

                    if (Files.exists(templatesFolder)) {
                        //list which will be filled up with all templates found in all files in the templates-folder
                        final List<AbstractTemplate> foundTemplates = new ArrayList<>();
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
                                        for (String language: BlocksConfig.getLanguages()) {
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
                for (String lang: BlocksConfig.getLanguages()) {
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
                Document doc = visitor.getSource(BlocksConfig.getFrontEndScripts());
                Traversor traversor = new Traversor(visitor);
                traversor.traverse(doc);
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

    protected List<Path> findAllResourceFolders() throws Exception
    {
        return FileFunctions.searchResourcesInClasspath(FileFunctions.getClasswideSearchFolder(), new FileFunctions.ResourceSearchPathFilter()
        {
            @Override
            public Path doFilter(Path path)
            {
                //since the URI is the META-INF folder, we're looking for it's parent, the root (resources) folder
                return path.getParent();
            }
        });
    }



}
