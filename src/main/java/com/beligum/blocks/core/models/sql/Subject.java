package com.beligum.blocks.core.models.sql;

import com.beligum.core.framework.models.AbstractSubject;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.OneToOne;

/**
 * Created by bas on 15.01.15.
 */
@Entity
@DiscriminatorValue("subject")
public class Subject extends AbstractSubject
{
    @OneToOne(mappedBy = "subject")
    private Person person;

    @Override
    public String getName()
    {
        return this.person.getFirstName() + " " + this.person.getLastName();
    }
}
