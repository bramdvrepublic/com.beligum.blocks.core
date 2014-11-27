package com.beligum.blocks.html;

import com.beligum.blocks.core.config.BlocksConfig;
import com.beligum.blocks.core.models.storables.Entity;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.parser.Tag;

import java.util.List;

/**
 * Created by wouter on 20/11/14.
 */
public class PageTemplate
{
    private String template;
    private String name;
    private boolean htmlSeen = false;


    public PageTemplate(Element node) {
        super();
        Element parent = node.parent();
        while (parent.parent() != null) {
            if (parent.tagName().equals("html")) {
                name = parent.attr("template");
            }
            parent = parent.parent();
        }
        Node e = new TextNode("${" + BlocksConfig.TEMPLATE_ENTITY_VARIABLE + "}", BlocksConfig.getSiteDomain());
        node.replaceWith(e);

        this.template = parent.outerHtml();
    }



    public String getName() {
        return this.name;
    }

    public boolean isTemplate() {
        boolean retVal = false;
        if (this.name != null) retVal = true;
        return retVal;
    }

    public String renderContent(Entity entity) {
//        Element filledTemplate = this.tempContent.clone();
//        List<Element> nodes = filledTemplate.select("div[template-content]");
//        Element node = nodes.get(0);
//        node.replaceWith(element);
        // TODO render content with velocity
        return "";
    }



}
