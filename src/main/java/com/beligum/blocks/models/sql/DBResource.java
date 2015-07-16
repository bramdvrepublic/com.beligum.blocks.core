package com.beligum.blocks.models.sql;

import com.beligum.blocks.config.BlocksConfig;
import com.beligum.blocks.models.ResourceImpl;
import com.beligum.blocks.models.interfaces.Resource;
import com.beligum.blocks.models.jackson.resource.ResourceLocalizedSerializer;
import com.beligum.blocks.models.jackson.resource.ResourceRootSerializer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.dataformat.smile.SmileFactory;

import javax.persistence.*;
import java.util.*;

/**
 * Created by wouter on 29/06/15.
 */

@Entity
@Table(name="resource")
public class DBResource extends DBDocumentInfo
{

    public static final ObjectMapper mapper = new ObjectMapper(new SmileFactory());
//    public static final ObjectMapper localizedMapper = new ObjectMapper(new SmileFactory());
//    public static final ObjectMapper rootMapper = new ObjectMapper(new SmileFactory());


    protected String blockId;

    @Lob
    protected byte[] rootData;

    protected String default_language;

    // Flag to know when the value of one of ourroot properties changed
    // This alerts us to update all translations in ElasticSearch
    @Transient
    protected boolean updatedRoot = false;

//    @MapKey(name = "language")
    @OneToMany(mappedBy="resource", cascade = CascadeType.ALL)
    protected Map<String, DBResourceLocalized> localized  = new HashMap<String, DBResourceLocalized>();

    // Default constructor for hibernate
    public DBResource() {

    }


    public DBResource(Resource resource) throws JsonProcessingException
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
        HashMap rootDataMap = mapper.readValue(this.rootData, HashMap.class);
        HashMap<String, Object> localData = new HashMap();
        if (localized != null) {
            localData = mapper.readValue(localized.getData(), HashMap.class);
        }
        retVal = new ResourceImpl(rootDataMap, localData, locale);
        return retVal;
    }

    public void setResource(Resource resource) throws JsonProcessingException
    {
        String language = resource.getLanguage().getLanguage();

        HashMap<String, Object> rootValues = (HashMap<String, Object>)((List)resource.getValue()).get(0);
        HashMap<String, Object> localValues = (HashMap<String, Object>)((List)resource.getValue()).get(1);
        byte[] newRootData = mapper.writeValueAsBytes(rootValues);
        if (!newRootData.equals(this.rootData)) {
            this.rootData = newRootData;
            this.updatedRoot = true;
        }

        DBResourceLocalized localized;
        if (this.localized.containsKey(language)) {
            localized = this.localized.get(language);
            localized.setdata(mapper.writeValueAsBytes(localValues));
        } else {
            localized = new DBResourceLocalized(mapper.writeValueAsBytes(localValues));
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


}
