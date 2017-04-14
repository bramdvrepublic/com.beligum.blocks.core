package com.beligum.blocks.config;

import com.beligum.blocks.rdf.ifaces.RdfClass;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Created by bram on 14/04/17.
 */
public class RdfClassNode implements Comparable<RdfClassNode>
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    private RdfClass rdfClass;
    private Set<RdfClassNode> dependencies;

    //-----CONSTRUCTORS-----
    public RdfClassNode(RdfClass rdfClass)
    {
        this.rdfClass = rdfClass;
        this.dependencies = new LinkedHashSet<>();
    }

    //-----PUBLIC METHODS-----
    public RdfClass getRdfClass()
    {
        return rdfClass;
    }
    public void addDependency(RdfClassNode rdfClassNode)
    {
        this.dependencies.add(rdfClassNode);
    }
    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (!(o instanceof RdfClassNode)) return false;

        RdfClassNode that = (RdfClassNode) o;

        return getRdfClass() != null ? getRdfClass().equals(that.getRdfClass()) : that.getRdfClass() == null;
    }
    @Override
    public int hashCode()
    {
        return getRdfClass() != null ? getRdfClass().hashCode() : 0;
    }
    @Override
    public int compareTo(RdfClassNode o)
    {
        final int BEFORE = -1;
        final int EQUAL = 0;
        final int AFTER = 1;

        if (o.equals(this)) {
            return EQUAL;
        }
        else if (this.dependencies.contains(o)) {
            return AFTER;
        }
        else {
            return BEFORE;
        }
    }
    @Override
    public String toString()
    {
        return rdfClass.toString();
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}
