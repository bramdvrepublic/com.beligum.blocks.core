package com.beligum.blocks.html.models;

import com.beligum.blocks.html.Cacher.TypeCacher;
import com.beligum.blocks.html.db.BlocksDBFactory;
import com.beligum.blocks.html.models.ifaces.EntityID;
import com.beligum.blocks.html.models.types.DefaultValue;
import com.beligum.blocks.html.models.types.Storable;
import com.beligum.blocks.html.parsers.AbstractParser;
import com.beligum.blocks.html.parsers.FillingNodeVisitor;
import org.jsoup.nodes.Element;

import java.util.List;

/**
 * Created by wouter on 21/11/14.
 */
public class Content extends Storable
{

    public Content(Element parsedContent, Element parentContent, int propertyNr) {
        super(parsedContent, parentContent, propertyNr);
        this.parsedContent = parsedContent;
        // Dient om content op te slaan in DB en content op te halen uit DB


    }


    public String getFullContentAsHtml() {
        String retVal = "";
        if (this.parsedContent != null) {
            retVal = this.getFullContent().html();
        }
        return retVal;
    }

    public Element getFullContent() {
        Element filledContent = this.getParsedContent().clone();
        filledContent.traverse(new FillingNodeVisitor());
        return filledContent;
    }

}
