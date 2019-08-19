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
public interface TemplateController
{
    /**
     * Internal method to set the config values, no need to use this method directly
     */
    TemplateConfig putConfig(String key, String value);

    /**
     * Internal method to clear the config values, no need to use this method directly
     */
    TemplateController resetConfig();

    /**
     * This method is called every time the controller is initialized for a template created (you can safely assume the putConfig has been set here)
     */
    void created();

    /**
     * This method is called just before a page is saved.
     * If you want to change anything to the content of the html beforehand, this is the time.
     * Make sure you do your own replacements to the output document.
     * Note that we supply a Jericho element (instead of a JSoup element) to make low-level edits possible.
     */
    void prepareForSave(Source source, Element element, OutputDocument htmlOutput) throws IOException;

    /**
     * This method is called just before a copy is created from the supplied source element to the target uri and language.
     * If you want to change anything to the content of the html before a copy is made, this is the time.
     * Make sure you do your own replacements to the output document.
     * Note that we supply a Jericho element (instead of a JSoup element) to make low-level edits possible.
     */
    void prepareForCopy(Source source, Element element, OutputDocument htmlOutput, URI targetUri, Locale targetLanguage) throws IOException;
}
