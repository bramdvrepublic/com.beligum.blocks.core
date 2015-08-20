package com.beligum.blocks.models.sql;

import com.beligum.base.utils.Logger;
import com.beligum.blocks.config.BlocksConfig;
import com.beligum.blocks.config.ParserConstants;
import com.beligum.blocks.models.ResourceImpl;
import com.beligum.blocks.models.factories.ResourceFactoryImpl;
import com.beligum.blocks.models.interfaces.Node;
import com.beligum.blocks.models.interfaces.Resource;
import com.beligum.blocks.models.jackson.resource.ResourceLocalizedSerializer;
import com.beligum.blocks.models.jackson.resource.ResourceRootSerializer;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.dataformat.smile.SmileFactory;

import javax.persistence.*;
import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.net.URI;
import java.util.*;

/**
 * Created by wouter on 29/06/15.
 */

@Entity
@Table(name="resource")
public class DBResource extends DBDocumentInfo
{

    public static final ObjectMapper mapper = new ObjectMapper();
    public static final ObjectMapper localizedMapper = new ObjectMapper().registerModule(new SimpleModule().addSerializer(Resource.class, new ResourceLocalizedSerializer()));
    public static final ObjectMapper rootMapper = new ObjectMapper().registerModule(new SimpleModule().addSerializer(Resource.class, new ResourceRootSerializer()));

    public static final Map<String, Resource> tempcache = new HashMap<String, Resource>();

    protected String blockId;

    @Lob
    protected byte[] rootData;

    protected String default_language;

    // Flag to know when the value of one of ourroot properties changed
    // This alerts us to update all translations in ElasticSearch
    @Transient
    protected boolean updatedRoot = false;

    @Transient
    // referenve to all linked resources to prevent an endless loop with
    // calls to db while linking self-referencing resources
    protected Map<String, Resource> linkedResources = new HashMap<String, Resource>();

    //    @MapKey(name = "language")
    @OneToMany(mappedBy="resource", cascade = CascadeType.ALL)
    protected Map<String, DBResourceLocalized> localized  = new HashMap<String, DBResourceLocalized>();

    // Default constructor for hibernate
    public DBResource() {

    }


    public DBResource(Resource resource) throws IOException
    {
        this.blockId = resource.getBlockId().toString();
        if (default_language == null) {
            BlocksConfig.instance().getDefaultLanguage().getLanguage();
        }
        setResource(resource);
    }

    // ------ GETTERS AND SETTERS ----------

    public Long getId() {
        return this.id;
    }


    public boolean hasLanguage(Locale locale) {
        return this.localized.containsKey(locale.getLanguage());
    }

    public boolean hasUpdatedRoot() {
        return this.updatedRoot;
    }

    // ---------PUBLIC METHODS -----------

    public Resource getResource(Locale locale) throws Exception
    {
        Resource retVal = null;
        DBResourceLocalized localized = getLocalizedResource(locale);
        HashMap<String, Object> rootDataMap = mapper.readValue(this.rootData, HashMap.class);
        HashMap<String, Object> localData = new HashMap();
        if (localized != null) {
            localData = mapper.readValue(localized.getData(), HashMap.class);
        }

        retVal = new ResourceImpl(new HashMap<String, Object>(), new HashMap<String, Object>(), locale);
        addToCache(this.blockId, retVal);


        HashMap<String, String> context = (HashMap<String, String>)rootDataMap.get(ParserConstants.JSONLD_CONTEXT);
        for (String key: rootDataMap.keySet()) {
            if (key.equals(ParserConstants.JSONLD_CONTEXT)) {
                // Do nothing, context is generated by the resource itself while adding values

            } else if (key.equals(ParserConstants.JSONLD_ID)) {
                retVal.setBlockId(UriBuilder.fromUri((String)rootDataMap.get(ParserConstants.JSONLD_ID)).build());
            } else if (key.equals(ParserConstants.JSONLD_TYPE)) {
                List<String> types = (List<String>)rootDataMap.get(ParserConstants.JSONLD_TYPE);
                for (String type: types) {
                    retVal.addRdfType(UriBuilder.fromUri(type).build());
                }
            } else {
                Object value = rootDataMap.get(key);
                URI fieldname = UriBuilder.fromUri((String) ((Map) rootDataMap.get(ParserConstants.JSONLD_CONTEXT)).get(key)).build();
                addValueToResource(retVal, fieldname, value, locale, Locale.ROOT);
            }
        }

        context = (HashMap<String, String>)localData.get(ParserConstants.JSONLD_CONTEXT);
        for (String key : localData.keySet()) {
            if (!key.equals(ParserConstants.JSONLD_CONTEXT)) {
                try {
                    Object value = localData.get(key);
                    URI fieldname = UriBuilder.fromUri((String) context.get(key)).build();
                    addValueToResource(retVal, fieldname, value, locale, locale);
                }
                catch (Exception e) {
                    Logger.error("Could not find key ion context " + key, e);
                }
            }
        }
        emptyCache();
        return retVal;
    }


