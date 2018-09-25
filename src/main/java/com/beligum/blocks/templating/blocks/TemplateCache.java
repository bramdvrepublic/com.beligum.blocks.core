/*
 * Copyright 2017 Republic of Reinvention bvba. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.beligum.blocks.templating.blocks;

import com.beligum.base.resources.ClasspathSearchResult;
import com.beligum.base.server.R;
import com.beligum.base.utils.Logger;
import com.beligum.blocks.caching.CacheKeys;
import com.google.common.base.Joiner;
import net.htmlparser.jericho.Source;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
     * Eg. /imports/blocks/doIsValid/tag.html will have the name "blocks-doIsValid-tag"
     */
    private static String[] INVISIBLE_START_FOLDERS = { RESOURCES_IMPORTS_FOLDER };

    //-----VARIABLES-----
    private static Object initMutex = new Object();
    private static boolean initCalled = false;

    private Map<String, HtmlTemplate> nameMapping;
    private Map<String, HtmlTemplate> relativePathMapping;
    private Set<HtmlTemplate> pageTemplates;
    private Set<HtmlTemplate> disabledTemplates;

    //NOTE see resetCache() if you add variables here
    private String cachedSpacedTagNames;
    private String cachedCsvTagNames;
    private String cachedCssReset;
    private String cachedJsArray;

    //-----CONSTRUCTORS-----
    private TemplateCache() throws IOException
    {
        this.nameMapping = new LinkedHashMap<>();
        this.relativePathMapping = new LinkedHashMap<>();
        this.pageTemplates = new LinkedHashSet<>();
        this.disabledTemplates = new LinkedHashSet<>();

        this.searchAllTemplates();
    }
    public static TemplateCache instance()
    {
        TemplateCache retVal = (TemplateCache) R.cacheManager().getApplicationCache().get(CacheKeys.TAG_TEMPLATES);
        if (retVal == null) {
            synchronized (initMutex) {
                if (retVal == null) {
                    // Here, we iterate the classpath to search for all html files with <template> tags.
                    // It is a bit of a tricky situation: when building and parsing the html import templates,
                    // we'll possibly encounter recursive calls to this constructor method. This would create
                    // infinite recursion.
                    //
                    // To make sure this doesn't happen and the cache is constructed with a full and sound list of
                    // detected html templates, we created a detection system to throw an exception if this
                    // happens, so the creator of the libraries is responsible of not using the template cache
                    // during creation of the html templates
                    //
                    // Note that we need to use another guard because the initMutex above passes just fine
                    // during recursion because it all happens in the same thread. This also means we can't allow any updates
                    // (directly via the setters or leaked via any exposed methods in the objects returned by the getters)
                    // before the searchAllTemplates() method below ends because they will be overwritten.
                    if (!initCalled) {
                        initCalled = true;

                        try {
                            R.cacheManager().getApplicationCache().put(CacheKeys.TAG_TEMPLATES, retVal = new TemplateCache());
                        }
                        catch (IOException e) {
                            Logger.error("Caught exception while searching for all the webcomponent templates in the current classpath; this is bad and needs to fixed", e);
                        }
                    }
                    else {
                        throw new RuntimeException("Recursive template cache initialization detected. This is forbidden, please update the code so this doesn't happen");
                    }
                }
            }
        }

        return retVal;
    }

    //-----PUBLIC METHODS-----
    /**
     * Returns the template attached to the tag name (eg. blocks-scripts)
     */
    public HtmlTemplate getByTagName(String templateTagName)
    {
        return !StringUtils.isEmpty(templateTagName) ? this.nameMapping.get(templateTagName) : null;
    }
    /**
     * Returns the template attached to the relative path (eg. /imports/blocks/blah.html)
     */
    public HtmlTemplate getByRelativePath(String templateRelativePath)
    {
        return !StringUtils.isEmpty(templateRelativePath) ? this.relativePathMapping.get(templateRelativePath) : null;
    }
    /**
     * Inserts an entry by supplying the relative classpath.
     * Note that it still should start with a slash, though; eg. /imports/blocks/blah.html
     * But make sure it doesn't have any schema.
     */
    public HtmlTemplate update(HtmlTemplate template, Source htmlSource) throws Exception
    {
        //double-check it's really here and make sure we don't change the cache instances
        HtmlTemplate existingTemplate = this.getByTagName(template.getTemplateName());
        if (existingTemplate == null) {
            throw new IOException("Can't update a html template " + template + " because it doesn't exist in the cache");
        }

        //this will re-init all fields of the template
        existingTemplate.init(template.getTemplateName(), htmlSource, template.getAbsolutePath(), template.getRelativePath(), template.getSuperTemplate());

        this.resetCache();

        return existingTemplate;
    }
    /**
     * Returns all templates in the cache
     */
    public Collection<HtmlTemplate> getAllTemplates()
    {
        return this.nameMapping.values();
    }
    /**
     * Disables the supplied template in all contexts
     */
    public void disableTemplate(HtmlTemplate template)
    {
        this.disabledTemplates.add(template);

        this.nameMapping.values().remove(template);
        this.relativePathMapping.values().remove(template);
        this.pageTemplates.remove(template);

        this.resetCache();
    }
    /**
     * Disables the template when it's used in the context of the page
     */
    public void disableTemplate(TagTemplate template, PageTemplate page)
    {
        template.addDisabledContext(page);
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
            synchronized (this) {
                if (this.cachedSpacedTagNames == null) {
                    this.cachedSpacedTagNames = Joiner.on(" ").join(this.nameMapping.keySet());
                }
            }
        }

        return this.cachedSpacedTagNames;
    }
    public String getAllTagNamesCsv()
    {
        if (this.cachedCsvTagNames == null) {
            synchronized (this) {
                if (this.cachedCsvTagNames == null) {
                    this.cachedCsvTagNames = Joiner.on(",").join(this.nameMapping.keySet());
                }
            }
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
            synchronized (this) {
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
            }
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
            synchronized (this) {
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
            }
        }

        return cachedJsArray;
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----
    /**
     * Inserts an entry by supplying the relative classpath.
     * Note that it still should start with a slash, though; eg. /imports/blocks/blah.html
     * But make sure it doesn't have any schema.
     */
    private void addTemplate(String templateRelativePath, HtmlTemplate template)
    {
        //both should be synched, so one retval = other retval
        this.relativePathMapping.put(templateRelativePath, template);
        this.nameMapping.put(template.getTemplateName(), template);

        if (template instanceof PageTemplate) {
            this.pageTemplates.add(template);
        }

        this.resetCache();
    }
    private void resetCache()
    {
        this.cachedSpacedTagNames = null;
        this.cachedCsvTagNames = null;
        this.cachedCssReset = null;
        this.cachedJsArray = null;
    }

    //-----CONSTRUCTOR METHODS-----
    private void searchAllTemplates() throws IOException
    {
        Logger.info("Iterating the classpath to search for all html import templates");
        List<ClasspathSearchResult> htmlFiles = new ArrayList<>();
        htmlFiles.addAll(R.resourceManager().getClasspathHelper().searchResourceGlob("/" + RESOURCES_IMPORTS_FOLDER + "/**.{html,htm}"));

        // first, we'll keep a reference to all the templates with the same name in the path
        // they're returned priority-first, so the parents and grandparents will end up deepest in the list
        Map<String, List<Path[]>> inheritanceTree = new HashMap<>();
        for (ClasspathSearchResult htmlFile : htmlFiles) {
            Path absolutePath = htmlFile.getResource();
            //note the toString(); it works around files found in jar files and throwing a ProviderMismatchException
            Path relativePath = Paths.get("/").resolve(htmlFile.getResourceFolder().relativize(htmlFile.getResource()).toString());
            String templateName = this.parseTemplateName(relativePath);

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

                    String source = HtmlParser.eatTemplateComments(inputStream);

                    HtmlTemplate template = HtmlTemplate.create(templateName, new Source(source), absolutePath, relativePath, parent);
                    if (template != null) {
                        // Note: because this will return the files in priority order, don't overwrite an already parsed template,
                        // because we want to be able to 'overload' the files with our priority system.
                        // we're iterating in reverse order for the parent system, so keep the 'last' = last (grand)child
                        if (i == 0) {
                            // Note: it's important this string is just that: the relative path, but make sure it starts with a slash (it's relative to the classpath),
                            // but more importantly; no schema (because the lookup above in parse() expects that)
                            this.addTemplate(relativePath.toString(), template);
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
    private String parseTemplateName(Path relativePath) throws IOException
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
                throw new IOException("The name of an import template should always contain at least one dash; '" + retVal + "' in " + relativePath);
            }
        }

        return retVal;
    }

}
