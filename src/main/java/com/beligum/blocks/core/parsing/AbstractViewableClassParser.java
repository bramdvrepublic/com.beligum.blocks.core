package com.beligum.blocks.core.parsing;

import com.beligum.blocks.core.caching.BlockClassCache;
import com.beligum.blocks.core.config.BlocksConfig;
import com.beligum.blocks.core.config.CSSClasses;
import com.beligum.blocks.core.exceptions.CacheException;
import com.beligum.blocks.core.exceptions.ParserException;
import com.beligum.blocks.core.identifiers.ElementID;
import com.beligum.blocks.core.models.classes.AbstractViewableClass;
import com.beligum.blocks.core.models.classes.BlockClass;
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
import java.util.Iterator;
import java.util.Set;

/**
 * Created by bas on 03.11.14.
 * Interface defining what you need to be able to parse a CachableClass (like f.i. a PageClass or BlockClass)
 */
public abstract class AbstractViewableClassParser<T extends AbstractViewableClass>
{
    /** the name of the viewable-class currently being parsed */
    protected String viewableClassName;
    /** boolean whether or not this parser is filled with viewable-class-data */
    protected boolean filled;
    /**the template containing names of variables of the viewable currently being parsed*/
    protected String viewableTemplate;
    /**the direct child-rows of this page-entity*/
    protected Set<Row> directChildren;
    /**a set of all the blocks of the page currently being parsed*/
    protected Set<Row> elements;

    protected AbstractViewableClassParser(){
        this.viewableClassName = "";
        this.filled = false;
        this.viewableTemplate = "";
    }

