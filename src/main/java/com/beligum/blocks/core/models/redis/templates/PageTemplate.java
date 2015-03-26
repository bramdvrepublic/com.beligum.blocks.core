package com.beligum.blocks.core.models.redis.templates;

import com.beligum.blocks.core.exceptions.DeserializationException;
import com.beligum.blocks.core.exceptions.IDException;
import com.beligum.blocks.core.identifiers.redis.BlocksID;
import com.beligum.blocks.core.utils.Utils;

import java.util.List;
import java.util.Map;

/**
 * Created by wouter on 20/11/14.
 */
public class PageTemplate extends AbstractTemplate
{
    private String name;

    /**
     *
     * @param name the name of this template
     * @param primaryLanguage
     * @param templates the map of templates (language -> template) which represent the content of this template  @throws IDException if no new id could be rendered using the specified name@throws IDException
     * @throws IDException if no new id could be rendered using the specified name and language, or if no template of that language is present in the specified map of templates
     */
    public PageTemplate(String name, String primaryLanguage, Map<BlocksID, String> templates, List<String> links, List<String> scripts) throws IDException
    {
        super(BlocksID.renderNewPageTemplateID(name, primaryLanguage), templates, links, scripts);
        this.name = name;
        if(!this.getLanguages().contains(this.getId().getLanguage())){
            throw new IDException("No html-template in language '" + primaryLanguage + "' found between templates.");
        }
    }

    /**
     * Constructor for template with one language: the one precent in the id. (Other language-templates could be added later if wanted.)
     * @param name the name of this template
     * @param language the language this template is written in
     * @param template the html-template of this template
     */
    public PageTemplate(String name, String language, String template, List<String> links, List<String> scripts) throws IDException
    {
        super(BlocksID.renderNewPageTemplateID(name, language), template, links, scripts);
        this.name = name;
    }

    /**
     * Constructor used for turning a redis-hash into a page-template.
     * @param id the id of this, containing language-information
     * @param templates the map of templates (language -> template) which represent the content of this template  @throws IDException if no new id could be rendered using the specified name@throws IDException
     * @throws IDException if no new id could be rendered using the specified name, or if no template of the language specified by the id is present in the map of templates
     */
    private PageTemplate(BlocksID id, Map<BlocksID, String> templates, List<String> links, List<String> scripts) throws IDException
    {
        super(id, templates, links, scripts);
        if(!this.getLanguages().contains(id.getLanguage())){
            throw new IDException("No html-template in language '" + id.getLanguage() + "' found between templates.");
        }
    }

    public String getName() {
        return name;
    }

    public boolean isTemplate() {
        return this.getName() != null && !this.getName().isEmpty();
    }

    /**
     * The PageTemplate-class can be used as a factory, to construct page-templates from data found in a hash in the redis-db
     * @param hash a map, mapping field-names to field-values
     * @return an page-template or null if no page-template could be constructed from the specified hash
     * @throws DeserializationException when a bad hash is found
     */
    //is protected so that all classes in package can access this method
    protected static PageTemplate createInstanceFromHash(BlocksID id, Map<String, String> hash) throws DeserializationException
    {
        try{
            if(hash == null || hash.isEmpty()) {
                throw new DeserializationException("Found empty hash.");
            }
            else{
                /*
                 * Fetch all fields from the hash, removing them as they are used.
                 * Afterwards use all remaining information to be wired to the a new instance
                 */
                Map<BlocksID, String> templates = AbstractTemplate.fetchLanguageTemplatesFromHash(hash);
                List<String> links = AbstractTemplate.fetchLinksFromHash(hash);
                List<String> scripts = AbstractTemplate.fetchScriptsFromHash(hash);
                PageTemplate newInstance = new PageTemplate(id, templates, links, scripts);
                Utils.autowireDaoToModel(hash, newInstance);
                String[] splitted = id.getUnversionedId().split("/");
                newInstance.name = splitted[splitted.length-1];
                return newInstance;
            }
        }
        catch(DeserializationException e){
            throw e;
        }
        catch(Exception e){
            throw new DeserializationException("Could not construct an object of class '" + PageTemplate.class.getName() + "' from specified hash.", e);
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
        return super.hashCode();
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
        if(obj instanceof PageTemplate) {
            return super.equals(obj);
        }
        else{
            return false;
        }
    }
}
