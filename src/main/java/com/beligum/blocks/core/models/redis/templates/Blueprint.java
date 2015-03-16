package com.beligum.blocks.core.models.redis.templates;

import com.beligum.blocks.core.caching.PageTemplatesCache;
import com.beligum.blocks.core.config.ParserConstants;
import com.beligum.blocks.core.exceptions.DeserializationException;
import com.beligum.blocks.core.exceptions.IDException;
import com.beligum.blocks.core.identifiers.BlocksID;
import com.beligum.blocks.core.utils.Utils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.util.List;
import java.util.Map;

/**
 * Created by bas on 05.11.14.
 */
public class Blueprint extends AbstractTemplate
{
    /**the default page-template this class should be rendered in*/
    private String pageTemplateName = ParserConstants.DEFAULT_PAGE_TEMPLATE;
    /**string the name of this entity-class*/
    private String name;
    /**true if this is a class which can be created as a new page*/
    private boolean pageBlock;
    /**true if this is a class which can be added as a new block*/
    private boolean addableBlock;

    /**
     * Constructor for an blueprint with one language and a template in that language. (Other language-templates could be added later if wanted.)
     * @param name the name of this entity-class
     * @param language the language of this class
     * @param template the html-template of this class
     * @param pageTemplateName the default page-template this entity-class should be rendered in
     * @param links the (css-)linked files this template needs
     * @param scripts the (javascript-)scripts this template needs
     * @throws IDException if no new id could be rendered using the specified name
     */
    public Blueprint(String name, String language, String template, String pageTemplateName, List<String> links, List<String> scripts) throws IDException
    {
        super(BlocksID.renderNewEntityTemplateClassID(name, language), template, links, scripts);
        this.name = name;
        if(pageTemplateName != null) {
            this.pageTemplateName = pageTemplateName;
        }
        this.pageBlock = false;
        this.addableBlock = false;
    }

    /**
     * Constructor used for turning a redis-hash into an entity-template.
     * @param id
     * @param templates
     * @throws IDException
     */
    private Blueprint(BlocksID id, Map<BlocksID, String> templates, List<String> links, List<String> scripts) throws IDException
    {
        super(id, templates, links, scripts);
        //the name of this blueprint doesn't start with a "/", so we split it of the given path
        String[] splitted = id.getUrl().getPath().split("/");
        if (splitted.length > 0) {
            this.name = splitted[1];
        }
        else {
            this.name = null;
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

    public String getType(){
        return this.getName();
    }

    /**
     *
     * @return the name of the page-template this entity-class should be rendered in
     */
    public String getPageTemplateName()
    {
        return pageTemplateName;
    }
    //Setter should not be deleted, since it is used when deserializing a blueprint from db
    public void setPageTemplateName(String pageTemplateName)
    {
        this.pageTemplateName = pageTemplateName;
    }
    /**
     *
     * @return the default page-template this entity-class should be rendered in, fetched from cache
     */
    public PageTemplate getPageTemplate() throws Exception
    {
        return PageTemplatesCache.getInstance().get(pageTemplateName);
    }

    public boolean isPageBlock()
    {
        return pageBlock;
    }
    public void setPageBlock(boolean pageBlock)
    {
        this.pageBlock = pageBlock;
    }
    public boolean isAddableBlock()
    {
        return addableBlock;
    }
    public void setAddableBlock(boolean addableBlock)
    {
        this.addableBlock = addableBlock;
    }

    /**
     * The EntityTemplateClass-class can be used as a factory, to construct blueprintes from data found in a hash in the redis-db
     * @param hash a map, mapping field-names to field-values
     * @return an blueprint or throws an exception if no blueprint could be constructed from the specified hash
     * @throws DeserializationException when a bad hash is found
     */
    //is protected so that all classes in package can access this method
    protected static Blueprint createInstanceFromHash(BlocksID id, Map<String, String> hash) throws DeserializationException
    {
        try {
            if (hash == null || hash.isEmpty()) {
                throw new DeserializationException("Found empty hash");
            }
            else{
                /*
                 * Fetch all fields from the hash, removing them as they are used.
                 * Afterwards use all remaining information to be wired to the a new instance
                 */
                Map<BlocksID, String> templates = AbstractTemplate.fetchLanguageTemplatesFromHash(hash);
                List<String> links = AbstractTemplate.fetchLinksFromHash(hash);
                List<String> scripts = AbstractTemplate.fetchScriptsFromHash(hash);
                Blueprint newInstance = new Blueprint(id, templates, links, scripts);
                Utils.autowireDaoToModel(hash, newInstance);
                return newInstance;
            }
        }
        catch(Exception e){
            throw new DeserializationException("Could not construct an object of class '" + Blueprint.class.getName() + "' from specified hash.", e);
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
                                                   .append(this.pageTemplateName)
                                                   .append(this.addableBlock)
                                                   .append(this.pageBlock);
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
        if(!(obj instanceof Blueprint)) {
            return false;
        }
        else{
            boolean equals = super.equals(obj);
            if(!equals){
                return false;
            }
            else{
                Blueprint templObj = (Blueprint) obj;
                EqualsBuilder significantFieldsSet = new EqualsBuilder();
                significantFieldsSet = significantFieldsSet.append(pageTemplateName, templObj.pageTemplateName)
                                                           .append(addableBlock, templObj.addableBlock)
                                                           .append(pageBlock, templObj.pageBlock);
                return significantFieldsSet.isEquals();
            }
        }
    }
}
