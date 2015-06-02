package com.beligum.blocks.templating.blocks;

import net.htmlparser.jericho.Source;

import java.nio.file.Path;

/**
 * Created by bram on 5/13/15.
 */
public class PageTemplate extends HtmlTemplate
{
    //-----CONSTANTS-----

    //-----CONSTRUCTORS-----
    protected PageTemplate(Source document, Path absolutePath, Path relativePath) throws Exception
    {
        //note that we use the entire source as the html segment
        this.init(document, document, null, absolutePath, relativePath);
    }

    //-----PUBLIC METHODS-----
    @Override
    public boolean renderTemplateTag()
    {
        return false;
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----
}
