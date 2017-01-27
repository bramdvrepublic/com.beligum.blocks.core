package com.beligum.blocks.templating.blocks.analyzer;

import com.beligum.base.resources.MimeTypes;
import com.beligum.base.resources.sources.StringSource;
import com.beligum.base.server.R;
import com.beligum.base.templating.ifaces.TemplateContext;
import com.beligum.blocks.templating.blocks.*;
import net.htmlparser.jericho.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.*;

/**
 * This is basically a wrapper around a Jericho StartTag that encapsulates all tag-related processing,
 * plus acts as a store for it's own normalized start, content and end tags (separately).
 * <p>
 * Created by bram on 1/26/17.
 */
public class HtmlTag
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    private com.beligum.base.resources.ifaces.Source source;
    private StartTag startTag;
    private CharSequence normalizedStartTag;
    private List<CharSequence> normalizedContent;
    private CharSequence normalizedEndTag;

    private HtmlTemplate cachedTemplate;
    private boolean triedTemplate;
    private List<StartTag> cachedTemplateProperties;
    private Document templateHtml;
    private Map<String, String[]> variableAttributeQueries;
    private Map<String, String[]> variableElementQueries;

    //-----CONSTRUCTORS-----
    public HtmlTag(com.beligum.base.resources.ifaces.Source source, StartTag startTag)
    {
        this.source = source;
        this.startTag = startTag;

        this.normalizedStartTag = "";
        this.normalizedContent = new ArrayList<>();
        this.normalizedEndTag = "";
    }

    //-----PUBLIC METHODS-----
    public String getName()
    {
        return this.startTag.getName();
    }
    public String getContent()
    {
        return this.startTag.getElement().getContent().toString();
    }
    public boolean equalsStartTag(Segment tag)
    {
        return this.startTag.equals(tag);
    }
    public boolean equalsEndTag(Segment tag)
    {
        boolean retVal = false;

        EndTag endTag = this.startTag.getElement().getEndTag();
        if (endTag != null) {
            retVal = endTag.equals(tag);
        }

        return retVal;
    }
    public boolean isProperty()
    {
        return this.isPropertyTag(this.startTag);
    }
    public boolean isTemplate()
    {
        return getTemplate() != null;
    }
    public boolean isPageTemplate()
    {
        HtmlTemplate template = getTemplate();
        return template != null && template instanceof PageTemplate;
    }
    public boolean isTagTemplate()
    {
        HtmlTemplate template = getTemplate();
        return template != null && template instanceof TagTemplate;
    }
    public HtmlTemplate getTemplate()
    {
        if (!this.triedTemplate) {
            //note: getByTagName() can handle null values
            this.cachedTemplate = TemplateCache.instance().getByTagName(getPossibleTemplateName());
            ;
            this.triedTemplate = true;
        }

        return this.cachedTemplate;
    }
    public boolean isStandAlone()
    {
        return this.startTag.isEmptyElementTag() || this.startTag.getElement().getEndTag() == null;
    }
    public String getStartTag()
    {
        String retVal;

        if (this.isTemplate()) {
            retVal = this.buildTemplateStartTag();
        }
        else {
            retVal = this.buildDefaultStartTag();
        }

        return retVal;
    }
    public String getEndTag()
    {
        String retVal = "";

        //standalone elements don't have end tags
        if (!this.isStandAlone()) {
            if (this.isTemplate()) {
                retVal = this.buildTemplateEndTag();
            }
            else {
                retVal = this.buildDefaultEndTag();
            }
        }

        return retVal;
    }
    public void setNormalizedStartTag(CharSequence value, boolean insidePropertyContext) throws IOException
    {
        //first check is to make sure we always write out the root page template tag
        if (this.isPageTemplate() || insidePropertyContext) {
            if (this.normalizedStartTag.length() > 0) {
                throw new IOException("Trying to overwrite a normalized start tag '" + this.normalizedStartTag + "', with a new one '" + value + "'; this is probably not what you want");
            }
            else {
                this.normalizedStartTag = value;
            }
        }
    }
    //Note that we don't take the property context into account here, because we expect the
    //tag to be normalized already (since we use it's toNormalizedString() method)
    public void appendNormalizedSubtag(HtmlTag tag) throws IOException
    {
        if (tag != null) {
            CharSequence value = tag.toNormalizedString();
            if (value.length() > 0) {
                this.normalizedContent.add(value);
            }
        }
    }
    public void appendNormalizedContent(CharSequence value, boolean insidePropertyContext)
    {
        if (insidePropertyContext && value.length() > 0) {
            this.normalizedContent.add(value);
        }
    }
    public void setNormalizedEndTag(CharSequence value, boolean insidePropertyContext) throws IOException
    {
        //first check is to make sure we always write out the root page template tag
        if (this.isPageTemplate() || insidePropertyContext) {
            if (this.normalizedEndTag.length() > 0) {
                throw new IOException("Trying to overwrite a normalized end tag '" + this.normalizedEndTag + "', with a new one '" + value + "'; this is probably not what you want");
            }
            else {
                this.normalizedEndTag = value;
            }
        }
    }
    public CharSequence toNormalizedString() throws IOException
    {
        StringBuilder normalizedHtml = new StringBuilder();

        //first, build the normalized html
        normalizedHtml.append(this.normalizedStartTag);
        normalizedHtml.append(this.buildNormalizedContent());
        normalizedHtml.append(this.normalizedEndTag);

        //replace the strings by variables
        return this.reinsertVariables(normalizedHtml);
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----
    private String getPossibleTemplateName()
    {
        return this.startTag.getName().equals("html") ? this.startTag.getAttributeValue(HtmlParser.HTML_ROOT_TEMPLATE_ATTR) : this.startTag.getName();
    }
    private boolean isPropertyTag(StartTag startTag)
    {
        return startTag.getAttributeValue(HtmlParser.RDF_PROPERTY_ATTR) != null || startTag.getAttributeValue(HtmlParser.NON_RDF_PROPERTY_ATTR) != null;
    }
    private Attribute getPropertyAttribute(StartTag startTag)
    {
        Attribute retVal = null;

        Attributes attributes = startTag.getAttributes();

        //regular property has precedence over data-property
        retVal = attributes.get(HtmlParser.RDF_PROPERTY_ATTR);

        //now try the data-property
        if (retVal == null) {
            retVal = attributes.get(HtmlParser.NON_RDF_PROPERTY_ATTR);
        }

        return retVal;
    }
    private String buildDefaultStartTag()
    {
        return this.startTag.toString();
    }
    private String buildTemplateStartTag()
    {
        Map<String, String> attributes = new LinkedHashMap<>();

        this.startTag.getAttributes().populateMap(attributes, false);

        //we'll save all the incoming attributes, except the ones that are already present in the template,
        //because those will be added during rendering, so we're normalizing them away
        Map<String, String> templateAttributes = this.getTemplate().getAttributes();
        for (Map.Entry<String, String> templateAttr : templateAttributes.entrySet()) {
            String instanceAttrValue = attributes.get(templateAttr.getKey());
            if (instanceAttrValue != null && instanceAttrValue.equals(templateAttr.getValue())) {
                attributes.remove(templateAttr.getKey());
            }
        }

        //if we have empty attributes coming in and they're not in the template,
        // we should wipe them (and hope we don't break any scripts)
        Iterator<Map.Entry<String, String>> iter = attributes.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<String, String> a = iter.next();
            if (a.getValue() != null && a.getValue().trim().isEmpty() && !templateAttributes.containsKey(a.getKey())) {
                iter.remove();
            }
        }

        //when normalizing, this is transferred to the tag name, so wipe it if it's still present
        if (this.isPageTemplate()) {
            attributes.remove(HtmlParser.HTML_ROOT_TEMPLATE_ATTR);
        }

        StringBuilder sb = new StringBuilder();
        sb.append("<" + this.getTemplate().getTemplateName());
        if (!attributes.isEmpty()) {
            sb.append(Attributes.generateHTML(attributes));
        }
        if (this.isStandAlone()) {
            sb.append("/");
        }
        //close the start tag
        sb.append(">");

        return sb.toString();
    }
    private String buildTemplateEndTag()
    {
        return new StringBuilder().append("</").append(this.getTemplate().getTemplateName()).append(">").toString();
    }
    private String buildDefaultEndTag()
    {
        return this.startTag.getElement().getEndTag().toString();
    }
    private CharSequence buildNormalizedContent() throws IOException
    {
        StringBuilder retVal = new StringBuilder();

        /**
         * If we're in a template, we'll iterate over all added sub-tags to see if it's a property of the template.
         * If it is, we lookup that property (or that nth property in case of doubles) and compare it with the original
         * property to see if it changed (in cleaned form). If it's unchanged, we can safely normalize it away
         * because the parser will render out the default content of a missing property.
         */
        if (this.isTemplate()) {

            Map<String, Integer> properties = new HashMap<>();
            for (CharSequence c : this.normalizedContent) {

                CharSequence processedContent = null;

                Source contentSource = new Source(c);
                List<Element> contentElements = contentSource.getChildElements();
                if (contentElements.size() == 1) {
                    Element content = contentElements.get(0);
                    if (this.isPropertyTag(content.getStartTag())) {
                        String property = this.getPropertyAttribute(content.getStartTag()).getValue();

                        //if a template has multiple properties for eg 'text',
                        //make sure we compare against the same n-th property
                        //of the the template. For this, we need to calculate 'n' first:
                        int propNum = 0;
                        if (properties.containsKey(property)) {
                            propNum = properties.get(property) + 1;
                        }
                        properties.put(property, propNum);

                        Element templateProperty = this.getNthTemplateProperty(property, propNum);
                        if (templateProperty != null) {
                            //note: the template needs to be rendered because we don't want to compare with variables here (yet)
                            String cleanTemplateProperty = this.render(this.collapseAllWhitespace(templateProperty));
                            String cleanContentProperty = this.collapseAllWhitespace(content);
                            if (cleanContentProperty.equals(cleanTemplateProperty)) {
                                //when all is set and done, we just wipe the property,
                                //relying of the html parser to add the default value again
                                processedContent = "";
                            }
                        }
                    }
                }

                if (processedContent == null) {
                    retVal.append(c);
                }
            }
        }
        else {
            for (CharSequence c : this.normalizedContent) {
                retVal.append(c);
            }
        }

        return retVal;
    }
    private String collapseAllWhitespace(Segment segment)
    {
        return new SourceFormatter(segment).setCollapseWhiteSpace(true).setIndentString("").setNewLine("").toString();
    }
    private List<StartTag> getTemplateProperties()
    {
        if (this.cachedTemplateProperties == null) {
            this.cachedTemplateProperties = new ArrayList<>();
            Source templateContent = new Source(this.getTemplate().getInnerHtml());
            List<StartTag> allTemplateStartTags = templateContent.getAllStartTags();
            for (StartTag startTag : allTemplateStartTags) {
                if (this.isPropertyTag(startTag)) {
                    this.cachedTemplateProperties.add(startTag);
                }
            }
        }

        return this.cachedTemplateProperties;
    }
    private Element getNthTemplateProperty(String propertyValue, int n)
    {
        Element retVal = null;

        int encounteredProperties = 0;
        for (StartTag p : this.getTemplateProperties()) {
            if (this.getPropertyAttribute(p).getValue().equals(propertyValue)) {
                if (encounteredProperties == n) {
                    retVal = p.getElement();
                    break;
                }
                else {
                    encounteredProperties++;
                }
            }
        }

        return retVal;
    }
    private CharSequence reinsertVariables(CharSequence normalizedHtml) throws IOException
    {
        CharSequence retVal = normalizedHtml;

        if (this.isTagTemplate()) {

            if (normalizedHtml.length() > 0) {

                if (this.getVariableAttributeQueries().size() + this.getVariableElementQueries().size() > 0) {
                    boolean edited = false;
                    Document normalized = Jsoup.parse(normalizedHtml.toString());

                    for (Map.Entry<String, String[]> a : this.getVariableAttributeQueries().entrySet()) {
                        Elements selectedEl = normalized.select(a.getKey());
                        if (!selectedEl.isEmpty() && selectedEl.attr(a.getValue()[0]).trim().equals(a.getValue()[1])) {
                            selectedEl.attr(a.getValue()[0], a.getValue()[2]);
                            edited = true;
                        }
                    }

                    for (Map.Entry<String, String[]> e : this.getVariableElementQueries().entrySet()) {
                        Elements selectedEl = normalized.select(e.getKey());
                        if (!selectedEl.isEmpty() && selectedEl.html().trim().equals(e.getValue()[0])) {
                            selectedEl.html(e.getValue()[1]);
                            edited = true;
                        }
                    }

                    if (edited) {
                        retVal = normalized.body().html();
                    }
                }
            }
        }

        return retVal;
    }
    private boolean isTemplateVariable(String html)
    {
        return html.startsWith("$" + TemplateContext.InternalProperties.MESSAGES.name()) ||
               html.startsWith("$" + TemplateContext.InternalProperties.CONSTANTS.name());
    }
    private String render(String html) throws IOException
    {
        return R.resourceManager().newTemplate(new StringSource(this.source.getUri(),
                                                                html,
                                                                MimeTypes.HTML,
                                                                this.source.getLanguage())).render();
    }
    private Document getTemplateHtml()
    {
        if (this.templateHtml == null) {
            this.templateHtml =
                            Jsoup.parse(new StringBuilder().append(this.getTemplate().buildStartTag()).append(this.getTemplate().getInnerHtml()).append(this.getTemplate().buildEndTag()).toString());
        }

        return this.templateHtml;
    }
    private Map<String, String[]> getVariableAttributeQueries() throws IOException
    {
        if (this.variableAttributeQueries == null) {

            //we'll do two in one here
            this.variableAttributeQueries = new HashMap<>();
            this.variableElementQueries = new HashMap<>();

            //Note that JSoup wraps all html in a <html><head></head><body></body>[inserted]</html> container...
            Elements templateIter = this.getTemplateHtml().body().children().select("*");
            for (org.jsoup.nodes.Element e : templateIter) {

                //(1) analyze the start tag's attributes
                org.jsoup.nodes.Attributes attributes = e.attributes();
                for (org.jsoup.nodes.Attribute a : attributes) {
                    String originalVal = a.getValue().trim();
                    if (this.isTemplateVariable(originalVal)) {
                        this.variableAttributeQueries.put(e.cssSelector(), new String[] { a.getKey(), this.render(originalVal), originalVal });
                    }
                }

                //(1) analyze the contents
                String originalHtml = e.html().trim();
                if (this.isTemplateVariable(originalHtml)) {
                    this.variableElementQueries.put(e.cssSelector(), new String[] { this.render(originalHtml), originalHtml });
                }
            }
        }

        return this.variableAttributeQueries;
    }
    private Map<String, String[]> getVariableElementQueries() throws IOException
    {
        if (this.variableElementQueries == null) {
            //this will init both
            this.getVariableAttributeQueries();
        }

        return this.variableElementQueries;
    }

    //-----MGMT METHODS-----
    @Override
    public String toString()
    {
        return this.startTag.toString();
    }
}
