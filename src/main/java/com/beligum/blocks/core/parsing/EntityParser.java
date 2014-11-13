package com.beligum.blocks.core.parsing;

import com.beligum.blocks.core.caching.EntityClassCache;
import com.beligum.blocks.core.config.BlocksConfig;
import com.beligum.blocks.core.config.CSSClasses;
import com.beligum.blocks.core.config.CacheConstants;
import com.beligum.blocks.core.exceptions.CacheException;
import com.beligum.blocks.core.exceptions.ParserException;
import com.beligum.blocks.core.identifiers.EntityID;
import com.beligum.blocks.core.models.classes.EntityClass;
import com.beligum.blocks.core.models.storables.Entity;
import com.beligum.core.framework.utils.toolkit.FileFunctions;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.DocumentType;
import org.jsoup.nodes.Node;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

/**
 * Created by bas on 30.09.14.
 * Parser class for parsing entities
 */
public class EntityParser extends AbstractViewableParser<EntityClass>
{
    /**the doctype of the page currently being parsed*/
    private String docType;

    /**
     *  Default constructor
     */
    public EntityParser(String entityClassName)
    {
        super(entityClassName);
        this.allChildren = new HashSet<>();
        this.docType = null;
    }


//    /**
//     * Parse the default template-file of the page-class and return a PageClass-object, filled with it's blocks, rows and the template of the page-class
//     * @param pageClassName the name of the page-class to be parsed (f.i. "default" for a pageClass filtered from the file "entities/default/index.html")
//     * @return a page-class parsed from the "entities/<page-class-name>/index.html"
//     * @throws com.beligum.blocks.core.exceptions.ParserException
//     */
//    @Override
//    public PageClass parseViewableClass(String pageClassName) throws ParserException
//    {
//        try{
//            String templateFilename = this.getAbsoluteTemplatePath(pageClassName);
//            File pageClassTemplate = new File(templateFilename);
//            //get the url used for identifying blocks and rows for this page-class
//            URL pageClassURL = PageClass.getBaseUrl(pageClassName);
//            //fill up this parser-class, with the elements and template filtered from the default html-file
//            String foundPageClassName = this.fillWithPageEntityClass(pageClassTemplate, pageClassURL);
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
//     * This method does the same as 'parseViewableClass(String pageClassName)', but is used as syntactic glue.
//     * @param pageClassName the name of the page-class to be parsed (f.i. "default" for a pageClass filtered from the file "entities/default/index.html")
//     * @return a page-class parsed from the "entities/<page-class-name>/index.html"
//     * @throws com.beligum.blocks.core.exceptions.ParserException
//     */
//    public PageClass parsePageClass(String pageClassName) throws ParserException
//    {
//        return this.parseViewableClass(pageClassName);
//    }

    /**
     *
     *
     * @param pageHtml a html-page to be parsed
     * @param pageUrl the url that will be used to id blocks and rows
     * @return a page-instance filled with the blocks and rows filtered from the url's htmlcontent
     */
    public Entity parsePage(String pageHtml, URL pageUrl) throws ParserException

    {
        try{
            this.fillWithPage(pageHtml, pageUrl);
            //return a page-instance with a newly versioned id and the found blocks and rows of class 'pageClass'
            return new Entity(new EntityID(pageUrl), this.allChildren, this.viewableClassName);
        }
        catch(CacheException e){
            throw new ParserException("Error while getting page-class from cache. ", e);
        }
        catch(Exception e){
            throw new ParserException("Error while parsing page '" + pageUrl + "'.", e);
        }

    }

//    /**
//     * check whether the specified document is a page (it must have <body class='page page-classname'> present in the html-structure)
//     * @param page page to be checked
//     * @return the name of the page-class of the page that was checked
//     * @throws com.beligum.blocks.core.exceptions.ParserException when the document isn't a correct 'page'
//     */
//    public String checkPage(Document page) throws ParserException
//    {
//        boolean isPage = false;
//        boolean hasPageClass = false;
//        String pageClassName = "";
//        Iterator<String> it = page.body().classNames().iterator();
//        while(it.hasNext() && (!isPage || !hasPageClass)){
//            String className = it.next();
//            if(!isPage) {
//                isPage = className.equals(CSSClasses.ENTITY);
//            }
//            if(!hasPageClass && className.startsWith(CSSClasses.ENTITY_CLASS_PREFIX)){
//                hasPageClass = true;
//                pageClassName = className.substring(CSSClasses.ENTITY_CLASS_PREFIX.length(), className.length());
//            }
//        }
//        if(!isPage){
//            throw new ParserException("Not a page, <body class='" + CSSClasses.ENTITY +"'> could not be found at '" + page.location() + "'");
//        }
//        else if(!hasPageClass){
//            throw new ParserException("Page has no page-class, <body class='" + CSSClasses.ENTITY + " " + CSSClasses.ENTITY_CLASS_PREFIX + "classname'> could not be found at '" + page.location() + "'");
//        }
//        else{
//            return pageClassName;
//        }
//    }


//    /**
//     * Parses a html-file, containing a page-class, to blocks and rows containing variables and fills this parser up with the found template.
//     * After the parse, a string containing the template of this page will be saved in the field 'pageTemplate' and the found blocks and rows will be stored in the fields 'blocks' and 'rows
//     * @param treeFilename the file containing a html-tree of rows and blocks
//     * @param baseUrl the base-url which will be used to define the row- and block-ids
//     * @return the name of the page-class found in the template
//     */
//    private String fillWithPageEntityClass(String treeFilename, URL baseUrl) throws ParserException
//    {
//        try {
//            //first fill in all known variables, so we get a normal <html><head></head><body></body></html>-structure from the template
//            Template template = R.templateEngine().getEmptyTemplate(treeFilename);
//            String pageClassName = this.fill(template.render(), baseUrl);
//            return pageClassName;
//        }catch(NullPointerException e){
//            throw e;
//        }
//    }

