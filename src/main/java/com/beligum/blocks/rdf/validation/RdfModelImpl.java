package com.beligum.blocks.rdf.validation;

import com.beligum.blocks.exceptions.RdfValidationException;
import com.beligum.blocks.rdf.validation.ifaces.RdfClassInstance;
import com.beligum.blocks.rdf.validation.ifaces.RdfModel;
import com.beligum.blocks.utils.RdfTools;
import com.google.common.collect.Iterables;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Statement;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by bram on May 21, 2019
 */
public class RdfModelImpl implements RdfModel
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    private final Model model;
    private Map<IRI, RdfClassInstanceImpl> instances;

    //-----CONSTRUCTORS-----
    public RdfModelImpl(Model model)
    {
        this.model = model;
        this.instances = new HashMap<>();

        for (Statement stmt : this.model) {

            IRI subjectIri = (IRI) stmt.getSubject();

            if (!this.instances.containsKey(subjectIri)) {
                this.instances.put(subjectIri, new RdfClassInstanceImpl(RdfTools.iriToUri(subjectIri)));
            }

            this.instances.get(subjectIri).addStatement(stmt);
        }
    }

    //-----PUBLIC METHODS-----
    @Override
    public Iterable<RdfClassInstance> getInstances()
    {
        return Iterables.filter(this.instances.values(), RdfClassInstance.class);
    }
    @Override
    public void validate() throws RdfValidationException
    {
        // a model is valid if all it's instances are (recursively) valid
        for (RdfClassInstanceImpl i : this.instances.values()) {
            i.validate();
        }
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----
}
