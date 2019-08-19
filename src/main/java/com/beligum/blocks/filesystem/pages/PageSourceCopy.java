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

package com.beligum.blocks.filesystem.pages;

import com.beligum.base.resources.ifaces.Source;
import com.beligum.base.utils.Logger;
import com.beligum.blocks.config.Settings;
import com.beligum.blocks.rdf.RdfFactory;
import com.beligum.blocks.rdf.ifaces.RdfClass;
import com.beligum.blocks.rdf.ontologies.Meta;
import com.beligum.blocks.templating.HtmlParser;
import com.beligum.blocks.templating.HtmlTemplate;
import com.beligum.blocks.utils.RdfTools;
import org.apache.commons.lang.StringUtils;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Iterator;

/**
 * Created by bram on 1/8/17.
 */
public class PageSourceCopy extends PageSource
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    private final boolean linkToSource;

    //-----CONSTRUCTORS-----
    public PageSourceCopy(Source source, boolean linkToSource) throws IOException
    {
        super(source.getUri());

        this.linkToSource = linkToSource;

        try (InputStream is = source.newInputStream()) {
            this.parseHtml(is);
        }
    }

    //-----PUBLIC METHODS-----

    //-----PROTECTED METHODS-----
    @Override
    protected void parseHtml(InputStream source) throws IOException
    {
        super.parseHtml(source);

        //We'll remove all language attributes because 9/10 we're creating a copy from another language
        //note that it will be added again on save
        this.language = null;
        this.htmlTag.removeAttr(HTML_ROOT_LANG_ATTR);

        Logger.info("BLAH");
        Iterator<Element> iter = this.metaTags.iterator();
        while (iter.hasNext()) {
            Element metaTag = iter.next();

            // we'll only delete meta properties (eg. not "<meta charset="UTF-8" />" or "<meta name="viewport" content="width=device-width, initial-scale=1" />")
            String propertyValue = HtmlTemplate.getPropertyAttribute(metaTag);
            if (propertyValue != null && (propertyValue.startsWith(Meta.NAMESPACE.getPrefix()) || propertyValue.startsWith(Meta.NAMESPACE.getUri().toString()))) {
                metaTag.remove();
                iter.remove();
            }
        }

        //if we need to break the link with the source (this means, it won't have the "about" attribute in common),
        //we need to replace the about attribute with a newly generated URI. If we need to keep the link,
        //nothing needs to be done because this is the default situation (keeping the @about intact).
        if (!this.linkToSource) {
            String aboutUri = this.htmlTag.attr(HTML_ROOT_SUBJECT_ATTR);
            if (StringUtils.isEmpty(aboutUri)) {
                throw new IOException("Encountered empty html @"+HTML_ROOT_SUBJECT_ATTR+" attribute while copying a page source; " + this.document);
            }
            else {
                //since we're making a copy of a page, it makes sense to use the class of that page
                String typeOfStr = this.htmlTag.attr(HTML_ROOT_TYPEOF_ATTR);
                RdfClass typeOf = StringUtils.isEmpty(typeOfStr) ? Settings.DEFAULT_CLASS : RdfFactory.getClass(typeOfStr);
                if (typeOf == null) {
                    throw new IOException("Unable to parse the html @typeof attribute to a valid RDF class; " + this.document);
                }
                else {
                    // remove the resource attribute; a new one will be generated (uniformly) on first save
                    // for details, see https://github.com/republic-of-reinvention/com.stralo.framework/issues/52
                    this.htmlTag.removeAttr(HTML_ROOT_SUBJECT_ATTR);
                }
            }
        }
    }

    //-----PRIVATE METHODS-----

}
