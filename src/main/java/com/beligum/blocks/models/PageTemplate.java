package com.beligum.blocks.models;

import com.beligum.blocks.config.ParserConstants;
import com.beligum.blocks.base.Blocks;
import com.beligum.blocks.exceptions.ParseException;
import com.beligum.blocks.parsers.ElementParser;
import com.beligum.blocks.parsers.visitors.template.PagetemplateVisitor;
import com.beligum.blocks.usermanagement.Permissions;
import org.apache.shiro.SecurityUtils;
import org.jsoup.nodes.Element;

/**
 * Created by wouter on 16/03/15.
 */
public abstract class PageTemplate extends Blueprint
{

    public PageTemplate() {

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
    protected PagetemplateVisitor getVisitor() {
        return new PagetemplateVisitor(this.name);
    }

    @Override
    public PagetemplateVisitor parse() throws ParseException
    {
        PagetemplateVisitor pagetemplateVisitor = (PagetemplateVisitor)super.parse();
        return  pagetemplateVisitor;
    }


    public String getRenderedTemplate(boolean readOnly, BasicTemplate templateToRender) throws Exception
    {
        String retVal = this.value;

        templateToRender.isTemplateContent(true);
        String template = templateToRender.getRenderedTemplate(readOnly, true).toString();
        retVal = retVal.replaceFirst(ParserConstants.TEMPLATE_CONTENT, template);


        StringBuilder scriptsAndLinks = new StringBuilder();
        // Append Blocks client only if logged in
        if (SecurityUtils.getSubject().isPermitted(Permissions.ENTITY_MODIFY)) {
            for (String link : Blocks.templateCache().getBlocksLinks()) {
                scriptsAndLinks.append(link).append(System.lineSeparator());
            }
        }

        for (String link: this.getLinks()) {
            scriptsAndLinks.append(link).append(System.lineSeparator());
        }

        // Append Blocks client only if logged in
        if (SecurityUtils.getSubject().isPermitted(Permissions.ENTITY_MODIFY)) {
            for (String script : Blocks.templateCache().getBlocksScripts()) {
                scriptsAndLinks.append(script).append(System.lineSeparator());
            }
        }

        for (String script: this.getScripts()) {
            scriptsAndLinks.append(script).append(System.lineSeparator());
        }


        retVal = retVal.replaceFirst(ParserConstants.TEMPLATE_HEAD, scriptsAndLinks.toString());

        StringBuilder props = new StringBuilder(retVal);
        if (properties.size() > 0) {
            props = this.fillTemplateWithProperties(props, readOnly, templateToRender, true);
        }

        return props.toString();

    }


}
