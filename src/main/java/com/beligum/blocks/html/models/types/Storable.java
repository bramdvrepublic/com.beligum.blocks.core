package com.beligum.blocks.html.models.types;

import com.beligum.blocks.html.models.ifaces.EntityID;
import com.beligum.blocks.html.parsers.AbstractParser;
import org.jsoup.nodes.Element;

/**
 * Created by wouter on 23/11/14.
 */
public class Storable extends DefaultValue
{
    private EntityID id;

    public Storable(Element element, Element parentContent, int propertyPosition) {
        super(element, parentContent, propertyPosition);
        // We parsen een doc om er concrete content in te vinden en die op te slaan in DB
        // we geven een id indien er nog geen id gegeven is.

        // Enkel entity interesseert ons
        // Indien entity van de resource anders is dan entity van element
        // dan nieuwe resource opslaan.


        if (AbstractParser.hasResource(element)) {
            this.id = EntityID.parse(AbstractParser.getResource(element));
        } else {
            this.id = EntityID.parse(AbstractParser.getType(element));
        }

    }

    public String getDefaultEntity() {
        return "Thing";
    }

    public EntityID getId() {
        return this.id;
    }

}
