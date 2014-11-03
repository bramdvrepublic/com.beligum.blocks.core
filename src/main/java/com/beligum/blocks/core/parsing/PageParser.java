package com.beligum.blocks.core.parsing;

import com.beligum.blocks.core.caching.PageClassCache;
import com.beligum.blocks.core.config.BlocksConfig;
import com.beligum.blocks.core.config.CSSClasses;
import com.beligum.blocks.core.config.CacheConstants;
import com.beligum.blocks.core.exceptions.CacheException;
import com.beligum.blocks.core.exceptions.ParserException;
import com.beligum.blocks.core.identifiers.ElementID;
import com.beligum.blocks.core.identifiers.PageID;
import com.beligum.blocks.core.models.PageClass;
import com.beligum.blocks.core.models.storables.Block;
import com.beligum.blocks.core.models.storables.Page;
import com.beligum.blocks.core.models.storables.Row;
import com.beligum.core.framework.base.R;
import com.beligum.core.framework.templating.ifaces.Template;
import org.apache.commons.lang3.StringEscapeUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.*;
import org.jsoup.select.Elements;

import java.io.File;
import java.lang.reflect.Constructor;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.ParseException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Created by bas on 30.09.14.
 * Parser class for parsing pages
 */
public class PageParser extends CachableClassParser<PageClass>
{
    /**the template containing names of variables of the page currently being parsed*/
    private String pageTemplate;
    /**a set of all the blocks of the page currently being parsed*/
    private Set<Block> blocks;
    /**a set of all the rows of the page currently being parsed*/
    private Set<Row> rows;
    /**the doctype of the page currently being parsed*/
    private String docType;

    /**
     *  Default constructor
     */
    public PageParser()
    {
        this.pageTemplate = null;
        this.blocks = new HashSet<>();
        this.rows = new HashSet<>();
        this.docType = null;
    }


//    /**
//     * Parse the default template-file of the page-class and return a PageClass-object, filled with it's blocks, rows and the template of the page-class
//     * @param pageClassName the name of the page-class to be parsed (f.i. "default" for a pageClass filtered from the file "pages/default/index.html")
//     * @return a page-class parsed from the "pages/<page-class-name>/index.html"
//     * @throws com.beligum.blocks.core.exceptions.ParserException
//     */
//    @Override
//    public PageClass parseCachableClass(String pageClassName) throws ParserException
//    {
//        try{
//            String templateFilename = this.getTemplatePath(pageClassName);
//            File pageClassTemplate = new File(templateFilename);
//            //get the url used for identifying blocks and rows for this page-class
//            URL pageClassURL = PageClass.getBaseUrl(pageClassName);
//            //fill up this parser-class, with the elements and template filtered from the default html-file
//            String foundPageClassName = this.fillWithPageClass(pageClassTemplate, pageClassURL);
//            if(!foundPageClassName.equals(pageClassName)){
//                throw new ParserException("The name of the page-class (" + pageClassName + ") does not match the page-class-name found in the template: " + foundPageClassName);
//            }
//            return new PageClass(pageClassName, this.blocks, this.rows, this.pageTemplate, this.docType);
//        }
//        catch(ParserException e){
//            throw e;
//        }
//        catch(Exception e){
//            throw new ParserException("Error while parsing page-class '" + pageClassName + "' from template.", e);
//        }
//    }
//
//    /**
//     * Parse the default template-file of the page-class and return a PageClass-object, filled with it's blocks, rows and the template of the page-class.
//     * This method does the same as 'parseCachableClass(String pageClassName)', but is used as syntactic glue.
//     * @param pageClassName the name of the page-class to be parsed (f.i. "default" for a pageClass filtered from the file "pages/default/index.html")
//     * @return a page-class parsed from the "pages/<page-class-name>/index.html"
//     * @throws com.beligum.blocks.core.exceptions.ParserException
//     */
//    public PageClass parsePageClass(String pageClassName) throws ParserException
//    {
//        return this.parseCachableClass(pageClassName);
//    }

