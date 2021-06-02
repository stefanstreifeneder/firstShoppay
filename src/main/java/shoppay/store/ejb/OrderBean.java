package shoppay.store.ejb;


import shoppayentity.entity.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.ws.rs.*;

/**
 * OrderBean is an EJB exposed as RESTful service Provides methods to manipulate
 * order status and query orders based on specific status.
 *
 * @author stefan.streifeneder@gmx.de
 */
@Stateless
@Path("/orders")
public class OrderBean extends AbstractFacade<CustomerOrder> implements Serializable {

    private static final Logger logger = 
            Logger.getLogger(OrderBean.class.getCanonicalName());
    
    private static final long serialVersionUID = -2407971550575800416L;
    
    @PersistenceContext
    private EntityManager em;
    
    CustomerOrder order;
    
    @EJB
    OrderStatusBean statusBean;
    
    
    @EJB
    OrderDetailBean detailBean;

    public OrderBean() {
        super(CustomerOrder.class);
    }

    /**
     * **************************************************************************
     * Business methods
     * ***************************************************************************
     * @return EntityManager Rules access to the dtabase.
     */
    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public List<CustomerOrder> getOrderByCustomerId(Integer id) {
        Query createNamedQuery = getEntityManager().createNamedQuery(
                "CustomerOrder.findByIdCustomerOrder");
        createNamedQuery.setParameter("idCustomerOrder", id);
        return createNamedQuery.getResultList();
    }
    
    public CustomerOrder getOrderById(Integer id) {
        Query createNamedQuery = getEntityManager().createNamedQuery(
                "CustomerOrder.findByIdCustomerOrder");
        createNamedQuery.setParameter("idCustomerOrder", id);
        return (CustomerOrder)createNamedQuery.getSingleResult();
    }
    
    public Integer createNewCustomerOrderId(){
        TypedQuery<CustomerOrder> query = em.createNamedQuery(
                    "CustomerOrder.findAll", CustomerOrder.class);
        List<CustomerOrder> li = query.getResultList();
        List<Integer> liIm = new ArrayList<>();        
        for(CustomerOrder i : li){
            liIm.add(i.getIdCustomerOrder());
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
    
    
  // It seems that method is never called!?
    @GET
    @Produces({"application/xml", "application/json"})
    public List<CustomerOrder> getOrderByStatus(@QueryParam("orderStatus") int status) {        
        System.out.println("OderBean, getOrderByStatus, status: "
                + status);
        Query createNamedQuery = 
                getEntityManager().createNamedQuery("CustomerOrder.findAll");
        //orig
//        OrderStatus result = statusBean.find(status);
//        createNamedQuery.setParameter("orderStatus", result.getStatus());
        
        List<CustomerOrder> orders = createNamedQuery.getResultList();
        List<CustomerOrder> retLi = new ArrayList<>();
        for(CustomerOrder co : orders){
            if(co.getOrderStatus().getStatus().equals(Integer.toString(status))){
                retLi.add(co);
            }
        }        
        return retLi;
    }
    

    @PUT
    @Path("{orderId}")
    @Produces({"application/xml", "application/json"})
    public void setOrderStatus(@PathParam("orderId") int orderId, 
            String newStatus) {         
        logger.log(Level.INFO, "Order id:{0} - Status:{1}", 
                new Object[]{orderId, newStatus});
        try {
            order = this.find(orderId);
            if (order != null) {
                logger.log(Level.FINEST, "Updating order {0} status to {1}", 
                        new Object[]{order.getIdCustomerOrder(), newStatus});
                OrderStatus oStatus = statusBean.find(orderId);
                oStatus.setStatus(newStatus);
                statusBean.edit(oStatus);
                order.setOrderStatus(oStatus);
                em.merge(order);
                logger.info("Order Updated!");
            }
        } catch (Exception ex) {
            logger.log(Level.SEVERE, ex.getMessage());
        }
    }
    
    
    
    
    @Override
    public void remove(CustomerOrder order) {
        
        //OrderDetails
        List<OrderDetail> liCO = em.createNamedQuery(
                "OrderDetail.findAll").getResultList();        
        
        //There is only one OrderDetail
        OrderDetail orderDetail = null;
        if(liCO != null){
            for(OrderDetail od : liCO){
                if(order.getIdCustomerOrder().equals(
                        od.getCustomerOrder().getIdCustomerOrder())){
                    orderDetail = od;
                    break;                    
                }
            }
        }
        
        //OrderStatus
        List<OrderStatus> liOS = em.createNamedQuery(
                "OrderStatus.findAll").getResultList();
        OrderStatus orderStatus = null;
        if(liOS != null){
            for(OrderStatus od : liOS){
                if(order.getOrderStatus().equals(
                        od)){
                    orderStatus = od;
                    break;                    
                }
            }
        }
        
        Person c = em.merge(order.getPerson());
        c.setCustomerOrderCollection(null);
        em.merge(c);
        
        em.remove(orderDetail);
        em.remove(em.merge(order));
        em.remove(orderStatus);
    }
    
    
    
     /**
     * ***************************************************************************
     * Status orders mapped to ENUM
     */
    public enum Status {

        PENDING_PAYMENT(2),
        READY_TO_SHIP(3),
        SHIPPED(4),
        CANCELLED_PAYMENT(5),
        CANCELLED_MANUAL(6);
        
        private int status;

        private Status(int pStatus) {
            status = pStatus;
        }

        public int getStatus() {
            return status;
        }
    }
}