    /**
     * Parses a html-string, containing a page-instance, to blocks and rows containing variables and fills this parser up with the found template.
     * After the parse, a string containing the template of this page will be saved in the field 'pageTemplate' and the found blocks and rows will be stored in the fields 'blocks' and 'rows
     * @param html a html-tree of rows and blocks
     * @param pageUrl the page-url which will be used to define the row- and block-ids
     */
    private void fillWithPage(String html, URL pageUrl) throws ParserException
    {
        this.fill(html, pageUrl);
    }


//    /**
//     * Parses a html-string, containing a html-tree of blocks and rows, to blocks and rows containing variables and fills this parser up with the found template.
//     * After the parse, a string containing the template of this page will be saved in the field 'pageTemplate' and the found blocks and rows will be stored in the fields 'blocks' and 'rows
//     * @param htmlTree a html-tree of rows and blocks
//     * @param baseUrl the base-url which will be used to define the row- and block-ids
//     * @return the name of the page-class found in the html-tree
//     */
//    private String fill(String htmlTree, URL baseUrl) throws ParserException
//    {
//        this.empty();
//        //TODO: extend Parser to let Jsoup do the hard row- and block-searching
//        //TODO BAS: ignore witespace between tags! -> makes block-comparison easier
//        Document htmlDOM = Jsoup.parse(htmlTree, BlocksConfig.getSiteDomain());
//        //throws errors if the htmlDOM is not a correct page
//        String pageClassName = this.checkPage(htmlDOM);
//        //if no document-type is present in the DOM, try to fetch the right one from the PageClassCache
//        htmlDOM = this.typeDocument(htmlDOM, pageClassName);
//        //fill up the rowset and the blockset and alter the htmlDOM to be a template holding variables for the upper-rows
//        this.recursiveParse(htmlDOM, baseUrl);
//        this.pageTemplate = StringEscapeUtils.unescapeXml(htmlDOM.outerHtml());
//        return pageClassName;
//    }

    /**
     * Empties this pageparser, so no residues of previous parsings will be lingering during parse.
     */
    protected void empty(){
        super.empty();
        this.allChildren.clear();
        this.docType = null;
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
                this.docType = EntityClassCache.getInstance().getCache().get(pageClassName).getDocType();
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
    @Override
    protected EntityClass getInternalViewableClass() throws ParserException
    {
        if(!this.isFilled()){
            throw new ParserException("Cannot construct an entity-class out of the internal parser-data. Did you fill up the parser before calling this method?");
        }
        try {
            return new EntityClass(this.viewableClassName, this.allChildren, this.viewableTemplate, this.docType);
        }
        catch(URISyntaxException e){
            throw new ParserException("Cannot construct an entity-class out of the internal parser-data.", e);
        }
    }

    /**
     * returns the base-url for the page-entity-class
     *
     * @param pageEntityClassName the name of the page-entity-class (f.i. "default" for a pageClass filtered from the file "entities/default/index.html")
     * @return
     */
    @Override
    public URL getBaseUrl(String pageEntityClassName) throws MalformedURLException
    {
        return new URL(BlocksConfig.getSiteDomain() + "/" + CacheConstants.PAGE_ENTITY_CLASS_ID_PREFIX + "/" + pageEntityClassName);
    }

    /**
     * returns the path to the html-template of a block-class
     * @param pageEntityClassName name of the block-class
     */
    protected Path getAbsoluteTemplatePath(String pageEntityClassName){
        String relativeBlockClassPath = this.getRelativeTemplatePath(pageEntityClassName);
        //we don't need the 'file://'-part of the returned URI, so we use 'getSchemeSpecificPart()'
        URI blockClassUri = FileFunctions.searchClasspath(this.getClass(), relativeBlockClassPath);
        return Paths.get(blockClassUri.getSchemeSpecificPart());
    }

    /**
     * returns the relative path to the html-template of a viewable-class
     *
     * @param pageEntityClassName name of the viewable-class
     */
    @Override
    protected String getRelativeTemplatePath(String pageEntityClassName)
    {
        return BlocksConfig.getTemplateFolder() + "/" + BlocksConfig.ENTITIES_FOLDER + "/" + pageEntityClassName + "/" + BlocksConfig.INDEX_FILE_NAME;
    }

    /**
     * @return the prefix used for a page-class in the class-attribute of the html-template (i.e. "page-")
     */
    @Override
    public String getViewableCssClassPrefix()
    {
        return CSSClasses.ENTITY_CLASS_PREFIX;
    }

    /**
     * @return the css class-name the parser must find in a certain <div class='classname'>-tag to know the html represents a viewable class
     */
    @Override
    public String getViewableCssClass()
    {
        return CSSClasses.ENTITY;
    }
}
