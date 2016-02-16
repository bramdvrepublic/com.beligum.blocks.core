package com.beligum.blocks.templating.blocks;

import com.beligum.base.utils.Logger;
import com.beligum.blocks.config.Settings;
import com.beligum.blocks.rdf.sources.HtmlSource;
import net.htmlparser.jericho.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.ext.com.google.common.collect.ImmutableSet;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.*;

/**
 * Convert incoming html to a normalized form, based on the current page and tag templates.
 * TODO would be nice if unmodified properties and/or template-instances would be reverted to their 'collapsed' form
 * <p/>
 * Created by bram on 1/23/16.
 */
public class HtmlAnalyzer
{
    //-----CONSTANTS-----
    private static final Set ALWAYS_INCLUDE_TAGS = new HashSet(Arrays.asList(new String[] {}));
    private static final Set<String> REFERENCE_ATTRS = ImmutableSet.of("src", "href", "content");

    private static Set<String> SITE_DOMAINS = new HashSet<>();
    static {
        SITE_DOMAINS.add(Settings.instance().getSiteDomain().getAuthority());
        for (URI alias : Settings.instance().getSiteAliases()) {
            if (alias!=null && !StringUtils.isEmpty(alias.getAuthority())) {
                SITE_DOMAINS.add(alias.getAuthority());
            }
        }
    }

    //-----VARIABLES-----
    private final TemplateCache allTagTemplates;
    private Source htmlDocument;
    private String normalizedHtml;
    private Locale htmlLocale;
    private ParentRef parent;
    private Map<URI, TranslationRef> translations;
    private Map<URI, ReferenceRef> internalRefs;
    private Map<URI, ReferenceRef> externalRefs;
    private String title;

    //-----CONSTRUCTORS-----
    public HtmlAnalyzer()
    {
        this.allTagTemplates = HtmlParser.getTemplateCache();

        this.translations = new HashMap<>();
        this.internalRefs = new HashMap<>();
        this.externalRefs = new HashMap<>();
        this.title = null;
    }

    //-----PUBLIC METHODS-----
    /**
     * Parses the incoming html string and stores all relevant structures in class variables,
     * to be retrieved later on by the getters below.
     *
     * @param htmlSource
     * @param prettyPrint
     * @throws IOException
     */
    public void analyze(HtmlSource htmlSource, boolean prettyPrint) throws IOException
    {
        try (InputStream is = htmlSource.openNewInputStream()) {
            this.htmlDocument = new Source(is);
        }

        HtmlTemplate currentTemplate = null;
        Element htmlElement = this.htmlDocument.getFirstElement(HtmlParser.HTML_ROOT_ELEM);
        String pageTemplateName = htmlElement.getAttributeValue(HtmlParser.HTML_ROOT_TEMPLATE_ATTR);
        if (StringUtils.isEmpty(pageTemplateName)) {
            throw new IOException("Encountered an attempt to save html without a page template; this shouldn't happen; " + htmlElement);
        }
        else {
            currentTemplate = this.allTagTemplates.getByTagName(pageTemplateName);
            if (currentTemplate == null) {
                throw new IOException("Encountered an attempt to save html, but I can't find a page template with name " + pageTemplateName);
            }
        }

        //extract and store the locale
        String langAttr = htmlElement.getAttributeValue(HtmlSource.HTML_ROOT_LANG_ATTR);
        if (!StringUtils.isEmpty(langAttr)) {
            this.htmlLocale = Locale.forLanguageTag(langAttr);
        }
        else {
            //this is a nice practice and allows us to skip a lot of null tests (reason why ROOT was added in the first place)
            this.htmlLocale = Locale.ROOT;
        }

        StringBuilder outputHtml = new StringBuilder();
        outputHtml.append(this.instantiateTemplateStartTag(htmlElement, currentTemplate, new HashSet(Arrays.asList(new String[] { HtmlParser.HTML_ROOT_TEMPLATE_ATTR }))));
        int depth = 0;
        Stack<Element> templateStack = new Stack<>();
        Stack<Element> propertyStack = new Stack<>();
        //Note: this is the only way I found to iterate over *all* nodes (including the literal and text nodes)
        for (Iterator<Segment> nodeIterator = htmlElement.getContent().getNodeIterator(); nodeIterator.hasNext(); ) {
            depth = this.processNode(nodeIterator.next(), depth, templateStack, propertyStack, outputHtml);
        }
        outputHtml.append(this.instantiateTemplateEndTag(currentTemplate));

        this.normalizedHtml = outputHtml.toString();
        if (prettyPrint) {
            SourceFormatter formatter = new SourceFormatter(new Source(this.normalizedHtml));
            formatter.setCollapseWhiteSpace(true);
            formatter.setIndentString("    ");
            formatter.setNewLine("\n");
            this.normalizedHtml = formatter.toString();
        }
    }

