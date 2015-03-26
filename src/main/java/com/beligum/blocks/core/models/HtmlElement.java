package com.beligum.blocks.core.models;

import com.beligum.blocks.core.config.ParserConstants;
import org.apache.commons.lang3.StringUtils;
import org.apache.velocity.runtime.directive.Parse;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Element;

import java.util.*;

/**
 * Created by wouter on 19/03/15.
 */
public class HtmlElement
{
    private String tag;
    private LinkedHashMap<String, String> attributes = new LinkedHashMap<String, String>();


    public HtmlElement() {
        this.tag = "div";
    }

    public HtmlElement(Element element) {
        this.tag = element.tagName();
        for (Attribute attribute: element.attributes().asList()) {
            this.attributes.put(attribute.getKey(), attribute.getValue());
        }
    }


    public HashMap<String, String> getAttributes() {
        return this.attributes;
    }

    public String getTag() {
        return this.tag;
    }
}
