package com.beligum.blocks.core.parsers.dynamicblocks;

import com.beligum.blocks.core.config.BlocksConfig;
import com.beligum.blocks.core.config.ParserConstants;
import com.beligum.blocks.core.exceptions.LanguageException;
import com.beligum.blocks.core.exceptions.ParseException;
import com.beligum.blocks.core.internationalization.Languages;
import com.beligum.blocks.core.parsers.TemplateParser;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.Elements;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by bas on 15.01.15.
 * Class representing a dynamic block, which will generate translation-urls for all preferred languages.
 */
public class TranslationList implements DynamicBlock
{
    private static final String ACTIVE_CLASS = "active";

    private URL pageUrl;
    private String activeLanguage;

    /**
     *
     * @param activeLanguage the language to be shown as 'active' in the generated html
     * @param pageUrl the url we want a list of translation-links of
     */
    public TranslationList(String activeLanguage, URL pageUrl)
    {
        this.pageUrl = pageUrl;
        this.activeLanguage = activeLanguage;
    }

    /**
     * Generate a list with links in all preferred languages for the url of this TranslationList
     * @param rootElement The node the list will be the only child of
     * @return The specified node, now with a list as it's only child
     * @throws ParseException
     */
    public Element generateBlock(Element rootElement) throws ParseException
    {
        try {
            String template = "<ul>\n";
            for(String language : BlocksConfig.getLanguages()){
                template += getListItem(language);
            }
            template += "</ul>\n";
            /*
             * The only child of the root-node will be the ul-tag, all others will be removed
             */
            Element listRoot = TemplateParser.parse(template).child(0);
            rootElement.empty();
            rootElement.appendChild(listRoot);
            return rootElement;
        }catch(Exception e){
            throw new ParseException("Could not generate a translation-block at \n \n" + rootElement + "\n \n", e);
        }
    }

    public String getTypeOf(){
        return ParserConstants.DynamicBlocks.TRANSLATION_LIST;
    }

    /**
     * @return an ordered list with all link-nodes needed to render this dynamic block
     */
    @Override
    public List<Element> getLinks()
    {
        //a translation-list doesn't need any css-files to be rendered
        return new ArrayList<>();
    }
    /**
     * @return an ordered list with all script-nodes needed to render this dynamic block
     */
    @Override
    public List<Element> getScripts()
    {
        //a translation-list doesn't need any javascript-files to be rendered
        return  new ArrayList<>();
    }
    private String getListItem(String language) throws LanguageException
    {
        //if we're dealing with a translation list, we simple want the links to be a link of this page, translated into the specified language
        String link = Languages.translateUrl(this.pageUrl.toString(), language);
        if(language.equals(activeLanguage)){
            return "<li><a href=\"" + link +"\" class=\"" + ACTIVE_CLASS + "\" title=\"\" >" + language + "</a></li>\n";
        }
        else {
            return "<li><a href=\"" + link +"\" title=\"\">" + language + "</a></li>\n";
        }
    }

}
