package com.beligum.blocks.core.URLMapping.simple;

import com.beligum.blocks.core.models.nosql.META;

import java.util.Calendar;

/**
 * Created by wouter on 24/03/15.
 */
public class UrlBranchHistory
{
    private Long versioned;
    private String updatedAt;
    private String updateBy;
    private String storedTemplateId;

    public  UrlBranchHistory(String storedTemplateID) {
        this.versioned = Calendar.getInstance().getTimeInMillis();
        this.updateBy = META.getCurrentUserName();
        this.updatedAt = META.getCurrentTime();
        this.storedTemplateId = storedTemplateID;
    }

    public String getStoredTemplateId() {
        return storedTemplateId;
    }
}
