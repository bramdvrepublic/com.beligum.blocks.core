package com.beligum.blocks.filesystem.pages;

import com.beligum.blocks.config.Settings;
import com.beligum.blocks.filesystem.AbstractResourceMetadata;
import com.beligum.blocks.filesystem.ifaces.ResourceMetadata;
import com.beligum.blocks.filesystem.pages.ifaces.Page;
import com.beligum.blocks.filesystem.pages.ifaces.PageMetadata;
import com.beligum.blocks.rdf.ontology.vocabularies.local.factories.Terms;
import com.beligum.blocks.rdf.sources.PageSource;
import com.beligum.blocks.security.ifaces.Acl;
import com.beligum.blocks.templating.blocks.HtmlParser;
import com.beligum.blocks.templating.blocks.analyzer.HtmlAnalyzer;
import com.beligum.blocks.templating.blocks.analyzer.HtmlTag;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.net.URI;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;

public class DefaultPageMetadata extends AbstractResourceMetadata implements PageMetadata
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    private Page page;
    private PageSource pageSource;
    private ZonedDateTime createdUtc;
    private URI creator;
    private ZonedDateTime modifiedUtc;
    private Collection<URI> contributors;
    private Acl aclRead;
    private Acl aclUpdate;
    private Acl aclDelete;
    private Acl aclManage;

    //-----CONSTRUCTORS-----
    public DefaultPageMetadata(Page page) throws IOException
    {
        super();

        this.page = page;

        this.init(this.page);
    }
    public DefaultPageMetadata(PageSource pageSource, List<Element> metaTags) throws IOException
    {
        super();

        this.pageSource = pageSource;

        this.init(this.pageSource, metaTags);
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
    public Acl getReadAcl()
    {
        return this.aclRead;
    }
    @Override
    public Acl getUpdateAcl()
    {
        return this.aclUpdate;
    }
    @Override
    public Acl getDeleteAcl()
    {
        return this.aclDelete;
    }
    @Override
    public Acl getManageAcl()
    {
        return this.aclManage;
    }
    @Override
    public boolean hasSameAcls(ResourceMetadata other)
    {
        boolean retVal = true;

        retVal = retVal && this.getReadAcl() != null && this.getReadAcl().equals(other.getReadAcl());
        retVal = retVal && this.getUpdateAcl() != null && this.getUpdateAcl().equals(other.getUpdateAcl());
        retVal = retVal && this.getDeleteAcl() != null && this.getDeleteAcl().equals(other.getDeleteAcl());
        retVal = retVal && this.getManageAcl() != null && this.getManageAcl().equals(other.getManageAcl());

        return retVal;
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----
    private void init(Page page) throws IOException
    {
        //we must force the analyzer to read the original html because we don't want
        //to get the normalized html (eg. for ReadOnlyPages)
        HtmlAnalyzer analyzer = page.createAnalyzer(true);

        this.contributors = new LinkedHashSet<>();
        for (HtmlTag metaTag : analyzer.getMetaTags()) {
            this.parseMetaTag(page.getUri(), metaTag.getAttributeValue(HtmlParser.RDF_PROPERTY_ATTR), metaTag.getAttributeValue(HtmlParser.RDF_CONTENT_ATTR));
        }
    }
    private void init(PageSource pageSource, List<Element> metaTags)
    {
        this.contributors = new LinkedHashSet<>();
        for (Element metaTag : metaTags) {
            this.parseMetaTag(pageSource.getUri(), metaTag.attr(HtmlParser.RDF_PROPERTY_ATTR), metaTag.attr(HtmlParser.RDF_CONTENT_ATTR));
        }
    }
    private void parseMetaTag(URI baseUri, String property, String content)
    {
        if (!StringUtils.isEmpty(property)) {
            if (!StringUtils.isEmpty(content)) {
                if (property.equals(Terms.created.getName())) {
                    this.createdUtc = this.getDatatypeFactory().newXMLGregorianCalendar(content).toGregorianCalendar().toZonedDateTime();
                }
                else if (property.equals(Terms.creator.getName())) {
                    this.creator = URI.create(content);
                    if (!this.creator.isAbsolute()) {
                        this.creator = baseUri.resolve(this.creator);
                    }
                }
                else if (property.equals(Terms.modified.getName())) {
                    this.modifiedUtc = this.getDatatypeFactory().newXMLGregorianCalendar(content).toGregorianCalendar().toZonedDateTime();
                }
                else if (property.equals(Terms.contributor.getName())) {
                    URI contributor = URI.create(content);
                    if (!contributor.isAbsolute()) {
                        contributor = baseUri.resolve(contributor);
                    }
                    this.contributors.add(contributor);
                }
                else if (property.equals(Terms.aclRead.getName())) {
                    int aclReadLevel = NumberUtils.toInt(content, -1);
                    if (aclReadLevel >= 0) {
                        this.aclRead = Settings.instance().getAcls().get(aclReadLevel);
                    }
                }
                else if (property.equals(Terms.aclUpdate.getName())) {
                    int aclUpdateLevel = NumberUtils.toInt(content, -1);
                    if (aclUpdateLevel >= 0) {
                        this.aclUpdate = Settings.instance().getAcls().get(aclUpdateLevel);
                    }
                }
                else if (property.equals(Terms.aclDelete.getName())) {
                    int aclDeleteLevel = NumberUtils.toInt(content, -1);
                    if (aclDeleteLevel >= 0) {
                        this.aclDelete = Settings.instance().getAcls().get(aclDeleteLevel);
                    }
                }
                else if (property.equals(Terms.aclManage.getName())) {
                    int aclManageLevel = NumberUtils.toInt(content, -1);
                    if (aclManageLevel >= 0) {
                        this.aclManage = Settings.instance().getAcls().get(aclManageLevel);
                    }
                }
            }
        }
    }
}
