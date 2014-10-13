package com.beligum.blocks.core.parsing;
import com.beligum.blocks.core.config.BlocksConfig;
import com.beligum.blocks.core.identifiers.ID;
import com.beligum.blocks.core.identifiers.RedisID;
import com.beligum.blocks.core.models.AbstractElement;
import com.beligum.blocks.core.models.PageClass;
import com.beligum.blocks.core.models.storables.Block;
import com.beligum.blocks.core.models.storables.Page;
import com.beligum.blocks.core.models.storables.Row;
import org.apache.commons.configuration.ConfigurationRuntimeException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.IOException;
import java.lang.String;import java.lang.StringBuilder;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;

/**
 * Created by bas on 30.09.14.
 * Parser class for parsing pages
 */
public class PageParser
{
    //the outer velocity-string of the page currently being parsed
    private String pageVelocity;
    //a set of all the blocks and rows of the page currently being parsed
    private Set<AbstractElement> elements;

    /**
     *  Default constructor
     */
    public PageParser()
    {
        this.pageVelocity = null;
        this.elements = new HashSet<>();
    }


    /**
     * Parse the default template-file of the page-class and return a PageClass-object, filled with it's blocks, rows and the most-outer velocity-string of the page-class
     * @param pageClassName the name of the page-class to be parsed (f.i. "default" for a pageClass filtered from the file "pages/default/index.html")
     * @return a page-class parsed from the "pages/<page-class-name>/index.html"
     * @throws PageParserException
     */
    public PageClass parsePageClass(String pageClassName) throws PageParserException
    {
        try {
            String templateFilename = this.getTemplatePath(pageClassName);
            File pageClassHtml = new File(templateFilename);
            //get the url used for identifying blocks and rows for this page-class
            URL pageClassURL = PageClass.getBaseUrl(pageClassName);
            //fill up this parser-class, with the elements and velocity filtered from the default template-file
            this.fill(pageClassHtml, pageClassURL);

            return new PageClass(pageClassName, this.elements, this.pageVelocity);
        }catch(IOException e){
            throw new ConfigurationRuntimeException("");
        }

    }

    /**
     *
     *
     * @param pageUrl url to the page which this parser will parse
     * @return a page-instance filled with the blocks and rows filtered from the url's htmlcontent
     */
    public Page parsePage(URL pageUrl) throws URISyntaxException

    {
        //TODO BAS: implement this
        return new Page(null, null);
    }

    /**
     * Parses a html-file to blocks and rows (containing velocity-variables) and fills this parser up with the found content.
     * After the parse, a string containing the velocity of this page will be saved in the field 'pageVelocity' and the found blocks and rows will be stored in the field 'elements'
     * @param htmlFile The file containing a html-tree
     * @param baseUrl the base-url which will be used to define the row- and block-ids
     * @return a set holding the blocks and rows that were parsed from the html-file (page)
     * @throws IOException
     */
    private void fill(File htmlFile, URL baseUrl) throws IOException, PageParserException
    {
        Document htmlDOM = Jsoup.parse(htmlFile, null);
        this.elements = recursiveParse(htmlDOM, baseUrl);
        this.pageVelocity = htmlDOM.outerHtml();
    }

    /**
     * Parses the tree starting with the node-element, looking for row- and block-elements and adding them to a set, which is returned
     * @param node root of the tree to be parsed
     * @param baseUrl the base-url used which will be used to define the row- and block-ids
     * @return a set holding blocks and rows
     */
    private Set<AbstractElement> recursiveParse(Element node, URL baseUrl) throws PageParserException
    {
        Set<AbstractElement> rowsAndBlocks = new HashSet<AbstractElement>();
        Elements children = node.children();
        for(Element child : children){
            //recursively iterate over the subtree starting with this child and add the found blocks and rows to the map
            rowsAndBlocks.addAll(recursiveParse(child, baseUrl));
            //TODO BAS: only can-edit and can-layout blocks and rows should be parsed
            boolean isRow = child.classNames().contains("row");
            boolean isBlock = child.classNames().contains("block");
            if(isRow || isBlock){
                if(child.id() != null && !child.id().isEmpty()) {
                    String childHtml = child.outerHtml();
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
                    AbstractElement element = isRow ? new Row(id, childHtml) : new Block(id, childHtml);
                    rowsAndBlocks.add(element);
                    child.replaceWith(new TextNode("\n ${" + child.id() + "}\n", ""));
                }
                else{
                    //if no id i
                    throw new PageParserException("A row- or block-element in the html-tree doesn't have an id, this shouldn't happen: \n" + child.outerHtml());
                }
            }
            else{
                //do nothing
            }
        }
        return rowsAndBlocks;
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
