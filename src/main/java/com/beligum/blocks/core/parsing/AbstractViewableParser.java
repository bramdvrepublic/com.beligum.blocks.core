package com.beligum.blocks.core.parsing;

import com.beligum.blocks.core.config.BlocksConfig;
import com.beligum.blocks.core.config.CSSClasses;
import com.beligum.blocks.core.exceptions.CacheException;
import com.beligum.blocks.core.exceptions.ParserException;
import com.beligum.blocks.core.identifiers.ElementID;
import com.beligum.blocks.core.models.classes.AbstractViewableClass;
import com.beligum.blocks.core.models.storables.Block;
import com.beligum.blocks.core.models.storables.Row;
import com.beligum.core.framework.base.R;
import com.beligum.core.framework.templating.ifaces.Template;
import org.apache.commons.lang3.StringEscapeUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Created by bas on 03.11.14.
 * Interface defining what you need to be able to parse a CachableClass (like f.i. a PageClass or BlockClass)
 */
public abstract class AbstractViewableParser<T extends AbstractViewableClass>
{
    /** the name of the viewable-class currently being parsed */
    protected String viewableClassName;
    /** boolean whether or not this parser is filled with viewable-class-data */
    protected boolean filled;
    /**the template containing names of variables of the viewable currently being parsed*/
    protected String viewableTemplate;
    /**the direct child-rows of the viewable currently being parsed*/
    protected Set<Row> directChildren;
    /**a set of all the blocks of the page currently being parsed*/
    protected Set<Row> elements;
    /**the current depth in the row-tree currently being parsed*/
    private int currentRowDepth;


    /**
     *
     * @param viewableClassName the name of the viewable-class to be parsed (f.i. "default" for a pageClass filtered from the file "entities/default/index.html")
     */
    protected AbstractViewableParser(String viewableClassName){
        this.viewableClassName = viewableClassName;
        this.filled = false;
        this.viewableTemplate = "";
        this.directChildren = new HashSet<> ();
        this.elements = new HashSet<>();
        this.currentRowDepth = 0;
        this.directChildren = new HashSet<>();
    }

    public String getViewableClassName()
    {
        return viewableClassName;
    }
    public boolean isFilled(){
        return filled;
    }
    protected void setFilled(boolean filled)
    {
        this.filled = filled;
    }

    /**
     * Parse the default template-file of the viewable-class and return a CachableClass-object, filled with it's blocks, rows and the template of the viewable-class
     * @return a viewable-class parsed from file system
     * @throws ParserException
     */
    public T parseViewableClass() throws ParserException
    {
        try{
            //retrieve the template from disk
            String relativeTemplatePath = this.getRelativeTemplatePath(viewableClassName);
            //get the url used for identifying blocks and rows for this viewable-class
            URL viewableClassBaseUrl = this.getBaseUrl(viewableClassName);
            //fill up this parser-class, with the elements and template filtered from the default html-file
            this.fillWithViewableClass(relativeTemplatePath, viewableClassBaseUrl);
            return this.getInternalViewableClass();
        }
        catch(ParserException e){
            throw e;
        }
        catch(Exception e){
            throw new ParserException("Error while parsing viewable-class '" + this.getViewableCssClassPrefix() + viewableClassName + "' from template.", e);
        }
    }

    /**
     * Parses a html-file, containing a page-class, to blocks and rows containing variables and fills this parser up with the found template.
     * After the parse, a string containing the template of this page will be saved in the field 'pageTemplate' and the found blocks and rows will be stored in the fields 'blocks' and 'rows
     * @param treeFilename the file containing a html-tree of rows and blocks
     * @param baseUrl the base-url which will be used to define the row- and block-ids
     */
    private void fillWithViewableClass(String treeFilename, URL baseUrl) throws ParserException
    {
        try {
            //first fill in all known variables, so we get a normal <html><head></head><body></body></html>-structure from the template
            Template template = R.templateEngine().getEmptyTemplate(treeFilename);
            this.fill(template.render(), baseUrl);
        }catch(NullPointerException e){
            throw e;
        }
    }

