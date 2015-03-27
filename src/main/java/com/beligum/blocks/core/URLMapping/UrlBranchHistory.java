package com.beligum.blocks.core.urlmapping;

import com.beligum.blocks.core.base.Blocks;
import org.joda.time.LocalDateTime;

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

    // constructor for jackson
    public UrlBranchHistory() {

    }

    public  UrlBranchHistory(String storedTemplateID) {
        this.versioned = Calendar.getInstance().getTimeInMillis();
        this.updateBy = Blocks.config().getCurrentUserName();
        this.updatedAt = LocalDateTime.now().toString();
        this.storedTemplateId = storedTemplateID;
    }

    public String getStoredTemplateId() {
        return storedTemplateId;
    }
}
