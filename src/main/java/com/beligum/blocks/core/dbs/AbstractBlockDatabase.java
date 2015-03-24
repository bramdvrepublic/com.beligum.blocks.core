package com.beligum.blocks.core.dbs;

import com.beligum.blocks.core.config.BlocksConfig;
import com.beligum.blocks.core.exceptions.DatabaseException;
import com.beligum.blocks.core.exceptions.ParseException;
import com.beligum.blocks.core.identifiers.BlockId;
import com.beligum.blocks.core.models.nosql.Blueprint;
import com.beligum.blocks.core.models.nosql.Entity;
import com.beligum.blocks.core.models.nosql.PageTemplate;
import com.beligum.blocks.core.models.nosql.StoredTemplate;
import com.beligum.blocks.core.mongo.MongoBlueprint;
import com.beligum.blocks.core.mongo.MongoEntity;
import com.beligum.blocks.core.mongo.MongoPageTemplate;
import com.beligum.blocks.core.mongo.MongoStoredTemplate;
import org.jsoup.nodes.Element;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by wouter on 23/03/15.
 */
public abstract class AbstractBlockDatabase implements BlocksDatabase
{


    public Entity fetchEntity(BlockId id, String language) {
        Entity retVal = doFetchEntity(id, language);
        if (retVal == null) {
            retVal = doFetchEntity(id);
        }
        return retVal;
    }

    public StoredTemplate fetchTemplate(BlockId id, String language) {
        StoredTemplate retVal = doFetchTemplate(id, language);
        if (retVal == null) {
            retVal = doFetchTemplate(id);
        }
        return retVal;
    }

    public StoredTemplate fetchSingletonTemplate(BlockId id, String language) {
        StoredTemplate retVal = doFetchSingletonTemplate(id, language);
        if (retVal == null) {
            retVal = doFetchSingletonTemplate(id);
        }
        return retVal;
    }

    public StoredTemplate fetchBlueprint(BlockId id, String language) {
        StoredTemplate retVal = doFetchBlueprint(id, language);
        if (retVal == null) {
            retVal = doFetchBlueprint(id);
        }
        return retVal;
    }

    public StoredTemplate fetchPageTemplate(BlockId id, String language) {
        StoredTemplate retVal = doFetchPageTemplate(id, language);
        if (retVal == null) {
            retVal = doFetchPageTemplate(id);
        }
        return retVal;
    }

    public BlocksUrlDispatcher fetchSiteMap() throws DatabaseException
    {
        BlocksUrlDispatcher retVal = doFetchSiteMap();
        if (retVal == null) {
            retVal = this.createUrlDispatcher();
            saveSiteMap(retVal);
        }
        return retVal;
    }

    public void saveEntity(Entity entity) {
        ArrayList<Entity> entities = entity.flatten(new ArrayList<Entity>());
        for (Entity e: entities) {
            saveSingleEntity(e);
        }
    }

    private void saveSingleEntity(Entity entity) {
        Entity oldEntity = null;
        if (entity.getId() != null) oldEntity = fetchEntity(entity.getId(), entity.getLanguage());
        if (oldEntity == null) {
            doSaveEntity(entity);
        } else if (!oldEntity.equals(entity)) {
            entity.merge(oldEntity, false);
            entity.setMeta(oldEntity.getMeta());
            entity.getMeta().touch();
            doSaveEntity(entity);
        }
    }



    public void saveTemplate(StoredTemplate template) throws DatabaseException
    {
        StoredTemplate oldTemplate = null;
        if (template.getId() != null) oldTemplate = fetchTemplate(template.getId(), template.getLanguage());
        if (oldTemplate == null) {
            doSaveTemplate(template);
        } else if (!oldTemplate.equals(template)) {
            doSaveTemplateHistory(oldTemplate);
            template.setMeta(oldTemplate.getMeta());
            template.getMeta().touch();
            doSaveTemplate(template);
        }
    }

    public void saveSiteMap(BlocksUrlDispatcher urlDispatcher) throws DatabaseException
    {
        doSaveSiteMap(urlDispatcher);
    }


    protected abstract Entity doFetchEntity(BlockId id, String language);
    protected abstract Entity doFetchEntity(BlockId id);

    protected abstract StoredTemplate doFetchTemplate(BlockId id, String language);
    protected abstract StoredTemplate doFetchTemplate(BlockId id);
    protected abstract StoredTemplate doFetchSingletonTemplate(BlockId id, String language);
    protected abstract StoredTemplate doFetchSingletonTemplate(BlockId id);
    protected abstract Blueprint doFetchBlueprint(BlockId id, String language);
    protected abstract Blueprint doFetchBlueprint(BlockId id);
    protected abstract PageTemplate doFetchPageTemplate(BlockId id, String language);
    protected abstract PageTemplate doFetchPageTemplate(BlockId id);

    protected abstract BlocksUrlDispatcher doFetchSiteMap();

    protected abstract void doSaveTemplate(StoredTemplate template) throws DatabaseException;
    protected abstract void doSaveSiteMap(BlocksUrlDispatcher urlDispatcher) throws DatabaseException;
    protected abstract void doSaveEntity(Entity entity);

    protected abstract StoredTemplate doFetchPreviousTemplate(BlockId id, String language);
    protected abstract StoredTemplate doFetchPreviousTemplateWithVersion(BlockId id, String language, Long version);
    protected abstract List<Long> getVersionNumbersForTemplate(BlockId id, String language);

    protected abstract StoredTemplate doFetchPreviousBlueprint(BlockId id, String language);
    protected abstract StoredTemplate doFetchPreviousBlueprintWithVersion(BlockId id, String language, Long version);
    protected abstract List<Long> getVersionNumbersForBlueprint(BlockId id, String language);

    protected abstract StoredTemplate doFetchPreviousPageTemplate(BlockId id, String language);
    protected abstract StoredTemplate doFetchPreviousPageTemplateWithVersion(BlockId id, String language, Long version);
    protected abstract List<Long> getVersionNumbersForPageTemplate(BlockId id, String language);

    protected abstract void doSaveTemplateHistory(StoredTemplate storedTemplate) throws DatabaseException;

    public abstract BlocksUrlDispatcher createUrlDispatcher();

    public abstract BlockId getIdForString(String s);

}
