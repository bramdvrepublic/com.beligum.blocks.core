package com.beligum.blocks.templating.blocks;

import com.beligum.base.resources.ClasspathSearchResult;
import com.beligum.base.server.R;
import com.beligum.base.utils.Logger;
import com.beligum.blocks.caching.CacheKeys;
import com.google.common.base.Joiner;
import net.htmlparser.jericho.Source;
import org.apache.commons.lang3.StringUtils;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.util.*;

/**
 * Created by bram on 5/10/15.
 */
public class TemplateCache
{
    //-----CONSTANTS-----
    /**
     * The root resource folder where all templates will be searched
     */
    private static String RESOURCES_IMPORTS_FOLDER = "imports";

    /**
     * These are the names of first folders that won't be taken into account when building the name of the element
     * Eg. /imports/blocks/test/tag.html will have the name "blocks-test-tag"
     */
    private static String[] INVISIBLE_START_FOLDERS = { RESOURCES_IMPORTS_FOLDER };

    //-----VARIABLES-----
    private Map<String, HtmlTemplate> nameMapping;
    private Map<String, HtmlTemplate> relativePathMapping;
    private List<HtmlTemplate> pageTemplates;

    //NOTE see resetCache() if you add variables here
    private String cachedSpacedTagNames;
    private String cachedCsvTagNames;
    private String cachedCssReset;
    private String cachedJsArray;

    //-----CONSTRUCTORS-----
    private TemplateCache()
    {
        this.nameMapping = new HashMap<>();
        this.relativePathMapping = new HashMap<>();
        this.pageTemplates = new ArrayList<>();

        this.resetCache();
    }
    public static TemplateCache instance()
    {
        TemplateCache retVal = (TemplateCache) R.cacheManager().getApplicationCache().get(CacheKeys.TAG_TEMPLATES);
        if (retVal == null) {
            R.cacheManager().getApplicationCache().put(CacheKeys.TAG_TEMPLATES, retVal = new TemplateCache());
            try {
                searchAllTemplates(retVal);
            }
            catch (Exception e) {
                Logger.error("Caught exception while searching for all the webcomponent templates in the current classpath; this is bad and needs to fixed", e);
            }
        }

        return retVal;
    }

    //-----PUBLIC METHODS-----
    public void flush()
    {
        R.cacheManager().getApplicationCache().remove(CacheKeys.TAG_TEMPLATES);
    }
    public HtmlTemplate getByTagName(String templateTagName)
    {
        return StringUtils.isEmpty(templateTagName) ? null : this.nameMapping.get(templateTagName);
    }
    public HtmlTemplate getByRelativePath(String templateRelativePath)
    {
        return StringUtils.isEmpty(templateRelativePath) ? null : this.relativePathMapping.get(templateRelativePath);
    }
    public List<HtmlTemplate> getPageTemplates()
    {
        return this.pageTemplates;
    }
    /**
     * Inserts an entry by supplying the relative classpath. Note that it still should start with a slash though;
     * eg. /imports/blocks/blah.html
     * But make sure it doesn't have any schema.
     */
    public HtmlTemplate putByRelativePath(String templateRelativePath, HtmlTemplate template)
    {
        //both should be synched, so one retval = other retval
        HtmlTemplate retVal = this.relativePathMapping.put(templateRelativePath, template);
        this.nameMapping.put(template.getTemplateName(), template);

        if (template instanceof PageTemplate) {
            this.pageTemplates.add(template);
        }

        this.resetCache();

        return retVal;
    }
    public Collection<HtmlTemplate> values()
    {
        return this.nameMapping.values();
    }
    public boolean containsKeyByTagName(String key)
    {
        return this.nameMapping.containsKey(key);
    }
    public boolean containsKeyByRelativePath(String key)
    {
        return this.relativePathMapping.containsKey(key);
    }
    public void clear()
    {
        this.nameMapping.clear();
        this.relativePathMapping.clear();
        this.pageTemplates.clear();
    }

    public String getAllTagNamesBySpace()
    {
        if (this.cachedSpacedTagNames == null) {
            this.cachedSpacedTagNames = Joiner.on(" ").join(this.nameMapping.keySet());
        }

        return this.cachedSpacedTagNames;
    }
    public String getAllTagNamesCsv()
    {
        if (this.cachedCsvTagNames == null) {
            this.cachedCsvTagNames = Joiner.on(",").join(this.nameMapping.keySet());
        }

        return this.cachedCsvTagNames;
    }

