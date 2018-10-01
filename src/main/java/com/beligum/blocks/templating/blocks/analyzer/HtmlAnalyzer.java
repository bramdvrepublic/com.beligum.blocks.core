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

package com.beligum.blocks.templating.blocks.analyzer;

import com.beligum.base.server.R;
import com.beligum.blocks.filesystem.pages.ifaces.Page;
import com.beligum.blocks.rdf.sources.PageSource;
import com.beligum.blocks.templating.blocks.HtmlParser;
import com.beligum.blocks.templating.blocks.HtmlTemplate;
import com.beligum.blocks.templating.blocks.PageTemplate;
import com.beligum.blocks.templating.blocks.TemplateCache;
import net.htmlparser.jericho.*;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Convert incoming html to a normalized form, based on the current page and tag templates.
 * <p/>
 * Created by bram on 1/23/16.
 */
public class HtmlAnalyzer
{
    //-----CONSTANTS-----
    private static Set<String> SITE_DOMAINS = new HashSet<>();

    static {
        SITE_DOMAINS.add(R.configuration().getSiteDomain().getAuthority());
        for (URI alias : R.configuration().getSiteAliases()) {
            if (alias != null && !StringUtils.isEmpty(alias.getAuthority())) {
                SITE_DOMAINS.add(alias.getAuthority());
            }
        }
    }

    //-----VARIABLES-----
    private com.beligum.base.resources.ifaces.Source pageSource;
    private Source htmlDocument;
    private Element rootElement;
    private String normalizedHtml;
    private AttributeRef htmlAbout;
    private AttributeRef htmlTypeof;
    private AttributeRef htmlVocab;
    private AttributeRef htmlPrefixes;
    private Locale htmlLocale;
    private String title;
    private Set<HtmlTag> metaTags;
    private Set<URI> subResources;
    private boolean analyzedShallow;
    private boolean analyzedDeep;

    //-----CONSTRUCTORS-----
    /**
     * This constructor will only hunt for the root element and save a reference.
     * The real work is loaded lazily and is split up phases: deep and shallow.
     * Shallow analysis only parses the attributes on the root (html or template tag) element,
     * while deep analysis parses the entire html body (and is much more computing intensive)
     */
    public HtmlAnalyzer(com.beligum.base.resources.ifaces.Source pageSource) throws IOException
    {
        this.pageSource = pageSource;

        try (InputStream is = pageSource.newInputStream()) {
            this.htmlDocument = HtmlTemplate.readHtmlInputStream(is);
        }

        this.init();
    }
    public HtmlAnalyzer(Page page, boolean readOriginal) throws IOException
    {
        this.pageSource = page;

        if (readOriginal) {
            try (InputStream is = page.getFileContext().open(page.getLocalStoragePath())) {
                this.htmlDocument = HtmlTemplate.readHtmlInputStream(is);
            }
        }
        else {
            try (InputStream is = page.newInputStream()) {
                this.htmlDocument = HtmlTemplate.readHtmlInputStream(is);
            }
        }

        this.init();
    }

