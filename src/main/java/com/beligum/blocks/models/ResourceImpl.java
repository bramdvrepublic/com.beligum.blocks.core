package com.beligum.blocks.models;

import com.beligum.blocks.config.BlocksConfig;
import com.beligum.blocks.config.ParserConstants;
import com.beligum.blocks.controllers.interfaces.PersistenceController;
import com.beligum.blocks.models.factories.ResourceFactoryImpl;
import com.beligum.blocks.models.interfaces.Node;
import com.beligum.blocks.models.interfaces.Resource;
import com.beligum.blocks.models.interfaces.ResourceFactory;
import com.beligum.blocks.utils.RdfTools;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.joda.time.LocalDateTime;
import org.w3c.dom.NodeList;

import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.*;

/**
 * Created by wouter on 21/06/15.
 */
public class ResourceImpl extends AbstractNode implements Resource
{
    private Object dbId;
    private URI blockId;
    private Set<URI> rdfType = new HashSet<>();
    private Map<String, String> context = new HashMap<>();
    private Map<String, Map<Locale, ArrayList<Node>>> properties = new HashMap<>();

    public ResourceImpl() {
        this(Locale.ROOT);
    }

    public ResourceImpl(Locale language) {
        this.language = language;
    }

    @Override
    public URI getBlockId()
    {
        URI retVal = null;
        if (this.blockId != null) {
            retVal = this.blockId;
        }
        return retVal;
    }

    @Override
    public void setBlockId(URI id)
    {
        this.blockId = id;
    }

    @Override
    public Set<URI> getRdfType()
    {
        Set<URI> retVal = new HashSet<>();
        if (this.rdfType != null) {
            retVal = this.rdfType;
        }
        return retVal;
    }

    @Override
    public void setRdfType(URI uri)
    {
        this.rdfType = new HashSet();
        this.rdfType.add(uri);
    }

    @Override
    public void addRdfType(URI uri)
    {
        if (this.rdfType == null) {
            this.rdfType = new HashSet<>();
        }
        this.rdfType.add(uri);
    }



    @Override
    public void add(URI field, Node node)
    {
        String key = addFieldToContext(field, node.getLanguage());
        if (node.isIterable()) {
            for (Node n: node) {
                this.add(field, n);
            }
        } else {
            Map<Locale, ArrayList<Node>> value = properties.get(key);
            ArrayList<Node> list = value.get(node.getLanguage());
            if (list == null) {
                list = new ArrayList<Node>();
                list.add(node);
            } else if (!(list.contains(node))) {
                list.add(node);
            }

            properties.get(key).put(node.getLanguage(), list);
        }
    }

    @Override
    public void set(URI field, Node node)
    {
        String key = addFieldToContext(field, node.getLanguage());
        Map<Locale, ArrayList<Node>> value = properties.get(key);
        if (value.get(node.getLanguage()).size() > 0) {
            value.put(node.getLanguage(), new ArrayList<Node>());
        }

        add(field, node);
    }

    @Override
    public Node get(URI field)
    {
        return get(field, this.language);
    }

    @Override
    public Node get(URI field, Locale locale)
    {
        List<Node> list = null;
        String key = RdfTools.makeDbFieldFromUri(field);
        if (this.properties.containsKey(key)) {

            if (this.properties.get(key).containsKey(locale)) {
                list = this.properties.get(key).get(locale);
            } else if (this.properties.get(key).containsKey(Locale.ROOT)) {
                list = this.properties.get(key).get(Locale.ROOT);
            }
        }

        return returnValidNode(list, locale);
    }

    @Override
    public Node get(String field)
    {
        return get(field, this.language);
    }

    @Override
    public Node get(String field, Locale locale)
    {
        if (this.context.containsKey(field)) {
            field = this.context.get(field);
        }
        URI key = UriBuilder.fromUri(field).build();
        return get(key);
    }

    @Override
    public Node remove(URI field)
    {
        return remove(field, this.getLanguage());
    }

    @Override
    public Node remove(URI field, Locale locale)
    {
        String key = RdfTools.makeDbFieldFromUri(field);
        Map<Locale, ArrayList<Node>> values = this.properties.get(key);

        ArrayList<Node> list = null;
        if (values != null && values.containsKey(locale)) {
            list = values.remove(locale);
        }

        // Remove this property from context when no values are available in all languages
        if (properties.get(key) != null && properties.get(key).size() ==0) {
            removeFieldFromContext(key);
            properties.remove(key);
        }

        return returnValidNode(list, locale);
    }

