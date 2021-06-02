package shoppay.store.web;

import shoppay.store.ejb.UserBean;
import shoppayentity.entity.*;
import shoppay.store.ejb.GroupsBean;
import shoppay.store.ejb.PersonGroupsBean;
import shoppay.store.qualifiers.LoggedIn;
import shoppay.store.web.util.*;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.enterprise.inject.Produces;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;
import javax.faces.model.DataModel;
import javax.faces.model.ListDataModel;
import javax.faces.model.SelectItem;
import javax.inject.Named;
import javax.inject.Inject;
import shoppay.store.ejb.PersonDetailsBean;


@Named(value = "customerController")
@SessionScoped
public class CustomerController implements Serializable {
    
    private static final long serialVersionUID = 2081269066939259737L;
    
    // orig
//    @Inject//Causes: WELD-001408: Unsatisfied dependencies for type Person with qualifiers @LoggedIn
//    @LoggedIn
    // Annotions do not run due to the necessary design.
    Person authenticated;
    
    
    private Customer currentCustomer;
    
    private DataModel<Customer> items = null;
    
    @EJB
    private UserBean ejbFacade;
    
    
    @Inject
    private GroupsBean groupsBean;
    
    
    @Inject
    private PersonGroupsBean personGroupsBean;
    
    @Inject
    private PersonDetailsBean personDetailsBean;
    
    private PersonDetails currentPersonDetails;

    

    
    
    
                
    private static final Logger logger = Logger.getLogger(CustomerController.class.getCanonicalName());
    
    private AbstractPaginationHelper pagination;
    
    private int selectedItemIndex;
    
    private static final String BUNDLE = "Bundle";
    
    
    /**
     * Noarg Constructor
     */
    public CustomerController() {
//        System.out.println("CustomerController, NoArg-Constructor, "
//                + "authenticated: " + this.authenticated
//                + "\ncurrent-customer: " + this.currentCustomer
//                + "\nuserBean: " + this.ejbFacade);
    }

    public Customer getSelected() {
        if (currentCustomer == null) {
            currentCustomer = new Customer();
            selectedItemIndex = -1;
        }
        return currentCustomer;
    }
    

    private UserBean getFacade() {
        return ejbFacade;
    }

    public AbstractPaginationHelper getPagination() {
        if (pagination == null) {
            pagination = new AbstractPaginationHelper(AbstractPaginationHelper.DEFAULT_SIZE) {

                @Override
                public int getItemsCount() {
                    return getFacade().count();
                }

                @Override
                public DataModel createPageDataModel() {
                    return new ListDataModel(getFacade().findRange(new int[]{getPageFirstItem(), 
                        getPageFirstItem() + getPageSize()}));
                    //return new ListDataModel(getFacade().findAll());
                }
            };
        }
        return pagination;
    }

    public PageNavigation prepareList() {
        recreateModel();
        return PageNavigation.LIST;
    }

    public PageNavigation prepareView() {
        currentCustomer     = (Customer) getItems().getRowData();
        selectedItemIndex = pagination.getPageFirstItem() + getItems().getRowIndex();
        
        System.out.println("CustomerController, prepareView, person: "
                + this.currentCustomer);
        
        if(this.currentPersonDetails == null){
            for (PersonDetails pd : this.currentCustomer.getPersonDetailsCollection()){
                this.currentPersonDetails = pd;
                
                System.out.println("CustomerController, prepareView, "
                        + "personDetails: "
                    + this.currentPersonDetails);
                
                
                break;
            }
        }    
        
        return PageNavigation.VIEW;
    }

