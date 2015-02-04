package com.beligum.blocks.core.data.in;

import com.beligum.blocks.core.validation.RoleExists;
import org.hibernate.validator.constraints.Email;
import org.hibernate.validator.constraints.NotBlank;

import javax.inject.Named;
import javax.validation.constraints.Size;
import javax.ws.rs.FormParam;

/**
 * Created by bramdeveirman on 13/10/14.
 */
@Named("editUser")
public class ProfileUser
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
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


    //-----CONSTRUCTORS-----

    //-----PUBLIC METHODS-----

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}
