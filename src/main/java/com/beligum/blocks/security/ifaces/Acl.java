package com.beligum.blocks.security.ifaces;

import com.beligum.base.security.PermissionRole;
import org.apache.shiro.authz.AuthorizationException;

/**
 * Our ACL is actually an Access Control Level. It's a security mapping between the resources and the PermissionRoles
 * because it only maps a level value (integer) to the PermissionRole level.
 * The buys us more flexibility when creating 'access levels' to our resources because this ACL
 * (and more specifically it's label) is therefore independent of the security roles in our system.
 * Example of added flexibility:
 * Suppose we have 4 roles (anonymous, user, admin and super), but we want to create only two access levels
 * for the resources. Therefore, we create two ACLs: one with the level of anonymous, and one with the
 * level of user. This way, we can simplify the management of our pages for the end users.
 *
 * Note that and ACL is also closely related to a ResourceAction, but for now, we have split them
 * out entirely. See eg. AbstractPage.isPermitted() for an example of how they are related.
 */
public interface Acl
{
    //-----CONSTANTS-----

    //-----VARIABLES-----

    //-----PUBLIC METHODS-----

    /**
     * This main representation of this ACL as the minimum role level (inclusive) a principal needs to have to be part of it
     */
    int getLevel();

    /**
     * The human-readable label of this role, to be used in UI
     */
    String getLabel();

    /**
     * Returns false if this ACL is not allowed for the supplied role
     */
    boolean isPermitted(PermissionRole role);

    /**
     * Throws an exception if this ACL is not allowed for the supplied role
     */
    void checkPermission(PermissionRole role) throws AuthorizationException;

}
