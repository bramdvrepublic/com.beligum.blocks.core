package com.beligum.blocks.core.parsers.dynamicblocks;

import com.beligum.blocks.core.exceptions.ParseException;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;

import java.util.List;

/**
 * Created by bas on 15.01.15.
 * Interface representing a dynamic html-block.
 */
public interface DynamicBlock
{
    //TODO BAS: need to implement listener which checks if a typeof is a dynamic block and if so, calls the generateBlock-method
    /**
     * Main function of a dynamic block which generates it's html.
     * @param element
     * @throws ParseException
     */
    public Element generateBlock(Element element) throws ParseException;

    /**
     * Get the typeof corresponding to this dynamic block
     */
    public String getTypeOf();

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
