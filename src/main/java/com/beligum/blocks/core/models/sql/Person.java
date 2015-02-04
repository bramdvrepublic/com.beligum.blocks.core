package com.beligum.blocks.core.models.sql;

import com.beligum.core.framework.models.BasicModel;

import javax.persistence.*;

/**
 * Created by bas on 15.01.15.
 */
@Entity
@NamedQueries({ @NamedQuery(
                name = Person.FIND_PERSON_BY_EMAIL,
                query = "FROM Person p WHERE p.email = :"+Person.QUERY_PARAMETER)
})
public class Person extends BasicModel
{
    public static final String FIND_PERSON_BY_EMAIL = "findPersonByEmail";
    public static final String QUERY_PARAMETER = "email";

    @OneToOne(fetch = FetchType.LAZY, cascade = { CascadeType.PERSIST, CascadeType.MERGE })
    private Subject subject;
    private String firstName;
    private String lastName;
    private String email;

    public Subject getSubject()
    {
        return subject;
    }
    public void setSubject(Subject subject)
    {
        this.subject = subject;
    }
    public String getFirstName()
    {
        return firstName;
    }
    public void setFirstName(String firstName)
    {
        this.firstName = firstName;
    }
    public String getLastName()
    {
        return lastName;
    }
    public void setLastName(String lastName)
    {
        this.lastName = lastName;
    }
    public String getEmail()
    {
        return email;
    }
    public void setEmail(String email)
    {
        this.email = email == null ? email : (email.toLowerCase().trim());
    }
}
