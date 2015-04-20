package com.beligum.blocks.models.rdf;

import com.hp.hpl.jena.graph.Capabilities;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.NodeFactory;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.graph.impl.GraphBase;
import com.hp.hpl.jena.graph.impl.SimpleEventManager;
import com.hp.hpl.jena.util.iterator.ClosableIterator;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;
import com.hp.hpl.jena.util.iterator.Filter;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Created by wouter on 16/04/15.
 */
public class OrderedMemGraph extends GraphBase
{

    private Set<Triple> triples = new LinkedHashSet<>() ;

    public OrderedMemGraph() {}

    @Override
    public Capabilities getCapabilities() {
        return gmpCapabilities;
    }

    @Override
    public void performAdd( Triple t )
    { triples.add(t) ; }

    @Override
    public void performDelete( Triple t )
    { triples.remove(t) ; }

    @Override
    public boolean graphBaseContains( Triple t )
    {
        if ( t.isConcrete() )
            return triples.contains( t ) ;

        ClosableIterator<Triple> it = find( t );
        try {
            for ( ; it.hasNext() ; )
            {
                Triple t2 = it.next() ;
                if ( tripleContained(t, t2) )
                    return true ;
            }
        } finally { it.close(); }
        return false ;
    }

    @Override
    protected ExtendedIterator<Triple> graphBaseFind(Triple m)
    {
        Iterator<Triple> iter = triples.iterator() ;
        return
                        SimpleEventManager.notifyingRemove(this, iter)
                                          .filterKeep ( new TripleMatchFilterEquality( m ) );
    }

    static boolean tripleContained(Triple patternTriple, Triple dataTriple)
    {
        return
                        equalNode(patternTriple.getSubject(),   dataTriple.getSubject()) &&
                        equalNode(patternTriple.getPredicate(), dataTriple.getPredicate()) &&
                        equalNode(patternTriple.getObject(),    dataTriple.getObject()) ;
    }

    private static boolean equalNode(Node m, Node n)
    {
        // m should not be null unless .getMatchXXXX used to get the node.
        // Language tag canonicalization
        n = fixupNode(n) ;
        m = fixupNode(m) ;
        return (m==null) || (m == Node.ANY) || m.equals(n) ;
    }

    private static Node fixupNode(Node node)
    {
        if ( node == null || node == Node.ANY )
            return node ;

        // RDF says ... language tags should be canonicalized to lower case.
        if ( node.isLiteral() )
        {
            String lang = node.getLiteralLanguage() ;
            if ( lang != null && ! lang.equals("") )
                node = NodeFactory.createLiteral(node.getLiteralLexicalForm(), lang.toLowerCase(Locale.ROOT)) ;
        }
        return node ;
    }

    static class TripleMatchFilterEquality extends Filter<Triple>
    {
        final protected Triple tMatch;

        /** Creates new TripleMatchFilter */
        public TripleMatchFilterEquality(Triple tMatch)
        { this.tMatch = tMatch; }

        @Override
        public boolean accept(Triple t)
        {
            return tripleContained(tMatch, t) ;
        }

    }

    private static Capabilities gmpCapabilities = new Capabilities() {

        @Override
        public boolean sizeAccurate() {
            return true;
        }

        @Override
        public boolean addAllowed() {
            return true;
        }

        @Override
        public boolean addAllowed(boolean everyTriple) {
            return true;
        }

        @Override
        public boolean deleteAllowed() {
            return true;
        }

        @Override
        public boolean deleteAllowed(boolean everyTriple) {
            return true;
        }

        @Override
        public boolean iteratorRemoveAllowed() {
            return true;
        }

        @Override
        public boolean canBeEmpty() {
            return true;
        }

        @Override
        public boolean findContractSafe() {
            return true;
        }

        @Override
        public boolean handlesLiteralTyping() {
            return false;
        }

    };
}
