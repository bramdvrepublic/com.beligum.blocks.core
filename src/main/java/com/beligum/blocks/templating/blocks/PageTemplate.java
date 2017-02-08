package com.beligum.blocks.templating.blocks;

import net.htmlparser.jericho.Attributes;
import net.htmlparser.jericho.Element;
import net.htmlparser.jericho.OutputDocument;
import net.htmlparser.jericho.Source;

import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by bram on 5/13/15.
 */
public class PageTemplate extends HtmlTemplate
{
    //-----CONSTANTS-----

    //-----VARIABLES-----

    //-----CONSTRUCTORS-----
    protected PageTemplate(String templateName, Source document, Path absolutePath, Path relativePath, HtmlTemplate parent) throws Exception
    {
        super(templateName, document, absolutePath, relativePath, parent);

        //Not yet activated...
//        //two default RDF attributes are set during a page save: vocab and prefix,
//        //so push those two on the stacks if we encounter a page template, cause any page
//        //that's created (and eventually saved), will have them too
//        if (this.rdfVocab == null) {
//            this.rdfVocab = HtmlTemplate.getDefaultRdfVocab();
//        }
//        if (this.rdfPrefixes.isEmpty()) {
//            this.rdfPrefixes.putAll(HtmlTemplate.getDefaultRdfPrefixes());
//        }
    }

    //-----PUBLIC STATIC METHODS-----

    //-----PUBLIC METHODS-----
    @Override
    public boolean renderTemplateTag()
    {
        return false;
    }

    //-----PROTECTED METHODS-----
    @Override
    protected OutputDocument doInitHtmlPreparsing(OutputDocument document, HtmlTemplate superTemplate) throws IOException
    {
        // note that it doesn't really make sense to do something with the parent here;
        // the html of a page always needs to be there and I don't know a reason why we would use the html of a possible parent

        // some extra preprocessing is fill in the template attribute with the name of the template
        // so we know which template was used when the code comes back from the client
        Element html = document.getSegment().getFirstElement(HtmlParser.HTML_ROOT_TEMPLATE_ATTR, null);
        if (!html.getName().equalsIgnoreCase(HtmlParser.HTML_ROOT_ELEM)) {
            throw new IOException("Found a template attribute on a non-html element, this shouldn't happen since it's been checked before; " + relativePath);
        }

        // fill in the template attribute's value
        // a little bit verbose, but I didn't find a shorter way...
        Attributes templateAttr = html.getAttributes();
        Map<String, String> attrs = new LinkedHashMap<>();
        templateAttr.populateMap(attrs, true);
        attrs.put(HtmlParser.HTML_ROOT_TEMPLATE_ATTR, this.getTemplateName());

        //we updated the approach: instead of copying one single argument to the <html> tag,
        // we will translate the "<html template>" tag (with it's 'template' placeholder, indicating it's a template)
        // to this: <html $!{HTML_TAG_ARGS}> (NOTE THAT THE SPACE IN BETWEEN html and $ is mandatory for parsing reasons!)
        // and set the HTML_TAG_ARGS variable while instantiating a page template, so we support all the arguments of the page template instance
        //document.replace(templateAttr, Attributes.generateHTML(attrs));
        document.replace(templateAttr, " $!{" + HtmlParser.HTML_ROOT_ARGS_VARIABLE_NAME + "}");

        this.attributes = attrs;

        //we didn't change the html structure, so we can return it
        return document;
    }
    @Override
    protected void saveHtml(OutputDocument document, HtmlTemplate superTemplate)
    {
        this.prefixHtml = new Source("");
        this.innerHtml = new Source(document.toString());
        this.suffixHtml = new Source("");
    }

    //-----PRIVATE METHODS-----
}
