package com.beligum.blocks.fs.index.entries.resources;

import org.openrdf.model.Model;

import java.net.URI;

/**
 * Class used to pull some general information (like title, description, image, link, ...) from a resource class to be used to render search results.
 * This is used while indexing a page to be able to render search results from the index without having to look up the whole page.
 *
 * Created by bram on 5/9/16.
 */
public interface ResourceIndexer
{
    //-----CONSTANTS-----

    //-----VARIABLES-----

    //-----CONSTRUCTORS-----

    //-----PUBLIC METHODS-----
    /**
     * Parse the supplied model and return the generated index that was extracted from it
     */
    IndexedResource index(Model model);

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

    //-----INNER CLASSES-----
    interface IndexedResource
    {
        String getTitle();
        String getDescription();
        URI getImage();
    }
    class DefaultIndexedResource implements IndexedResource
    {
        private String title;
        private String description;
        private URI image;

        public DefaultIndexedResource(String title, String description, URI image)
        {
            this.title = title;
            this.description = description;
            this.image = image;
        }

        @Override
        public String getTitle()
        {
            return title;
        }
        @Override
        public String getDescription()
        {
            return description;
        }
        @Override
        public URI getImage()
        {
            return image;
        }
    }
}
