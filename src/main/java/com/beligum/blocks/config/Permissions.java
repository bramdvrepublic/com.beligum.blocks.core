/*
 * Copyright 2017 Republic of Reinvention bvba. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.beligum.blocks.config;

import com.beligum.base.config.ifaces.SecurityConfig;
import com.beligum.base.security.Permission;
import com.beligum.base.security.PermissionFactory;
import com.beligum.base.security.PermissionImpl;
import com.google.common.collect.ImmutableSet;
import gen.com.beligum.blocks.core.constants.blocks.core;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * Created by bram on 4/26/15.
 */
public class Permissions implements PermissionFactory
{
    //-----CONSTANTS-----
    public static final Permission PAGE_CREATE_PERMISSION = new PermissionImpl(core.Entries.PAGE_CREATE_PERMISSION, SecurityConfig.DEFAULT_ADMIN_ROLE);
    public static final Permission PAGE_EDIT_PERMISSION = new PermissionImpl(core.Entries.PAGE_EDIT_PERMISSION, SecurityConfig.DEFAULT_ADMIN_ROLE);
    public static final Permission PAGE_DELETE_PERMISSION = new PermissionImpl(core.Entries.PAGE_DELETE_PERMISSION, SecurityConfig.DEFAULT_ADMIN_ROLE);
    public static final Permission PAGE_REINDEX_PERMISSION = new PermissionImpl(core.Entries.PAGE_REINDEX_PERMISSION, SecurityConfig.DEFAULT_ADMIN_ROLE);

    protected static final ImmutableSet<Permission> PERMISSIONS = ImmutableSet.of(PAGE_CREATE_PERMISSION,
                                                                                  PAGE_EDIT_PERMISSION,
                                                                                  PAGE_DELETE_PERMISSION,
                                                                                  PAGE_REINDEX_PERMISSION
    );

    //-----VARIABLES-----

    //-----CONSTRUCTORS-----

    //-----PUBLIC METHODS-----
    @Override
    public Collection<Permission> getPermissions()
    {
        return PERMISSIONS;
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----
}
