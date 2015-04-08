package com.beligum.blocks.data.in;

import com.beligum.blocks.models.account.Person;
import com.beligum.blocks.validation.ExistingEntityId;
import com.beligum.blocks.validation.PasswordComparable;
import com.beligum.blocks.validation.PasswordEquality;
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
