package shoppay.store.web;

import shoppay.store.ejb.GroupsBean;
import shoppayentity.entity.Groups;
import shoppay.store.web.util.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ResourceBundle;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;
import javax.faces.model.DataModel;
import javax.faces.model.ListDataModel;
import javax.faces.model.SelectItem;
import javax.inject.Named;

@Named(value = "groupsController")
@SessionScoped
public class GroupsController implements Serializable {

    private static final String BUNDLE = "Bundle";
    private static final long serialVersionUID = 915049365977089806L;

    private Groups current;
    
    private DataModel<Groups> items = null;
    
    @EJB
    private GroupsBean ejbFacade;
    
    private AbstractPaginationHelper pagination;
    
    private int selectedItemIndex;

    
    public GroupsController() {
    }

    public Groups getSelected() {
        if (current == null) {
            current = new Groups();
            selectedItemIndex = -1;
        }
        return current;
    }

    private GroupsBean getFacade() {
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
                    return new ListDataModel(getFacade().findRange(
                            new int[]{
                                getPageFirstItem(), 
                                    getPageFirstItem() + getPageSize()
                            }
                        )
                    );
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
        current = (Groups) getItems().getRowData();
        selectedItemIndex = pagination.getPageFirstItem() + getItems().getRowIndex();
        return PageNavigation.VIEW;
    }

    public PageNavigation prepareCreate() {
        current = new Groups();
        current.setIdGroups(this.getIdCategory());
        selectedItemIndex = -1;
        return PageNavigation.CREATE;
    }

    public PageNavigation create() {
        try {
            getFacade().create(current);
            JsfUtil.addSuccessMessage(ResourceBundle.getBundle(BUNDLE).getString("GroupsCreated"));
            recreateModel();
            return PageNavigation.LIST;//new            
            // orig
            //return prepareCreate();
        } catch (Exception e) {
            JsfUtil.addErrorMessage(e, ResourceBundle.getBundle(BUNDLE).getString("PersistenceErrorOccured"));
            return null;
        }
    }

    /**
     * 
     * Is called in Groups.List.xhtml.
     * Assigns the selected item to current Groups object.
     * Sets the selectedItemIndex.
     * 
     * @return PageNavigation is an enum to navigate to EDIT. 
     */
    public PageNavigation prepareEdit() {
        current = (Groups) getItems().getRowData();
        selectedItemIndex = pagination.getPageFirstItem() + getItems().getRowIndex();
        return PageNavigation.EDIT;
    }

    
    
    /**
     * 
     * Is called in Groups.View.xhtml which uses 
     * resources.util.commandButtons.xhtml.
     * 
     * Beware the file Groups.View.xhtml installs a filter,
     * which allows only the field description to be altered.
     * 
     * 
     * @return PageNavigation is an enum to navigate to VIEW.
     */
    public PageNavigation update() {
        try {
            getFacade().edit(current);
            JsfUtil.addSuccessMessage(
                    ResourceBundle.getBundle(BUNDLE).getString("GroupsUpdated"));
            return PageNavigation.VIEW;
        } catch (Exception e) {
            JsfUtil.addErrorMessage(e, 
                    ResourceBundle.getBundle(BUNDLE).getString(
                            "PersistenceErrorOccured"));
            return null;
        }
    }

    
    /**
     * 
     * Is called in Groups.List.xhtml.
     * Assigns the selected item to current Groups object.
     * Sets the selectedItemIndex.
     * Calls the private method performDestroy() to erase the 
     * entry in the database.
     * Sets items to null by calling recreateModel().
     * 
     * 
     * 
     * @return PageNavigation is an enum to navigate to LIST. 
     */
    public PageNavigation destroy() {
        current = (Groups) getItems().getRowData();
        //new
         try{
            this.checkGroups(current);
        }catch(Exception e){
            JsfUtil.addErrorMessage(e.getMessage());
            return PageNavigation.LIST;
        }
        selectedItemIndex = pagination.getPageFirstItem() + getItems().getRowIndex();
        performDestroy();
        recreateModel();
        updateCurrentItem();// new
        return PageNavigation.LIST;
    }

    
    /**
     * 
     * Is called in Groups.View.xhtml which uses 
     * resources.util.commandButtons.xhtml.
     * 
     * Calls the private method performDestroy() to erase the 
     * entry in the database.
     * Sets items to null by calling recreateModel().
     * Sets the int value of 'selectedItemIndex'.
     * 
     * 
     * @return PageNavigation is an enum to navigate to View.
     */
    public PageNavigation destroyAndView() {
        //new
        try{
            this.checkGroups(current);
        }catch(Exception e){
            JsfUtil.addErrorMessage(e.getMessage());
            return PageNavigation.VIEW;
        }
        
        performDestroy();
        recreateModel();
        updateCurrentItem();
        
        // orig - not necessary, because you van delete only one item.
//        if (selectedItemIndex >= 0) {
//            return PageNavigation.VIEW;
//        } else {
//            // all items were removed - go back to list
//            recreateModel();
//            return PageNavigation.LIST;
//        }
        return PageNavigation.LIST;
    }

    
    
    /**
     * Private method, which is called by destroyAndView() and destroy().
     * 
     */
    private void performDestroy() {
        try {
            getFacade().remove(current);
            JsfUtil.addSuccessMessage(ResourceBundle.getBundle(BUNDLE).getString("GroupsDeleted"));
        } catch (Exception e) {
            JsfUtil.addErrorMessage(e, ResourceBundle.getBundle(BUNDLE).getString("PersistenceErrorOccured"));
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
        if (selectedItemIndex >= 0) {
            current = getFacade().findRange(new int[]{selectedItemIndex, selectedItemIndex + 1}).get(0);
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

    // never called, because you can select only one item.
    public SelectItem[] getItemsAvailableSelectMany() {
        return JsfUtil.getSelectItems(ejbFacade.findAll(), false);
    }

    // never called, because you can select only one item.
    public SelectItem[] getItemsAvailableSelectOne() {
        return JsfUtil.getSelectItems(ejbFacade.findAll(), true);
    }

    @FacesConverter(forClass = Groups.class)
    public static class GroupsControllerConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            GroupsController controller = (GroupsController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "groupsController");
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
        public String getAsString(FacesContext facesContext, UIComponent component, Object object) {
            if (object == null) {
                return null;
            }
            if (object instanceof Groups) {
                Groups o = (Groups) object;
                return getStringKey(o.getIdGroups());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type " + object.getClass().getName() + "; expected type: " + GroupsController.class.getName());
            }
        }
    }
    
    
    //********************** NEW ***********************************
    
    
    
    
    
    /**
     * Creats a new id based on an investigation of the existing numbers.
     * Is there a missing number based of regular order, it will be used as 
     * new id. Is there no missing number the new id will be the sum of all
     * plus 1.
     * 
     * @return int The new id.
     */
    private int getIdCategory(){        
        List<Groups> li = this.getFacade().findAll();  
        List<Integer> liIm = new ArrayList<>();        
        for(Groups i : li){
            liIm.add(i.getIdGroups());
        }
        
        Collections.sort(liIm);        
        Integer recNo = 1;
        for(Integer in : liIm){
            if(!in.equals(recNo)){             
                return recNo;
            }
            recNo++;
        } 
        return recNo;
    }
    
    
    private void checkGroups(Groups g) throws Exception{
        if(g.getPersonGroupsCollection() != null && 
                !g.getPersonGroupsCollection().isEmpty()){
            throw new Exception("NO update or delete! This Group has members!");
        }
    }
    
    
}