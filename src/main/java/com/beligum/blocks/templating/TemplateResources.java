/*
 * Copyright 2017 Republic of Reinvention bvba. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.beligum.blocks.templating;

import com.beligum.base.server.R;
import com.beligum.blocks.templating.directives.TemplateResourcesDirective;
import com.google.common.collect.Iterators;

import java.io.StringWriter;
import java.net.URI;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * This is a container to structurally save all resources (wrapped in a TemplateResources.Resource instance) during the current request.
 *
 * Created by bram on 5/18/15.
 */
public class TemplateResources
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    private Set<Resource> printedResources;
    private Set<Resource> styles;
    private Set<Resource> scripts;

    //-----CONSTRUCTORS-----
    public TemplateResources()
    {
        this.styles = new LinkedHashSet<>();
        this.scripts = new LinkedHashSet<>();

        //these doen't need order
        this.printedResources = new HashSet<>();
    }

    //-----PUBLIC METHODS-----
    public Iterable<Resource> getStyles()
    {
        final Iterator iter = styles.iterator();
        return new Iterable<Resource>()
        {
            @Override
            public Iterator iterator()
            {
                return iter;
            }
        };
    }
    public Iterable<Resource> getInlineStyles()
    {
        final Iterator iter = Iterators.filter(styles.iterator(), InlineStyle.class);
        return new Iterable<Resource>()
        {
            @Override
            public Iterator iterator()
            {
                return iter;
            }
        };
    }
    public Iterable<Resource> getExternalStyles()
    {
        final Iterator iter = Iterators.filter(styles.iterator(), ExternalStyle.class);
        return new Iterable<Resource>()
        {
            @Override
            public Iterator<Resource> iterator()
            {
                return iter;
            }
        };
    }
    public Iterable<Resource> getScripts()
    {
        final Iterator iter = scripts.iterator();
        return new Iterable<Resource>()
        {
            @Override
            public Iterator iterator()
            {
                return iter;
            }
        };
    }
    public Iterable<Resource> getInlineScripts()
    {
        final Iterator iter = Iterators.filter(scripts.iterator(), InlineScript.class);
        return new Iterable<Resource>()
        {
            @Override
            public Iterator iterator()
            {
                return iter;
            }
        };
    }
    public Iterable<Resource> getExternalScripts()
    {
        final Iterator iter = Iterators.filter(scripts.iterator(), ExternalScript.class);
        return new Iterable<Resource>()
        {
            @Override
            public Iterator iterator()
            {
                return iter;
            }
        };
    }
    public boolean addInlineStyle(String element, StringWriter writer, boolean print, HtmlTemplate.ResourceJoinHint joinHint, boolean enableDynamicFingerprinting)
    {
        return this.addResource(writer, print, new InlineStyle(element, joinHint, enableDynamicFingerprinting), this.styles);
    }
    public boolean addExternalStyle(String element, StringWriter writer, String href, boolean print, HtmlTemplate.ResourceJoinHint joinHint, boolean enableDynamicFingerprinting)
    {
        return this.addResource(writer, print, new ExternalStyle(element, href, joinHint, enableDynamicFingerprinting), this.styles);
    }
    public boolean addInlineScript(String element, StringWriter writer, boolean print, HtmlTemplate.ResourceJoinHint joinHint, boolean enableDynamicFingerprinting)
    {
        return this.addResource(writer, print, new InlineScript(element, joinHint, enableDynamicFingerprinting), this.scripts);
    }
    public boolean addExternalScript(String element, StringWriter writer, String src, boolean print, HtmlTemplate.ResourceJoinHint joinHint, boolean enableDynamicFingerprinting)
    {
        return this.addResource(writer, print, new ExternalScript(element, src, joinHint, enableDynamicFingerprinting), this.scripts);
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----
    /**
     *
     * @param writer
     * @param print Controls wether we print out the value to the output stream, or just eat it up for future use
     * @param res
     * @param set
     * @return returns true if the resource was actually added, false it it was already there (and thus skipped)
     */
    private boolean addResource(StringWriter writer, boolean print, Resource res, Set<Resource> set)
    {
        boolean retVal = !set.contains(res);

        if (print) {
            this.printedResources.add(res);

            // remove it from the set if it's there
            // this means the printed version came after the template version
            set.remove(res);

            //really write it out
            writer.write(res.getElement());
        }
        else if (!this.printedResources.contains(res)) {
            set.add(res);
        }

        return retVal;
    }

    //-----PRIVATE CLASSES-----
    public abstract class Resource
    {
        //type of this class so we can use it in switch statements
        private TemplateResourcesDirective.Argument type;

        //this is the publicly accessible value
        //in the case of inline resources, this will hold the entire <script>...</script> and <style>...</style> strings
        //in the case of external resources, this will hold the <script src="..."></script> and <link href="..." rel="stylesheet"> tags
        //this value will be printed out when the resource is needed as-is (eg. no minification, no packing, etc)
        private String element;

        //hints to the joiner system as to decide what to do (like 'don't join')
        private HtmlTemplate.ResourceJoinHint joinHint;

        //indicates if the supplied element and value are already fingerprinted or not
        protected boolean enableDynamicFingerprinting;

        //keeps track if we already modified the value to it's fingerprinted equivalent so we don't do it twice while lazy parsing
        private boolean fingerprintedElement;

        //-----CONSTRUCTORS-----
        protected Resource(TemplateResourcesDirective.Argument type, String element, HtmlTemplate.ResourceJoinHint joinHint, boolean enableDynamicFingerprinting)
        {
            this.type = type;
            this.element = element;
            this.joinHint = joinHint;
            this.enableDynamicFingerprinting = enableDynamicFingerprinting;

            this.fingerprintedElement = false;
        }

        //-----PUBLIC METHODS-----
        public TemplateResourcesDirective.Argument getType()
        {
            return type;
        }
        public String getElement()
        {
            if (this.enableDynamicFingerprinting && !this.fingerprintedElement) {
                this.element = R.resourceManager().getFingerprinter().fingerprintAllUris(this.element);
                this.fingerprintedElement = true;
            }

            return this.element;
        }
        public HtmlTemplate.ResourceJoinHint getJoinHint()
        {
            return joinHint;
        }

        //-----PRIVATE METHODS-----

        //-----MANAGEMENT METHODS-----
        @Override
        public String toString()
        {
            return this.getElement();
        }
        //These two are important to check if this resource is already present in the set of resources for this page
        @Override
        public boolean equals(Object o)
        {
            if (this == o)
                return true;
            if (!(o instanceof Resource))
                return false;

            Resource resource = (Resource) o;

            return !(element != null ? !element.equals(resource.element) : resource.element != null);

        }
        @Override
        public int hashCode()
        {
            return element != null ? element.hashCode() : 0;
        }
    }
    public abstract class InlineResource extends Resource
    {
        protected InlineResource(TemplateResourcesDirective.Argument type, String element, HtmlTemplate.ResourceJoinHint joinHint, boolean enableDynamicFingerprinting)
        {
            super(type, element, joinHint, enableDynamicFingerprinting);
        }
    }
    public abstract class ExternalResource extends Resource
    {
        //this is the value that will be used to compare two resources
        //in the case of inline resources, this is the same value as the value variable
        //in the case of external resources, this will only hold the URI of the style or script
        private String uriStr;
        private URI uri;

        protected ExternalResource(TemplateResourcesDirective.Argument type, String element, String uriStr, HtmlTemplate.ResourceJoinHint joinHint, boolean enableDynamicFingerprinting)
        {
            super(type, element, joinHint, enableDynamicFingerprinting);

            this.uriStr = uriStr;
        }

        public com.beligum.base.resources.ifaces.Resource getResource()
        {
            return R.resourceManager().get(this.getUri());
        }
        public URI getUri()
        {
            if (this.uri == null) {
                String address = this.uriStr;
                if (this.enableDynamicFingerprinting) {
                    //Note: if the uri is already fingerprinted, this will do nothing
                    address = R.resourceManager().getFingerprinter().fingerprintAllUris(address);
                }

                this.uri = URI.create(address);
            }

            return this.uri;
        }

        //we choose to override these and make use of the URI alone to detect similarity;
        //note that we might run into problems regarding the scopes etc
        //(not adding a new resource that's already present, but with different scope)
        //but it seems to work fine for now, so I'm leaving it overridden.
        @Override
        public boolean equals(Object o)
        {
            if (this == o) return true;
            if (!(o instanceof ExternalResource)) return false;
            //Note: it's important this one isn't included or a slight difference (eg. if the @type="application/javascript" comes before the @src)
            //will trigger inequality, allowing resources to be printed out in double
            //if (!super.equals(o)) return false;

            ExternalResource that = (ExternalResource) o;

            return getUri() != null ? getUri().equals(that.getUri()) : that.getUri() == null;
        }
        @Override
        public int hashCode()
        {
            //Note: see above in equals() why we can't do this
            //int result = super.hashCode();
            int result = 1;
            result = 31 * result + (getUri() != null ? getUri().hashCode() : 0);
            return result;
        }
    }

    public class InlineStyle extends InlineResource
    {
        public InlineStyle(String element, HtmlTemplate.ResourceJoinHint joinHint, boolean enableDynamicFingerprinting)
        {
            super(TemplateResourcesDirective.Argument.inlineStyles, element, joinHint, enableDynamicFingerprinting);
        }


    }

    public class InlineScript extends InlineResource
    {
        public InlineScript(String element, HtmlTemplate.ResourceJoinHint joinHint, boolean enableDynamicFingerprinting)
        {
            super(TemplateResourcesDirective.Argument.inlineScripts, element, joinHint, enableDynamicFingerprinting);
        }
    }

    public class ExternalStyle extends ExternalResource
    {
        public ExternalStyle(String element, String href, HtmlTemplate.ResourceJoinHint joinHint, boolean enableDynamicFingerprinting)
        {
            super(TemplateResourcesDirective.Argument.externalStyles, element, href, joinHint, enableDynamicFingerprinting);
        }
    }

    public class ExternalScript extends ExternalResource
    {
        public ExternalScript(String element, String src, HtmlTemplate.ResourceJoinHint joinHint, boolean enableDynamicFingerprinting)
        {
            super(TemplateResourcesDirective.Argument.externalScripts, element, src, joinHint, enableDynamicFingerprinting);
        }
    }
}
