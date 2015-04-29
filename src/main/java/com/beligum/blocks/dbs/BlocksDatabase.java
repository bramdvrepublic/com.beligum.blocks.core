package com.beligum.blocks.dbs;

import com.beligum.blocks.exceptions.DatabaseException;
import com.beligum.blocks.identifiers.BlockId;
import com.beligum.blocks.models.*;
import com.beligum.blocks.models.interfaces.BlocksStorable;
import com.beligum.blocks.models.interfaces.BlocksVersionedStorable;
import com.beligum.blocks.models.jsonld.ResourceNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by wouter on 23/03/15.
 */
public interface BlocksDatabase
{
    public <T extends BlocksStorable> T fetch(BlockId id, Class<T> clazz);

    public <T extends BlocksVersionedStorable> T fetch(BlockId id, String language, Class<T> clazz);
    public abstract List<ResourceContext> fetchEntities(String query);

    public ResourceNode fetchResource(String blockId, String language);

    public void save(ResourceContext context);

    //    public StoredTemplate fetchTemplate(BlockId id, String language);
//    public Blueprint fetchBlueprint(BlockId id, String language);
//    public StoredTemplate fetchPageTemplate(BlockId id, String language);

    public void save(BlocksStorable storable) throws DatabaseException;

    public void save(BlocksVersionedStorable storable) throws DatabaseException;
    public void saveEntity(ResourceNode entity) throws DatabaseException;

    public void remove(BlocksVersionedStorable storable, String language) throws DatabaseException;

    public void remove(BlocksVersionedStorable storable) throws DatabaseException;

    public void remove(BlocksStorable storable) throws DatabaseException;

    public ArrayList<HashMap<String, Object>> testFetch();

}
