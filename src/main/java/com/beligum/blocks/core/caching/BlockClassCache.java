//package com.beligum.blocks.core.caching;
//
//import com.beligum.blocks.core.config.BlocksConfig;
//import com.beligum.blocks.core.exceptions.CacheException;
//import com.beligum.blocks.core.models.classes.BlockClass;
//import com.beligum.blocks.core.parsing.AbstractViewableParser;
//import com.beligum.blocks.core.parsing.BlockParser;
//import com.beligum.core.framework.base.R;
//
//import java.util.HashMap;
//import java.util.Map;
//
///**
//* Created by bas on 07.10.14.
//* Singleton for interacting with the applications block-cache, containing pairs of (block-class, default-block-instance)
//*/
//public class BlockClassCache extends AbstractViewableClassCache<BlockClass>
//{
//    //the instance of this singleton
//    private static BlockClassCache instance = null;
//
//    /**
//     * private constructor for singleton-use
//     */
//    private BlockClassCache(){
//    }
//
//    /**
//     * Static method for getting a singleton block-class-cacher
//     * @return a singleton instance of BlockClassCache
//     * @throws NullPointerException if no application cache could be found
//     */
//    public static BlockClassCache getInstance() throws CacheException
//    {
//        if (instance == null) {
//            //if the application-cache doesn't exist, throw exception, else instantiate the application's block-cache with a new empty hashmap
//            if (R.cacheManager() != null && R.cacheManager().getApplicationCache() != null) {
//                if (!R.cacheManager().getApplicationCache().containsKey(CacheKeys.BLOCK_CLASSES)) {
//                    R.cacheManager().getApplicationCache().put(CacheKeys.BLOCK_CLASSES, new HashMap<String, BlockClass>());
//                    instance = new BlockClassCache();
//                    instance.fillCache();
//                }
//            }
//            else {
//                throw new NullPointerException("No application-cache found.");
//            }
//        }
//        return instance;
//    }
//
//    /**
//     * This method returns a map with all default block-instances (value) of all present blockClasses (key)
//     * @returns a map of all the currently cached block-classes from the application cache
//     */
//    @Override
//    public Map<String, BlockClass> getCache(){
//        return (Map<String, BlockClass>) R.cacheManager().getApplicationCache().get(CacheKeys.BLOCK_CLASSES);
//    }
//
//    @Override
//    protected String getClassRootFolder()
//    {
//        return BlocksConfig.getBlocksFolder();
//    }
//
//    @Override
//    protected AbstractViewableParser<BlockClass> getParser(String blockClassName)
//    {
//        return new BlockParser(blockClassName);
//    }
//}
