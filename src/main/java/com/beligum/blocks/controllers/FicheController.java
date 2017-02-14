package com.beligum.blocks.controllers;

import com.beligum.base.resources.ifaces.Source;
import com.beligum.base.server.R;
import com.beligum.base.templating.ifaces.TemplateContext;
import com.beligum.blocks.config.RdfFactory;
import com.beligum.blocks.endpoints.ifaces.AutocompleteSuggestion;
import com.beligum.blocks.endpoints.ifaces.RdfQueryEndpoint;
import com.beligum.blocks.endpoints.ifaces.ResourceInfo;
import com.beligum.blocks.rdf.ifaces.RdfClass;
import com.beligum.blocks.rdf.ifaces.RdfProperty;
import com.beligum.blocks.templating.blocks.DefaultTemplateController;
import com.beligum.blocks.templating.blocks.HtmlParser;
import com.beligum.blocks.templating.blocks.HtmlTemplate;
import com.beligum.blocks.utils.RdfTools;
import gen.com.beligum.blocks.core.constants.blocks.core;
import net.htmlparser.jericho.*;
import org.apache.commons.lang.StringUtils;

import java.io.IOException;
import java.net.URI;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Iterator;
import java.util.Locale;

import static gen.com.beligum.blocks.core.constants.blocks.core.INPUT_TYPE_TIME_GMT_ATTR;
import static java.time.ZoneOffset.UTC;

/**
 * Created by bram on 2/3/17.
 */
public class FicheController extends DefaultTemplateController
{
    //-----CONSTANTS-----

    //-----VARIABLES-----

    //-----CONSTRUCTORS-----

