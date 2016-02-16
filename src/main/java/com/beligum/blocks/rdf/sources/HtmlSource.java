package com.beligum.blocks.rdf.sources;

import com.beligum.base.server.R;
import com.beligum.base.utils.Logger;
import com.beligum.blocks.config.Settings;
import com.beligum.blocks.fs.HdfsPathInfo;
import com.beligum.blocks.fs.ifaces.PathInfo;
import com.beligum.blocks.fs.pages.DefaultPageImpl;
import com.beligum.blocks.templating.blocks.HtmlAnalyzer;
import org.apache.hadoop.fs.FileContext;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import javax.ws.rs.core.UriBuilder;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

/**
 * Created by bram on 1/23/16.
 */
public abstract class HtmlSource implements com.beligum.blocks.rdf.ifaces.Source
{
    //-----CONSTANTS-----
    public static final String HTML_ROOT_LANG_ATTR = "lang";

    public static final String HTML_TRANSLATION_ELEMENT = "link";
    public static final String HTML_TRANSLATION_ATTR_REL = "rel";
    public static final String HTML_TRANSLATION_ATTR_REL_VALUE = "alternate";
    public static final String HTML_TRANSLATION_ATTR_TYPE = "type";
    public static final String HTML_TRANSLATION_ATTR_HREFLANG = "hreflang";
    public static final String HTML_TRANSLATION_ATTR_HREF = "href";

    public static final String HTML_TITLE_ELEMENT = "title";

    //-----VARIABLES-----
    protected URI baseUri;
    protected Document document;
    protected Element htmlTag;
    protected HtmlAnalyzer htmlAnalyzer;
    private Map<URI, Locale> pageTranslations;

    //-----CONSTRUCTORS-----
    protected HtmlSource(URI baseUri) throws IOException, URISyntaxException
    {
        this.baseUri = baseUri;
        this.document = null;
        this.htmlAnalyzer = null;
    }

