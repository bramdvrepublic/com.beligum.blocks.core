package com.beligum.blocks.core.mongo;

import com.beligum.blocks.core.base.Blocks;
import com.beligum.blocks.core.mongo.versioned.MongoVersionable;
import com.beligum.blocks.core.urlmapping.BlocksUrlDispatcher;
import com.beligum.blocks.core.models.interfaces.BlocksStorable;
import com.beligum.blocks.core.models.interfaces.BlocksVersionedStorable;
import com.beligum.blocks.core.mongo.versioned.MongoVersionedObject;
import com.beligum.blocks.core.dbs.AbstractBlockDatabase;
import com.beligum.blocks.core.exceptions.DatabaseException;
import com.beligum.blocks.core.identifiers.BlockId;
import com.beligum.core.framework.utils.Logger;
import com.fasterxml.jackson.databind.MapperFeature;
import com.mongodb.*;
import org.bson.types.ObjectId;
import org.jongo.Jongo;
import org.jongo.MongoCollection;
import org.jongo.MongoCursor;
import org.jongo.marshall.jackson.JacksonMapper;

import java.net.UnknownHostException;
import java.util.List;

/**
 * Created by wouter on 17/03/15.
 */

//http://stackoverflow.com/questions/15789471/efficient-pojo-mapping-to-from-java-mongo-dbobject-using-jackson
public class MongoDatabase extends AbstractBlockDatabase
{

    private Jongo jongo;

    public MongoDatabase() throws UnknownHostException
    {

        // or
        MongoClient client = new MongoClient(Blocks.config().getBlocksDBHost() , Blocks.config().getBlocksDBPort() );


        DB db = client.getDB( "BLOCKS" );
        this.jongo = new Jongo(db,
                                new JacksonMapper.Builder().enable(MapperFeature.AUTO_DETECT_GETTERS)
                                                .build()
        );
    }

    private String getCollectionName(Class<? extends BlocksStorable> clazz) {
        return clazz.getSimpleName();
    }

    private String getCollectionName(Class<? extends BlocksStorable> clazz, String language) {
        return getCollectionName(clazz) + "_" + language;
    }

    private String getHistoryCollectionName(Class<? extends BlocksStorable> clazz) {
        return clazz.getSimpleName()+ "_history";
    }

    private String getHistoryCollectionName(Class<? extends BlocksStorable> clazz, String language) {
        return getHistoryCollectionName(clazz) + "_" + language;
    }

    @Override
    protected void doSave(BlocksStorable storable) {
        MongoCollection collection = jongo.getCollection(getCollectionName(storable.getClass()));
        collection.save(storable);
    }

    @Override
    protected void doSave(BlocksVersionedStorable storable) {

        String collectionName = getCollectionName(storable.getClass());
        MongoCollection collection = null;
        if (doFetch(storable.getId(), storable.getClass()) == null || storable.getLanguage() == Blocks.config().getDefaultLanguage()) {
            collection = jongo.getCollection(collectionName);
            collection.save(storable);
        }
        collection = jongo.getCollection(getCollectionName(storable.getClass(), storable.getLanguage()));
        collection.save(storable);
    }

    @Override
    protected  <T extends BlocksVersionedStorable> T doFetch(BlockId id, String language, Class<T> clazz) {
        T retVal = null;
        if (id != null && language != null) {
            MongoCollection entities = jongo.getCollection(getCollectionName(clazz, language));
            MongoCursor<T> cursor = entities.find("{_id: '" + id.toString() + "'}").as(clazz);
            if (cursor.hasNext()) {
                retVal = cursor.next();
            }
        }
        return retVal;
    }

    @Override
    public BlocksUrlDispatcher fetchUrlDispatcher() {
        return doFetch(MongoUrlDispatcher.dispatcherID, MongoUrlDispatcher.class);
    }

    @Override
    protected <T extends BlocksStorable> T doFetch(BlockId id, Class<T> clazz) {
        T retVal = null;
        if (id != null) {
            MongoCollection entities = jongo.getCollection(getCollectionName(clazz));
            MongoCursor<T> cursor = entities.find("{_id: '" + id.toString() + "'}").as(clazz);
            if (cursor.hasNext()) {
                retVal = cursor.next();
            }
        }
        return retVal;
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




    /* Template Versioning */

    @Override
    protected void doSaveHistory(BlocksVersionedStorable storable) throws DatabaseException
    {

        if (storable != null && storable.getId() != null) {
            MongoVersionedObject history = new MongoVersionedObject((MongoVersionable)storable);
            MongoCollection collection = jongo.getCollection(getHistoryCollectionName(storable.getClass(), storable.getLanguage()));
            collection.save(history);
        }
    }

    @Override
    protected void doRemove(BlocksStorable storable) {
        if (storable != null && storable.getId() != null) {
            MongoCollection collection = jongo.getCollection(getCollectionName(storable.getClass()));
            collection.remove(storable.getId().toString());
        }
    }

    @Override
    protected void doRemove(BlocksVersionedStorable storable) {
        if (storable != null && storable.getId() != null) {
            MongoCollection collection = jongo.getCollection(getCollectionName(storable.getClass()));
            collection.remove(storable.getId().toString());
        }
    }




    /*
    * Fetch versioned objects
    * */

    @Override
    protected <T extends BlocksVersionedStorable> T doFetchPrevious(BlockId id, String language, Class<T> clazz) {
        T retVal = null;
        MongoVersionedObject versionedObject = null;
        if (id != null && language != null) {
            MongoCollection collection = jongo.getCollection(getHistoryCollectionName(clazz, language));
            MongoCursor<MongoVersionedObject> cursor = collection.find("{versionedId: #, language: #}", id.toString(), language).limit(1).sort("{_id: 1}").as(MongoVersionedObject.class);
            if (cursor.hasNext()) {
                versionedObject = cursor.next();
                if (versionedObject != null) {
                    retVal = (T)versionedObject.getVersionedStorable();
                }
            }
        }
        return retVal;
    }

    @Override
    protected <T extends BlocksVersionedStorable> T doFetchPreviousVersion(BlockId id, String language, Long version,  Class<T> clazz) {
        T retVal = null;
        MongoVersionedObject versionedObject = null;
        if (id != null && language != null && version != null) {
            MongoCollection collection = jongo.getCollection(getCollectionName(clazz));
            MongoCursor<MongoVersionedObject> cursor = collection.find("{versionedId: "+id.toString()+ ", language: " + language+ ", version: " + version + " }").limit(1).sort("{_id: 1}").as(
                            MongoVersionedObject.class);
            if (cursor.hasNext()) {
                versionedObject = cursor.next();
                if (versionedObject != null) {
                    retVal = (T)versionedObject.getVersionedStorable();
                }
            }
        }
        return retVal;
    }


    @Override
    protected List<Long> getVersionNumbers(BlockId id, String language) {
        return null;
    }


}
