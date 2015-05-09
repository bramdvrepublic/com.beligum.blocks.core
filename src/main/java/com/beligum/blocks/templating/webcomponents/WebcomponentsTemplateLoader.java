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

import com.beligum.base.resources.ResourceSearchResult;
import com.beligum.base.server.R;
import com.beligum.base.templating.ifaces.Template;
import com.beligum.base.utils.Logger;
import com.beligum.blocks.caching.CacheKeys;
import com.beligum.blocks.templating.webcomponents.html5.HtmlCodeFactory;
import com.beligum.blocks.templating.webcomponents.html5.ifaces.Html;
import com.beligum.blocks.templating.webcomponents.html5.ifaces.HtmlImportTemplate;
import org.apache.commons.io.Charsets;
import org.apache.commons.io.IOUtils;

import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by bram on 7/26/14.
 */
public class WebcomponentsTemplateLoader
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    private WebcomponentsTemplateEngine templateEngine;

    //-----CONSTRUCTORS-----
    public WebcomponentsTemplateLoader(WebcomponentsTemplateEngine templateEngine) throws Exception
    {
        this.templateEngine = templateEngine;
    }

    //-----PUBLIC METHODS-----
    public WebcomponentsTemplateCache getCachedTemplates()
    {
        WebcomponentsTemplateCache retVal = (WebcomponentsTemplateCache) R.cacheManager().getApplicationCache().get(CacheKeys.WEBCOMPONENT_TEMPLATES);
        if (retVal == null) {
            R.cacheManager().getApplicationCache().put(CacheKeys.WEBCOMPONENT_TEMPLATES, retVal = new WebcomponentsTemplateCache(this.templateEngine));
            try {
                searchAllTemplates(retVal);
            }
            catch (Exception e) {
                Logger.error("Caught exception while searching for all the webcomponent templates in the current classpath; this is bad and needs to fixed", e);
            }
        }

        return retVal;
    }
    public String getTemplateTagsCsv()
    {
        String retVal = (String) R.cacheManager().getApplicationCache().get(CacheKeys.WEBCOMPONENT_TEMPLATES_CSV);
        if (retVal == null) {
            Map<String, HtmlImportTemplate> cachedTemplates = getCachedTemplates();
            StringBuilder sb = new StringBuilder();

            for (String tag : cachedTemplates.keySet()) {
                if (sb.length() == 0) {
                    sb.append(tag);
                }
                else {
                    sb.append(", ").append(tag);
                }
            }

            R.cacheManager().getApplicationCache().put(CacheKeys.WEBCOMPONENT_TEMPLATES_CSV, retVal = sb.toString());
        }

        return retVal;
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----
    private void searchAllTemplates(WebcomponentsTemplateCache templateCache) throws Exception
    {
        //start with a clean slate
        templateCache.clear();

        //TODO clean this up
        List<ResourceSearchResult> htmlFiles = new ArrayList<>();
        htmlFiles.addAll(R.resourceLoader().searchResourceGlob("/templates/**.{html,htm}"));
        htmlFiles.addAll(R.resourceLoader().searchResourceGlob("/views/**.{html,htm}"));
        htmlFiles.addAll(R.resourceLoader().searchResourceGlob("/assets/imports/**.{html,htm}"));
        htmlFiles.addAll(R.resourceLoader().searchResourceGlob("/imports/**.{html,htm}"));

        for (ResourceSearchResult htmlFile : htmlFiles) {
            // Note: this code re-occurs in JsoupHtmlImportTemplate.checkReload(), so check there if you modify important things!
            try (Reader reader = Files.newBufferedReader(htmlFile.getResource(), Charset.forName(Charsets.UTF_8.name()))) {
                //TODO what about the language? Will it matter?
                Template wrappedTemplate = this.templateEngine.getWrappedTemplateEngine().getNewStringTemplate(IOUtils.toString(reader));

                // I hope this is the right way to go: first renderContent out the file, allowing for a possible low-level template engine to do it's work,
                // and then analyze the more high-level html for special meaning
                Html html = HtmlCodeFactory.create(wrappedTemplate.render(), htmlFile);
                if (html instanceof HtmlImportTemplate) {
                    HtmlImportTemplate template = (HtmlImportTemplate) html;
                    templateCache.put(template.getName(), template);
                }
            }
        }
    }
}
