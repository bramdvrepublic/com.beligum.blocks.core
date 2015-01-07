package com.beligum.blocks.core.models.templates;

import com.beligum.blocks.core.config.BlocksConfig;
import com.beligum.blocks.core.config.DatabaseConstants;
import com.beligum.blocks.core.dbs.Redis;
import com.beligum.blocks.core.exceptions.DeserializationException;
import com.beligum.blocks.core.exceptions.IDException;
import com.beligum.blocks.core.exceptions.SerializationException;
import com.beligum.blocks.core.identifiers.RedisID;
import com.beligum.blocks.core.internationalization.Languages;
import com.beligum.blocks.core.models.IdentifiableObject;
import com.beligum.blocks.core.models.ifaces.Storable;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.util.*;

/**
 * Created by bas on 05.11.14.
 */
public abstract class AbstractTemplate extends IdentifiableObject implements Storable, Comparable<AbstractTemplate>
{
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
     * Constructor for template with one language: the one precent in the id. (Other language-templates could be added later if wanted.)
     * @param id id for this template
     * @param template the html-template of this template
     * @throws NullPointerException if the template is null
     */
    protected AbstractTemplate(RedisID id, String template){
        this(id, (Map) null);
        this.templates = new HashMap<>();
        if(template == null){
            throw new NullPointerException("Null-template found while constructing a template with id '" + id + "'.");
        }
        this.templates.put(id.getLanguage(), template);
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
     * @return the template in the specified language, or if this language is not present, the first preferred language is returned
     * @throws NullPointerException if language is null
     */
    public String getTemplate(String language){
        String template = templates.get(language);
        if(template == null){
            template = this.getTemplate();
        }
        return template;
    }

    /**
     * Looks for the best fitting template to be returned. First this method looks if a template in the language carried inside this template's id is present to return.
     * If not it looks if one of the site's preferred languages is present (in order of appearance).
     * If still no template is found, it returns a random template present. (Their is always at least one template present, since it is added while constructing the template and that cannot be changed later.)
     * @return the template in the language specified by this template's id, or if this language is not present, the first preferred language is returned
     */
    public String getTemplate(){
        String template = this.templates.get(this.getLanguage());
        if(template == null){
            String[] preferredLanguages = BlocksConfig.getLanguages();
            int i = 0;
            while(template == null && i < preferredLanguages.length){
                template = this.templates.get(preferredLanguages[i]);
                i++;
            }
            if(template == null){
                Collection<String> templates = this.templates.values();
                Iterator<String> it = templates.iterator();
                if(!it.hasNext()){
                    throw new RuntimeException("Could not find ANY html-templates inside the " + AbstractTemplate.class.getSimpleName() + " '" + this.getId() + "'. This should NEVER happen: did someone add a setter-method for templates in this class? That is not expected.");
                }
                template = it.next();
            }
        }
        return template;
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
    public Map<String, String> toHash() throws SerializationException{
        try {
            Map<String, String> hash = new HashMap<>();
            for (String language : this.templates.keySet()) {
                RedisID languagedId = new RedisID(this.getId(), language);
                hash.put(language, languagedId.toString());
            }
            hash.put(DatabaseConstants.APP_VERSION, this.applicationVersion);
            hash.put(DatabaseConstants.CREATOR, this.creator);
            return hash;
        }catch(Exception e){
            throw new SerializationException("Could not construct a proper hash from " + AbstractTemplate.class.getSimpleName() + ": " + this, e);
        }
    }

    /**
     * Method fetching all templates in different languages, found as keys in the specified hash.
     * @param hash
     * @return
     * @throws IDException if a bad id is found in the specified hash
     */
    static protected Map<String, String> fetchLanguageTemplatesFromHash(Map<String, String> hash) throws DeserializationException
    {
        try {
            Set<String> keys = hash.keySet();
            Set<String> permittedLanguages = Languages.getPermittedLanguageCodes();
            Map<String, String> templates = new HashMap<>();
            for (String key : keys) {
                if (permittedLanguages.contains(key)) {
                    templates.put(key, Redis.getInstance().fetchStringForId(new RedisID(hash.get(key))));
                }
            }
            if(templates.isEmpty()){
                throw new DeserializationException("No html-template found for any language in hash: \n \n " + hash + "\n \n");
            }
            return templates;
        }catch (Exception e){
            throw new DeserializationException("Could not fetch a language-templates from db.", e);
        }
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
