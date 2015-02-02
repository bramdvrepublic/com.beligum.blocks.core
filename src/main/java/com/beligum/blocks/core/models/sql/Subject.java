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
public class Subject extends AbstractSubject implements Comparable<Subject>
{
    @OneToOne(mappedBy = "subject")
    private Person person;

    public Person getPerson()
    {
        return person;
    }
    public void setPerson(Person person)
    {
        this.person = person;
    }
    @Override
    public String getName()
    {
        return this.person.getFirstName() + " " + this.person.getLastName();
    }

    /**
     * Compare subject by role-name, for sorting in user-list.
     * @param subject
     */
    @Override
    public int compareTo(Subject subject)
    {
        return this.getRole().compareToIgnoreCase(subject.getRole());
    }
}
