package com.beligum.blocks.templating.blocks;

import net.htmlparser.jericho.Element;
import net.htmlparser.jericho.OutputDocument;
import net.htmlparser.jericho.Source;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * Created by bram on 5/13/15.
 */
public class TagTemplate extends HtmlTemplate
{
    //-----CONSTANTS-----

    //-----CONSTRUCTORS-----
    protected TagTemplate(Source document, Path absolutePath, Path relativePath) throws Exception
    {
        this.init(document, absolutePath, relativePath);
    }

    //-----PUBLIC METHODS-----
    @Override
    public boolean renderTemplateTag()
    {
        return true;
    }

    //-----PROTECTED METHODS-----
    @Override
    protected void doInitHtmlPreparsing(Source document, OutputDocument output) throws IOException
    {
        List<Element> templateElements = document.getAllElements("template");
        if (templateElements != null && !templateElements.isEmpty() && templateElements.size() == 1) {
            Element templateTag = templateElements.get(0);
            this.setAttributes(templateTag.getAttributes());

            //we'll 'ignore' the code around the <template> tag
            output.replace(document, templateTag.getContent());
        }
        else {
            throw new IOException("Encountered tag template with an invalid <template> tag config (found " + (templateElements == null ? null : templateElements.size()) + " tags); " + absolutePath);
        }
    }

    //-----PRIVATE METHODS-----
}
