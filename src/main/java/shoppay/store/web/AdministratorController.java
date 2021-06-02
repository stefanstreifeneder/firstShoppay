package shoppay.store.web;

import shoppay.store.ejb.AdministratorBean;
import shoppayentity.entity.*;
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
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;
import javax.faces.model.DataModel;
import javax.faces.model.ListDataModel;
import javax.faces.model.SelectItem;
import javax.inject.Inject;
import javax.inject.Named;
import shoppay.store.ejb.GroupsBean;
import shoppay.store.ejb.PersonDetailsBean;
import shoppay.store.ejb.PersonGroupsBean;


@Named(value = "administratorController")
@SessionScoped
public class AdministratorController implements Serializable {
    
    private static final String BUNDLE = "Bundle";
    private static final long serialVersionUID = -2691147357609941284L;
    
    private Administrator currentAdministrator;

    private DataModel<Administrator> items = null;
    
    @EJB
    private AdministratorBean ejbFacade;
    
    private AbstractPaginationHelper pagination;
    
    private int selectedItemIndex;

    
    @Inject
    private PersonDetailsBean personDetailsBean;

    
    @Inject
    private GroupsBean groupsBean;
    
    
    @Inject
    private PersonGroupsBean personGroupsBean;
    
    private PersonDetails currentPersonDetails;   

    
        
    public AdministratorController() {
    }

    // Admin 1
    public Administrator getSelected() {
        
        System.out.println("AdminsitratorController, getSelected, "
                + "admin: " + currentAdministrator);
        
        if (currentAdministrator == null) {
            currentAdministrator = new Administrator();
            selectedItemIndex = -1;
        }
        return currentAdministrator;
    }

    private AdministratorBean getFacade() {
        return ejbFacade;
    }

