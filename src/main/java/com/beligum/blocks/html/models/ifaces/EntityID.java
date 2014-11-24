package com.beligum.blocks.html.models.ifaces;

import com.beligum.blocks.core.models.storables.Entity;
import com.beligum.blocks.html.parsers.AbstractParser;
import org.jsoup.nodes.Element;

/**
 * Created by wouter on 21/11/14.
 */
public class EntityID
{

    public static final String domainSeparator = "::";
    public static final String fieldSeparator = "#";
    public static final String implementationIDSeparator = "/";
    public static final String domainFixed = "MOT";
    public static final String defaultEntity = "Thing";

    private String domain;
    private String entity;
    private String implementationID;
    private String property;

    private EntityID(String domain, String entity, String implementationId, String property) {
        this.domain = domain;
        this.entity = entity;
        this.implementationID = implementationId;
        this.property = property;
    }

    public static EntityID parse(String id) {
        String domain = null;
        String entity = null;
        String implementationID = null;
        String property = null;

        if (id != null) {
            String[] idList = id.split(EntityID.domainSeparator);
            if (idList.length == 2) {
                domain = idList[0];
                id = idList[1];
            }
            else {
                domain = EntityID.getDefaultDomain();
            }

            // Set field
            idList = id.split(EntityID.fieldSeparator);
            if (idList.length == 2) {
                property = idList[1];
                id = idList[0];
            }

            idList = id.split(EntityID.implementationIDSeparator);
            if (idList.length == 2) {
                implementationID = idList[1];
                entity = idList[0];
            }
            else {
                entity = id;
            }
        }
        return new EntityID(domain, entity, implementationID, property);
    }


    public String getImplementationID() {
        return this.implementationID;
    }

    public boolean hasImplementationID() {
        return this.getImplementationID() != null;
    }

    public String getDomainName() {
        String retVal = EntityID.getDefaultDomain();
        if (domain != null) {
            retVal = domain;
        }
        return retVal;
    }

    public String getEntityName() {
        String retVal = EntityID.getDefaultEntity();
        if (entity != null) {
            retVal = entity;
        }
        return retVal;
    }


    public String toString() {
        String retVal = this.getDomainName() + EntityID.domainSeparator + this.getEntityName();
        if (getImplementationID() != null) {
            retVal += this.implementationIDSeparator + this.getImplementationID();
        }
        if (this.property != null) {
            retVal += EntityID.fieldSeparator + this.property;
        }
        return retVal;
    }

    public static String getDefaultDomain() {
        return EntityID.domainFixed;
    }

    public static String getDefaultEntity() {
        return EntityID.defaultEntity;
    }
}
