package com.beligum.blocks.core.parsing;

import com.beligum.blocks.core.caching.PageClassCache;
import com.beligum.blocks.core.config.BlocksConfig;
import com.beligum.blocks.core.exceptions.ElementException;
import com.beligum.blocks.core.exceptions.PageClassCacheException;
import com.beligum.blocks.core.exceptions.PageParserException;
import com.beligum.blocks.core.identifiers.ElementID;
import com.beligum.blocks.core.identifiers.PageID;
import com.beligum.blocks.core.identifiers.RedisID;
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
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Created by bas on 30.09.14.
 * Parser class for parsing pages
 */
public class PageParser
{

    /**
     * The css-class indicating that a certain <body>-tag is a page.
     * (f.i. <body class="page">)
     */
    public final static String CSS_CLASS_FOR_PAGE = "page";
    /**
     * The prefix which a css-class indicating that a certain <body>-tag is of a certain page-class must have to be recognized as such.
     * (f.i. 'page-default' has the prefix 'page-' added to it's page-class name 'default')
     */
    public final static String CSS_CLASS_FOR_PAGECLASS_PREFIX = "page-";

    public final static String CSS_CLASS_FOR_ROW = "row";
    public final static String CSS_CLASS_FOR_BLOCK = "block";

    public final static String CSS_CLASS_FOR_MODIFIABLE_ROW = "can-modify";
    public final static String CSS_CLASS_FOR_LAYOUTABLE_ROW = "can-layout";
    public final static String CSS_CLASS_FOR_CREATE_ENABLED_ROW = "can-create";
    public final static String CSS_CLASS_FOR_EDITABLE_BLOCK = "can-edit";

