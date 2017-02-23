package com.beligum.blocks.rdf.sources;

import com.beligum.base.resources.ifaces.Source;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by bram on 1/8/17.
 */
public class PageSourceCopy extends PageSource
{
    //-----CONSTANTS-----

    //-----VARIABLES-----

    //-----CONSTRUCTORS-----
    public PageSourceCopy(Source source) throws IOException
    {
        super(source.getUri());

        try (InputStream is = source.newInputStream()) {
            this.parseHtml(is);
        }

        this.postparseUri();
    }

    //-----PUBLIC METHODS-----

    //-----PROTECTED METHODS-----
    @Override
    protected void parseHtml(InputStream source) throws IOException
    {
        super.parseHtml(source);

        //We'll remove all language attributes because 9/10 we're creating a copy from another language
        //note that it will be added again on save
        this.language = null;
        this.htmlTag.removeAttr(HTML_ROOT_LANG_ATTR);
    }

    //-----PRIVATE METHODS-----

}
