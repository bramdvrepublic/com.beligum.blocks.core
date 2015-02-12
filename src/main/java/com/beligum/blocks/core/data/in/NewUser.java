package com.beligum.blocks.core.data.in;

import com.beligum.blocks.core.validation.EmailDoesNotExist;
import com.beligum.blocks.core.validation.PasswordComparable;
import com.beligum.blocks.core.validation.PasswordEquality;
import com.beligum.blocks.core.validation.RoleExists;
import org.hibernate.validator.constraints.Email;
import org.hibernate.validator.constraints.NotBlank;

import javax.inject.Named;
import javax.validation.constraints.Size;
import javax.ws.rs.FormParam;

/**
 * Created by bas on 28.01.15.
 */
@Named("newUser")
@PasswordEquality(formParam = "passwordEqualityCheck", message = "Please fill in the same password twice.", caseSensitive = true)
public class NewUser implements PasswordComparable
{
    //-----CONSTANTS-----
    @FormParam("firstName")
    @NotBlank
    public String firstName;

    @FormParam("lastName")
    @NotBlank
    public String lastName;

    @FormParam("email")
    @NotBlank
    @Email
    public String email;

    @FormParam("password")
    @NotBlank
    //TODO: remove maximum size (need to change database entry type of subject.password for longer possibilities)
    @Size(min = 6, max = 20)
    public String cleartextPassword;

    @FormParam("passwordCheck")
    @NotBlank
    @Size(min = 6, max = 20)
    public String cleartextPasswordCheck;

    @FormParam("role")
    @RoleExists
    public String role;

    @FormParam("active")
    public boolean active ;


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
    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}
