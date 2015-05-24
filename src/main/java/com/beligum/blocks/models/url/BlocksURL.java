package com.beligum.blocks.models.url;

import com.beligum.base.models.BasicModelImpl;
import com.beligum.base.models.ifaces.BasicModel;
import com.beligum.blocks.base.Blocks;

import javax.persistence.*;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.Locale;

/**
 * Created by wouter on 30/04/15.
 */
@Entity
@Inheritance(strategy= InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(discriminatorType = DiscriminatorType.STRING)
@DiscriminatorValue("ABSTRACT")
@Table(name="routing",
                uniqueConstraints=
                @UniqueConstraint(columnNames={"domain", "path", "language"}))
public abstract class BlocksURL extends BasicModelImpl
{
    private String domain;
    private String path;
    private Locale language;

    protected BlocksURL() {

    }

    public BlocksURL(URI url, Locale language) {
        this.domain = url.getAuthority();
        this.path = url.getPath();
        this.language = language;
    }

    public String getDomain()
    {
        return domain;
    }

    public void setDomain(String domain)
    {
        this.domain = domain;
    }

    public String getPath()
    {
        return path;
    }

    public void setPath(String path)
    {
        this.path = path;
    }

    public Locale getLanguage() {
        return this.language;
    }

    public void setLanguage(Locale lang) {
        this.language = lang;
    }

    public abstract Response response(Locale language);

    public abstract int statusCode();

}
