package com.beligum.blocks.core.usermanagement;

import com.beligum.core.framework.security.PermissionRole;
import com.beligum.core.framework.security.PermissionsConfigurator;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.apache.shiro.authz.Permission;
import org.apache.shiro.authz.permission.WildcardPermission;

import java.util.Map;
import java.util.Set;

/**
 * Created by bas on 27.01.15.
 */
public class Permissions implements PermissionsConfigurator
{
    //-----CONSTANTS-----

    //-----ROLES-----

    //-----PERMISSIONS-----
    public static final String USER_LOGGEDIN = "user:loggedIn";
    public static final String USER_CREATE = "user:create";
    public static final String USER_DELETE = "user:delete";

    //-----ROLE/PERMISSION MAPPINGS-----
    private static final Map<PermissionRole, ImmutableSet<Permission>> PERMISSIONS =
                    ImmutableMap.of(
                                    PermissionsConfigurator.ROLE_USER, ImmutableSet.of(
                                                    (Permission) new WildcardPermission(USER_LOGGEDIN)
                                    ),
                                    PermissionsConfigurator.ROLE_ADMIN, ImmutableSet.of(
                                                    (Permission) new WildcardPermission(USER_CREATE),
                                                    (Permission) new WildcardPermission(USER_DELETE)
                                    )
                    );

    //-----VARIABLES-----

    //-----CONSTRUCTORS-----

    //-----PUBLIC METHODS-----
    @Override
    public Set<PermissionRole> getPermissionGroups()
    {
        return PERMISSIONS.keySet();
    }
    @Override
    public Set<Permission> getPermissionsFor(PermissionRole permRole)
    {
        return PERMISSIONS.get(permRole);
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----
}
