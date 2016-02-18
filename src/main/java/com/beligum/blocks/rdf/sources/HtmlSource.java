package com.beligum.blocks.rdf.sources;

import com.beligum.base.server.R;
import com.beligum.blocks.config.Settings;
import com.beligum.blocks.fs.HdfsResourcePath;
import com.beligum.blocks.fs.pages.DefaultPageImpl;
import com.beligum.blocks.fs.pages.ifaces.Page;
import com.beligum.blocks.templating.blocks.HtmlAnalyzer;
import com.beligum.blocks.utils.RdfTools;
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
    public static final String HTML_ROOT_RESOURCE_ATTR = "resource";
    public static final String HTML_ROOT_TYPEOF_ATTR = "typeof";
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
     * For now, it does:
     *  - modify the "lang" attribute of the <html> tag to the current request language
     *  - compact (non-pretty printing) of the code
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
        this.updateBaseResourceAndType(fileContext, this.htmlTag);

        //make the input html a bit more uniform
        this.document.outputSettings().prettyPrint(true);

        //force a re-analyze (and hope it hasn't been analyzed yet)
        this.htmlAnalyzer = null;
    }
    /**
     * Note that this will return _X_HTML
     */
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
            this.htmlAnalyzer = new HtmlAnalyzer(this, true);
        }

        return this.htmlAnalyzer;
    }
    private void updateBaseResourceAndType(FileContext fileContext, Element htmlTag) throws IOException
    {
        URI resource = null;

        String resourceAttr = htmlTag.attr(HTML_ROOT_RESOURCE_ATTR);
        String typeofAttr = htmlTag.attr(HTML_ROOT_TYPEOF_ATTR);
        //See simpleflake docs and http://akmanalp.com/simpleflake_presentation/#/12
        if (resourceAttr.isEmpty()) {
            // If we don't have a resourceId, we check if this page is the translation of another page
            // by looking up other files by exchanging the (possible) language-part in the url.
            HtmlAnalyzer translationAnalyzer = this.findTranslationAnalyzer(fileContext);
            if (translationAnalyzer != null) {

                resource = URI.create(translationAnalyzer.getHtmlResource().value);

                //this means we can override the value of the translation if we provide our own, custom typeof;
                //it's flexible, but maybe we should force-lock it to the translation's typeof?
                if (typeofAttr.isEmpty() && translationAnalyzer.getHtmlTypeof() != null) {
                    typeofAttr = translationAnalyzer.getHtmlTypeof().value;
                }
            }
        }
        else {
            resource = URI.create(resourceAttr);
        }

        // If still nothing was found, this is a true new page (and thus we generate a new resource id)
        if (resource==null) {
            //it makes sense to make the link relative; we're much more future proof this way
            resource = RdfTools.createRelativeResourceId(typeofAttr);

            //for new pages, we default to a simple webpage; note that if a typeof is provided and no resource,
            // the new resource will receive the typeof that was set, that ok?
            if (typeofAttr.isEmpty()) {
                typeofAttr = new com.beligum.blocks.rdf.schema.Page().getResourceUriClassName();
            }
        }

        htmlTag.attr(HTML_ROOT_RESOURCE_ATTR, resource.toString());

        //note: we don't set the typeof attribute if there's no resource attribute set
        if (!typeofAttr.isEmpty()) {
            htmlTag.attr(HTML_ROOT_TYPEOF_ATTR, typeofAttr);
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
                        HtmlAnalyzer.AttributeRef transPageResource = analyzer.getHtmlResource();
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
