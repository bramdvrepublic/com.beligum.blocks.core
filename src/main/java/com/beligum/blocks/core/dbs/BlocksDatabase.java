package com.beligum.blocks.core.dbs;

import com.beligum.blocks.core.exceptions.DatabaseException;
import com.beligum.blocks.core.identifiers.BlockId;
import com.beligum.blocks.core.models.Blueprint;
import com.beligum.blocks.core.models.Entity;
import com.beligum.blocks.core.models.StoredTemplate;
import com.beligum.blocks.core.models.interfaces.BlocksStorable;
import com.beligum.blocks.core.models.interfaces.BlocksVersionedStorable;

/**
 * Created by wouter on 23/03/15.
 */
public interface BlocksDatabase
{
    public <T extends BlocksStorable> T fetch(BlockId id, Class<T> clazz);
    public <T extends BlocksVersionedStorable> T fetch(BlockId id, String language, Class<T> clazz);
    public Entity fetchEntity(BlockId id, String language);
    public StoredTemplate fetchTemplate(BlockId id, String language);
    public Blueprint fetchBlueprint(BlockId id, String language);
    public StoredTemplate fetchPageTemplate(BlockId id, String language);

    public void save(BlocksStorable storable) throws DatabaseException;
    public void save(BlocksVersionedStorable storable) throws DatabaseException;
    public void saveEntity(Entity entity) throws DatabaseException;

    public void remove(BlocksVersionedStorable storable, String language) throws DatabaseException;
    public void remove(BlocksVersionedStorable storable) throws DatabaseException;
    public void remove(BlocksStorable storable) throws DatabaseException;



}
