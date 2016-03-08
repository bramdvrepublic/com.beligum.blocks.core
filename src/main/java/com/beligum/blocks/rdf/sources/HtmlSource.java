package com.beligum.blocks.rdf.sources;

import com.beligum.base.server.R;
import com.beligum.blocks.config.Settings;
import com.beligum.blocks.fs.HdfsResourcePath;
import com.beligum.blocks.fs.pages.DefaultPageImpl;
import com.beligum.blocks.fs.pages.ifaces.Page;
import com.beligum.blocks.rdf.ontology.Classes;
import com.beligum.blocks.templating.blocks.HtmlAnalyzer;
import com.beligum.blocks.utils.RdfTools;
import org.apache.commons.lang3.StringUtils;
import org.apache.hadoop.fs.FileContext;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import javax.ws.rs.core.UriBuilder;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Locale;
import java.util.Map;

/**
 * Created by bram on 1/23/16.
 */
public abstract class HtmlSource implements com.beligum.blocks.rdf.ifaces.Source
{
    //-----CONSTANTS-----
    public static final String HTML_ROOT_LANG_ATTR = "lang";
    /**
     * Actually, it would be better to use an @about attribute here,
     * but for our (current) uses, the effect is the same.
     * (see remarks about @resource in https://www.w3.org/TR/rdfa-syntax/#typing-resources-with-typeof).
     * However, I'm still not 100% convinced we should prefer @resource over @about for the
     * main top-level subject statement. The main reason I'm going with @resource now is
     * simplicity (eg. it's the only one of the two that's present in RDFa Lite).
     * --> End of discussion, switched to @about because it the right thing
     */
    public static final String HTML_ROOT_SUBJECT_ATTR = "about";
    public static final String HTML_ROOT_TYPEOF_ATTR = "typeof";
    public static final String HTML_ROOT_VOCAB_ATTR = "vocab";
    public static final String HTML_TITLE_ELEMENT = "title";

    //-----VARIABLES-----
    protected URI sourceAddress;
    protected Document.OutputSettings documentOutputSettings;
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
     * For now, it does:
     *  - modify the "lang" attribute of the <html> tag to the current request language
     *  - set or update the "resource" and "typeof" attribute
     *  - clean up the the code
     */
    @Override
    public void prepareForSaving(FileContext fileContext) throws IOException
    {
        // actually, the html tag can have both the @lang and the @xml:lang attribute.
        // See https://www.w3.org/TR/html-rdfa/#specifying-the-language-for-a-literal
        // and (good example) https://www.w3.org/TR/rdfa-syntax/#language-tags
        // We'll be a little opportunistic here and adjust all "lang" attributes (ignoring namespaces)
        // This is also interesting: https://www.w3.org/International/questions/qa-html-language-declarations
        // --> "Use the lang attribute for pages served as HTML, and the xml:lang attribute for pages served as XML."
        //see http://tools.ietf.org/html/rfc4646 for ISO guidelines -> "shortest ISO 639 code"
        this.htmlTag.attr(HTML_ROOT_LANG_ATTR, R.i18nFactory().getOptimalLocale(this.sourceAddress).getLanguage());

        //NOTE that the search for the base resource uses the language set in the previous code, so make sure this comes after it
        this.updateBaseAttributes(fileContext, this.htmlTag);

        //make the input html a bit more uniform
        this.document.outputSettings().prettyPrint(true);

        //force a re-analyze (and hope it hasn't been analyzed yet)
        this.htmlAnalyzer = null;
    }
    /**
     * Note that by default this will return _X_HTML
     */
    @Override
    public InputStream openNewInputStream() throws IOException
    {
        return this.openNewXHtmlInputStream();
    }
    public InputStream openNewHtmlInputStream() throws IOException
    {
        return new ByteArrayInputStream(this.document.outputSettings(this.document.outputSettings().syntax(Document.OutputSettings.Syntax.html)).outerHtml().getBytes(this.document.charset()));
    }
    public InputStream openNewXHtmlInputStream() throws IOException
    {
        return new ByteArrayInputStream(this.document.outputSettings(this.document.outputSettings().syntax(Document.OutputSettings.Syntax.xml)).outerHtml().getBytes(this.document.charset()));
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

        //we'll normalize everything to XHTML (this means the newInputStream will also return xhtml)
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
            this.htmlAnalyzer = new HtmlAnalyzer(this);
        }

