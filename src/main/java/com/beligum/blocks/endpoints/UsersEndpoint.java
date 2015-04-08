package com.beligum.blocks.endpoints;

import com.beligum.blocks.data.in.*;
import com.beligum.blocks.models.account.Person;
import com.beligum.blocks.models.account.Subject;
import com.beligum.blocks.usermanagement.Permissions;
import com.beligum.blocks.utils.Utils;
import com.beligum.blocks.validation.ExistingEntityId;
import com.beligum.blocks.validation.messages.CustomFeedbackMessage;
import com.beligum.base.server.R;
import com.beligum.base.server.RequestContext;
import com.beligum.base.email.EmailException;
import com.beligum.base.i18n.I18nFactory;
import com.beligum.base.models.AbstractSubject;
import com.beligum.base.security.DefaultCookiePrincipal;
import com.beligum.base.templating.ifaces.Template;
import com.beligum.base.utils.Logger;
import com.beligum.base.utils.toolkit.BasicFunctions;
import com.beligum.base.validation.messages.DefaultFeedbackMessage;
import com.beligum.base.validation.messages.FeedbackMessage;
import com.google.common.net.HttpHeaders;
import gen.com.beligum.blocks.core.endpoints.ApplicationEndpointRoutes;
import gen.com.beligum.blocks.core.endpoints.UsersEndpointRoutes;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.authz.UnauthorizedException;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.apache.shiro.authz.annotation.RequiresRoles;
import org.apache.shiro.authz.annotation.RequiresUser;

import javax.naming.AuthenticationException;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.util.*;

/**
 * Created by bas on 15.01.15.
 */
@Path("/users")
public class UsersEndpoint
{
    private static final String FIRST_NAME = "firstName";

    private static final String FORGOT_PASSWORD = "forgotpassword";
    private static final String CHANGE_EMAIL = "changeemail";
    private static final String FEEDBACK_MESSAGE = "feedbackMessage";

    @GET
    @RequiresRoles(Permissions.ADMIN_ROLE_NAME)
    public Response users(@QueryParam("sort") @DefaultValue(FIRST_NAME)
                          final String fieldName, @QueryParam("inactive") @DefaultValue("true") boolean showInactive) throws Exception
    {
        /*
         * Fetch users-template
         */
        Template template = R.templateEngine().getEmptyTemplate("/views/usermanagement/users-all.vm");
        List<Person> users = RequestContext.getEntityManager().createNamedQuery(Person.FIND_UNDELETED_PERSONS, Person.class).getResultList();

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
        template.set("showInactive", showInactive);
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
        EntityManager em = RequestContext.getEntityManager();
        Subject subject;
        Person person;
        /*
         * If this email has been used before, reuse the previous subject- and person-entity.
         * If not, create a new subject and person.
         */
        try{
            subject = (Subject) em.createNamedQuery(AbstractSubject.FIND_SUBJECT_BY_PRINCIPAL_QUERY_NAME, AbstractSubject.class)
                                  .setParameter(1, newUser.email)
                                  .getSingleResult();
            subject.setDeleted(false);
            subject.setActive(true);
        }catch(NoResultException e) {
            subject = new Subject();
        }
        try{
            person = em.createNamedQuery(Person.FIND_PERSON_BY_EMAIL, Person.class)
                       .setParameter(Person.QUERY_PARAMETER, newUser.email)
                       .getSingleResult();
            person.setDeleted(false);
        }catch (NoResultException e){
            person = new Person();
        }
        /*
         * Persist all other received fields
         */
        subject.setPassword(R.configuration().getSecurityConfig().getPasswordService().encryptPassword(newUser.cleartextPassword));
        subject.setRole(newUser.role);
        subject.setPrincipal(newUser.email);
        subject.setActive(newUser.active);
        person.setSubject(subject);
        person.setFirstName(newUser.firstName);
        person.setLastName(newUser.lastName);
        person.setEmail(newUser.email);
        em.merge(person);
        R.cacheManager().getFlashCache().addMessage(new DefaultFeedbackMessage(FeedbackMessage.Level.SUCCESS, "userCreated"));
        return Response.seeOther(URI.create(UsersEndpointRoutes.users(FIRST_NAME, true).getPath())).build();
    }

