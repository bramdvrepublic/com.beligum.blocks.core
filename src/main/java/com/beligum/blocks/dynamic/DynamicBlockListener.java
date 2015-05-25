package com.beligum.blocks.dynamic;

import com.beligum.blocks.exceptions.ParseException;
import com.beligum.blocks.models.BasicTemplate;
import com.beligum.blocks.models.StoredTemplate;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.util.List;

/**
 * Created by bas on 15.01.15.
 * Interface representing a dynamic html-block.
 */
public interface DynamicBlockListener
{

    /**
     * Main function of a dynamic block which generates it's html.
     *
     * @throws ParseException
     */
    public StringBuilder render(BasicTemplate basicTemplate);

    /**
     * function that allows to tamper block before save.
     *
     * @throws ParseException
     */
    public void save(StoredTemplate storedTemplate);

    /**
     * Get the blueprint type corresponding to this dynamic block
     */
    public String getType();

    /**
     * @return an ordered list with all link-nodes needed to renderContent this dynamic block
     */
    public List<Element> getLinks();

    /**
     * @return an ordered list with all script-nodes needed to renderContent this dynamic block
     */
    public List<Element> getScripts();

}
