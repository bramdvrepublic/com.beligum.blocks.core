package com.beligum.blocks.controllers;

import com.beligum.base.resources.ifaces.Source;
import com.beligum.base.server.R;
import com.beligum.base.templating.ifaces.TemplateContext;
import com.beligum.blocks.config.RdfFactory;
import com.beligum.blocks.rdf.ifaces.RdfClass;
import com.beligum.blocks.templating.blocks.DefaultTemplateController;
import com.beligum.blocks.templating.blocks.HtmlParser;
import com.beligum.blocks.templating.blocks.HtmlTemplate;
import gen.com.beligum.blocks.core.constants.blocks.core;
import net.htmlparser.jericho.*;
import org.apache.commons.lang.StringUtils;

import java.net.URI;
import java.util.Iterator;

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
    public void prepareForCopy(Source source, Element element, OutputDocument htmlOutput)
    {
        this.normalizeLabel(source, element, htmlOutput);
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
                if (rdfClass != null) {
                    Element labelEl = element.getFirstElement(HtmlParser.NON_RDF_PROPERTY_ATTR, gen.com.beligum.blocks.core.constants.blocks.core.FICHE_ENTRY_NAME_PROPERTY, false);
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
}
