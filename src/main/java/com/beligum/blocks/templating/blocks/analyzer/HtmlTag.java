package com.beligum.blocks.templating.blocks.analyzer;

import com.beligum.blocks.templating.blocks.HtmlParser;
import com.beligum.blocks.templating.blocks.HtmlTemplate;
import com.beligum.blocks.templating.blocks.PageTemplate;
import com.beligum.blocks.templating.blocks.TemplateCache;
import net.htmlparser.jericho.*;

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
    private StartTag startTag;
    private HtmlTemplate cachedTemplate;
    private boolean triedTemplate;

    private CharSequence normalizedStartTag;
    private List<CharSequence> normalizedContent;
    private CharSequence normalizedEndTag;

    //-----CONSTRUCTORS-----
    public HtmlTag(StartTag startTag)
    {
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
    public void appendNormalizedSubtag(HtmlTag tag)
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
    public CharSequence toNormalizedString()
    {
        StringBuilder retVal = new StringBuilder();

        retVal.append(this.normalizedStartTag);
        retVal.append(this.buildNormalizedContent());
        retVal.append(this.normalizedEndTag);

        return retVal;
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

        //first normalization: wipe all empty attributes
        //TODO what about comparing later on?

        //we'll save all the incoming attributes, except the ones that are already present in the template,
        //because those will be added during rendering, so we're normalizing them away
        Map<String, String> templateAttributes = this.getTemplate().getAttributes();
        if (templateAttributes != null) {
            for (Map.Entry<String, String> templateAttr : templateAttributes.entrySet()) {
                String instanceAttrValue = attributes.get(templateAttr.getKey());
                if (instanceAttrValue != null && instanceAttrValue.equals(templateAttr.getValue())) {
                    attributes.remove(templateAttr.getKey());
                }
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
    private CharSequence buildNormalizedContent()
    {
        StringBuilder retVal = new StringBuilder();

        if (this.isTemplate()) {
            Source templateContent = new Source(this.getTemplate().getInnerHtml());
            List<StartTag> templateProperties = new ArrayList<>();
            List<StartTag> allTemplateStartTags = templateContent.getAllStartTags();
            for (StartTag startTag : allTemplateStartTags) {
                if (this.isPropertyTag(startTag)) {
                    templateProperties.add(startTag);
                }
            }

            Map<String, Integer> properties = new HashMap<>();
            for (CharSequence c : this.normalizedContent) {

                CharSequence processedContent = null;

                Source contentSource = new Source(c);
                List<Element> contentElements = contentSource.getChildElements();
                if (contentElements.size() == 1) {
                    StartTag contentStartTag = contentElements.get(0).getStartTag();
                    if (this.isPropertyTag(contentStartTag)) {
                        EndTag contentEndTag = contentStartTag.getElement().getEndTag();

                        Attribute property = this.getPropertyAttribute(contentStartTag);

                        int cardinality = 0;
                        if (properties.containsKey(property.getValue())) {
                            cardinality = properties.get(property.getValue()) + 1;
                        }
                        properties.put(property.getValue(), cardinality);

                        int encounteredProperties = 0;
                        StartTag templateProperty = null;
                        for (StartTag p : templateProperties) {
                            if (this.getPropertyAttribute(p).getValue().equals(property.getValue())) {
                                if (encounteredProperties == cardinality) {
                                    templateProperty = p;
                                    break;
                                }
                                else {
                                    encounteredProperties++;
                                }
                            }
                        }

                        if (templateProperty != null) {
                            String cleanTemplateProperty = new SourceFormatter(templateProperty).setCollapseWhiteSpace(true).setIndentString("").setNewLine("").toString();
                            String cleanContentProperty = new SourceFormatter(contentStartTag).setCollapseWhiteSpace(true).setIndentString("").setNewLine("").toString();

                            if (cleanContentProperty.equals(cleanTemplateProperty)) {
                                processedContent = new StringBuilder().append(contentStartTag.toString()).append(contentEndTag == null ? "" : contentEndTag.toString());
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

        //        if (retVal.length() > 0 && this.isTemplate()) {
        //            SourceFormatter formatter1 = new SourceFormatter(new Source(retVal));
        //            formatter1.setCollapseWhiteSpace(true);
        //            formatter1.setIndentString("");
        //            formatter1.setNewLine("");
        //            String oneliner1 = formatter1.toString();
        //
        //
        //
        //            if (oneliner1.equals(oneliner2)) {
        //                retVal.setLength(0);
        //            }
        //        }

        return retVal;
    }

    //-----MGMT METHODS-----

    @Override
    public String toString()
    {
        return this.startTag.toString();
    }
}
