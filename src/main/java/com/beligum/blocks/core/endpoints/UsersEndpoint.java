package com.beligum.blocks.core.endpoints;

import com.beligum.blocks.core.models.sql.Person;
import com.beligum.blocks.core.models.sql.Subject;
import com.beligum.blocks.core.repositories.PersonRepository;
import com.beligum.blocks.core.usermanagement.Permissions;
import com.beligum.blocks.core.data.in.LoginUser;
import com.beligum.blocks.core.data.in.NewUser;
import com.beligum.core.framework.base.R;
import com.beligum.core.framework.base.RequestContext;
import com.beligum.core.framework.templating.ifaces.Template;
import com.google.common.net.HttpHeaders;
import gen.com.beligum.blocks.core.endpoints.ApplicationEndpointRoutes;
import gen.com.beligum.blocks.core.endpoints.UsersEndpointRoutes;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.apache.shiro.authz.annotation.RequiresRoles;

import javax.persistence.EntityManager;
import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by bas on 15.01.15.
 */
@Path("/users")
public class UsersEndpoint
{
    private static final String FIRST_NAME = "firstName";

    @GET
    @RequiresRoles(Permissions.ADMIN_ROLE_NAME)
    public Response users(@QueryParam("sort") @DefaultValue(FIRST_NAME)
                          final String fieldName) throws Exception
    {
        /*
         * Fetch users-template
         */
        Template template = R.templateEngine().getEmptyTemplate("/views/usermanagement/users-all.vm");
        List<Person> users = PersonRepository.getAllPersons();

        /*
         * Sort the users using the specified field-name.
         * This uses java-reflection.
         * First check if the specified field exists
         */
        Field[] fields = Person.class.getDeclaredFields();
        boolean foundField = false;
        int i = 0;
        while(!foundField && i<fields.length){
            foundField = fields[i].getName().equals(fieldName);
            i++;
        }
        if(!foundField){
            throw new Exception("Cannot sort by '" + fieldName + "'. No such field in " + Person.class.getName() + ".class.");
        }
        final Field toBeCompared = fields[i-1];
        Collections.sort(users, new Comparator<Person>()
        {
            @Override
            public int compare(Person p1, Person p2)
            {
                int compared;
                try {
                    Comparable field1 = (Comparable) PropertyUtils.getProperty(p1, fieldName);
                    Comparable field2 = (Comparable) PropertyUtils.getProperty(p2, fieldName);
                    if (field1 == null) {
                        compared = 1;
                    }
                    else if (field2 == null) {
                        compared = -1;
                    }
                    else {
                        if(toBeCompared.getType().equals(String.class)) {
                            String string1 = (String) field1;
                            String string2 = (String) field2;
                            compared = string1.compareToIgnoreCase(string2);
                        }
                        else {
                            compared = field1.compareTo(field2);
                        }
                    }
                }
                catch(ClassCastException e){
                    throw new RuntimeException("Cannot sort by '" + fieldName + "', since it is not a comparable field.");
                }
                catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
                    throw new RuntimeException("Cannot sort by '" + fieldName + "', since it is not an accessible field.");
                }
                return compared;
            }
        });

        /*
         * Add users-list and return
         */
        template.set("users", users);
        template.set("sortedBy", fieldName);
        return Response.ok(template).build();
    }

    @GET
    @Path("/new")
    @RequiresRoles(Permissions.ADMIN_ROLE_NAME)
    public Response newUser(){
        Template template = R.templateEngine().getEmptyTemplate("/views/usermanagement/users-new.vm");
        template.set("roles", Permissions.getRoleNames());
        return Response.ok(template).build();
    }

    @POST
    @RequiresPermissions(Permissions.USER_CREATE)
    public Response storeNewUser(@BeanParam @Valid NewUser newUser){
        Subject subject = new Subject();
        subject.setPassword(R.configuration().getSecurityConfig().getPasswordService().encryptPassword(newUser.cleartextPassword));
        subject.setRole(newUser.role);
        Person person = new Person();
        person.setSubject(subject);
        person.setFirstName(newUser.firstName);
        person.setLastName(newUser.lastName);
        person.setEmail(newUser.email);
        RequestContext.getEntityManager().persist(person);
        return Response.seeOther(URI.create(UsersEndpointRoutes.users(FIRST_NAME).getPath())).build();
    }

    @GET
    @Path("login")
    @Produces(MediaType.TEXT_HTML)
    //TODO BAS: Redirect after UnauthorizedException to this method?
    public Response getLogin() {
        return Response.ok().entity(R.templateEngine().getEmptyTemplate("/views/usermanagement/login.vm")).build();
    }

    @POST
    @Path("login")
    @Produces(MediaType.TEXT_HTML)
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
    @Path("logout")
    public Response logout() {
        /*
         * Calling this method in web environments will usually remove any associated session cookie as part of session invalidation.
         * Because cookies are part of the HTTP header, and headers can only be set before the response body (html, image, etc) is sent,
         * this method in web environments must be called before any content has been rendered.
         *
         * The typical approach most applications use in this scenario is to redirect the user to a different location (e.g. home page) immediately after calling this method.
         * This is an effect of the HTTP protocol itself and not a reflection of Shiro's implementation.
         */
        SecurityUtils.getSubject().logout();

        return Response.seeOther(URI.create(ApplicationEndpointRoutes.index().getPath())).build();
    }

    @GET
    @Path("init")
    public Response initializeUsers(){
        // Make sure 1 admin account exists
        List<Subject> admins = RequestContext.getEntityManager().createQuery("SELECT s FROM Subject s WHERE s.role = 'admin'", Subject.class)
                                             .getResultList();
        if (admins.size() == 0) {
            Person admin = new Person();
            Subject subject = new Subject();
            subject.setRole(Permissions.ROLE_ADMIN.getRoleName());
            subject.setPrincipal("admin");
            subject.setPassword(R.configuration().getSecurityConfig().getPasswordService().encryptPassword("blgf0re"));
            admin.setSubject(subject);
            admin.setFirstName("Mr.");
            admin.setLastName("admin");
            RequestContext.getEntityManager().persist(admin);
            return Response.ok("Admin initialized").build();
        }
        else{
            return Response.ok("An admin was already present in db.").build();
        }
    }

}
