package com.beligum.blocks.core.dynamic;

import com.beligum.blocks.core.exceptions.ParseException;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;

import java.util.List;

/**
 * Created by bas on 15.01.15.
 * Interface representing a dynamic html-block.
 */
public interface DynamicBlockListener
{

    /**
     * Main function of a dynamic block which generates it's html.
     * @param element
     * @throws ParseException
     */
    public Element onShow(Element element) throws ParseException;

    /**
     * function that allows to tamper block before save.
     * @param element
     * @throws ParseException
     */
    public Element onSave(Element element) throws ParseException;

    /**
     * Get the blueprint type corresponding to this dynamic block
     */
    public String getType();

    /**
     *
     * @return an ordered list with all link-nodes needed to render this dynamic block
     */
    public List<Element> getLinks();

    /**
     *
     * @return an ordered list with all script-nodes needed to render this dynamic block
     */
    public List<Element> getScripts();

}
