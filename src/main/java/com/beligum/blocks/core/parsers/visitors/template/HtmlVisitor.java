package com.beligum.blocks.core.parsers.visitors.template;

import com.beligum.blocks.core.base.Blocks;
import com.beligum.blocks.core.config.ParserConstants;
import com.beligum.blocks.core.exceptions.ParseException;
import com.beligum.blocks.core.models.StoredTemplate;
import com.beligum.blocks.core.parsers.ElementParser;
import com.beligum.blocks.core.parsers.redis.visitors.SuperVisitor;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;

import java.net.URL;
import java.util.ArrayList;

/**
 * Created by wouter on 20/03/15.
 */
public class HtmlVisitor extends SuperVisitor
{

    private StoredTemplate content = null;
    private ArrayList<StoredTemplate> other = new ArrayList<>();
    private URL htmlUrl = null;

    public HtmlVisitor(URL url) {
        this.htmlUrl = url;
    }

    @Override
    public Node head(Node node, int depth) throws ParseException
    {
        String language = Blocks.urlDispatcher().getLanguage(htmlUrl);
        Node retVal = node;
        if (node instanceof Element) {
            if (ElementParser.getReferenceUrl((Element)node) != null || ( ElementParser.isUseBlueprint((Element) node) && ElementParser.isSingleton((Element) node))) {
                // This is or a singleton blueprint or an entity
                this.other.add(Blocks.factory().createStoredTemplate((Element) node, language));
                this.addProperty((Element) node);
            } else if (ElementParser.isProperty((Element)node) || ElementParser.isUseBlueprint((Element) node)) {
                // this is probably the content of the template
                if (content != null) {
                    throw new ParseException("Template can only contain 1 content property");
                }
                this.content = Blocks.factory().createStoredTemplate((Element) node, this.htmlUrl);
                this.addProperty((Element) node);
            }  else if (ElementParser.isTypeOf((Element)node)) {
                // save this entity but this should not happen
                StoredTemplate storedTemplate = Blocks.factory().createStoredTemplate((Element) node, language);
                if (this.content == null) {
                    this.content = storedTemplate;
                } else {
                    this.other.add(storedTemplate);
                }
                this.addProperty((Element) node);
            }
        }
        return retVal;
    }

    public void addProperty(Element element) {
        if (!ElementParser.isProperty(element)) {
            element.attr(ParserConstants.PROPERTY, "");
        }
    }

    public StoredTemplate getContent() {
        return this.content;
    }

    public ArrayList<StoredTemplate> getOther() {
        return this.other;
    }

}
