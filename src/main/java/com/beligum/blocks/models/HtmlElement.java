package com.beligum.blocks.models;

import com.beligum.blocks.config.ParserConstants;
import com.beligum.blocks.models.jsonld.ResourceImpl;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Element;

import java.util.HashMap;

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
            this.attributes.put(attribute, this.unwrap().get(fullAttribute).getString());
        }
    }


    public HtmlElement(Element element) {
        this.setTag(element.tagName().toLowerCase());
        for (Attribute attribute: element.attributes().asList()) {
           this.attributes.put(attribute.getKey(), attribute.getValue());
           this.setString(HtmlElement.attribute + "/" + attribute.getKey(), attribute.getValue());
        }
    }


    public String getTag() {
        return getString(HtmlElement.tag);
    }

    public void setTag(String tag) {
        setString(HtmlElement.tag, tag);
    }

    public HashMap<String, String> getAttributes() {
        return this.attributes;
    }

}