    public AbstractPaginationHelper getPagination() {
        if (pagination == null) {
            pagination = new AbstractPaginationHelper(10) {

                @Override
                public int getItemsCount() {
                    return getFacade().count();
                }

                @Override
                public DataModel createPageDataModel() {
                    return new ListDataModel(getFacade().findRange(new int[]{getPageFirstItem(), getPageFirstItem() + getPageSize()}));
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
        
        //admin 2
        currentAdministrator = (Administrator) getItems().getRowData();
        selectedItemIndex = pagination.getPageFirstItem() + getItems().getRowIndex();
        
         System.out.println("AdminsitratorController, prepareView:"
                 + "\nperson: " + this.currentAdministrator
                + "\npd: " + this.currentPersonDetails);
        
         //currentPersonDetails 1
        if(this.currentPersonDetails == null){
            for (PersonDetails pd : this.currentAdministrator.getPersonDetailsCollection()){
                this.currentPersonDetails = pd;
                
                System.out.println("AdminsitratorController, prepareView, "
                        + "personDetails: "
                    + this.currentPersonDetails);
                
                
                break;
            }
        }
        
        return PageNavigation.VIEW;
    }

    public PageNavigation prepareCreate() {        
        System.out.println("AdminsitratorController, prepareCreate, ");
        
        // Admin 3
        currentAdministrator = new Administrator();
        
        // PD 1
        this.currentPersonDetails = new PersonDetails();
        
        
        
        selectedItemIndex = -1;
        return PageNavigation.CREATE;
    }

    public PageNavigation create() {        
        System.out.println("AdminsitratorController, create, current: "
                + this.getCurrentAdministrator().getEmail()
                + "\npersonDetails: " + this.getCurrentPersonDetails().getCity()
        ); 
        
        
        try {
            // password encrypt
            this.currentAdministrator.setPassword(AuthenticationUtils.encodeSHA256(this.currentAdministrator.getPassword()));
        } catch (UnsupportedEncodingException | NoSuchAlgorithmException ex) {
            Logger.getLogger(AdministratorController.class.getName()).log(Level.SEVERE, null, ex);
        }
                
                //dtype
                this.currentAdministrator.setDtype("admin");
                
                //id
                int recNo = this.getFacade().getIdNewPerson();
                this.currentAdministrator.setIdPerson(recNo);
                
                //List<CustomerOrder>
                this.currentAdministrator.setCustomerOrderCollection(new ArrayList<CustomerOrder>());
                
                
                this.getFacade().create(currentAdministrator);
                
                
                
                // PersonDetails
                if(this.currentPersonDetails == null){
                    this.currentPersonDetails = new PersonDetails();
                }
                this.currentPersonDetails.setIdPersonDetails(recNo);
                this.currentPersonDetails.setPerson(this.currentAdministrator);                
                this.personDetailsBean.create(this.currentPersonDetails);
                
                
                
                // Add PersonDetails to Customer
                this.currentAdministrator.setPersonDetailsCollection(new ArrayList<PersonDetails>());
                this.currentAdministrator.getPersonDetailsCollection().add(this.currentPersonDetails);
                
                
                //Person_Groups
                // id - can be the same like the id of the person, because
                // each person is stored in the table 'PERSON_GROUPS'
                // therefore it has to follow the same scheme like 'PERSON'
                PersonGroups pg = new PersonGroups();
                pg.setIdPersonGroups(this.personGroupsBean.getIdNewPersonGroups());
                pg.setEmail(this.currentAdministrator.getEmail());
                pg.setPerson(this.currentAdministrator);
                try{
                    this.personGroupsBean.create(pg);
                }catch(Exception e){
                    System.out.println("CustomerController, create, EXC: "
                    + e.getMessage());
                }
                
                // There are only two GRPOUPS (admin, user).
                Groups g = this.groupsBean.find(1);// '1' because it is a ADMIN
                pg.setGroups(g);
                g.getPersonGroupsCollection().add(pg);               
                this.groupsBean.edit(g);
                this.personGroupsBean.edit(pg);
                
                this.getFacade().edit(currentAdministrator);
                JsfUtil.addSuccessMessage(ResourceBundle.getBundle(BUNDLE).getString("CustomerCreated"));
                
        
        return PageNavigation.VIEW;
//                return PageNavigation.CREATE;
//        return null;
        
        //old
//        try {            
//            getFacade().create(currentAdministrator);
//            JsfUtil.addSuccessMessage(
//                    ResourceBundle.getBundle(BUNDLE).getString(
//                            "AdministratorCreated"));
//
//            return PageNavigation.VIEW;
//            
//        } catch (Exception e) {
//            JsfUtil.addErrorMessage(e, 
//                    ResourceBundle.getBundle(BUNDLE).getString(
//                            "PersistenceErrorOccured"));
//            return null;
//        }
    }

    public PageNavigation prepareEdit() {
        
        System.out.println("AdminsitratorController, prepareEdit, current: "
                + this.currentAdministrator
                + "\npersonDetails: " + this.getCurrentPersonDetails()
                + "\nselected: " + this.getSelected()
        ); 
        
        //Admin 5
        currentAdministrator = (Administrator) getItems().getRowData();
        
        PersonDetails pd = null;
        for(PersonDetails details : this.currentAdministrator.getPersonDetailsCollection()){
            pd = details;
            break;
        }
        this.currentPersonDetails = pd;
        
        selectedItemIndex = pagination.getPageFirstItem() + getItems().getRowIndex();
        return PageNavigation.EDIT;
    }

    public PageNavigation update() {
        try {
            
            // Admin 6
            getFacade().edit(currentAdministrator);
            
            this.personDetailsBean.edit(this.currentPersonDetails);
            
            
            JsfUtil.addSuccessMessage(
                    ResourceBundle.getBundle(BUNDLE).getString(
                            "AdministratorUpdated"));
            return PageNavigation.VIEW;
        } catch (Exception e) {
            JsfUtil.addErrorMessage(e, 
                    ResourceBundle.getBundle(BUNDLE).getString(
                            "PersistenceErrorOccured"));
            return null;
        }
    }

    public PageNavigation destroy() {
        
        // Admin 7
        currentAdministrator = (Administrator) getItems().getRowData();
        
        PersonDetails pd = null;
        for(PersonDetails details : this.currentAdministrator.getPersonDetailsCollection()){
            pd = details;
            break;
        }
        this.currentPersonDetails = pd;
        
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

    private void performDestroy() {
        try {
            this.personDetailsBean.remove(currentPersonDetails);
            // Admin 8
            getFacade().remove(currentAdministrator);
            
            
            
            
            JsfUtil.addSuccessMessage(
                    ResourceBundle.getBundle(BUNDLE).getString(
                            "AdministratorDeleted"));
        } catch (Exception e) {
            JsfUtil.addErrorMessage(e, 
                    ResourceBundle.getBundle(BUNDLE).getString(
                            "PersistenceErrorOccured"));
        }
    }

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
        
        //Admin 9
        if (selectedItemIndex >= 0) {
            currentAdministrator = getFacade().findRange(new int[]{ 
                selectedItemIndex, selectedItemIndex + 1}).get(0);
        }
    }

    public DataModel getItems() {
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

    @FacesConverter(forClass = Administrator.class)
    public static class AdministratorControllerConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, 
                            UIComponent component, String value) {
            
            System.out.println("AdminsitratorController, Converter, getAsObject");
            
            
            if (value == null || value.length() == 0) {
                return null;
            }
            AdministratorController controller = 
                    (AdministratorController) facesContext.
                            getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, 
                            "administratorController");
            
            return controller.ejbFacade.find(getKey(value));
        }

        java.lang.Integer getKey(String value) {
            
            System.out.println("AdminsitratorController, Converter, getKey");
            java.lang.Integer key;
            key = Integer.valueOf(value);
            return key;
        }

        String getStringKey(java.lang.Integer value) {
            
            System.out.println("AdminsitratorController, Converter, getStringKey");
            StringBuilder sb = new StringBuilder();
            sb.append(value);
            return sb.toString();
        }

        @Override
        public String getAsString(FacesContext facesContext, 
                            UIComponent component, Object object) {
            
            System.out.println("AdminsitratorController, Converter, getAsString");
            
            if (object == null) {
                return null;
            }
            if (object instanceof Administrator) {
                Administrator o = (Administrator) object;
                return getStringKey(o.getIdPerson());
            } else {
                throw new IllegalArgumentException("object " + 
                        object + " is of type " + object.getClass().getName() 
                        + "; expected type: " 
                        + AdministratorController.class.getName());
            }
        }
    }
    
    
    /**
     * Get the value of currentAdministrator
     *
     * @return the value of currentAdministrator
     */
    public Administrator getCurrentAdministrator() {
        
        System.out.println("AdminsitratorController, getCurrentAdministrator, "
                + "admin: " + this.currentAdministrator);
        
        return currentAdministrator;
    }

    /**
     * Set the value of currentAdministrator
     *
     * @param currentAdministrator new value of currentAdministrator
     */
    public void setCurrentAdministrator(Administrator currentAdministrator) {
        
        System.out.println("AdminsitratorController, SetCurrentAdministrator, "
                + "admin: " + currentAdministrator);
        
        
        this.currentAdministrator = currentAdministrator;
    }
    
    /**
     * Get the value of currentPersonDetails
     *
     * @return the value of currentPersonDetails
     */
    public PersonDetails getCurrentPersonDetails() {
        
        System.out.println("AdminsitratorController, getCurrentPersonDetails, "
                + "admin: " + this.currentPersonDetails);
        
        if(this.currentPersonDetails == null){
            this.currentPersonDetails = new PersonDetails();
        }
        
        return currentPersonDetails;
    }

    /**
     * Set the value of currentPersonDetails
     *
     * @param currentPersonDetails new value of currentPersonDetails
     */
    public void setCurrentPersonDetails(PersonDetails currentPersonDetails) {
        
        System.out.println("AdminsitratorController, SetCurrentPersonDetails, "
                + "admin: " + currentPersonDetails);
        
        this.currentPersonDetails = currentPersonDetails;
    }
    
}