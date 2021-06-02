package shoppay.store.web;

import java.io.Serializable;
import java.util.Map;
import javax.annotation.security.DeclareRoles;
import javax.annotation.security.RolesAllowed;
import javax.enterprise.inject.Produces;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.annotation.HttpConstraint;
import javax.servlet.annotation.ServletSecurity;
import javax.servlet.annotation.ServletSecurity.TransportGuarantee;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import shoppay.store.ejb.UserBean;
import shoppayentity.entity.*;
import shoppay.store.ejb.PersonDetailsBean;
import shoppay.store.ejb.ShoppingCart;
import shoppay.store.qualifiers.LoggedIn;


/**
 * 
 * The class cares for login and logout.
 * 
 * @author stefan.streifeneder@gmx.de
 * 
 */
@ManagedBean
@SessionScoped
@DeclareRoles({"admin, user"})
@RolesAllowed({"admin, user"})
@ServletSecurity(
 @HttpConstraint(transportGuarantee = TransportGuarantee.CONFIDENTIAL,
    rolesAllowed = {"admin, user"}))
public class UserController implements Serializable{    
    
    private static final long serialVersionUID = 3254181235309041386L;    
    
    @Inject
    private UserBean requestBean;
    
    /**
     * Property of the class.
     */
    private String email = "email@address.com";
    
    /**
     * Property of the class.
     */
    private String password = "password";
    
    /**
     * Property of the class.
     */
    private Person user;
    
    
    
    private PersonDetails personDetails;

   

    
    @Inject
    CustomerController customerController;
    
    
    @Inject
    ShoppingCart shoppingCart;
    

    
    @RolesAllowed({"admin, user"})
    @LoggedIn
    public String login(){
        FacesContext context = FacesContext.getCurrentInstance();
        HttpServletRequest request = 
                (HttpServletRequest) context.getExternalContext().getRequest();
        
        System.out.println("UserController, login, email: "
                        + email + " - password: " + password);

        try{
            request.login(email, password);
        } catch (ServletException e) {
                context.addMessage(null, 
                        new FacesMessage(FacesMessage.SEVERITY_ERROR, 
                                "Login failed!", null));
                return "/loginTest.xhtml";
        }
        
        System.out.println("UserController, after login: "
                + "\n - bean: " + this.requestBean
                + "\n - email: " + email
                + "\n - isUserInRol('admin'): " + request.isUserInRole("admin")
                + "\n - isUserInRol('name'): " + request.isUserInRole("user")
                        );
        
        Person p = 
                (Person)this.requestBean.getPersonByRequest(email);

        String s = p.getDtype();

        if(s.equals("admin")){
            user = (Administrator)this.requestBean.getPersonByRequest(email);
        }else if(s.equals("user")){
            user = (Customer)this.requestBean.getPersonByRequest(email);          
        }
        
        //old
//        this.customerController.setAuthenticated(user);        

        ExternalContext externalContext = 
                FacesContext.getCurrentInstance().getExternalContext();                
            Map<String, Object> sessionMap = externalContext.getSessionMap();  

        if(user.getDtype().equals("admin")){    
            
            sessionMap.put("admin", (Administrator)user);
            
            //new
            this.customerController.setAuthenticated(user);

            System.out.println("UserController, ADMIN login, "
                     + "\nisUserInRol('admin'): "            + request.isUserInRole("admin")
                     + "\nisUserInRol('user'): "         + request.isUserInRole("user")
                     + "\ngetUserPrincipal(): "             + request.getUserPrincipal());       

            return "/admin/index";

        }else if(user.getDtype().equals("user")){
            
            //new
            this.customerController.setAuthenticated(user);
            
            sessionMap.put("user", (Customer)user);

            System.out.println("UserController, login, "
                     + "\nisUserInRol('student'): "         + request.isUserInRole("user")
                     + "\ngetUserPrincipal(): "             + request.getUserPrincipal());
            
            return "/index";

        }
        return "/admin/index";
    }
        
        
    public String logout() {
        System.out.println("UserController, logout");
        FacesContext context = FacesContext.getCurrentInstance();
        HttpServletRequest request = 
                (HttpServletRequest) context.getExternalContext().getRequest();
        try {
                this.user = null;
                request.logout();
                // clear the session
                ((HttpSession) context.getExternalContext().getSession(false)).invalidate();
        } catch (ServletException e) {
            System.out.println("UserController, logout, Exc: " + e.getMessage());
        }
        
        System.out.println("UserController, logout - END");
        return "/loginTest.xhtml";
    }

    @Produces
    @LoggedIn
    public Person getAuthenticatedUser() {
            return user;
    }
    
    
    public boolean isLoggedIn(){
        if(user != null){
            if(!user.getDtype().equals("admin")){
                if(this.shoppingCart != null){
                    if(this.shoppingCart.getCartItems() != null){
                        if(!this.shoppingCart.getCartItems().isEmpty()){
                            if(this.shoppingCart.getUser() == null){

                        System.out.println("UserController, isLoggedIn, user: " 
                        + user
                        + "\nshoppingCart - items: " + this.shoppingCart
                        + "\nEND is loggedIn"); 
                                this.shoppingCart.setPerson(user);
                            }
                        }

                    }
                }
            }
        }
        return user != null;
    }
    
    public String getEmail() {
            return email;
    }
    
    public void setEmail(String email) {
            this.email = email;
    }
    
     public String getPassword() {
            return password;
    }
    
    public void setPassword(String password) {
            this.password = password;
    }
    

    public boolean isAdmin() {
        if(user.getDtype().equals("admin")){
            return true;
        }
        return false;
    }
    
    
    public String goAdmin() {
        if (isAdmin()) {
            return "/admin/index";
        } else {
            return "index";
        }
    }
    
    public Person getUser() {
        System.out.println("UserController, getUser, user: " 
                + user); 
        
        for (PersonDetails pd : user.getPersonDetailsCollection()){
            this.personDetails = pd;
            break;
        }
        
        System.out.println("UserController, getUser, personDetails: " 
                + this.personDetails); 
        
        return user;
    }
    
    
    public String goToEditCustomer(Person p){
        
        System.out.println("UserController, goToEditCustomer, customerController: " 
                + this.customerController
                + "\nperson: " + p); 
        
        
        this.customerController.setAuthenticated(p);
        this.customerController.setCurrentCustomer((Customer)p);        
        return "/customer/Edit.xhtml";
    }
    
    
     /**
     * Get the value of currentPersonDetails
     *
     * @return the value of currentPersonDetails
     */
    public PersonDetails getPersonDetails() {
        
        System.out.println("UserController, getPersonDetails: "
                + this.personDetails);
        if(this.personDetails == null){
            for (PersonDetails pd : user.getPersonDetailsCollection()){
                this.personDetails = pd;
                break;
            }
        }
        
        return personDetails;
    }

    /**
     * Set the value of currentPersonDetails
     *
     * @param personDetails new value of currentPersonDetails
     */
    public void setPersonDetails(PersonDetails personDetails) {
        this.personDetails = personDetails;
    }
   
}