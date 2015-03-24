package com.beligum.blocks.core.mongo;

import com.beligum.blocks.core.URLMapping.simple.UrlDispatcher;
import com.beligum.blocks.core.config.BlocksConfig;
import com.beligum.blocks.core.dbs.AbstractBlockDatabase;
import com.beligum.blocks.core.dbs.BlocksDatabase;
import com.beligum.blocks.core.dbs.BlocksUrlDispatcher;
import com.beligum.blocks.core.exceptions.DatabaseException;
import com.beligum.blocks.core.exceptions.ParseException;
import com.beligum.blocks.core.identifiers.BlockId;
import com.beligum.blocks.core.identifiers.MongoID;
import com.beligum.blocks.core.models.nosql.*;
import com.beligum.core.framework.utils.Logger;
import com.mongodb.*;
import org.jongo.Find;
import org.jongo.Jongo;
import org.jongo.MongoCollection;
import org.jongo.MongoCursor;
import org.jongo.marshall.jackson.JacksonMapper;
import org.jsoup.nodes.Element;
import sun.jvm.hotspot.opto.Block;

import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Created by wouter on 17/03/15.
 */

//http://stackoverflow.com/questions/15789471/efficient-pojo-mapping-to-from-java-mongo-dbobject-using-jackson
public class MongoDatabase extends AbstractBlockDatabase
{
    public static final String TEMPLATE_COLLECTION = "templates";
    public static final String TEMPLATE_HISTORY_COLLECTION = "templates_history";
    public static final String ENTITY_COLLECTION = "entities";
    public static final String ENTITY_HISTORY_COLLECTION = "entities";
    public static final String SITEMAP_COLLECTION = "sitemap";
    public static final String SINGLETON_COLLECTION = "singletons";
    public static final String SINGLETON_HISTORY_COLLECTION = "singletons_history";
    public static final String BLUEPRINT_COLLECTION = "blueprints";
    public static final String BLUEPRINT_HISTORY_COLLECTION = "blueprints_history";
    public static final String PAGE_TEMPLATE_COLLECTION = "pagetemplates";
    public static final String PAGE_TEMPLATE_HISTORY_COLLECTION = "pagetemplates_history";




    private Jongo jongo;

    public MongoDatabase() throws UnknownHostException
    {

        // or
        MongoClient client = new MongoClient(BlocksConfig.getMongoHost() , BlocksConfig.getMongoPort() );


        DB db = client.getDB( "BLOCKS" );
        this.jongo = new Jongo(db,
                                new JacksonMapper.Builder()
                                                .build()
        );
    }

    @Override
    protected void doSaveEntity(Entity entity) {
        MongoCollection collection = null;
        if (doFetchTemplate(entity.getId()) == null || entity.getLanguage() == BlocksConfig.getDefaultLanguage()) {
            collection = jongo.getCollection(MongoDatabase.ENTITY_COLLECTION);
            collection.save(entity);
        }
        collection = jongo.getCollection(MongoDatabase.ENTITY_COLLECTION + "_" + entity.getLanguage());
        collection.save(entity);
    }

    @Override
    protected Entity doFetchEntity(BlockId id, String language) {
        Entity retVal = null;
        if (id != null && language != null) {
            MongoCollection entities = jongo.getCollection(MongoDatabase.ENTITY_COLLECTION + "_" + language);
            MongoCursor<MongoEntity> cursor = entities.find("{_id: '" + id.toString() + "'}").as(MongoEntity.class);
            if (cursor.hasNext()) {
                retVal = cursor.next();
            }
        }
        return retVal;
    }

    @Override
    protected Entity doFetchEntity(BlockId id) {
        Entity retVal = null;
        if (id != null) {
            MongoCollection entities = jongo.getCollection(MongoDatabase.ENTITY_COLLECTION);
            MongoCursor<MongoEntity> cursor = entities.find("{_id: '" + id.toString() + "'}").as(MongoEntity.class);
            if (cursor.hasNext()) {
                retVal = cursor.next();
            }
        }
        return retVal;
    }

