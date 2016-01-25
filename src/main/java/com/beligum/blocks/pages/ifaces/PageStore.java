package com.beligum.blocks.pages.ifaces;

import com.beligum.base.auth.models.Person;
import com.beligum.base.resources.Asset;
import com.beligum.blocks.rdf.ifaces.Source;
import gen.com.beligum.blocks.core.maven;
import org.apache.tika.mime.MediaType;

import java.io.IOException;

/**
 * Created by bram on 1/14/16.
 */
public interface PageStore
{
    MediaType PAGE_PROXY_NORMALIZED_MIME_TYPE = Asset.MimeType.HTML.getMimeType();
    //Note: makes sense to prefix with the mvn artifact, so we know what we wrote it with
    String PAGE_PROXY_NORMALIZED_FILE_NAME = maven.Entries.project_artifactId.getValue()+"_normalized."+Asset.MimeType.HTML.getExtension();

    MediaType PAGE_PROXY_RDF_JSONLD_MIME_TYPE = Asset.MimeType.JSONLD.getMimeType();
    // Note: makes sense to prefix with the mvn artifact, so we know what we wrote it with
    // also note that the reason it's suffixed 'rdf' is that JSONLD is a Resource Description Framework serialization format:
    // https://en.wikipedia.org/wiki/Linked_data
    String PAGE_PROXY_RDF_JSONLD_FILE_NAME = maven.Entries.project_artifactId.getValue() + "_rdf." + Asset.MimeType.JSONLD.getExtension();

    //-----PUBLIC METHODS-----
    void init() throws IOException;
    void save(Source source, Person creator) throws IOException;
}
