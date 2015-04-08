package com.beligum.blocks.usermanagement;

import com.beligum.base.security.PermissionRole;
import com.beligum.base.security.PermissionsConfigurator;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.apache.shiro.authz.Permission;
import org.apache.shiro.authz.permission.WildcardPermission;

import java.util.HashSet;
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
    public static final String ENTITY_MODIFY = "entity:modify";
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
                                                    (Permission) new WildcardPermission(ENTITY_MODIFY),
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

    public static Set<String> getRoleNames(){
        Set<String> roleNames = new HashSet<>();
        for(PermissionRole permission : PERMISSIONS.keySet()){
            roleNames.add(permission.getRoleName());
        }
        return roleNames;
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----
}
