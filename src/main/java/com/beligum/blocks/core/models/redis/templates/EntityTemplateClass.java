package com.beligum.blocks.core.models.redis.templates;

import com.beligum.blocks.core.caching.PageTemplateCache;
import com.beligum.blocks.core.config.BlocksConfig;
import com.beligum.blocks.core.config.CacheConstants;
import com.beligum.blocks.core.config.DatabaseConstants;
import com.beligum.blocks.core.config.ParserConstants;
import com.beligum.blocks.core.exceptions.CacheException;
import com.beligum.blocks.core.exceptions.DeserializationException;
import com.beligum.blocks.core.exceptions.IDException;
import com.beligum.blocks.core.exceptions.SerializationException;
import com.beligum.blocks.core.identifiers.RedisID;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;

/**
 * Created by bas on 05.11.14.
 */
public class EntityTemplateClass extends AbstractTemplate
{
    /**the default page-template this class should be rendered in*/
    private String pageTemplateName = ParserConstants.DEFAULT_PAGE_TEMPLATE;
    /**string the name of this entity-class*/
    private String name;

    /**
     *
     * @param name the name of this entity-class
     * @param primaryLanguage the language this entity-template-class will always fall back to if needed
     * @param templates A map relating languages to template-strings corresponding to the most outer layer of the element-tree in this entity. At least one template in the primary-language should be present.
     * @param pageTemplateName the default page-template this entity-class should be rendered in
     * @param links the (css-)linked files this template needs
     * @param scripts the (javascript-)scripts this template needs
     * @throws IDException if no new id could be rendered using the specified name and language, or if no template of that language is present in the specified map of templates
     */
    public EntityTemplateClass(String name, String primaryLanguage, Map<RedisID, String> templates, String pageTemplateName, List<String> links, List<String> scripts) throws IDException
    {
        super(RedisID.renderNewEntityTemplateClassID(name, primaryLanguage), templates, links, scripts);
        this.name = name;
        if(pageTemplateName != null) {
            this.pageTemplateName = pageTemplateName;
        }
        if(!templates.containsKey(primaryLanguage)){
            throw new IDException("No html-template in language '" + primaryLanguage + "' found between templates.");
        }
    }

    /**
     * Constructor for an entity-template-class with one language and a template in that language. (Other language-templates could be added later if wanted.)
     * @param name the name of this entity-class
     * @param language the language of this class
     * @param template the html-template of this class
     * @param pageTemplateName the default page-template this entity-class should be rendered in
     * @param links the (css-)linked files this template needs
     * @param scripts the (javascript-)scripts this template needs
     * @throws IDException if no new id could be rendered using the specified name
     */
    public EntityTemplateClass(String name, String language, String template, String pageTemplateName, List<String> links, List<String> scripts) throws IDException
    {
        super(RedisID.renderNewEntityTemplateClassID(name, language), template, links, scripts);
        this.name = name;
        if(pageTemplateName != null) {
            this.pageTemplateName = pageTemplateName;
        }
    }

    /**
     * Constructor used for turning a redis-hash into an entity-template.
     * @param id
     * @param templates
     * @param pageTemplateName
     * @throws IDException
     */
    private EntityTemplateClass(RedisID id, Map<RedisID, String> templates, String pageTemplateName, List<String> links, List<String> scripts) throws IDException
    {
        super(id, templates, links, scripts);
        //the name of this entity-template-class doesn't start with a "/", so we split it of the given path
        String[] splitted = id.getUrl().getPath().split("/");
        if (splitted.length > 0) {
            this.name = splitted[1];
        }
        else {
            this.name = null;
        }
        if(pageTemplateName != null) {
            this.pageTemplateName = pageTemplateName;
        }
        if(!this.getLanguages().contains(id.getLanguage())){
            throw new IDException("No html-template in language '" + id.getLanguage() + "' found between templates.");
        }
    }

    /**
     *
     * @return the name of this entity-class
     */
    public String getName()
    {
        return name;
    }

    /**
     *
     * @return the name of the page-template this entity-class should be rendered in
     */
    public String getPageTemplateName()
    {
        return pageTemplateName;
    }
    /**
     *
     * @return the default page-template this entity-class should be rendered in, fetched from cache
     */
    public PageTemplate getPageTemplate() throws CacheException
    {
        return PageTemplateCache.getInstance().get(pageTemplateName);
    }


    /**
     * returns the base-url for the entity-class
     * @param entityClassName the name of the entity-class (f.i. "default" for a entityClass filtered from the file "entities/default/index.html")
     */
    public static URL getBaseUrl(String entityClassName) throws MalformedURLException
    {
        return new URL(BlocksConfig.getSiteDomain() + "/" + CacheConstants.ENTITY_CLASS_ID_PREFIX + "/" + entityClassName);
    }

    /**
     * The EntityTemplateClass-class can be used as a factory, to construct entity-template-classes from data found in a hash in the redis-db
     * @param hash a map, mapping field-names to field-values
     * @return an entity-template-class or throws an exception if no entity-template-class could be constructed from the specified hash
     * @throws DeserializationException when a bad hash is found
     */
    //is protected so that all classes in package can access this method
    protected static EntityTemplateClass createInstanceFromHash(RedisID id, Map<String, String> hash) throws DeserializationException
    {
        try {
            if (hash == null || hash.isEmpty()) {
                throw new DeserializationException("Found empty hash");
            }
            else{
                Map<RedisID, String> templates = AbstractTemplate.fetchLanguageTemplatesFromHash(hash);
                List<String> links = AbstractTemplate.fetchLinksFromHash(hash);
                List<String> scripts = AbstractTemplate.fetchScriptsFromHash(hash);
                EntityTemplateClass newInstance = new EntityTemplateClass(id, templates, hash.get(DatabaseConstants.PAGE_TEMPLATE), links, scripts);
                newInstance.applicationVersion = hash.get(DatabaseConstants.APP_VERSION);
                newInstance.created_by = hash.get(DatabaseConstants.CREATOR);
                return newInstance;
            }
        }
        catch(Exception e){
            throw new DeserializationException("Could not construct an object of class '" + EntityTemplateClass.class.getName() + "' from specified hash.", e);
        }
    }






    //________________OVERRIDE OF OBJECT_______________//

    /**
     * Two templates have the same hashCode when their template-content, url and meta-data are equal.
     * (thus equal through object-state, not object-address)
     */
    @Override
    public int hashCode()
    {
        int hashCode = super.hashCode();
        HashCodeBuilder significantFieldsSet = new HashCodeBuilder(9, 17);
        significantFieldsSet = significantFieldsSet.appendSuper(hashCode)
                                                   .append(this.pageTemplateName);
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
        if(!(obj instanceof EntityTemplateClass)) {
            return false;
        }
        else{
            boolean equals = super.equals(obj);
            if(!equals){
                return false;
            }
            else{
                EntityTemplateClass templObj = (EntityTemplateClass) obj;
                EqualsBuilder significantFieldsSet = new EqualsBuilder();
                significantFieldsSet = significantFieldsSet.append(pageTemplateName, templObj.pageTemplateName);
                return significantFieldsSet.isEquals();
            }
        }
    }
}