    //-----PUBLIC METHODS-----
    public AttributeRef getHtmlAbout() throws IOException
    {
        this.assertAnalyzedShallow();

        return htmlAbout;
    }
    public AttributeRef getHtmlTypeof() throws IOException
    {
        this.assertAnalyzedShallow();

        return htmlTypeof;
    }
    public AttributeRef getHtmlVocab() throws IOException
    {
        this.assertAnalyzedShallow();

        return htmlVocab;
    }
    public AttributeRef getHtmlPrefixes() throws IOException
    {
        this.assertAnalyzedShallow();

        return htmlPrefixes;
    }
    public Locale getHtmlLanguage() throws IOException
    {
        this.assertAnalyzedShallow();

        return htmlLocale;
    }
    public String getNormalizedHtml() throws IOException
    {
        this.assertAnalyzedDeep();

        return normalizedHtml;
    }
    public String getTitle() throws IOException
    {
        this.assertAnalyzedDeep();

        return title;
    }
    public Set<HtmlTag> getMetaTags() throws IOException
    {
        this.assertAnalyzedDeep();

        return metaTags;
    }
    public Set<URI> getSubResources() throws IOException
    {
        this.assertAnalyzedDeep();

        return subResources;
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----
    private void init() throws IOException
    {
        this.rootElement = this.htmlDocument.getFirstElement(HtmlParser.HTML_ROOT_ELEM);
        if (this.rootElement == null) {
            //this will make sure we also support normalized pages (which is needed because the default inputstream of a page is the normalized source)
            Element firstElement = this.htmlDocument.getFirstElement();
            HtmlTemplate pageTemplate = TemplateCache.instance().getByTagName(firstElement.getName());
            if (pageTemplate!=null && pageTemplate instanceof PageTemplate) {
                this.rootElement = firstElement;
            }

            if (this.rootElement == null) {
                throw new IOException("Encountered an attempt to save html without a <" + HtmlParser.HTML_ROOT_ELEM + "> template; this shouldn't happen; " + pageSource);
            }
        }

        this.analyzedShallow = false;
        this.analyzedDeep = false;
    }
    private void assertAnalyzedShallow() throws IOException
    {
        if (!this.analyzedShallow) {
            this.analyzeShallow();
        }
    }
    private void assertAnalyzedDeep() throws IOException
    {
        if (!this.analyzedDeep) {
            this.analyzeDeep();
        }
    }
    /**
     * Parses the incoming html string and stores all relevant structures in class variables,
     * to be retrieved later on by the getters below.
     */
    private void analyzeShallow() throws IOException
    {
        //extract the base resource id
        Attributes htmlAttributes = rootElement.getAttributes();
        String tempAttrValue;
        if (htmlAttributes.get(PageSource.HTML_ROOT_SUBJECT_ATTR) != null && !StringUtils.isEmpty(tempAttrValue = htmlAttributes.getValue(PageSource.HTML_ROOT_SUBJECT_ATTR))) {
            //note that the html tag is always part of the normalized html
            this.htmlAbout = new AttributeRef(tempAttrValue, htmlAttributes.get(PageSource.HTML_ROOT_SUBJECT_ATTR), true);
        }
        else {
            //makes sense to allow null resources; it allows to use this analyzer more generally
            this.htmlAbout = null;
        }

        //extract the base typeof
        if (htmlAttributes.get(PageSource.HTML_ROOT_TYPEOF_ATTR) != null && !StringUtils.isEmpty(tempAttrValue = htmlAttributes.getValue(PageSource.HTML_ROOT_TYPEOF_ATTR))) {
            //note that the html tag is always part of the normalized html
            this.htmlTypeof = new AttributeRef(tempAttrValue, htmlAttributes.get(PageSource.HTML_ROOT_TYPEOF_ATTR), true);
        }
        else {
            this.htmlTypeof = null;
        }

        //extract the base vocab
        if (htmlAttributes.get(PageSource.HTML_ROOT_VOCAB_ATTR) != null && !StringUtils.isEmpty(tempAttrValue = htmlAttributes.getValue(PageSource.HTML_ROOT_VOCAB_ATTR))) {
            //note that the html tag is always part of the normalized html
            this.htmlVocab = new AttributeRef(tempAttrValue, htmlAttributes.get(PageSource.HTML_ROOT_VOCAB_ATTR), true);
        }
        else {
            this.htmlVocab = null;
        }

        //extract the base prefix
        if (htmlAttributes.get(PageSource.HTML_ROOT_PREFIX_ATTR) != null && !StringUtils.isEmpty(tempAttrValue = htmlAttributes.getValue(PageSource.HTML_ROOT_PREFIX_ATTR))) {
            //note that the html tag is always part of the normalized html
            this.htmlPrefixes = new AttributeRef(tempAttrValue, htmlAttributes.get(PageSource.HTML_ROOT_PREFIX_ATTR), true);
        }
        else {
            this.htmlPrefixes = null;
        }

        //extract and store the locale
        if (htmlAttributes.get(PageSource.HTML_ROOT_LANG_ATTR) != null && !StringUtils.isEmpty(tempAttrValue = htmlAttributes.getValue(PageSource.HTML_ROOT_LANG_ATTR))) {
            this.htmlLocale = Locale.forLanguageTag(tempAttrValue);
        }
        else {
            //this is a nice practice and allows us to skip a lot of null tests (reason why ROOT was added in the first place)
            this.htmlLocale = Locale.ROOT;
        }

        this.analyzedShallow = true;
    }
    /**
     * Parses the incoming html string and stores all relevant structures in class variables,
     * to be retrieved later on by the getters below.
     */
    private void analyzeDeep() throws IOException
    {
        //Note: the normalizer not only normalizes the html, but it also saves certain references along the way
        HtmlNormalizer normalizer = new HtmlNormalizer(pageSource);
        CharSequence unformattedNormalizedHtml = normalizer.process(rootElement);

        //we store the normalized html pretty printed
        SourceFormatter formatter = new SourceFormatter(new Source(unformattedNormalizedHtml));
        formatter.setCollapseWhiteSpace(true);
        formatter.setIndentString("    ");
        formatter.setNewLine("\n");
        this.normalizedHtml = formatter.toString();

        //save some additional variables that were detected during normalizing
        this.title = normalizer.getTitle();
        this.metaTags = normalizer.getMetaTags();
        this.subResources = normalizer.getSubResources();

        this.analyzedDeep = true;
    }

    //-----INNER CLASSES-----
    public class AttributeRef
    {
        public final String value;
        public final Attribute attribute;
        public final boolean isNormalizedTag;

        protected AttributeRef(String value, Attribute attribute, boolean isNormalizedTag)
        {
            this.value = value;
            this.attribute = attribute;
            this.isNormalizedTag = isNormalizedTag;
        }
    }
}
