package com.beligum.blocks.models;

import com.beligum.base.utils.Logger;
import com.beligum.blocks.config.BlocksConfig;
import com.beligum.blocks.config.ParserConstants;
import com.beligum.blocks.controllers.PersistenceControllerImpl;
import com.beligum.blocks.controllers.interfaces.PersistenceController;
import com.beligum.blocks.models.factories.ResourceFactoryImpl;
import com.beligum.blocks.models.interfaces.Node;
import com.beligum.blocks.models.interfaces.Resource;
import com.beligum.blocks.models.interfaces.ResourceFactory;
import org.joda.time.LocalDateTime;

import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.*;

/**
 * Created by wouter on 21/06/15.
 */
public class ResourceImpl extends AbstractResource
{
    private Object dbId;
    private Map<String, Object> vertex;
    private Map<String, Object> localized;



    public ResourceImpl(Map<String, Object> vertex, Map<String, Object> localized, Locale language) {
        this.vertex = vertex;
        this.localized = localized;
        this.localized.put(ParserConstants.JSONLD_LANGUAGE, language.getLanguage());
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
        if (locale.equals(Locale.ROOT) || getFactory().isResource(value)) {
            vertex = this.vertex;
        }

        if (value != null || !(value instanceof String && ((String)value).trim().equals(""))) {
            if (getFactory().isResource(value)) {
                this.vertex.put(key, new ArrayList<>());
                this.addFieldDirect(key, value, locale);
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
            //            lang = Locale.ROOT;
        }

        fieldValue = ResourceFactoryImpl.instance().createNode(vertex.get(key), lang);

        return fieldValue;
    }
    @Override
    public void addFieldDirect(String key, Object value, Locale locale)
    {
        Map<String, Object> vertex = localized;
        Object existingField = null;

        if (locale.equals(Locale.ROOT) || getFactory().isResource(value)) {
            vertex = this.vertex;
        }

        existingField = vertex.get(key);

        if (value != null || !(value instanceof String && ((String)value).trim().equals(""))) {
            // we want to add so create a list for this property if there isn't a list yet
            if (existingField == null) {
                vertex.put(key, new ArrayList<Object>());
            } else if (getFactory().isResource(existingField) || !(existingField instanceof List)) {
                List<Object> t = new ArrayList<Object>();
                t.add(existingField);
                vertex.put(key, t);
            }


            if (getFactory().isResource(value)) {
                // Remove this resource if it was already added
                Iterator iterator = ((List)vertex.get(key)).iterator();
                while (iterator.hasNext()) {
                    Object existing = iterator.next();
                    URI id = getFactory().getResourceId(value);
                    if (getFactory().isResource(existing) && getFactory().getResourceId(existing).equals(id)) {
                        iterator.remove();
                    }
                }
                ((List)vertex.get(key)).add(value);
            } else if (value instanceof  Collection) {
                for (Object val : (Collection) value) {
                    addFieldDirect(key, val, locale);
                }
            } else {
                ((List)vertex.get(key)).add(value);
            }

            // Clean up: flatten a list with only one value
            if (vertex.get(key) instanceof  List && ((List)vertex.get(key)).size() == 1) {
                vertex.put(key, ((List) vertex.get(key)).get(0));
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

        return ResourceFactoryImpl.instance().createNode(fieldValue, lang);
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
        Locale retVal = null;
        String language = (String)localized.get(ParserConstants.JSONLD_LANGUAGE);
        if (language != null) {
            retVal = BlocksConfig.instance().getLocaleForLanguage(language);
        }
        if (retVal == null){
            retVal = BlocksConfig.instance().getDefaultLanguage();
        }

        return retVal;
    }

    @Override
    public void setLanguage(Locale locale) {
        localized.put(ParserConstants.JSONLD_LANGUAGE, locale.getLanguage());
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

}
