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

import com.beligum.base.filesystem.GeneratedFile;
import com.beligum.base.templating.freemarker.FreemarkerTemplateEngine;
import com.beligum.base.templating.ifaces.Template;
import com.beligum.base.templating.ifaces.TemplateEngine;
import com.beligum.base.templating.velocity.VelocityTemplateEngine;

public class WebcomponentsTemplateEngine implements TemplateEngine
{
    //-----CONSTANTS-----
    private enum WrappedTemplateEngineType
    {
        VELOCITY,
        FREEMARKER
    }
    //TODO make this configurable?
    private WrappedTemplateEngineType WRAPPED_ENGINE_TYPE = WrappedTemplateEngineType.VELOCITY;

    //-----VARIABLES-----
    private TemplateEngine wrappedTemplateEngine;
    private WebcomponentsTemplateLoader templateLoader;

    //-----CONSTRUCTORS-----
    public WebcomponentsTemplateEngine() throws Exception
    {
        switch (WRAPPED_ENGINE_TYPE) {
            case VELOCITY:
                this.wrappedTemplateEngine = new VelocityTemplateEngine();
                break;
            case FREEMARKER:
                this.wrappedTemplateEngine = new FreemarkerTemplateEngine();
                break;
            default:
                throw new Exception("Encountered unknown wrapped template engine, this shouldn't happen; "+WRAPPED_ENGINE_TYPE);
        }

        //this will immediately scan the classpath for webcomponent templates
        this.templateLoader = new WebcomponentsTemplateLoader(this);
    }

    //-----PUBLIC FUNCTIONS-----
    @Override
    public Template getEmptyTemplate(GeneratedFile generatedFile)
    {
        return this.getNewTemplate(generatedFile);
    }
    @Override
    public Template getNewTemplate(GeneratedFile generatedFile)
    {
        return new WebcomponentsTemplate(this, this.wrappedTemplateEngine.getNewTemplate(generatedFile));
    }
    @Override
    public Template getEmptyStringTemplate(String templateString)
    {
        return this.getNewStringTemplate(templateString);
    }
    @Override
    public Template getNewStringTemplate(String templateString)
    {
        return new WebcomponentsTemplate(this, this.wrappedTemplateEngine.getNewStringTemplate(templateString));
    }
    @Override
    public Object getDelegateEngine()
    {
        return this.wrappedTemplateEngine;
    }
    public TemplateEngine getWrappedTemplateEngine()
    {
        return this.wrappedTemplateEngine;
    }

    //-----PROTECTED FUNCTIONS-----
    protected WebcomponentsTemplateLoader getTemplateLoader()
    {
        return this.templateLoader;
    }

    //-----PRIVATE FUNCTIONS-----
}
