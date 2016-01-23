package com.beligum.blocks.pages;

import com.beligum.base.utils.Logger;
import com.beligum.blocks.templating.blocks.HtmlParser;
import com.beligum.blocks.templating.blocks.HtmlTemplate;
import com.beligum.blocks.templating.blocks.TemplateCache;
import net.htmlparser.jericho.*;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.net.URI;
import java.util.*;

/**
 * Created by bram on 1/23/16.
 */
public class NewPageParser
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    private final TemplateCache allTagTemplates;
    private static final Set IGNORED_TAGS = new HashSet(Arrays.asList(new String[] { "html", "head", "body" }));

    //-----CONSTRUCTORS-----
    public NewPageParser()
    {
        this.allTagTemplates = HtmlParser.getTemplateCache();
    }

    //-----PUBLIC METHODS-----
    public void parse(String html, URI baseContext) throws IOException
    {
        Source source = new Source(html);

        OutputDocument outputDocument = new OutputDocument(new Source(""));

        HtmlTemplate currentTemplate = null;
        Element htmlElement = source.getFirstElement(HtmlParser.HTML_ROOT_ELEM);
        String pageTemplateName = htmlElement.getAttributeValue(HtmlParser.HTML_ROOT_TEMPLATE_ATTR);
        if (StringUtils.isEmpty(pageTemplateName)) {
            throw new IOException("Encountered an attempt to save html without a page template; this shouldn't happen; " + htmlElement);
        }
        else {
            currentTemplate = this.allTagTemplates.get(pageTemplateName);
            if (currentTemplate == null) {
                throw new IOException("Encountered an attempt to save html, but I can't find a page template with name " + pageTemplateName);
            }
        }

        StringBuilder outputHtml = new StringBuilder();

        outputHtml.append(this.instantiateTemplateStartTag(htmlElement, currentTemplate, new HashSet(Arrays.asList(new String[] { HtmlParser.HTML_ROOT_TEMPLATE_ATTR }))));
        this.processNodes(htmlElement.getContent().getFirstStartTag(), 0, new Stack<Element>(), new Stack<Element>(), outputHtml);
        outputHtml.append(this.instantiateTemplateEndTag(currentTemplate));

        SourceFormatter formatter = new SourceFormatter(new Source(outputHtml.toString()));
        formatter.setCollapseWhiteSpace(true);
        formatter.setIndentString("\t");
        formatter.setNewLine("\n");
        Logger.info(formatter.toString());
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----
    private void processNodes(Tag tag, int depth, Stack<Element> templateStack, Stack<Element> propertyStack, StringBuilder outputDocument)
    {
        if (tag == null || tag.getName().equals(HtmlParser.HTML_ROOT_ELEM)) {
            return;
        }

        Element currentTemplate = templateStack.isEmpty() ? null : templateStack.peek();
        Element currentProperty = propertyStack.isEmpty() ? null : propertyStack.peek();

        still a bug here: if a new tempalte is started, the properties should be reset

        boolean writeTag = this.isIgnoredTag(tag) || currentProperty != null;

        if (tag instanceof StartTag) {
            StartTag startTag = (StartTag) tag;

            if (this.allTagTemplates.containsKey(tag.getName())) {
                templateStack.push(startTag.getElement());
                writeTag = true;
            }
            if (this.isProperty(startTag)) {
                propertyStack.push(startTag.getElement());
                writeTag = true;
            }

            depth++;
        }
        else if (tag instanceof EndTag) {
            if (currentTemplate != null && currentTemplate.getEndTag() != null && currentTemplate.getEndTag().equals(tag)) {
                templateStack.pop();
                writeTag = true;
            }
            if (currentProperty != null && currentProperty.getEndTag() != null && currentProperty.getEndTag().equals(tag)) {
                propertyStack.pop();
                writeTag = true;
            }

            depth--;
        }

        if (writeTag) {
            outputDocument.append(tag.toString());
        }

        this.processNodes(tag.getNextTag(), depth, templateStack, propertyStack, outputDocument);
    }
    private boolean isIgnoredTag(Tag tag)
    {
        return tag.getElement() != null && this.IGNORED_TAGS.contains(tag.getElement().getName());
    }
    private boolean isStandAlone(StartTag tag)
    {
        return tag.isEmptyElementTag() || tag.getElement().getEndTag() == null;
    }
    private boolean isProperty(StartTag tag)
    {
        return tag.getAttributeValue(HtmlParser.RDF_PROPERTY_ATTR) != null || tag.getAttributeValue(HtmlParser.NON_RDF_PROPERTY_ATTR) != null;
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
