package com.beligum.blocks.models;

import com.beligum.blocks.base.Blocks;
import com.beligum.blocks.config.ParserConstants;
import com.beligum.blocks.models.jsonld.jsondb.ResourceImpl;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Element;

import java.util.HashMap;
import java.util.Locale;

/**
* Created by wouter on 19/03/15.
*/
public class HtmlElement extends ResourceImpl
{
    public static final String tag = ParserConstants.BLOCKS_SCHEMA + "tag";
    public static final String attribute = ParserConstants.BLOCKS_SCHEMA + "attribute";
//    private String tag;

    private HashMap<String, String> attributes = new HashMap<>();


    public HtmlElement() {
        this.setTag("div");
    }

    public HtmlElement(ResourceImpl resource) {
        super(resource);
        for (String fullAttribute: this.unwrap().keySet()) {
            String attribute = fullAttribute.substring(fullAttribute.lastIndexOf("/")+1);
            this.attributes.put(attribute, this.unwrap().get(fullAttribute).asString());
        }
    }


    public HtmlElement(Element element) {
        this.setTag(element.tagName().toLowerCase());
        for (Attribute attribute: element.attributes().asList()) {
           this.attributes.put(attribute.getKey(), attribute.getValue());
           this.set(HtmlElement.attribute + "/" + attribute.getKey(), Blocks.resourceFactory().asNode(attribute.getValue(), Locale.ROOT));
        }
    }


    public String getTag() {
        return getString(HtmlElement.tag);
    }

    public void setTag(String tag) {
        set(HtmlElement.tag, Blocks.resourceFactory().asNode(tag, Locale.ROOT));
    }

    public HashMap<String, String> getAttributes() {
        return this.attributes;
    }

}
