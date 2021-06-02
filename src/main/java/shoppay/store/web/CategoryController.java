package shoppay.store.web;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import javax.ejb.EJB;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;
import javax.faces.model.DataModel;
import javax.faces.model.ListDataModel;
import javax.faces.model.SelectItem;
import shoppay.store.ejb.CategoryBean;
import shoppayentity.entity.Category;
import shoppay.store.web.util.AbstractPaginationHelper;
import shoppay.store.web.util.JsfUtil;
import shoppay.store.web.util.PageNavigation;



/**
 * 
 * This class is used of a client and an admin.
 * Afterwrds there is one table for all Categories,
 * but different Categories will be displayed in different Menues of
 * the TopBarMenu (topbarmenu.xhtml), the table 'Category' has a 
 * field called 'menu', which indicates by a Strng ('Products' or 'Extras')
 * in which menu the Category will be displayed.
 * Concerning this issue in the file Category.Create.xhtml
 * is a SelectOneMenu with mentioned the options 'Products' and 'Extras'.
 * 
 * 
 * 
 * 
 * @author stefan.streifeneder@gmx.de
 */
@ManagedBean (name= "categoryController")
@SessionScoped
public class CategoryController implements Serializable {
    
    private static final String BUNDLE = "Bundle";
    
    private static final long serialVersionUID = 2310259107429450847L;

    private Category current;    
    
    private DataModel<Category> items = null;  
    
    @EJB
    private CategoryBean ejbFacade;
    
    private AbstractPaginationHelper pagination;
    
    private int selectedItemIndex;
    
    /**
     * New. It is used by Create.xhtml to use a SelectOneMenu.
     */
    private Map<String, Object> mapMenues;    
    

    /**
     * Noarg constructor.
     */
    public CategoryController() {
    }
    
    public Category getSelected() {
        if (current == null) {
            current = new Category();
            selectedItemIndex = -1;
        }
        return current;
    }

    /**
     * Returns an object of type CategoryBean, which is an EJB.
     * 
     * @return CategoryBean The EJB to access the dtatabse.
     */
    private CategoryBean getFacade() {
        return ejbFacade;
    }
    
    
    public AbstractPaginationHelper getPagination() {
        if (pagination == null) {
            pagination = new AbstractPaginationHelper(
                    AbstractPaginationHelper.DEFAULT_SIZE) {

                @Override
                public int getItemsCount() {
                    return getFacade().count();
                }

                @Override
                public DataModel createPageDataModel() {
                    return new ListDataModel(getFacade().findRange(
                            new int[]{getPageFirstItem(), 
                        getPageFirstItem() + getPageSize()}));
                }
            };
        }
        return pagination;
    }
    
    
    
    
    
    public PageNavigation prepareList() {
        this.recreateModel();
        return PageNavigation.LIST;
    }
    
    
    public PageNavigation prepareView() {
        current = (Category) getItems().getRowData();
        selectedItemIndex = 
                pagination.getPageFirstItem() + getItems().getRowIndex();
        return PageNavigation.VIEW; //
    }
    
    
    
    public PageNavigation prepareCreate() {
        current = new Category();
        this.current.setIdCategory(this.getIdCategory());
        selectedItemIndex = -1;
        return PageNavigation.CREATE; 
    }
    
    
    
    public PageNavigation create() {
        try {
            getFacade().create(current);
            JsfUtil.addSuccessMessage(
                    ResourceBundle.getBundle(BUNDLE).getString(
                            "CategoryCreated"));
            recreateModel();
            return PageNavigation.LIST;
            
            //orig - WRONG!
            //return prepareCreate();            
            
        } catch (Exception e) {
            JsfUtil.addErrorMessage(e, 
                    ResourceBundle.getBundle(BUNDLE).getString(
                            "PersistenceErrorOccured"));
            return null;
        }
    }
    
    
    public PageNavigation prepareEdit() {
        current = (Category) getItems().getRowData();
        selectedItemIndex = 
                pagination.getPageFirstItem() + getItems().getRowIndex();
        return PageNavigation.EDIT;
    }

    public PageNavigation update() {
        try {
            getFacade().edit(current);
            JsfUtil.addSuccessMessage(
                    ResourceBundle.getBundle(BUNDLE).getString(
                            "CategoryUpdated"));
            return PageNavigation.VIEW;
        } catch (Exception e) {
            JsfUtil.addErrorMessage(e, 
                    ResourceBundle.getBundle(BUNDLE).getString(
                            "PersistenceErrorOccured"));
            return null;
        }
    }
    
