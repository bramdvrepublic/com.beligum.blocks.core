package com.beligum.blocks.serializing;

import com.beligum.base.filesystem.ConstantsFileEntry;
import com.beligum.base.utils.Logger;
import com.beligum.blocks.rdf.ifaces.RdfProperty;
import com.beligum.blocks.templating.HtmlTemplate;
import com.beligum.blocks.templating.TagTemplate;
import com.beligum.blocks.templating.TemplateCache;
import com.google.common.base.Joiner;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Created by bram on Aug 20, 2019
 */
public abstract class AbstractBlockSerializer implements BlockSerializer
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    private static Set<TagTemplate> cachedSupportedBlocks;

    //-----CONSTRUCTORS-----

    //-----PUBLIC METHODS-----
    @Override
    public CharSequence toHtml(TagTemplate blockType, RdfProperty property, Locale language, String value) throws IOException
    {
        return this.toHtml(blockType, property, language, null, null, value);
    }
    @Override
    public Iterable<TagTemplate> getSupportedBlockTypes()
    {
        if (cachedSupportedBlocks == null) {
            cachedSupportedBlocks = new LinkedHashSet<>();
            for (String tagName : this.getSupportedBlockNames()) {
                HtmlTemplate tag = TemplateCache.instance().getByTagName(tagName);
                if (tag instanceof TagTemplate) {
                    cachedSupportedBlocks.add((TagTemplate) tag);
                }
                else {
                    Logger.error("Encountered unsupported tag template. Skipping, but this shouldn't happen and needs to be fixed; " + tagName);
                }
            }
        }

        return cachedSupportedBlocks;
    }


    //-----PROTECTED METHODS-----
    protected abstract String[] getSupportedBlockNames();
    protected Element createTag(String tagName)
    {
        return this.createTag(tagName, (String[]) null);
    }
    protected Element createTag(TagTemplate blockType)
    {
        return this.createTag(blockType.getTemplateName(), (String[]) null);
    }
    protected Element createTag(TagTemplate blockType, String[][] arguments, String... classes)
    {
        return this.createTag(blockType.getTemplateName(), arguments, classes);
    }
    protected Element createTag(String tagName, String... classes)
    {
        return this.createTag(tagName, null, classes);
    }
    protected Element createTag(TagTemplate blockType, String... classes)
    {
        return this.createTag(blockType.getTemplateName(), classes);
    }
    protected Element createTag(String tagName, String[][] arguments, String... classes)
    {
        Element retVal = new Element(tagName);

        // first add the general arguments
        if (arguments != null) {
            for (String[] a : arguments) {
                if (a.length != 2) {
                    Logger.error("Encountered tag argument with the wrong structure, skipping. Please fix this; " + a);
                }
                else {
                    retVal.attr(a[0], a[1]);
                }
            }
        }

        // then the classes
        this.addClasses(retVal, classes);

        return retVal;
    }
    /**
     * Creates a html tag with the specified name and arguments
     */
    protected Element createTag(String tagName, Map<String, String> arguments, ConstantsFileEntry[] classes, Map<String, String> styles)
    {
        Element retVal = new Element(tagName);

        // first add the general arguments
        if (arguments != null) {
            for (Map.Entry<String, String> a : arguments.entrySet()) {
                if (a.getKey().equalsIgnoreCase("class")) {
                    Logger.warn("Found a \"class\" argument in the general arguments list, skipping. Please use the designated parameter instead; " + a);
                }
                else if (a.getKey().equalsIgnoreCase("style")) {
                    Logger.warn("Found a \"style\" argument in the general arguments list, skipping. Please use the designated parameter instead; " + a);
                }
                else {
                    retVal.attr(a.getKey(), a.getValue());
                }
            }
        }

        // then the classes
        this.addClasses(retVal, classes);

        // then the styles
        this.addStyles(retVal, styles);

        return retVal;
    }
    protected Element createTag(TagTemplate blockType, Map<String, String> arguments, ConstantsFileEntry[] classes, Map<String, String> styles)
    {
        return this.createTag(blockType.getTemplateName(), arguments, classes, styles);
    }
    /**
     * Appends the specified CSS classes to the stringbuilder
     */
    protected void addClasses(Element element, ConstantsFileEntry... classes)
    {
        if (classes != null) {
            for (ConstantsFileEntry c : classes) {
                element.addClass(c.getValue().trim());
            }
        }
    }
    /**
     * Appends the specified CSS classes to the stringbuilder
     */
    protected void addClasses(Element element, String... classes)
    {
        if (classes != null) {
            for (String c : classes) {
                element.addClass(c.trim());
            }
        }
    }
    /**
     * Appends the specified CSS style pairs to the stringbuilder
     */
    protected void addStyles(Element element, Map<String, String> styles)
    {
        if (styles != null && styles.size() > 0) {
            String existingStyles = null;
            if (element.hasAttr("style")) {
                existingStyles = element.attr("style");
            }

            String newStyles = Joiner.on(";").skipNulls().withKeyValueSeparator(": ").join(styles);

            // if the new styles have no extra information, there's nothing to do
            if (!StringUtils.isBlank(newStyles)) {
                if (!StringUtils.isBlank(existingStyles)) {
                    newStyles = existingStyles + ";" + newStyles;
                }
                element.attr("style", newStyles.trim());
            }
        }
    }

    //-----PRIVATE METHODS-----
}
