/*
 * Copyright 2017 Republic of Reinvention bvba. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.beligum.blocks.rdf.sources;

import com.beligum.base.i18n.I18nFactory;
import com.beligum.base.resources.MimeTypes;
import com.beligum.base.resources.ResourceInputStream;
import com.beligum.base.resources.ifaces.Source;
import com.beligum.base.resources.sources.AbstractSource;
import com.beligum.base.server.R;
import com.beligum.base.utils.UriDetector;
import com.beligum.base.utils.toolkit.StringFunctions;
import com.beligum.blocks.templating.blocks.HtmlParser;
import com.beligum.blocks.templating.blocks.analyzer.HtmlAnalyzer;
import com.google.common.base.Charsets;
import com.google.common.collect.Sets;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Entities;
import org.jsoup.select.Elements;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriBuilder;
import java.io.*;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by bram on 1/23/16.
 */
public abstract class PageSource extends AbstractSource implements Source
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
     * --> End of discussion, switched to @about because it's the right thing
     */
    public static final String HTML_ROOT_SUBJECT_ATTR = "about";
    public static final String HTML_ROOT_TYPEOF_ATTR = "typeof";
    public static final String HTML_ROOT_VOCAB_ATTR = HtmlParser.RDF_VOCAB_ATTR;
    public static final String HTML_ROOT_PREFIX_ATTR = HtmlParser.RDF_PREFIX_ATTR;
    public static final String HTML_TITLE_ELEMENT = "title";

    private static final Charset DEFAULT_CHARSET = Charsets.UTF_8;

    /*
     * These are the supported query params that are relevant to the storage/retrieval of pages.
     * All other ones will be removed from the URI during postparse.
     */
    public static final Set<String> SUPPORTED_QUERY_PARAMS = Sets.newHashSet(
                    I18nFactory.LANG_QUERY_PARAM
    );

    //-----VARIABLES-----
    protected Document document;
    protected Element htmlTag;
    protected HtmlAnalyzer htmlAnalyzer;
    protected Entities.EscapeMode escapeMode;

    //-----CONSTRUCTORS-----
    protected PageSource(URI uri, String html) throws IOException
    {
        this(uri, new ByteArrayInputStream(html.getBytes(DEFAULT_CHARSET)));
    }
    protected PageSource(URI uri, URI stream) throws IOException
    {
        //note: the language will be set in parseHtml()
        super(preparseUri(uri), MimeTypes.HTML, null);

        InputStream is = null;
        try {
            if (stream.getScheme().equals("file")) {
                is = new FileInputStream(new File(stream));
            }
            else {
                is = stream.toURL().openStream();
            }

            this.parseHtml(is);
        }
        finally {
            IOUtils.closeQuietly(is);
        }
    }
    protected PageSource(Source source) throws IOException
    {
        //note: the language will be set in parseHtml()
        super(preparseUri(source.getUri()), MimeTypes.HTML, null);

        try (InputStream is = source.newInputStream()) {
            this.parseHtml(is);
        }
    }
    protected PageSource(URI uri, InputStream html) throws IOException
    {
        //note: the language will be set in parseHtml()
        super(preparseUri(uri), MimeTypes.HTML, null);

        this.parseHtml(html);
    }
    protected PageSource(URI uri)
    {
        //note: the language will be set in parseHtml()
        super(preparseUri(uri), MimeTypes.HTML, null);

        //Note: subclass must call parseHtml() manually
    }

    //-----PUBLIC METHODS-----
    /**
     * This will remove all query params from the supplied URI so that the only ones left,
     * are the ones relevant to the storage/retrieval system of a page,
     * eg. removing all search/sort/... related params.
     */
    public static URI cleanQueryParams(URI unsafeUri)
    {
        //remove all unsupported query params from the page URI, so it doesn't matter if we
        // eg. save a page while on page 6 in a search result, or in the root; they should resolve to
        //     and save the same page, from use user-admin's point of view.
        MultivaluedMap<String, String> queryParams = StringFunctions.getQueryParameters(unsafeUri);
        UriBuilder uriBuilder = UriBuilder.fromUri(unsafeUri);
        for (Map.Entry<String, List<String>> param : queryParams.entrySet()) {
            if (!SUPPORTED_QUERY_PARAMS.contains(param.getKey())) {
                uriBuilder.replaceQueryParam(param.getKey(), null);
            }
        }

        return uriBuilder.build();
    }
    /**
     * Note that by default this will return _X_HTML
     */
    @Override
    public ResourceInputStream newInputStream() throws IOException
    {
        return new ResourceInputStream(this.toXHtmlString(), this.document.charset());
    }
    /**
     * Note this always returns -1 because we only render out the HTML document lazily in newInputStream() (where it's size is correctly filled in)
     *
     * @return
     */
    @Override
    public long getSize() throws IOException
    {
        return -1;
    }

    //-----HTMl-ONLY PUBLIC METHODS-----
    public String toHtmlString()
    {
        return this.document.outputSettings(this.buildNewStreamOutputSettings(this.document)
                                                .syntax(Document.OutputSettings.Syntax.html)
                                                .charset(DEFAULT_CHARSET)
        ).outerHtml();
    }
    public String toXHtmlString()
    {
        return this.document.outputSettings(this.buildNewStreamOutputSettings(this.document)
                                                .syntax(Document.OutputSettings.Syntax.xml)
                                                .charset(DEFAULT_CHARSET)
        ).outerHtml();
    }
    public String getNormalizedHtml() throws IOException
    {
        return this.getHtmlAnalyzer().getNormalizedHtml();
    }

    //-----PROTECTED METHODS-----
    protected void parseHtml(InputStream source) throws IOException
    {
        this.document = Jsoup.parse(source, null, this.getUri().toString());

        // Clean the document (doesn't work because it strips the head out)
        //Whitelist whitelist = Whitelist.relaxed();
        //doc = new Cleaner(whitelist).clean(doc);

        // Adjust escape mode
        //doc.outputSettings().escapeMode(Entities.EscapeMode.base);

        //some basic validation and preparsing to make sure we can apply all features later on.
        // - the document must contain a <html> tag
        Elements htmlTags = this.document.getElementsByTag("html");
        if (htmlTags.isEmpty()) {
            throw new IOException("The supplied HTML value to a HtmlSource wrapper must contain a <html> tag; " + this.getUri());
        }
        else {
            this.htmlTag = htmlTags.first();
        }

        //we'll normalize everything to XHTML (this means the newInputStream will also return xhtml)
        this.document.outputSettings().syntax(Document.OutputSettings.Syntax.xml);

        //Note: there seems to be a problem with ampersands in inline CSS not being escaped during the parsing of the html,
        //this is a workaround for that situation
        Elements styleEls = this.document.select("style");
        for (Element el : styleEls) {
            String html = R.resourceManager().getFingerprinter().detectAllResourceUris(el.html(), new UriDetector.ReplaceCallback()
            {
                @Override
                public String uriDetected(String uriStr)
                {
                    //Make sure we don't escape the ampersand of an "&amp;" to "&amp;amp;" by unescaping first
                    uriStr = StringEscapeUtils.unescapeXml(uriStr);
                    uriStr = StringEscapeUtils.escapeXml10(uriStr);
                    return uriStr;
                }
            });

            el.html(html);
        }
    }

    //-----PRIVATE METHODS-----
    /**
     * Reason this is static is because it's called from the super() call in our constructor
     */
    private static URI preparseUri(URI unsafeUri)
    {
        //avoid directory attacks
        unsafeUri = unsafeUri.normalize();

        //note that we need an absolute URI (eg. to have a correct root RDF context for Sesame),
        // but we allow for relative URIs to be imported -> just make them absolute based on the current settings
        if (!unsafeUri.isAbsolute()) {
            unsafeUri = R.configuration().getSiteDomain().resolve(unsafeUri);
        }

        unsafeUri = cleanQueryParams(unsafeUri);

        return unsafeUri;
    }
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
    /**
     * This allows us to trim the values when parsing RDFA html
     */
    private Document.OutputSettings buildNewStreamOutputSettings(Document doc)
    {
        Document.OutputSettings retVal = doc.outputSettings().indentAmount(0).prettyPrint(false);

        if (this.escapeMode != null) {
            retVal.escapeMode(this.escapeMode);
        }

        return retVal;
    }

    //-----MANAGEMENT METHODS-----
    @Override
    public String toString()
    {
        return "HtmlSource{" +
               "baseUri=" + this.getUri() +
               ", document=" + document +
               '}';
    }
}
