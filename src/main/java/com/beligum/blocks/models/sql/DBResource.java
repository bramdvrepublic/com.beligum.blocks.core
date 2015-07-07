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
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Created by wouter on 29/06/15.
 */

@Entity
@Table(name="resource")
public class DBResource extends DBDocumentInfo
{

    public static final ObjectMapper mapper = new ObjectMapper(new SmileFactory());
    public static final ObjectMapper localizedMapper = new ObjectMapper(new SmileFactory()).registerModule(new SimpleModule().addSerializer(Resource.class, new ResourceLocalizedSerializer()));
    public static final ObjectMapper rootMapper = new ObjectMapper(new SmileFactory()).registerModule(new SimpleModule().addSerializer(Resource.class, new ResourceRootSerializer()));


    protected String blockId;

    @Lob
    protected byte[] rootData;

    protected String default_language;

//    @MapKey(name = "language")
    @OneToMany(mappedBy="resource", cascade = CascadeType.ALL)
    protected Map<String, DBResourceLocalized> localized  = new HashMap<String, DBResourceLocalized>();

    // Default constructor for hibernate
    public DBResource() {

    }

    public void setResource(Resource resource) throws JsonProcessingException
    {
        String language = resource.getLanguage().getLanguage();
        this.rootData = rootMapper.writeValueAsBytes(resource);
        DBResourceLocalized localized = new DBResourceLocalized(localizedMapper.writeValueAsBytes(resource));
        localized.setResource(this);
        if (language != null && !"".equals(language.trim())) {
            this.localized.put(language, localized);
        }
    }

    public DBResource(Resource resource) throws JsonProcessingException
    {
        this.blockId = resource.getBlockId().toString();
        if (default_language == null) {
            BlocksConfig.instance().getDefaultLanguage().getLanguage();
        }


        setResource(resource);
    }

    public Long getId() {
        return this.id;
    }

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

    public boolean hasLanguage(Locale locale) {
        return this.localized.containsKey(locale.getLanguage());
    }

    protected DBResourceLocalized getLocalizedResource(Locale locale) {
        DBResourceLocalized localized = null;
        if (this.localized.containsKey(locale.getLanguage())) {
            localized = this.localized.get(locale.getLanguage());
        }
        return localized;
    }

}
