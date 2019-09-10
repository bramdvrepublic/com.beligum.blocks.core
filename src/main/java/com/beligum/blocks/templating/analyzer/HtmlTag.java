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

package com.beligum.blocks.templating.analyzer;

import com.beligum.base.resources.MimeTypes;
import com.beligum.base.resources.sources.StringSource;
import com.beligum.base.server.R;
import com.beligum.blocks.templating.HtmlParser;
import com.beligum.blocks.templating.HtmlTemplate;
import com.beligum.blocks.templating.PageTemplate;
import com.beligum.blocks.templating.TagTemplate;
import net.htmlparser.jericho.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.util.*;

/**
 * This is basically a wrapper around a Jericho StartTag that encapsulates all tag-related processing,
 * plus acts as a store for it's own normalized start, content and end tags (separately).
 * <p>
 * Created by bram on 1/26/17.
 */
public class HtmlTag implements HtmlTemplate.SubstitionReferenceRenderer
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

    //-----CONSTRUCTORS-----
    public HtmlTag(com.beligum.base.resources.ifaces.Source source, StartTag startTag)
    {
        this.source = source;
        this.startTag = startTag;

        this.normalizedStartTag = "";
        this.normalizedContent = new ArrayList<>();
        this.normalizedEndTag = "";
    }
    protected HtmlTag(HtmlTag htmlTag)
    {
        this(htmlTag.source, htmlTag.startTag);
    }

    //-----PUBLIC METHODS-----
    @Override
    public String renderTemplateString(String html) throws IOException
    {
        return R.resourceManager().newTemplate(new StringSource(this.source.getUri(),
                                                                html,
                                                                MimeTypes.HTML,
                                                                this.source.getLanguage())).render();
    }
    public String getName()
    {
        return this.startTag.getName();
    }
    public String getContent()
    {
        return this.startTag.getElement().getContent().toString();
    }
    public String getAttributeValue(String attributeName)
    {
        return this.startTag.getAttributeValue(attributeName);
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
        return HtmlTemplate.isPropertyTag(this.startTag);
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
            this.cachedTemplate = HtmlTemplate.getTemplateInstance(this.startTag);
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
        for (CharSequence c : this.normalizedContent) {
            normalizedHtml.append(c);
        }
        normalizedHtml.append(this.normalizedEndTag);

        CharSequence retVal = normalizedHtml;

        //if we're dealing with a template tag, we need to execute some more post-processing
        if (this.isTemplate()) {
            retVal = this.normalizeTemplate(normalizedHtml);
        }

        return retVal;
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----
    private String buildDefaultStartTag()
    {
        return this.startTag.toString();
    }
    private String buildTemplateStartTag()
    {
        Map<String, String> attributes = new LinkedHashMap<>();

        this.startTag.getAttributes().populateMap(attributes, false);

        //if we have empty attributes coming in and they're not in the template,
        // we should wipe them (and hope we don't break any scripts)
        Iterator<Map.Entry<String, String>> iter = attributes.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<String, String> a = iter.next();
            if (a.getValue() != null && a.getValue().trim().isEmpty() && !this.getTemplate().getAttributes().containsKey(a.getKey())) {
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
    private CharSequence normalizeTemplate(CharSequence normalizedHtml) throws IOException
    {
        CharSequence retVal = normalizedHtml;

        if (normalizedHtml.length() > 0) {

            Document document = Jsoup.parseBodyFragment(normalizedHtml.toString());

            //iterate over all template attributes and delete them in the tag
            // because those are default template attributes and will be added again during rendering,
            // so we're normalizing them away
            org.jsoup.nodes.Attributes attributes = document.attributes();
            Map<String, String> templateAttributes = this.getTemplate().getAttributes();
            for (Map.Entry<String, String> templateAttr : templateAttributes.entrySet()) {
                String instanceAttr = attributes.get(templateAttr.getKey());
                if (instanceAttr != null && instanceAttr.equals(templateAttr.getValue())) {
                    attributes.remove(templateAttr.getKey());
                }
            }

            boolean edited = false;
            for (HtmlTemplate.SubstitionReference substitutor : this.getTemplate().getNormalizationSubstitutions()) {
                edited = substitutor.replaceIn(document, this) || edited;
            }
            if (edited) {
                retVal = document.body().html();
            }
        }

        return retVal;
    }

    //-----MGMT METHODS-----
    @Override
    public String toString()
    {
        return this.startTag.toString();
    }
}
