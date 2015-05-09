package com.beligum.blocks.templating.webcomponents.html5;

import com.beligum.base.resources.ResourceSearchResult;
import com.beligum.base.templating.ifaces.Template;
import com.beligum.base.utils.Logger;
import com.beligum.blocks.templating.webcomponents.WebcomponentsTemplateEngine;
import com.beligum.blocks.templating.webcomponents.html5.ifaces.*;
import org.apache.commons.io.Charsets;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;

/**
 * This class more or less implements "HTML Imports".
 * Eg. a file that can be imported using <code><link rel="import" href="/path/to/imports/file.html"></code>
 * See http://www.html5rocks.com/en/tutorials/webcomponents/imports/ for details.
 * <p/>
 * Created by bram on 5/7/15.
 */
public class JsoupHtmlImportTemplate extends JsoupHtmlSnippet implements HtmlImportTemplate
{
    //-----CONSTANTS-----
    /**
     * These are the names of first folders that won't be taken into account when building the name of the element
     * Eg. /imports/blocks/test/tag.html will have the name "blocks-test-tag"
     */
    private String[] INVISIBLE_START_FOLDERS = {"import", "imports"};

    //-----VARIABLES-----
    private Path absolutePath;
    private Long lastModified;
    private Path relativePath;
    private String name;
    private Element templateElement;

    //-----CONSTRUCTORS-----
    public JsoupHtmlImportTemplate(Document htmlDocument) throws ParseException, IOException
    {
        this(htmlDocument, null);
    }
    public JsoupHtmlImportTemplate(Document htmlDocument, ResourceSearchResult resource) throws ParseException, IOException
    {
        super(htmlDocument);

        this.absolutePath = resource.getResource();
        this.relativePath = Paths.get("/").resolve(resource.getResourceFolder().relativize(resource.getResource()));

        Path namePath = this.relativePath;
        if (this.relativePath!=null) {
            for (String invisiblePrefix : INVISIBLE_START_FOLDERS) {
                if (namePath.startsWith(invisiblePrefix) || namePath.startsWith(namePath.getFileSystem().getSeparator() + invisiblePrefix)) {
                    namePath = namePath.subpath(1, namePath.getNameCount());
                    //this is a safe choice that might change in the future: do we want to keep eating first folders? Of so, then we should actually start over, no?
                    break;
                }
            }
            this.name = StringUtils.strip(namePath.toString().replaceAll("/", "-"), "-");
            int lastDot = this.name.lastIndexOf(".");
            if (lastDot >= 0) {
                this.name = this.name.substring(0, lastDot);
            }
            //note: we may want to let the user override the name with an id attribute on the <template> tag

            // In Web Components speak, this new element is a Custom Element,
            // and the only two requirements are that its name must contain a dash,
            // and its prototype must extend HTMLElement.
            // See https://css-tricks.com/modular-future-web-components/
            if (!this.name.contains("-")) {
                throw new ParseException("The name of an import template should always contain at least one dash; '" + this.name + "' in " + relativePath, 0);
            }
        }
        else {
            this.name = null;
        }

        this.parseNewData();
    }