    public PageNavigation prepareCreate() {
        currentCustomer = new Customer();
        this.currentPersonDetails = new PersonDetails();
        selectedItemIndex = -1;
        return PageNavigation.CREATE;
    }

    
    /**
     * Returns true, if the requested email is already in use.
     * 
     * @param p The Person object, which should be stored.
     * 
     * @return boolean True, if the requested email is already in use.
     */
    private boolean isUserDuplicated(Person p) {
        try{
            if(getFacade().getPersonByRequest(p.getEmail()) == null){
                return false;
            }             
        }catch(Exception e){
            JsfUtil.addErrorMessage(
                    ResourceBundle.getBundle(BUNDLE).getString(
                            "DuplicatedCustomerError"));
            return false;
        }
        return true;
    }

    
//    public PageNavigation create() {
    public String create() {
        System.out.println("CustomerController, create, START, person: "
                + this.currentCustomer.getEmail()
                + "\np-details: " + this.currentPersonDetails);
        
        
        
        
        
        try {
            if (!isUserDuplicated(currentCustomer)) {
                
                
                // password encrypt
                this.currentCustomer.setPassword(AuthenticationUtils.encodeSHA256(this.currentCustomer.getPassword()));
                
                //dtype
                this.currentCustomer.setDtype("user");
                
                //id
                int recNo = this.createIdCustomer();
                this.currentCustomer.setIdPerson(recNo);
                
                //List<CustomerOrder>
                this.currentCustomer.setCustomerOrderCollection(new ArrayList<CustomerOrder>());
                
                
                this.getFacade().create(currentCustomer);
                
                
                
                // PersonDetails
                if(this.currentPersonDetails == null){
                    this.currentPersonDetails = new PersonDetails();
                }
                this.currentPersonDetails.setIdPersonDetails(recNo);
                this.currentPersonDetails.setPerson(this.currentCustomer);                
                this.personDetailsBean.create(this.currentPersonDetails);
                
                
                
                // Add PersonDetails to Customer
                this.currentCustomer.setPersonDetailsCollection(new ArrayList<PersonDetails>());
                this.currentCustomer.getPersonDetailsCollection().add(this.currentPersonDetails);
                
                
                //Person_Groups
                // id - can be the same like the id of the person, because
                // each person is stored in the table 'PERSON_GROUPS'
                // therefore it has to follow the same scheme like 'PERSON'
                PersonGroups pg = new PersonGroups();
                pg.setIdPersonGroups(this.personGroupsBean.getIdNewPersonGroups());
                pg.setEmail(this.currentCustomer.getEmail());
                pg.setPerson(this.currentCustomer);
                try{
                    this.personGroupsBean.create(pg);
                }catch(Exception e){
                    System.out.println("CustomerController, create, EXC: "
                    + e.getMessage());
                }
                
                // There are only two GRPOUPS (admin, user).
                Groups g = this.groupsBean.find(2);// '2' because it is a USER
                pg.setGroups(g);
                g.getPersonGroupsCollection().add(pg);               
                this.groupsBean.edit(g);
                this.personGroupsBean.edit(pg);
                
                this.getFacade().edit(currentCustomer);
                JsfUtil.addSuccessMessage(ResourceBundle.getBundle(BUNDLE).getString("CustomerCreated"));

            } else {
                JsfUtil.addErrorMessage(ResourceBundle.getBundle(BUNDLE).getString("DuplicatedCustomerError"));

            }         
            
            if(this.authenticated != null){
                
                this.items.setWrappedData(this.ejbFacade.findAll());
                
                return "/admin/customer/List.xhtml";
            }else{
                return "/index.xhtml";
            }
        } catch (UnsupportedEncodingException | NoSuchAlgorithmException e) {
            JsfUtil.addErrorMessage(e, 
                ResourceBundle.getBundle(BUNDLE).getString(
                        "CustomerCreationError"));
            return null;
        }
        
//        return null;
    } 
    

    public PageNavigation prepareEdit() {
        currentCustomer = getItems().getRowData();
        selectedItemIndex = pagination.getPageFirstItem() + getItems().getRowIndex();        
        
        System.out.println("CustomerController, prepareEdit, person: "
                + this.currentCustomer);
        
        if(this.currentPersonDetails == null){
            for (PersonDetails pd : this.currentCustomer.getPersonDetailsCollection()){
                this.currentPersonDetails = pd;
                break;
            }
        }       
        
        return PageNavigation.EDIT;
    }
    
    

    public PageNavigation update() {
        try {
            logger.log(Level.INFO, "Updating customer ID:{0}", currentCustomer.getIdPerson());
           getFacade().edit(currentCustomer);
            JsfUtil.addSuccessMessage(ResourceBundle.getBundle(BUNDLE).getString("CustomerUpdated"));
            return PageNavigation.VIEW;
        } catch (Exception e) {
            JsfUtil.addErrorMessage(e, ResourceBundle.getBundle(BUNDLE).getString("PersistenceErrorOccured"));
            return null;
        }
    }
    

    public PageNavigation editByCustomer() {
        try {
            logger.log(Level.INFO, "Updating customer ID:{0}", currentCustomer.getIdPerson());
            getFacade().edit(currentCustomer);
            JsfUtil.addSuccessMessage(ResourceBundle.getBundle(BUNDLE).getString("CustomerUpdated"));
            return PageNavigation.VIEW;
        } catch (Exception e) {
            JsfUtil.addErrorMessage(e, ResourceBundle.getBundle(BUNDLE).getString("PersistenceErrorOccured"));
            return null;
        }
    }
    
    

    public PageNavigation destroy() {
        
        currentCustomer = (Customer) getItems().getRowData();        
        
        // To ensure you have the most actual version of that cutomer
        currentCustomer = (Customer)this.ejbFacade.getPersonByRequest(
                this.currentCustomer.getEmail());
        
        
        if(this.currentCustomer.getCustomerOrderCollection() != null){
            if(!this.currentCustomer.getCustomerOrderCollection().isEmpty()){                
                JsfUtil.addErrorMessage(new Exception(), 
                    ResourceBundle.getBundle(BUNDLE).getString("PersistenceErrorOccured"));
                return PageNavigation.LIST;
            }
        } 
        selectedItemIndex = pagination.getPageFirstItem() + getItems().getRowIndex();
        performDestroy();
        recreateModel();
        return PageNavigation.LIST;
    }
    

    public PageNavigation destroyAndView() {
        performDestroy();
        recreateModel();
        updateCurrentItem();
        if (selectedItemIndex >= 0) {
            return PageNavigation.VIEW;
        } else {
            // all items were removed - go back to list
            recreateModel();
            return PageNavigation.LIST;
        }
    }

