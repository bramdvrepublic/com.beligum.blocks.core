package com.beligum.blocks.core.endpoints;

import com.beligum.blocks.core.models.sql.Person;
import com.beligum.blocks.core.validation.LoginUser;
import com.beligum.core.framework.base.R;
import com.beligum.core.framework.base.RequestContext;
import com.beligum.core.framework.templating.ifaces.Template;
import com.beligum.core.framework.templating.ifaces.TemplateEngine;
import com.google.common.net.HttpHeaders;
import gen.com.beligum.blocks.core.endpoints.ApplicationEndpointRoutes;
import gen.com.beligum.blocks.core.endpoints.UsersEndpointRoutes;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.UsernamePasswordToken;

import javax.persistence.EntityManager;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.HashSet;
import java.util.List;

/**
 * Created by bas on 15.01.15.
 */
@Path("/users")
public class UsersEndpoint
{
    @GET
    @Path("login")
    @Produces(MediaType.TEXT_HTML)
    public Response getLogin() {
        return Response.ok().entity(R.templateEngine().getEmptyTemplate("/views/usermanagement/login.vm")).build();
    }

    @POST
    @Path("login")
    @Produces(MediaType.TEXT_HTML)
    //@RequiresPermissions("user:login")
    //TODO BAS: is this user validated?
    public Response login(
                    @BeanParam
                    LoginUser loginUser) {
        EntityManager em = RequestContext.getEntityManager();
        Person user = null;

        org.apache.shiro.subject.Subject currentSubject = SecurityUtils.getSubject();
        //this destroys the current session if one is active (otherwise it's not overwritten with eg. new role data)
        if (currentSubject.isAuthenticated()) {
            currentSubject.logout();
        }
        currentSubject.login(new UsernamePasswordToken(loginUser.username, loginUser.password, loginUser.rememberMe));
        Response retVal = null;
        URI referer = URI.create(RequestContext.getRequest().getHeader(HttpHeaders.REFERER));
        if (referer.getPath().equalsIgnoreCase(UsersEndpointRoutes.login().getPath())) {
            retVal = Response.seeOther(URI.create(ApplicationEndpointRoutes.index().getPath())).build();
        } else {
            retVal = Response.seeOther(referer).build();
        }
        return retVal;
    }

    @GET
    @Path("init")
    public Response initializeUsers(){
        //TODO BAS SH: make at least the admin-person (subject?) so that logging in becomes possible
        // Make sure 1 admin account exists
        List<Person> admins = RequestContext.getEntityManager().createQuery("SELECT p FROM Person p WHERE p..name = 'admin'", User.class)
                              .getResultList();

        if (admins.size() == 0) {
            User admin = new User();
            admin.setFirstName("Mr.");
            admin.setLastName("Admin");
            admin.setEmail("admin");

            UserCredentials credentials = new UserCredentials();
            Role adminRole = em.createQuery("Select r FROM Role r WHERE r.name = 'admin'", Role.class).getSingleResult();
            credentials.setLogin("admin");
            credentials.setPassword(R.configuration().getSecurityConfig().getPasswordService().encryptPassword("blgf0re"));

            credentials.setRoles(new HashSet<Role>());
            credentials.getRoles().add(adminRole);
            em.persist(credentials);
            admin.setCredentials(credentials);
            em.persist(admin);
        }

        return Response.ok("Roles and users created").build();
    }
}
