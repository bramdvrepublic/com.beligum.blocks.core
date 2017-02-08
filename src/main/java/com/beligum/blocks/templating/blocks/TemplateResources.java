package com.beligum.blocks.templating.blocks;

import com.beligum.blocks.templating.blocks.directives.TemplateResourcesDirective;
import com.google.common.collect.Iterators;

import java.io.StringWriter;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * This is a little container for all resources in the current request.
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
    public boolean addInlineStyle(String element, StringWriter writer, boolean print, HtmlTemplate.ResourceJoinHint joinHint)
    {
        return this.addResource(writer, print, new InlineStyle(element, writer.getBuffer().length(), joinHint), this.styles);
    }
    public boolean addExternalStyle(String element, StringWriter writer, String href, boolean print, HtmlTemplate.ResourceJoinHint joinHint)
    {
        return this.addResource(writer, print, new ExternalStyle(element, href, writer.getBuffer().length(), joinHint), this.styles);
    }
    public boolean addInlineScript(String element, StringWriter writer, boolean print, HtmlTemplate.ResourceJoinHint joinHint)
    {
        return this.addResource(writer, print, new InlineScript(element, writer.getBuffer().length(), joinHint), this.scripts);
    }
    public boolean addExternalScript(String element, StringWriter writer, String src, boolean print, HtmlTemplate.ResourceJoinHint joinHint)
    {
        return this.addResource(writer, print, new ExternalScript(element, src, writer.getBuffer().length(), joinHint), this.scripts);
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

        //this is the value that will be used to compare two resources
        //in the case of inline resources, this is the same value as the value variable
        //in the case of external resources, this will only hold the URI of the style or script
        private String value;

        //this is the publicly accessible value
        //in the case of inline resources, this will hold the entire <script>...</script> and <style>...</style> strings
        //in the case of external resources, this will hold the <script src="..."></script> and <link href="..." rel="stylesheet"> tags
        //this value will be printed out when the resource is needed as-is (eg. no minification, no packing, etc)
        private String element;

        //this is the position in the output writer the resource should be written (if not handled otherwise)
        private int bufferPosition;

        //hints to the joiner system as to decide what to do (like 'don't join')
        private HtmlTemplate.ResourceJoinHint joinHint;

        //keeps track if we already modified the value to it's fingerprinted equivalent so we don't do it twice while lazy parsing
        private boolean fingerprintedValue;
        private boolean fingerprintedElement;

        protected Resource(TemplateResourcesDirective.Argument type, String element, int bufferPosition, HtmlTemplate.ResourceJoinHint joinHint)
        {
            this(type, element, element, bufferPosition, joinHint);
        }
        protected Resource(TemplateResourcesDirective.Argument type, String element, String value, int bufferPosition, HtmlTemplate.ResourceJoinHint joinHint)
        {
            this.type = type;
            this.value = value;
            this.element = element;
            this.bufferPosition = bufferPosition;
            this.joinHint = joinHint;

            this.fingerprintedValue = false;
            this.fingerprintedElement = false;
        }

        //-----PUBLIC METHODS-----
        public TemplateResourcesDirective.Argument getType()
        {
            return type;
        }
        public String getElement()
        {
            this.checkElementFingerprint();

            return this.element;
        }
        public String getValue()
        {
            this.checkValueFingerprint();

            return this.value;
        }
        public HtmlTemplate.ResourceJoinHint getJoinHint()
        {
            return joinHint;
        }

        //-----PRIVATE METHODS-----
        private void checkElementFingerprint()
        {
//            if (R.configuration().getResourceConfig().getEnableFingerprintedResources() && !this.fingerprintedElement) {
//                //optimization: if the value==the element, we'll do both at once
//                boolean same = this.value.equals(this.element);
//                this.element = R.resourceManager().getFingerprinter().fingerprintUris(this.element);
//
//                this.fingerprintedElement = true;
//                if (same) {
//                    this.value = this.element;
//                    this.fingerprintedValue = true;
//                }
//            }
        }
        private void checkValueFingerprint()
        {
//            if (R.configuration().getResourceConfig().getEnableFingerprintedResources() && !this.fingerprintedValue) {
//                //optimization: if the value==the element, we'll do both at once
//                boolean same = this.value.equals(this.element);
//                //Note: if the uri is already fingerprinted, this will do nothing
//                this.value = R.resourceManager().getFingerprinter().fingerprintUris(this.value);
//
//                this.fingerprintedValue = true;
//                if (same) {
//                    this.element = this.value;
//                    this.fingerprintedElement = true;
//                }
//            }
        }

        //-----MANAGEMENT METHODS-----
        @Override
        public String toString()
        {
            return this.getElement();
        }
        @Override
        public boolean equals(Object o)
        {
            if (this == o)
                return true;
            if (!(o instanceof Resource))
                return false;

            Resource resource = (Resource) o;

            return !(value != null ? !value.equals(resource.value) : resource.value != null);

        }
        @Override
        public int hashCode()
        {
            return value != null ? value.hashCode() : 0;
        }
    }

    public class InlineStyle extends Resource
    {
        public InlineStyle(String element, int bufferPosition, HtmlTemplate.ResourceJoinHint joinHint)
        {
            super(TemplateResourcesDirective.Argument.inlineStyles, element, bufferPosition, joinHint);
        }
    }

    public class ExternalStyle extends Resource
    {
        public ExternalStyle(String element, String href, int bufferPosition, HtmlTemplate.ResourceJoinHint joinHint)
        {
            super(TemplateResourcesDirective.Argument.externalStyles, element, href, bufferPosition, joinHint);
        }
    }

    public class InlineScript extends Resource
    {
        public InlineScript(String element, int bufferPosition, HtmlTemplate.ResourceJoinHint joinHint)
        {
            super(TemplateResourcesDirective.Argument.inlineScripts, element, bufferPosition, joinHint);
        }
    }

    public class ExternalScript extends Resource
    {
        public ExternalScript(String element, String src, int bufferPosition, HtmlTemplate.ResourceJoinHint joinHint)
        {
            super(TemplateResourcesDirective.Argument.externalScripts, element, src, bufferPosition, joinHint);
        }
    }
}
