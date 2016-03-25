package com.beligum.blocks.endpoints.ifaces;

import java.net.URI;

/**
 * Created by bram on 3/12/16.
 */
public interface ResourceInfo
{
    /**
     * The public ID of this resource
     */
    URI getResourceUri();

    /**
     * The curie URI type of this resource
     */
    URI getResourceType();

    /**
     * The label-value (eg. i18n caption-text) to render for this resource
     */
    String getLabel();

    /**
     * The hyperlink to attach to the label (may be null, then don't render a hyperlink, but plain text)
     */
    URI getLink();

    /**
     * Specifies if the link of getLink() (if any) is external (eg. should open in a new tab/window) or local to this site.
     */
    boolean isExternalLink();

    /**
     * The URL of the image-value of this resource or null if this value doesn't have an image. If not-null, this takes precedence over the label (which is attached to the alt attribute of this image)
     */
    URI getImage();

    /**
     * Unlike the label, this is the more 'official' name of this value; eg the name that will be placed in the autocomplete input box when we load the value back in
     * For example: the label for "Brussels" might be "Brussels, capital of Belgium" and it's (Dutch) name could eg. be "Brussel"
     */
    String getName();
}
