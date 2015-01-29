package com.beligum.blocks.core.data.in;

import org.hibernate.validator.constraints.Email;
import org.hibernate.validator.constraints.NotBlank;

import javax.inject.Named;
import javax.ws.rs.FormParam;

/**
 * Created by bram on 10/13/14.
 */
@Named("forgotPasswordUser")
public class ForgotPasswordUser
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    @FormParam("email")
    @NotBlank
    @Email(message = "Incorrect e-mail address")
    public String email;

    //-----CONSTRUCTORS-----
    //-----PUBLIC METHODS-----

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}
