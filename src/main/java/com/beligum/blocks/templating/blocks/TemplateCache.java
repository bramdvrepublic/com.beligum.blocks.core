package com.beligum.blocks.templating.blocks;

import com.google.common.base.Joiner;

import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by bram on 5/10/15.
 */
public class TemplateCache
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    private Map<String, HtmlTemplate> nameMapping;
    private Map<Path, HtmlTemplate> pathMapping;

    //NOTE see resetCache() if you add variables here
    private String cachedSpacedTagNames;
    private String cachedCsvTagNames;
    private String cachedCssReset;


    //-----CONSTRUCTORS-----
    public TemplateCache()
    {
        this.nameMapping = new HashMap<String, HtmlTemplate>();
        this.pathMapping = new HashMap<Path, HtmlTemplate>();

        this.resetCache();
    }

    //-----PUBLIC METHODS-----
    public HtmlTemplate get(String templateTagName)
    {
        return this.nameMapping.get(templateTagName);
    }
    public HtmlTemplate get(Path templateAbsolutePath)
    {
        return this.pathMapping.get(templateAbsolutePath);
    }
    public HtmlTemplate put(Path templateAbsolutePath, HtmlTemplate template)
    {
        //both should be synched, so one retval = other retval
        HtmlTemplate retVal = this.pathMapping.put(templateAbsolutePath, template);
        this.nameMapping.put(template.getTemplateName(), template);

        this.resetCache();

        return retVal;
    }
    public Collection<HtmlTemplate> values()
    {
        return this.nameMapping.values();
    }
    public boolean containsKey(String key)
    {
        return this.nameMapping.containsKey(key);
    }
    public boolean containsKey(Path key)
    {
        return this.pathMapping.containsKey(key);
    }
    public void clear()
    {
        this.nameMapping.clear();
        this.pathMapping.clear();
    }

    public String getAllTagNamesBySpace()
    {
        if (this.cachedSpacedTagNames ==null) {
            this.cachedSpacedTagNames = Joiner.on(" ").join(this.nameMapping.keySet());
        }

        return this.cachedSpacedTagNames;
    }
    /**
     * Returns and caches css code that resets our custom template tags
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

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----
    private void resetCache()
    {
        this.cachedSpacedTagNames = null;
        this.cachedCsvTagNames = null;
    }

    //-----PRIVATE CLASSES-----
}
