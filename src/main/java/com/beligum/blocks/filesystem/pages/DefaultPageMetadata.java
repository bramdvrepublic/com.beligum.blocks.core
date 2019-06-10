package com.beligum.blocks.filesystem.pages;

import com.beligum.base.utils.Logger;
import com.beligum.blocks.filesystem.AbstractResourceMetadata;
import com.beligum.blocks.filesystem.ifaces.ResourceMetadata;
import com.beligum.blocks.filesystem.pages.ifaces.Page;
import com.beligum.blocks.filesystem.pages.ifaces.PageMetadata;
import com.beligum.blocks.rdf.ontologies.Meta;
import com.beligum.blocks.templating.blocks.HtmlParser;
import com.beligum.blocks.templating.blocks.analyzer.HtmlAnalyzer;
import com.beligum.blocks.templating.blocks.analyzer.HtmlTag;
import com.google.common.base.Function;
import com.google.common.collect.Maps;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.net.URI;
import java.time.ZonedDateTime;
import java.util.*;

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
    @Override
    public Map<Locale, URI> getTranslations()
    {
        Map<Locale, URI> retVal = null;

        try {
            Map<Locale, Page> translations = this.page.getTranslations();
            if (translations != null) {
                retVal = Maps.transformValues(translations, new Function<Page, URI>()
                {
                    @Override
                    public URI apply(Page input)
                    {
                        return input == null ? null : input.getPublicAbsoluteAddress();
                    }
                });
            }
        }
        catch (IOException e) {
            Logger.error("Error while fetching the translations of a page; " + this.page);
        }

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
                if (property.equals(Meta.created.getName()) || property.equals(Meta.created.getCurie().toString())) {
                    this.createdUtc = this.getDatatypeFactory().newXMLGregorianCalendar(content).toGregorianCalendar().toZonedDateTime();
                }
                else if (property.equals(Meta.creator.getName()) || property.equals(Meta.creator.getCurie().toString())) {
                    this.creator = URI.create(content);
                    if (!this.creator.isAbsolute()) {
                        this.creator = baseUri.resolve(this.creator);
                    }
                }
                else if (property.equals(Meta.modified.getName()) || property.equals(Meta.modified.getCurie().toString())) {
                    this.modifiedUtc = this.getDatatypeFactory().newXMLGregorianCalendar(content).toGregorianCalendar().toZonedDateTime();
                }
                else if (property.equals(Meta.contributor.getName()) || property.equals(Meta.contributor.getCurie().toString())) {
                    URI contributor = URI.create(content);
                    if (!contributor.isAbsolute()) {
                        contributor = baseUri.resolve(contributor);
                    }
                    this.contributors.add(contributor);
                }
                else if (property.equals(Meta.aclRead.getName()) || property.equals(Meta.aclRead.getCurie().toString())) {
                    int aclReadLevel = NumberUtils.toInt(content, -1);
                    if (aclReadLevel >= 0) {
                        this.aclRead = aclReadLevel;
                    }
                }
                else if (property.equals(Meta.aclUpdate.getName()) || property.equals(Meta.aclUpdate.getCurie().toString())) {
                    int aclUpdateLevel = NumberUtils.toInt(content, -1);
                    if (aclUpdateLevel >= 0) {
                        this.aclUpdate = aclUpdateLevel;
                    }
                }
                else if (property.equals(Meta.aclDelete.getName()) || property.equals(Meta.aclDelete.getCurie().toString())) {
                    int aclDeleteLevel = NumberUtils.toInt(content, -1);
                    if (aclDeleteLevel >= 0) {
                        this.aclDelete = aclDeleteLevel;
                    }
                }
                else if (property.equals(Meta.aclManage.getName()) || property.equals(Meta.aclManage.getCurie().toString())) {
                    int aclManageLevel = NumberUtils.toInt(content, -1);
                    if (aclManageLevel >= 0) {
                        this.aclManage = aclManageLevel;
                    }
                }
            }
        }
    }
}