    /**the outer velocity-string of the page currently being parsed*/
    private String pageVelocity;
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
        this.pageVelocity = null;
        this.blocks = new HashSet<>();
        this.rows = new HashSet<>();
        this.docType = null;
    }


    /**
     * Parse the default template-file of the page-class and return a PageClass-object, filled with it's blocks, rows and the most-outer velocity-string of the page-class
     * @param pageClassName the name of the page-class to be parsed (f.i. "default" for a pageClass filtered from the file "pages/default/index.html")
     * @return a page-class parsed from the "pages/<page-class-name>/index.html"
     * @throws PageParserException
     */
    public PageClass parsePageClass(String pageClassName) throws PageParserException
    {
        try{
            String templateFilename = this.getTemplatePath(pageClassName);
            File pageClassVelocity = new File(templateFilename);
            //get the url used for identifying blocks and rows for this page-class
            URL pageClassURL = PageClass.getBaseUrl(pageClassName);
            //fill up this parser-class, with the elements and velocity filtered from the default template-file
            String foundPageClassName = this.fillWithPageClass(pageClassVelocity, pageClassURL);
            if(!foundPageClassName.equals(pageClassName)){
                throw new PageParserException("The name of the specified page-class (" + pageClassName + ") does not match the name found in the page-class template: " + foundPageClassName);
            }
            return new PageClass(pageClassName, this.blocks, this.rows, this.pageVelocity, this.docType);
        }
        catch(PageParserException e){
            throw e;
        }
        catch(ElementException e){
            throw new PageParserException("Could not create PageClass with parsed data.", e);
        }
        catch(Exception e){
            throw new PageParserException("Error while parsing page-class '" + pageClassName + "' from template.", e);
        }
    }

    /**
     *
     *
     * @param pageHtml a html-page to be parsed
     * @param pageUrl the url that will be used to id blocks and rows
     * @return a page-instance filled with the blocks and rows filtered from the url's htmlcontent
     */
    public Page parsePage(String pageHtml, URL pageUrl) throws PageParserException

    {
        try{
            String pageClassName = this.fillWithPage(pageHtml, pageUrl);
            PageClass pageClass = PageClassCache.getInstance().getPageClassCache().get(pageClassName);
            //return a page-instance with a newly versioned id and the found blocks and rows of class 'pageClass'
            return new Page(new PageID(pageUrl), this.blocks, this.rows, pageClass);
        }
        catch(PageClassCacheException e){
            throw new PageParserException("Error while getting page-class from cache. ", e);
        }
        catch (ElementException e){
            throw new PageParserException("Could not create Page with parsed data.", e);
        }
        catch(Exception e){
            throw new PageParserException("Error while parsing page '" + pageUrl + "'.", e);
        }

    }

    /**
     * check whether the specified document is a page (it must have <body class='page page-classname'> present in the html-structure)
     * @param page page to be checked
     * @return the name of the page-class of the page that was checked
     * @throws PageParserException when the document isn't a correct 'page'
     */
    public String checkPage(Document page) throws PageParserException
    {
        boolean isPage = false;
        boolean hasPageClass = false;
        String pageClassName = "";
        Iterator<String> it = page.body().classNames().iterator();
        while(it.hasNext() && (!isPage || !hasPageClass)){
            String className = it.next();
            if(!isPage) {
                isPage = className.equals(CSS_CLASS_FOR_PAGE);
            }
            if(!hasPageClass && className.startsWith(CSS_CLASS_FOR_PAGECLASS_PREFIX)){
                hasPageClass = true;
                pageClassName = className.substring(CSS_CLASS_FOR_PAGECLASS_PREFIX.length(), className.length());
            }
        }
        if(!isPage){
            throw new PageParserException("Not a page, <body class='" + CSS_CLASS_FOR_PAGE +"'> could not be found at '" + page.location() + "'");
        }
        else if(!hasPageClass){
            throw new PageParserException("Page has no page-class, <body class='" + CSS_CLASS_FOR_PAGE + " " + CSS_CLASS_FOR_PAGECLASS_PREFIX + "classname'> could not be found at '" + page.location() + "'");
        }
        else{
            return pageClassName;
        }
    }


    /**
     * Parses a velocity-file, containing a page-class, to blocks and rows containing velocity-variables and fills this parser up with the found content.
     * After the parse, a string containing the velocity of this page will be saved in the field 'pageVelocity' and the found blocks and rows will be stored in the fields 'blocks' and 'rows
     * @param velocityTemplate the velocitytemplate containing a html-tree of rows and blocks
     * @param baseUrl the base-url which will be used to define the row- and block-ids
     * @return the name of the page-class found in the template
     */
    private String fillWithPageClass(File velocityTemplate, URL baseUrl) throws PageParserException, PageClassCacheException
    {
        this.empty();
        //first fill in all known velocity-variables, so we get a normal <html><head></head><body></body></html>-structure from the template
        Template template = R.templateEngine().getEmptyTemplate(velocityTemplate.getAbsolutePath());
        String pageClassName = this.fill(template.render(), baseUrl);
        return pageClassName;
    }

    /**
     * Parses a html-string, containing a page-instance, to blocks and rows containing velocity-variables and fills this parser up with the found content.
     * After the parse, a string containing the velocity of this page will be saved in the field 'pageVelocity' and the found blocks and rows will be stored in the fields 'blocks' and 'rows
     * @param html a html-tree of rows and blocks
     * @param pageUrl the page-url which will be used to define the row- and block-ids
     * @return the name of the page-class of the specified page
     */
    private String fillWithPage(String html, URL pageUrl) throws PageParserException, PageClassCacheException
    {
        this.empty();
        String pageClassName = this.fill(html, pageUrl);
        return pageClassName;
    }


    /**
     * Parses a html-string, containing a html-tree of blocks and rows, to blocks and rows containing velocity-variables and fills this parser up with the found content.
     * After the parse, a string containing the velocity of this page will be saved in the field 'pageVelocity' and the found blocks and rows will be stored in the fields 'blocks' and 'rows
     * @param htmlTree a html-tree of rows and blocks
     * @param baseUrl the base-url which will be used to define the row- and block-ids
     * @return the name of the page-class found in the html-tree
     */
    private String fill(String htmlTree, URL baseUrl) throws PageParserException, PageClassCacheException
    {
        //TODO: extend Parser to let Jsoup do the hard row- and block-searching
        //TODO BAS: ignore witespace between tags! -> makes block-comparison easier
        Document htmlDOM = Jsoup.parse(htmlTree, BlocksConfig.getSiteDomain());
        //throws errors if the htmlDOM is not a correct page
        String pageClassName = this.checkPage(htmlDOM);
        //if no document-type is present in the DOM, try to fetch the right one from the PageClassCache
        htmlDOM = this.typeDocument(htmlDOM, pageClassName);
        //fill up the rowset and the blockset and alter the htmlDOM to be a velocity-template holding velocity-variables for the upper-rows
        this.recursiveParse(htmlDOM, baseUrl);
        this.pageVelocity = StringEscapeUtils.unescapeXml(htmlDOM.outerHtml());
        return pageClassName;
    }

    /**
     * Empties this pageparser, so no residues of previous parsings will be lingering during parse.
     */
    private void empty(){
        this.pageVelocity = "";
        this.rows = new HashSet<>();
        this.blocks = new HashSet<>();
        this.docType = null;
    }

    /**
     * Parses the tree starting with the node-element, looking for row- and block-elements and adding them to the proper fields in this PageParser
     * Alters the node-element to holding velocity-variables instead of other elements.
     * @param node root of the tree to be parsed
     * @param baseUrl the base-url used which will be used to define the row- and block-ids
     * @return a set holding blocks and rows
     */
    private void recursiveParse(Element node, URL baseUrl) throws PageParserException
    {
        Elements children = node.children();
        for(Element child : children){
            //recursively iterate over the subtree starting with this child and add the found blocks and rows to the map
            recursiveParse(child, baseUrl);
            boolean isRow = child.classNames().contains(CSS_CLASS_FOR_ROW);
            boolean isBlock = child.classNames().contains(CSS_CLASS_FOR_BLOCK);
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
                        throw new PageParserException("Base-url doesn't seem to be correct. Cannot construct proper IDs with this page-url: " + baseUrl, e);
                    }catch(URISyntaxException e){
                        throw new PageParserException("Base-url doesn't seem to be correct. Cannot construct proper IDs with this page-url: " + baseUrl, e);
                    }
                    if(isRow){
                        boolean isFinal =  !(child.classNames().contains(CSS_CLASS_FOR_MODIFIABLE_ROW) || child.classNames().contains(CSS_CLASS_FOR_LAYOUTABLE_ROW) || child.classNames().contains(
                                        CSS_CLASS_FOR_CREATE_ENABLED_ROW));
                        this.rows.add(new Row(id, childHtml, isFinal));
                    }
                    else{
                        boolean isFinal =  !(child.classNames().contains(CSS_CLASS_FOR_EDITABLE_BLOCK));
                        this.blocks.add(new Block(id, childHtml, isFinal));
                    }
                    child.replaceWith(new TextNode("\n ${" + child.id() + "}\n", ""));
                }
                else{
                    //if no id i
                    throw new PageParserException("A row- or block-element in the html-tree doesn't have an id, this shouldn't happen: \n" + child.outerHtml());
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
    private Document typeDocument(Document htmlDOM, String pageClassName) throws PageClassCacheException
    {
        List<Node> nodes = htmlDOM.childNodes();
        Iterator<Node> it = nodes.iterator();
        boolean hasDocumentType = false;
        while(!hasDocumentType && it.hasNext()){
            Node node = it.next();
            if (node instanceof DocumentType) {
                this.docType = node.toString();
                hasDocumentType = true;
            }
        }
        if(!hasDocumentType){
            this.docType = PageClassCache.getInstance().getPageClassCache().get(pageClassName).getDocType();
            if(this.docType != null){
                htmlDOM.prepend(this.docType);
            }
            else{
                //no doctype could be found in the page-class-cache, so do nothing
            }
        }
        return htmlDOM;
    }


    /**
     * returns the path to the html-template of a page-class
     * @param pageClassName name of the page-class
     */
    private String getTemplatePath(String pageClassName){
        return BlocksConfig.getTemplateFolder() + "/pages/" + pageClassName + "/index.html";
    }




    //    /**
    //     * Turn xhtml into normal html using JSoup
    //     * @param xhtml the xhtml to parse
    //     * @return a string containing html
    //     */
    //    public String toHtml(String xhtml){
    //        //re-turn the xhtml to html using Jsoup
    //        Document returningXhtmlDOM = Jsoup.parse(xhtml, "", Parser.xmlParser());
    //        String html = returningXhtmlDOM.outerHtml();
    //        return html;
    //    }


}
