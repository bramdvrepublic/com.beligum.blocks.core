package com.beligum.blocks.core.parsing;

import com.beligum.blocks.core.config.BlocksConfig;
import com.beligum.blocks.core.exceptions.PageParserException;
import com.beligum.blocks.core.identifiers.RedisID;
import com.beligum.blocks.core.models.PageClass;
import com.beligum.blocks.core.models.storables.Block;
import com.beligum.blocks.core.models.storables.Page;
import com.beligum.blocks.core.models.storables.Row;
import com.beligum.core.framework.base.R;
import com.beligum.core.framework.templating.ifaces.Template;
import org.apache.commons.configuration.ConfigurationRuntimeException;
import org.apache.commons.lang3.StringEscapeUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashSet;
import java.util.Iterator;
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

    //the outer velocity-string of the page currently being parsed
    private String pageVelocity;
    //a set of all the blocks of the page currently being parsed
    private Set<Block> blocks;
    //a set of all the rows of the page currently being parsed
    private Set<Row> rows;

    /**
     *  Default constructor
     */
    public PageParser()
    {
        this.pageVelocity = null;
        this.blocks = new HashSet<>();
        this.rows = new HashSet<>();
    }


    /**
     * Parse the default template-file of the page-class and return a PageClass-object, filled with it's blocks, rows and the most-outer velocity-string of the page-class
     * @param pageClassName the name of the page-class to be parsed (f.i. "default" for a pageClass filtered from the file "pages/default/index.html")
     * @return a page-class parsed from the "pages/<page-class-name>/index.html"
     * @throws com.beligum.blocks.core.exceptions.PageParserException
     */
    public PageClass parsePageClass(String pageClassName)
    {
        try {
            String templateFilename = this.getTemplatePath(pageClassName);
            File pageClassVelocity = new File(templateFilename);
            //get the url used for identifying blocks and rows for this page-class
            URL pageClassURL = PageClass.getBaseUrl(pageClassName);
            //fill up this parser-class, with the elements and velocity filtered from the default template-file
            this.fillWithPageClass(pageClassVelocity, pageClassURL);

            return new PageClass(pageClassName, this.blocks, this.rows, this.pageVelocity);
        }catch(IOException e){
            throw new ConfigurationRuntimeException("");
        }

    }

    /**
     *
     *
     * @param html a html-page to be parsed
     * @return a page-instance filled with the blocks and rows filtered from the url's htmlcontent
     */
    public Page parsePage(String html) throws URISyntaxException

    {
        //TODO BAS: implement this
        return new Page(null, null);
    }

    /**
     * check whether the specified document is a page (it must have <body class='page page-classname'> present in the html-structure)
     * @param page page to be checked
     * @return the pageclass of the page that was checked
     * @throws PageParserException when the document isn't a correct 'page'
     */
    public String checkPage(Document page){
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
     * After the parse, a string containing the velocity of this page will be saved in the field 'pageVelocity' and the found blocks and rows will be stored in the field 'elements'
     * @param velocityTemplate the velocitytemplate containing a html-tree of rows and blocks
     * @param baseUrl the base-url which will be used to define the row- and block-ids
     * @return a set holding the blocks and rows that were parsed from the html-file (page)
     * @throws IOException
     */
    private void fillWithPageClass(File velocityTemplate, URL baseUrl) throws IOException
    {
        this.empty();
        //first fill in all known velocity-variables, so we get a normal <html><head></head><body></body></html>-structure from the template
        Template template = R.templateEngine().getEmptyTemplate(velocityTemplate.getAbsolutePath());
        Document htmlDOM = Jsoup.parse(template.render(), BlocksConfig.getSiteDomain());
        //throws errors if the htmlDOM is not a correct page
        String pageClassName = this.checkPage(htmlDOM);
        //fill up the rowset and the blockset and alter the htmlDOM to be a velocity-template holding velocity-variables for the upper-rows
        recursiveParse(htmlDOM, baseUrl);
        this.pageVelocity = StringEscapeUtils.unescapeXml(htmlDOM.outerHtml());
    }

    private void fillWithPage(String html){
        this.empty();
        Document htmlDOM = Jsoup.parse(html, BlocksConfig.getSiteDomain());
    }

    /**
     * Empties this pageparser, so no residues of previous parsings will be lingering during parse.
     */
    private void empty(){
        this.pageVelocity = "";
        this.rows = new HashSet<>();
        this.blocks = new HashSet<>();
    }

    /**
     * Parses the tree starting with the node-element, looking for row- and block-elements and adding them to the proper fields in this PageParser
     * Alters the node-element to holding velocity-variables instead of other elements.
     * @param node root of the tree to be parsed
     * @param baseUrl the base-url used which will be used to define the row- and block-ids
     * @return a set holding blocks and rows
     */
    private void recursiveParse(Element node, URL baseUrl)
    {
        Elements children = node.children();
        for(Element child : children){
            //recursively iterate over the subtree starting with this child and add the found blocks and rows to the map
            recursiveParse(child, baseUrl);
            //TODO BAS: only can-edit and can-layout blocks and rows should be parsed
            boolean isRow = child.classNames().contains("row");
            boolean isBlock = child.classNames().contains("block");
            if(isRow || isBlock){
                if(child.id() != null && !child.id().isEmpty()) {
                    //TODO BAS: is this the most efficient way we can get rid of the &quot;-problem during return-velocity-parsing, since this will read over the whole content again
                    String childHtml = StringEscapeUtils.unescapeXml(child.outerHtml());
                    //render id for this element (row or block)
                    RedisID id = null;
                    try {
                        URI temp = baseUrl.toURI().resolve("#" + child.id());
                        URL childUrl = temp.toURL();
                        id = new RedisID(childUrl);
                    }catch(MalformedURLException e){
                        throw new PageParserException("Base-url doesn't seem to be correct. Cannot construct proper IDs with this page-url: " + baseUrl, e);
                    }catch(URISyntaxException e){
                        throw new PageParserException("Base-url doesn't seem to be correct. Cannot construct proper IDs with this page-url: " + baseUrl, e);
                    }
                    if(isRow){
                        this.rows.add(new Row(id, childHtml));
                    }
                    else{
                        this.blocks.add(new Block(id, childHtml));
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
