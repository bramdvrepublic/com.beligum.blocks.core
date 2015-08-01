package com.beligum.blocks.templating.blocks;

import net.htmlparser.jericho.Element;
import net.htmlparser.jericho.Source;

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
        List<Element> templateElements = document.getAllElements("template");
        if (templateElements != null && !templateElements.isEmpty() && templateElements.size() == 1) {
            Element templateTag = templateElements.get(0);
            this.initAll(document, templateTag.getContent(), templateTag.getAttributes(), absolutePath, relativePath);
        }
        else {
            throw new Exception("Encountered tag template with an invalid <template> tag config (found " + (templateElements == null ? null : templateElements.size()) + " tags); " + absolutePath);
        }
    }

    //-----PUBLIC METHODS-----
    @Override
    public boolean renderTemplateTag()
    {
        return true;
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----
}
