package com.beligum.blocks.serializing;

import com.beligum.base.cache.CacheFunction;
import com.beligum.base.cache.CacheKey;
import com.beligum.base.filesystem.ConstantsFileEntry;
import com.beligum.base.server.R;
import com.beligum.base.utils.toolkit.ReflectionFunctions;
import com.beligum.blocks.caching.CacheKeys;
import com.beligum.blocks.rdf.ifaces.RdfProperty;
import com.beligum.blocks.templating.TagTemplate;

import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Created by bram on Aug 19, 2019
 */
public interface BlockSerializer
{
    //-----CONSTANTS-----

    //-----VARIABLES-----

    //-----PUBLIC METHODS-----

    /**
     * Shortcut for the method below without classes or styles
     */
    CharSequence toHtml(TagTemplate blockType, RdfProperty property, Locale language, String value) throws IOException;

    /**
     * This is the most general contract an importer class must provide to import data.
     * This method returns the serialized and normalized (!) HTML of a block with the supplied data, property and language, ready to be used in page templates.
     */
    CharSequence toHtml(TagTemplate blockType, RdfProperty property, Locale language, ConstantsFileEntry[] classes, Map<String, String> styles, String value) throws IOException;

    /**
     * This method must be implemented by all serializers to report back the block types they support.
     */
    Iterable<TagTemplate> getSupportedBlockTypes();

    /**
     * Lookup the serializer for the specified block type (or null if no serializer was found)
     */
    static BlockSerializer lookup(TagTemplate blockType)
    {
        Map<TagTemplate, BlockSerializer> mapping = R.cacheManager().getApplicationCache().getAndInitIfAbsent(CacheKeys.SERIALIZER_MAPPING,
                                                                         new CacheFunction<CacheKey, Map<TagTemplate, BlockSerializer>>()
                                                                         {
                                                                             @Override
                                                                             public Map<TagTemplate, BlockSerializer> apply(CacheKey cacheKey) throws IOException
                                                                             {
                                                                                 Map<TagTemplate, BlockSerializer> retVal = new HashMap<>();

                                                                                 Set<Class<? extends BlockSerializer>> allSerializers = ReflectionFunctions.searchAllClassesImplementing(BlockSerializer.class, true);
                                                                                 for (Class<? extends BlockSerializer> serializerClass : allSerializers) {
                                                                                     try {
                                                                                         BlockSerializer serializer = serializerClass.newInstance();
                                                                                         for (TagTemplate tag : serializer.getSupportedBlockTypes()) {
                                                                                             retVal.put(tag, serializer);
                                                                                         }
                                                                                     }
                                                                                     catch (Exception e) {
                                                                                         throw new IOException("Error while instantiating block serializer; "+serializerClass, e);
                                                                                     }
                                                                                 }

                                                                                 return retVal;
                                                                             }
                                                                         }
        );

        return mapping.get(blockType);
    }
}
