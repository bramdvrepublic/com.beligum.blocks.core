package com.beligum.blocks.rdf.importers;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.graph.BlankNodeId;
import org.apache.jena.rdf.model.AnonId;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;

import org.openrdf.model.BNode;
import org.openrdf.model.Literal;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;


/**
 * Copied from https://searchcode.com/codesearch/view/47062473/
 *
 * (c) Copyright 2009 Talis Information Ltd. All rights reserved.
 *
 * Also see http://repo.aduna-software.org/websvn/filedetails.php?repname=aduna&path=%2Forg.openrdf%2Fprojects%2Fsesame2-contrib%2Fopenrdf-ext-contrib%2Fopenrdf-jena%2Fsrc%2Fmain%2Fjava%2Forg%2Fopenrdf%2Fjena%2FValueToNode.java&rev=2944&sc=1
 *
 * Created by bram on 1/25/16.
 */
public class Convert
{
    //-----CONSTANTS-----

    //-----VARIABLES-----

    //-----CONSTRUCTORS-----

    //-----PUBLIC METHODS-----
    public static Node bnodeToNode(final BNode value)
    {
        return NodeFactory.createBlankNode(new BlankNodeId(value.getID()));
    }

    public static Node literalToNode(final Literal value)
    {
        if(value.getLanguage() != null)
        {
            return NodeFactory.createLiteral(value.getLabel(), value.getLanguage(), false);
        }
        if(value.getDatatype() != null)
        {
            return NodeFactory.createLiteral(value.getLabel(), null, NodeFactory.getType(value.getDatatype().stringValue()));
        }
        // Plain literal
        return NodeFactory.createLiteral(value.getLabel());
    }

    public static RDFNode literalToRDFNode(final Model nextModel, final Literal value)
    {
        if(value.getDatatype() != null)
        {
            return nextModel.createTypedLiteral(value.stringValue(), value.getDatatype().stringValue());
        }
        else if(value.getLanguage() != null && !"".equals(value.getLanguage()))
        {
            return nextModel.createLiteral(value.stringValue(), value.getLanguage());
        }
        else
        {
            return nextModel.createLiteral(value.stringValue());
        }
    }

    public static BNode nodeBlankToBNode(final ValueFactory factory, final Node node)
    {
        return factory.createBNode(node.getBlankNodeLabel());
    }

    public static Value nodeLiteralToLiteral(final ValueFactory factory, final Node node)
    {
        if(node.getLiteralDatatype() != null)
        {
            final URI x = factory.createURI(node.getLiteralDatatypeURI());
            return factory.createLiteral(node.getLiteralLexicalForm(), x);
        }
        if(!node.getLiteralLanguage().equals(""))
        {
            return factory.createLiteral(node.getLiteralLexicalForm(), node.getLiteralLanguage());
        }

        return factory.createLiteral(node.getLiteralLexicalForm());
    }

    public static Value nodeToValue(final ValueFactory factory, final Node node)
    {
        if(node.isLiteral())
        {
            return Convert.nodeLiteralToLiteral(factory, node);
        }
        if(node.isURI())
        {
            return Convert.nodeURIToValue(factory, node);
        }
        if(node.isBlank())
        {
            return Convert.nodeBlankToBNode(factory, node);
        }
        throw new IllegalArgumentException("Not a concrete node");
    }

    public static Resource nodeToValueResource(final ValueFactory factory, final Node node)
    {
        if(node.isURI())
        {
            return Convert.nodeURIToValue(factory, node);
        }
        if(node.isBlank())
        {
            return Convert.nodeBlankToBNode(factory, node);
        }
        throw new IllegalArgumentException("Neither a URI nor a blank node");
    }

    public static URI nodeURIToValue(final ValueFactory factory, final Node node)
    {
        return factory.createURI(node.getURI());
    }

    public static Node resourceToNode(final Resource resource)
    {
        return Convert.valueToNode(resource);
    }

    public static org.apache.jena.rdf.model.Resource resourceToResource(final Model nextModel, final Resource resource)
    {
        if(resource instanceof URI)
        {
            return nextModel.createResource(resource.stringValue());
        }
        else
        {
            return nextModel.createResource(new AnonId(resource.stringValue()));
        }
    }

    public static org.apache.jena.rdf.model.Statement statementToJenaStatement(final Model nextModel,
                                                                               final Statement stmt)
    {
        final org.apache.jena.rdf.model.Resource s = Convert.resourceToResource(nextModel, stmt.getSubject());
        final Property p = Convert.uriToProperty(nextModel, stmt.getPredicate());
        final RDFNode o = Convert.valueToRDFNode(nextModel, stmt.getObject());

        return nextModel.createStatement(s, p, o);
    }

    /* BEGIN ADDED BY THOMAS FRANCART */
    public static org.openrdf.model.URI propertyToURI(
                    final ValueFactory factory,
                    final org.apache.jena.rdf.model.Property p
    ) {
        return factory.createURI(p.getURI());
    }

    public static org.openrdf.model.Statement statementToSesameStatement(
                    final ValueFactory factory,
                    final org.apache.jena.rdf.model.Statement stmt
    ) {
        final org.openrdf.model.Resource subject = Convert.nodeToValueResource(factory, stmt.getSubject().asNode());
        final org.openrdf.model.URI predicate = Convert.propertyToURI(factory, stmt.getPredicate());
        final org.openrdf.model.Value object = Convert.nodeToValue(factory, stmt.getObject().asNode());

        return factory.createStatement(subject, predicate, object);
    }
    /* END ADDED BY THOMAS FRANCART */

    public static Triple statementToTriple(final Statement stmt)
    {
        final Node s = Convert.resourceToNode(stmt.getSubject());
        final Node p = Convert.uriToNode(stmt.getPredicate());
        final Node o = Convert.valueToNode(stmt.getObject());
        return new Triple(s, p, o);
    }

    // ----
    // Problems with the ValueFactory

    public static Statement tripleToStatement(final ValueFactory factory, final Triple triple)
    {
        return factory.createStatement(Convert.nodeToValueResource(factory, triple.getSubject()),
                                       Convert.nodeURIToValue(factory, triple.getPredicate()),
                                       Convert.nodeToValue(factory, triple.getObject()));
    }

    public static Node uriToNode(final URI value)
    {
        return NodeFactory.createURI(value.stringValue());
    }

    public static org.apache.jena.rdf.model.Property uriToProperty(final Model nextModel, final URI resource)
    {
        return nextModel.createProperty(resource.stringValue());
    }

    public static Node valueToNode(final Value value)
    {
        if(value instanceof Literal)
        {
            return Convert.literalToNode((Literal)value);
        }
        if(value instanceof URI)
        {
            return Convert.uriToNode((URI)value);
        }
        if(value instanceof BNode)
        {
            return Convert.bnodeToNode((BNode)value);
        }
        throw new IllegalArgumentException("Not a concrete value");
    }

    public static RDFNode valueToRDFNode(final Model nextModel, final Value value)
    {
        if(value instanceof Resource)
        {
            return Convert.resourceToResource(nextModel, (Resource)value);
        }
        else
        {
            return Convert.literalToRDFNode(nextModel, (Literal)value);
        }
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}
