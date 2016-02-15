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
    private Map<URI, StartTag> translations;
    private Map<URI, Attribute> internalRefs;
    private Map<URI, Attribute> externalRefs;

    //-----CONSTRUCTORS-----
    public HtmlAnalyzer()
    {
        this.allTagTemplates = HtmlParser.getTemplateCache();

        this.translations = new HashMap<>();
        this.internalRefs = new HashMap<>();
        this.externalRefs = new HashMap<>();
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
        this.htmlLocale = Locale.forLanguageTag(htmlElement.getAttributeValue(HtmlSource.HTML_ROOT_LANG_ATTR));

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
    public Locale getHtmlLocale()
    {
        return htmlLocale;
    }
    public Map<URI, Attribute> getInternalRefs()
    {
        return internalRefs;
    }
    public Map<URI, Attribute> getExternalRefs()
    {
        return externalRefs;
    }
    public Map<URI, StartTag> getTranslations()
    {
        return translations;
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

                this.extractTranslation(startTag);

                // check if we need to pull out this tag as a reference
                // note it doesn't make sense to store the references of tags that are not included in the normalized html,
                // because they're not really 'part' of this document.
                if (writeTag) {
                    this.extractReference(startTag);
                }

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
     */
    private void extractReference(StartTag startTag)
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
                                this.internalRefs.put(uri, attr);
                            }
                            //otherwise it's an external ref
                            else {
                                this.externalRefs.put(uri, attr);
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
     * Extract and save the translation (if this start tag is a <link rel="alternate"> tag)
     * Example: <link rel="alternate" type="text/html" hreflang="fr" href="/fr/other/page/index.html">
     * The "alternate" keyword creates a hyperlink referencing an alternate representation of the current document.
     * For details, see https://www.w3.org/TR/html5/links.html
     */
    private void extractTranslation(StartTag startTag)
    {
        if (startTag.getName().equalsIgnoreCase("link")) {
            String relAttr = startTag.getAttributeValue("rel");
            if (!StringUtils.isEmpty(relAttr) && relAttr.equalsIgnoreCase("alternate")) {
                String typeAttr = startTag.getAttributeValue("type");
                if (!StringUtils.isEmpty(typeAttr) && typeAttr.equalsIgnoreCase(com.beligum.base.resources.ifaces.Resource.MimeType.HTML.getMimeType().toString())) {
                    String hrefLangAttr = startTag.getAttributeValue("hreflang");
                    String hrefAttr = startTag.getAttributeValue("href");
                    if (!StringUtils.isEmpty(hrefLangAttr) && !StringUtils.isEmpty(hrefAttr)) {
                        try {
                            //validate the reference
                            URI uri = URI.create(hrefAttr);

                            this.translations.put(uri, startTag);
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
}
