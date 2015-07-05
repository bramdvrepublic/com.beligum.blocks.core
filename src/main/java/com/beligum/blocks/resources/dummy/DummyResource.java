package com.beligum.blocks.resources.dummy;

import com.beligum.blocks.config.BlocksConfig;
import com.beligum.blocks.config.ParserConstants;
import com.beligum.blocks.database.DummyBlocksController;
import com.beligum.blocks.database.interfaces.BlocksController;
import com.beligum.blocks.resources.AbstractResource;
import com.beligum.blocks.resources.interfaces.Node;
import com.beligum.blocks.resources.interfaces.Resource;
import org.joda.time.LocalDateTime;

import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.*;

/**
 * Created by wouter on 21/06/15.
 */
public class DummyResource extends AbstractResource
{
    private Object dbId;
    private Map<String, Object> vertex;
    private Map<String, Object> localized;

    public DummyResource(Map<String, Object> vertex, Map<String, Object> localized) {
        this.vertex = vertex;
        this.localized = localized;
    }

    public DummyResource(Map<String, Object> vertex, Map<String, Object> localized, Locale language) {
        this(vertex, localized);
        this.localized.put(ParserConstants.JSONLD_LANGUAGE, language);
        this.language = language;
    }

    @Override
    public Object getValue()
    {
        ArrayList<Map<String, Object>> retVal = new ArrayList<>();
        retVal.add(vertex);
        retVal.add(localized);
        return retVal;
    }
    @Override
    public void setFieldDirect(String key, Object value, Locale locale)
    {
        Map<String, Object> vertex = localized;
        if (locale.equals(Locale.ROOT) || (value instanceof HashMap && ((HashMap) value).containsKey(ParserConstants.JSONLD_ID))) {
            vertex = this.vertex;
        }

        if (value != null) {
            if (value instanceof HashMap && ((HashMap) value).containsKey(ParserConstants.JSONLD_ID)) {
                ArrayList<HashMap> list = new ArrayList<>();
                list.add((HashMap)value);
                vertex.put(key, list);
            } else {
                vertex.put(key, value);
            }
        }
    }
    @Override
    public Node getFieldDirect(String key)
    {
        Locale lang = this.getLanguage();
        Node fieldValue = null;
        Map<String, Object> vertex = localized;
        if (!vertex.containsKey(key)) {
            vertex = this.vertex;
            lang = Locale.ROOT;
        }

        if (vertex != null) {
            fieldValue = getDatabase().createNode(vertex.get(key), lang);
        }

        return fieldValue;
    }
    @Override
    public void addFieldDirect(String key, Node node)
    {
        Map<String, Object> vertex = localized;
        Object existingField = null;
        if (node.getLanguage().equals(Locale.ROOT) || node.isResource()) {
            vertex = this.vertex;
        }

        existingField = vertex.get(key);

        if (!node.isNull()) {
            if (existingField == null) {
                if (node.isResource()) {
                    List<Object> t = new ArrayList<Object>();
                    t.add(node.getValue());
                    vertex.put(key, t);
                } else {
                    vertex.put(key, node.getValue());
                }
            }
            else if (existingField instanceof List) {
                List valueList = ((List) existingField);
                if (node.isIterable()) {
                    for (Node val: node)
                        valueList.add(val.getValue());
                } else {
                    valueList.add(node.getValue());
                }
                vertex.put(key, valueList);
            }
            else {
                List newValues = new ArrayList();
                newValues.add(existingField);
                newValues.add(node.getValue());
                vertex.put(key, newValues);
            }
        }
    }


    // Just for import
    public void addToLocale(URI field, Resource resource) {
        String key = addFieldToContext(field);

        if (this.localized != null) {
            if (!this.localized.containsKey(key)) {
                this.localized.put(key, new ArrayList());
            }
            ((List)this.localized.get(key)).add(resource.getValue());
        }
    }

    @Override
    public Node removeFieldDirect(String key)
    {
        Locale lang = this.getLanguage();
        Object fieldValue = null;
        Map<String, Object> vertex = localized;
        if (!vertex.containsKey(key)) {
            vertex = this.vertex;
        }

        if (vertex.containsKey(key)) {
            fieldValue = vertex.remove(key);
        }

        return getDatabase().createNode(fieldValue, lang);
    }

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
        Set<String> properties = this.localized.keySet();
        HashMap<String, String> context = getContext();
        for (String prop: properties) {
            if (context.containsKey(prop)) {
                retVal.add(UriBuilder.fromUri(this.getContext().get(prop)).build());
            }
        }
        return retVal;
    }
    @Override
    public Set<URI> getRootFields()
    {
        Set<URI>retVal = new HashSet<URI>();
        Set<String> properties = this.vertex.keySet();

        // fetch context first to prevent ConcurrentModifictionException
        HashMap<String, String> context = getContext();
        for (String prop: properties) {
            if (context.containsKey(prop)) {
                retVal.add(UriBuilder.fromUri(this.getContext().get(prop)).build());
            }
        }
        return retVal;
    }

    @Override
    public Locale getLanguage() {
        Locale retVal = this.language;
        if (retVal == null) {
            String language = (String)localized.get(ParserConstants.JSONLD_LANGUAGE);
            retVal = BlocksConfig.instance().getLocaleForLanguage(language);
            this.language = retVal;
        }
        return retVal;
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
    public BlocksController getDatabase()
    {
        return DummyBlocksController.instance();
    }

    @Override
    public String toString() {
        return "A resource";
    }

}