    //-----PUBLIC METHODS-----
    public static boolean representsHtmlImportTemplate(Document document)
    {
        return !document.select("template").isEmpty();
    }
    @Override
    public Iterable<HtmlScriptElement> getScripts()
    {
        return new WrappedJsoupIterator<Element, HtmlScriptElement>(this.jsoupDocument.select("script").iterator())
        {
            @Override
            public HtmlScriptElement wrapNext(Element element)
            {
                return new JsoupHtmlScriptElement(element);
            }
        };
    }
    @Override
    public Iterable<HtmlStyleElement> getStyles()
    {
        return new WrappedJsoupIterator<Element, HtmlStyleElement>(this.jsoupDocument.select("link[rel=stylesheet],style").iterator())
        {
            @Override
            public HtmlStyleElement wrapNext(Element element)
            {
                return new JsoupHtmlStyleElement(element);
            }
        };
    }
    /**
     * This returns the name of the file, without the file extension, where parent directories are represented by dashes.
     * Eg. /blocks/test/tag.html will have the name "blocks-test-tag" and result in tags like <blocks-test-tag></blocks-test-tag>
     * Please note that the HTML spec forces you to include at lease one dash in custom tag names to ensure their future tag names.
     * @return
     */
    @Override
    public String getName()
    {
        return this.name;
    }
    @Override
    public JsoupHtmlElement renderContent(HtmlElement dataWrapper)
    {
        //since this is a Jsoup implementation, we can safely assume the other one is too, making our API life a little bit easier
        Element data = ((Element) dataWrapper.getDelegateObject()).clone();

        // JSoup is a little bit weird when it comes to manipulating html data
        // to keep things readable, we'll "start from scratch" and re-build
        // the template as much as possible instead of manipulating the existing DOM
        // it's easy to start with the body of the template because that's where everything will get copied to
        // but the node we're rendering is actually the data, so we use the name and attributes of the data, but the body of the template
        Element retVal = data.clone();
        retVal.html(this.templateElement.html());

        // If the selector is empty, all "left over" html should be used, we'll do that later on
        Elements contentTags = retVal.select("content[select]");
        for (Element contentTag : contentTags) {
            // A comma-separated list of selectors. These have the same syntax as CSS selectors.
            // They select the content to insert in place of the <content> element.
            String selector = contentTag.attr("select");
            if (!StringUtils.isEmpty(selector)) {
                // now we need to apply the selector to the instance data and put the result inside the <content> element
                Elements matches = data.select(selector);
                //note: this will clear the existing html first (which is what we're expecting)
                contentTag.html(matches.outerHtml());
                //remove the <content> tag and only keep it's contents
                contentTag.unwrap();
                //remove the matches to be able to calcualte the "left-over-html"
                matches.remove();
            }
            else {
                Logger.warn("Encountered empty select attribute on <content> tag in "+this.relativePath+"; ignoring this tag!");
            }
        }

        // we've replaced all <content> tags with a select attribute, now search for content tags without select
        // and replace it with the left-over-html
        contentTags = retVal.select("content");
        if (!contentTags.isEmpty()) {
            Element firstContent = contentTags.get(0);
            firstContent.html(data.html());
            firstContent.unwrap();

            if (contentTags.size()>1) {
                Logger.warn("Encountered multiple <content> tags withtout select attribute in template "+this.relativePath+"; only replacing first!");
                for (int i=1;i<contentTags.size();i++) {
                    contentTags.get(i).remove();
                }
            }
        }

        return new JsoupHtmlElement(retVal);
    }
    @Override
    public boolean checkReload(WebcomponentsTemplateEngine engine) throws Exception
    {
        boolean retVal = false;

        long lastMod = this.calcLastModified();
        if (this.lastModified < lastMod) {
            try (Reader reader = Files.newBufferedReader(this.absolutePath, Charset.forName(Charsets.UTF_8.name()))) {
                Template wrappedTemplate = engine.getWrappedTemplateEngine().getNewStringTemplate(IOUtils.toString(reader));
                this.jsoupDocument = HtmlCodeFactory.parseHtml(wrappedTemplate.render());
            }

            this.parseNewData();

            retVal = true;
        }

        return retVal;
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----
    private long calcLastModified() throws IOException
    {
        return Files.getLastModifiedTime(this.absolutePath).toMillis();
    }
    private void parseNewData() throws IOException, ParseException
    {
        this.lastModified = this.calcLastModified();

        Elements elements = this.jsoupDocument.select("template");
        if (elements.isEmpty()) {
            throw new ParseException("An import template should always contain a <template> tag, couldn't find it; '" + this.name + "' in " + relativePath, 0);
        }
        else if (elements.size()>1) {
            throw new ParseException("An import template should at max have only one <template> tag, found "+elements.size()+"; '" + this.name + "' in " + relativePath, 0);
        }
        else {
            this.templateElement = elements.first();
        }
    }
}
