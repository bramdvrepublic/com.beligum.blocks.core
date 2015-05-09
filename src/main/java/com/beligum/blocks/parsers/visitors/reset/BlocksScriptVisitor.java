package com.beligum.blocks.parsers.visitors.reset;

import com.beligum.blocks.exceptions.ParseException;
import com.beligum.blocks.parsers.ScriptsLinksParser;
import com.beligum.blocks.parsers.visitors.BasicVisitor;
import org.jsoup.nodes.Node;

/**
 * Created by wouter on 18/03/15.
 */
public class BlocksScriptVisitor extends BasicVisitor
{
    private ScriptsLinksParser scriptsLinksParser;

    public BlocksScriptVisitor()
    {
        this.scriptsLinksParser = new ScriptsLinksParser();
    }

    @Override
    public Node head(Node node, int depth) throws ParseException
    {
        return this.scriptsLinksParser.parse(node);
    }

    @Override
    public Node tail(Node node, int depth) throws ParseException
    {
        return node;
    }

    public ScriptsLinksParser getScriptsLinksParser()
    {
        return this.scriptsLinksParser;
    }
}