    //-----PUBLIC METHODS-----
    @Override
    public void created()
    {
    }
    @Override
    public void prepareForSave(Source source, Element element, OutputDocument htmlOutput)
    {
        this.normalizeLabel(source, element, htmlOutput);
    }
    /**
     * Note that we could actually only normalize during save, because a new page should be rendered out correctly
     * if the fiche entry was normalized during save. But by doing it just before a copy, we are backwards compatible.
     */
    @Override
    public void prepareForCopy(Source source, Element element, OutputDocument htmlOutput, URI targetUri, Locale targetLanguage) throws IOException
    {
        this.normalizeLabel(source, element, htmlOutput);

        if (!source.getLanguage().equals(targetLanguage)) {
            this.translateValue(source, element, htmlOutput, targetLanguage);
        }
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----
    /**
     * When we copy/save a page with fiche entries, the labels of the entries will have changed using javascript, so we don't have a template-safe way of
     * normalizing them back to variables (only the default placeholder of the label tag will be normalized back to it's template variable).
     * So every time a template element comes in, we need to search for the label and normalize it back to it's template variable, based on
     * the currently set resource type label.
     */
    private void normalizeLabel(Source source, Element element, OutputDocument htmlOutput)
    {
        Element propertyEl = element.getFirstElementByClass(core.FICHE_ENTRY_PROPERTY_CLASS);
        if (propertyEl != null) {
            String resourceType = HtmlTemplate.getPropertyAttribute(propertyEl.getStartTag());
            if (!StringUtils.isEmpty(resourceType)) {
                RdfClass rdfClass = RdfFactory.getClassForResourceType(URI.create(resourceType));
                if (rdfClass != null && rdfClass instanceof RdfProperty) {
                    Element labelEl = element.getFirstElement(HtmlParser.NON_RDF_PROPERTY_ATTR, core.FICHE_ENTRY_NAME_PROPERTY, false);
                    if (labelEl != null) {
                        String testLabel = R.i18n().get(rdfClass.getLabelMessage(), source.getLanguage());

                        Iterator<Segment> labelContentIter = labelEl.getNodeIterator();
                        while (labelContentIter.hasNext()) {
                            Segment labelContent = labelContentIter.next();
                            //if we're dealing with a content tag (eg. no start or end tag), we check if we need to replace the stringified
                            // label by it's message
                            if (!(labelContent instanceof StartTag) && !(labelContent instanceof EndTag)) {
                                String label = labelContent.toString().trim();
                                if (label.equals(testLabel)) {
                                    String variable = new StringBuilder().append(R.resourceManager().getTemplateEngine().getVariablePrefix())
                                                                         .append(TemplateContext.InternalProperties.MESSAGES.name())
                                                                         .append(R.resourceManager().getTemplateEngine().getPathSeparator())
                                                                         .append(rdfClass.getLabelKey())
                                                                         .toString();
                                    htmlOutput.replace(labelContent, variable);
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    /**
     * When we copy a page with fiche entries, some of the values of the entries have been set during creation and in a specified language.
     * We might want to re-read them to eg. set the name of the link to a translated value.
     */
    private void translateValue(Source source, Element element, OutputDocument htmlOutput, Locale toLanguage) throws IOException
    {
        Element propertyEl = element.getFirstElementByClass(core.FICHE_ENTRY_PROPERTY_CLASS);
        if (propertyEl != null) {
            String resourceType = HtmlTemplate.getPropertyAttribute(propertyEl.getStartTag());
            if (!StringUtils.isEmpty(resourceType)) {
                RdfClass rdfClass = RdfFactory.getClassForResourceType(URI.create(resourceType));
                if (rdfClass != null && rdfClass instanceof RdfProperty) {
                    RdfProperty property = (RdfProperty) rdfClass;

                    String content = propertyEl.getAttributeValue("content");
                    String resource = propertyEl.getAttributeValue("resource");
                    if (content != null || resource != null) {

                        switch (property.getWidgetType()) {
                            case Editor:
                            case InlineEditor:
                            case Boolean:
                            case Number:
                            case Color:
                            case Uri:
                                break;
                            case Date:
                            case Time:
                            case DateTime:
                                //note that the value is always stored in UTC zone (so the zone of this ZonedDateTime below should always be UTC)
                                ZonedDateTime value = ZonedDateTime.parse(content, DateTimeFormatter.ISO_DATE_TIME);
                                //this flag only controls how the value above is rendered out to the html, not how it's stored
                                ZoneId zone = RdfTools.parseRdfaBoolean(propertyEl.getAttributeValue(INPUT_TYPE_TIME_GMT_ATTR)) ? UTC : ZoneId.systemDefault();

                                switch (property.getWidgetType()) {
                                    case Date:
                                        htmlOutput.replace(propertyEl.getContent(), RdfTools.serializeDateHtml(zone, toLanguage, value));
                                        break;
                                    case Time:
                                        htmlOutput.replace(propertyEl.getContent(), RdfTools.serializeTimeHtml(zone, toLanguage, value));
                                        break;
                                    case DateTime:
                                        htmlOutput.replace(propertyEl.getContent(), RdfTools.serializeDateTimeHtml(zone, toLanguage, value));
                                        break;
                                }

                                break;

                            case Enum:

                                //note: contrary to the resource endpoint below, we want the endpoint of the property, not the class, so don't use property.getDataType().getEndpoint() here
                                Collection<AutocompleteSuggestion> enumSuggestion = property.getEndpoint().search(property, content, RdfQueryEndpoint.QueryType.NAME, toLanguage, 1);
                                if (enumSuggestion.size() == 1) {
                                    htmlOutput.replace(propertyEl.getContent(), RdfTools.serializeEnumHtml(enumSuggestion.iterator().next()));
                                }

                                break;

                            case Resource:

                                ResourceInfo resourceInfo = property.getDataType().getEndpoint().getResource(property.getDataType(), URI.create(resource), toLanguage);
                                if (resourceInfo != null) {
                                    htmlOutput.replace(propertyEl.getContent(), RdfTools.serializeResourceHtml(resourceInfo));
                                }

                                break;
                            default:
                                throw new IOException("Encountered unimplemented widget type parser, please fix; " + property.getWidgetType());
                        }
                    }
                }
            }
        }
    }
}