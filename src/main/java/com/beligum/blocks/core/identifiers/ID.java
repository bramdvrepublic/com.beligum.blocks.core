package com.beligum.blocks.core.identifiers;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.net.URI;

/**
 * Created by bas on 13.10.14.
 * Super class for identification of objects and resources. It is actually a wrapper for a URI.
 */
public class ID
{
    //TODO BAS: currentMillis since 2000, not since 1970 -> makes things shorter
    /** interal uri-representation of this id */
    protected URI idUri;

    public ID(URI id){
        this.idUri = id;
    }

    /**
     *
     * @return the URI-representation of this ID
     */
    public URI toURI()
    {
        return idUri;
    }

    @Override
    public String toString(){
        return idUri.toString();
    }


    //___________OVERRIDE OF OBJECT_____________//

    /**
     * Two ids are equal when their string-representations are equal
     * @param obj
     * @return true if content and id of two rows are equal, false otherwise
     */
    @Override
    public boolean equals(Object obj)
    {
        if(obj instanceof ID) {
            if(obj == this){
                return true;
            }
            else {
                ID idObj = (ID) obj;
                EqualsBuilder significantFieldsSet = new EqualsBuilder();
                significantFieldsSet = significantFieldsSet.append(this.toString(), idObj.toString());
                return significantFieldsSet.isEquals();
            }
        }
        else{
            return false;
        }
    }
    /**
     * Two ids have the same hashCode when their string-representations are equal
     * @return
     */
    @Override
    public int hashCode()
    {
        //13 and 41 are two randomly chosen prime numbers, needed for building hashcodes, ideally, these are different for each class
        HashCodeBuilder significantFieldsSet = new HashCodeBuilder(13, 41);
        significantFieldsSet = significantFieldsSet.append(this.toString());
        return significantFieldsSet.toHashCode();
    }
}
