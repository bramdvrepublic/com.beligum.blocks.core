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

import com.beligum.base.templating.AbstractTemplateContext;
import com.beligum.base.templating.ifaces.Template;
import com.beligum.base.templating.ifaces.TemplateContext;
import com.beligum.base.validation.messages.FeedbackMessage;

import java.util.HashMap;
import java.util.Map;

public class WebcomponentsTemplateContext extends AbstractTemplateContext
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    private HashMap<String, Object> dataModel;

    //-----CONSTRUCTORS-----
    public WebcomponentsTemplateContext(Template template)
    {
        super(template);

        this.dataModel = new HashMap<>();

        this.initGlobals();
    }

    //-----PUBLIC FUNCTIONS-----
    @Override
    public TemplateContext set(String name, Object object)
    {
        this.dataModel.put(name, object);

        return this;
    }
    @Override
    public Object get(String name)
    {
        return this.dataModel.get(name);
    }
    @Override
    public TemplateContext addMessage(FeedbackMessage feedbackMessage)
    {
        this.addMessage(dataModel, feedbackMessage);

        return this;
    }
    public HashMap<String, Object> getDataModel()
    {
        return this.dataModel;
    }

    //-----PROTECTED FUNCTIONS-----
    @Override
    protected void addAll(Map<String, Object> values)
    {
        this.dataModel.putAll(values);
    }

    //-----PRIVATE FUNCTIONS-----
}
