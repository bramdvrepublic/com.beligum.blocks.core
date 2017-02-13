package com.beligum.blocks.templating.blocks;

import com.beligum.base.resources.ifaces.Source;
import net.htmlparser.jericho.Element;
import net.htmlparser.jericho.OutputDocument;

import java.net.URI;
import java.util.Locale;

/**
 * Created by bram on 5/27/15.
 */
public interface TemplateController
{
    /**
     * Internal method to set the config values, no need to use this method directly
     */
    TemplateConfig putConfig(String key, String value);

    /**
     * Internal method to clear the config values, no need to use this method directly
     */
    TemplateController resetConfig();

    /**
     * This method is called every time the controller is initialized for a template created (you can safely assume the putConfig has been set here)
     */
    void created();

    /**
     * This method is called just before a page is saved.
     * If you want to change anything to the content of the html beforehand, this is the time.
     * Make sure you do your own replacements to the output document.
     * Note that we supply a Jericho element (instead of a JSoup element) to make low-level edits possible.
     */
    void prepareForSave(Source source, Element element, OutputDocument htmlOutput);

    /**
     * This method is called just before a copy is created from the supplied source element to the target uri and language.
     * If you want to change anything to the content of the html before a copy is made, this is the time.
     * Make sure you do your own replacements to the output document.
     * Note that we supply a Jericho element (instead of a JSoup element) to make low-level edits possible.
     */
    void prepareForCopy(Source source, Element element, OutputDocument htmlOutput, URI targetUri, Locale targetLanguage);
}