    /**
     * Returns and caches css code that resets our custom template tags
     *
     * @return
     */
    public String getCssReset()
    {
        if (this.cachedCssReset == null) {
            StringBuilder css = new StringBuilder();
            boolean first = true;
            for (HtmlTemplate htmlTemplate : this.nameMapping.values()) {
                if (!first) {
                    css.append(",").append("\n");
                }
                css.append(htmlTemplate.getTemplateName());
                first = false;
            }

            css.append(" {").append("\n");
            css.append("\t").append("display: block;").append("\n");
            css.append("\t").append("margin: 0;").append("\n");
            css.append("\t").append("padding: 0;").append("\n");
            css.append("}");

            this.cachedCssReset = css.toString();
        }

        return cachedCssReset;
    }
    /**
     * Returns and caches javascript code of an array with all the tag names of our custom template tags
     *
     * @return
     */
    public String getJsArray()
    {
        if (this.cachedJsArray == null) {
            StringBuilder js = new StringBuilder();
            js.append("base.plugin(\"blocks.imports.All\", function () ").append("{").append("this.IMPORTS = [");

            boolean first = true;
            for (HtmlTemplate htmlTemplate : this.nameMapping.values()) {
                if (!first) {
                    js.append(",");
                }
                js.append("'").append(htmlTemplate.getTemplateName()).append("'");
                first = false;
            }

            js.append("];").append("});");

            this.cachedJsArray = js.toString();
        }

        return cachedJsArray;
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----
    private static void searchAllTemplates(TemplateCache templateCache) throws Exception
    {
        //start with a clean slate
        templateCache.clear();

        List<ClasspathSearchResult> htmlFiles = new ArrayList<>();
        htmlFiles.addAll(R.resourceManager().getClasspathHelper().searchResourceGlob("/" + RESOURCES_IMPORTS_FOLDER + "/**.{html,htm}"));

        // first, we'll keep a reference to all the templates with the same name in the path
        // they're returned priority-first, so the parents and grandparents will end up deepest in the list
        Map<String, List<Path[]>> inheritanceTree = new HashMap<>();
        for (ClasspathSearchResult htmlFile : htmlFiles) {
            Path absolutePath = htmlFile.getResource();
            //note the toString(); it works around files found in jar files and throwing a ProviderMismatchException
            Path relativePath = Paths.get("/").resolve(htmlFile.getResourceFolder().relativize(htmlFile.getResource()).toString());
            String templateName = TemplateCache.parseTemplateName(relativePath);

            List<Path[]> entries = inheritanceTree.get(templateName);
            if (entries == null) {
                inheritanceTree.put(templateName, entries = new ArrayList<>());
            }

            //let's keep both so we don't have to recalculate
            entries.add(new Path[] { absolutePath, relativePath });
        }

        // now iterate the list in reverse order to parse the grandparents first
        // and be able to pass them to the overloading children
        for (Map.Entry<String, List<Path[]>> entry : inheritanceTree.entrySet()) {
            String templateName = entry.getKey();
            List<Path[]> inheritList = entry.getValue();
            HtmlTemplate parent = null;
            for (int i = inheritList.size() - 1; i >= 0; i--) {
                Path[] absRelArr = inheritList.get(i);

                Path absolutePath = absRelArr[0];
                Path relativePath = absRelArr[1];

                try (InputStream inputStream = Files.newInputStream(absolutePath)) {

                    String source = HtmlParser.eatVelocityComments(inputStream);

                    HtmlTemplate template = HtmlTemplate.create(templateName, new Source(source), absolutePath, relativePath, parent);
                    if (template != null) {
                        // Note: because this will return the files in priority order, don't overwrite an already parsed template,
                        // because we want to be able to 'overload' the files with our priority system.
                        // we're iterating in reverse order for the parent system, so keep the 'last' = last (grand)child
                        if (i == 0) {
                            // Note: it's important this string is just that: the relative path, but make sure it starts with a slash (it's relative to the classpath),
                            // but more importantly; no schema (because the lookup above in parse() expects that)
                            templateCache.putByRelativePath(relativePath.toString(), template);
                        }

                        //this can be used by the next loop
                        parent = template;
                    }
                    else {
                        Logger.warn("Encountered null-valued html template after parsing it; " + absolutePath.toString());
                    }
                }
                catch (Exception e) {
                    Logger.error("Exception caught while parsing a possible tag template file; " + absolutePath, e);
                }
            }
        }
    }
    private static String parseTemplateName(Path relativePath) throws ParseException
    {
        String retVal = null;

        Path namePath = relativePath;
        if (relativePath != null) {
            for (String invisiblePrefix : INVISIBLE_START_FOLDERS) {
                if (namePath.startsWith(invisiblePrefix) || namePath.startsWith(namePath.getFileSystem().getSeparator() + invisiblePrefix)) {
                    namePath = namePath.subpath(1, namePath.getNameCount());
                    //this is a safe choice that might change in the future: do we want to keep eating first folders? Of so, then we should actually created over, no?
                    break;
                }
            }
            retVal = StringUtils.strip(namePath.toString().replaceAll("/", "-"), "-");
            int lastDot = retVal.lastIndexOf(".");
            if (lastDot >= 0) {
                retVal = retVal.substring(0, lastDot);
            }
            //note: we may want to let the user override the name with an id attribute on the <template> tag

            // In Web Components speak, this new element is a Custom Element,
            // and the only two requirements are that its name must contain a dash,
            // and its prototype must extend HTMLElement.
            // See https://css-tricks.com/modular-future-web-components/
            if (!retVal.contains("-")) {
                throw new ParseException("The name of an import template should always contain at least one dash; '" + retVal + "' in " + relativePath, 0);
            }
        }

        return retVal;
    }
    private void resetCache()
    {
        this.cachedSpacedTagNames = null;
        this.cachedCsvTagNames = null;
        this.cachedCssReset = null;
        this.cachedJsArray = null;
    }

    //-----PRIVATE CLASSES-----
}