    /**
     * Empties this parser, so no residues of previous parsings will be lingering during parse.
     */
    protected void empty(){
        this.filled = false;
        this.viewableTemplate = "";
    }

    /**
     * Parses a html-file, containing a viewable-class and fills this parser up with the found template.
     * @param viewableClassTemplateString the relative path to the file containing html of a viewable-class
     * @param baseUrl the base-url which will be used to define the row- and block-ids
     * @return the name of the viewable-class found in the template
     */
    protected void fill(String viewableClassTemplateString, URL baseUrl) throws ParserException
    {
        this.empty();
        //TODO: extend Parser to let Jsoup do the hard row- and block-searching
        //TODO BAS: ignore witespace between tags! -> makes block-comparison easier
        Document htmlDOM = Jsoup.parse(viewableClassTemplateString, BlocksConfig.getSiteDomain());
        //throws errors if the htmlDOM is not a correct viewable
        this.checkForViewable(htmlDOM);
        //if no document-type is present in the DOM, try to fetch the right one from the PageClassCache
        //TODO BAS: typeDocument should be added again!
        //            htmlDOM = this.typeDocument(htmlDOM, pageClassName);
        //fill up the rowset and the blockset and alter the htmlDOM to be a template holding variables for the upper-rows
        this.recursiveParse(htmlDOM, baseUrl);
        this.setFilled(true);
    }

//    /**
//     * Parse a document and fill in all parser-content-fields Parses a html-document, containing a viewable-class, to blocks and rows containing variables and fills this parser up with the found template.
//     * After the parse, a string containing the template of this viewable will be saved in the field 'viewableTemplate' TODO BAS: and the found blocks and rows will be stored in the field 'elements'
//     *
//     * @param DOM the document to be parsed
//     * @param baseUrl the base-url which will be used to define the ids
//     * @return the documents template, with template-variable-names were actual rows used to be
//     */
//    abstract protected Document parse(Document DOM, URL baseUrl) throws ParserException;

    /**
     * check whether the specified document is a viewableInstance (it must have <body class='page page-classname'> present in the html-structure)
     * @param viewable viewable to be checked
     * @return true if the viewable  has a correct html-structure, or false if an empty viewable has been found, for all uncorrect structures, it throws an ParserException
     * @throws ParserException when the document isn't a correct 'viewable' or if the found viewable is not the one this parser was specified to parse on initialization
     */
    protected boolean checkForViewable(Document viewable) throws ParserException
    {
        Elements children = viewable.body().children();
        if(children.size()>1){
            throw new ParserException("The html-template of a viewable-class must be surrounded with one single <div class='"+ this.getViewableCssClass() + " " + this.getViewableCssClassPrefix() +"class'>-tag, but I found " + children.size() + ".");
        }
        else if(children.size() == 1){
            boolean isViewable = false;
            boolean hasViewableClass = false;
            String foundViewableClassName = "";
            Iterator<String> it = children.get(0).classNames().iterator();
            while (it.hasNext() && (!isViewable || !hasViewableClass)) {
                String className = it.next();
                if (!isViewable) {
                    isViewable = className.equals(this.getViewableCssClass());
                }
                if (!hasViewableClass && className.startsWith(this.getViewableCssClassPrefix())) {
                    hasViewableClass = true;
                    foundViewableClassName = this.removeViewableClassPrefix(className);
                    if(!foundViewableClassName.contentEquals(this.viewableClassName)){
                        throw new ParserException("The class of this viewable '" + foundViewableClassName + "' does not match the class I am supposed to parse '" + this.viewableClassName + "'. This should not happen.");
                    }
                }
            }
            if(!isViewable){
                throw new ParserException("Not a page, <div class='" + this.getViewableCssClass() +"'> could not be found at '" + viewable.location() + "'");
            }
            else if(!hasViewableClass){
                throw new ParserException("Page has no page-class, <div class='" + this.getViewableCssClass() + " " + this.getViewableCssClassPrefix() + "classname'> could not be found at '" + viewable.location() + "'");
            }
            else{
                return true;
            }
        }
        else{
            //if no children have been found, we're dealing with an empty instance and we return an empty string
            return false;
        }
    }


