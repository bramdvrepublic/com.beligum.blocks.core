package com.beligum.blocks.core.dbs;

import com.beligum.blocks.core.base.Blocks;
import com.beligum.blocks.core.exceptions.DatabaseException;
import com.beligum.blocks.core.identifiers.BlockId;
import com.beligum.blocks.core.models.Blueprint;
import com.beligum.blocks.core.models.Entity;
import com.beligum.blocks.core.models.StoredTemplate;
import com.beligum.blocks.core.models.interfaces.BlocksStorable;
import com.beligum.blocks.core.models.interfaces.BlocksVersionedStorable;
import com.beligum.blocks.core.urlmapping.BlocksUrlDispatcher;
import org.joda.time.LocalDateTime;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * Created by wouter on 23/03/15.
 */
public abstract class AbstractBlockDatabase implements BlocksDatabase
{

    private void touch(BlocksVersionedStorable storable) {
        storable.setDocumentVersion(Calendar.getInstance().getTimeInMillis());
        storable.setApplicationVersion(Blocks.config().getProjectVersion());
        storable.setUpdatedAt(LocalDateTime.now().toString());
        storable.setUpdatedBy(Blocks.config().getCurrentUserName());
        if (storable.getCreatedAt() == null || storable.getCreatedBy() == null) {
            storable.setCreatedAt(storable.getUpdatedAt());
            storable.setCreatedBy(storable.getUpdatedBy());
        }
    }

    private void touch(BlocksStorable storable) {
        storable.setUpdatedAt(LocalDateTime.now().toString());
        storable.setUpdatedBy(Blocks.config().getCurrentUserName());
        if (storable.getCreatedAt() == null || storable.getCreatedBy() == null) {
            storable.setCreatedAt(storable.getUpdatedAt());
            storable.setCreatedBy(storable.getUpdatedBy());
        }
    }

    public <T extends BlocksStorable> T fetch(BlockId id, Class<T> clazz) {
        return doFetch(id, clazz);
    }

    public <T extends BlocksVersionedStorable> T fetch(BlockId id, String language, Class<T> clazz) {
        T retVal = doFetch(id, language, clazz);
        if (retVal == null) {
            retVal = doFetch(id, clazz);
        }
        return retVal;
    }

    public Entity fetchEntity(BlockId id, String language) {
        return fetch(id, language, Blocks.factory().getEntityClass());
    }

    public StoredTemplate fetchTemplate(BlockId id, String language) {
        return fetch(id, language, Blocks.factory().getStoredTemplateClass());
    }

    public Blueprint fetchBlueprint(BlockId id, String language) {
        return fetch(id, language, Blocks.factory().getBlueprintClass());
    }

    public StoredTemplate fetchPageTemplate(BlockId id, String language) {
        return fetch(id, language, Blocks.factory().getPageTemplateClass());
    }



    //
//    public BlocksUrlDispatcher fetchSiteMap() throws DatabaseException
//    {
//        return doFetchSiteMap();
//    }

    public void save(BlocksStorable storable) throws DatabaseException
    {
        touch(storable);
        doSave(storable);
    }

    public void save(BlocksVersionedStorable storable) throws DatabaseException
    {
        if (storable.getId() != null) {
            boolean deleted = false;
            // Get the current version in the database
            BlocksVersionedStorable oldStorable = fetch(storable.getId(), storable.getLanguage(), storable.getClass());
            if (oldStorable == null) {
                oldStorable = doFetchPrevious(storable.getId(), storable.getLanguage(), storable.getClass());
                deleted = true;
            }

            if (oldStorable != null && (deleted || !oldStorable.equals(storable))) {
                // save current version to history
                doSaveHistory(oldStorable);
                // upodate metadata
                touch(storable);

            }
        }
        doSave(storable);

    }


    public void saveEntity(Entity entity) throws DatabaseException
    {
        ArrayList<Entity> entities = entity.flatten(new ArrayList<Entity>());
        for (Entity e: entities) {
            save(e);
        }
    }


    public void remove(BlocksVersionedStorable storable, boolean all) throws DatabaseException
    {
        if (storable.getId() != null) {
            BlocksVersionedStorable oldStorable = null;
            for (String lang : Blocks.config().getLanguages()) {
                if (all || Blocks.config().getLanguages().equals(storable.getLanguage())) {
                    oldStorable = fetchTemplate(storable.getId(), lang);
                    if (oldStorable != null) {
                        doSaveHistory(oldStorable);
                    }
                    doRemove(storable);
                }
            }
        }
    }

    public void remove(BlocksStorable storable) throws DatabaseException
    {
        if (storable.getId() != null) {
            doRemove(storable);
        }
    }

    protected abstract <T extends BlocksStorable> T doFetch(BlockId id, Class<T> clazz);
    protected abstract <T extends BlocksVersionedStorable> T doFetch(BlockId id, String language, Class<T> clazz);
    public abstract BlocksUrlDispatcher fetchUrlDispatcher();

    protected abstract void doSave(BlocksVersionedStorable storable) throws DatabaseException;
    protected abstract void doSave(BlocksStorable storable) throws DatabaseException;
    protected abstract void doSaveHistory(BlocksVersionedStorable versionedStorable) throws DatabaseException;


    protected abstract void doRemove(BlocksVersionedStorable storable) throws DatabaseException;
    protected abstract void doRemove(BlocksStorable storable) throws DatabaseException;


    protected abstract <T extends BlocksVersionedStorable> T doFetchPrevious(BlockId id, String language, Class<T> clazz);
    protected abstract <T extends BlocksVersionedStorable> T doFetchPreviousVersion(BlockId id, String language, Long version, Class<T> clazz);
    protected abstract List<Long> getVersionNumbers(BlockId id, String language);




}