    /**
     *
     *
     * @param pageHtml a html-page to be parsed
     * @param pageUrl the url that will be used to id blocks and rows
     * @return a page-instance filled with the blocks and rows filtered from the url's htmlcontent
     */
    public Page parsePage(String pageHtml, URL pageUrl) throws ParserException

    {
        try{
            String pageClassName = this.fillWithPage(pageHtml, pageUrl);
            //return a page-instance with a newly versioned id and the found blocks and rows of class 'pageClass'
            return new Page(new PageID(pageUrl), this.blocks, this.rows, pageClassName);
        }
        catch(CacheException e){
            throw new ParserException("Error while getting page-class from cache. ", e);
        }
        catch(Exception e){
            throw new ParserException("Error while parsing page '" + pageUrl + "'.", e);
        }

    }

    /**
     * check whether the specified document is a page (it must have <body class='page page-classname'> present in the html-structure)
     * @param page page to be checked
     * @return the name of the page-class of the page that was checked
     * @throws com.beligum.blocks.core.exceptions.ParserException when the document isn't a correct 'page'
     */
    public String checkPage(Document page) throws ParserException
    {
        boolean isPage = false;
        boolean hasPageClass = false;
        String pageClassName = "";
        Iterator<String> it = page.body().classNames().iterator();
        while(it.hasNext() && (!isPage || !hasPageClass)){
            String className = it.next();
            if(!isPage) {
                isPage = className.equals(CSSClasses.PAGE);
            }
            if(!hasPageClass && className.startsWith(CSSClasses.PAGECLASS_PREFIX)){
                hasPageClass = true;
                pageClassName = className.substring(CSSClasses.PAGECLASS_PREFIX.length(), className.length());
            }
        }
        if(!isPage){
            throw new ParserException("Not a page, <body class='" + CSSClasses.PAGE +"'> could not be found at '" + page.location() + "'");
        }
        else if(!hasPageClass){
            throw new ParserException("Page has no page-class, <body class='" + CSSClasses.PAGE + " " + CSSClasses.PAGECLASS_PREFIX + "classname'> could not be found at '" + page.location() + "'");
        }
        else{
            return pageClassName;
        }
    }

    /**
     * Parses a html-file, containing a page-class, to blocks and rows containing variables and fills this parser up with the found content.
     * After the parse, a string containing the template of this cachable will be saved in the field 'cachableTemplate' and the found blocks and rows will be stored in the fields 'blocks' and 'rows
     *
     * @param cachableClassTemplate the file containing html of a cachable-class
     * @param baseUrl               the base-url which will be used to define the row- and block-ids if
     * @return the name of the cachable-class found in the template
     */
    @Override
    protected String fill(File cachableClassTemplate, URL baseUrl) throws ParserException
    {
        return this.fillWithPageClass(cachableClassTemplate, baseUrl);
    }


    /**
     * Parses a html-file, containing a page-class, to blocks and rows containing variables and fills this parser up with the found content.
     * After the parse, a string containing the template of this page will be saved in the field 'pageTemplate' and the found blocks and rows will be stored in the fields 'blocks' and 'rows
     * @param treeFile the file containing a html-tree of rows and blocks
     * @param baseUrl the base-url which will be used to define the row- and block-ids
     * @return the name of the page-class found in the template
     */
    private String fillWithPageClass(File treeFile, URL baseUrl) throws ParserException
    {
        this.empty();
        //first fill in all known variables, so we get a normal <html><head></head><body></body></html>-structure from the template
        Template template = R.templateEngine().getEmptyTemplate(treeFile.getAbsolutePath());
        String pageClassName = this.fill(template.render(), baseUrl);
        return pageClassName;
    }

