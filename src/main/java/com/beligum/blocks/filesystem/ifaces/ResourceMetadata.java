package com.beligum.blocks.filesystem.ifaces;

import com.beligum.blocks.security.ifaces.Acl;

import java.net.URI;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Collection;

public interface ResourceMetadata
{
    //-----CONSTANTS-----

    //-----VARIABLES-----

    //-----PUBLIC METHODS-----

    /**
     * The counterpart of dc:created; Date of creation of the resource.
     * The returned value should be in the UTC time zone.
     */
    java.time.ZonedDateTime getCreated();

    /**
     * Same as getCreated(), but in the local time zone
     */
    java.time.LocalDateTime getCreatedLocal();

    /**
     * The counterpart of dc:creator; An entity primarily responsible for making the resource.
     */
    URI getCreator();

    /**
     * The counterpart of dc:modified; Date on which the resource was changed.
     * The returned value should be in the UTC time zone.
     */
    java.time.ZonedDateTime getLastModified();

    /**
     * Same as getLastModified(), but in the local time zone
     */
    java.time.LocalDateTime getLastModifiedLocal();

    /**
     * The counterpart of dc:contributor; An entity responsible for making contributions to the resource.
     */
    Collection<URI> getContributors();

    /**
     * The minimum security level that is required for viewing this resource
     */
    Acl getReadAcl();

    /**
     * The minimum security level that is required for editing this resource
     */
    Acl getUpdateAcl();

    /**
     * The minimum security level that is required for deleting this resource
     */
    Acl getDeleteAcl();

    /**
     * The minimum security level that is required for managing this resource's security levels
     */
    Acl getManageAcl();

    /**
     * Returns true if the ACLs in this metadata object all equal to the ones of the supplied metadata object
     */
    boolean hasSameAcls(ResourceMetadata other);
}
