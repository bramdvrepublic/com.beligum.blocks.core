package com.beligum.blocks.core.validation;

import javax.ws.rs.FormParam;

/**
 * Created by bas on 27.01.15.
 */
public class LoginUser
{
    @FormParam("username")
    public String username;

    @FormParam("password")
    public String password;

    @FormParam("rememberMe")
    public boolean rememberMe;
}