    /**
     * Parses a html-string, containing a page-instance, to blocks and rows containing variables and fills this parser up with the found content.
     * After the parse, a string containing the template of this page will be saved in the field 'pageTemplate' and the found blocks and rows will be stored in the fields 'blocks' and 'rows
     * @param html a html-tree of rows and blocks
     * @param pageUrl the page-url which will be used to define the row- and block-ids
     * @return the name of the page-class of the specified page
     */
    private String fillWithPage(String html, URL pageUrl) throws ParserException
    {
        this.empty();
        String pageClassName = this.fill(html, pageUrl);
        return pageClassName;
    }


    /**
     * Parses a html-string, containing a html-tree of blocks and rows, to blocks and rows containing variables and fills this parser up with the found content.
     * After the parse, a string containing the tempalte of this page will be saved in the field 'pageTemplate' and the found blocks and rows will be stored in the fields 'blocks' and 'rows
     * @param htmlTree a html-tree of rows and blocks
     * @param baseUrl the base-url which will be used to define the row- and block-ids
     * @return the name of the page-class found in the html-tree
     */
    private String fill(String htmlTree, URL baseUrl) throws ParserException
    {
        //TODO: extend Parser to let Jsoup do the hard row- and block-searching
        //TODO BAS: ignore witespace between tags! -> makes block-comparison easier
        Document htmlDOM = Jsoup.parse(htmlTree, BlocksConfig.getSiteDomain());
        //throws errors if the htmlDOM is not a correct page
        String pageClassName = this.checkPage(htmlDOM);
        //if no document-type is present in the DOM, try to fetch the right one from the PageClassCache
        htmlDOM = this.typeDocument(htmlDOM, pageClassName);
        //fill up the rowset and the blockset and alter the htmlDOM to be a template holding variables for the upper-rows
        this.recursiveParse(htmlDOM, baseUrl, pageClassName);
        this.pageTemplate = StringEscapeUtils.unescapeXml(htmlDOM.outerHtml());
        return pageClassName;
    }

    /**
     * Empties this pageparser, so no residues of previous parsings will be lingering during parse.
     */
    private void empty(){
        this.pageTemplate = "";
        this.rows = new HashSet<>();
        this.blocks = new HashSet<>();
        this.docType = null;
    }

    /**
     * Parses the tree starting with the node-element, looking for row- and block-elements and adding them to the proper fields in this PageParser
     * Alters the node-element to holding variables instead of other elements.
     * @param node root of the tree to be parsed
     * @param baseUrl the base-url used which will be used to define the row- and block-ids
     * @param pageClassName the name of the page-class (of the page) being parsed
     * @return a set holding blocks and rows
     */
    private void recursiveParse(Element node, URL baseUrl, String pageClassName) throws ParserException
    {
        Elements children = node.children();
        for(Element child : children){
            //recursively iterate over the subtree starting with this child and add the found blocks and rows to the map
            recursiveParse(child, baseUrl, pageClassName);
            boolean isRow = child.classNames().contains(CSSClasses.ROW);
            boolean isBlock = child.classNames().contains(CSSClasses.BLOCK);
            if(isRow || isBlock){
                if(child.id() != null && !child.id().isEmpty()) {
                    //TODO BAS: is this the most efficient way we can get rid of the &quot;-problem during return-velocity-parsing, since this will read over the whole content again
                    String childHtml = StringEscapeUtils.unescapeXml(child.outerHtml());
                    //render id for this element (row or block)
                    ElementID id = null;
                    try {
                        URI temp = baseUrl.toURI().resolve("#" + child.id());
                        URL childUrl = temp.toURL();
                        id = new ElementID(childUrl);
                    }catch(MalformedURLException e){
                        throw new ParserException("Base-url doesn't seem to be correct. Cannot construct proper IDs with this page-url: " + baseUrl, e);
                    }catch(URISyntaxException e){
                        throw new ParserException("Base-url doesn't seem to be correct. Cannot construct proper IDs with this page-url: " + baseUrl, e);
                    }
                    if(isRow){
                        boolean isFinal =  !(child.classNames().contains(CSSClasses.MODIFIABLE_ROW) || child.classNames().contains(CSSClasses.LAYOUTABLE_ROW) || child.classNames().contains(
                                        CSSClasses.CREATE_ENABLED_ROW));
                        this.rows.add(new Row(id, childHtml, pageClassName, isFinal));
                    }
                    else{
                        boolean isFinal =  !(child.classNames().contains(CSSClasses.EDITABLE_BLOCK));
                        this.blocks.add(new Block(id, childHtml, pageClassName, isFinal));
                    }
                    child.replaceWith(new TextNode("\n ${" + child.id() + "}\n", ""));
                }
                else{
                    //if no id i
                    throw new ParserException("A row- or block-element in the html-tree doesn't have an id, this shouldn't happen: \n" + child.outerHtml());
                }
            }
            else{
                //do nothing (skip ahead)
            }
        }
    }

