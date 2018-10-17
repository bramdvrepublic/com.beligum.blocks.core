package com.beligum.blocks.security;

import com.beligum.base.config.ifaces.SecurityConfig;
import com.beligum.base.filesystem.MessagesFileEntry;
import com.beligum.base.security.PermissionRole;
import com.beligum.blocks.security.ifaces.Acl;
import org.apache.shiro.authz.AuthorizationException;
import org.apache.shiro.authz.UnauthorizedException;

import java.util.Objects;

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
    @Override
    public boolean isPermitted(PermissionRole role)
    {
        //we assert a straightup level comparison
        return role.getLevel() <= this.getLevel();
    }
    @Override
    public void checkPermission(PermissionRole role) throws AuthorizationException
    {
        if (!this.isPermitted(role)) {
            throw new UnauthorizedException("Role '"+role+"' does not include ACL [" + this.getLabel() + "]");
        }
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----
    @Override
    public String toString()
    {
        return this.getLabel();
    }
    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (!(o instanceof AclImpl)) return false;
        AclImpl acl = (AclImpl) o;
        return getLevel() == acl.getLevel();
    }
    @Override
    public int hashCode()
    {
        return Objects.hash(getLevel());
    }
}
