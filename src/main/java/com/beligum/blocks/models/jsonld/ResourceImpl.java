package com.beligum.blocks.models.jsonld;

import com.beligum.base.utils.Logger;
import com.beligum.blocks.base.Blocks;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

/**
 * Created by wouter on 23/04/15.
 */
public class ResourceImpl extends BlankNode implements Resource
{

    private HashMap<String, Node> internalObject = new HashMap<String, Node>();
    StringNode id = null;

    public ResourceImpl() {
    }

    public ResourceImpl(ResourceImpl resource) {
        this.wrap(resource.unwrap());
    }


    // We expect a valid absolute URI or a relative path from our RDF Schema
    // When we add a property we check if it starts with our domain
    // If it is a property from our namespace, remove the namespace
    public String fixPropertyName(String key) {
        if (!internalObject.keySet().contains(key)) {
            // Key does not exist as a shortened version
            // is this already a shortened Version?
            if (key.startsWith(Blocks.config().getDefaultRdfPrefix())) {
                int length = Blocks.config().getDefaultRdfPrefix().toString().length();
                key = key.substring(length);
            }
        }
        return key;
    }

    public void add(String key, Node node) {
        key = fixPropertyName(key);
        if (!node.isNull() && key != null) {

            if (key.equals(JsonLDGraph.ID)) {
                if (node.isString()) {
                    this.id = (StringNode) node;
                }
            }
            else {

                if (!internalObject.containsKey(key)) {
                    internalObject.put(key, node);
                }
                else if (internalObject.get(key).isList()) {
                    ((ListNode) internalObject.get(key)).add(node);
                }
                else {
                    ListNode list = new ListNode(internalObject.get(key));
                    list.add(node);
                    internalObject.put(key, list);
                }

            }
        }
    }

    public void set(String key, Node node) {
        key = fixPropertyName(key);
        if (!node.isNull() && key != null) {
            if (key.equals(JsonLDGraph.ID)) {
                if (node.isString())
                    this.id = (StringNode) node;
            }
            else {
                internalObject.put(key, node);
            }
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

    public Resource getResource(String key)
    {
        Node retVal = this.get(key);
        if (retVal != null && retVal.isResource())
            return (ResourceImpl)retVal;
        else
            return  null;
    }

    public Set<String> getFields() {
        return this.internalObject.keySet();
    }

    public Resource copy() {
        ResourceImpl retVal = new ResourceImpl();
        for (String key: internalObject.keySet()) {
            Node fieldNode = internalObject.get(key);
            retVal.add(key, fieldNode.copy());
        }
        return retVal;
    }

}
