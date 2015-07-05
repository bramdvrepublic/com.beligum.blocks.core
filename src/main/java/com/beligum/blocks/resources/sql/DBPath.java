package com.beligum.blocks.resources.sql;

import com.beligum.base.models.BasicModelImpl;
import com.beligum.blocks.config.BlocksConfig;
import com.beligum.blocks.resources.jackson.path.PathDeserializer;
import com.beligum.blocks.resources.jackson.path.PathSerializer;
import com.beligum.blocks.routing.ifaces.WebPath;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.hibernate.annotations.Type;
import org.joda.time.LocalDateTime;

import javax.persistence.*;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * Created by wouter on 30/06/15.
 */
@Entity
@Table(name="path")
public class DBPath extends DBDocumentInfo implements WebPath
{

    public static  ObjectMapper PATH_MAPPER = new ObjectMapper().registerModule(new SimpleModule().addSerializer(DBPath.class, new PathSerializer()).addDeserializer(DBPath.class, new PathDeserializer()));


    private String masterPage;

    private String language;

    private String url;

    private String localizedUrl;

    private int statusCode = 404;



    // Default constructor for hibernate
    public DBPath() {

    }

    public DBPath(URI masterPage, Path path, Locale locale) {
        this.masterPage = masterPage.toString();
        this.url = Paths.get("/").resolve(path).normalize().toString();
        this.language = locale.getLanguage();
        this.localizedUrl = Paths.get("/").resolve(Paths.get(language)).resolve(url).normalize().toString();
    }

    @Override
    public String getDBid() {
        return this.id.toString();
    }

    public void setDBid(String id) {
        this.id = Long.parseLong(id);
    }

    @Override
    public URI getMasterPage()
    {
        return UriBuilder.fromUri(masterPage).build();
    }

    @Override
    public Locale getLanguage()
    {
        return BlocksConfig.instance().getLocaleForLanguage(this.language);
    }

    @Override
    public Path getUrl()
    {
        return Paths.get(url);
    }

    @Override
    public Path getLocalizedUrl()
    {
        return Paths.get(localizedUrl);
    }

    @Override
    public int getStatusCode()
    {
        return statusCode;
    }

    public void setStatusCode(int code)
    {
        statusCode = code;
    }


    @Override
    public void setPageOk(URI masterPage)
    {
        this.statusCode = 200;
        this.masterPage = masterPage.toString();
    }
    @Override
    public void setPageRedirect(URI masterPage)
    {
        statusCode = 303;
    }
    @Override
    public void setPageNotFound()
    {
        statusCode = 404;
    }
    @Override
    public boolean isNotFound()
    {
        return statusCode == 404;
    }
    @Override
    public boolean isPage()
    {
        return statusCode == 200;
    }
    @Override
    public boolean isRedirect()
    {
        return statusCode == 303;
    }

    @Override
    public String toJson() throws JsonProcessingException
    {
        return PATH_MAPPER.writeValueAsString(this);
    }

}
