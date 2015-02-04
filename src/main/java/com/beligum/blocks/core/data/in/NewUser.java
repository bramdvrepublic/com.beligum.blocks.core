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
@PasswordEquality(formParam = "passwordCheck", message = "Please fill in the same password twice.", caseSensitive = true)
public class NewUser implements PasswordComparable
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    //a new user is active by default
    //TODO BAS SH: dit moet ook gekozen worden bij het aanmaken van ene nieuwe user
    //TODO BAS SH 2: daarna veilige password-functionaliteiten (=geen van server terugkerende paswoorden) bij een user-profile implementeren (naar een apparte site gaan met twee veldjes, en dan wordt het paswoord verandert zonder een confirmatie-email)
    public boolean active = true;

    @FormParam("firstName")
    @NotBlank
    public String firstName;

    @FormParam("lastName")
    @NotBlank
    public String lastName;

    @FormParam("email")
    @NotBlank
    @Email
    @EmailDoesNotExist
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
