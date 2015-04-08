package com.beligum.blocks.models.interfaces;

/**
 * Created by wouter on 25/03/15.
 */
public interface BlocksVersionedStorable extends BlocksStorable
{
    /**
     * @return the versioned time of the document in millis
     */
    public Long getDocumentVersion();
    public void setDocumentVersion(Long documentVersion);

    /**
     * @return the version of the application this storable is supposed ot interact with
     */
    public String getApplicationVersion();
    public void setApplicationVersion(String applicationVersion);

    public String getLanguage();
    public void setLanguage(String language);


}
