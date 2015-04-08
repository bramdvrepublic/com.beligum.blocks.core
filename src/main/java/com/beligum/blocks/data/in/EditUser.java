package com.beligum.blocks.data.in;

import com.beligum.blocks.validation.RoleExists;
import org.hibernate.validator.constraints.Email;
import org.hibernate.validator.constraints.NotBlank;

import javax.inject.Named;
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

    @FormParam("role")
    @RoleExists
    public String role;

    @FormParam("active")
    public boolean active;


    //-----CONSTRUCTORS-----
    //-----PUBLIC METHODS-----



    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}
