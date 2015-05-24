package com.beligum.blocks.repositories;

import com.beligum.blocks.models.jsonld.interfaces.Resource;

/**
 * Created by wouter on 13/05/15.
 */
public class ResourceRepository
{
    private static ResourceRepository instance;

    private ResourceRepository() {

    }

    public static ResourceRepository instance() {
        if (instance == null) {
            ResourceRepository.instance = new ResourceRepository();
        }
        return ResourceRepository.instance;
    }


    public void save(Resource resource) {
        //  check if id
        // if not create new resource
        // if defaultLang -> create
        // if not defaultlang -> create default lang + create translation + create EDGE

        // if id -> get original
        //
        // Versioning?


    }


}