    public String getNormalizedHtml()
    {
        return normalizedHtml;
    }
    public Locale getHtmlLanguage()
    {
        return htmlLocale;
    }
    public Map<URI, ReferenceRef> getInternalRefs()
    {
        return internalRefs;
    }
    public Map<URI, ReferenceRef> getExternalRefs()
    {
        return externalRefs;
    }
    public ParentRef getParent()
    {
        return parent;
    }
    public Map<URI, TranslationRef> getTranslations()
    {
        return translations;
    }
    public String getTitle()
    {
        return title;
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----
    /**
     * Analyzes a Jericho node
     */
    private int processNode(Segment node, int depth, Stack<Element> templateStack, Stack<Element> propertyStack, StringBuilder outputDocument)
    {
        if (node != null) {

            Element currentTemplate = templateStack.isEmpty() ? null : templateStack.peek();
            Element currentProperty = propertyStack.isEmpty() ? null : propertyStack.peek();

            boolean writeTag = false;

            //some general rules when to always include the node
            //some exception tags
            if (node instanceof Tag && this.isAlwaysIncludeTag((Tag) node)) {
                writeTag = true;
            }
            //if we're inside a valid property, save everything
            else if (currentProperty != null) {
                writeTag = true;
            }

            if (node instanceof StartTag) {
                StartTag startTag = (StartTag) node;

                boolean isProperty = this.isProperty(startTag);
                boolean isTemplateTag = this.isTemplate(startTag);

                //*also* write the tag out in these cases
                if (/*isTemplateTag || */isProperty) {
                    writeTag = true;
                }

                this.extractParent(startTag);
                this.extractTranslation(startTag);
                this.extractTitle(startTag);
                this.extractReference(startTag, writeTag);

                //this means we won't encounter an end tag for this start tag
                if (this.isStandAlone(startTag)) {
                    //No changes here
                }
                else {
                    if (isProperty) {
                        propertyStack.push(startTag.getElement());
                    }

                    if (isTemplateTag) {
                        //the only way to go from one template into another is to cross a template tag
                        templateStack.push(startTag.getElement());

                        //this will 'reset' the property stack on template bounds, so the above peek in the stack will return null
                        if (!isProperty) {
                            propertyStack.push(null);
                        }
                    }
                }

                depth++;
            }
            else if (node instanceof EndTag) {
                EndTag endTag = (EndTag) node;

                boolean popPropertyStack = currentProperty != null && currentProperty.getEndTag() != null && currentProperty.getEndTag().equals(endTag);
                boolean popTemplateStack = currentTemplate != null && currentTemplate.getEndTag() != null && currentTemplate.getEndTag().equals(endTag);

                if (popTemplateStack) {
                    templateStack.pop();

                    //in case of a template, we always need to pop the property stack too, because of the template bounds reset (see above)
                    propertyStack.pop();

                    // we don't always write the template, but if the previous (after above pop) property context (before resetting it and entering the template) was not empty,
                    // it needs to be written out
                    if (!propertyStack.isEmpty() && propertyStack.peek() != null) {
                        writeTag = true;
                    }
                }
                //if we're not ending a template (where we always pop the property stack as well), check if we need to pop any properties
                else {
                    if (popPropertyStack) {
                        propertyStack.pop();
                        writeTag = true;
                    }
                }

                depth--;
            }

            if (writeTag) {
                outputDocument.append(node.toString());
            }
        }
        else {
            Logger.warn("Enountered null node during parsing, this shouldn't happen");
        }

        return depth;
    }
    /**
     * Extract and save the internal (internal pages to this site) and external (http/https/ftp/...) references
     * in this tag.
     * @param isNormalizedTag indicates if this reference (actually the startTag) will end up in the normalized version
     */
    private void extractReference(StartTag startTag, boolean isNormalizedTag)
    {
        Attributes startTagAttrs = startTag.getAttributes();

        if (startTagAttrs!=null) {
            for (Attribute attr : startTagAttrs) {
                if (REFERENCE_ATTRS.contains(attr.getName())) {
                    if (!StringUtils.isEmpty(attr.getValue())) {
                        try {
                            //validate the reference
                            URI uri = URI.create(attr.getValue());
                            //if the url is relative to this domain or is abolute and inside this domain, store as internal ref
                            //note that we need to include the port in the check (authority instead of host)
                            //TODO: note that, for now, this will also contain garbage URI's that passed the create() test above like "IE=edge"
                            if (StringUtils.isEmpty(uri.getAuthority()) || SITE_DOMAINS.contains(uri.getAuthority())) {
                                this.internalRefs.put(uri, new ReferenceRef(attr, isNormalizedTag));
                            }
                            //otherwise it's an external ref
                            else {
                                this.externalRefs.put(uri, new ReferenceRef(attr, isNormalizedTag));
                            }
                        }
                        catch (IllegalArgumentException e) {
                            Logger.debug("Encountered illegal URI as an attribute value of " + attr + " in " + startTag, e);
                        }
                    }
                }
            }
        }
    }
    /**
     * Extract and save the translation (if this start tag is a <link rel="up"> tag)
     * Example: <link rel="up" href="/fr/other/page/index.html">
     * According to Mozilla, the "up" keyword "Indicates that the page is part of a hierarchical
     * structure and that the hyperlink leads to the higher level resource of that structure."
     * For details, see https://developer.mozilla.org/en-US/docs/Web/HTML/Link_types
     */
    private void extractParent(StartTag startTag)
    {
        if (startTag.getName().equalsIgnoreCase(HtmlSource.HTML_PARENT_ELEMENT)) {
            String relAttr = startTag.getAttributeValue(HtmlSource.HTML_PARENT_ATTR_REL);
            if (!StringUtils.isEmpty(relAttr) && relAttr.equalsIgnoreCase(HtmlSource.HTML_PARENT_ATTR_REL_VALUE)) {
                String hrefAttr = startTag.getAttributeValue(HtmlSource.HTML_PARENT_ATTR_HREF);
                if (!StringUtils.isEmpty(hrefAttr)) {
                    try {
                        //validate the reference
                        URI uri = URI.create(hrefAttr);
                        this.parent = new ParentRef(startTag, uri);
                    }
                    catch (IllegalArgumentException e) {
                        Logger.debug("Encountered illegal parent URI as an attribute value of 'href' in " + startTag, e);
                    }
                }
            }
        }
    }
    /**
     * Extract and save the translation (if this start tag is a <link rel="alternate"> tag)
     * Example: <link rel="alternate" type="text/html" hreflang="fr" href="/fr/other/page/index.html">
     * The "alternate" keyword creates a hyperlink referencing an alternate representation of the current document.
     * For details, see https://www.w3.org/TR/html5/links.html
     */
    private void extractTranslation(StartTag startTag)
    {
        if (startTag.getName().equalsIgnoreCase(HtmlSource.HTML_TRANSLATION_ELEMENT)) {
            String relAttr = startTag.getAttributeValue(HtmlSource.HTML_TRANSLATION_ATTR_REL);
            if (!StringUtils.isEmpty(relAttr) && relAttr.equalsIgnoreCase(HtmlSource.HTML_TRANSLATION_ATTR_REL_VALUE)) {
                String typeAttr = startTag.getAttributeValue(HtmlSource.HTML_TRANSLATION_ATTR_TYPE);
                if (!StringUtils.isEmpty(typeAttr) && typeAttr.equalsIgnoreCase(com.beligum.base.resources.ifaces.Resource.MimeType.HTML.getMimeType().toString())) {
                    String hrefLangAttr = startTag.getAttributeValue(HtmlSource.HTML_TRANSLATION_ATTR_HREFLANG);
                    String hrefAttr = startTag.getAttributeValue(HtmlSource.HTML_TRANSLATION_ATTR_HREF);
                    if (!StringUtils.isEmpty(hrefLangAttr) && !StringUtils.isEmpty(hrefAttr)) {
                        try {
                            //validate the reference
                            URI uri = URI.create(hrefAttr);

                            this.translations.put(uri, new TranslationRef(startTag, Locale.forLanguageTag(hrefLangAttr)));
                        }
                        catch (IllegalArgumentException e) {
                            Logger.debug("Encountered illegal translation URI as an attribute value of 'href' in " + startTag, e);
                        }
                    }
                }
            }
        }
    }
    /**
     * Check if the tag is a title tag and extract and save it's value if it is.
     */
    private void extractTitle(StartTag startTag)
    {
         if (startTag.getName().equalsIgnoreCase(HtmlSource.HTML_TITLE_ELEMENT)) {
             this.title = startTag.getElement().getContent().toString();
             if (!StringUtils.isEmpty(this.title)) {
                 this.title = this.title.trim();
             }
         }
    }
    /**
     * @return true if this tag is special and should always be included in the normalized form
     */
    private boolean isAlwaysIncludeTag(Tag tag)
    {
        return tag.getElement() != null && this.ALWAYS_INCLUDE_TAGS.contains(tag.getElement().getName());
    }
    private boolean isStandAlone(StartTag tag)
    {
        return tag.isEmptyElementTag() || tag.getElement().getEndTag() == null;
    }
    private boolean isProperty(StartTag tag)
    {
        return tag.getAttributeValue(HtmlParser.RDF_PROPERTY_ATTR) != null || tag.getAttributeValue(HtmlParser.NON_RDF_PROPERTY_ATTR) != null;
    }
    private boolean isTemplate(StartTag tag)
    {
        return this.allTagTemplates.containsKeyByTagName(tag.getName());
    }
    private String instantiateTemplateStartTag(Element element, HtmlTemplate template, Set<String> excludeAttributes)
    {
        Map<String, String> attributes = new LinkedHashMap<>();
        element.getAttributes().populateMap(attributes, false);

        //copy in all the attributes of the template to the attributes map, except the ones that were already set in the instance
        Map<String, String> templateAttributes = template.getAttributes();
        if (templateAttributes != null) {
            for (Map.Entry<String, String> attribute : templateAttributes.entrySet()) {
                if (!attributes.containsKey(attribute.getKey())) {
                    attributes.put(attribute.getKey(), attribute.getValue());
                }
            }
        }

        if (excludeAttributes != null) {
            for (String attr : excludeAttributes) {
                attributes.remove(attr);
            }
        }

        StringBuilder builder = new StringBuilder();
        builder.append("<" + template.getTemplateName());
        if (!attributes.isEmpty()) {
            builder.append(Attributes.generateHTML(attributes));
        }
        //close the start tag
        builder.append(">");

        return builder.toString();
    }
    private String instantiateTemplateEndTag(HtmlTemplate template)
    {
        return new StringBuilder().append("</").append(template.getTemplateName()).append(">").toString();
    }

    //-----INNER CLASSES-----
    public class ParentRef
    {
        public final StartTag tag;
        public final URI parentUri;

        protected ParentRef(StartTag tag, URI parentUri)
        {
            this.tag = tag;
            this.parentUri = parentUri;
        }
    }
    public class TranslationRef
    {
        public final StartTag tag;
        public final Locale locale;

        protected TranslationRef(StartTag tag, Locale locale)
        {
            this.tag = tag;
            this.locale = locale;
        }
    }
    public class ReferenceRef
    {
        public final Attribute attribute;
        public final boolean isNormalizedTag;

        protected ReferenceRef(Attribute attribute, boolean isNormalizedTag)
        {
            this.attribute = attribute;
            this.isNormalizedTag = isNormalizedTag;
        }
    }
}