        return this.htmlAnalyzer;
    }
    private void updateBaseAttributes(FileContext fileContext, Element htmlTag) throws IOException
    {
        String subjectAttr = htmlTag.attr(HTML_ROOT_SUBJECT_ATTR);
        String typeofAttr = htmlTag.attr(HTML_ROOT_TYPEOF_ATTR);
        String vocabAttr = htmlTag.attr(HTML_ROOT_VOCAB_ATTR);

        // If the saved page has no about attribute, we assume it's a newly saved paged and two things can happen:
        //  - There's another page with the same language in the system (like /en/new-page for /fr/new-page)
        //    If this is the case, we need to link the two pages together (eg; to allow them to move around to other URLs in the future),
        //    so we search for such a page and use the same base resource if we find one.
        //  - If there's not such a page, this is the very first page for this URL and this language, so we generate a new resource ID,
        //    based on the (B-tree friendly) SimpleFlake number (eg. see http://akmanalp.com/simpleflake_presentation/#/12)
        //
        // Note that we only consider changing the typeof attribute if we're changing the about attribute. Otherwise, we leave it alone.
        if (StringUtils.isEmpty(subjectAttr)) {

            URI newResource = null;
            URI newTypeOf = null;

            // If we don't have a resourceId, we check if this page is the translation of another page
            // by looking up other files by exchanging the (possible) language-part in the url.
            HtmlAnalyzer translationAnalyzer = this.findTranslationAnalyzer(fileContext);
            if (translationAnalyzer != null) {
                newResource = URI.create(translationAnalyzer.getHtmlAbout().value);

                //by only modifying if empty, this means we can override the value of the translation if we provide our own, custom typeof;
                //it's flexible, but maybe we should force-lock it to the translation's typeof?
                if (typeofAttr.isEmpty() && translationAnalyzer.getHtmlTypeof() != null) {
                    newTypeOf = URI.create(translationAnalyzer.getHtmlTypeof().value);
                }
            }

            // If nothing was found, this is a true new page and thus we generate a new resource id.
            // Note that we discard any possible supplied typeOf values in this case; we force it to be a page
            if (newResource==null) {
                //since the vocab is set to the same value as the vocab of the Page class, we can safely use the short version
                //Not any more: we're trying to always use the curie name as 'value' in dropdowns etc, so to make the type dropdown
                //              work, it needs to be a curie value
                newTypeOf = Classes.Page.getCurieName();
                newResource = RdfTools.createRelativeResourceId(Classes.Page);
            }

            htmlTag.attr(HTML_ROOT_SUBJECT_ATTR, newResource.toString());
            htmlTag.attr(HTML_ROOT_TYPEOF_ATTR, newTypeOf.toString());
        }

        if (StringUtils.isEmpty(vocabAttr)) {
            htmlTag.attr(HTML_ROOT_VOCAB_ATTR, Settings.instance().getRdfOntologyUri().toString());
            //TODO ideally, this should set the prefix too, but since it allows for multiple prefixes, it's more complex...
        }
    }
    private HtmlAnalyzer findTranslationAnalyzer(FileContext fileContext) throws IOException
    {
        HtmlAnalyzer retVal = null;

        Locale thisLang = R.i18nFactory().getUrlLocale(this.getSourceAddress());
        Map<String, Locale> siteLanguages = Settings.instance().getLanguages();
        for (Map.Entry<String, Locale> l : siteLanguages.entrySet()) {
            Locale lang = l.getValue();
            //we're searching for a translation, not the same language
            if (!lang.equals(thisLang)) {
                UriBuilder translatedUri = UriBuilder.fromUri(this.getSourceAddress());
                if (R.i18nFactory().getUrlLocale(this.getSourceAddress(), translatedUri, lang) != null) {
                    URI transPagePublicUri = translatedUri.build();
                    URI transPageResourceUri = DefaultPageImpl.toResourceUri(transPagePublicUri, Settings.instance().getPagesStorePath());
                    if (fileContext.util().exists(new org.apache.hadoop.fs.Path(transPageResourceUri))) {
                        Page transPage = new DefaultPageImpl(new HdfsResourcePath(fileContext, transPageResourceUri));
                        HtmlAnalyzer analyzer = transPage.createAnalyzer();
                        HtmlAnalyzer.AttributeRef transPageResource = analyzer.getHtmlAbout();
                        if (transPageResource!=null) {
                            retVal = analyzer;
                            break;
                        }
                    }
                }
            }
        }

        return retVal;
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
