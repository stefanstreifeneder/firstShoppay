/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package shoppay.store.web;

import shoppay.store.ejb.ProductBean;
import shoppayentity.entity.Product;
import shoppay.store.web.util.*;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
import javax.servlet.http.Part;
import javax.ws.rs.Path;
import shoppayentity.entity.Category;
import shoppay.store.ejb.CategoryBean;


/**
 * 
 * This class is used of a client and an admin.
 * Afterwrds there is one table for all Products,
 * but different Products can be placed in different Categories,
 * which are represented by different web-sides and different menues.
 * Additional a product can be displayed at the front-page of the
 * web-application and under its category.
 * This web-application provides two menues 'Products' and 'Extras' 
 * within the menuebar, which is represented by the file topbarmenu.xhtml.
 * Due to the mentioned the table 'Product' has a 
 * field called 'status', which indicates by a String in which 
 * web-side the Product will be displayed.
 * The following values are available:
 * 
 * offline
 * products
 * front_products
 * extras
 * front_extras
 * 
 * @author stefan.streifeneder@gmx.de
 */
@Named(value = "productController")
@SessionScoped
public class ProductController implements Serializable {

    private final static Logger logger = Logger.getLogger(
            ProductController.class.getCanonicalName());
    
    private static final String BUNDLE = "Bundle";
    
    private static final long serialVersionUID = -1835103655519682074L;
    
    private Product current;
    
    private DataModel<Product> items = null;
    
    @EJB
    private ProductBean ejbFacade;
    
    @Inject
    private CategoryBean categoryBean;
    
    private Category currentCategory;
    
    private Map<String, Object> categoryMap;    
    
    private Map<String, Object> stausDisplay;
    
    private AbstractPaginationHelper pagination;
    
    private int selectedItemIndex;
    
    // used for wizard
    private int step = 1;
    
    private int categoryId;
    
    private Part filePart;
    
    private static final List<String> EXTENSIONS_ALLOWED = new ArrayList<>();

    static {
        // images only
        EXTENSIONS_ALLOWED.add(".jpg");
        EXTENSIONS_ALLOWED.add(".bmp");
        EXTENSIONS_ALLOWED.add(".png");
        EXTENSIONS_ALLOWED.add(".gif");
    }

    private String getFileName(Part part) {
        String partHeader = part.getHeader("content-disposition");
        logger.log(Level.INFO, "Part Header = {0}", partHeader);
        for (String cd : part.getHeader("content-disposition").split(";")) {
            if (cd.trim().startsWith("filename")) {
                return cd.substring(cd.indexOf('=') + 1).trim().replace("\"", "");
            }
        }
        return null;

    }

    
    public void upload() {
        logger.info(getFilePart().getName());
        try {
            InputStream is = getFilePart().getInputStream();

            int i = is.available();
            byte[] b = new byte[i];
            is.read(b);

            logger.log(Level.INFO, "Length : {0}", b.length);
            String fileName = getFileName(getFilePart());
            logger.log(Level.INFO, "File name : {0}", fileName);

            // generate *unique* filename 
            final String extension = fileName.substring(fileName.length() - 4);

            if (!EXTENSIONS_ALLOWED.contains(extension)) {
                logger.severe("User tried to upload file that's not an image. "
                        + "Upload canceled.");
                JsfUtil.addErrorMessage(new Exception("Error trying to upload file"), 
                        ResourceBundle.getBundle(BUNDLE).getString("Error "
                                + "trying to upload file"));
                //response.sendRedirect("admin/product/List.xhtml?errMsg=Error trying to upload file");
                return;
            }
            
            current.setImgSource(b);
            current.setImg(fileName);            
            ejbFacade.edit(current);
            setStep(3);
            JsfUtil.addSuccessMessage("Product image successfuly uploaded!");
            
        } catch (Exception ex) {
        }

    }

    public Product getSelected() {
        if (current == null) {
            current = new Product();
            selectedItemIndex = -1;
        }
        //new
        currentCategory = current.getCategory();
        return current;
    }

    public String showAll() {
        recreateModel();
        categoryId = 0; // show all products
        return "product/List";
    }
    

