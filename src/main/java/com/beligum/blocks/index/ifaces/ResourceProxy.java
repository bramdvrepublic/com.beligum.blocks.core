package com.beligum.blocks.index.ifaces;

import com.beligum.blocks.rdf.ifaces.RdfClass;

import java.io.Serializable;
import java.net.URI;
import java.util.Locale;

/**
 * This class represents a 'subset' of the features and properties of a resource. Eg. when a resource is saved to the indexer (eg. the JSON or SPARQL index),
 * the resource (eg. a Page) is passed through a parser that transforms the data of the resource so it can be stored in the index. The result of such
 * a parsing operation is a 'resource proxy', so that subsequently, when it's served from that index, the original resource doesn't need to be loaded,
 * but the proxy can be returned, resulting in a much higher throughput.
 *
 * Since this API is used to present the user with a 'sneek peek' of the resource, it should include enough information to do so:
 * eg; a label, a description (eg. for disambiguation), a link to the resource and maybe an image.
 * Note that this interface is only the minimal contract a proxy should (try to) implement; other fields should be added to subclasses/subinterfaces.
 * It will be used to index a snapshot of referenced resources together with the main resources that point to them (instead of just indexing the target URI).
 *
 * This interface is not limited to our own resources, since it will be used to create proxies of external resources as well (Wikidata, Geonames, ...).
 *
 * Note that this class is created with quick storage and interoperability with external APIs in mind, so we decided to have all getters return a very generic type on purpose (String, boolean, ...)
 *
 * See https://github.com/republic-of-reinvention/com.stralo.framework/issues/50
 *
 * Created by bram on Apr 10, 2019
 */
public interface ResourceProxy extends Serializable
{
    //-----PUBLIC METHODS-----
    /**
     * A public URI that uniquely identifies the resource. For local resources, this should return a relative URI.
     * Note that we prefer to use the relative address instead of the absolute one,
     * so there's no need to reindex when moving to a different domain name.
     * For remote ones, any kind of unique identifier. The more unique, the better.
     * Eg. for pages, this should return the public relative address, not the common @about resource URI.
     */
    URI getUri();

    /**
     * The descriptor (eg. the URI) or the actual value of the original resource this proxy object points to.
     * This should return the string representation of the most basic resource URI (eg. for a public page,
     * this is the low-level interconnecting "about" URI, not the public SEO-friendly one).
     */
    String getResource();

    /**
     * The string representation (CURIE) of the RDF type of the resource.
     */
    RdfClass getTypeOf();

    /**
     * The language of this resource
     */
    Locale getLanguage();

    /**
     * Specifies if the resource is external or local to this site.
     */
    boolean isExternal();

    /**
     * The id of the parent index entry or null if this entry doesn't have a parent (eg. probably only for sub-resources)
     */
    URI getParentUri();

    /**
     * The label-value (in the right language) of this resource. Keep this short and simple.
     */
    String getLabel();

    /**
     * A (possibly empty) description string (in the right language) that further describes this resource to disambiguate or provide more information.
     */
    String getDescription();

    /**
     * The address of an image describing this resource or null if the resource doesn't have an image.
     */
    URI getImage();

}
