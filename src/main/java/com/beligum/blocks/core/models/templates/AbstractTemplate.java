package com.beligum.blocks.core.models.templates;

import com.beligum.blocks.core.config.DatabaseConstants;
import com.beligum.blocks.core.identifiers.RedisID;
import com.beligum.blocks.core.internationalization.Languages;
import com.beligum.blocks.core.models.IdentifiableObject;
import com.beligum.blocks.core.models.ifaces.Storable;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by bas on 05.11.14.
 */
public abstract class AbstractTemplate extends IdentifiableObject implements Storable, Comparable<AbstractTemplate>
{
    //TODO BAS!: internationalization should be added to a template (probably a map of languages on template-strings)
    /**string representing the html-template of this element, once the template has been set, it cannot be changed*/
    protected Map<String, String> templates;
    /**the version of the application this row is supposed to interact with*/
    protected String applicationVersion;
    /**the creator of this row*/
    protected String creator;

    /**
     * Constructor taking a unique id.
     * @param id id for this template
     * @param templates the map of templates (language -> template) which represent the content of this template
     */
    protected AbstractTemplate(RedisID id, Map<String, String> templates)
    {
        super(id);
        this.templates = templates;
        //TODO: this version should be fetched from pom.xml
        this.applicationVersion = "test";
        //TODO: logged in user should be added here
        this.creator = "me";
    }

    /**
     * Constructor for template with one language and a html-template in that language. (Other language-templates could be added later if wanted.)
     * @param id id for this template
     * @param language the language of this template
     * @param template the html-template of this template
     */
    protected AbstractTemplate(RedisID id, String language, String template){
        this(id, null);
        this.templates = new HashMap<>();
        language = Languages.getStandardizedLanguage(language);
        this.templates.put(language, template);
    }

    /**
     *
     * @return the template of this viewable
     */
    public Map<String, String> getTemplates()
    {
        return templates;
    }

    /**
     *
     * @return the template in the specified language, or null otherwise
     * @throws NullPointerException if language is null
     */
    public String getTemplate(String language){
        return templates.get(language);
    }

    /**
     *
     * @return the template in the language specified by this template's id
     */
    public String getTemplate(){
        return templates.get(this.getLanguage());
    }

    /**
     *
     * @return the language stored inside this template's id
     */
    public String getLanguage(){
        return this.getId().getLanguage();
    }

    /**
     *
     * @return the language stored in the id of this template
     */

    abstract public String getName();

    //________________IMPLEMENTATION OF STORABLE_____________
    /**
     * Override of the getId-method of IdentifiableObject. Here a RedisID is returned, which has more functionalities.
     * @return the id of this storable
     */
    @Override
    public RedisID getId()
    {
        return (RedisID) super.getId();
    }
    /**
     * @return the version of this storable, which is the time it was created in milliseconds
     */
    @Override
    public long getVersion()
    {
        return this.getId().getVersion();
    }
    /**
     * @return the id of this storable with it's version attached ("[storableId]:[version]")
     */
    @Override
    public String getVersionedId(){
        return this.getId().getVersionedId();
    }
    /**
     * @return the id of this storable without a version attached ("[storableId]")
     */
    @Override
    public String getUnversionedId(){
        return this.getId().getUnversionedId();
    }
    /**
     * @return the id of the hash representing this storable ("[storableId]:[version]:hash")
     */
    @Override
    public String getHashId(){
        return this.getId().getHashId();
    }
    /**
     * @return the version of the application this storable is supposed ot interact with
     */
    @Override
    public String getApplicationVersion()
    {
        return this.applicationVersion;
    }
    /**
     * @return the creator of this storable
     */
    @Override
    public String getCreator()
    {
        return this.creator;
    }
    /**
     * Gives a hash-representation of this storable to save to the db. This method decides what information is stored in db, and what is not.
     *
     * @return a map representing the key-value structure of this element to be saved to db
     */
    @Override
    public Map<String, String> toHash(){
        Map<String, String> hash = new HashMap<>();
        hash.put(DatabaseConstants.TEMPLATE, this.getTemplates());
        hash.put(DatabaseConstants.APP_VERSION, this.applicationVersion);
        hash.put(DatabaseConstants.CREATOR, this.creator);
        return hash;
    }

    //__________IMPLEMENTATION OF COMPARABLE_______________//

    @Override
    /**
     * Comparison of templates is done by using the string-comparison of their names.
     */
    public int compareTo(AbstractTemplate abstractTemplate)
    {
        return this.getName().compareToIgnoreCase(abstractTemplate.getName());
    }

    //________________OVERRIDE OF OBJECT_______________//

    /**
     * Two templates have the same hashCode when their template-content, url and meta-data are equal.
     * (thus equal through object-state, not object-address)
     * @return
     */
    @Override
    public int hashCode()
    {
        //7 and 31 are two randomly chosen prime numbers, needed for building hashcodes, ideally, these are different for each class
        HashCodeBuilder significantFieldsSet = new HashCodeBuilder(7, 31);
        significantFieldsSet = significantFieldsSet.append(templates)
                        .append(this.getUnversionedId())
                        .append(this.creator)
                        .append(this.applicationVersion);
        return significantFieldsSet.toHashCode();
    }

    /**
     * Two templates are equal when their template-content, url and meta-data are equal
     * (thus equal through object-state, not object-address).
     * @param obj
     * @return true if two templates are equal, false otherwise
     */
    @Override
    public boolean equals(Object obj)
    {
        if(obj instanceof AbstractTemplate) {
            if(obj == this){
                return true;
            }
            else {
                AbstractTemplate abstractTemplateObj = (AbstractTemplate) obj;
                EqualsBuilder significantFieldsSet = new EqualsBuilder();
                significantFieldsSet = significantFieldsSet.append(templates, abstractTemplateObj.templates)
                                .append(this.getUnversionedId(), abstractTemplateObj.getUnversionedId())
                                .append(this.creator, abstractTemplateObj.creator)
                                .append(this.applicationVersion, abstractTemplateObj.applicationVersion);
                return significantFieldsSet.isEquals();
            }
        }
        else{
            return false;
        }
    }

    @Override
    public String toString()
    {
        return this.getTemplate();
    }
}
