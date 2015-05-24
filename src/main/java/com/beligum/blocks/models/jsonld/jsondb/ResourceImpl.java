package com.beligum.blocks.models.jsonld.jsondb;

import com.beligum.base.utils.Logger;
import com.beligum.blocks.base.Blocks;
import com.beligum.blocks.config.ParserConstants;
import com.beligum.blocks.models.jsonld.JsonLDGraph;
import com.beligum.blocks.models.jsonld.OrientNode;
import com.beligum.blocks.models.jsonld.interfaces.Node;
import com.beligum.blocks.models.jsonld.interfaces.Resource;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.orient.OrientGraph;
import com.tinkerpop.blueprints.impls.orient.OrientVertex;

import java.util.*;

/**
 * Created by wouter on 23/04/15.
 */
public class ResourceImpl extends OrientNode implements Resource
{
    private HashMap<String, Node> internalObject = new HashMap<String, Node>();
    StringNode id = null;

    public ResourceImpl() {
    }

    public ResourceImpl(Resource resource) {
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
                else if (internalObject.get(key).isIterable()) {
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
            if (key.equals(JsonLDGraph.ID))
            {
                if (node.isString())
                    this.id = (StringNode) node;
            }
            else if (key.equals(ParserConstants.JSONLD_TYPE)) {
                if (node.isString())
                {

                } else if (node.isIterable())
                {

                } else
                {
                    // This is not a valid type
                    Logger.error("Type not added to resource. Type is not valid. Use setRdfType().");
                }
            }
            else
            {
                internalObject.put(key, node);
            }
        }
    }

    public Node remove(String key) {
        return internalObject.remove(key);
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
            if (node != null && node.isIterable()) {
                Iterator<Node> it = node.getIterable().iterator();
                if (it.hasNext()) {
                    retVal = it.next();
                } else {
                    retVal = new BlankNode();
                }
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

    @Override
    public Object getDBId()
    {
        return null;
    }

    @Override
    public Object setDBId()
    {
        return null;
    }

    public String getBlockId() {
        String retVal = null;
        if (id != null) retVal = id.asString();
        return retVal;
    }

    public void setBlockId(String id) {
        this.id = new StringNode(id);
    }

    @Override
    public Node getRdfType()
    {
        return null;
    }

    @Override
    public void setRdfType(Node node)
    {

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
    public void merge(Resource resource)
    {
    }

    @Override
    public boolean isResource()
    {
        return true;
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
        return getFirst(key).asString();
    }


    public Iterable<Node> getIterable(String key) {
        return get(key).getIterable();
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
