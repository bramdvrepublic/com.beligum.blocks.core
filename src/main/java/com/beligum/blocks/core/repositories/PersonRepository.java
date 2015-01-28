package com.beligum.blocks.core.repositories;

import com.beligum.blocks.core.models.sql.Person;
import com.beligum.core.framework.base.RequestContext;

import javax.persistence.EntityManager;
import java.util.List;

/**
 * Created by bas on 28.01.15.
 */
public class PersonRepository
{
    public static List<Person> getAllPersons(){
        EntityManager em = RequestContext.getEntityManager();
        return em.createQuery("SELECT p FROM Person p", Person.class).getResultList();
    }
}
