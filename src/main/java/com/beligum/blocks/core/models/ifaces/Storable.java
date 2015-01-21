package com.beligum.blocks.core.models.ifaces;

import com.beligum.blocks.core.exceptions.SerializationException;
import com.beligum.blocks.core.identifiers.RedisID;

import java.util.Map;

/**
 * Created by bas on 13.10.14.
 */
public interface Storable extends Identifiable
{
    /**
     * Override of the getId-method of Identifiable interface. Here a RedisID is returned, which has more functionalities.
     * @return the id of this storable
     */
    @Override
    public RedisID getId();
    /**
     * @return the version of this storable, which is the time it was created in milliseconds
     */
    public long getVersion();
    /**
     * @return the id of this storable with it's version attached ("[storableId]:[version]")
     */
    public String getVersionedId();
    /**
     * @return the id of this storable without a version attached ("[storableId]")
     */
    public String getUnversionedId();
    /**
     * @return the version of the application this storable is supposed ot interact with
     */
    public String getApplicationVersion();
    /**
     * @return the creator of this storable
     */
    public String getCreator();
    /**
     * Gives a hash-representation of this storable to save to the db. This method decides what information is stored in db, and what is not.
     * @return a map representing the key-value structure of this element to be saved to db
     */
    public Map<String, String> toHash() throws SerializationException;
}