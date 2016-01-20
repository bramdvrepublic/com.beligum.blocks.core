package com.beligum.blocks.fs;

import com.beligum.base.auth.models.Person;
import com.beligum.blocks.fs.ifaces.PathInfo;
import com.beligum.blocks.schema.ebucore.v1_6.jaxb.*;
import com.beligum.blocks.utils.RdfTools;
import org.apache.commons.lang3.StringUtils;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.joda.time.DateTime;

import javax.xml.bind.JAXB;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.io.*;
import java.lang.String;
import java.util.List;
import java.util.Locale;

/**
 * Created by bram on 1/20/16.
 */
public class EBUCoreHdfsMetadataWriter extends AbstractHdfsMetadataWriter
{
    //-----CONSTANTS-----
    private static final String EBUCORE_XSD_RESOURCE_PATH = "/com/beligum/blocks/schema/ebucore/v1_6/ebucore.xsd";

    //-----VARIABLES-----
    //valid during an entire session (after a successful init())
    private ObjectFactory factory;

    //valid during an open/write session, nulled after close()
    private EbuCoreMainType root;

    //-----CONSTRUCTORS-----
    public EBUCoreHdfsMetadataWriter() throws IOException
    {
        super();
    }

    //-----PUBLIC METHODS-----
    @Override
    public void init(FileSystem fileSystem) throws IOException
    {
        super.init(fileSystem);

        this.factory = new ObjectFactory();
    }
    @Override
    public void open(PathInfo<Path> pathInfo) throws IOException
    {
        super.open(pathInfo);

        //read in the existing metadata if it exists, or create a new instance if it doesn't
        if (fileSystem.exists(this.baseMetadataFile)) {
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
    public void updateCreator(Person creator) throws IOException
    {
        super.updateCreator(creator);

        String creatorId = RdfTools.createLocalResourceId(creator.getResourceUriClassName(), "" + creator.getId()).toString();

        EntityType creatorEntity = null;
        //if we're the first, set us as creator
        if (this.root.getCoreMetadata().getCreator().isEmpty()) {
            this.root.getCoreMetadata().getCreator().add(creatorEntity = this.factory.createEntityType());
        }
        //otherwise we're a contributor
        else {
            List<EntityType> contributors = this.root.getCoreMetadata().getContributor();
            //let's search if this contributor already exists before we add it again
            for (EntityType c : contributors) {
                if (c.getEntityId().equals(creatorId)) {
                    creatorEntity = c;
                    break;
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

        DateType dateEntity = null;
        if (this.root.getCoreMetadata().getDate().isEmpty()) {
            this.root.getCoreMetadata().getDate().add(dateEntity = this.factory.createDateType());
        }
        else {
            this.root.getCoreMetadata().getDate().get(0);
        }

        XMLGregorianCalendar now = this.getCalendar();

        DateType.Created created = dateEntity.getCreated();
        if (created==null) {
            dateEntity.setCreated(created = this.factory.createDateTypeCreated());
        }


        c  reated.setStartDate(now);



    }
    @Override
    public void write() throws IOException
    {
        super.write();

        //write the metadata to disk (overwriting the possibly existing metadata; that's ok since we read it in first)
        try (Writer writer = new BufferedWriter(new OutputStreamWriter(this.fileSystem.create(this.baseMetadataFile, true)))) {
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
        return EBUCORE_XSD_RESOURCE_PATH;
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
        return this.getCalendar(DateTime.now());
    }
    private XMLGregorianCalendar getCalendar(DateTime jodaTime) throws IOException
    {
        XMLGregorianCalendar retVal = null;

        if (jodaTime!=null) {
            try {
                retVal = DatatypeFactory.newInstance().newXMLGregorianCalendar(jodaTime.toGregorianCalendar());
            }
            catch (DatatypeConfigurationException e) {
                throw new IOException("Error while creating instance of XMLGregorianCalendar", e);
            }
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
