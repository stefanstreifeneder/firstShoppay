package shoppay.store.ejb;

//import dukesforest.events.OrderEvent;
import shoppay.events.OrderEvent;
import shoppay.store.qualifiers.New;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.Asynchronous;
import javax.ejb.Stateless;
import javax.enterprise.event.Event;
import javax.inject.Inject;
import javax.inject.Named;

/**
 *
 * @author stefan.streifeneder@gmx.de
 */
@Named("EventDisptacherBean")
@Stateless
public class EventDispatcherBean {

     private static final Logger logger = Logger.getLogger(
             EventDispatcherBean.class.getCanonicalName());

    
    @Inject 
    @New
    Event<OrderEvent> eventManager;

    @Asynchronous
    public void publish(OrderEvent event) {
        
        
        System.out.println("EventDispatcherBean, event: " + event);
        
        
        logger.log(Level.FINEST, "{0} Sending event from EJB", 
                Thread.currentThread().getName());
        eventManager.fire(event);
    }
}