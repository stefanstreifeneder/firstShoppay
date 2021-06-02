/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package shoppay.store.web;

import java.io.Serializable;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.faces.bean.SessionScoped;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;
import javax.faces.model.DataModel;
import javax.faces.model.ListDataModel;
import javax.faces.model.SelectItem;
import shoppay.store.ejb.OrderBean;
import shoppayentity.entity.*;
import shoppay.store.ejb.EventDispatcherBean;
import shoppay.store.ejb.OrderDetailBean;
import shoppay.store.ejb.OrderStatusBean;
import shoppay.store.ejb.ShoppingCart;
import shoppay.store.qualifiers.LoggedIn;
import shoppay.store.web.util.AbstractPaginationHelper;
import shoppay.store.web.util.JsfUtil;
import shoppay.store.web.util.PageNavigation;
import shoppay.events.OrderEvent;
import java.util.ArrayList;
import java.util.Calendar;
import javax.faces.bean.ManagedBean;
import javax.inject.Inject;
import shoppay.store.ejb.OrderJMSManager;
import shoppay.store.ejb.UserBean;


@ManagedBean (name= "customerOrderController")
@SessionScoped
public class CustomerOrderController implements Serializable {    

    private static final String BUNDLE = "Bundle";
    
    private static final long serialVersionUID = 8606060319870740714L;    
    
    @Inject
    @LoggedIn
    private Person user;// Warning is ok.
    
    private List<CustomerOrder> myOrders;
    
    private CustomerOrder currentOrder;
    
    private DataModel<CustomerOrder> items = null;
    
    @EJB
    private OrderBean ejbFacade;    
    
    @Inject
    private ShoppingCart shoppingCard;
    
    @Inject
    private OrderDetailBean orderDetailBean;
    
    
    @Inject
    private OrderStatusBean orderStatusBean;
    
    
    @Inject
    private UserBean userBean;
    
    
    @Inject
    private EventDispatcherBean eventDispatcher;
    
    @Inject
    private OrderJMSManager orderJMSManager;
    
    private AbstractPaginationHelper pagination;
    
    private int selectedItemIndex;
    
    private String searchString;
    
    private static final Logger logger = 
            Logger.getLogger(CustomerOrderController.class.getCanonicalName());

    public CustomerOrderController() {}

    public CustomerOrder getSelected() {
        //if-clause is a MUST
        if (currentOrder == null) {
            currentOrder = new CustomerOrder();
            selectedItemIndex = -1;
        }
        return currentOrder;
    }

