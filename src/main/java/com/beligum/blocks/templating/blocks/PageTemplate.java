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
    protected PageTemplate(String templateName, Source document, Path absolutePath, Path relativePath, HtmlTemplate parent) throws Exception
    {
        this.init(templateName, document, absolutePath, relativePath, parent);
    }

    //-----PUBLIC METHODS-----
    @Override
    public boolean renderTemplateTag()
    {
        return false;
    }

    //-----PROTECTED METHODS-----
    @Override
    protected OutputDocument doInitHtmlPreparsing(Source document, OutputDocument output, HtmlTemplate parent) throws IOException
    {
        // note that it doesn't really make sense to do something with the parent here;
        // the html of a page always needs to be there and I don't know a reason why we would use the html of a possible parent

        // some extra preprocessing is fill in the template attribute with the name of the template
        // so we know what template was used when the code comes back from the client
        Element html = document.getFirstElement("template", null);
        if (!html.getName().equalsIgnoreCase("html")) {
            throw new IOException("Found a template attribute on a non-html element, this shouldn't happen since it's been checked before; "+relativePath);
        }

        // fill in the template attribute's value
        // a little bit verbose, but I didn't find a shorter way...
        Attributes templateAttr = html.getAttributes();
        Map<String,String> attrs = new LinkedHashMap<>();
        templateAttr.populateMap(attrs, true);
        attrs.put("template", this.getTemplateName());
        output.replace(templateAttr, Attributes.generateHTML(attrs));

        return output;
    }

    //-----PRIVATE METHODS-----
}
