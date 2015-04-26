package com.beligum.blocks.models;

import com.beligum.blocks.exceptions.ParseException;
import com.beligum.blocks.parsers.ElementParser;
import com.beligum.blocks.parsers.visitors.template.PagetemplateVisitor;
import org.jsoup.nodes.Element;

/**
 * Created by wouter on 16/03/15.
 */
public abstract class PageTemplate extends Blueprint
{

    public PageTemplate()
    {

    }

    public PageTemplate(Element element, String language) throws ParseException
    {
        super(element, language);
        element.children().remove();
        while (element.parent() != null) {
            if (ElementParser.isPageTemplateRoot(element)) {
                this.name = ElementParser.getPagetemplateName(element);
            }
            element = element.parent();
        }
        this.transientElement = element;
    }

    @Override
    protected PagetemplateVisitor getVisitor()
    {
        return new PagetemplateVisitor(this.name);
    }

    @Override
    public PagetemplateVisitor parse() throws ParseException
    {
        PagetemplateVisitor pagetemplateVisitor = (PagetemplateVisitor) super.parse();
        return pagetemplateVisitor;
    }

}
