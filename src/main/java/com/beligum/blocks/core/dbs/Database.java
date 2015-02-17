package com.beligum.blocks.core.dbs;

import com.beligum.blocks.core.exceptions.DatabaseException;
import com.beligum.blocks.core.identifiers.BlocksID;
import com.beligum.blocks.core.models.redis.Storable;

import java.io.Closeable;
import java.util.List;
import java.util.Set;

/**
 * Created by bas on 17.02.15.
 */
public interface Database<T extends Storable> extends Closeable
{
    void create(T storable) throws DatabaseException;

    void update(T storable) throws DatabaseException;

    /**
     * Fetch a {@link Storable} from db.
     * @param id id of the {@link Storable}
     * @param type class of the {@link Storable}
     * @return an object of the type specified, so casting is save
     * @throws DatabaseException
     */
    T fetch(BlocksID id, Class<? extends T> type) throws DatabaseException;

    T fetchLastVersion(BlocksID id, Class<? extends T> type) throws DatabaseException;

    List<T> fetchVersionList(BlocksID id, Class<? extends T> type) throws DatabaseException;

    boolean trash(BlocksID id) throws DatabaseException;

    void flushDB();

    @Override
    void close();

    /**
     * Get the last saved version number of a storable with a certain id.
     * @param unversionedId the unversioned id of the storable to get the last version number of
     * @return the last version number saved in db, -1 if no version is present in db
     */
    public Long getLastVersionNumber(String unversionedId);

    /**
     * Fetch all language alternatives present in db for a storable with a certain id.
     * This looks for alternative languages within the same version of the storable.
     * @param id
     * @return a set with all language alternatives, or an empty one if no alternatives were found
     */
    public Set<String> fetchLanguageAlternatives(BlocksID id);

}