    @Override
    protected void doSaveTemplate(StoredTemplate template) throws DatabaseException
    {
        MongoCollection collection = null;
        try {
            if (template instanceof Blueprint) {
                template.setId(new MongoID(template.getBlueprintName()));
                if (doFetchTemplate(template.getId()) == null || template.getLanguage() == BlocksConfig.getDefaultLanguage()) {
                    collection = jongo.getCollection(MongoDatabase.BLUEPRINT_COLLECTION);
                    generalSave(template, collection);
                }
                collection = jongo.getCollection(MongoDatabase.BLUEPRINT_COLLECTION + "_" + template.getLanguage());
                generalSave(template, collection);
            }
            else if (template instanceof PageTemplate) {
                template.setId(new MongoID(template.getBlueprintName()));
                if (doFetchTemplate(template.getId()) == null || template.getLanguage() == BlocksConfig.getDefaultLanguage()) {
                    collection = jongo.getCollection(MongoDatabase.PAGE_TEMPLATE_COLLECTION);
                    generalSave(template, collection);
                }
                collection = jongo.getCollection(MongoDatabase.PAGE_TEMPLATE_COLLECTION + "_" + template.getLanguage());
                generalSave(template, collection);
            }
            else if (template.isSingleton()) {
                template.setId(new MongoID(template.getSingleton()));
                if (doFetchTemplate(template.getId()) == null || template.getLanguage() == BlocksConfig.getDefaultLanguage()) {
                    collection = jongo.getCollection(MongoDatabase.SINGLETON_COLLECTION);
                    generalSave(template, collection);
                }
                collection = jongo.getCollection(MongoDatabase.SINGLETON_COLLECTION + "_" + template.getLanguage());
                generalSave(template, collection);
            }
            else {
                if (doFetchTemplate(template.getId()) == null || template.getLanguage() == BlocksConfig.getDefaultLanguage()) {
                    collection = jongo.getCollection(MongoDatabase.TEMPLATE_COLLECTION);
                    generalSave(template, collection);
                }
                collection = jongo.getCollection(MongoDatabase.TEMPLATE_COLLECTION + "_" + template.getLanguage());
                generalSave(template, collection);
            }
        } catch (Exception e) {
            throw new DatabaseException("Could not save template");
        }

    }

    private void generalSave(BlocksStorable storable, MongoCollection collection) throws DatabaseException
    {
        if (storable != null && collection != null) {
            try {
                collection.save(storable);
            }
            catch (Exception e) {
                Logger.error("Could not save", e);
                throw new DatabaseException("Could not save");
            }
        } else {
            throw new DatabaseException("Could not save");
        }
    }

    @Override
    protected StoredTemplate doFetchTemplate(BlockId id, String language) {
        StoredTemplate retVal = null;
        if (id != null && language != null) {
            MongoCollection entities = jongo.getCollection(MongoDatabase.TEMPLATE_COLLECTION + "_" + language);
            MongoCursor<MongoStoredTemplate> cursor = entities.find("{_id: '" + id.toString() + "'}").as(MongoStoredTemplate.class);
            if (cursor.hasNext()) {
                retVal = cursor.next();
            }
        }
        return retVal;
    }

    @Override
    protected StoredTemplate doFetchTemplate(BlockId id) {
        StoredTemplate retVal = null;
        if (id != null) {
            MongoCollection entities = jongo.getCollection(MongoDatabase.TEMPLATE_COLLECTION);
            MongoCursor<MongoStoredTemplate> cursor = entities.find("{_id: '" + id.toString() + "'}").as(MongoStoredTemplate.class);
            if (cursor.hasNext()) {
                retVal = cursor.next();
            }
        }
        return retVal;
    }


    protected StoredTemplate doFetchSingletonTemplate(BlockId id, String language) {
        StoredTemplate retVal = null;
        if (id != null && language != null) {
            MongoCollection entities = jongo.getCollection(MongoDatabase.SINGLETON_COLLECTION + "_" + language);
            MongoCursor<MongoStoredTemplate> cursor = entities.find("{_id: '" + id.toString() + "'}").as(MongoStoredTemplate.class);
            if (cursor.hasNext()) {
                retVal = cursor.next();
            }
        }
        return retVal;
    }

    protected StoredTemplate doFetchSingletonTemplate(BlockId id) {
        StoredTemplate retVal = null;
        if (id != null) {
            MongoCollection entities = jongo.getCollection(MongoDatabase.SINGLETON_COLLECTION);
            MongoCursor<MongoStoredTemplate> cursor = entities.find("{_id: '" + id.toString() + "'}").as(MongoStoredTemplate.class);
            if (cursor.hasNext()) {
                retVal = cursor.next();
            }
        }
        return retVal;
    }

    protected Blueprint doFetchBlueprint(BlockId id, String language) {
        Blueprint retVal = null;
        if (id != null && language != null) {
            MongoCollection entities = jongo.getCollection(MongoDatabase.BLUEPRINT_COLLECTION + "_" + language);
            MongoCursor<MongoBlueprint> cursor = entities.find("{_id: '" + id.toString() + "'}").as(MongoBlueprint.class);
            if (cursor.hasNext()) {
                retVal = cursor.next();
            }
        }
        return retVal;
    }

    protected Blueprint doFetchBlueprint(BlockId id) {
        Blueprint retVal = null;
        if (id != null) {
            MongoCollection entities = jongo.getCollection(MongoDatabase.BLUEPRINT_COLLECTION);
            MongoCursor<MongoBlueprint> cursor = entities.find("{_id: '" + id.toString() + "'}").as(MongoBlueprint.class);
            if (cursor.hasNext()) {
                retVal = cursor.next();
            }
        }
        return retVal;
    }


    protected PageTemplate doFetchPageTemplate(BlockId id, String language) {
        PageTemplate retVal = null;
        if (id != null && language != null) {
            MongoCollection entities = jongo.getCollection(MongoDatabase.PAGE_TEMPLATE_COLLECTION + "_" + language);
            MongoCursor<MongoPageTemplate> cursor = entities.find("{_id: '" + id.toString() + "'}").as(MongoPageTemplate.class);
            if (cursor.hasNext()) {
                retVal = cursor.next();
            }
        }
        return retVal;
    }


