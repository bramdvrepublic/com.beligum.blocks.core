package com.beligum.blocks.rdf.sources;

import com.beligum.base.i18n.I18nFactory;
import com.beligum.blocks.rdf.ifaces.Source;
import com.beligum.blocks.templating.blocks.HtmlAnalyzer;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.Set;

/**
 * Created by bram on 1/23/16.
 */
public abstract class HtmlSource implements Source
{
    //-----CONSTANTS-----
    public static final String HTML_ROOT_LANG_ATTR = "lang";

    //-----VARIABLES-----
    protected URI baseUri;
    protected Document document;
    protected Element htmlTag;
    protected HtmlAnalyzer htmlAnalyzer;

    //-----CONSTRUCTORS-----
    protected HtmlSource(URI baseUri) throws IOException, URISyntaxException
    {
        this.baseUri = baseUri;
        this.document = null;
    }

    //-----PUBLIC METHODS-----
    @Override
    public URI getBaseUri()
    {
        return baseUri;
    }
    /**
     * This does the required (html-universal) processing before writing it to disk.
     *
     * @param adjustLanguage flag to modify the "lang" attribute of the <html> tag to the current request language
     * @param compact enable or disable compacting (non-pretty printing) of the code
     */
    @Override
    public void prepareForSaving(boolean adjustLanguage, boolean compact)
    {
        // actually, the html tag can have both the @lang and the @xml:lang attribute.
        // See https://www.w3.org/TR/html-rdfa/#specifying-the-language-for-a-literal
        // We'll be a little opportunistic here and adjust all "lang" attributes (ignoring namespaces)
        // This is also interesting: https://www.w3.org/International/questions/qa-html-language-declarations
        // --> "Use the lang attribute for pages served as HTML, and the xml:lang attribute for pages served as XML."
        if (adjustLanguage) {
            //see http://tools.ietf.org/html/rfc4646 for ISO guidelines -> "shortest ISO 639 code"
            this.htmlTag.attr(HTML_ROOT_LANG_ATTR, I18nFactory.instance().getOptimalLocale(this.baseUri).getLanguage());
        }

        //the opposite of pretty printing is compacting
        if (compact) {
            this.document.outputSettings().prettyPrint(false);
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
    public Locale getHtmlLocale() throws IOException
    {
        return this.getHtmlAnalyzer().getHtmlLocale();
    }
    public Set<URI> getInternalRefs() throws IOException
    {
        //don't think we need to expose the attribute details for now
        return this.getHtmlAnalyzer().getInternalRefs().keySet();
    }
    public Set<URI> getExternalRefs() throws IOException
    {
        //don't think we need to expose the attribute details for now
        return this.getHtmlAnalyzer().getExternalRefs().keySet();
    }
    public Set<URI> getTranslations() throws IOException
    {
        //don't think we need to expose the tag details for now
        return this.getHtmlAnalyzer().getTranslations().keySet();
    }

    //-----PROTECTED METHODS-----
    /**
     * Initializes the jsoup document.
     */
    protected void init() throws IOException
    {
        // Clean the document (doesn't work because it strips the head out)
        //Whitelist whitelist = Whitelist.relaxed();
        //doc = new Cleaner(whitelist).clean(doc);

        // Adjust escape mode
        //doc.outputSettings().escapeMode(Entities.EscapeMode.base);

        //some basic validation and preparsing to make sure we can apply all features later on.
        // - the document must contain a <html> tag
        Elements htmlTags = this.document.getElementsByTag("html");
        if (htmlTags.isEmpty()) {
            throw new IOException("The supplied HTML value to a HtmlSource wrapper must contain a <html> tag; "+this.getBaseUri());
        }
        else {
            this.htmlTag = htmlTags.first();
        }

        //we'll normalize everything to XHTML
        this.document.outputSettings().syntax(Document.OutputSettings.Syntax.xml);

        //make sure the

        //no serialization yet, we might have to apply tweaks later on..
    }

    //-----PRIVATE METHODS-----
    /**
     * Lazy loaded html analyzer (that analyzes on first load)
     * @return
     */
    private HtmlAnalyzer getHtmlAnalyzer() throws IOException
    {
        if (this.htmlAnalyzer==null) {
            this.htmlAnalyzer = new HtmlAnalyzer();
            this.htmlAnalyzer.analyze(this, true);
        }

        return this.htmlAnalyzer;
    }

    //-----MANAGEMENT METHODS-----
    @Override
    public String toString()
    {
        return "HtmlSource{" +
               "baseUri=" + baseUri +
               ", document=" + document +
               '}';
    }
}