    public PageNavigation destroy() {
        //new
        try{
            this.checkCategory(current);
        }catch(Exception e){
            JsfUtil.addErrorMessage(e.getMessage());
            return PageNavigation.LIST;
        }
        current = (Category) getItems().getRowData();
        selectedItemIndex = 
                pagination.getPageFirstItem() + getItems().getRowIndex();
        performDestroy();
        recreateModel();
        return PageNavigation.LIST;
    }
    

    public PageNavigation destroyAndView() {
        //new
        try{
            this.checkCategory(current);
        }catch(Exception e){
            JsfUtil.addErrorMessage(e.getMessage());
            return PageNavigation.VIEW;
        }
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
    
    // Private method to destroy a category
    private void performDestroy() {
        try {
            getFacade().remove(current);
            JsfUtil.addSuccessMessage(
                    ResourceBundle.getBundle(BUNDLE).getString(
                            "CategoryDeleted"));
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
        if (selectedItemIndex >= 0) {
            current = getFacade().findRange(new int[]{
                selectedItemIndex, selectedItemIndex + 1}).get(0);
        }
    }

    
    
    
    /**
     * Sets the DataModel object to null.Returns a DataModel object,
     * which is called in the xhtml file to display the categories.
     * 
     * @return DataModel The object is used to to store all categories.
     */
    public DataModel<Category> getItems() {
        if (items == null) {
            items = getPagination().createPageDataModel();
        }
        return items;
    }

    
    /**
     * Sets the DataModel object to null.
     */
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
    
    // obvisiouly not implemented 
    // find usages returns no occurrences
    public SelectItem[] getItemsAvailableSelectMany() {
        return JsfUtil.getSelectItems(ejbFacade.findAll(), false);
    }

    // obvisiouly not implemented 
    // find usages returns no occurrences
    public SelectItem[] getItemsAvailableSelectOne() {
        return JsfUtil.getSelectItems(ejbFacade.findAll(), true);
    }

    
    
    
//    @FacesConverter(forClass = Category.class, value="category")
    @FacesConverter(forClass = Category.class)
    public static class CategoryControllerConverter implements Converter {

        // Is called by admin/product/Create.xhtml
        @Override
        public Object getAsObject(FacesContext facesContext, 
                            UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            
            CategoryController controller = 
                    (CategoryController)facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, 
                            "categoryController");
            
            Category ca = null;
            for(Category c : controller.getFacade().findAll()){
                if(c.getName().equals(value)){
                    ca = c;
                    break;
                }
            }
            
            return ca;
            // Returns an object of type Category.
//            return controller.ejbFacade.find(getKey(value));
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
            if (object instanceof Category) {
                Category o = (Category) object;
                return o.getName();
            } else {
                throw new IllegalArgumentException("object " + object
                        + " is of type " + object.getClass().getName() 
                        + "; expected type: " + CategoryController.class.getName());
            }
        }
    }
    
    
    //********************** NEW ***********************************
    
    
    
    
    /**
     * Creats a new id based on an investigation of the existing numbers.
     * Is there a missing number based of regular order, which will be used as 
     * new id. Is there no missing number the new id will be the sum of all
     * plus 1.
     * 
     * @return int The new id.
     */
    private int getIdCategory(){        
        List<Category> li = this.getFacade().findAll();  
        List<Integer> liIm = new ArrayList<>();        
        for(Category i : li){
            liIm.add(i.getIdCategory());
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
    
/**
     * Get the value of mapMenues
     *
     * @return the value of mapMenues
     */
    public Map<String, Object> getMapMenues() {
        
        String p = "Products";
        String e = "Extras";
        
        if(mapMenues == null){
            mapMenues = new LinkedHashMap<>();
            mapMenues.put(p, p);
            mapMenues.put(e, e);
        }
        
        return mapMenues;
    }

    /**
     * Set the value of mapMenues
     *
     * @param mapMenues new value of mapMenues
     */
    public void setMapMenues(Map<String, Object> mapMenues) {
        this.mapMenues = mapMenues;
    }
    
    
    private void checkCategory(Category c) throws Exception{
        if(c.getProductCollection() != null && 
                !c.getProductCollection().isEmpty()){
            throw new Exception("NO DELETE! This Group has members!");
        }
    }
}
