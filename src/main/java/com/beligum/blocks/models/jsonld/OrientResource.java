package com.beligum.blocks.models.jsonld;

import com.beligum.blocks.base.Blocks;
import com.beligum.blocks.config.ParserConstants;
import com.beligum.blocks.models.jsonld.interfaces.Node;
import com.beligum.blocks.models.jsonld.interfaces.Resource;
import com.beligum.blocks.utils.UrlTools;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Vertex;

import java.util.*;

/**
 * Created by wouter on 13/05/15.
 **/
public class OrientResource implements Resource
{
    // The vertex that this resource represents
    protected ODocument localizedResource;
    protected Locale language;

    // The default resource that this localized resource translates
    // if not this is corrected on save.
    protected ODocument defaultResource;
    protected HashMap<String, HashMap<String, String>> context;


    // Resource can only be created by resourceFactory
    public OrientResource(ODocument defaultVertex, ODocument localizedResource) {
        if (localizedResource != null) {
            this.language = Blocks.config().getLocaleForLanguage((String) localizedResource.field(Resource.LANGUAGE));
        } else {
            this.language = Locale.ROOT;
        }

        this.localizedResource = localizedResource;
        this.defaultResource = defaultVertex;
    }

    // -------- GETTERS AND SETTERS ---------
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
    @Override
    public String getBlockId()
    {
        return this.defaultResource.field(Resource.ID);
    }
    @Override
    public void setBlockId(String id)
    {
        this.defaultResource.field(Resource.ID, id);
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
    @Override
    public Locale getLanguage()
    {
        Locale retVal = this.language;
        if (retVal == null) {
            if (localizedResource == null) {
                retVal = Locale.ROOT;
            } else {
                retVal = Blocks.config().getLocaleForLanguage((String) localizedResource.field(Resource.LANGUAGE));
            }
        }
        return retVal;
    }


    // ------- FUNCTIONS FOR RESOURCE INTERFACE ------

    @Override
    public void add(String key, Node node)
    {
        if (!node.isNull()) {
            if (node.isResource()) {
                add(this.defaultResource, key, node);
            }
            else {
                if (this.localizedResource != null && node.getLanguage().getLanguage().equals(this.language.getLanguage())) {
                    // add to default
                    add(this.localizedResource, key, node);
                }
                else if (node.getLanguage().getLanguage().equals(Locale.ROOT.getLanguage())) {
                    // add to translation
                    add(this.defaultResource, key, node);
                } else {
                    Resource otherLocalized = OrientResourceFactory.instance().createResource(this.getBlockId(), node.getLanguage());
                    otherLocalized.set(key, node);
                }
            }
        }
    }


    @Override
    public void set(String key, Node node)
    {
        if (!node.isNull()) {
            ODocument vertex = defaultResource;
            if (node.isResource()) {
                ArrayList<ODocument> list = new ArrayList<>();
                list.add((ODocument)node.getValue());
                vertex.field(key, list);
            } else if (this.localizedResource != null && node.getLanguage().getLanguage().equals(this.language.getLanguage())) {
                vertex = this.localizedResource;
                vertex.field(key, node.getValue());
            } else if (node.getLanguage().equals(Locale.ROOT)) {
                vertex.field(key, node.getValue());
            } else {
                Resource otherLocalized = OrientResourceFactory.instance().createResource(this.getBlockId(), node.getLanguage());
                otherLocalized.set(key, node);
            }
        }
    }

    @Override
    public Node getFirst(String key)
    {
        Node node = get(key);
        if (node.isIterable()) {
            Iterator<Node> iterator = node.getIterable().iterator();
            if (iterator.hasNext()) {
                return iterator.next();
            } else {
                return OrientResourceFactory.instance().asNode("", this.language);
            }
        } else {
            return node;
        }
    }

    @Override
    public Node get(String key)
    {
        Locale lang = this.getLanguage();
        Object fieldValue = null;
        if (this.localizedResource != null) {
            fieldValue = this.localizedResource.field(key);
        }

        if (fieldValue == null) {
            fieldValue = this.defaultResource.field(key);
            lang = Locale.ROOT;
        }

        return OrientResourceFactory.instance().asNode(fieldValue, lang);
    }



    public Resource getResource(String key)
    {
        Iterable<Resource> fieldValue = getResources(key);
        Iterator<Resource> it = fieldValue.iterator();
        Resource retVal = null;
        if (it.hasNext()) retVal = it.next();
        return retVal;
    }


    public Iterable<Resource> getResources(String key)
    {
        Iterable<Resource> retVal = new ResourceIterable((Iterable)this.defaultResource.field(key), this.getLanguage());
        return retVal;
    }


    @Override
    public boolean isEmpty()
    {
        return false;
    }
    @Override
    public Node remove(String key)
    {
        Locale lang = this.getLanguage();
        Object fieldValue = null;
        if (this.localizedResource != null) {
            fieldValue = this.localizedResource.field(key);
        }

        if (fieldValue == null) {
            fieldValue = this.defaultResource.field(key);
            lang = Locale.ROOT;
            if (fieldValue != null) {
                fieldValue = this.defaultResource.removeField(key);
            }
        } else {
            fieldValue = this.localizedResource.removeField(key);
        }

        return OrientResourceFactory.instance().asNode(fieldValue, lang);
    }

    @Override
    public Set<String> getFields()
    {
        Set<String> retVal = new HashSet(Arrays.asList(this.defaultResource.fieldNames()));
        if (this.localizedResource != null) {
            retVal.addAll(Arrays.asList(this.localizedResource.fieldNames()));
        }
        return retVal;
    }

    public ODocument getDefaultVertex()
    {
        return this.defaultResource;
    }

    public Resource getLocalizedResource(Locale locale) {
        return OrientResourceFactory.instance().createResource(this.getBlockId(), locale);
    }

    @Override
    public Resource copy()
    {
        return null;
    }
    @Override
    public HashMap<String, Node> unwrap()
    {
        return null;
    }
    @Override
    public void wrap(HashMap<String, Node> unwrappedResource)
    {

    }
    @Override
    public void merge(Resource resource)
    {

    }

    // -------- PRIVATE METHODS ---------
    private void add(ODocument vertex, String key, Node node)
    {
        Object existingField = vertex.field(key);
        if (!node.isNull()) {
            if (existingField == null) {
                if (node.isResource()) {
                    List<Object> t = new ArrayList<Object>();
                    t.add(node.getValue());
                    vertex.field(key, t);
                } else {
                    vertex.field(key, node.getValue());
                }
            }
            else if (existingField instanceof List) {
                List valueList = ((List) existingField);
                if (node.isIterable()) {
                    for (Node val: node.getIterable())
                        valueList.add(val);
                } else {
                    valueList.add(node.getValue());
                }
            }
            else {
                List newValues = new ArrayList();
                newValues.add(existingField);
                newValues.add(node.getValue());
                vertex.field(key, newValues);
            }
        }
    }

//    private String validateContextForNode(String field, Node node) {
//        // Get or create a context for this resource
//        if (context == null) {
//            if (this.defaultResource.field(ParserConstants.JSONLD_CONTEXT) == null) {
//                this.context = new HashMap<String, HashMap<String, String>>();
//                this.defaultResource.field(ParserConstants.JSONLD_CONTEXT, this.context);
//            } else {
//                this.context = (HashMap<String, HashMap<String, String>>)this.defaultResource.field(ParserConstants.JSONLD_CONTEXT);
//            }
//        }
//
//        // Create a short field name
//        String shortFieldName = field;
//        int index = Math.max(Math.max(field.lastIndexOf("/"), field.lastIndexOf("#")), field.lastIndexOf(":"));
//        if (index > 0) {
//            shortFieldName = field.substring(index+1);
//        } else  {
//            field = UrlTools.createLocalType(shortFieldName);
//        }
//
//        if (!this.context.containsKey(shortFieldName)) {
//            this.context.put(shortFieldName, new HashMap<String, String>());
//            this.context.put()
//        } else {
//
//        }
//
//
//    }



    @Override
    public Object getValue()
    {
        return getDefaultVertex();
    }


    // ------- Methods from Node interface that are less relevant for Resource -----


    @Override
    public boolean isString()
    {
        return false;
    }
    @Override
    public boolean isDouble()
    {
        return false;
    }
    @Override
    public boolean isLong()
    {
        return false;
    }
    @Override
    public boolean isBoolean()
    {
        return false;
    }
    @Override
    public boolean isInt()
    {
        return false;
    }
    @Override
    public boolean isIterable()
    {
        return false;
    }

    @Override
    public boolean isResource()
    {
        return true;
    }

    @Override
    public boolean isNull()
    {
        return false;
    }
    @Override
    public NodeIterable getIterable()
    {
        return null;
    }
    @Override
    public String asString()
    {
        return null;
    }


    @Override
    public Double getDouble()
    {
        return null;
    }
    @Override
    public Integer getInteger()
    {
        return null;
    }
    @Override
    public Boolean getBoolean()
    {
        return null;
    }
    @Override
    public Long getLong()
    {
        return null;
    }

    //     -------------   DEPRECATED -------------
    // temporarily kept for blueprints to keep working

    @Override
    public Boolean getBoolean(String key)
    {
        return null;
    }
    @Override
    public Integer getInteger(String key)
    {
        return null;
    }
    @Override
    public Long getLong(String key)
    {
        return null;
    }
    @Override
    public Double getDouble(String key)
    {
        return null;
    }
    @Override
    public String getString(String key)
    {
        return null;
    }
    @Override
    public Iterable<Node> getIterable(String key)
    {
        return null;
    }


}
