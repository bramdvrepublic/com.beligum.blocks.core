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
    private AttributeRef htmlResource;
    private AttributeRef htmlTypeof;
    private Locale htmlLocale;
    private Map<URI, ReferenceRef> internalRefs;
    private Map<URI, ReferenceRef> externalRefs;
    private String title;

    //-----CONSTRUCTORS-----
    public HtmlAnalyzer(HtmlSource htmlSource, boolean prettyPrint) throws IOException
    {
        this.allTagTemplates = HtmlParser.getTemplateCache();

        this.internalRefs = new HashMap<>();
        this.externalRefs = new HashMap<>();
        this.title = null;

        this.analyze(htmlSource, prettyPrint);
    }

    //-----PUBLIC METHODS-----
    public String getNormalizedHtml()
    {
        return normalizedHtml;
    }
    public AttributeRef getHtmlResource()
    {
        return htmlResource;
    }
    public AttributeRef getHtmlTypeof()
    {
        return htmlTypeof;
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
    public String getTitle()
    {
        return title;
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----
    /**
     * Parses the incoming html string and stores all relevant structures in class variables,
     * to be retrieved later on by the getters below.
     *
     * @param htmlSource
     * @param prettyPrint
     * @throws IOException
     */
    private void analyze(HtmlSource htmlSource, boolean prettyPrint) throws IOException
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

        //extract the base resource id
        Attributes htmlAttributes = htmlElement.getAttributes();
        String tempAttrValue;
        if (htmlAttributes.get(HtmlSource.HTML_ROOT_RESOURCE_ATTR)!=null && !StringUtils.isEmpty(tempAttrValue = htmlAttributes.getValue(HtmlSource.HTML_ROOT_RESOURCE_ATTR))) {
            //note that the html tag is always part of the normalized html
            this.htmlResource = new AttributeRef(tempAttrValue, htmlAttributes.get(HtmlSource.HTML_ROOT_RESOURCE_ATTR), true);
        }
        else {
            //makes sense to allow null resources; it allows to use this analyzer more generally
            this.htmlResource = null;
        }

        //extract the base typeof
        if (htmlAttributes.get(HtmlSource.HTML_ROOT_TYPEOF_ATTR)!=null && !StringUtils.isEmpty(tempAttrValue = htmlAttributes.getValue(HtmlSource.HTML_ROOT_TYPEOF_ATTR))) {
            //note that the html tag is always part of the normalized html
            this.htmlTypeof = new AttributeRef(tempAttrValue, htmlAttributes.get(HtmlSource.HTML_ROOT_TYPEOF_ATTR), true);
        }
        else {
            //this is a nice practice and allows us to skip a lot of null tests (reason why ROOT was added in the first place)
            this.htmlTypeof = null;
        }

        //extract and store the locale
        if (htmlAttributes.get(HtmlSource.HTML_ROOT_LANG_ATTR)!=null && !StringUtils.isEmpty(tempAttrValue = htmlAttributes.getValue(HtmlSource.HTML_ROOT_LANG_ATTR))) {
            this.htmlLocale = Locale.forLanguageTag(tempAttrValue);
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

                this.extractTitle(startTag);
                this.extractReferences(startTag, writeTag);

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
    private void extractReferences(StartTag startTag, boolean isNormalizedTag)
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
