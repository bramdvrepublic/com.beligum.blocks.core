package com.beligum.blocks.html.models.types;

import com.beligum.blocks.core.config.BlocksConfig;
import com.beligum.blocks.html.Cacher.TypeCacher;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Tag;

import java.util.List;

/**
 * Created by wouter on 20/11/14.
 */
public class Template
{
    private Element tempContent;
    private String name;
    private boolean htmlSeen = false;


    public Template(Element node) {
        super();
        Element parent = node.parent();
        while (parent.parent() != null) {
            if (parent.tagName().equals("html")) {
                name = parent.attr("template");
            }
            parent = parent.parent();
        }
        Element e = new Element(Tag.valueOf("div"), "");
        e.attr("template-content", "");
        node.replaceWith(e);

        this.tempContent = parent;
        TypeCacher.instance().addTemplate(this, false);
    }



    public String getName() {
        return this.name;
    }

    public boolean isTemplate() {
        boolean retVal = false;
        if (this.name != null) retVal = true;
        return retVal;
    }

    public String renderContent(Element element) {
        Element filledTemplate = this.tempContent.clone();
        List<Element> nodes = filledTemplate.select("div[template-content]");
        Element node = nodes.get(0);
        node.replaceWith(element);
        return filledTemplate.outerHtml();
    }



}