    protected PageTemplate doFetchPageTemplate(BlockId id) {
        PageTemplate retVal = null;
        if (id != null) {
            MongoCollection entities = jongo.getCollection(MongoDatabase.BLUEPRINT_COLLECTION);
            MongoCursor<MongoPageTemplate> cursor = entities.find("{_id: '" + id.toString() + "'}").as(MongoPageTemplate.class);
            if (cursor.hasNext()) {
                retVal = cursor.next();
            }
        }
        return retVal;
    }

    @Override
    protected void doSaveSiteMap(BlocksUrlDispatcher urlDispatcher) throws DatabaseException
    {
        try {
            MongoCollection collection = jongo.getCollection(MongoDatabase.SITEMAP_COLLECTION);
            collection.save(urlDispatcher);
        } catch (Exception e) {
            Logger.error("Could not save sitemap", e);
            throw new DatabaseException("Could not save sitemap");
        }
    }

    @Override
    protected BlocksUrlDispatcher doFetchSiteMap() {

        BlocksUrlDispatcher retVal = null;
        MongoCollection collection = jongo.getCollection(MongoDatabase.SITEMAP_COLLECTION);
        MongoCursor<MongoUrlDispatcher> cursor = collection.find().limit(1).sort("{_id: 1}").as(MongoUrlDispatcher.class);
        if (cursor.hasNext()) {
            retVal = cursor.next();
        }
        return retVal;
    }



    /* Template Versioning */

    @Override
    protected void doSaveTemplateHistory(StoredTemplate storedTemplate) throws DatabaseException
    {
        if (storedTemplate != null && storedTemplate.getId() != null) {
            MongoTemplateHistory history = new MongoTemplateHistory(storedTemplate);
            MongoCollection collection = jongo.getCollection(MongoDatabase.TEMPLATE_HISTORY_COLLECTION);
            collection.save(history);
        }
    }

    @Override
    protected StoredTemplate doFetchPreviousTemplate(BlockId id, String language) {
        StoredTemplate retVal = null;
        if (id != null && language != null) {
            MongoCollection collection = jongo.getCollection(MongoDatabase.TEMPLATE_HISTORY_COLLECTION);
            MongoCursor<MongoTemplateHistory> cursor = collection.find("{templateId: "+id.toString()+ ", language: " + language+ "}").limit(1).sort("{_id: 1}").as(MongoTemplateHistory.class);
            if (cursor.hasNext()) {
                retVal = cursor.next().getStoredTemplate();
            }
        }
        return retVal;
    }

    @Override
    protected StoredTemplate doFetchPreviousTemplateWithVersion(BlockId id, String language, Long version) {
        StoredTemplate retVal = null;
        if (id != null && language != null) {
            MongoCollection collection = jongo.getCollection(MongoDatabase.TEMPLATE_HISTORY_COLLECTION);
            MongoCursor<MongoTemplateHistory> cursor = collection.find("{templateId: "+id.toString()+ ", language: " + language+ ", version: " + version + " }").limit(1).sort("{_id: 1}").as(
                            MongoTemplateHistory.class);
            if (cursor.hasNext()) {
                retVal = cursor.next().getStoredTemplate();
            }
        }
        return retVal;
    }


    protected StoredTemplate doFetchPreviousBlueprint(BlockId id, String language) {
        return null;
    }

    protected StoredTemplate doFetchPreviousBlueprintWithVersion(BlockId id, String language, Long version) {
        return null;
    }
    protected List<Long> getVersionNumbersForBlueprint(BlockId id, String language) {
        return null;
    }

    protected StoredTemplate doFetchPreviousPageTemplate(BlockId id, String language) {
        return null;
    }

    protected StoredTemplate doFetchPreviousPageTemplateWithVersion(BlockId id, String language, Long version) {
        return null;
    }

    protected List<Long> getVersionNumbersForPageTemplate(BlockId id, String language) {
        return null;
    }

    @Override
    protected List<Long> getVersionNumbersForTemplate(BlockId id, String language) {
        return new ArrayList<Long>();
    }

    @Override
    public UrlDispatcher createUrlDispatcher() {
        return new MongoUrlDispatcher();

    }


    @Override
    public Blueprint createBlueprint(Element element, String language) throws ParseException
    {
        return new MongoBlueprint(element, language);
    }

    @Override
    public StoredTemplate createStoredTemplate(Element element, String language) throws ParseException
    {
        return new MongoStoredTemplate(element, language);
    }

    @Override
    public StoredTemplate createStoredTemplate(Element element, URL url) throws ParseException
    {
        return new MongoStoredTemplate(element, url);
    }

    @Override
    public PageTemplate createPageTemplate(Element element, String language) throws ParseException
    {
        return new MongoPageTemplate(element, language);
    }

    @Override
    public Entity createEntity(String name)
    {
        return new MongoEntity(name);
    }

    @Override
    public Entity createEntity(String name, String language)
    {
        return new MongoEntity(name, language);
    }

    public BlockId getIdForString(String s) {
        return new MongoID(s);
    }

}
