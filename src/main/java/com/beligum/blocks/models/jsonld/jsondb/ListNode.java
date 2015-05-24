package com.beligum.blocks.models.jsonld.jsondb;

import com.beligum.blocks.models.jsonld.interfaces.Node;

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


    public boolean isIterable() {
        return true;
    }

    public ArrayList<Node> getIterable() {
        return this.internalObject;
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
