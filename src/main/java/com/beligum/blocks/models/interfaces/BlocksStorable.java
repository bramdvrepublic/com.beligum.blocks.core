package com.beligum.blocks.models.interfaces;

import com.beligum.blocks.identifiers.BlockId;

/**
 * Created by wouter on 24/03/15.
 */
public interface BlocksStorable
{

    /**
     * @return the creator of this storable
     */
    public String getCreatedBy();

    public void setCreatedBy(String created_by);

    /**
     * @return the updater of this storable
     */
    public String getUpdatedBy();

    public void setUpdatedBy(String updated_by);

    /**
     * @return the moment of creation of this storable
     */
    public String getCreatedAt();

    public void setCreatedAt(String createdAt);

    /**
     * @return the moment of last update of this storable
     */
    public String getUpdatedAt();

    public void setUpdatedAt(String updatedAt);

}
