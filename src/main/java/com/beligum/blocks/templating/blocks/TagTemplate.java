package com.beligum.blocks.templating.blocks;

import net.htmlparser.jericho.Element;
import net.htmlparser.jericho.OutputDocument;
import net.htmlparser.jericho.Source;

import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by bram on 5/13/15.
 */
public class TagTemplate extends HtmlTemplate
{
    //-----CONSTANTS-----

    //-----CONSTRUCTORS-----
    protected TagTemplate(String templateName, Source document, Path absolutePath, Path relativePath, HtmlTemplate parent) throws Exception
    {
        this.init(templateName, document, absolutePath, relativePath, parent);
    }

    //-----PUBLIC METHODS-----
    @Override
    public boolean renderTemplateTag()
    {
        return true;
    }

    //-----PROTECTED METHODS-----
    @Override
    protected OutputDocument doInitHtmlPreparsing(Source document, OutputDocument output, HtmlTemplate parent) throws IOException
    {
        List<Element> templateElements = document.getAllElements("template");

        // Note that there always needs to be a <template> tag to indicate this is a template
        if (templateElements != null && !templateElements.isEmpty()) {
            if (templateElements.size() != 1) {
                throw new IOException("Encountered tag template with more than one <template> tags ("+templateElements.size()+"); " + absolutePath);
            }

            Element templateTag = templateElements.get(0);

            //FIRST, parse the attributes
            //we'll merge the attributes of the <template> tag from the parent, but let them be overridden by this child template
            Map<String, String> attrs = new LinkedHashMap<>();
            if (parent!=null && parent.getAttributes()!=null) {
                attrs = parent.getAttributes();
            }
            if (templateTag.getAttributes()!=null) {
                //note that populate just does a put() so this works well to override the parent properties
                templateTag.getAttributes().populateMap(attrs, true);
            }
            this.setAttributes(attrs);

            //THEN, parse the body
            //if the <template> tag is empty and we have a parent, inherit the content from the parent
            //note that .isEmpty() also returns true when the element has attributes
            if (templateTag.isEmpty() && parent!=null) {
                output = new OutputDocument(parent.getHtml());
            }
            else {
                //we'll 'ignore' the code around the <template> tag
                output.replace(document, templateTag.getContent());
            }

            return output;
        }
        else {
            throw new IOException("Encountered tag template with an invalid <template> tag config (found " + (templateElements == null ? null : templateElements.size()) + " tags); " + absolutePath);
        }
    }

    //-----PRIVATE METHODS-----
}
