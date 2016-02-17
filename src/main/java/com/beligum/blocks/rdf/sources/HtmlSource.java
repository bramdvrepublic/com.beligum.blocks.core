package com.beligum.blocks.rdf.sources;

import com.beligum.base.server.R;
import com.beligum.blocks.templating.blocks.HtmlAnalyzer;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

/**
 * Created by bram on 1/23/16.
 */
public abstract class HtmlSource implements com.beligum.blocks.rdf.ifaces.Source
{
    //-----CONSTANTS-----
    public static final String HTML_ROOT_LANG_ATTR = "lang";
    public static final String HTML_TITLE_ELEMENT = "title";

    //-----VARIABLES-----
    protected URI sourceAddress;
    protected Document document;
    protected Element htmlTag;
    protected HtmlAnalyzer htmlAnalyzer;

    //-----CONSTRUCTORS-----
    protected HtmlSource(URI sourceAddress) throws IOException
    {
        this.sourceAddress = sourceAddress;
        this.document = null;
        this.htmlAnalyzer = null;
    }

    //-----PUBLIC METHODS-----
    @Override
    public URI getSourceAddress()
    {
        return sourceAddress;
    }
    /**
     * This does the required (html-universal) processing before writing it to disk.
     * For performance sake, try to call this method before invoking the getters below,
     * cause it will trigger a re-analyze.
     *
     * @param adjustLanguage flag to modify the "lang" attribute of the <html> tag to the current request language
     * @param compact        enable or disable compacting (non-pretty printing) of the code
     */
    @Override
    public void prepareForSaving(boolean adjustLanguage, boolean compact) throws IOException
    {
        boolean changed = false;

        // actually, the html tag can have both the @lang and the @xml:lang attribute.
        // See https://www.w3.org/TR/html-rdfa/#specifying-the-language-for-a-literal
        // We'll be a little opportunistic here and adjust all "lang" attributes (ignoring namespaces)
        // This is also interesting: https://www.w3.org/International/questions/qa-html-language-declarations
        // --> "Use the lang attribute for pages served as HTML, and the xml:lang attribute for pages served as XML."
        if (adjustLanguage) {
            //see http://tools.ietf.org/html/rfc4646 for ISO guidelines -> "shortest ISO 639 code"
            this.htmlTag.attr(HTML_ROOT_LANG_ATTR, R.i18nFactory().getOptimalLocale(this.sourceAddress).getLanguage());
            changed = true;
        }

        //the opposite of pretty printing is compacting
        if (compact) {
            this.document.outputSettings().prettyPrint(false);
            changed = true;
        }

        //force a re-analyze if changed
        if (changed) {
            this.htmlAnalyzer = null;
        }
    }
    @Override
    public InputStream openNewInputStream() throws IOException
    {
        return new ByteArrayInputStream(this.document.outerHtml().getBytes(this.document.charset()));
    }

    //-----HTMl-ONLY PUBLIC METHODS-----
    public String getNormalizedHtml() throws IOException
    {
        return this.getHtmlAnalyzer().getNormalizedHtml();
    }

    //-----PROTECTED METHODS-----
    protected void initDocument() throws IOException
    {
        if (this.document == null) {
            throw new IOException("Can't prepare the html for saving without a document; this shouldn't happen");
        }

        // Clean the document (doesn't work because it strips the head out)
        //Whitelist whitelist = Whitelist.relaxed();
        //doc = new Cleaner(whitelist).clean(doc);

        // Adjust escape mode
        //doc.outputSettings().escapeMode(Entities.EscapeMode.base);

        //some basic validation and preparsing to make sure we can apply all features later on.
        // - the document must contain a <html> tag
        Elements htmlTags = this.document.getElementsByTag("html");
        if (htmlTags.isEmpty()) {
            throw new IOException("The supplied HTML value to a HtmlSource wrapper must contain a <html> tag; " + this.getSourceAddress());
        }
        else {
            this.htmlTag = htmlTags.first();
        }

        //we'll normalize everything to XHTML
        this.document.outputSettings().syntax(Document.OutputSettings.Syntax.xml);
    }

    //-----PRIVATE METHODS-----
    /**
     * Lazy loaded html analyzer (that analyzes on first load)
     *
     * @return
     */
    private HtmlAnalyzer getHtmlAnalyzer() throws IOException
    {
        if (this.htmlAnalyzer == null) {
            this.htmlAnalyzer = new HtmlAnalyzer(this, true);
        }

        return this.htmlAnalyzer;
    }

    //-----MANAGEMENT METHODS-----
    @Override
    public String toString()
    {
        return "HtmlSource{" +
               "baseUri=" + sourceAddress +
               ", document=" + document +
               '}';
    }
}
