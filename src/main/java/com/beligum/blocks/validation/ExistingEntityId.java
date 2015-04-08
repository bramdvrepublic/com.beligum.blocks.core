package com.beligum.blocks.validation;

import com.beligum.base.server.RequestContext;
import com.beligum.base.utils.Logger;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.validation.*;
import javax.validation.constraints.NotNull;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;

/**
 * Created by bram on 10/6/14.
 */
//Annotation saying this newly defined annotation can be placed on class-fields
@Target({ ElementType.FIELD, ElementType.PARAMETER })
//Annotation saying this newly defined annotation will be stored inside the .class-file by the compiler so it is available during runtime
@Retention(RetentionPolicy.RUNTIME)
//Annotation for declaring this a java-bean constraint. This contraint on a given class will be validated by the class 'validatedBy'
@Constraint(validatedBy = ExistingEntityId.Validator.class)
@ReportAsSingleViolation
@NotNull
public @interface ExistingEntityId
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    //
    Class<?> entityClass();

    //string for the error-message returned to the client when not able to validate
    String message() default "entity doesn't exist";

    //array for allowing group-validation
    Class<?>[] groups() default { };

    //field for allowing different payloads of the violated constraint (f.i. Info or Error)
    Class<? extends Payload>[] payload() default { };

    //-----CONSTRUCTORS-----

    //-----PUBLIC METHODS-----

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

    class Validator implements ConstraintValidator<ExistingEntityId, Long>
    {
        private Class<?> entityClass;

        @Override
        public void initialize(ExistingEntityId constraintAnnotation)
        {
            this.entityClass = constraintAnnotation.entityClass();
        }
        @Override
        public boolean isValid(Long value, ConstraintValidatorContext context)
        {
            boolean retVal = false;

            if (this.entityClass != null && value != null && value > 0) {
                EntityManager em = RequestContext.getEntityManager();
                try {
                    TypedQuery<?> query = em.createQuery("SELECT s.id FROM " + this.entityClass.getCanonicalName() + " s WHERE s.id=:id", Long.class).setParameter("id", value);
                    List<?> results = query.getResultList();

                    if (results != null && !results.isEmpty()) {
                        retVal = true;
                    }
                }
                catch (Exception e) {
                    Logger.debug("Entity type " + this.entityClass.getCanonicalName() + " with id " + value + " is not a valid database Entity");
                }
            }

            return retVal;
        }
    }
}
