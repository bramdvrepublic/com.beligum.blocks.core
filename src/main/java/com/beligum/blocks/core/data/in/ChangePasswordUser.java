package com.beligum.blocks.core.data.in;

import com.beligum.blocks.core.models.sql.Person;
import com.beligum.blocks.core.validation.ExistingEntityId;
import com.beligum.blocks.core.validation.PasswordComparable;
import com.beligum.blocks.core.validation.PasswordEquality;
import org.hibernate.validator.constraints.Email;
import org.hibernate.validator.constraints.NotBlank;

import javax.inject.Named;
import javax.validation.constraints.Size;
import javax.ws.rs.FormParam;

/**
 * Created by bas on 04.02.15.
 */
@Named("changePasswordUser")
@PasswordEquality(formParam = "equality", message = "Please fill in the same password twice.", caseSensitive = true)
public class ChangePasswordUser implements PasswordComparable
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    @FormParam("id")
    @ExistingEntityId(entityClass = Person.class)
    public Long id;
    @FormParam("email")
    @NotBlank
    @Email(message = "email address not right")
    public String email;
    @FormParam("password")
    @NotBlank
    @Size(min = 6, max = 20)
    public String cleartextPassword;
    @FormParam("passwordCheck")
    @NotBlank
    @Size(min = 6, max = 20)
    public String cleartextPasswordCheck;


    //-----CONSTRUCTORS-----
    //-----PUBLIC METHODS-----
    @Override
    public String getClearTextPassword() {
        return cleartextPassword;
    }

    @Override
    public String getClearTextPasswordCheck() {
        return cleartextPasswordCheck;
    }
}
