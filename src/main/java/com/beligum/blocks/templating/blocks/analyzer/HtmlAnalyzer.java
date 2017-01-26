package com.beligum.blocks.templating.blocks.analyzer;

import com.beligum.base.server.R;
import com.beligum.blocks.rdf.sources.PageSource;
import com.beligum.blocks.templating.blocks.HtmlParser;
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
 * TODO would be nice if unmodified properties and/or template-instances would be reverted to their 'collapsed' form
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
    private Source htmlDocument;
    private String normalizedHtml;
    private AttributeRef htmlAbout;
    private AttributeRef htmlTypeof;
    private AttributeRef htmlVocab;
    private AttributeRef htmlPrefixes;
    private Locale htmlLocale;
    private String title;

    //-----CONSTRUCTORS-----
    public HtmlAnalyzer(com.beligum.base.resources.ifaces.Source pageSource) throws IOException
    {
        this.title = null;

        this.analyze(pageSource);
    }

    //-----PUBLIC METHODS-----
    public String getNormalizedHtml()
    {
        return normalizedHtml;
    }
    public AttributeRef getHtmlAbout()
    {
        return htmlAbout;
    }
    public AttributeRef getHtmlTypeof()
    {
        return htmlTypeof;
    }
    public AttributeRef getHtmlVocab()
    {
        return htmlVocab;
    }
    public AttributeRef getHtmlPrefixes()
    {
        return htmlPrefixes;
    }
    public Locale getHtmlLanguage()
    {
        return htmlLocale;
    }
    public String getTitle()
    {
        return title;
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----
    /**
     * Parses the incoming html string and stores all relevant structures in class variables,
     * to be retrieved later on by the getters below.
     */
    private void analyze(com.beligum.base.resources.ifaces.Source pageSource) throws IOException
    {
        try (InputStream is = pageSource.newInputStream()) {
            this.htmlDocument = new Source(is);
        }

        Element rootElement = this.htmlDocument.getFirstElement(HtmlParser.HTML_ROOT_ELEM);
        if (rootElement == null) {
            throw new IOException("Encountered an attempt to save html without a <" + HtmlParser.HTML_ROOT_ELEM + "> template; this shouldn't happen; " + pageSource);
        }

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

        HtmlNormalizer normalizer = new HtmlNormalizer();
        CharSequence unformattedNormalizedHtml = normalizer.process(rootElement);

        //we store the normalized html pretty printed
        SourceFormatter formatter = new SourceFormatter(new Source(unformattedNormalizedHtml));
        formatter.setCollapseWhiteSpace(true);
        formatter.setIndentString("    ");
        formatter.setNewLine("\n");
        this.normalizedHtml = formatter.toString();

        //save some additional variables that were detected during normalizing
        this.title = normalizer.getTitle();
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
