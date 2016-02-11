package com.beligum.blocks.fs.pages;

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
 * Convert incoming html to a normalized form, based on the current page and tag templates.
 * TODO would be nice if unmodified properties and/or template-instances would be reverted to their 'collapsed' form
 *
 * Created by bram on 1/23/16.
 */
public class PageHtmlParser
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    private final TemplateCache allTagTemplates;
    private static final Set ALWAYS_INCLUDE_TAGS = new HashSet(Arrays.asList(new String[] {  }));

    //-----CONSTRUCTORS-----
    public PageHtmlParser()
    {
        this.allTagTemplates = HtmlParser.getTemplateCache();
    }

    //-----PUBLIC METHODS-----
    public String parse(String html, URI baseContext, boolean format) throws IOException
    {
        Source source = new Source(html);

        HtmlTemplate currentTemplate = null;
        Element htmlElement = source.getFirstElement(HtmlParser.HTML_ROOT_ELEM);
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

        String retVal = outputHtml.toString();
        if (format) {
            SourceFormatter formatter = new SourceFormatter(new Source(retVal));
            formatter.setCollapseWhiteSpace(true);
            formatter.setIndentString("    ");
            formatter.setNewLine("\n");
            retVal = formatter.toString();
        }

        return retVal;
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----
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
                    if (!propertyStack.isEmpty() && propertyStack.peek()!=null) {
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