    private ProductBean getFacade() {
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
                    if (categoryId != 0) {
                        return new ListDataModel(
                                getFacade().findByCategory(
                                        new int[]{getPageFirstItem(),
                            getPageFirstItem() + getPageSize()}, categoryId));
                    }
                    
                    return new ListDataModel(                            
                        getFacade().findRange(
                            new int[]{
                                getPageFirstItem(), getPageFirstItem() + getPageSize()
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

    public PageNavigation done() {        
        this.getFacade().edit(current);        
        recreateModel();
        setStep(1);
        current = null;
        return PageNavigation.LIST;
    }

    public Product findById(int id) {
        return ejbFacade.find(id);
    }

    public PageNavigation prepareView() {
        current = (Product) getItems().getRowData();
        selectedItemIndex = pagination.getPageFirstItem() 
                + getItems().getRowIndex();
        return PageNavigation.VIEW;
    }    

    public PageNavigation prepareCreate() {
        current = new Product();
        selectedItemIndex = -1;
        setStep(1);
        return PageNavigation.CREATE;
    }

    public PageNavigation nextStep() {
        setStep(getStep() + 1);

        return PageNavigation.CREATE;
    }

    
    /**
     * Creats a new product.
     * 
     * @return PageNavigation To navigate.
     */
    public PageNavigation create() {
        this.current.setIdProduct(this.getNewIDproduct());
        this.current.setCategory(this.currentCategory);                
        try {
            getFacade().create(current);
            JsfUtil.addSuccessMessage(
                    ResourceBundle.getBundle(BUNDLE).getString(
                            "ProductCreated"));
            setStep(2);
            return PageNavigation.CREATE;

        } catch (Exception e) {
            JsfUtil.addErrorMessage(e, 
                    ResourceBundle.getBundle(BUNDLE).getString(
                            "PersistenceErrorOccured"));
            return null;
        }
    }

    public PageNavigation prepareEdit() {
        current = (Product) getItems().getRowData();
        selectedItemIndex = pagination.getPageFirstItem() + 
                getItems().getRowIndex();
        return PageNavigation.EDIT;
    }

    
    
    /**
     * The method updates a Product, except its category.
     * Since the category can be placed in different menues, this 
     * update operation has to be done by its own.
     * 
     * @return PageNavigation is an enum to navigate to VIEW. 
     */
    public PageNavigation update() {
        try {            
            getFacade().edit(current);
            JsfUtil.addSuccessMessage(
                    ResourceBundle.getBundle(BUNDLE).getString(
                            "ProductUpdated"));

            return PageNavigation.VIEW;
        } catch (Exception e) {
            JsfUtil.addErrorMessage(e, 
                    ResourceBundle.getBundle(BUNDLE).getString(
                            "PersistenceErrorOccured"));
            return null;
        }
    }

    public PageNavigation destroy() {
        current = (Product) getItems().getRowData();
        selectedItemIndex = pagination.getPageFirstItem() 
                + getItems().getRowIndex();
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
            getFacade().remove(current);
            JsfUtil.addSuccessMessage(
                    ResourceBundle.getBundle(BUNDLE).getString(
                            "ProductDeleted"));
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

    /**
     * @return the categoryId
     */
    public int getCategoryId() {
        return categoryId;
    }

    /**
     * @param categoryId the categoryId to set
     */
    public void setCategoryId(int categoryId) {
        this.categoryId = categoryId;
    }

    /**
     * @return the filePart
     */
    public Part getFilePart() {
        return filePart;
    }

    /**
     * @param filePart the filePart to set
     */
    public void setFilePart(Part filePart) {
        this.filePart = filePart;
    }

    /**
     * 
     */
    @FacesConverter(forClass = Product.class, value="product")
    public static class ProductControllerConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, 
                                UIComponent component, String value) {            
            if (value == null || value.length() == 0) {
                return null;
            }
            ProductController controller = 
                    (ProductController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, 
                            "productController");
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
            
            if (object instanceof Product) {
                Product o = (Product) object;
                return o.getName();
//                return getStringKey(o.getIdProduct());
            } else {
                throw new IllegalArgumentException("object " 
                        + object + " is of type " 
                        + object.getClass().getName() + "; expected type: " 
                        + ProductController.class.getName());
            }
        }
    }

    public int getStep() {
        return step;
    }

    public void setStep(int step) {
        this.step = step;
    }
    
    
    //********************** NEW ***********************************
    
    
    
    // Enables a buyer to show details of the product.
    // Is called in loginTest.xhtml.
    public String goToProductViewForCustumer(){        
        current = (Product) getItems().getRowData();
        selectedItemIndex = pagination.getPageFirstItem() 
                + getItems().getRowIndex();
        return "/product/View.xhtml";
    }
    
    
    public DataModel getFrontpageProducts(){        
        List<Product> lp = this.getFacade().findAll();
        List<Product> productsToReturn = new ArrayList<>();
        for(Product p : lp){
            if(p.getStatus().equals("front_products")){
                productsToReturn.add(p);
            }else if(p.getStatus().equals("front_extras")){
                productsToReturn.add(p);
            }
        }
        
        this.items = new ListDataModel(productsToReturn);
        return items;
    }
    
    /**
     * Creats a new id based on an investigation of the existing numbers.
     * Is there a missing number based of regular order, which will be used as 
     * new id. Is there no missing number the new id will be the sum of all
     * plus 1.
     * 
     * @return int The new id.
     */
    private int getNewIDproduct(){        
        List<Product> li = this.getFacade().findAll();  
        List<Integer> liIm = new ArrayList<>();        
        for(Product i : li){
            liIm.add(i.getIdProduct());
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
    
    // seems unused
    @Path("/image")
    public void getIMG(){
        System.out.println("ProductController, getIMG");
        
    }
    
    
    /**
     * 
     * Returns the category due to the variable 'categoryId'.
     * Is called in Product.List.xhtml.
     * 
     * 
     * @return Category returns the value due to the variable 'categoryId'.
     */
    public Category readMyCat(){
        return this.categoryBean.find(this.categoryId);
    }
    
    
    /**
     * Get the value of currentCategory.
     * The returned value is not initialized in the moment
     * the list to display all items is created.
     *
     * @return Category the value of currentCategory
     */
    public Category getCurrentCategory() {
        return currentCategory;
    }

    /**
     * Set the value of currentCategory
     *
     * @param currentCategory new value of currentCategory
     */
    public void setCurrentCategory(Category currentCategory) {
        this.currentCategory = currentCategory;
    }
    
    /**
     * Get the value of categoryMap
     *
     * @return the value of categoryMap
     */
    public Map<String, Object> getCategoryMap() {
        if(categoryMap == null){
            categoryMap = new LinkedHashMap<>();
            for(Category c : this.categoryBean.findAll()){
                categoryMap.put(c.getName(), c);
            }
        }
        return categoryMap;
    }

    /**
     * Set the value of categoryMap
     *
     * @param categoryMap new value of categoryMap
     */
    public void setCategoryMap(Map<String, Object> categoryMap) {
        this.categoryMap = categoryMap;
    }
    
    
    /**
     * Get the value of stausDisplay
     *
     * @return the value of stausDisplay
     */
    public Map<String, Object> getStausDisplay() {
        String s = "offline";
        this.stausDisplay = new LinkedHashMap<>();
        this.stausDisplay.put(s, s);
        if(this.current.getCategory().getMenu().equals("Products")){
            s = "products";            
            this.stausDisplay.put(s, s);
            s = "front_products";            
            this.stausDisplay.put(s, s);
        }else{
            s = "extras";            
            this.stausDisplay.put(s, s);
            s = "front_extras";            
            this.stausDisplay.put(s, s);
        }
        
        return this.stausDisplay;
    }

    /**
     * Set the value of stausDisplay
     *
     * @param stausDisplay new value of stausDisplay
     */
    public void setStausDisplay(Map<String, Object> stausDisplay) {
        this.stausDisplay = stausDisplay;
    }
    
    
    public boolean setDisplay(Product p){
        if(p != null && p.getStatus().equals("offline")){
            return false;
        }        
        return true;
    }
    
    
    // New
    // Is called in topbarmenu.xhtml 
    public String showByCatId(int catId){
        categoryId = catId; 
        recreateModel();
        return "product/List";
    }
    
    
    public PageNavigation editCategory(){
        try {            
            Product p = this.getFacade().find(this.current.getIdProduct());
            Category c = this.categoryBean.find(this.currentCategory.getIdCategory());
            p.setStatus("offline");
            p.setCategory(c);
            Collection<Product> cp = c.getProductCollection();
            cp.add(p);
            this.current = p;
            this.categoryBean.edit(c);
            this.getFacade().edit(p);
            JsfUtil.addSuccessMessage(
                    ResourceBundle.getBundle(BUNDLE).getString(
                            "ProductUpdated"));
            return PageNavigation.VIEW;
        } catch (Exception e) {
            JsfUtil.addErrorMessage(e, 
                    ResourceBundle.getBundle(BUNDLE).getString(
                            "PersistenceErrorOccured"));
            return null;
        }
    }
        
}