    /**
     * Parses the tree starting with the node-element, looking for row- and block-elements and adding them to the proper fields in this PageParser. Blocks are always seen as leafs
     * Alters the node-element to holding variables instead of other elements.
     * @param node root of the tree to be parsed
     * @param baseUrl the base-url used which will be used to define the row- and block-ids
     */
    private void recursiveParse(Element node, URL baseUrl) throws ParserException{
        this.recursiveParse(node, baseUrl, 0);
    }

    /**
     * Parses the tree starting with the node-element, looking for row- and block-elements and adding them to the proper fields in this PageParser. Blocks are always seen as leafs
     * Alters the node-element to holding variables instead of other elements.
     * @param node root of the tree to be parsed
     * @param baseUrl the base-url used which will be used to define the row- and block-ids
     * @param rowDepth the depth of the row currently being parsed
     * @return a set of all row-children of the given node
     */
    private Set<Row> recursiveParse(Element node, URL baseUrl, int rowDepth) throws ParserException
    {
        //TODO BAS: this should check if all blocks are inside a column, if not, throw ParserException

        Set<Row> rowChildren = new HashSet<>();
        Elements children = node.children();
        //the css-classes needed for an html-tag to represent a entity- or block-class (from a template-file)
        Set<String> cssClassesForViewableClass = new HashSet<>();
        cssClassesForViewableClass.add(this.getViewableCssClass());
        cssClassesForViewableClass.add(this.getViewableCssClassPrefix() + this.getViewableClassName());
        for (Element child : children) {
            boolean isEntity = child.classNames().contains(CSSClasses.ENTITY);
            boolean isRow = child.classNames().contains(CSSClasses.ROW);
            boolean isBlock = child.classNames().contains(CSSClasses.BLOCK);
            boolean isClass = child.classNames().containsAll(cssClassesForViewableClass);
            if(isClass){
                if(isEntity || isBlock){
                    //a class always is on row-depth 0
                    rowChildren.addAll(recursiveParse(child, baseUrl, 0));
                    //TODO BAS: is this the most efficient way we can get rid of the &quot;-problem during return-velocity-parsing, since this will read over the whole template again
                    this.viewableTemplate = StringEscapeUtils.unescapeXml(child.outerHtml());
                }
                else{
                    throw new ParserException("Found a class-definition for '" + child.tag().toString() + "' instead of '" + this.getViewableCssClassPrefix() + this.getViewableClassName() + "', which is expected.");
                }
            }
            else if (isRow || isBlock) {
                if (child.id() == null || child.id().isEmpty()) {
                    throw new ParserException("A row or block in the html-tree doesn't have an id, this shouldn't happen: \n" + child.outerHtml());
                }
                rowDepth = rowDepth + 1;
                //TODO BAS: is this the most efficient way we can get rid of the &quot;-problem during return-velocity-parsing, since this will read over the whole template again
                String childHtml = StringEscapeUtils.unescapeXml(child.outerHtml());
                //render id for this element (row or block)
                ElementID id = null;
                try {
                    URI temp = baseUrl.toURI().resolve("#" + child.id());
                    URL childUrl = temp.toURL();
                    id = new ElementID(childUrl);
                }
                catch (MalformedURLException | URISyntaxException e) {
                    throw new ParserException("Base-url doesn't seem to be correct. Cannot construct proper IDs with this page-url: " + baseUrl, e);
                }
                Row childRow = null;
                if (isBlock) {
                    //we found a block, so we do not parse any further recursively (block == leaf)
                    Set<String> classNames = child.classNames();
                    /*
                     * First set the block-class to 'default'. If another block-class can be found, change it to that block-class.
                     */
                    String blockClassName= CSSClasses.DEFAULT_BLOCK_CLASS;
                    boolean foundNonDefaultClassName = false;
                    Iterator<String> it = classNames.iterator();
                    while(it.hasNext() && !foundNonDefaultClassName){
                        String className = it.next();
                        if(className.contains(CSSClasses.BLOCK_CLASS_PREFIX)){
                            blockClassName = this.removeViewableClassPrefix(className);
                            foundNonDefaultClassName = true;
                        }
                    }

                    boolean isFinal = !(classNames.contains(CSSClasses.EDITABLE_BLOCK));
                    try{
                        childRow = new Block(id, childHtml, blockClassName, isFinal);
                    }catch(CacheException e){
                        throw new ParserException("Couldn't retrieve block-class '" + blockClassName + "'from cacher.", e);
                    }
                }
                else{
                    //recursively iterate over the subtree starting with this row and add the found blocks and rows to the map
                    this.recursiveParse(child, baseUrl, rowDepth);
                    boolean isFinal = !(child.classNames().contains(CSSClasses.MODIFIABLE_ROW) || child.classNames().contains(CSSClasses.LAYOUTABLE_ROW) || child.classNames().contains(
                                    CSSClasses.CREATE_ENABLED_ROW));
                    childRow = new Row(id, childHtml, this.directChildren, isFinal);
                }
                child.replaceWith(new TextNode("\n ${" + child.id() + "}\n", ""));
                this.elements.add(childRow);
                //TODO BAS SH: je bent aan het recursief parsen, rekening houdend met de boomdiepte, die moet in het terugkeren ook aangepast worden, net als de kinderen, die ook correct moeten toegevoegd worden aan een rij
                if(rowDepth == this.currentRowDepth) {
                    rowChildren.add(childRow);
                }
                else if(rowDepth == this.currentRowDepth + 1){
                    this.directChildren = new HashSet<>();
                    this.directChildren.add(childRow);
                    this.currentRowDepth = rowDepth;
                }
                else if(rowDepth == this.currentRowDepth - 1){

                }
                else{
                    throw new RuntimeException("No idea what happened, found a double step in the recursive parse. This should not happen.");
                }
            }
            else {
                //recursively iterate over the tree (skip ahead, since we didn't find a row, block or entity)
                this.recursiveParse(child, baseUrl);
            }
        }
        return rowChildren;
    }

