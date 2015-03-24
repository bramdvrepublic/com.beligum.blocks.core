package com.beligum.blocks.core.models.nosql;

import com.beligum.blocks.core.config.BlocksConfig;
import com.beligum.blocks.core.exceptions.ParseException;
import com.beligum.blocks.core.identifiers.BlockId;
import com.beligum.blocks.core.identifiers.MongoID;
import com.beligum.blocks.core.parsers.ElementParser;
import com.beligum.core.framework.annotations.Meta;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jongo.marshall.jackson.oid.ObjectId;
import org.jsoup.nodes.Element;
import sun.jvm.hotspot.opto.Block;

import javax.persistence.Id;

/**
 * Created by wouter on 17/03/15.
 */
public abstract class StorableTemplate extends BasicTemplate implements BlocksStorable
{

    private BlockId id;
    private META meta;

    public StorableTemplate() {
        this.meta = new META();
    }


    public StorableTemplate(Element node, String language) throws ParseException
    {
        super(node, language);
        this.meta = new META();
        String reference = ElementParser.getReferenceTo((Element) node);
        if (reference != null) {
            this.setId(BlocksConfig.getInstance().getDatabase().getIdForString(reference));
        }
    }


    @Override
    public BlockId getId() {
        return this.id;
    }

    @Override
    public void setId(BlockId id) {
        this.id = id;
    }

    @Override
    public META getMeta() {
        if (this.meta == null) {
            this.meta = new META();
        }
        return this.meta;
    }
    @Override
    public void setMeta(META meta) {
        this.meta = meta;
    }


}
