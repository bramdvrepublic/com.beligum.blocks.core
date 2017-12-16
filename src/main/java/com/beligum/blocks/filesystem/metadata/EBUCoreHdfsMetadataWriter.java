/*
 * Copyright 2017 Republic of Reinvention bvba. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.beligum.blocks.filesystem.metadata;

import com.beligum.base.config.CoreConfiguration;
import com.beligum.base.models.Person;
import com.beligum.blocks.filesystem.ifaces.BlocksResource;
import com.beligum.blocks.schema.ebucore.v1_6.jaxb.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.hadoop.fs.CreateFlag;
import org.apache.hadoop.fs.FileContext;
import org.apache.hadoop.fs.Options;

import javax.xml.bind.JAXB;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.io.*;
import java.lang.String;
import java.util.EnumSet;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;

/**
 * Created by bram on 1/20/16.
 */
public class EBUCoreHdfsMetadataWriter extends AbstractHdfsMetadataWriter
{
    //-----CONSTANTS-----
    public static final String CREATOR_ROLE_SOFTWARE_LABEL = "software";
    public static class EBUCoreVersionInfo
    {
        public static final String NAME = "EBU Core Metadata Set";
        public static final String VERSION = "1.6";
        public static final String WEBSITE = "https://tech.ebu.ch/publications/tech3293";
        public static final String SCHEMA_URI = "https://www.ebu.ch/metadata/schemas/EBUCore/20150630/ebucore.xsd";
        public static final String SCHEMA_RESOURCE_PATH = "/com/beligum/blocks/schema/ebucore/v1_6/ebucore.xsd";
        public static final String SCHEMA_RESOURCE_PATH_GZ = "/com/beligum/blocks/schema/ebucore/v1_6/ebucore.xsd.gz";
    }

    //-----VARIABLES-----
    //valid during an entire session (after a successful init())
    private ObjectFactory factory;

    //valid during an open/write session, nulled after close()
    private EbuCoreMainType root;

    //-----CONSTRUCTORS-----
    public EBUCoreHdfsMetadataWriter(FileContext fileSystem) throws IOException
    {
        super(fileSystem);

        this.factory = new ObjectFactory();
    }