    @GET
    @Path("/login")
    @Produces(MediaType.TEXT_HTML)
    public Response getLogin() {
        return Response.ok(this.getLoginTemplate()).build();
    }
    /**
     *
     * @return a template holding all necessary (velocity-)variables to be rendered correctly
     */
    private Template getLoginTemplate(){
        //if ever the login-template changes, all velocity-variables need to be set in this method before returning the template
        return R.templateEngine().getEmptyTemplate("/views/usermanagement/login.vm");
    }

    @POST
    @Path("/login")
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
        try{
            currentSubject.login(new UsernamePasswordToken(loginUser.username, loginUser.password, loginUser.rememberMe));
            user = fetchCurrentUserFromCookie();
            if(!user.getSubject().getActive()){
                currentSubject.logout();
                throw new AuthenticationException("Cannot log in inactive user '" + user.getId() + "'.");
            }
        }catch (Exception e){
            Logger.error(e.getMessage(), e.getCause());
            Template template = this.getLoginTemplate();
            return Response.status(Response.Status.BAD_REQUEST).entity(template).build();
        }
        Response retVal = null;
        URI referer = URI.create(RequestContext.getJaxRsRequest().getHeaders().getFirst(HttpHeaders.REFERER));
        if (referer.getPath().equalsIgnoreCase(UsersEndpointRoutes.login().getPath())) {
            retVal = Response.seeOther(URI.create(ApplicationEndpointRoutes.getPageWithId("", null, false).getPath())).build();
        } else {
            retVal = Response.seeOther(referer).build();
        }
        return retVal;
    }

    @GET
    @Path("/logout")
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

        return Response.seeOther(URI.create(ApplicationEndpointRoutes.getPageWithId("", null, false).getPath())).build();
    }

    @GET
    @Path("/init")
    public Response initializeUsers(){
        // Make sure 1 admin account exists
        List<Subject> admins = RequestContext.getEntityManager().createQuery("SELECT s FROM Subject s WHERE s.role = 'admin'", Subject.class)
                                             .getResultList();
        if (admins.size() == 0) {
            Person admin = new Person();
            Subject subject = new Subject();
            subject.setRole(Permissions.ROLE_ADMIN.getRoleName());
            subject.setPrincipal("admin@beligum.com");
            subject.setPassword(R.configuration().getSecurityConfig().getPasswordService().encryptPassword("blgf0re"));
            admin.setSubject(subject);
            admin.setFirstName("Mr.");
            admin.setLastName("admin");
            admin.setEmail("admin@beligum.com");
            RequestContext.getEntityManager().persist(admin);
            return Response.ok("Admin initialized").build();
        }
        else{
            return Response.ok("An admin was already present in db.").build();
        }
    }




    //_____________________UPATE_USER_____________________//
    @GET
    @Path("/{userId}")
    @RequiresRoles(Permissions.ADMIN_ROLE_NAME)
    public Response getUser(@PathParam("userId") long userId) throws Exception
    {
        //will be true if the admin is editing himself
        boolean isCurrentUser = this.checkForCurrentUser(userId);
        return Response.ok(getUserTemplate(userId, isCurrentUser)).build();
    }
    @GET
    @Path("/profile/{userId}")
    @RequiresUser
    public Response getUserProfile(@PathParam("userId") long userId) throws Exception
    {
        boolean isCurrentUser = this.checkForCurrentUser(userId);
        if(!isCurrentUser){
            throw new UnauthorizedException("Admin is trying to access profile of other user.");
        }
        return Response.ok(getUserTemplate(userId, true)).build();
    }
    private Template getUserTemplate(long userId, boolean isCurrentUser)
    {
        Person userToBeEdited = RequestContext.getEntityManager().find(Person.class, userId);
        Template template = R.templateEngine().getEmptyTemplate("/views/usermanagement/users-edit.vm");
        template.set("editUser", userToBeEdited);
        template.set("roles", Permissions.getRoleNames());
        template.set("isProfile", isCurrentUser);
        template.set("isAdmin", this.fetchCurrentUserFromCookie().getSubject().getRole().equals(Permissions.ADMIN_ROLE_NAME));
        return template;
    }

    @POST
    @Path("/{userId}")
    @RequiresRoles(Permissions.ADMIN_ROLE_NAME)
    /**
     * Admin updating-method for users
     */
    public Response editUser(@PathParam("userId") @ExistingEntityId(entityClass = Person.class) long userId, @Valid @BeanParam
    EditUser editUser) throws Exception
    {
        return updateUser(userId, editUser, false, false);
    }
    @POST
    @Path("/profile/{userId}")
    @RequiresUser
    /**
     * Updating-method for current user-profile
     */
    public Response editUserProfile(@PathParam("userId") @ExistingEntityId(entityClass = Person.class) long userId, @Valid @BeanParam
    ProfileUser profileUser) throws Exception
    {
        this.checkForCurrentUser(userId);
        EditUser editUser = new EditUser();
        Utils.autowireDaoToModel(profileUser, editUser);
        Person persisted = RequestContext.getEntityManager().find(Person.class, userId);
        editUser.role = persisted.getSubject().getRole();
        editUser.active = persisted.getSubject().getActive();
        return updateUser(userId, editUser, true, true);
    }
    @GET
    @Path("/profile")
    @RequiresUser
    public Response getCurrentUserProfile(){
        DefaultCookiePrincipal principal = (DefaultCookiePrincipal) SecurityUtils.getSubject().getPrincipal();
        long currentUserId = principal.getId();
        return Response.seeOther(URI.create(UsersEndpointRoutes.getUserProfile(currentUserId).getPath())).build();
    }
    private Response updateUser(long userId, EditUser editUser, boolean needsConfirmation, boolean isProfile) throws Exception
    {
        EntityManager em = RequestContext.getEntityManager();
        Person persistedUser = em.find(Person.class, userId);
        String persistedEmailAddress = persistedUser.getEmail();
        if (!persistedEmailAddress.toLowerCase().trim().equals(editUser.email.trim().toLowerCase())) {
            List<Person> emailUser = RequestContext.getEntityManager()
                                                   .createQuery("SELECT p FROM Person p WHERE p.email = :email", Person.class)
                                                   .setParameter("email", editUser.email.trim().toLowerCase())
                                                   .getResultList();
            //if this email is already in use, do not save, and return error
            if(!emailUser.isEmpty()){
                R.cacheManager().getFlashCache().addMessage(new DefaultFeedbackMessage(FeedbackMessage.Level.ERROR, "emailAlreadyInUse"));
                if(isProfile){
                    return Response.seeOther(URI.create(UsersEndpointRoutes.getUserProfile(userId).getPath())).build();
                }
                else {
                    return Response.seeOther(URI.create(UsersEndpointRoutes.getUser(userId).getPath())).build();
                }
            }
            else {
                persistedUser.getSubject().setPrincipalReset(editUser.email);
                String confirmationString = BasicFunctions.createRandomString(32);
                persistedUser.getSubject().setConfirmation(confirmationString);
                if (needsConfirmation){
                    this.sendChangeEmailEmail(persistedUser);
                    R.cacheManager().getFlashCache().addMessage(new DefaultFeedbackMessage(FeedbackMessage.Level.INFO, "emailChanged"));
                }
                else{
                    this.changeEmailFinalConfirmation(userId, confirmationString, true);
                }
            }
        }
        persistedUser.getSubject().setActive(editUser.active);
        persistedUser.getSubject().setRole(editUser.role);
        Utils.autowireDaoToModel(editUser, persistedUser);
        if(needsConfirmation) {/*
         * Reset the email address to it's original state, since the autowire will have changed the email to the new value.
         * However, we do not want to permanently save the new email address yet.  We first need confirmation.
         */
            persistedUser.setEmail(persistedEmailAddress);
        }
        R.cacheManager().getFlashCache().addMessage(new DefaultFeedbackMessage(FeedbackMessage.Level.SUCCESS, "updateUser"));
        if(isProfile){
            return Response.seeOther(URI.create(UsersEndpointRoutes.getUserProfile(userId).getPath())).build();
        }
        else {
            return Response.seeOther(URI.create(UsersEndpointRoutes.getUser(userId).getPath())).build();
        }
    }

    /**
     *
     * @param userId the id of the user which should be the current user
     * @return true if the current user has the specified id, false if the current user is not the current user but has the admin-role
     * @throws org.apache.shiro.authz.UnauthorizedException if the specified user-id is not the same as off the current (logged in) user
     */
    private boolean checkForCurrentUser(long userId){
        try {
            //Note: probably only should use this method when @RequiresUser has been assessed
            DefaultCookiePrincipal cookiePrincipal = (DefaultCookiePrincipal) SecurityUtils.getSubject().getPrincipal();
            if (!cookiePrincipal.getId().equals(userId)) {
                if (!SecurityUtils.getSubject().hasRole(Permissions.ADMIN_ROLE_NAME)) {
                    throw new UnauthorizedException("User '" + userId + "' is not the current authenticated user. Currently authenticated is user '" +cookiePrincipal.getId() + "'.");
                }
                else {
                    return false;
                }
            }
            else {
                return true;
            }
        }catch (Exception e){
            throw new UnauthorizedException("Current user could not be checked for access to user information of user '" + userId +"'.", e);
        }
    }

    //___________________DELETE_USER____________________________
    @DELETE
    @Path("/{userId}")
    @Consumes
    @RequiresPermissions(Permissions.USER_DELETE)
    public Response deleteUser(@PathParam("userId") long userId){
        if(this.getCurrentUserId() == userId){
            Logger.warn("User '" + userId + "' cannot delete himself.");
            return Response.status(Response.Status.FORBIDDEN).entity(I18nFactory.instance().getDefaultResourceBundle().getMessage("selfDelete")).build();
        }
        EntityManager em = RequestContext.getEntityManager();
        Person user = em.find(Person.class, userId);
        user.setDeleted(true);
        user.getSubject().setActive(false);
        user.getSubject().setDeleted(true);
        em.merge(user);
        //return the redirect url
        return Response.ok(I18nFactory.instance().getDefaultResourceBundle().getMessage("userDeleted")).build();
    }



    //___________________CHANGE_EMAIL_CONFIRMATION___________________
    @GET
    @Path("changeemailfinal")
    public Response changeEmailFinalConfirmation(@ExistingEntityId(entityClass = Person.class) @QueryParam("u") Long personId,
                                                 @QueryParam("c") String confirmString, @QueryParam("conf") boolean confirmation){
        if(confirmation) {
            EntityManager em = RequestContext.getEntityManager();
            Template template = this.getLoginTemplate();
            Person person = em.find(Person.class, personId);
            // if userId is found
            // if confirmation string is correct, update person and update subject of person
            // logout principal if logged in
            if (confirmString.equals(person.getSubject().getConfirmation())) {
                String newEmail = person.getSubject().getPrincipalReset();
                person.getSubject().setPrincipal(newEmail);
                person.setEmail(newEmail);
                person.getSubject().setPrincipalReset(null);
                person.getSubject().setConfirmation(null);
                em.merge(person);
                R.cacheManager().getFlashCache().addMessage(new DefaultFeedbackMessage(FeedbackMessage.Level.SUCCESS, "emailChangedSuccess"));
                return Response.seeOther(URI.create(UsersEndpointRoutes.getLogin().getPath())).build();
            }
            else {
                R.cacheManager().getFlashCache().addMessage(new DefaultFeedbackMessage(FeedbackMessage.Level.ERROR, "emailConfirmationFailure"));
                return Response.seeOther(URI.create(UsersEndpointRoutes.getLogin().getPath())).build();
            }
        }
        else{
            return Response.seeOther(URI.create(ApplicationEndpointRoutes.getPageWithId("", null, false).getPath())).build();
        }
    }

    private boolean sendChangeEmailEmail(Person person) throws EmailException
    {
        try {
            Template emailTemplate = R.templateEngine().getEmptyTemplate("views/emails/changeemail.vm");
            emailTemplate.set("emailMessage",
                              //start email message
                              person.getFirstName() + " " + person.getLastName() + " has requested to change " +
                              "her or his emailaddress to this one. If this was you, click the link below."
                              //end email message
            );
            String link = UsersEndpointRoutes.getConfirmation(CHANGE_EMAIL, person.getId(), person.getSubject().getConfirmation()).getAbsoluteUrl();
            emailTemplate.set("resetAddress", link);
            R.emailManager().sendHtmlEmail("MOT.be - confirm your email", emailTemplate, person.getSubject().getPrincipalReset());
            return true;
        } catch (EmailException e) {
            Logger.error("email could not be sent: " + e);
            throw e;
        }
    }


    //____________________CHANGE_PASSWORD_AS_LOGGED_IN_USER_____________//
    @GET
    @Path("/changepassword/{userId}")
    @RequiresUser
    public Response getChangePassword(@PathParam("userId") @ExistingEntityId(entityClass = Person.class) long userId) {
        //will throw exception if the current user is not the one being accessed, or an administrator
        this.checkForCurrentUser(userId);
        Template template = R.templateEngine().getEmptyTemplate("/views/usermanagement/changePassword.vm");
        template.set("person", RequestContext.getEntityManager().find(Person.class, userId));
        return Response.ok(template).build();
    }

    @POST
    @Path("/changepassword/{userId}")
    @RequiresUser
    public Response changePassword(@PathParam("userId") long userId, @BeanParam @Valid
    ChangePasswordUser changePasswordUser) throws Exception
    {
        //will throw exception if the current user is not the one being accessed, or an administrator
        boolean isCurrentUser = this.checkForCurrentUser(userId);
        EntityManager em = RequestContext.getEntityManager();
        Person user = em.find(Person.class, userId);
        if(user.getEmail().equals(changePasswordUser.email)) {
            user.getSubject().setPassword(R.configuration().getSecurityConfig().getPasswordService().encryptPassword(changePasswordUser.cleartextPassword));
        }
        else{
            throw new Exception("Found wrong email while changing user password. This should not happen!");
        }
        em.merge(user);
        R.cacheManager().getFlashCache().addMessage(new DefaultFeedbackMessage(FeedbackMessage.Level.SUCCESS, "changedPassword"));
        if(isCurrentUser && !user.getSubject().getRole().equals(Permissions.ADMIN_ROLE_NAME)){
            return Response.seeOther(URI.create(UsersEndpointRoutes.getUserProfile(userId).getPath())).build();
        }
        else{
            return Response.seeOther(URI.create(UsersEndpointRoutes.getUser(userId).getPath())).build();
        }
    }



    //_____________________PASSWORD_RESET_________________//
    @GET
    @Path(FORGOT_PASSWORD)
    public Response getForgotPassword() {
        Template template = R.templateEngine().getEmptyTemplate("/views/usermanagement/forgotPassword.vm");
        return Response.ok(template).build();
    }


    @POST
    @Path(FORGOT_PASSWORD)
    public Response postForgotPassword(@Valid @BeanParam
                                           ForgotPasswordUser forgotPasswordUser) throws EmailException
    {
        EntityManager em = RequestContext.getEntityManager();
        //fakes sending email == success if user enters an email address that's not linked to a user
        Person person = em.createNamedQuery(Person.FIND_PERSON_BY_EMAIL, Person.class)
                          .setParameter(Person.QUERY_PARAMETER, forgotPasswordUser.email.toLowerCase().trim()).getSingleResult();
        if (person != null) {
            person.getSubject().setConfirmation(BasicFunctions.createRandomString(32));
            sendForgotPasswordEmail(person);
        }
        R.cacheManager().getFlashCache().addMessage(new DefaultFeedbackMessage(FeedbackMessage.Level.SUCCESS, FORGOT_PASSWORD));
        return Response.seeOther(URI.create(UsersEndpointRoutes.getForgotPassword().getPath())).build();
    }

    private boolean sendForgotPasswordEmail(Person person) throws EmailException {
        try {
            Template emailTemplate = R.templateEngine().getEmptyTemplate("views/emails/forgotpasswordemail.vm");
            emailTemplate.set("emailMessage",
                              //start email message
                              "Someone has used this email to request a password reset on www.mot.be<br/>" +
                              "If this was you, please click the link below to confirm the reset."
                              //end email message
            );
            String link = UsersEndpointRoutes.getConfirmation(FORGOT_PASSWORD, person.getId(), person.getSubject().getConfirmation()).getAbsoluteUrl();
            emailTemplate.set("link", link);
            R.emailManager().sendHtmlEmail("MOT.be - confirm your email", emailTemplate, person.getSubject().getPrincipal());
            return true;
        } catch (Exception e) {
            throw new EmailException();
        }
    }

    @POST
    @Path("forgotpasswordfinal")
    public Response postForgotPasswordFinal(@Valid @BeanParam
                                            ForgotPasswordUserFinal forgotPasswordUserFinal) throws Exception {
        EntityManager em = RequestContext.getEntityManager();
        Person person = em.createNamedQuery(Person.FIND_PERSON_BY_EMAIL, Person.class)
                          .setParameter(Person.QUERY_PARAMETER, forgotPasswordUserFinal.email.toLowerCase().trim()).getSingleResult();
        if (person != null) {
            if (person.getId().equals(forgotPasswordUserFinal.id) && person.getSubject().getConfirmation().equals(forgotPasswordUserFinal.confirmation)) {
                person.getSubject().setPassword(R.configuration().getSecurityConfig().getPasswordService().encryptPassword(forgotPasswordUserFinal.cleartextPassword));
            }
            person.getSubject().setConfirmation(null);
        }
        R.cacheManager().getFlashCache().addMessage(new DefaultFeedbackMessage(FeedbackMessage.Level.SUCCESS, "forgotPasswordFinal"));
        return Response.ok(URI.create(UsersEndpointRoutes.getLogin().getPath())).build();
    }

    @GET
    @Path("confirm")
    public Response getConfirmation(@QueryParam("action") String action, @ExistingEntityId(entityClass = Person.class) @QueryParam("u") Long personId,
                                    @QueryParam("c") String confirmString) throws Exception {
        EntityManager em = RequestContext.getEntityManager();
        Template template;
        Person person = em.find(Person.class, personId);
        //logout subject before continuing
        SecurityUtils.getSubject().logout();


        /*
         * Cannot use a DefaultFeedbackMessage here with a redirect, since some e-mail clients receive the redirect,
         * and then send the redirected url to a browser, which then requests that url for a second time
         * (where the feedback-message will no longer be present in the template-context).
         */

        //action for changing email address
        if (action.equals(CHANGE_EMAIL)) {
            template = R.templateEngine().getEmptyTemplate("/views/usermanagement/changeEmailFinal.vm");
            template.set("query", "u=" + personId + "&c=" + confirmString);
            template.set("oldEmail", person.getEmail());
            template.set("newEmail", person.getSubject().getPrincipalReset());
        }
        //action for forgotten password
        else if (action.equals(FORGOT_PASSWORD)) {
            // if userId is found
            // if confirmation string is correct, update person and update subject of person
            if (confirmString.equals(person.getSubject().getConfirmation())) {
                template = R.templateEngine().getEmptyTemplate("/views/usermanagement/forgotPasswordFinal.vm");
                template.set("person", person);
                template.set(FEEDBACK_MESSAGE, new CustomFeedbackMessage("forgotPasswordSuccess", FeedbackMessage.Level.SUCCESS));
            } else {
                template = R.templateEngine().getEmptyTemplate("/views/usermanagement/forgotPasswordFailure.vm");
                template.set(FEEDBACK_MESSAGE, new CustomFeedbackMessage("emailConfirmationFailure", FeedbackMessage.Level.ERROR));
            }
        } else {
            throw new Exception("Unsupported confirmation-action found: " + action);
        }
        return Response.ok(template).build();
    }

    public Person fetchCurrentUserFromCookie()
    {
        try {
            EntityManager em = RequestContext.getEntityManager();
            return em.find(Person.class, getCurrentUserId());
        }catch (ClassCastException e){
            throw new RuntimeException("Found unsupported cookieprincipal of class '" + SecurityUtils.getSubject().getPrincipal().getClass().getName() + "'.");
        }
    }

    public long getCurrentUserId(){
        DefaultCookiePrincipal principal = (DefaultCookiePrincipal) SecurityUtils.getSubject().getPrincipal();
        return principal.getId();
    }
}
