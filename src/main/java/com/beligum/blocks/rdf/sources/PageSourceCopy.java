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
    }

    //-----PUBLIC METHODS-----

    //-----PROTECTED METHODS-----
    @Override
    protected void parseHtml(InputStream source) throws IOException
    {
        super.parseHtml(source);

        //We'll remove all attributes we know so the new page doesn't have
        // any backreferences to the old page we're copying from.
        //Note: we can't just delete all attributes because we eg. can't touch the page template attribute

        this.language = null;
        this.htmlTag.removeAttr(HTML_ROOT_LANG_ATTR);
        this.htmlTag.removeAttr(HTML_ROOT_PREFIX_ATTR);
        this.htmlTag.removeAttr(HTML_ROOT_SUBJECT_ATTR);
        this.htmlTag.removeAttr(HTML_ROOT_TYPEOF_ATTR);
        this.htmlTag.removeAttr(HTML_ROOT_VOCAB_ATTR);
    }

    //-----PRIVATE METHODS-----

}