    //-----PUBLIC METHODS-----
    @Override
    public void open(BlocksResource blocksResource) throws IOException
    {
        super.open(blocksResource);

        //read in the existing metadata if it exists, or instance a new instance if it doesn't
        if (fileSystem.util().exists(this.baseMetadataFile)) {
            try (Reader reader = new BufferedReader(new InputStreamReader(fileSystem.open(this.baseMetadataFile)))) {
                this.root = JAXB.unmarshal(reader, EbuCoreMainType.class);
            }
        }

        if (this.root == null) {
            this.root = this.factory.createEbuCoreMainType();
        }

        if (this.root.getCoreMetadata() == null) {
            this.root.setCoreMetadata(this.factory.createCoreMetadataType());
        }
    }
    @Override
    public void updateSchemaData() throws IOException
    {
        super.updateSchemaData();

        this.root.setVersion(EBUCoreVersionInfo.VERSION);
        this.root.setSchema(EBUCoreVersionInfo.SCHEMA_URI);
    }
    @Override
    public void updateSoftwareData(CoreConfiguration.ProjectProperties properties) throws IOException
    {
        super.updateSoftwareData(properties);

        //this is the creator role we're looking for
        EntityType.Role creatorEntityRole = this.factory.createEntityTypeRole();
        creatorEntityRole.setTypeLabel(CREATOR_ROLE_SOFTWARE_LABEL);

        EntityType creatorEntity = null;
        //if we're the first, set us as creator
        if (this.root.getCoreMetadata().getCreator().isEmpty()) {
            this.root.getCoreMetadata().getCreator().add(creatorEntity = this.factory.createEntityType());
            creatorEntity.getRole().add(creatorEntityRole);
        }
        //otherwise we're a contributor
        else {
            List<EntityType> creators = this.root.getCoreMetadata().getCreator();
            //let's search if the creator with the correct role already exists before we add it again
            for (EntityType c : creators) {
                if (!c.getRole().isEmpty() && c.getRole().get(0).getTypeLabel().equals(creatorEntityRole.getTypeLabel())) {
                    creatorEntity = c;
                    break;
                }
            }
            if (creatorEntity==null) {
                this.root.getCoreMetadata().getCreator().add(creatorEntity = this.factory.createEntityType());
                creatorEntity.getRole().add(creatorEntityRole);
            }
        }

        //this will return the maven package of the current module writing this metadata -> very useful
        String artifactId = properties.getProperty(CoreConfiguration.ProjectProperties.Property.MAVEN_PROJECT_ARTIFACT_ID_KEY);
        String version = properties.getProperty(CoreConfiguration.ProjectProperties.Property.MAVEN_PROJECT_VERSION_KEY);
        if (!StringUtils.isEmpty(artifactId) && !StringUtils.isEmpty(version)) {
            creatorEntity.setEntityId(artifactId+"-"+version);
        }
        //that's not good, let's crash
        else {
            throw new IOException("Encountered an empty mvn artifactId as well as an empty version; that shouldn't happen; "+this.baseMetadataFile);
        }
    }
    @Override
    public void updateFileData() throws IOException
    {
        super.updateFileData();
    }
    @Override
    public void updateCreator(Person creator) throws IOException
    {
        super.updateCreator(creator);

        //makes sense to use a relative url, right? (is more update-proof?)
        if (true) throw new RuntimeException("We disabled this next line when refactoring the ontologies out of blocks-core, please fix this before using it");
        //String creatorId = RdfTools.createRelativeResourceId(Classes.Person, "" + creator.getId()).toString();
        String creatorId = "";

        EntityType creatorEntity = null;
        //if we're the first, set us as creator
        if (this.root.getCoreMetadata().getCreator().isEmpty()) {
            this.root.getCoreMetadata().getCreator().add(creatorEntity = this.factory.createEntityType());
        }
        //otherwise we're a contributor
        else {

            //before switching to contributors, let's first check if the current creator is not the dummy software creator
            for (EntityType c : this.root.getCoreMetadata().getCreator()) {
                if (c.getRole().isEmpty() || !c.getRole().get(0).getTypeLabel().equals(CREATOR_ROLE_SOFTWARE_LABEL)) {
                    creatorEntity = c;
                    break;
                }
            }
            //this means there's no creator after all
            if (creatorEntity==null) {
                this.root.getCoreMetadata().getCreator().add(creatorEntity = this.factory.createEntityType());
            }
            else {
                //start off by resetting it to null cause we'll check that later on
                creatorEntity = null;

                List<EntityType> contributors = this.root.getCoreMetadata().getContributor();
                //let's search if this contributor already exists before we add it again
                for (EntityType c : contributors) {
                    if (c.getEntityId().equals(creatorId)) {
                        creatorEntity = c;
                        break;
                    }
                }
            }
            if (creatorEntity==null) {
                this.root.getCoreMetadata().getContributor().add(creatorEntity = this.factory.createEntityType());
            }
        }

        //all this will possibly overwrite some values, but that's ok cause we need the latest version
        creatorEntity.setEntityId(creatorId);

        //we'll use the first contact details to store personal information, if there's more, then it didn't come from us and we leave it alone
        ContactDetailsType contact = creatorEntity.getContactDetails().isEmpty() ? null : creatorEntity.getContactDetails().get(0);
        if (contact == null) {
            creatorEntity.getContactDetails().add(contact = this.factory.createContactDetailsType());
        }

        //will only copy in the new data
        this.updateWith(creator, contact);
    }
    @Override
    public void updateTimestamps() throws IOException
    {
        super.updateTimestamps();

        XMLGregorianCalendar now = this.getCalendar();

        this.root.setDateLastModified(now);
        this.root.setTimeLastModified(now);

        DateType dateEntity = null;
        if (this.root.getCoreMetadata().getDate().isEmpty()) {
            this.root.getCoreMetadata().getDate().add(dateEntity = this.factory.createDateType());
        }
        else {
            dateEntity = this.root.getCoreMetadata().getDate().get(0);
        }

        DateType.Created created = dateEntity.getCreated();
        if (created==null) {
            dateEntity.setCreated(created = this.factory.createDateTypeCreated());
            created.setStartDate(now);
            created.setStartTime(now);
        }

        DateType.Modified modified = dateEntity.getModified();
        if (modified==null) {
            dateEntity.setModified(modified = this.factory.createDateTypeModified());
        }
        modified.setStartDate(now);
        modified.setStartTime(now);
    }
    @Override
    public void write() throws IOException
    {
        super.write();

        //write the metadata to disk (overwriting the possibly existing metadata; that's ok since we read it in first)
        try (Writer writer = new BufferedWriter(new OutputStreamWriter(this.fileSystem.create(this.baseMetadataFile, EnumSet.of(CreateFlag.CREATE, CreateFlag.OVERWRITE), Options.CreateOpts.createParent())))) {
            JAXB.marshal(root, writer);
        }
    }
    @Override
    public void close() throws IOException
    {
        super.close();

        this.root = null;
    }

