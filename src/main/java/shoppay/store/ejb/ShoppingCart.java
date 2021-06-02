package shoppay.store.ejb;

import shoppayentity.entity.*;
import shoppay.store.qualifiers.LoggedIn;
import shoppay.store.web.util.JsfUtil;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.enterprise.context.Conversation;
import javax.enterprise.context.ConversationScoped;
import javax.inject.Inject;
import javax.inject.Named;

@Named(value = "shoppingCart")
@ConversationScoped
public class ShoppingCart implements Serializable {

    private static final long serialVersionUID = 3313992336071349028L;
    
    @Inject
    Conversation conversation;
    
    @EJB
    OrderBean facade;
    
    
    @Inject//MUST
    @LoggedIn//MUST, after test set in comments- EXIT/START NB 
    Person user;
    
    
    private PersonDetails personDetails;

    

    
    
    private static final Logger LOGGER = Logger.getLogger(ShoppingCart.class.getCanonicalName());
    
    private List<Product> cartItems;
    
    @EJB
    EventDispatcherBean eventDispatcher;
    
    
    private boolean cleared = false;
    
    void setCleared(boolean b){
        cleared = b;
    }
    
    public boolean getCleared(){
        return cleared;
    }

    
    /**
     * 
     */
    public void init() {
        cartItems = new ArrayList<>();
    }

    public String addItem(final Product p) {
        if (cartItems == null) {
            cartItems = new ArrayList<>();
            if (conversation.isTransient()) {
                conversation.begin();
            }
        }
        // Makes no sense, why should not buy a customer more than one
        // of the same product.
//        if (!cartItems.contains(p)) {
            cartItems.add(p);
        return "";
    }

    public boolean removeItem(Product p) {
        if (cartItems.contains(p)) {
            return cartItems.remove(p);
        } else {
            // no items removed
            return false;
        }
    }

    public BigDecimal getTotal() {
        if (cartItems == null || cartItems.isEmpty()) {
            return BigDecimal.ZERO;
        }
        
        BigDecimal total = BigDecimal.ZERO;
        for (Product item : cartItems) {
            total = total.add(item.getPrice());
        }
        LOGGER.log(Level.FINEST, "Actual Total:{0}", total);
        return total;
    }
    
    public String checkout() {
        if (user == null) {
            JsfUtil.addErrorMessage(
                    JsfUtil.getStringFromBundle("Bundle", 
                            "LoginBeforeCheckout"));
            return "/index.xhtml";
        } 
        
        for(PersonDetails p : this.user.getPersonDetailsCollection()){
            this.personDetails = p;
            break;
        }
        
        
        return "/order/Create.xhtml";
    }

    public void clear() {
        cartItems.clear();
    }
    
    
    public String clearGoBack() {
        cartItems.clear();
        return "/index.html";
    }

    public List<Product> getCartItems() {
        return cartItems;
    }

    public Conversation getConversation() {
        return conversation;
    }
    
    public String getUserEmail(){
        return user.getEmail();
    }
    
    
    public String getDate(){
        return Calendar.getInstance().getTime().toString();
    }
    
    
    public Person getUser(){
        
        System.out.println("ShoppingCard, getUser, user: " + this.user);
        
        return user;
    }
    
    public void setPerson(Person p){
        user = p;
    }
    
    
    public int getItemsCount(){
        System.out.println("ShoppingCart, getItems, cartItems: "
                + this.cartItems);
        int ret = 0;
        if(this.cartItems != null){
            ret = this.cartItems.size();
        }
        
        return ret;
    }
    
    /**
     * Get the value of personDetails
     *
     * @return the value of personDetails
     */
    public PersonDetails getPersonDetails() {
        return personDetails;
    }

    /**
     * Set the value of personDetails
     *
     * @param personDetails new value of personDetails
     */
    public void setPersonDetails(PersonDetails personDetails) {
        this.personDetails = personDetails;
    }
    
}