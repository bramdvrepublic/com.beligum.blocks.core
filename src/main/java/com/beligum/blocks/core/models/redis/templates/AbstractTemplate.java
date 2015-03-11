package com.beligum.blocks.core.models.redis.templates;

import com.beligum.blocks.core.config.BlocksConfig;
import com.beligum.blocks.core.config.DatabaseConstants;
import com.beligum.blocks.core.dbs.Database;
import com.beligum.blocks.core.dbs.RedisDatabase;
import com.beligum.blocks.core.exceptions.CacheException;
import com.beligum.blocks.core.exceptions.DeserializationException;
import com.beligum.blocks.core.exceptions.IDException;
import com.beligum.blocks.core.exceptions.SerializationException;
import com.beligum.blocks.core.identifiers.BlocksID;
import com.beligum.blocks.core.internationalization.Languages;
import com.beligum.blocks.core.models.redis.Storable;
import com.beligum.blocks.core.parsers.TemplateParser;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.jsoup.nodes.Element;

import java.util.*;

/**
 * Created by bas on 05.11.14.
 */
public abstract class AbstractTemplate extends Storable implements Comparable<AbstractTemplate>
{
    /**string representing the html-template of this element, once the template has been set, it cannot be changed*/
    protected Map<BlocksID, String> templates;
    /**the (css-)linked files this abstract template needs*/
    protected Set<String> links = new HashSet<>();
    protected List<String> linksInOrder = new ArrayList<>();
    /**the scripts this abstract template needs*/
    protected Set<String> scripts = new HashSet<>();
    protected List<String> scriptsInOrder = new ArrayList<>();

    protected Map<String, EntityTemplate> properties = new HashMap<>();


    /**
     * Constructor taking a unique id.
     * @param id id for this template
     * @param templates the map of templates (language -> template) which represent the content of this template
     * @param links the (css-)linked files this abstract template needs
     * @param scripts the (javascript-)scripts this abstract template needs
     */
    protected AbstractTemplate(BlocksID id, Map<BlocksID, String> templates, List<String> links, List<String> scripts)
    {
        super(id);
        this.templates = templates;
        if(links != null) {
            for (String link : links) {
                boolean added = this.links.add(link);
                //if this link wasn't present yet, add it to the list
                if (added) {
                    this.linksInOrder.add(link);
                }
            }
        }
        if(scripts != null){
            for (String script : scripts) {
                boolean added = this.scripts.add(script);
                //if this script wasn't present yet, add it to the list
                if (added) {
                    this.scriptsInOrder.add(script);
                }
            }
        }
    }

    /**
     * Constructor for template with one language: the one present in the id. (Other language-templates could be added later if wanted.)
     * @param id id for this template
     * @param template the html-template of this template
     * @param links the (css-)linked files this template needs
     * @param scripts the (javascript-)scripts this template needs
     * @throws NullPointerException if the template is null
     */
    protected AbstractTemplate(BlocksID id, String template, List<String> links, List<String> scripts){
        this(id, (Map) null, links, scripts);
        this.templates = new HashMap<>();
        if(template == null){
            throw new NullPointerException("Null-template found while constructing a template with id '" + id + "'.");
        }
        this.templates.put(id, template);
    }

    public List<String> getScripts() throws CacheException
    {
        return scriptsInOrder;
    }

    public List<String> getLinks() throws CacheException
    {
        return linksInOrder;
    }

    /**
     *
     * @return the template of this viewable
     */
    public Map<BlocksID, String> getTemplates()
    {
        return templates;
    }

    /**
     *
     * @return all languages this template possesses
     */
    public Set<String> getLanguages(){
        Set<String> languages = new HashSet<>();
        for(BlocksID languageId : this.templates.keySet()){
            languages.add(languageId.getLanguage());
        }
        return languages;
    }

    /**
     *
     * @return the template in the specified language, or if this language is not present, null is returned
     * @throws NullPointerException if language is null
     */
    public String getTemplate(String language){
        Map<String, String> templates = new HashMap<>();
        for(BlocksID languageID : this.templates.keySet()){
            templates.put(languageID.getLanguage(), this.templates.get(languageID));
        }
        String template = templates.get(language);
        return template;
    }

