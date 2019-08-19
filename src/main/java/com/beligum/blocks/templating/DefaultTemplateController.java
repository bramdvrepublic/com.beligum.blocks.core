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

package com.beligum.blocks.templating;

import com.beligum.base.resources.ifaces.Source;
import net.htmlparser.jericho.Element;
import net.htmlparser.jericho.OutputDocument;

import java.io.IOException;
import java.net.URI;
import java.util.Locale;

/**
 * Created by bram on 5/27/15.
 */
public abstract class DefaultTemplateController implements TemplateController
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    protected TemplateConfig config = new TemplateConfig();

    //-----CONSTRUCTORS-----

    //-----PUBLIC METHODS-----
    @Override
    public TemplateConfig putConfig(String key, String value)
    {
        this.config.put(key, value);

        return this.config;
    }
    @Override
    public TemplateController resetConfig()
    {
        this.config.clear();

        return this;
    }
    @Override
    public void prepareForSave(Source source, Element element, OutputDocument htmlOutput) throws IOException
    {
        //NOOP, override in subclass if you want to do something special
    }
    @Override
    public void prepareForCopy(Source source, Element element, OutputDocument htmlOutput, URI targetUri, Locale targetLanguage) throws IOException
    {
        //NOOP, override in subclass if you want to do something special
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}
