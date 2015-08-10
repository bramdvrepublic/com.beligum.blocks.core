package com.beligum.blocks.templating.blocks;

import net.htmlparser.jericho.Attributes;
import net.htmlparser.jericho.Element;
import net.htmlparser.jericho.OutputDocument;
import net.htmlparser.jericho.Source;

import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by bram on 5/13/15.
 */
public class PageTemplate extends HtmlTemplate
{
    //-----CONSTANTS-----

    //-----CONSTRUCTORS-----
    protected PageTemplate(Source document, Path absolutePath, Path relativePath) throws Exception
    {
        this.init(document, absolutePath, relativePath);
    }

    //-----PUBLIC METHODS-----
    @Override
    public boolean renderTemplateTag()
    {
        return false;
    }

    //-----PROTECTED METHODS-----
    @Override
    protected void doInitHtmlPreparsing(Source document, OutputDocument output) throws IOException
    {
        // some extra preprocessing is fill in the template attribute with the name of the template
        // so we know what template was used when the code comes back from the client
        Element html = document.getFirstElement("template", null);
        if (!html.getName().equalsIgnoreCase("html")) {
            throw new IOException("Found a template attribute on a non-html element, this shouldn't happen since it's been checked before; "+relativePath);
        }
        //a little bit verbose, but I didn't find a shorter way...
        Attributes templateAttr = html.getAttributes();
        Map<String,String> attrs = new LinkedHashMap<>();
        templateAttr.populateMap(attrs, true);
        attrs.put("template", this.getTemplateName());
        output.replace(templateAttr, Attributes.generateHTML(attrs));
    }

    //-----PRIVATE METHODS-----
}
