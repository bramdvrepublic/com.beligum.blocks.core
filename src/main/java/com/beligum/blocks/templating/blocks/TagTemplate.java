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

package com.beligum.blocks.templating.blocks;

import net.htmlparser.jericho.Element;
import net.htmlparser.jericho.OutputDocument;
import net.htmlparser.jericho.Segment;
import net.htmlparser.jericho.Source;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

/**
 * Created by bram on 5/13/15.
 */
public class TagTemplate extends HtmlTemplate
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    protected Set<PageTemplate> disabledPages;

    //-----CONSTRUCTORS-----
    protected TagTemplate(String templateName, Source document, Path absolutePath, Path relativePath, HtmlTemplate parent) throws Exception
    {
        this.init(templateName, document, absolutePath, relativePath, parent);
    }

    //-----PUBLIC METHODS-----
    @Override
    public boolean renderTemplateTag()
    {
        return true;
    }
    public boolean isDisabledInContext(PageTemplate pageTemplate)
    {
        return this.disabledPages.contains(pageTemplate);
    }

    //-----PROTECTED METHODS-----
    @Override
    protected void init(String templateName, Source source, Path absolutePath, Path relativePath, HtmlTemplate superTemplate) throws Exception
    {
        super.init(templateName, source, absolutePath, relativePath, superTemplate);

        //by default, this template is not disabled inside any page template context,
        //but if this method is called again and there were some contexts disabled, we probably
        //want to keep them disabled, so only initialize the set if it's null
        if (this.disabledPages == null) {
            this.disabledPages = new LinkedHashSet<>();
        }
    }
    @Override
    protected OutputDocument doInitHtmlPreparsing(OutputDocument document, HtmlTemplate superTemplate) throws IOException
    {
        List<Element> templateElements = document.getSegment().getAllElements(HtmlParser.WEBCOMPONENTS_TEMPLATE_ELEM);

        // Note that there always needs to be a <template> tag to indicate this is a template
        if (templateElements != null && !templateElements.isEmpty()) {
            if (templateElements.size() > 1) {
                throw new IOException("Encountered tag template with more than one <" + HtmlParser.WEBCOMPONENTS_TEMPLATE_ELEM + "> tags (" + templateElements.size() + "); " + absolutePath);
            }
            else {
                //save it for future use
                this.rootElement = templateElements.get(0);
            }

            //FIRST, parse the attributes
            //we'll merge the attributes of the <template> tag from the parent, but let them be overridden by this child template
            Map<String, String> attrs = new LinkedHashMap<>();
            if (superTemplate != null && superTemplate.getAttributes() != null) {
                attrs = superTemplate.getAttributes();
            }
            if (this.rootElement.getAttributes() != null) {
                //note that populate just does a put() so this works well to override the parent properties
                this.rootElement.getAttributes().populateMap(attrs, true);
            }
            this.attributes = attrs;
        }
        else {
            throw new IOException("Encountered tag template with an invalid <" + HtmlParser.WEBCOMPONENTS_TEMPLATE_ELEM + "> tag config (found " +
                                  (templateElements == null ? null : templateElements.size()) + " tags); " + absolutePath);
        }

        return document;
    }
    @Override
    protected void saveHtml(OutputDocument document, HtmlTemplate superTemplate)
    {
        //first, we need to render it out, because we've changed it a lot, and any query
        // on the non-rendered version will be wrong (querying the original)
        Segment documentSource = new Source(document.toString());

        //note that we already checked there's exactly one <template> tag
        Element templateTag = documentSource.getFirstElement(HtmlParser.WEBCOMPONENTS_TEMPLATE_ELEM);

        //now we want to unwrap the <template> tag
        // note that it's not enough to return the content of that tag,
        // because we want to keep the wrapped resources outside the template too...

        //Note: don't append 'superTemplate.getPrefixHtml()' because it's already there, we copied it in (see HtmlTemplate.init())
        StringBuilder prefix = new StringBuilder();
        prefix.append(documentSource.subSequence(0, templateTag.getBegin()));
        this.prefixHtml = new Source(prefix);

        //same for the suffix but in reverse order
        //Note: I don't really know if we can call suffix.append(superTemplate.getSuffixHtml()) here,
        // because I think this is never used, so I can't really doIsValid, but as far as I know, we never (as with the prefix)
        // mess with the prefix html by hand, so I think it's safe to call it.
        StringBuilder suffix = new StringBuilder();
        suffix.append(documentSource.subSequence(templateTag.getEnd(), documentSource.length()));
        if (superTemplate != null) {
            suffix.append(superTemplate.getSuffixHtml());
        }
        this.suffixHtml = new Source(suffix);

        //this checks if the template tag is there pro-forma (just to make it a template file)
        //if the <template> tag is empty and we have a parent, inherit the content from the parent <template> tag
        //this allows us to really overload the <template> tag and start over
        //note that .isEmpty() also returns true when the element has attributes
        if (templateTag.isEmpty() && superTemplate != null) {
            this.innerHtml = superTemplate.getInnerHtml();
        }
        else {
            //we unwrap the template tag (already saved the attributes)
            this.innerHtml = templateTag.getContent();
        }
    }
    protected void addDisabledContext(PageTemplate pageTemplate)
    {
        this.disabledPages.add(pageTemplate);
    }

    //-----PRIVATE METHODS-----
}