    /**
     *
     * @param viewableCssClass a css class containing the name of a viewable class (f.i. of the from "entity-default")
     * @return the name of the viewable class
     */
    private String removeViewableClassPrefix(String viewableCssClass){
        return viewableCssClass.substring(this.getViewableCssClassPrefix().length(), viewableCssClass.length());
    }

        /**
         * returns the absolute path to the html-template of a viewable-class
         * @param viewableClassName name of the viewable-class
         */
    abstract protected Path getAbsoluteTemplatePath(String viewableClassName);

    /**
     * returns the relative path to the html-template of a viewable-class
     * @param viewableClassName name of the viewable-class
     */
    abstract protected String getRelativeTemplatePath(String viewableClassName);

    /**
     * returns the base-url for the viewable-class
     * @param viewableClassName the name of the viewable-class (f.i. "default" for a pageClass filtered from the file "entities/default/index.html")
     * @return
     */
    abstract public URL getBaseUrl(String viewableClassName) throws MalformedURLException;

    /**
     *
     * @return the prefix of the viewable class parsed by this CacableClassParser, used in the class-attribute of the html-template (f.i. "page-" for a PageParser)
     */
    abstract public String getViewableCssClassPrefix();

    /**
     *
     * @return the css class-name the parser must find in a certain <div class='classname'>-tag to know the html represents a viewable class
     */
    abstract public String getViewableCssClass();

    /**
     * Returns the viewable-class present in the internal parser-data. The parser needs to be filled, before you can use this method.
     * @return get the viewable-class with which this parser is filled up
     * @throws ParserException if no viewable-class could be constructed out of the internal parser-data
     */
    abstract protected T getInternalViewableClass() throws ParserException;


}
