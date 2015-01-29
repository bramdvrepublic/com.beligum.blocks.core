package com.beligum.blocks.core.data.in;


import com.beligum.blocks.core.validation.PasswordComparable;
import com.beligum.blocks.core.validation.PasswordEquality;
import org.hibernate.validator.constraints.Email;
import org.hibernate.validator.constraints.NotBlank;

import javax.inject.Named;
import javax.validation.constraints.Size;
import javax.ws.rs.FormParam;

/**
 * Created by bram on 10/13/14.
 */
@Named("forgotPasswordUserFinal")
@PasswordEquality(formParam = "equality", message = "Please fill in the same password twice.", caseSensitive = true)
public class ForgotPasswordUserFinal implements PasswordComparable
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
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
    @FormParam("confirmation")
    public String confirmation;
    @FormParam("personid")
    public Long personId;



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