    //-----PUBLIC METHODS-----
    @Override
    public URI getBaseUri()
    {
        return baseUri;
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
        // actually, the html tag can have both the @lang and the @xml:lang attribute.
        // See https://www.w3.org/TR/html-rdfa/#specifying-the-language-for-a-literal
        // We'll be a little opportunistic here and adjust all "lang" attributes (ignoring namespaces)
        // This is also interesting: https://www.w3.org/International/questions/qa-html-language-declarations
        // --> "Use the lang attribute for pages served as HTML, and the xml:lang attribute for pages served as XML."
        if (adjustLanguage) {
            //see http://tools.ietf.org/html/rfc4646 for ISO guidelines -> "shortest ISO 639 code"
            this.htmlTag.attr(HTML_ROOT_LANG_ATTR, R.i18nFactory().getOptimalLocale(this.baseUri).getLanguage());
        }

        //the opposite of pretty printing is compacting
        if (compact) {
            this.document.outputSettings().prettyPrint(false);
        }
    }
    /**
     * This will try to find all translations of this source (based on existing file structures)
     * and fill in, but also update (add/delete) <link> tags in the source.
     * Make sure you call this method before using the openNewInputStream() because it might change the source.
     * @return returns the actual translations map, reflecting the latest state of the html
     */
    @Override
    public void processTranslations(FileContext fs) throws IOException
    {
        //parse the translations for this page; this variable will eventually contain all translations of the page
        this.pageTranslations = new LinkedHashMap<>();
        //this fetches the translations present in the HTML source
        Map<URI, HtmlAnalyzer.TranslationRef> sourceTranslations = this.getHtmlAnalyzer().getTranslations();
        for (Map.Entry<URI, HtmlAnalyzer.TranslationRef> t : sourceTranslations.entrySet()) {
            this.pageTranslations.put(t.getKey(), t.getValue().locale);
        }
        //see below
        Set<URI> sourceTranslationsToDelete = new HashSet<>();
        //this is the lang attribute in the <html> tag
        Locale sourceLang = this.getHtmlLocale();
        if (sourceLang!=null) {
            Map<URI, Locale> existingTranslationPages = new HashMap<>();
            Map<String, Locale> siteLanguages = Settings.instance().getLanguages();
            for (Map.Entry<String, Locale> l : siteLanguages.entrySet()) {
                Locale lang = l.getValue();
                if (!lang.equals(sourceLang)) {
                    UriBuilder translatedUri = UriBuilder.fromUri(this.getBaseUri());
                    Locale detectedLang = R.i18nFactory().getUrlLocale(this.getBaseUri(), translatedUri, lang);
                    if (detectedLang != null) {
                        URI transPagePublicUri = translatedUri.build();
                        URI transPageResourceUri = DefaultPageImpl.toResourceUri(transPagePublicUri, Settings.instance().getPagesStorePath());
                        PathInfo transPagePathInfo = new HdfsPathInfo(fs, transPageResourceUri);
                        if (fs.util().exists(transPagePathInfo.getPath())) {
                            existingTranslationPages.put(transPagePublicUri, lang);
                            this.pageTranslations.put(transPagePublicUri, lang);
                        }
                        else {
                            // if we came all this way and detect there's a translation in the html that doesn't exist on disk,
                            // then clean it up.
                            if (sourceTranslations.containsKey(transPagePublicUri)) {
                                sourceTranslationsToDelete.add(transPagePublicUri);
                                this.pageTranslations.remove(transPagePublicUri);
                                Logger.warn("Encountered a translation (" + transPagePublicUri + ") in a html page (" + this.getBaseUri() + ") that doesn't exist (anymore?) on disk, so I'm deleting it while I'm saving the page.");
                            }
                        }
                    }
                }
            }

            // here, we have a list of public URIs that have stored pages on disk
            // that form the (url-based) translations of the requested page, together with
            // a list of translate-URIs in the html that don't exist on disk (anymore).
            // So first add, then delete.
            this.addTranslations(existingTranslationPages);
            this.removeTranslations(sourceTranslationsToDelete);
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
        return this.getHtmlAnalyzer().getHtmlLanguage();
    }
    public Set<URI> getInternalRefs() throws IOException
    {
        //don't think we need to expose the attribute details for now
        return this.getHtmlAnalyzer().getInternalRefs().keySet();
    }
    public Set<URI> getExternalRefs() throws IOException
    {
        return this.getHtmlAnalyzer().getExternalRefs().keySet();
    }
    public String getTitle() throws IOException
    {
        return this.getHtmlAnalyzer().getTitle();
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
            throw new IOException("The supplied HTML value to a HtmlSource wrapper must contain a <html> tag; "+this.getBaseUri());
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
        if (this.htmlAnalyzer==null) {
            this.htmlAnalyzer = new HtmlAnalyzer();
            this.htmlAnalyzer.analyze(this, true);
        }

        return this.htmlAnalyzer;
    }
    private void addTranslations(Map<URI, Locale> publicUris)
    {
        for (Map.Entry<URI, Locale> e : publicUris.entrySet()) {
            //makes sense to prepend it instead of appending it, no?
            this.document.head().prependElement(HTML_TRANSLATION_ELEMENT)
                         .attr(HTML_TRANSLATION_ATTR_REL, HTML_TRANSLATION_ATTR_REL_VALUE)
                         .attr(HTML_TRANSLATION_ATTR_TYPE, com.beligum.base.resources.ifaces.Resource.MimeType.HTML.getMimeType().toString())
                         .attr(HTML_TRANSLATION_ATTR_HREFLANG, e.getValue().getLanguage())
                         .attr(HTML_TRANSLATION_ATTR_HREF, e.getKey().toString());
        }
    }
    private void removeTranslations(Set<URI> publicUris)
    {
        //optimization, because there'll be a lot of <link> tags
        if (publicUris!=null && !publicUris.isEmpty()) {
            Elements translations = this.document.head().getElementsByTag(HTML_TRANSLATION_ELEMENT);
            for (Element transEl : translations) {
                String rel = transEl.attr(HTML_TRANSLATION_ATTR_REL);
                String type = transEl.attr(HTML_TRANSLATION_ATTR_TYPE);
                if (rel.equalsIgnoreCase(HTML_TRANSLATION_ATTR_REL_VALUE) && type.equalsIgnoreCase(com.beligum.base.resources.ifaces.Resource.MimeType.HTML.getMimeType().toString())) {
                    URI href = URI.create(transEl.attr(HTML_TRANSLATION_ATTR_HREF));
                    //Note: we don't check the hreflang, just the uri, can't have two uri's pointing to the same language
                    if (publicUris.contains(href)) {
                        transEl.remove();
                    }
                }
            }
        }
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
