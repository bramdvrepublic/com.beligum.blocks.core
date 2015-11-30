package com.beligum.blocks.models;

import com.beligum.blocks.models.interfaces.Node;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

/**
 * Created by wouter on 26/08/15.
 */
public class ListNode extends NodeImpl
{
    List<Node> nodeList;

    public ListNode(Object value, Locale lang)
    {
        super(value, lang);
    }

    public ListNode(Locale lang)
    {
        super(new ArrayList<Node>(), lang);
    }

    @Override
    protected void setValue(Object value)
    {
        try {
            nodeList = (List<Node>) value;
            this.value = nodeList;
        }
        catch (Exception e) {
            super.setValue(value);
        }
    }

    @Override
    public boolean isIterable()
    {
        boolean retVal = false;
        if (nodeList != null) {
            retVal = true;
        }
        return retVal;
    }

    @Override
    public Iterator<Node> iterator()
    {
        Iterator<Node> retVal = null;
        if (isIterable()) {
            retVal = nodeList.iterator();
        }
        else {
            retVal = super.iterator();
        }
        return retVal;
    }

    public void add(Node node)
    {
        if (node.isIterable()) {
            for (Node n : node) {
                this.add(n);
            }
        }
        else {
            nodeList.add(node);
        }
    }
}
