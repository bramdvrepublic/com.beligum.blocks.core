package com.beligum.blocks.security;

import com.beligum.base.config.ifaces.SecurityConfig;
import com.beligum.base.filesystem.MessagesFileEntry;
import com.beligum.base.security.PermissionRole;
import com.beligum.blocks.security.ifaces.Acl;

public class AclImpl implements Acl
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    private int level;
    private Object label;

    //-----CONSTRUCTORS-----
    public AclImpl(PermissionRole role)
    {
        this(role.getLevel(), role.getLabelRaw());
    }
    public AclImpl(int level, MessagesFileEntry label)
    {
        this(level, (Object) label);
    }
    public AclImpl(int level, String label)
    {
        this(level, (Object) label);
    }
    private AclImpl(int level, Object label)
    {
        this.level = level;
        this.label = label;
    }

    //-----PUBLIC METHODS-----
    @Override
    public int getLevel()
    {
        return level;
    }
    @Override
    public String getLabel()
    {
        return this.label == null ? null : this.label.toString();
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----
}
