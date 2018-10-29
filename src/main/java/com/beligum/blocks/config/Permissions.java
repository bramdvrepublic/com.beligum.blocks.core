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

/**
 * Created by bram on 4/26/15.
 */
public class Permissions implements PermissionFactory
{
    //-----CONSTANTS-----
    public static final Permission PAGE_READ_ALL_PERM = new PermissionImpl(core.Entries.PAGE_READ_ALL_PERM);
    public static final Permission PAGE_READ_ALL_HTML_PERM = new PermissionImpl(core.Entries.PAGE_READ_ALL_HTML_PERM, SecurityConfig.ANON_ROLE);
    public static final Permission PAGE_READ_ALL_RDF_PERM = new PermissionImpl(core.Entries.PAGE_READ_ALL_RDF_PERM, SecurityConfig.ANON_ROLE);
    public static final Permission PAGE_CREATE_ALL_PERM = new PermissionImpl(core.Entries.PAGE_CREATE_ALL_PERM, SecurityConfig.DEFAULT_ADMIN_ROLE);
    public static final Permission PAGE_CREATE_TEMPLATE_ALL_PERM = new PermissionImpl(core.Entries.PAGE_CREATE_TEMPLATE_ALL_PERM, SecurityConfig.DEFAULT_ADMIN_ROLE);
    public static final Permission PAGE_CREATE_COPY_ALL_PERM = new PermissionImpl(core.Entries.PAGE_CREATE_COPY_ALL_PERM, SecurityConfig.DEFAULT_ADMIN_ROLE);
    public static final Permission PAGE_UPDATE_ALL_PERM = new PermissionImpl(core.Entries.PAGE_UPDATE_ALL_PERM, SecurityConfig.DEFAULT_ADMIN_ROLE);
    public static final Permission PAGE_UPDATE_OWN_PERM = new PermissionImpl(core.Entries.PAGE_UPDATE_OWN_PERM, SecurityConfig.DEFAULT_ADMIN_ROLE);
    public static final Permission PAGE_DELETE_ALL_PERM = new PermissionImpl(core.Entries.PAGE_DELETE_ALL_PERM, SecurityConfig.DEFAULT_ADMIN_ROLE);
    public static final Permission PAGE_DELETE_OWN_PERM = new PermissionImpl(core.Entries.PAGE_DELETE_OWN_PERM, SecurityConfig.DEFAULT_ADMIN_ROLE);
    public static final Permission PAGE_REINDEX_ALL_PERM = new PermissionImpl(core.Entries.PAGE_REINDEX_ALL_PERM, SecurityConfig.DEFAULT_ADMIN_ROLE);

    public static final Permission RDF_CLASS_READ_ALL_PERM = new PermissionImpl(core.Entries.RDF_CLASS_READ_ALL_PERM, SecurityConfig.DEFAULT_ADMIN_ROLE);
    public static final Permission RDF_PROPERTY_READ_ALL_PERM = new PermissionImpl(core.Entries.RDF_PROPERTY_READ_ALL_PERM, SecurityConfig.DEFAULT_ADMIN_ROLE);
    public static final Permission RDF_RESOURCE_READ_ALL_PERM = new PermissionImpl(core.Entries.RDF_RESOURCE_READ_ALL_PERM, SecurityConfig.DEFAULT_ADMIN_ROLE);

    protected static final ImmutableSet<Permission> PERMS = ImmutableSet.of(PAGE_READ_ALL_PERM,
                                                                            PAGE_READ_ALL_HTML_PERM,
                                                                            PAGE_READ_ALL_RDF_PERM,
                                                                            PAGE_CREATE_ALL_PERM,
                                                                            PAGE_UPDATE_ALL_PERM,
                                                                            PAGE_UPDATE_OWN_PERM,
                                                                            PAGE_DELETE_ALL_PERM,
                                                                            PAGE_DELETE_OWN_PERM,
                                                                            PAGE_REINDEX_ALL_PERM,
                                                                            RDF_CLASS_READ_ALL_PERM,
                                                                            RDF_PROPERTY_READ_ALL_PERM,
                                                                            RDF_RESOURCE_READ_ALL_PERM
    );

    //-----VARIABLES-----

    //-----CONSTRUCTORS-----

    //-----PUBLIC METHODS-----
    @Override
    public Collection<Permission> getPermissions()
    {
        return PERMS;
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----
}
