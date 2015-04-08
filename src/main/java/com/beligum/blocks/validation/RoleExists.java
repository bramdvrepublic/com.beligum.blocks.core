package com.beligum.blocks.validation;

import com.beligum.blocks.usermanagement.Permissions;
import org.hibernate.validator.constraints.NotBlank;

import javax.validation.*;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by bas on 28.01.15.
 */
//Annotation saying this newly defined annotation can be placed on class-fields
@Target({ ElementType.FIELD, ElementType.PARAMETER })
//Annotation saying this newly defined annotation will be stored inside the .class-file by the compiler so it is available during runtime
@Retention(RetentionPolicy.RUNTIME)
//Annotation for declaring this a java-bean constraint. This contraint on a given class will be validated by the class 'validatedBy'
@Constraint(validatedBy = RoleExists.Validator.class)
//report all constraint-annotations as one violation, if necessary
@ReportAsSingleViolation
@NotBlank
public @interface RoleExists
{
    //string for the error-message returned to the client when not able to validate
    String message() default "incorrect role";

    //array for allowing group-validation
    Class<?>[] groups() default { };

    //field for allowing different payloads of the violated constraint (f.i. Info or Error)
    Class<? extends Payload>[] payload() default { };


    class Validator implements ConstraintValidator<RoleExists, String>
    {
        @Override
        public void initialize(RoleExists constraintAnnotation)
        {

        }
        @Override
        public boolean isValid(String role, ConstraintValidatorContext context)
        {
            return Permissions.getRoleNames().contains(role);
        }
    }
}
