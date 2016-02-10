package com.beligum.blocks.templating.blocks;

import com.google.common.base.Joiner;

import java.util.*;

/**
 * Created by bram on 5/10/15.
 */
public class TemplateCache
{
    //-----CONSTANTS-----

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
    public TemplateCache()
    {
        this.nameMapping = new HashMap<>();
        this.relativePathMapping = new HashMap<>();
        this.pageTemplates = new ArrayList<>();

        this.resetCache();
    }

    //-----PUBLIC METHODS-----
    public HtmlTemplate getByTagName(String templateTagName)
    {
        return this.nameMapping.get(templateTagName);
    }
    public HtmlTemplate getByRelativePath(String templateRelativePath)
    {
        return this.relativePathMapping.get(templateRelativePath);
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
    private void resetCache()
    {
        this.cachedSpacedTagNames = null;
        this.cachedCsvTagNames = null;
        this.cachedCssReset = null;
        this.cachedJsArray = null;
    }

    //-----PRIVATE CLASSES-----
}
