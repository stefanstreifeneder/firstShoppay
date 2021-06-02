/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package shoppay.store.handlers;

import shoppay.store.ejb.OrderBean;
import shoppay.store.ejb.OrderJMSManager;
import shoppay.events.OrderEvent;
import shoppay.store.qualifiers.Paid;
import java.io.Serializable;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.Asynchronous;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.enterprise.event.Observes;
import shoppayentity.entity.CustomerOrder;

/**
 *
 * @author stefan.streifeneder@gmx.de
 */
@Stateless
public class DeliveryHandler implements IOrderHandler, Serializable {

    private static final Logger logger = Logger.getLogger(
            DeliveryHandler.class.getCanonicalName());
    
    private static final long serialVersionUID = 4L;
    
    @EJB 
    OrderBean orderBean;
    
    @EJB
    OrderJMSManager orderPublisher;
    
    @Override
    @Asynchronous
    public void onNewOrder(@Observes @Paid OrderEvent event) {        
        logger.log(Level.FINEST, "{0} Event being processed by DeliveryHandler", 
                Thread.currentThread().getName());
       
        try {           
            logger.log(Level.INFO, "Order #{0} has been paid in the amount of {1}. "
                    + "Order is now ready for delivery!", 
                    new Object[]{event.getOrderID(), event.getAmount()});
                                    
            orderBean.setOrderStatus(
                    event.getOrderID(), 
                    String.valueOf(OrderBean.Status.READY_TO_SHIP.getStatus())
            );
            
            CustomerOrder order = null;
            try{
                order = orderBean.getOrderById(event.getOrderID());
                
                System.out.println("DeliveryHandler, onNewOrder, order: " 
                    + order.getIdCustomerOrder());
            }catch(Exception e){ 
                System.out.println("DeliveryHandler, onNewOrder, Exc.: " + e.getMessage()
                + "\nevent: " + event
                + "\nevent.getOrderID: " + event.getOrderID());
            }
            
            if (order != null) {
                orderPublisher.sendMessage(order);
               
            } else {
                throw new Exception("The order does not exist");
            }
        } catch (Exception jex) {
            logger.log(Level.SEVERE, null, jex);
        }
    }
}