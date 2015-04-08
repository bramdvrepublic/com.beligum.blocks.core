package com.beligum.blocks.dynamic;

import com.beligum.blocks.config.ParserConstants;
import com.beligum.blocks.base.Blocks;
import com.beligum.blocks.exceptions.ParseException;
import com.beligum.blocks.models.BasicTemplate;
import com.beligum.blocks.models.StoredTemplate;
import com.beligum.blocks.utils.URLFactory;
import org.jsoup.nodes.Element;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by bas on 15.01.15.
 * Class representing a dynamic block, which will generate translation-urls for all preferred languages.
 */
public class TranslationList implements DynamicBlockListener
{
    private static final String ACTIVE_CLASS = "active";


    public TranslationList()
    {
    }

    /**
     * Generate a list with links in all preferred languages for the url of this TranslationList
     * @return The specified node, now with a list as it's only child
     * @throws ParseException
     */
    public StringBuilder render(BasicTemplate basicTemplate)
    {
        StringBuilder retVal = new StringBuilder();
        retVal.append(basicTemplate.renderStartElement(true, false));
        retVal.append("<ul>");
        for(String language : Blocks.config().getLanguages()){
            getListItem(retVal, language, basicTemplate.getLanguage());
        }
        retVal.append("</ul>");
        retVal.append(basicTemplate.renderEndElement());

        return retVal;

    }

    public void save(StoredTemplate storedTemplate) {

    }

    public String getType(){
        return URLFactory.makeAbsoluteRdfValue(ParserConstants.DynamicBlocks.TRANSLATION_LIST);
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

    private void getListItem(StringBuilder retVal, String language, String activeLanguage)
    {
        //if we're dealing with a translation list, we simple want the links to be a link of this page, translated into the specified language

        if(language.equals(activeLanguage)){
            retVal.append("<li><a href=\"").append(" ").append("\" class=\"").append(ACTIVE_CLASS).append("\" title=\"\" >").append(language).append("</a></li>");
        }
        else {
            retVal.append("<li><a href=\"").append(" ").append("\" title=\"\">").append(language).append("</a></li>");
        }
    }

}
