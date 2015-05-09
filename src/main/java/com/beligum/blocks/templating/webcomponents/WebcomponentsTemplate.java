/*
 * Copyright (c) 2015 Beligum b.v.b.a. (http://www.beligum.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Contributors:
 *     Beligum <info@beligum.com> - initial implementation
 */

package com.beligum.blocks.templating.webcomponents;

import com.beligum.base.templating.ifaces.Template;
import com.beligum.base.templating.ifaces.TemplateContext;
import com.beligum.base.utils.Logger;
import com.beligum.base.validation.messages.FeedbackMessage;
import com.beligum.blocks.templating.webcomponents.html5.HtmlCodeFactory;
import com.beligum.blocks.templating.webcomponents.html5.ifaces.HtmlElement;
import com.beligum.blocks.templating.webcomponents.html5.ifaces.HtmlImportTemplate;
import com.beligum.blocks.templating.webcomponents.html5.ifaces.HtmlSnippet;
import freemarker.template.TemplateException;
import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.text.ParseException;

public class WebcomponentsTemplate implements Template
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    private WebcomponentsTemplateEngine templateEngine;
    private Template wrappedTemplate;
    private WebcomponentsTemplateContext context;

    //-----CONSTRUCTORS-----
    public WebcomponentsTemplate(WebcomponentsTemplateEngine templateEngine, Template wrappedTemplate)
    {
        this.templateEngine = templateEngine;
        this.wrappedTemplate = wrappedTemplate;
        this.context = new WebcomponentsTemplateContext(this);
    }

    //-----PUBLIC FUNCTIONS-----
    @Override
    public String getName()
    {
        return this.wrappedTemplate.getName();
    }
    @Override
    public TemplateContext getContext()
    {
        return this.context;
    }
    @Override
    public Template set(String name, Object object)
    {
        return this.context.set(name, object).getTemplate();
    }
    @Override
    public TemplateContext addMessage(FeedbackMessage feedbackMessage)
    {
        return this.context.addMessage(feedbackMessage);
    }
    @Override
    public String render()
    {
        String retVal = "";

        if (this.wrappedTemplate != null) {
            try (StringWriter writer = new StringWriter()) {
                retVal = this.doRender(writer).toString();
            }
            catch (Exception e) {
                Logger.error("Exception while rendering template " + this.wrappedTemplate, e);
            }
        }

        return retVal;
    }
    @Override
    public void render(Writer writer)
    {
        if (this.wrappedTemplate != null) {
            try {
                this.doRender(writer);
            }
            catch (Exception e) {
                Logger.error("Exception while rendering template " + this.wrappedTemplate, e);
            }
        }
    }
    @Override
    public Object getNodeTree()
    {
        throw new NotImplementedException();
    }
    @Override
    public String toString()
    {
        return this.render();
    }

    //-----PROTECTED FUNCTIONS-----

    //-----PRIVATE FUNCTIONS-----
    private Writer doRender(Writer writer) throws IOException, TemplateException, ParseException
    {
        //first, renderContent the html out on a low-level basis (eg. parsed through velocity)
        String lowlevelHtml = this.wrappedTemplate.render();

        //now, let's "understand" the html into an abstracted dom
        HtmlSnippet html = HtmlCodeFactory.create(lowlevelHtml);

        //and parse the templates
        String templateTagsCsv = this.templateEngine.getTemplateLoader().getTemplateTagsCsv();
        if (!StringUtils.isEmpty(templateTagsCsv)) {
            Iterable<HtmlElement> templateElements = html.select(templateTagsCsv);
            for (HtmlElement templateElement : templateElements) {
                HtmlImportTemplate template = this.templateEngine.getTemplateLoader().getCachedTemplates().get(templateElement.getTagName());
                //render out the template with the element as "data" and replace it with the result
                templateElement.replaceWith(template.renderContent(templateElement));
            }
        }

        writer.write(html.getHtml());

        return writer;
    }
}