    private OrderBean getFacade() {
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
                            new int[]{getPageFirstItem(), 
                                getPageFirstItem() + getPageSize()}));
                }
            };
        }
        return pagination;
    }

    public PageNavigation prepareList() {
        recreateModel();
        return PageNavigation.LIST;
    }
    
    
    // Called in bill.xhtml only by users.
    public String goToOrders(){
        List<CustomerOrder> custo = this.ejbFacade.findAll();
        List<CustomerOrder> liStor = new ArrayList();
        
        try{
            for(CustomerOrder cOrd : custo){
                if(cOrd.getPerson().getIdPerson().equals(
                        this.user.getIdPerson())){
                    liStor.add(cOrd);
                }
            }
        }catch(Exception e){
            JsfUtil.addErrorMessage(e, 
                    ResourceBundle.getBundle(BUNDLE).getString(
                            "PersistenceErrorOccured"));
            return "";
        }
        
        // To avoid NullPointerException
        if(this.items == null){
            this.items = this.getPagination().createPageDataModel();
        }
        this.items.setWrappedData(liStor);        
        
        return "/order/MyOrders.xhtml";
    }
    
    
    public PageNavigation prepareView() {
        currentOrder = (CustomerOrder) getItems().getRowData();
        selectedItemIndex = pagination.getPageFirstItem() 
                + getItems().getRowIndex();
        return PageNavigation.VIEW;
    }

    public PageNavigation prepareCreate() {
        currentOrder = new CustomerOrder();
        selectedItemIndex = -1;
        return PageNavigation.CREATE;
    }

    public String create() {
        // orig
    //public PageNavigation create() {
        // The signature of the method is changed,
        // because the original github code ends here.
        // But this application implements a kind of a fake
        // creditcard payment operation, which is handled
        // by separate side '/order/bill.xhtml'.        
        
        
        int recNo;
        int recordNo = -1;
        try {
            if (user == null) {
                JsfUtil.addErrorMessage(
                        JsfUtil.getStringFromBundle("Bundle", 
                                "LoginBeforeCheckout"));

            } else {
                if (user.getDtype().equals("admin")) {
                    JsfUtil.addErrorMessage(JsfUtil.getStringFromBundle("Bundle",
                            "AdministratorNotAllowed"));
                    return "";
                }
                
                
                //ID for Order and OrderStatus
                recordNo = this.getFacade().createNewCustomerOrderId();
                
                //out due orderstatus
                OrderStatus orderStatus = new OrderStatus();                
                orderStatus.setIdOrderStatus(recordNo);
                orderStatus.setStatus(
                        Integer.toString(
                                OrderBean.Status.PENDING_PAYMENT.getStatus()));
                orderStatus.setDescription("order");
                List<CustomerOrder> listOfOrders = new ArrayList();
                this.orderStatusBean.create(orderStatus);
                
                CustomerOrder order = new CustomerOrder();                
                order.setIdCustomerOrder(recordNo);                
                order.setAmount(shoppingCard.getTotal().doubleValue());
                order.setDateCreated(Calendar.getInstance().getTime());
                order.setPerson(user);
                order.setOrderStatus(orderStatus);
                getFacade().create(order);
                
                listOfOrders.add(order);                
                orderStatus.setCustomerOrderCollection(listOfOrders);
                this.orderStatusBean.edit(orderStatus);           
               
                
                Product p = null;
                for (Product pro : shoppingCard.getCartItems()) {
                    p = pro;
                    
                    // SHOULD BE HERE !!!!!!!!!!!!!!!!!!!!!!!!!!!
//                    OrderDetail detail = new OrderDetail();
//                    recNo = this.orderDetailBean.createNewOrderDetailId();
//                    detail.setIdOrderDetail(recNo);
//                    detail.setQty(1);//just one product
//                    detail.setCustomerOrder(order);
//                    detail.setProduct(p);
//                    this.orderDetailBean.create(detail);
//                    p.setOrderDetailCollection(new ArrayList<OrderDetail>());
//                    p.getOrderDetailCollection().add(detail);
                
                }
                
                // There will be just one OrderDetail for an CustomOrder
                OrderDetail detail = new OrderDetail();
                recNo = this.orderDetailBean.createNewOrderDetailId();
                detail.setIdOrderDetail(recNo);
                detail.setQty(1);//just one product
                detail.setCustomerOrder(order);
                
                //old
                //detail.setProduct(p);
                this.orderDetailBean.create(detail);
                
                
                // old
//                if(p != null){
//                    p.setOrderDetailCollection(new ArrayList<OrderDetail>());
//                    p.getOrderDetailCollection().add(detail);
//                }

                
                List<OrderDetail> listOfOderDetails = new ArrayList<>();
                listOfOderDetails.add(detail);
                order.setOrderDetailCollection(listOfOderDetails);
                this.getFacade().edit(order);
                
                if(this.user.getCustomerOrderCollection() == null){
                    this.user.setCustomerOrderCollection(
                            new ArrayList<CustomerOrder>());                    
                }
                this.user.getCustomerOrderCollection().add(order);                
                this.userBean.edit((Customer)this.user);
                
                this.shoppingCard.clear();
            }
            // orig, but causes problems
//            JsfUtil.addSuccessMessage(
//                    ResourceBundle.getBundle(BUNDLE).getString(
//                            "CustomerOrderCreated"));
        } catch (Exception e) {
            JsfUtil.addErrorMessage(e, 
                    ResourceBundle.getBundle(BUNDLE).getString(
                            "PersistenceErrorOccured"));
            return null;
        }
        
        try{
            OrderEvent event = null;
            for(CustomerOrder cuor : this.getFacade().findAll()){                
                if(cuor.getIdCustomerOrder() == recordNo){
                    event = orderToEvent(cuor);
                    break;
                }
            }            
            eventDispatcher.publish(event);            
        }catch(Exception e){
            JsfUtil.addErrorMessage(e, ResourceBundle.getBundle(BUNDLE).getString(
                    "PersistenceErrorOccured"));
        }
        return "/order/bill.xhtml";
    }
    
    
    
    
    public PageNavigation prepareEdit() {
        currentOrder = (CustomerOrder) getItems().getRowData();
        selectedItemIndex = pagination.getPageFirstItem() + getItems().getRowIndex();
        return PageNavigation.EDIT;
    }

    
    // it seems that the method is never called.
    public PageNavigation update() {
        try {
            getFacade().edit(currentOrder);
            JsfUtil.addSuccessMessage(ResourceBundle.getBundle(BUNDLE).getString("CustomerOrderUpdated"));
            return PageNavigation.VIEW;
        } catch (Exception e) {
            JsfUtil.addErrorMessage(e, ResourceBundle.getBundle(BUNDLE).getString("PersistenceErrorOccured"));
            return null;
        }
    }

    public PageNavigation destroy() {
        currentOrder = (CustomerOrder) getItems().getRowData();
        selectedItemIndex = pagination.getPageFirstItem() + getItems().getRowIndex();
        performDestroy();
        recreateModel();
        return PageNavigation.LIST;
    }
    
    
    public PageNavigation destroyAndView() {
        performDestroy();
        recreateModel();
        updateCurrentItem();
        recreateModel();
        return PageNavigation.LIST;
    }

    private void performDestroy() {
        try {
            getFacade().remove(currentOrder);
            JsfUtil.addSuccessMessage(
                    ResourceBundle.getBundle(BUNDLE).getString(
                            "CustomerOrderDeleted"));
        } catch (Exception e) {
            JsfUtil.addErrorMessage(e, 
                    ResourceBundle.getBundle(BUNDLE).getString(
                            "PersistenceErrorOccured"));
        }
    }

    public PageNavigation cancelOrder() {
        System.out.println("CustomerOrderController, cancelOrder, START");
        currentOrder = (CustomerOrder) getItems().getRowData();
        selectedItemIndex = pagination.getPageFirstItem() + getItems().getRowIndex();

        try {
            // remove from JMS queue
            //orig
            orderJMSManager.deleteMessage(currentOrder.getIdCustomerOrder());

            // update DB order status
            ejbFacade.setOrderStatus(currentOrder.getIdCustomerOrder(), 
                    String.valueOf(OrderBean.Status.CANCELLED_MANUAL.getStatus()));


            recreateModel();
            return PageNavigation.LIST;
        } catch (Exception ex) {            
            System.out.println("CustomerOrderController, cancelOrder, Exc: "
                    + ex.getMessage());
        }

        return PageNavigation.INDEX;
    }

    
    public List<CustomerOrder> getMyOrders() {
        if (user != null) {

            myOrders = getFacade().getOrderByCustomerId(user.getIdPerson());
            if (myOrders.isEmpty()) {

                logger.log(Level.FINEST, "Customer {0} has no orders to display.", user.getEmail());
                return null;
            } else {
                logger.log(Level.FINEST, "Order amount:{0}", myOrders.get(0).getAmount());
                return myOrders;
            }
        } else {
            JsfUtil.addErrorMessage(
                    "Current user is not authenticated. Please do login "
                            + "before accessing your orders.");
            return null;
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
            currentOrder = getFacade().findRange(new int[]{selectedItemIndex, selectedItemIndex + 1}).get(0);
        }
    }

    
    //xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
    //
    //                 getItems
    //
    //xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
    public DataModel getItems() {
        
        System.out.println("CustomerOrderController, getItems, auth-p: "
                + this.user.getEmail());
        if (items == null) {
            items = getPagination().createPageDataModel();
        }        
        
        // if the method is used by an user, his orders will be filtered here
        if(!this.user.getDtype().equals("admin")){            
            DataModel<CustomerOrder> z = getPagination().createPageDataModel();
            List<CustomerOrder> l = new ArrayList();
            for (CustomerOrder co : items) {
                System.out.println("CustomerOrderController, getItems, "
                        + "item-status: " + co.getPerson());
                if (co.getPerson().getEmail().equals(this.user.getEmail())) {
                    l.add(co);
                }
            }
            z.setWrappedData(l);
            return z;
        } 
        return items;
    }
    
    
    // HARD TO BELIEVE, but the method is a MUST
    // even the functionality is NOT really been used
    // but it cares the index of the selected order is in ORDER.
    public String refreshList(){
        items = getPagination().createPageDataModel();
        return "/steves-shop-allin-store/admin/order/List.xhtml";
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
     * @return the searchString
     */
    public String getSearchString() {
        return searchString;
    }

    /**
     * @param searchString the searchString to set
     */
    public void setSearchString(String searchString) {
        this.searchString = searchString;
    }

    @FacesConverter(forClass = CustomerOrder.class)
    public static class CustomerOrderControllerConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, 
                            UIComponent component, String value) {
            
            System.out.println("CustomerOrderController, "
                    + "getMyOrdersCustomerOrderControllerConverter");
            
            if (value == null || value.length() == 0) {
                return null;
            }
            CustomerOrderController controller = 
                    (CustomerOrderController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, 
                            "customerOrderController");
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
            if (object instanceof CustomerOrder) {
                CustomerOrder o = (CustomerOrder) object;
                return getStringKey(o.getIdCustomerOrder());
            } else {
                throw new IllegalArgumentException("object " + object 
                        + " is of type " + object.getClass().getName() 
                        + "; expected type: " 
                        + CustomerOrderController.class.getName());
            }
        }
    }
    
    
    //has to be in comments
//    @Produces
//    @LoggedIn
    public Person getAuthenticated() {
        return this.user;
    }
    
    
    public void setAuthenticated(Person p) {
        this.user = p;
    }
    
    
    
    public String goToPayment(){
        return "/order/payment_shipping.xhtml";
    }
    
    
    private OrderEvent orderToEvent(CustomerOrder order) {        
        OrderEvent event = new OrderEvent();
        event.setAmount(order.getAmount());
        event.setCustomerID(order.getPerson().getIdPerson());
        event.setDateCreated(order.getDateCreated());
        event.setStatusID(order.getOrderStatus().getIdOrderStatus());        
        event.setOrderID(order.getIdCustomerOrder());
        return event;
    }
}
