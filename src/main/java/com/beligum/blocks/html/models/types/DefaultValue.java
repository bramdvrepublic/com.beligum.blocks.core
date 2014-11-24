package com.beligum.blocks.html.models.types;

import com.beligum.blocks.html.parsers.AbstractParser;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Tag;

/**
 * Created by wouter on 22/11/14.
 */
public class DefaultValue extends Identifiable
{
    protected boolean canLayout = false;
    protected boolean canReplace = false;
    protected boolean canEdit = false;
    protected boolean canEditInline = false;

    protected Element parsedContent;
    protected Element parent;

    public DefaultValue(Element parsedElement, Element parentContent, int propertyNr) {
        super(parsedElement, parentContent, propertyNr);

        this.parent = parentContent;
        this.parsedContent = parsedElement;
        /*
        * Content can be 3 things:
        *   - just a field with a name (a property)
        *   - a big field that can be layouted and that can contain many 'Things'
        *   - a predefined type
        * */

    }

    public Element getParsedContent() {
        return this.parsedContent;
    }

    public String getParsedContentAsHtml() {
        return this.parsedContent.html();
    }

    public boolean isBlueprint() {
        return AbstractParser.isBlueprint(this.parsedContent);
    }

    public boolean canLayout() {
        return AbstractParser.isLayoutable(this.parsedContent);
    }

    public boolean canReplace() {
        return AbstractParser.isReplaceable(this.parsedContent);

    }

    public boolean canEdit() {
        return AbstractParser.isEditable(this.parsedContent);
    }

    public boolean canEditInline() {
         return AbstractParser.isInlineEditable(this.parsedContent);
    }




    public boolean isMutable() {
        return canLayout || canReplace || canEdit || canEditInline;
    }

    @Override
    public String toString() {
        return super.toString();
    }
}
