package com.beligum.blocks.core.models.storables;

import com.beligum.blocks.core.config.DatabaseFieldNames;
import com.beligum.blocks.core.identifiers.ElementID;
import com.beligum.blocks.core.models.AbstractElement;
import com.beligum.blocks.core.models.ifaces.StorableElement;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by bas on 01.10.14.
 * Class representing the basic layout-element in a html-page
 */
public class Row extends AbstractElement implements StorableElement
{
    /**the version of the application this row is supposed to interact with*/
    private String applicationVersion;
    /**the creator of this row*/
    private String creator;
    /**hte name (id) of the page-class of the page this row belongs to */
    private String pageClassName;


    /**
     * Constructor
     * @param id the id to this row (is of the form "[site]/[pageName]#[rowId]")
     * @param content the content of this row
     * @param isFinal boolean whether or not the content of this element can be changed by the client
     */
    public Row(ElementID id, String content, String pageClassName, boolean isFinal)
    {
        super(id, content, isFinal);
        this.pageClassName = pageClassName;
        //TODO BAS: this version should be fetched from pom.xml and added to the row.java as a field
        this.applicationVersion = "test";
        //TODO: logged in user should be added here
        this.creator = "me";
    }

    /**
     * @param id the id to this row (is of the form "[site]/[pageName]#[rowId]")
     * @param content the content of this row
     * @param pageClassName the name of the page-class this row belongs to
     * @param isFinal boolean whether or not the content of this element can be changed by the client
     * @param applicationVersion the version of the application this row was saved under
     * @param creator the creator of this row
     */
    public Row(ElementID id, String content, String pageClassName, boolean isFinal, String applicationVersion, String creator){
        super(id, content, false);
        this.pageClassName = pageClassName;
        this.applicationVersion = applicationVersion;
        this.creator = creator;
    }


    //_______________IMPLEMENTATION OF STORABLE_ELEMENT____________________//
    @Override
    public long getVersion()
    {
        return this.getId().getVersion();
    }
    @Override
    public ElementID getId()
    {
        return (ElementID) super.getId();
    }
    @Override
    public String getUnversionedId(){
        return this.getId().getUnversionedId();
    }
    @Override
    public String getVersionedId(){
        return this.getId().getVersionedId();
    }
    @Override
    public Map<String, String> toHash(){
        Map<String, String> hash = new HashMap<>();
        hash.put(DatabaseFieldNames.CONTENT, this.getContent());
        hash.put(DatabaseFieldNames.APP_VERSION, this.applicationVersion);
        hash.put(DatabaseFieldNames.CREATOR, this.creator);
        hash.put(DatabaseFieldNames.ELEMENT_CLASS_TYPE, this.getClass().getSimpleName());
        hash.put(DatabaseFieldNames.PAGE_CLASS, this.pageClassName);
        return hash;
    }
    @Override
    public String getTemplateVariableName()
    {
        return this.getId().toURI().getFragment();
    }
    @Override
    public String getApplicationVersion()
    {
        return this.applicationVersion;
    }
    @Override
    public String getCreator()
    {
        return this.creator;
    }
    @Override
    public String getPageClassName()
    {
        return this.pageClassName;
    }
    @Override
    public String getHtmlId()
    {
        return this.getId().getElementIdFromFragment();
    }


    //__________IMPLEMENTATION OF ABSTRACT METHODS OF ABSTRACTELEMENT________//
    @Override
    public String getDBSetName()
    {
        return DatabaseFieldNames.ROW_SET_NAME;
    }



    //___________OVERRIDE OF OBJECT_____________//

    /**
     * Two rows are equal when their content, meta-data (page-class, creator and application-version), site-domain and unversioned element-id (everything after the '#') are equal
     * (thus equal through object-state, not object-address).
     * @param obj
     * @return true if two rows are equal, false otherwise
     */
    @Override
    public boolean equals(Object obj)
    {
        if(obj instanceof Row) {
            if(obj == this){
                return true;
            }
            else {
                Row rowObj = (Row) obj;
                EqualsBuilder significantFieldsSet = new EqualsBuilder();
                significantFieldsSet = significantFieldsSet.append(content, rowObj.content)
                                                           .append(this.getHtmlId(), rowObj.getHtmlId())
                                                           .append(this.pageClassName, rowObj.pageClassName)
                                                           .append(this.getId().getAuthority(), rowObj.getId().getAuthority())
                                                           .append(this.creator, rowObj.creator)
                                                           .append(this.applicationVersion, rowObj.applicationVersion);
                return significantFieldsSet.isEquals();
            }
        }
        else{
            return false;
        }
    }

    /**
     * Two rows have the same hashCode when their content, meta-data (page-class, creator and application-version), site-domain and unversioned element-id (everything after the '#') are equal
     * (thus equal through object-state, not object-address)
     * @return
     */
    @Override
    public int hashCode()
    {
        //7 and 31 are two randomly chosen prime numbers, needed for building hashcodes, ideally, these are different for each class
        HashCodeBuilder significantFieldsSet = new HashCodeBuilder(7, 31);
        significantFieldsSet = significantFieldsSet.append(content)
                                                   .append(this.getHtmlId())
                                                   .append(this.pageClassName)
                                                   .append(this.getId().getAuthority())
                                                   .append(this.creator)
                                                   .append(this.applicationVersion);
        return significantFieldsSet.toHashCode();
    }


}
