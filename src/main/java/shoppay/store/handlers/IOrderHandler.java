package shoppay.store.handlers;

//import dukesforest.events.OrderEvent;
import shoppay.events.OrderEvent;

/**
 *
 * @author stefan.streifeneder@gmx.de
 */
public interface IOrderHandler  {
    
    public void onNewOrder(OrderEvent event);
    
}