package com.beligum.blocks.models.jsonld;

import java.io.StringWriter;
import java.util.*;

/**
 * Created by wouter on 23/04/15.
 */
public class ResourceNode extends BlankNode implements ResourceNodeInf
{

    private HashMap<String, Node> internalObject = new HashMap<String, Node>();
    StringNode id = null;

    public ResourceNode() {

    }

    public ResourceNode(ResourceNode resource) {
        this.wrap(resource.unwrap());
    }

    public void add(String key, Node node) {
        if (node.isNull()) return;

        if (key.equals(JsonLDGraph.ID)) {
            if (node.isString()) {
                this.id = (StringNode)node;
            }
        } else {

            if (!internalObject.containsKey(key)) {
                internalObject.put(key, node);
            } else if (internalObject.get(key).isList()) {
                ((ListNode)internalObject.get(key)).add(node);
            } else {
                ListNode list = new ListNode(internalObject.get(key));
                list.add(node);
                internalObject.put(key, list);
            }

        }
    }

    public void set(String key, Node node) {
        if (node.isNull()) return;

        if (key.equals(JsonLDGraph.ID)) {
            if (node.isString()) this.id = (StringNode)node;
        } else {
            internalObject.put(key, node);
        }
    }

    public void remove(String key) {
        internalObject.remove(key);
    }

    @Override
    public boolean isNull()
    {
        return this.internalObject == null;
    }

    public Node getFirst(String key) {
        if (key == null) return new BlankNode();
        Node retVal = new BlankNode();
        if (key.equals(JsonLDGraph.ID)) {
            retVal = this.id;
        } else {
            Node node = internalObject.get(key);
            if (node != null && node.isList()) {
                retVal = node.getList().get(0);
            } else if (node != null) {
                retVal = node;
            }
        }
        return retVal;
    }

    public Node get(String key) {
        Node retVal = null;
        if (key != null) {
            retVal = internalObject.get(key);
        }

        if (retVal == null) {
            retVal = new BlankNode();
        }
        return retVal;
    }

    public String getId() {
        String retVal = null;
        if (id != null) retVal = id.getString();
        return retVal;
    }

    public void setId(String id) {
        this.id = new StringNode(id);
    }


    public boolean isEmpty() {
        return internalObject.keySet().size() == 0;
    }

    public HashMap<String, Node> unwrap() {
        return this.internalObject;
    }

    public void wrap(HashMap<String, Node> unwrappedResource) {
        this.internalObject = unwrappedResource;
    }

    @Override
    public boolean isResource()
    {
        return true;
    }


    public void addBoolean(String key, Boolean value) {
        BooleanNode node = new BooleanNode(value);
        add(key, node);
    }

    public void addInteger(String key, Integer value) {
        IntegerNode node = new IntegerNode(value);
        add(key, node);
    }

    public void addLong(String key, Long value) {
        LongNode node = new LongNode(value);
        add(key, node);
    }

    public void addDouble(String key, Double value) {
        DoubleNode node = new DoubleNode(value);
        add(key, node);
    }

    public void addString(String key, String value, String language) {
        StringNode node = new StringNode(value, language);
        add(key, node);
    }

    public void setBoolean(String key, Boolean value) {
        BooleanNode node = new BooleanNode(value);
        set(key, node);
    }

    public void setInteger(String key, Integer value) {
        IntegerNode node = new IntegerNode(value);
        set(key, node);
    }

    public void setLong(String key, Long value) {
        LongNode node = new LongNode(value);
        set(key, node);
    }

    public void setDouble(String key, Double value) {
        DoubleNode node = new DoubleNode(value);
        set(key, node);
    }

    public void setString(String key, String value, String language) {
        StringNode node = new StringNode(value, language);
        set(key, node);
    }

    public void setString(String key, String value) {
        StringNode node = new StringNode(value);
        set(key, node);
    }


    public Boolean getBoolean(String key) {
        return getFirst(key).getBoolean();
    }

    public Integer getInteger(String key) {
        return getFirst(key).getInteger();
    }

    public Long getLong(String key) {
        return getFirst(key).getLong();
    }

    public Double getDouble(String key) {
        return getFirst(key).getDouble();
    }

    public String getString(String key) {
        return getFirst(key).getString();
    }

    public ArrayList<Node> getList(String key) {
        return get(key).getList();
    }

    public ResourceNode getResource(String key)
    {
        Node retVal = this.get(key);
        if (retVal != null && retVal.isResource())
            return (ResourceNode)retVal;
        else
            return  null;
    }

    public Set<String> getFields() {
        return this.internalObject.keySet();
    }

    public ResourceNode copy() {
        ResourceNode retVal = new ResourceNode();
        for (String key: internalObject.keySet()) {
            Node fieldNode = internalObject.get(key);
            retVal.add(key, fieldNode.copy());
        }
        return retVal;
    }


    public void write(StringWriter writer, boolean expanded) {
        writer.append("{");
        boolean added = false;
        if (JsonLDGraph.ID != null) {
            writer.append(JsonLDGraph.ID).append(": ").append(getId());
        }
        for (String key: internalObject.keySet()) {
            Node value = internalObject.get(key);
            if (added) {
                writer.append(", ");
            }
            writer.append(key);
            writer.append(": ");
            value.write(writer, expanded);
            added = true;

        }

        writer.append("}");
    }

}
