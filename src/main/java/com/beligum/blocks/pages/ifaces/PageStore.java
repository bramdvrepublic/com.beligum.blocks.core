package com.beligum.blocks.pages.ifaces;

import com.beligum.base.auth.models.Person;
import com.beligum.blocks.rdf.ifaces.Source;
import com.google.common.net.MediaType;
import gen.com.beligum.blocks.core.maven;

import java.io.IOException;

/**
 * Created by bram on 1/14/16.
 */
public interface PageStore
{
    MediaType PAGE_PROXY_NORMALIZED_MIME_TYPE = MediaType.HTML_UTF_8;
    //Note: makes sense to prefix with the mvn artifact, so we know what we wrote it with
    String PAGE_PROXY_NORMALIZED_FILE_NAME = maven.Entries.project_artifactId.getValue()+"_normalized.html";

    //-----PUBLIC METHODS-----
    void init() throws IOException;
    void save(Source source, Person creator) throws IOException;
}
