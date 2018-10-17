package com.beligum.blocks.utils;

import com.beligum.base.config.ifaces.SecurityConfig;
import com.beligum.base.security.PermissionRole;
import com.beligum.base.server.R;
import com.beligum.blocks.config.Settings;
import com.beligum.blocks.security.ifaces.Acl;
import org.apache.commons.lang.StringUtils;
import org.apache.shiro.authz.AuthorizationException;
import org.apache.shiro.authz.UnauthorizedException;

public class SecurityTools
{
    //-----CONSTANTS-----

    //-----VARIABLES-----

    //-----CONSTRUCTORS-----

    //-----PUBLIC METHODS-----
    /**
     * Returns the default read ACL level.
     * If no default level is set in the configuration, we return either the current role level or anonymous role level,
     * depending if the restricted-default-read config setting is up or down
     */
    public static int getDefaultReadAclLevel()
    {
        //Note: by default, we allow all anonymous visitors to view the pages by default
        //      if the restricted setting is active, viewing will be restricted to the level of the current user
        Integer retVal = Settings.instance().getDefaultLevelRead();
        if (retVal == null) {
            retVal = Settings.instance().getEnableRestrictedDefaultRead() ? R.securityManager().getCurrentRole().getLevel() : SecurityConfig.ANON_ROLE.getLevel();
        }

        return retVal;
    }
    /**
     * Gets the default update ACL level.
     * f no default level is set in the configuration, we return the current role level.
     */
    public static int getDefaultUpdateAclLevel()
    {
        Integer retVal = Settings.instance().getDefaultLevelUpdate();
        if (retVal == null) {
            retVal = R.securityManager().getCurrentRole().getLevel();
        }

        return retVal;
    }
    /**
     * Gets the default delete ACL level.
     * f no default level is set in the configuration, we return the current role level.
     */
    public static int getDefaultDeleteAclLevel()
    {
        Integer retVal = Settings.instance().getDefaultLevelDelete();
        if (retVal == null) {
            retVal = R.securityManager().getCurrentRole().getLevel();
        }

        return retVal;
    }
    /**
     * Gets the default manage ACL level.
     * f no default level is set in the configuration, we return the current role level.
     */
    public static int getDefaultManageAclLevel()
    {
        Integer retVal = Settings.instance().getDefaultLevelManage();
        if (retVal == null) {
            retVal = R.securityManager().getCurrentRole().getLevel();
        }

        return retVal;
    }
    /**
     * Returns false if this ACL is not allowed for the supplied role
     */
    public static boolean isPermitted(PermissionRole role, int acl)
    {
        //we assert a straightup level comparison
        return role.getLevel() <= acl;
    }

    /**
     * Throws an exception if this ACL is not allowed for the supplied role
     */
    public static void checkPermission(PermissionRole role, int acl) throws AuthorizationException
    {
        if (!isPermitted(role, acl)) {
            throw new UnauthorizedException("Role '" + role + "' has no permission for ACL [" + acl + "]");
        }
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----
}
