package com.beligum.blocks.core.validation;

import javax.validation.*;
import javax.validation.constraints.NotNull;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by bas on 25.07.14.
 * Constraint-annotation for checking whether or not the two passwords in the class ValidationUser are equal
 */
//Annotation saying this newly defined annotation can be placed on classes and interfaces
@Target(ElementType.TYPE)
//Annotation saying this newly defined annotation will be stored inside the .class-file by the compiler so it is available during runtime
@Retention(RetentionPolicy.RUNTIME)
//Annotation for declaring this a java-bean constraint. This contraint on a given class will be validated by the class 'validatedBy'
@Constraint(validatedBy = PasswordEquality.PasswordValidator.class)
@ReportAsSingleViolation
@NotNull
public @interface PasswordEquality
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    //string for the error-message returned to the client when not able to validate
    String message() default "passwords don't match";

    //array for allowing group-validation
    Class<?>[] groups() default { };

    //field for allowing different payloads of the violated constraint (f.i. Info or Error)
    Class<? extends Payload>[] payload() default { };

    /**
     * Sync the name of this field with com.beligum.core.framework.templating.AbstractTemplateContext.FORM_PARAM_ANNOTATION_FIELD
     *
     * @return specifies to which formParam this class-wide Validator should be attached
     */
    String formParam();

    //boolean for case sensitivity
    boolean caseSensitive() default true;

    //-----CONSTRUCTORS-----

    //-----PUBLIC METHODS-----

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

    /*
     * Used for validating the self-defined bean-validation-constraint-annotation PasswordEquality when placed on a ValidationUser
     */
    public class PasswordValidator implements ConstraintValidator<PasswordEquality, PasswordComparable>
    {
        private boolean caseSensitive;
        private String formParam;

        public void initialize(PasswordEquality pe)
        {
            //getting the caseSensitive-field from the annotation during initialization (default = true)
            this.caseSensitive = pe.caseSensitive();
            this.formParam = pe.formParam();
        }

        public boolean isValid(PasswordComparable user, ConstraintValidatorContext context)
        {
            boolean retVal = false;

            //if no user is specified, the constraint is considered false
            if (user != null) {
                //equal passwords?
                String pw1 = user.getClearTextPassword();
                String pw2 = user.getClearTextPasswordCheck();
                //if one of the two passwords is missing, the constraint is considered false
                if (pw1 != null && pw2 != null) {
                    if (!caseSensitive) {
                        pw1 = pw1.toLowerCase();
                        pw2 = pw2.toLowerCase();
                    }
                    else {
                        //do nothing
                    }
                    retVal = pw1.equals(pw2);
                }
            }

            return retVal;
        }
    }

}
