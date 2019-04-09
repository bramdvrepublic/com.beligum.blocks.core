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

package com.beligum.blocks.filesystem.pages;

import com.beligum.base.i18n.I18nFactory;
import com.beligum.base.models.Person;
import com.beligum.base.resources.MimeTypes;
import com.beligum.base.resources.ResourceInputStream;
import com.beligum.base.resources.ifaces.ResourceRequest;
import com.beligum.base.resources.ifaces.Source;
import com.beligum.base.resources.sources.AbstractSource;
import com.beligum.base.security.PermissionRole;
import com.beligum.base.server.R;
import com.beligum.base.utils.Logger;
import com.beligum.base.utils.UriDetector;
import com.beligum.base.utils.toolkit.StringFunctions;
import com.beligum.blocks.config.Settings;
import com.beligum.blocks.filesystem.pages.ifaces.PageMetadata;
import com.beligum.blocks.rdf.ifaces.RdfProperty;
import com.beligum.blocks.rdf.ontologies.Meta;
import com.beligum.blocks.templating.blocks.HtmlParser;
import com.beligum.blocks.templating.blocks.analyzer.HtmlAnalyzer;
import com.beligum.blocks.utils.SecurityTools;
import com.google.common.base.Charsets;
import com.google.common.collect.Sets;
import org.apache.commons.io.IOUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Entities;
import org.jsoup.select.Elements;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriBuilder;
import javax.xml.datatype.DatatypeFactory;
import java.io.*;
import java.net.URI;
import java.nio.charset.Charset;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.*;

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
    public static final String HTML_ROOT_SUBJECT_ATTR = HtmlParser.RDF_ABOUT_ATTR;
    public static final String HTML_ROOT_TYPEOF_ATTR = HtmlParser.RDF_TYPEOF_ATTR;
    public static final String HTML_ROOT_VOCAB_ATTR = HtmlParser.RDF_VOCAB_ATTR;
    public static final String HTML_ROOT_PREFIX_ATTR = HtmlParser.RDF_PREFIX_ATTR;
    public static final String HTML_TITLE_ELEMENT = "title";
    public static final String HTML_META_ELEMENT = "meta";

    private static final Charset DEFAULT_CHARSET = Charsets.UTF_8;

    /*
     * These are the supported query params that are relevant to the storage/retrieval of pages.
     * All other ones will be removed from the URI during postparse.
     */
    public static final Set<String> SUPPORTED_QUERY_PARAMS = Sets.newHashSet(
                    I18nFactory.LANG_QUERY_PARAM,
                    ResourceRequest.TYPE_QUERY_PARAM
    );

    //-----VARIABLES-----
    protected Document document;
    protected Element htmlTag;
    protected Element headTag;
    protected List<Element> metaTags;
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
                uriBuilder.replaceQueryParam(param.getKey(), (Object[]) null);
            }
        }

        return uriBuilder.build();
    }
    /**
     * Same as method above, but with the raw values in a MultivaluedMap that are to be transferred (or not) to a uriBuilder.
     * The cleaned query params are also returned in a MultivaluedMap
     */
    public static MultivaluedMap<String, String> transferCleanedQueryParams(UriBuilder uriBuilder, MultivaluedMap<String, String> queryParams)
    {
        MultivaluedMap<String, String> retVal = new MultivaluedHashMap<>();

        for (Map.Entry<String, List<String>> param : queryParams.entrySet()) {
            if (SUPPORTED_QUERY_PARAMS.contains(param.getKey())) {
                uriBuilder.replaceQueryParam(param.getKey(), param.getValue().toArray());
                retVal.addAll(param.getKey(), param.getValue());
            }
        }

        return retVal;
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
    public PageMetadata getMetadata() throws IOException
    {
        return new DefaultPageMetadata(this, this.metaTags);
    }
    public void updateMetadata(Person editor) throws IOException
    {
        try {
            ValueFactory valueFactory = SimpleValueFactory.getInstance();
            DatatypeFactory datatypeFactory = DatatypeFactory.newInstance();

            //we need custom conversion to store everything in UTC format
            GregorianCalendar nowUtc = GregorianCalendar.from(ZonedDateTime.now(ZoneOffset.UTC));
            String timestamp = valueFactory.createLiteral(datatypeFactory.newXMLGregorianCalendar(nowUtc)).stringValue();

            //let's use the user endpoint url to designate the editor
            //hope it's okay to save this as a relative URI (sinc we try to save all RDFa URIs as relative)
            String editorUri = R.securityManager().getPersonUri(editor, false).toString();

            PermissionRole currentRole = R.securityManager().getCurrentRole();

            //update the created date
            Element createdProp = this.findOrCreateElement(Meta.created, timestamp, true, false, false, false);

            //update the creator URL
            Element creatorProp = this.findOrCreateElement(Meta.creator, editorUri, true, false, false, false);

            //update the modified date
            Element modifiedProp = this.findOrCreateElement(Meta.modified, timestamp, true, true, false, false);

            //update the contributor list
            Element contributorProp = this.findOrCreateElement(Meta.contributor, editorUri, false, false, false, false);

            if (!Settings.instance().getDisableAcls()) {

                //Note: we switched to wiping blank permissions because we don't want to fill these on every save of a page
                //It would mean that every page would get it's custom permissions if it's saved once and we don't want that,

                //initialize the view ACL (no values are overwritten, only initialized)
                Element aclViewProp = this.findOrCreateElement(Meta.aclRead, String.valueOf(SecurityTools.getDefaultReadAclLevel()), true, false, false, true);

                //initialize the update ACL (no values are overwritten, only initialized)
                Element aclUpdateProp = this.findOrCreateElement(Meta.aclUpdate, String.valueOf(SecurityTools.getDefaultUpdateAclLevel()), true, false, false, true);

                //initialize the delete ACL (no values are overwritten, only initialized)
                Element aclDeleteProp = this.findOrCreateElement(Meta.aclDelete, String.valueOf(SecurityTools.getDefaultDeleteAclLevel()), true, false, false, true);

                //initialize the manage ACL (no values are overwritten, only initialized)
                Element aclManageProp = this.findOrCreateElement(Meta.aclManage, String.valueOf(SecurityTools.getDefaultManageAclLevel()), true, false, false, true);
            }
            else {
                //if the ACL system is disabled, we need to make sure to delete incoming ACL entries (especially the empty ones)
                //or the indexer will probably crash (because it doesn't know how to index empty integer property values)
                this.deletePropertyElements(Meta.aclRead);
                this.deletePropertyElements(Meta.aclUpdate);
                this.deletePropertyElements(Meta.aclDelete);
                this.deletePropertyElements(Meta.aclManage);
            }

            // This is subtle: when the sameAs property element is left alone, it will get serialized to RDF as a blank value since it's dataType is anyURI.
            // But this clouds further processing because if we're not careful, the "blank URI" will expand to eg. the base URI when converting relative to absolute.
            // It's better to delete those blank sameAs tags here, while we're processing the meta tags anyway
            Elements sameAsEls = this.getPropertyElements(Meta.sameAs);
            for (Element sameAsEl : sameAsEls) {
                //we delete the blanks while we iterate
                if (StringUtils.isBlank(sameAsEl.attr(HtmlParser.RDF_CONTENT_ATTR))) {
                    //note: this doesn't change the list
                    sameAsEl.remove();
                }
            }
        }
        catch (Exception e) {
            throw new IOException("Error while updating the html with the new metadata values; " + this.getUri(), e);
        }
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
            throw new IOException("The supplied HTML must contain a <html> tag; " + this.getUri());
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

        Elements headTags = this.document.getElementsByTag("head");
        if (headTags.isEmpty()) {
            throw new IOException("The supplied HTML must contain a <head> tag; " + this.getUri());
        }
        else {
            this.headTag = headTags.first();
        }

        this.metaTags = this.document.getElementsByTag("meta");
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
    private void deletePropertyElements(RdfProperty property)
    {
        this.getPropertyElements(property).remove();
    }
    private Element findOrCreateElement(RdfProperty property, String value, boolean deleteOthers, boolean overwrite, boolean onlyUpdateIfBlank, boolean deleteIfBlank) throws IOException
    {
        Element retVal = null;

        Elements existingPropEls = this.getPropertyElements(property);

        if (existingPropEls.isEmpty()) {
            this.headTag.prependChild(retVal = this.document.createElement(PageSource.HTML_META_ELEMENT));
        }
        else {
            //this basically means 'append'
            if (!deleteOthers && !overwrite) {

                //we start out by adding the retval to the DOM, so it's grouped together with the others
                //note: this doesn't add the retVal to the existingPropEls
                existingPropEls.last().after(retVal = this.document.createElement(PageSource.HTML_META_ELEMENT));

                //first, check if we are already in the existing list
                boolean alreadyPresent = false;
                for (Element contributorEl : existingPropEls) {
                    //note: this will be null when no such attribute exists
                    String content = contributorEl.attr(HtmlParser.RDF_CONTENT_ATTR);
                    //we delete the blanks while we iterate
                    if (StringUtils.isBlank(content)) {
                        //note: this doesn't change the list
                        contributorEl.remove();
                    }
                    else if (content.trim().equals(value) && contributorEl != retVal) {
                        alreadyPresent = true;
                    }
                }

                //remove the new element again if it's value was already present in the list
                if (alreadyPresent) {
                    retVal.remove();
                    retVal = null;
                }
            }
            else {
                Element existingEl = existingPropEls.first();

                if (deleteOthers && existingPropEls.size() > 1) {
                    for (int i = 0; i < existingPropEls.size(); i++) {
                        Logger.warn("Encountered html with more than one " + property + " meta property, deleting others; " + this.getUri());
                        existingPropEls.get(i).remove();
                    }
                }

                //note that we need to use placeholders in the templates as anchors for our properties,
                // so we should always overwrite an empty-valued element since it's the same as no element
                if (overwrite || StringUtils.isBlank(existingEl.attr(HtmlParser.RDF_CONTENT_ATTR))) {
                    retVal = existingEl;
                }
            }
        }

        if (retVal != null) {
            //note that this can "update" the property name to use the curie variant if it was in the default ontology scope
            //I hope that's okay...
            retVal.attr(HtmlParser.RDF_PROPERTY_ATTR, property.getCurie().toString())
                  .attr(HtmlParser.RDF_DATATYPE_ATTR, property.getDataType().getCurieName().toString());

            String existingValue = retVal.attr(HtmlParser.RDF_CONTENT_ATTR);
            if (onlyUpdateIfBlank || deleteIfBlank) {
                if (onlyUpdateIfBlank && deleteIfBlank) {
                    throw new IOException("Use one of both params onlyUpdateIfBlank or deleteIfBlank, not both");
                }
                else {
                    if (StringUtils.isBlank(existingValue)) {
                        if (onlyUpdateIfBlank) {
                            retVal.attr(HtmlParser.RDF_CONTENT_ATTR, value);
                        }
                        else {
                            retVal.remove();
                        }
                    }
                }
            }
            else {
                retVal.attr(HtmlParser.RDF_CONTENT_ATTR, value);
            }
        }

        return retVal;
    }
    private Elements getPropertyElements(RdfProperty property)
    {
        String cssQuery = "[" + HtmlParser.RDF_PROPERTY_ATTR + "=" + property.getCurieName() + "]";
        // if the property is inside the main ontology, we need to search for the non-prefixed name as well
        if (property.getOntology().equals(Settings.instance().getRdfMainOntologyNamespace())) {
            cssQuery += ", [" + HtmlParser.RDF_PROPERTY_ATTR + "=" + property.getName() + "]";
        }

        return this.document.select(cssQuery);
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