    /**
     * Looks for the best fitting template to be returned. First this method looks if a template in the language carried inside this template's id is present to return.
     * If not it looks if one of the site's preferred languages is present (in order of appearance).
     * If still no template is found, it returns a random template present. (Their is always at least one template present, since it is added while constructing the template and that cannot be changed later.)
     * @return the template in the language specified by this template's id, or if this language is not present, the first preferred language is returned
     */
    public String getTemplate(){
        String template = this.getTemplate(this.getLanguage());
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
     * Tries to add a new html-template for a certain language. If this html-template has a language which is already present in db, but differs from the html-template already present, the most recent one will be kept.
     * @param languageID a redis-id holding a language
     * @param html a html-string
     * @return false if the language-code is not correct, if the html-string is empty or if the same template is already present in the same language
     * true otherwise, which means the template has been added
     */
    public boolean add(BlocksID languageID, String html){
        if(StringUtils.isEmpty(html)){
            return false;
        }
        if(!this.getLanguages().contains(languageID.getLanguage())){
            templates.put(languageID, html);
            return true;
        }
        else{
            String oldHtml = this.getTemplate(languageID.getLanguage());
            if(html.equals(oldHtml)){
                return false;
            }
            else{
                boolean added = false;
                for(BlocksID languageIdIntern : templates.keySet()){
                    if(languageIdIntern.getLanguage().equals(languageID.getLanguage()) && languageID.getVersion() >= languageIdIntern.getVersion()){
                        templates.remove(languageIdIntern);
                        templates.put(languageID, html);
                        added = true;
                    }
                }
                return added;
            }
        }
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
     * @return the name this template
     */
    abstract public String getName();


    /**
     * Gives a hash-representation of this storable to save to the db. This method decides what information is stored in db, and what is not.
     *
     * @return a map representing the key-value structure of this element to be saved to db
     */
    @Override
    public Map<String, String> toHash() throws SerializationException{
        try {
            Map<String, String> hash = super.toHash();
            /*
             * The id should not be saved as a seperate field in db. It is the key of this template, so present in db anyway.
             * The templates are separately saved per language, not under the key "templates".
             * Links and scripts field should only be saved when present.
             */
            hash.remove(EntityTemplate.class.getField("id").getName());
            hash.remove(EntityTemplate.class.getField("templates").getName());
            hash.remove(EntityTemplate.class.getField("links").getName());
            hash.remove(EntityTemplate.class.getField("linksInOrder").getName());
            hash.remove(EntityTemplate.class.getField("scripts").getName());
            hash.remove(EntityTemplate.class.getField("scriptsInOrder").getName());
            for (BlocksID languageId : this.templates.keySet()) {
                hash.put(languageId.getLanguage(), languageId.toString());
            }
            String links = "";
            for(String link : this.linksInOrder){
                links += link;
            }
            if(!StringUtils.isEmpty(links)) {
                hash.put(DatabaseConstants.LINKS, links);
            }
            String scripts = "";
            for(String script : this.scriptsInOrder){
                scripts += script;
            }
            if(!StringUtils.isEmpty(scripts)) {
                hash.put(DatabaseConstants.SCRIPTS, scripts);
            }
            return hash;
        }catch(Exception e){
            throw new SerializationException("Could not construct a proper hash from " + AbstractTemplate.class.getSimpleName() + ": " + this, e);
        }
    }

    /**
     * Method fetching all templates in different languages, found as keys in the specified hash. The used keys will be deleted from the hash.
     * @param hash
     * @throws IDException if a bad id is found in the specified hash
     */
    static protected Map<BlocksID, String> fetchLanguageTemplatesFromHash(Map<String, String> hash) throws DeserializationException
    {
        try {
            Set<String> keys = hash.keySet();
            Set<String> permittedLanguages = Languages.getPermittedLanguageCodes();
            Map<BlocksID, String> templates = new HashMap<>();
            Set<String> keysToBeRemoved = new HashSet<>();
            for (String key : keys) {
                if (permittedLanguages.contains(key)) {
                    BlocksID languageId = new BlocksID(hash.get(key));
                    Database redis = RedisDatabase.getInstance();
                    if(redis instanceof RedisDatabase) {
                        templates.put(languageId,((RedisDatabase) redis).fetchStringForId(languageId));
                    }
                    else{
                        throw new DeserializationException("Could not fetch language string from database. Unknown database type found: " + redis.getClass().getName());
                    }
                    keysToBeRemoved.add(key);
                }
            }
            for(String key: keysToBeRemoved){
                hash.remove(key);
            }
            if(templates.isEmpty()){
                throw new DeserializationException("No html-template found for any language in hash: \n \n " + hash + "\n \n");
            }
            return templates;
        }catch (Exception e){
            throw new DeserializationException("Could not fetch a language-templates from db.", e);
        }
    }

    static private List<String> fetchTagListFromHash(String dataBaseConstant, Map<String, String> hash){
        List<String> tags = new ArrayList<>();
        String tagsList = hash.get(dataBaseConstant);
        if(!StringUtils.isEmpty(tagsList)) {
            Element tagsRoot = TemplateParser.parse(tagsList).child(0);
            tags.add(tagsRoot.outerHtml());
            for(Element tag : tagsRoot.siblingElements()){
                tags.add(tag.outerHtml());
            }
            hash.remove(dataBaseConstant);
        }
        return tags;
    }

    /**
     * Method for fetching a list of link-tags from a db-hash. The links will be removed from the hash afterwards.
     * @param hash
     */
    static protected List<String> fetchLinksFromHash(Map<String, String> hash){
        return fetchTagListFromHash(DatabaseConstants.LINKS, hash);
    }

    /**
     * Method for fetching a list of script-tags from a db-hash. The scripts will be removed from the hash afterwards.
     * @param hash
     */
    static protected List<String> fetchScriptsFromHash(Map<String, String> hash){
        return fetchTagListFromHash(DatabaseConstants.SCRIPTS, hash);
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
     * (thus equal through object-state, not object-address). Version numbers are not used to check for equality!
     */
    @Override
    public int hashCode()
    {
        //7 and 31 are two randomly chosen prime numbers, needed for building hashcodes, ideally, these are different for each class
        HashCodeBuilder significantFieldsSet = new HashCodeBuilder(7, 31);
        significantFieldsSet = significantFieldsSet.append(this.getUnversionedId())
                                                   .append(this.applicationVersion)
                                                   .append(this.deleted)
                                                   .append(this.getName());
        //all map-pairs "language -> template" must be added to the hashcode, we do this by customly specifying a string containing both
        for(BlocksID languageId : templates.keySet()){
            String language = languageId.getLanguage();
            significantFieldsSet = significantFieldsSet.append(language + "->" + templates.get(languageId));
        }
        for(String link : this.linksInOrder){
            significantFieldsSet = significantFieldsSet.append(link);
        }
        for(String script : this.scriptsInOrder){
            significantFieldsSet = significantFieldsSet.append(script);
        }
        return significantFieldsSet.toHashCode();
    }



    /**
     * Two templates are equal when their template-content, url and meta-data are equal
     * (thus equal through object-state, not object-address). Version numbers are not used to check for equality!
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
                significantFieldsSet = significantFieldsSet.append(this.getUnversionedId(), abstractTemplateObj.getUnversionedId())
                                                           .append(this.applicationVersion, abstractTemplateObj.applicationVersion)
                                                           .append(this.deleted, abstractTemplateObj.deleted)
                                                           .append(this.getName(), abstractTemplateObj.getName());
                //check if all templates in different languages are equal and that exactly the same languages are present in both objects
                significantFieldsSet = significantFieldsSet.append(templates.size(), abstractTemplateObj.templates.size());
                for(BlocksID languageId : templates.keySet()){
                    String language = languageId.getLanguage();
                    significantFieldsSet = significantFieldsSet.append(this.getTemplate(language), abstractTemplateObj.getTemplate(language));
                }
                significantFieldsSet = significantFieldsSet.append(this.linksInOrder.size(), abstractTemplateObj.linksInOrder.size());
                significantFieldsSet = significantFieldsSet.append(this.scriptsInOrder.size(), abstractTemplateObj.scriptsInOrder.size());
                for(int i = 0; significantFieldsSet.isEquals() && i<this.linksInOrder.size(); i++){
                    significantFieldsSet = significantFieldsSet.append(this.linksInOrder.get(i), abstractTemplateObj.linksInOrder.get(i));
                }
                for(int i = 0; significantFieldsSet.isEquals() && i<this.scriptsInOrder.size(); i++){
                    significantFieldsSet = significantFieldsSet.append(this.scriptsInOrder.get(i), abstractTemplateObj.scriptsInOrder.get(i));
                }
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
