package com.beligum.blocks.filesystem.pages;

import com.beligum.base.config.ifaces.SecurityConfig;
import com.beligum.base.security.PermissionRole;
import com.beligum.base.server.R;
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
import com.beligum.blocks.utils.SecurityTools;
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
    private Integer aclRead;
    private Integer aclUpdate;
    private Integer aclDelete;
    private Integer aclManage;

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
    public Integer getReadAcl()
    {
        return this.aclRead;
    }
    @Override
    public Integer getUpdateAcl()
    {
        return this.aclUpdate;
    }
    @Override
    public Integer getDeleteAcl()
    {
        return this.aclDelete;
    }
    @Override
    public Integer getManageAcl()
    {
        return this.aclManage;
    }
    @Override
    public boolean hasSameAcls(ResourceMetadata other)
    {
        boolean retVal = true;

        retVal = retVal && ((this.getReadAcl() != null && this.getReadAcl().equals(other.getReadAcl())) || (this.getReadAcl() == null && other.getReadAcl() == null));
        retVal = retVal && ((this.getUpdateAcl() != null && this.getUpdateAcl().equals(other.getUpdateAcl())) || (this.getUpdateAcl() == null && other.getUpdateAcl() == null));
        retVal = retVal && ((this.getDeleteAcl() != null && this.getDeleteAcl().equals(other.getDeleteAcl())) || (this.getDeleteAcl() == null && other.getDeleteAcl() == null));
        retVal = retVal && ((this.getManageAcl() != null && this.getManageAcl().equals(other.getManageAcl())) || (this.getManageAcl() == null && other.getManageAcl() == null));

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

        //note: we don't fill ACLs by default because it means a page-specific permission
        //      and we don't want every page to have a specific permission set (only global permission system if unset)
    }
    private void init(PageSource pageSource, List<Element> metaTags)
    {
        this.contributors = new LinkedHashSet<>();
        for (Element metaTag : metaTags) {
            this.parseMetaTag(pageSource.getUri(), metaTag.attr(HtmlParser.RDF_PROPERTY_ATTR), metaTag.attr(HtmlParser.RDF_CONTENT_ATTR));
        }

        //note: we don't fill ACLs by default because it means a page-specific permission
        //      and we don't want every page to have a specific permission set (only global permission system if unset)
    }
    private void parseMetaTag(URI baseUri, String property, String content)
    {
        if (!StringUtils.isEmpty(property)) {
            if (!StringUtils.isEmpty(content)) {
                if (property.equals(Terms.created.getName()) || property.equals(Terms.created.getCurieName().toString())) {
                    this.createdUtc = this.getDatatypeFactory().newXMLGregorianCalendar(content).toGregorianCalendar().toZonedDateTime();
                }
                else if (property.equals(Terms.creator.getName()) || property.equals(Terms.creator.getCurieName().toString())) {
                    this.creator = URI.create(content);
                    if (!this.creator.isAbsolute()) {
                        this.creator = baseUri.resolve(this.creator);
                    }
                }
                else if (property.equals(Terms.modified.getName()) || property.equals(Terms.modified.getCurieName().toString())) {
                    this.modifiedUtc = this.getDatatypeFactory().newXMLGregorianCalendar(content).toGregorianCalendar().toZonedDateTime();
                }
                else if (property.equals(Terms.contributor.getName()) || property.equals(Terms.contributor.getCurieName().toString())) {
                    URI contributor = URI.create(content);
                    if (!contributor.isAbsolute()) {
                        contributor = baseUri.resolve(contributor);
                    }
                    this.contributors.add(contributor);
                }
                else if (property.equals(Terms.aclRead.getName()) || property.equals(Terms.aclRead.getCurieName().toString())) {
                    int aclReadLevel = NumberUtils.toInt(content, -1);
                    if (aclReadLevel >= 0) {
                        this.aclRead = aclReadLevel;
                    }
                }
                else if (property.equals(Terms.aclUpdate.getName()) || property.equals(Terms.aclUpdate.getCurieName().toString())) {
                    int aclUpdateLevel = NumberUtils.toInt(content, -1);
                    if (aclUpdateLevel >= 0) {
                        this.aclUpdate = aclUpdateLevel;
                    }
                }
                else if (property.equals(Terms.aclDelete.getName()) || property.equals(Terms.aclDelete.getCurieName().toString())) {
                    int aclDeleteLevel = NumberUtils.toInt(content, -1);
                    if (aclDeleteLevel >= 0) {
                        this.aclDelete = aclDeleteLevel;
                    }
                }
                else if (property.equals(Terms.aclManage.getName()) || property.equals(Terms.aclManage.getCurieName().toString())) {
                    int aclManageLevel = NumberUtils.toInt(content, -1);
                    if (aclManageLevel >= 0) {
                        this.aclManage = aclManageLevel;
                    }
                }
            }
        }
    }
}