    // Serializes the resource
    public void setResource(Resource resource) throws IOException
    {
        String language = resource.getLanguage().getLanguage();

//        //
//        Resource rootResource = getRootResource(resource.getLanguage());
//        for (URI key: resource.getRootFields()) {
//            rootResource.set(key, resource.get(key));
//        }

        byte[] newRootData = rootMapper.writeValueAsBytes(resource);
        if (!newRootData.equals(this.rootData)) {
            this.rootData = newRootData;
            this.updatedRoot = true;
        }

        DBResourceLocalized localized;
        if (this.localized.containsKey(language)) {
            localized = this.localized.get(language);
            localized.setdata(localizedMapper.writeValueAsBytes(resource));
        } else {
            localized = new DBResourceLocalized(localizedMapper.writeValueAsBytes(resource));
        }

        localized.setResource(this);
        if (language != null && !"".equals(language.trim())) {
            this.localized.put(language, localized);
        }
    }

    public Set<Locale> getLanguages() {
        Set<Locale> retVal = new HashSet<>();
        for (String lang: this.localized.keySet()) {
            Locale locale = BlocksConfig.instance().getLocaleForLanguage(lang);
            retVal.add(locale);
        }
        return retVal;
    }


    // ------- PROTECTED METHODS -----------

    protected DBResourceLocalized getLocalizedResource(Locale locale) {
        DBResourceLocalized localized = null;
        if (this.localized.containsKey(locale.getLanguage())) {
            localized = this.localized.get(locale.getLanguage());
        }
        return localized;
    }


    protected void addValueToResource(Resource resource, URI key, Object value, Locale resourceLocale, Locale valueLocale) {
      try {
          if (ResourceFactoryImpl.instance().isResource(value)) {
              URI id = ResourceFactoryImpl.instance().getResourceId(value);
              Resource resourceValue = getFromCache(id.toString());
              if (resourceValue == null) {
                  resourceValue = ResourceFactoryImpl.instance().getResource(value, resourceLocale);
              }
              resource.add(key, resourceValue);
          }
          else if (value instanceof List) {
              for (Object item : (List) value) {
                  if (!(item instanceof List)) {
                      addValueToResource(resource, key, item, resourceLocale, valueLocale);
                  }
                  else {
                      // Todo: this can happen but causes a bug a the moment so we skip this case until bug is found.
                      Logger.error("Error: list in a list");
                  }
              }
          }
          else {
              Node nodeValue = ResourceFactoryImpl.instance().createNode(value, valueLocale);
              resource.add(key, nodeValue);
          }
      } catch (Exception e) {
          Logger.error("");
      }
    }

    protected Resource getRootResource(Locale locale) throws IOException
    {
        Resource retVal = new ResourceImpl(new HashMap<String, Object>(), new HashMap<String, Object>(), locale);

        if (this.rootData != null) {
            HashMap<String, Object> rootDataMap = mapper.readValue(this.rootData, HashMap.class);

            for (String key : rootDataMap.keySet()) {
                if (key.equals(ParserConstants.JSONLD_CONTEXT)) {
                    // Do nothing, context is generated by the resource itself while adding values
                }
                else if (key.equals(ParserConstants.JSONLD_ID)) {
                    retVal.setBlockId(UriBuilder.fromUri((String) rootDataMap.get(ParserConstants.JSONLD_ID)).build());
                }
                else if (key.equals(ParserConstants.JSONLD_TYPE)) {
                    List<String> types = (List<String>) rootDataMap.get(ParserConstants.JSONLD_TYPE);
                    for (String type : types) {
                        retVal.addRdfType(UriBuilder.fromUri(type).build());
                    }
                }
                else {
                    Object value = rootDataMap.get(key);
                    URI fieldname = UriBuilder.fromUri((String) ((Map) rootDataMap.get(ParserConstants.JSONLD_CONTEXT)).get(key)).build();
                    addValueToResource(retVal, fieldname, value, locale, Locale.ROOT);
                }
            }
        }
        return retVal;
    }

    /*
    * These are temporary functions to cache resources when resources are selfreferencing, so we still
    * deserialize self-referencing resources. But this has to be improved
    * TODO: create a better system. When do we clear the cache???
    * */

    protected Resource getFromCache(String id) {
        return tempcache.get(id);
    }

    protected void addToCache(String id, Resource resource) {
        if (!tempcache.containsKey(id)) {
            tempcache.put(id, resource);
        }
    }

    protected void emptyCache() {
        tempcache.clear();
    }


}
