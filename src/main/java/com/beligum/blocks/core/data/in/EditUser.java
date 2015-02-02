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
public class EditUser
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

    @FormParam("password")
    @NotBlank
    @Size(min = 6, max = 20)
    public String cleartextPassword;

    @FormParam("passwordCheck")
    @NotBlank
    @Size(min = 6, max = 20)
    public String cleartextPasswordCheck;

    @FormParam("role")
    @RoleExists
    public String role;


    //-----CONSTRUCTORS-----
    //-----PUBLIC METHODS-----



    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}