    /**
     * Deletes a Customer from the database.
     */
    private void performDestroy() {    
        try {
            getFacade().remove(this.currentCustomer);
            JsfUtil.addSuccessMessage(ResourceBundle.getBundle(BUNDLE).getString("CustomerDeleted"));
        } catch (Exception e) {
            JsfUtil.addErrorMessage(e, ResourceBundle.getBundle(BUNDLE).getString("PersistenceErrorOccured"));
//            e.printStackTrace();
        }
    }

    /**
     * Updates a Customer.
     */
    private void updateCurrentItem() {
        int count = getFacade().count();
        if (selectedItemIndex >= count) {
            // selected index cannot be bigger than number of items:
            selectedItemIndex = count - 1;
            // go to previous page if last page disappeared:
            if (pagination.getPageFirstItem() >= count) {
                pagination.previousPage();
            }
        }
        if (selectedItemIndex >= 0) {
            currentCustomer = getFacade().findRange(new int[]{selectedItemIndex, selectedItemIndex + 1}).get(0);
        }
    }

    //public DataModel getItems() {
    public DataModel<Customer> getItems() {        
        if (items == null) {
            items = getPagination().createPageDataModel();
        }
        return items;
    }    
    

    private void recreateModel() {
        items = null;
    }

    public PageNavigation next() {
        getPagination().nextPage();
        recreateModel();
        return PageNavigation.LIST;
    }

    public PageNavigation previous() {
        getPagination().previousPage();
        recreateModel();
        return PageNavigation.LIST;
    }

    public SelectItem[] getItemsAvailableSelectMany() {
        return JsfUtil.getSelectItems(ejbFacade.findAll(), false);
    }

    public SelectItem[] getItemsAvailableSelectOne() {
        return JsfUtil.getSelectItems(ejbFacade.findAll(), true);
    }

    @FacesConverter(forClass = Customer.class)
    public static class CustomerControllerConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent 
                                                component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            CustomerController controller = 
                    (CustomerController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, 
                            "customerController");
            return controller.ejbFacade.find(getKey(value));
        }

        java.lang.Integer getKey(String value) {
            java.lang.Integer key;
            key = Integer.valueOf(value);
            return key;
        }

        String getStringKey(java.lang.Integer value) {
            StringBuilder sb = new StringBuilder();
            sb.append(value);
            return sb.toString();
        }

        @Override
        public String getAsString(FacesContext facesContext, 
                            UIComponent component, Object object) {
            if (object == null) {
                return null;
            }
            if (object instanceof Customer) {
                Customer o = (Customer) object;
                return getStringKey(o.getIdPerson());
            } else {
                throw new IllegalArgumentException("object " + object 
                        + " is of type " + object.getClass().getName() 
                        + "; expected type: " 
                        + CustomerController.class.getName());
            }
        }
    }

    
    @Produces
    @LoggedIn
    public Person getAuthenticated() {
        
//        System.out.println("CustomerController, getAutheticated, "
//                + "Produces/LoggedIn, "
//                + "\nauthenticated: " + this.authenticated
//                + "\ncurrent-customer: " + this.currentCustomer
//                + "\nuserBean: " + this.ejbFacade);
        return authenticated;
    }
    
    
    public void setAuthenticated(Person p) {
        
//         System.out.println("CustomerController, setAutheticated, "
//                + "Produces/LoggedIn, "
//                + "\nperson: " + p.getEmail()
//                + "\nauthenticated: " + this.authenticated
//                + "\ncurrent-customer: " + this.currentCustomer
//                + "\nuserBean: " + this.ejbFacade);
        
        this.authenticated = p;
    }
    
    /**
     * Creats a new id based on an investigation of the existing numbers.
     * Is there a missing number based of regular order, which will be used as 
     * new id. Is there no missing number the new id will be the sum of all
     * plus 1.
     * 
     * @return int The new id.
     */
    private int createIdCustomer(){
        return this.getFacade().getIdNewPerson();
    }
    
    
    public void setCurrentCustomer(Customer c){
        
        this.currentCustomer = c;
        
        System.out.println("CustomerController, setCurrentCustomer:"
                + "\ncurrentCustomer: " + this.currentCustomer
                + "\ncurrentPersonDetails: " + this.currentPersonDetails);
        
        
        if(this.currentPersonDetails == null){
            for (PersonDetails pd : this.currentCustomer.getPersonDetailsCollection()){
                this.currentPersonDetails = pd;
                break;
            }
        }
    }
    
    
    /**
     * Get the value of currentPersonDetails
     *
     * @return the value of currentPersonDetails
     */
    public PersonDetails getCurrentPersonDetails() {
        if(currentPersonDetails == null){
            this.currentPersonDetails = new PersonDetails();
            return this.currentPersonDetails;
        }
        return currentPersonDetails;
    }

    /**
     * Set the value of currentPersonDetails
     *
     * @param currentPersonDetails new value of currentPersonDetails
     */
    public void setCurrentPersonDetails(PersonDetails currentPersonDetails) {
        this.currentPersonDetails = currentPersonDetails;
    }
    
}