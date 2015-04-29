package com.beligum.blocks.security;

import com.beligum.base.security.PermissionRole;
import com.beligum.base.security.PermissionsConfigurator;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.apache.shiro.authz.Permission;
import org.apache.shiro.authz.permission.WildcardPermission;

import java.util.Map;
import java.util.Set;

/**
 * Created by bram on 4/26/15.
 */
public class Permissions implements PermissionsConfigurator
{
    //-----CONSTANTS-----

    //-----ROLES-----

    //-----PERMISSIONS-----
    public static final String ENTITY_MODIFY = "entity:modify";

    //-----ROLE/PERMISSION MAPPINGS-----
    private static final Map<PermissionRole, ImmutableSet<Permission>> PERMISSIONS =
                    ImmutableMap.of(
                                    PermissionsConfigurator.ROLE_ADMIN, ImmutableSet.of(
                                                    (Permission) new WildcardPermission(ENTITY_MODIFY)
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
