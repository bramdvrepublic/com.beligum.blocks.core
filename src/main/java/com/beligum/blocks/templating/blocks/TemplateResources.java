package com.beligum.blocks.templating.blocks;

import com.beligum.blocks.templating.blocks.directives.TemplateResourcesDirective;
import com.google.common.collect.Iterators;

import java.io.StringWriter;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

/**
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
    public boolean addInlineStyle(boolean print, String value, StringWriter writer)
    {
        return this.addResource(print, new InlineStyle(value, writer.getBuffer().length()), this.styles);
    }
    public boolean addExternalStyle(boolean print, String href, String element, StringWriter writer)
    {
        return this.addResource(print, new ExternalStyle(href, element, writer.getBuffer().length()), this.styles);
    }
    public boolean addInlineScript(boolean print, String value, StringWriter writer)
    {
        return this.addResource(print, new InlineScript(value, writer.getBuffer().length()), this.scripts);
    }
    public boolean addExternalScript(boolean print, String src, String element, StringWriter writer)
    {
        return this.addResource(print, new ExternalScript(src, element, writer.getBuffer().length()), this.scripts);
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----
    /**
     * @param print Controls wether we print out the value to the output stream, or just eat it up for future use
     * @param res
     * @param set
     * @return returns true if the resource was actually added, false it it was already there (and thus skipped)
     */
    private boolean addResource(boolean print, Resource res, Set<Resource> set)
    {
        boolean retVal = !set.contains(res);

        if (print) {
            this.printedResources.add(res);
            // remove it from the set if it's there
            // this means the printed version came after the template version
            set.remove(res);
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
        protected TemplateResourcesDirective.Argument type;
        //this is the value that will be used to compare two resources
        protected String equalsValue;
        //this is the publicly accessible value
        protected String value;
        //this is the position in the output writer the resource should be written (if not handled otherwise)
        protected int bufferPosition;

        protected Resource(TemplateResourcesDirective.Argument type, String value, int bufferPosition)
        {
            this(type, value, value, bufferPosition);
        }
        protected Resource(TemplateResourcesDirective.Argument type, String equalsValue, String value, int bufferPosition)
        {
            this.type = type;
            this.equalsValue = equalsValue;
            this.value = value;
            this.bufferPosition = bufferPosition;
        }

        public TemplateResourcesDirective.Argument getType()
        {
            return type;
        }
        public String getValue()
        {
            return this.value;
        }
        public String getEqualsValue()
        {
            return equalsValue;
        }

        @Override
        public String toString()
        {
            return this.getValue();
        }
        @Override
        public boolean equals(Object o)
        {
            if (this == o)
                return true;
            if (!(o instanceof Resource))
                return false;

            Resource resource = (Resource) o;

            return !(equalsValue != null ? !equalsValue.equals(resource.equalsValue) : resource.equalsValue != null);

        }
        @Override
        public int hashCode()
        {
            return equalsValue != null ? equalsValue.hashCode() : 0;
        }
    }

    public class InlineStyle extends Resource
    {
        public InlineStyle(String value, int bufferPosition)
        {
            super(TemplateResourcesDirective.Argument.inlineStyles, value, bufferPosition);
        }
    }

    public class ExternalStyle extends Resource
    {
        public ExternalStyle(String href, String element, int bufferPosition)
        {
            super(TemplateResourcesDirective.Argument.externalStyles, href, element, bufferPosition);
        }
    }

    public class InlineScript extends Resource
    {
        public InlineScript(String value, int bufferPosition)
        {
            super(TemplateResourcesDirective.Argument.inlineScripts, value, bufferPosition);
        }
    }

    public class ExternalScript extends Resource
    {
        public ExternalScript(String src, String element, int bufferPosition)
        {
            super(TemplateResourcesDirective.Argument.externalScripts, src, element, bufferPosition);
        }
    }
}
