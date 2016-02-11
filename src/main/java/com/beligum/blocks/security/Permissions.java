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
    public enum Action
    {
        PAGE_CREATE(PAGE_CREATE_PERMISSION_STRING),
        PAGE_MODIFY(PAGE_MODIFY_PERMISSION_STRING),
        PAGE_DELETE(PAGE_DELETE_PERMISSION_STRING),

        ;

        Permission permission;
        Action(String action)
        {
            this.permission = new WildcardPermission(action);
        }
        public Permission getPermission()
        {
            return permission;
        }
        @Override
        public String toString()
        {
            return this.permission.toString();
        }
    }
    //to use in annotations
    /**
     * Note: only use this if you can't use the above enum (eg. only in @RequiresPermissions annotations)
     */
    public static final String PAGE_CREATE_PERMISSION_STRING = "page:create";
    /**
     * Note: only use this if you can't use the above enum (eg. only in @RequiresPermissions annotations)
     */
    public static final String PAGE_MODIFY_PERMISSION_STRING = "page:modify";
    /**
     * Note: only use this if you can't use the above enum (eg. only in @RequiresPermissions annotations)
     */
    public static final String PAGE_DELETE_PERMISSION_STRING = "page:delete";

    //-----ROLE/PERMISSION MAPPINGS-----
    private static final Map<PermissionRole, ImmutableSet<Permission>> PERMISSIONS =
                    ImmutableMap.of(
                                    PermissionsConfigurator.ROLE_ADMIN, ImmutableSet.of(
                                                    Action.PAGE_CREATE.getPermission(),
                                                    Action.PAGE_MODIFY.getPermission(),
                                                    Action.PAGE_DELETE.getPermission()
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