    @Override
    public Object getValue()
    {
        ArrayList<Map<String, ArrayList<Node>>> retVal = new ArrayList<>();
        //        retVal.add(properties);
        return retVal;
    }
    @Override
    protected void setValue(Object value)
    {

    }
    //
    //    @Override
    //    public void setFieldDirect(String key, Object value, Locale locale)
    //    {
    //        Map<String, Object> vertex = localizedProperties;
    //        if (locale.equals(Locale.ROOT) || getFactory().isResource(value)) {
    //            vertex = this.properties;
    //        }
    //
    //        if (value != null) {
    //            if (getFactory().isResource(value)) {
    //                this.properties.put(key, new ArrayList<>());
    //                this.addFieldDirect(key, value, locale);
    //            } else {
    //                vertex.put(key, value);
    //            }
    //        }
    //    }
    //
    //    @Override
    //    public Node getFieldDirect(String key)
    //    {
    //        Locale lang = this.getLanguage();
    //        Node fieldValue = null;
    //        Map<String, Object> vertex = localizedProperties;
    //        if (!vertex.containsKey(key)) {
    //            vertex = this.properties;
    //            lang = Locale.ROOT;
    //        }
    //
    //        fieldValue = ResourceFactoryImpl.instance().createNode(vertex.get(key), lang);
    //
    //        return fieldValue;
    //    }
    //    @Override
    //    public void addFieldDirect(String key, Object value, Locale locale)
    //    {
    //        Map<String, Object> vertex = localizedProperties;
    //        Object existingField = null;
    //
    //        if (locale.equals(Locale.ROOT) || getFactory().isResource(value)) {
    //            vertex = this.properties;
    //        }
    //
    //        existingField = vertex.get(key);
    //
    //        if (value != null) {
    //            // we want to add so create a list for this property if there isn't a list yet
    //            if (existingField == null) {
    //                vertex.put(key, new ArrayList<Object>());
    //            } else if (getFactory().isResource(existingField) || !(existingField instanceof List)) {
    //                List<Object> t = new ArrayList<Object>();
    //                t.add(existingField);
    //                vertex.put(key, t);
    //            }
    //
    //
    //            if (getFactory().isResource(value)) {
    //                // Remove this resource if it was already added
    //                Iterator iterator = ((List)vertex.get(key)).iterator();
    //                while (iterator.hasNext()) {
    //                    Object existing = iterator.next();
    //                    URI id = getFactory().getResourceId(value);
    //                    if (getFactory().isResource(existing) && getFactory().getResourceId(existing).equals(id)) {
    //                        iterator.remove();
    //                    }
    //                }
    //                ((List)vertex.get(key)).add(value);
    //            } else if (value instanceof  Collection) {
    //                for (Object val : (Collection) value) {
    //                    addFieldDirect(key, val, locale);
    //                }
    //            } else {
    //                ((List)vertex.get(key)).add(value);
    //            }
    //
    //            // Clean up: flatten a list with only one value
    //            if (vertex.get(key) instanceof  List && ((List)vertex.get(key)).size() == 1) {
    //                vertex.put(key, ((List) vertex.get(key)).get(0));
    //            }
    //
    //        }
    //    }

    //
    //    @Override
    //    public Node removeFieldDirect(String key)
    //    {
    //        Locale lang = this.getLanguage();
    //        Object fieldValue = null;
    //        Map<String, ArrayList<Node>> vertex = localizedProperties;
    //        if (!vertex.containsKey(key)) {
    //            vertex = this.properties;
    //        }
    //
    //        if (vertex.containsKey(key)) {
    //            fieldValue = vertex.remove(key);
    //        }
    //
    //        return ResourceFactoryImpl.instance().createNode(fieldValue, lang);
    //    }

    @Override
    public Object getDBId()
    {
        return this.dbId;
    }

    @Override
    public void setDBId(Object id)
    {
        this.dbId = id;
    }

