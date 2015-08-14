package com.beligum.blocks.templating.blocks;

import net.htmlparser.jericho.Element;
import net.htmlparser.jericho.OutputDocument;
import net.htmlparser.jericho.Segment;
import net.htmlparser.jericho.Source;

import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by bram on 5/13/15.
 */
public class TagTemplate extends HtmlTemplate
{
    //-----CONSTANTS-----

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

    //-----PROTECTED METHODS-----
    @Override
    protected OutputDocument doInitHtmlPreparsing(OutputDocument document, HtmlTemplate parent) throws IOException
    {
        List<Element> templateElements = document.getSegment().getAllElements("template");

        // Note that there always needs to be a <template> tag to indicate this is a template
        if (templateElements != null && !templateElements.isEmpty()) {
            if (templateElements.size() > 1) {
                throw new IOException("Encountered tag template with more than one <template> tags ("+templateElements.size()+"); " + absolutePath);
            }

            Element templateTag = templateElements.get(0);

            //FIRST, parse the attributes
            //we'll merge the attributes of the <template> tag from the parent, but let them be overridden by this child template
            Map<String, String> attrs = new LinkedHashMap<>();
            if (parent!=null && parent.getAttributes()!=null) {
                attrs = parent.getAttributes();
            }
            if (templateTag.getAttributes()!=null) {
                //note that populate just does a put() so this works well to override the parent properties
                templateTag.getAttributes().populateMap(attrs, true);
            }
            this.setAttributes(attrs);
        }
        else {
            throw new IOException("Encountered tag template with an invalid <template> tag config (found " + (templateElements == null ? null : templateElements.size()) + " tags); " + absolutePath);
        }

        return document;
    }
    @Override
    protected Segment unwrapHtml(OutputDocument document, HtmlTemplate parent)
    {
        Segment retVal = null;

        //first, we need to render it out, because we've changed it a lot, and any query
        // on the non-rendered version will be wrong (querying the original)
        Segment documentSource = new Source(document.toString());

        //note that we already checked there's exactly one <template> tag
        Element templateTag = documentSource.getFirstElement("template");

        //this checks if the template tag is there pro-forma (just to make it a template file)
        //if the <template> tag is empty and we have a parent, inherit the content from the parent
        //note that .isEmpty() also returns true when the element has attributes
        if (templateTag.isEmpty() && parent!=null) {
            retVal = parent.getHtml();
        }
        else {
            //now we want to unwrap the <template> tag
            // note that it's not enough to return the content of that tag,
            // because we want to keep the wrapped resources outside the template too...
            OutputDocument htmlDoc = new OutputDocument(documentSource);
            htmlDoc.replace(templateTag, templateTag.getContent());

            //and now, wrap it in a source again...
            retVal = new Source(htmlDoc.toString());
        }

        return retVal;
    }

    //-----PRIVATE METHODS-----
}
