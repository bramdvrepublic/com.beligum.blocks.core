package com.beligum.blocks.core.dynamic;

import com.beligum.blocks.core.URLMapping.XMLUrlIdMapper;
import com.beligum.blocks.core.config.BlocksConfig;
import com.beligum.blocks.core.config.ParserConstants;
import com.beligum.blocks.core.exceptions.IDException;
import com.beligum.blocks.core.exceptions.LanguageException;
import com.beligum.blocks.core.exceptions.ParseException;
import com.beligum.blocks.core.exceptions.UrlIdMappingException;
import com.beligum.blocks.core.identifiers.BlocksID;
import com.beligum.blocks.core.internationalization.Languages;
import com.beligum.blocks.core.parsers.TemplateParser;
import org.jsoup.nodes.Element;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by bas on 15.01.15.
 * Class representing a dynamic block, which will generate translation-urls for all preferred languages.
 */
public class TranslationList implements DynamicBlockListener
{
    private static final String ACTIVE_CLASS = "active";

    private URL entityUrl;
    private String activeLanguage;

    /**
     *
     * @param activeLanguage the language to be shown as 'active' in the generated html
     * @param entityUrl the url we want a list of translation-links of
     */
    public TranslationList(String activeLanguage, URL entityUrl)
    {
        this.entityUrl = entityUrl;
        this.activeLanguage = activeLanguage;
    }

    /**
     * Generate a list with links in all preferred languages for the url of this TranslationList
     * @param rootElement The node the list will be the only child of
     * @return The specified node, now with a list as it's only child
     * @throws ParseException
     */
    public Element onShow(Element rootElement) throws ParseException
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

    public Element onSave(Element element) {
        return element;
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
    private String getListItem(String language) throws LanguageException, UrlIdMappingException, IDException
    {
        //if we're dealing with a translation list, we simple want the links to be a link of this page, translated into the specified language
        BlocksID id = BlocksID.renderLanguagedId(this.entityUrl, BlocksID.NO_VERSION, language);
        URL url = XMLUrlIdMapper.getInstance().getUrl(id);
        String link = Languages.translateUrl(url.toString(), language)[0];
        if(language.equals(activeLanguage)){
            return "<li><a href=\"" + link +"\" class=\"" + ACTIVE_CLASS + "\" title=\"\" >" + language + "</a></li>\n";
        }
        else {
            return "<li><a href=\"" + link +"\" title=\"\">" + language + "</a></li>\n";
        }
    }

}