    //-----PROTECTED METHODS-----
    @Override
    protected String getXsdResourcePath()
    {
        return EBUCoreVersionInfo.SCHEMA_RESOURCE_PATH_GZ;
    }

    //-----PRIVATE METHODS-----
    private ElementType createElementType(String value)
    {
        return this.createElementType(value, null);
    }
    private ElementType createElementType(String value, Locale lang)
    {
        ElementType retVal = this.factory.createElementType();
        retVal.setValue(value);
        if (lang!=null) {
            // See http://dublincore.org/documents/2003/02/04/dces/#iso639
            // Actually both ISO2 and ISO3 are recommended, but iso639-2 has more languages
            // -> http://www.loc.gov/standards/iso639-2/php/code_list.php
            String langStr = lang.getISO3Language();
            if (!StringUtils.isEmpty(langStr)) {
                retVal.setLang(langStr);
            }
        }

        return retVal;
    }
    private XMLGregorianCalendar getCalendar() throws IOException
    {
        XMLGregorianCalendar retVal = null;

        try {
            retVal = DatatypeFactory.newInstance().newXMLGregorianCalendar(new GregorianCalendar());
        }
        catch (DatatypeConfigurationException e) {
            throw new IOException("Error while creating instance of XMLGregorianCalendar", e);
        }

        return retVal;
    }
    private void updateWith(Person person, ContactDetailsType contact) throws IOException
    {
        if (contact.getUsername().isEmpty()) {
            contact.getUsername().add(this.createElementType(person.getSubject().getPrincipal()));
        }
        else {
            contact.getUsername().get(0).setValue(person.getSubject().getPrincipal());
        }

        if (contact.getGivenName()==null) {
            contact.setGivenName(this.createElementType(person.getFirstName()));
        }
        else {
            contact.getGivenName().setValue(person.getFirstName());
        }

        if (contact.getFamilyName()==null) {
            contact.setFamilyName(this.createElementType(person.getLastName()));
        }
        else {
            contact.getFamilyName().setValue(person.getLastName());
        }

        DetailsType contactDetails = null;
        if (contact.getDetails().isEmpty()) {
            contact.getDetails().add(contactDetails = this.factory.createDetailsType());
        }
        else {
            contactDetails = contact.getDetails().get(0);
        }
        this.addEmailIfNotExists(contactDetails, person.getEmail());

        //save this update stamp
        contact.setLastUpdate(this.getCalendar());
    }
    private void addEmailIfNotExists(DetailsType contactDetails, String email)
    {
        boolean found = false;
        for (String s : contactDetails.getEmailAddress()) {
            if (s.equals(email)) {
                found = true;
                break;
            }
        }
        if (!found) {
            contactDetails.getEmailAddress().add(email);
        }
    }
}
