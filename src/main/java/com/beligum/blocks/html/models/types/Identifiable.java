package com.beligum.blocks.html.models.types;

import com.beligum.blocks.html.parsers.AbstractParser;
import org.jsoup.nodes.Element;

/**
 * Created by wouter on 22/11/14.
 */
public class Identifiable
{

    private String property;
    private String type;
    private String parentType;
    private String lang;
    private int propertyPosition;

    public Identifiable(Element element, Element parentContent, int propertyPosition) {
        this.propertyPosition = propertyPosition;
        this.type = AbstractParser.getType(element);
        if (parentContent != null) {
            this.parentType = AbstractParser.getType(parentContent);
        }
        this.property = AbstractParser.getProperty(element);
    }

    public String getProperty() {
        return this.property;
    }

    public String getParentType() {
        return this.parentType;
    }

    public String getType() {
        return this.type;
    }

    public String getPropertyName() {
        return this.type + "-" + this.getProperty();
    }

    public String getUniquePropertyName() {
        if (propertyPosition > 1) {
            return this.getPropertyName() + propertyPosition;
        } else {
            return this.getPropertyName();
        }
    }

    public String getLanguage() {
        return this.lang;
    }

}
