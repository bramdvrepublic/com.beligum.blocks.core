package com.beligum.blocks.models.jsonld;

import java.io.StringWriter;
import java.util.ArrayList;

/**
 * Created by wouter on 24/04/15.
 */
public class ListNode extends BlankNode
{
    private ArrayList<Node> internalObject = new ArrayList<>();

    public ListNode(ArrayList<Node> list) {
        if (list == null) {
            internalObject = new ArrayList<>();
        } else {
            this.internalObject = list;
        }
    }

    @Override
    public boolean isNull()
    {
        return this.internalObject == null;
    }

    public ListNode(Node node) {
        this.internalObject.add(node);
    }

    public void add(Node node) {
        this.internalObject.add(node);
    }


    public boolean isList() {
        return true;
    }

    public ArrayList<Node> getList() {
        return this.internalObject;
    }

    @Override
    public void write(StringWriter writer, boolean expanded)
    {
        writer.append("[");
        boolean added = false;
        for (Node node: internalObject) {
            if (added) {
                writer.append(", ");
            }
            node.write(writer, expanded);
            added = true;
        }
        writer.append("]");

    }



    @Override
    public Node copy()
    {
        ArrayList<Node> retVal = new ArrayList<>();
        for (Node node: internalObject) {
            retVal.add(node.copy());
        }
        return new ListNode(retVal);
    }
}