    public String getCachableClassName()
    {
        return viewableClassName;
    }
    protected void setViewableClassName(String viewableClassName)
    {
        this.viewableClassName = viewableClassName;
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
     * @param viewableClassName the name of the viewable-class to be parsed (f.i. "default" for a pageClass filtered from the file "pages/default/index.html")
     * @return a viewable-class parsed from file system
     * @throws ParserException
     */
    public T parseViewableClass(String viewableClassName) throws ParserException
    {
        try{
            //retrieve the template from disk
            String relativeTemplatePath = this.getRelativeTemplatePath(viewableClassName);
            //get the url used for identifying blocks and rows for this viewable-class
            URL viewableClassBaseUrl = this.getBaseUrl(viewableClassName);
            //fill up this parser-class, with the elements and template filtered from the default html-file
            String foundPageClassName = this.fillWithViewableClass(relativeTemplatePath, viewableClassBaseUrl);
            if(!foundPageClassName.equals(viewableClassName)){
                throw new ParserException("The name of the viewable-class (" + this.getCssClassPrefix() + viewableClassName + ") does not match the viewable-class-name found in the template: " + this.getCssClassPrefix() + foundPageClassName);
            }
            return this.getInternalViewableClass();
        }
        catch(ParserException e){
            throw e;
        }
        catch(Exception e){
            throw new ParserException("Error while parsing viewable-class '" + this.getCssClassPrefix() + viewableClassName + "' from template.", e);
        }
    }

    /**
     * Parses a html-file, containing a page-class, to blocks and rows containing variables and fills this parser up with the found template.
     * After the parse, a string containing the template of this page will be saved in the field 'pageTemplate' and the found blocks and rows will be stored in the fields 'blocks' and 'rows
     * @param treeFilename the file containing a html-tree of rows and blocks
     * @param baseUrl the base-url which will be used to define the row- and block-ids
     * @return the name of the page-class found in the template
     */
    private String fillWithViewableClass(String treeFilename, URL baseUrl) throws ParserException
    {
        try {
            //first fill in all known variables, so we get a normal <html><head></head><body></body></html>-structure from the template
            Template template = R.templateEngine().getEmptyTemplate(treeFilename);
            String pageClassName = this.fill(template.render(), baseUrl);
            return pageClassName;
        }catch(NullPointerException e){
            throw e;
        }
    }

    /**
     * Empties this parser, so no residues of previous parsings will be lingering during parse.
     */
    protected void empty(){
        this.viewableClassName = "";
        this.filled = false;
        this.viewableTemplate = "";
    }

    /**
     * Parses a html-file, containing a viewable-class and fills this parser up with the found template.
     * @param viewableClassTemplateString the relative path to the file containing html of a viewable-class
     * @param baseUrl the base-url which will be used to define the row- and block-ids
     * @return the name of the viewable-class found in the template
     */
    protected String fill(String viewableClassTemplateString, URL baseUrl) throws ParserException
    {
        this.empty();
        //TODO: extend Parser to let Jsoup do the hard row- and block-searching
        //TODO BAS: ignore witespace between tags! -> makes block-comparison easier
        Document htmlDOM = Jsoup.parse(viewableClassTemplateString, BlocksConfig.getSiteDomain());
        //throws errors if the htmlDOM is not a correct page
        String pageClassName = this.checkViewableInstance(htmlDOM);
        //if no document-type is present in the DOM, try to fetch the right one from the PageClassCache
        //TODO BAS: typeDocument should be added again!
        //            htmlDOM = this.typeDocument(htmlDOM, pageClassName);
        //fill up the rowset and the blockset and alter the htmlDOM to be a template holding variables for the upper-rows
        this.recursiveParse(htmlDOM, baseUrl);
        this.viewableTemplate = StringEscapeUtils.unescapeXml(htmlDOM.outerHtml());
        return pageClassName;
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
     * @param viewableInstance viewable to be checked
     * @return the name of the viewable-class of the viewable that was checked, or an empty string if an empty viewable-class has been found
     * @throws ParserException when the document isn't a correct 'viewableInstance'
     */
    public String checkViewableInstance(Document viewableInstance) throws ParserException
    {
        //TODO BAS SH: you're trying to get out of all the shit, by debugging everything again. Currently parsing entities and blocks to put in cacher. This method here should check for block-classes as wel as entity-classes. We probably need somethinig like a parameter telling us wich of the two were checking here, or do we make this method abstract?
        Elements children = viewableInstance.body().children();
        if(children.size()>1){
            throw new ParserException("The html-template of a viewable-class must be surrounded with one single <div class='entity entity-class'>-tag or <div class='block block-class'>-tag, but I found " + children.size() + ".");
        }
        else if(children.size() == 1){
            boolean isEntity = false;
            boolean hasEntityClass = false;
            boolean isBlock = false;
            boolean hasBlockClass = false;
            String viewableClassName = "";
            Iterator<String> it = children.get(0).classNames().iterator();
            while (it.hasNext() && (!isEntity || !hasEntityClass)) {
                String className = it.next();
                if (!isEntity) {
                    isEntity = className.equals(CSSClasses.ENTITY);
                }
                if (!hasEntityClass && className.startsWith(CSSClasses.ENTITY_CLASS_PREFIX)) {
                    hasEntityClass = true;
                    viewableClassName = className.substring(CSSClasses.ENTITY_CLASS_PREFIX.length(), className.length());
                }
            }
            if(!isEntity){
                throw new ParserException("Not a page, <body class='" + CSSClasses.ENTITY +"'> could not be found at '" + viewableInstance.location() + "'");
            }
            else if(!hasEntityClass){
                throw new ParserException("Page has no page-class, <body class='" + CSSClasses.ENTITY + " " + CSSClasses.ENTITY_CLASS_PREFIX + "classname'> could not be found at '" + viewableInstance.location() + "'");
            }
            else{
                return viewableClassName;
            }
        }
        else{
            //if no children have been found, we're dealing with an empty instance and we return an empty string
            return "";
        }
    }

    /**
     * Parses the tree starting with the node-element, looking for row- and block-elements and adding them to the proper fields in this PageParser. Blocks are always seen as leafs
     * Alters the node-element to holding variables instead of other elements.
     * @param node root of the tree to be parsed
     * @param baseUrl the base-url used which will be used to define the row- and block-ids
     */
    protected void recursiveParse(Element node, URL baseUrl) throws ParserException
    {
        try {
            //TODO BAS: this should check if all blocks are inside a column, if not, throw ParserException

            Elements children = node.children();
            for (Element child : children) {
                boolean isRow = child.classNames().contains(CSSClasses.ROW);
                boolean isBlock = child.classNames().contains(CSSClasses.BLOCK);
                if (isRow || isBlock) {
                    if (child.id() != null && !child.id().isEmpty()) {
                        //TODO BAS: is this the most efficient way we can get rid of the &quot;-problem during return-velocity-parsing, since this will read over the whole template again
                        String childHtml = StringEscapeUtils.unescapeXml(child.outerHtml());
                        //render id for this element (row or block)
                        ElementID id = null;
                        try {
                            URI temp = baseUrl.toURI().resolve("#" + child.id());
                            URL childUrl = temp.toURL();
                            id = new ElementID(childUrl);
                        }
                        catch (MalformedURLException e) {
                            throw new ParserException("Base-url doesn't seem to be correct. Cannot construct proper IDs with this page-url: " + baseUrl, e);
                        }
                        catch (URISyntaxException e) {
                            throw new ParserException("Base-url doesn't seem to be correct. Cannot construct proper IDs with this page-url: " + baseUrl, e);
                        }
                        if (isBlock)  {
                            //we found a block, so we do not parse any further recursively (block == leaf)
                            //TODO BAS: block-class should be added here, and block should be added to direct children of parent-block
                            boolean isFinal = !(child.classNames().contains(CSSClasses.EDITABLE_BLOCK));
                            BlockClass blockClass = BlockClassCache.getInstance().get("default");
                            this.elements.add(new Block(id, blockClass, isFinal));
                        }
                        else{
                            //recursively iterate over the subtree starting with this row and add the found blocks and rows to the map
                            recursiveParse(child, baseUrl);
                            boolean isFinal = !(child.classNames().contains(CSSClasses.MODIFIABLE_ROW) || child.classNames().contains(CSSClasses.LAYOUTABLE_ROW) || child.classNames().contains(
                                            CSSClasses.CREATE_ENABLED_ROW));
                            this.elements.add(new Row(id, childHtml, isFinal));
                        }
                        child.replaceWith(new TextNode("\n ${" + child.id() + "}\n", ""));
                    }
                    else {
                        //if no id i
                        throw new ParserException("A row- or block-element in the html-tree doesn't have an id, this shouldn't happen: \n" + child.outerHtml());
                    }
                }
                else {
                    //recursively iterate over the tree (skip ahead, since we didn't find a row or block)
                    recursiveParse(child, baseUrl);
                }
            }
        }
        catch(CacheException e){
            throw new ParserException("Could not parse the node '" + node.nodeName() + "'.", e);
        }
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
     * @param viewableClassName the name of the viewable-class (f.i. "default" for a pageClass filtered from the file "pages/default/index.html")
     * @return
     */
    abstract public URL getBaseUrl(String viewableClassName) throws MalformedURLException;

    /**
     *
     * @return the prefix of the viewable class parsed by this CacableClassParser, used in the class-attribute of the html-template (f.i. "page-" for a PageParser)
     */
    abstract public String getCssClassPrefix();

    /**
     * Returns the viewable-class present in the internal parser-data. The parser needs to be filled, before you can use this method.
     * @return get the viewable-class with which this parser is filled up
     * @throws ParserException if no viewable-class could be constructed out of the internal parser-data
     */
    abstract protected T getInternalViewableClass() throws ParserException;


}
