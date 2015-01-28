package com.beligum.blocks.core.validation;

import com.beligum.blocks.core.models.sql.Person;
import com.beligum.core.framework.base.RequestContext;
import com.beligum.core.framework.security.Authentication;
import com.beligum.core.framework.security.Principal;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.validation.*;
import java.lang.annotation.*;

/**
 * Created by bramdeveirman on 2/10/14.
 */
@Documented
@Target({ ElementType.METHOD, ElementType.FIELD, ElementType.ANNOTATION_TYPE })
@Retention(RetentionPolicy.RUNTIME)
@ReportAsSingleViolation
@Constraint(validatedBy = EmailDoesNotExist.EmailExistsCheckerValidator.class)
public @interface EmailDoesNotExist
{
    String message() default "Email already exists";

    Class<?>[] groups() default { };

    Class<? extends Payload>[] payload() default { };

    class EmailExistsCheckerValidator implements ConstraintValidator<EmailDoesNotExist, String> {
        @Override
        public void initialize(EmailDoesNotExist arg0) {
        }

        @Override
        public boolean isValid(String email, ConstraintValidatorContext arg1) {
            EntityManager em = RequestContext.getEntityManager();
            Principal currentPrincipal = Authentication.getCurrentPrincipal();
            Person person = null;
            boolean retVal = false;
            if (currentPrincipal != null && email.toLowerCase().trim().equals(currentPrincipal.getUsername().toLowerCase().trim())) {
                return true;
            }
                try {
                    person = em.createQuery("SELECT p FROM Person p WHERE p.email like :email", Person.class).setParameter("email", email).getSingleResult();
                } catch (NoResultException ex) {
                    return true;
                }
                return retVal;
            }
        }
    }