    /**
     * Save the documenttype, if it can be found, to the field 'docType', since it has been lost when the client sent the html to the server (without the document-type attached)
     * If no document-type can be found, it is fetched from the specified page-class.
     * @param htmlDOM the document to be checked for a document-type
     * @param pageClassName fall-back document-type for when no document type can be found in the document itself
     * @return the same document with document-type attached, if one could be found in the
     */
    private Document typeDocument(Document htmlDOM, String pageClassName) throws ParserException
    {
        try {
            List<Node> nodes = htmlDOM.childNodes();

            Iterator<Node> it = nodes.iterator();
            boolean hasDocumentType = false;
            while (!hasDocumentType && it.hasNext()) {
                Node node = it.next();
                if (node instanceof DocumentType) {
                    this.docType = node.toString();
                    hasDocumentType = true;
                }
            }
            if (!hasDocumentType) {
                this.docType = PageClassCache.getInstance().getCache().get(pageClassName).getDocType();
                if (this.docType != null) {
                    htmlDOM.prepend(this.docType);
                }
                else {
                    //no doctype could be found in the page-class-cache, so do nothing
                }
            }
            return htmlDOM;
        }
        catch(CacheException e){
            throw new ParserException("No document-type found for html document '" + htmlDOM.title() + "' of page-class '" + pageClassName + "'. Tried to fetch it from the cache, but caught exception.", e);
        }
    }

    /**
     *
     * @return get the page-class with which this parser is filled up
     */
    protected PageClass getInternalCachableClass() throws ParserException
    {
        if(!this.isFilled()){
            throw new ParserException("Cannot construct a page-class out of the internal parser-data. Did you fill up the parser before calling this method?");
        }
        try {
            return new PageClass(this.cachableClassName, this.blocks, this.rows, this.pageTemplate, this.docType);
        }
        catch(URISyntaxException e){
            throw new ParserException("Cannot construct a page-class out of the internal parser-data.", e);
        }
    }

    /**
     * returns the path to the html-template of a page-class
     * @param pageClassName name of the page-class
     */
    protected String getTemplatePath(String pageClassName){
        return BlocksConfig.getTemplateFolder() + "/" + BlocksConfig.PAGES_FOLDER + "/" + pageClassName + "/" + BlocksConfig.INDEX_FILE_NAME;
    }

    /**
     * returns the base-url for the page-class
     *
     * @param pageClassName the name of the page-class (f.i. "default" for a pageClass filtered from the file "pages/default/index.html")
     * @return
     */
    @Override
    public URL getBaseUrl(String pageClassName) throws MalformedURLException
    {
        return new URL(BlocksConfig.getSiteDomain() + "/" + CacheConstants.PAGE_CLASS_ID_PREFIX + "/" + pageClassName);
    }

    /**
     * @return the prefix used for a page-class in the class-attribute of the html-template (i.e. "page-")
     */
    @Override
    public String getCssClassPrefix()
    {
        return CacheConstants.PAGE_CLASS_ID_PREFIX;
    }

}
