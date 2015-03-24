package com.beligum.blocks.core.mongo;

import com.beligum.blocks.core.exceptions.DatabaseException;
import com.beligum.blocks.core.models.nosql.StoredTemplate;

import java.util.Calendar;

/**
 * Created by wouter on 24/03/15.
 */
public class MongoTemplateHistory
{
    private String _id;
    private StoredTemplate storedTemplate;
    private Long version;
    private Long versioned;
    private String language;
    private String templateId;

    public MongoTemplateHistory(StoredTemplate storedTemplate) throws DatabaseException
    {
        if (storedTemplate != null && storedTemplate.getId() != null) {
            this.storedTemplate = storedTemplate;
            this.version = this.storedTemplate.getMeta().getDocumentVersion();
            this.versioned = Calendar.getInstance().getTimeInMillis();
            this.language = this.storedTemplate.getLanguage();
            this.templateId = this.storedTemplate.getId().toString();
        } else {
            throw new DatabaseException("Could not version empty template");
        }
    }

    public StoredTemplate getStoredTemplate() {
        return this.storedTemplate;
    }
}
