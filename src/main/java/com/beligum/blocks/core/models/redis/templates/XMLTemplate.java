package com.beligum.blocks.core.models.redis.templates;

import com.beligum.blocks.core.exceptions.DeserializationException;
import com.beligum.blocks.core.identifiers.BlocksID;
import com.beligum.blocks.core.utils.Utils;

import java.util.List;
import java.util.Map;

/**
 * Created by bas on 24.02.15.
 */
public class XMLTemplate extends AbstractTemplate
{
    private String fileName;
    /**
     * Constructor for template with one language: the one present in the id. (Other language-templates could be added later if wanted.)
     *
     * @param id       id for this template
     * @param xml the xml-string of the file
     * @throws NullPointerException if the template is null
     */
    public XMLTemplate(BlocksID id, String xml, String fileName)
    {
        super(id, xml, null, null);
        this.fileName = fileName;
    }

    /**
     * Constructor used when fetched from db.
     *
     * @param id        id for this template
     * @param templates the map of templates (language -> template) which represent the content of this template
     */
    private XMLTemplate(BlocksID id, Map<BlocksID, String> templates)
    {
        super(id, templates, null, null);
    }
    /**
     * @return the language stored in the id of this template
     */
    @Override
    public String getName()
    {
        return fileName;
    }

    /**
     * The {@link com.beligum.blocks.core.models.redis.templates.XMLTemplate}-class can be used as a factory, to construct templates from data found in a hash in the redis-db
     * @param hash a map, mapping field-names to field-values
     * @return an template or null if no template could be constructed from the specified hash
     * @throws com.beligum.blocks.core.exceptions.DeserializationException when a bad hash is found
     */
    //is protected so that all classes in package can access this method
    protected static XMLTemplate createInstanceFromHash(BlocksID id, Map<String, String> hash) throws DeserializationException
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
                XMLTemplate newInstance = new XMLTemplate(id,templates);
                Utils.autowireDaoToModel(hash, newInstance);
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
}
