package com.beligum.blocks.filesystem.pages;

import com.beligum.blocks.filesystem.AbstractResourceMetadata;
import com.beligum.blocks.filesystem.pages.ifaces.Page;
import com.beligum.blocks.filesystem.pages.ifaces.PageMetadata;
import com.beligum.blocks.rdf.ontology.vocabularies.local.factories.Terms;
import com.beligum.blocks.templating.blocks.HtmlParser;
import com.beligum.blocks.templating.blocks.analyzer.HtmlAnalyzer;
import com.beligum.blocks.templating.blocks.analyzer.HtmlTag;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.net.URI;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.LinkedHashSet;

public class DefaultPageMetadata extends AbstractResourceMetadata implements PageMetadata
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    private Page page;
    private ZonedDateTime createdUtc;
    private URI creator;
    private ZonedDateTime modifiedUtc;
    private Collection<URI> contributors;
    private Integer aclRead;
    private Integer aclUpdate;
    private Integer aclDelete;
    private Integer aclManage;

    //-----CONSTRUCTORS-----
    public DefaultPageMetadata(Page page) throws IOException
    {
        super();

        this.page = page;

        this.init();
    }

    //-----PUBLIC METHODS-----
    @Override
    public ZonedDateTime getCreated()
    {
        return this.createdUtc;
    }
    @Override
    public URI getCreator()
    {
        return this.creator;
    }
    @Override
    public ZonedDateTime getLastModified()
    {
        return this.modifiedUtc;
    }
    @Override
    public Collection<URI> getContributors()
    {
        return this.contributors;
    }
    @Override
    public Integer getReadAccessControlLevel()
    {
        return this.aclRead;
    }
    @Override
    public Integer getUpdateAccessControlLevel()
    {
        return this.aclUpdate;
    }
    @Override
    public Integer getDeleteAccessControlLevel()
    {
        return this.aclDelete;
    }
    @Override
    public Integer getManageAccessControlLevel()
    {
        return this.aclManage;
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----
    private void init() throws IOException
    {
        //we must force the analyzer to read the original html because we don't want
        //to get the normalized html (eg. for ReadOnlyPages)
        HtmlAnalyzer analyzer = this.page.createAnalyzer(true);

        this.contributors = new LinkedHashSet<>();
        for (HtmlTag metaTag : analyzer.getMetaTags()) {
            String property = metaTag.getAttributeValue(HtmlParser.RDF_PROPERTY_ATTR);
            if (!StringUtils.isEmpty(property)) {
                String content = metaTag.getAttributeValue(HtmlParser.RDF_CONTENT_ATTR);
                if (!StringUtils.isEmpty(content)) {
                    if (property.equals(Terms.created.getName())) {
                        this.createdUtc = this.getDatatypeFactory().newXMLGregorianCalendar(content).toGregorianCalendar().toZonedDateTime();
                    }
                    else if (property.equals(Terms.creator.getName())) {
                        this.creator = URI.create(content);
                        if (!this.creator.isAbsolute()) {
                            this.creator = this.page.getUri().resolve(this.creator);
                        }
                    }
                    else if (property.equals(Terms.modified.getName())) {
                        this.modifiedUtc = this.getDatatypeFactory().newXMLGregorianCalendar(content).toGregorianCalendar().toZonedDateTime();
                    }
                    else if (property.equals(Terms.contributor.getName())) {
                        URI contributor = URI.create(content);
                        if (!contributor.isAbsolute()) {
                            contributor = this.page.getUri().resolve(contributor);
                        }
                        this.contributors.add(contributor);
                    }
                    else if (property.equals(Terms.aclRead.getName())) {
                        this.aclRead = Integer.valueOf(content);
                    }
                    else if (property.equals(Terms.aclUpdate.getName())) {
                        this.aclUpdate = Integer.valueOf(content);
                    }
                    else if (property.equals(Terms.aclDelete.getName())) {
                        this.aclDelete = Integer.valueOf(content);
                    }
                    else if (property.equals(Terms.aclManage.getName())) {
                        this.aclManage = Integer.valueOf(content);
                    }
                }
            }
        }
    }
}