    @Override
    public Set<URI> getFields()
    {
        Set<URI>retVal = new HashSet<URI>();
        retVal.addAll(getLocalizedFields());
        retVal.addAll(getRootFields());

        return retVal;
    }
    @Override
    public Set<URI> getLocalizedFields()
    {
        Set<URI>retVal = new HashSet<URI>();
        Set<String> properties = this.properties.keySet();
        Map<String, String> context = getContext();
        for (String prop: properties) {
            if (context.containsKey(prop) && (!this.properties.get(prop).containsKey(Locale.ROOT) || this.properties.get(prop).size() > 1)) {
                retVal.add(UriBuilder.fromUri(this.getContext().get(prop)).build());
            }
        }
        return retVal;
    }
    @Override
    public Set<URI> getRootFields()
    {
        Set<URI>retVal = new HashSet<URI>();
        Set<String> properties = this.properties.keySet();

        // fetch context first to prevent ConcurrentModifictionException
        Map<String, String> context = getContext();
        for (String prop: properties) {
            if (context.containsKey(prop) && this.properties.get(prop).containsKey(Locale.ROOT)) {
                retVal.add(UriBuilder.fromUri(this.getContext().get(prop)).build());
            }
        }
        return retVal;
    }

    @Override
    public void setLanguage(Locale locale)
    {
        this.language = locale;
    }


    @Override
    public Map<String, String> getContext() {

        return this.context;
    }
    @Override
    public boolean isEmpty()
    {
        return this.properties.size() == 0;
    }

    @Override
    public Set<Locale> getLocalesForField(URI field)
    {
        String key = RdfTools.makeDbFieldFromUri(field);
        Set<Locale> retVal = null;
        if (this.properties.containsKey(key)) {
            retVal = this.properties.get(key).keySet();
        } else {
            retVal = new HashSet<>();
        }
        return retVal;
    }

    protected void removeFieldFromContext(String shortFieldName)
    {

        Map<String, String> context = this.getContext();
        if (context.containsKey(shortFieldName)) {
            context.remove(shortFieldName);
        }
    }

    protected String addFieldToContext(URI field, Locale locale)
    {

        // Create a short field name
        String shortFieldName = RdfTools.makeDbFieldFromUri(field);

        Map<String, String> context = this.getContext();
        if (!context.containsKey(shortFieldName)) {
            context.put(shortFieldName, field.toString());
        }


        // Put this property also in the map, if it does not yet exist
        if (!this.properties.containsKey(shortFieldName)) {
            this.properties.put(shortFieldName, new HashMap<Locale, ArrayList<Node>>());
        }

        Map<Locale, ArrayList<Node>> value = this.properties.get(shortFieldName);
        if (!value.containsKey(locale)) {
            value.put(locale, new ArrayList<Node>());
        }

        return shortFieldName;
    }

    @Override
    public boolean isResource() {
        return true;
    }


    @Override
    public void setCreatedAt(LocalDateTime date)
    {

    }
    @Override
    public LocalDateTime getCreatedAt()
    {
        return null;
    }
    @Override
    public void setCreatedBy(String user)
    {

    }
    @Override
    public String getCreatedBy()
    {
        return null;
    }
    @Override
    public void setUpdatedAt(LocalDateTime date)
    {

    }
    @Override
    public LocalDateTime getUpdatedAt()
    {
        return null;
    }
    @Override
    public void setUpdatedBy(String user)
    {

    }
    @Override
    public String getUpdatedBy()
    {
        return null;
    }
    @Override
    public ResourceFactory getFactory()
    {
        return ResourceFactoryImpl.instance();
    }

    @Override
    public String toString() {
        return "A resource";
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
            return true;
        if (!(o instanceof ResourceImpl))
            return false;

        ResourceImpl nodes = (ResourceImpl) o;

        return blockId.equals(nodes.blockId);

    }
    @Override
    public int hashCode()
    {
        return blockId.hashCode();
    }


    // -------- PROTECTED METHODS -----------

    protected Node returnValidNode(List<Node> list, Locale locale) {
        Node retVal = null;
        if (list == null) {
            retVal = ResourceFactoryImpl.instance().createNode(null, locale);
        } else if (list.size() == 1) {
            retVal = list.get(0);
        } else {
            retVal = new ListNode(list, locale);
        }


        // Set the language of the resource as the expected language
        if (retVal.isResource()) {
            ((Resource)retVal).setLanguage(language);
        }
        return retVal;
    }
}
