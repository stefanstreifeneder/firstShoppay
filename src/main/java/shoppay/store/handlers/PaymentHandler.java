package shoppay.store.handlers;


import shoppay.store.ejb.OrderBean;

//import dukesforest.events.OrderEvent;
import shoppay.events.OrderEvent;
import shoppay.store.qualifiers.New;
import shoppay.store.qualifiers.Paid;
import shoppay.store.web.util.AuthenticationUtils;
import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.Asynchronous;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.enterprise.event.Event;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import shoppayentity.entity.Customer;
import shoppayentity.entity.OrderStatus;
import shoppay.store.ejb.OrderStatusBean;
import shoppay.store.ejb.UserBean;

/**
 * CDI event handler that calls Payment service for new orders. It will
 * intercept (observe) an
 * <code>OrderEvent</code> with
 * <code>@New</code> <b>qualifier</b>.
 *
 * @author stefan.streifeneder@gmx.de
 */
@Stateless
public class PaymentHandler implements IOrderHandler, Serializable {

    private static final Logger logger = Logger.getLogger(PaymentHandler.class.getCanonicalName());
    private static final long serialVersionUID = 47L;
    private static final String ENDPOINT =
//            "https://localhost:8181/z_test_forest_payment/payment/pay";//was running
//            "https://localhost:8181/steves_shop_allin_payment/payment";
            "http://localhost:8080/steves-shop-allin-payment/payment";
//            "https://localhost:8080/steves_shop_allin_payment/webresources";
    
    
    @Inject
    @Paid
    Event<OrderEvent> eventManager;
    
    /**
     * Payment service endpoint
     */
    @EJB
    OrderBean orderBean;
    
    
    @EJB
    UserBean userBean;

    @Override
    @Asynchronous
    public void onNewOrder(@Observes @New OrderEvent event) {
        
        System.out.println("PaymentHandler, onNewOrder, IF event: " + event
                    + "\norder-id: " + event.getOrderID()
                    + "\norderStatus-id: " + event.getStatusID());

        logger.log(Level.FINEST, "{0} Event being processed by PaymentHandler",
                Thread.currentThread().getName());

        if (this.processPayment(event)) {
            orderBean.setOrderStatus(event.getOrderID(),
                    String.valueOf(
                            OrderBean.Status.PENDING_PAYMENT.getStatus()));
            logger.info("Payment Approved");
            
            eventManager.fire(event);
            
        } else {
            // orig, but I do not like it.
//            orderBean.setOrderStatus(event.getOrderID(),
//                    String.valueOf(OrderBean.Status.CANCELLED_PAYMENT.getStatus()));
            logger.info("Payment Denied");
        }
    }

    private boolean processPayment(OrderEvent order) {
        boolean success = false;
        Client client = ClientBuilder.newClient();
        Customer c = this.userBean.find(order.getCustomerID());
        
        // client examination
        // int cardNumber = order.getCustomer().getCreditCardNumber();
        // String cardLoderName = order.getCustomer().getCreditCardHolder();
        // String mmYY = order.getCustomer().getMMyy();
        // int cardVerificationNumber = order.getCustomer().getCardVerificationNumber()
        
        client.register(new AuthClientRequestFilter(c.getEmail(), 
                c.getPassword()));
        
        WebTarget wt = client.target(ENDPOINT).path("pay"); 
        
        System.out.println("PaymentHandler, processPayment, "
                + wt.toString());
            
        Builder b = wt.request(MediaType.APPLICATION_XML);
            
        Response resp = b.post(Entity.entity(order, 
                    MediaType.APPLICATION_XML), Response.class);
        
        System.out.println("PaymentHandler, processPayment, "
                + resp.toString());
        
        // orig.
//        Response resp = client.target(ENDPOINT)
//                .request(MediaType.APPLICATION_XML)
//                .post(Entity.entity(order, MediaType.APPLICATION_XML), Response.class);
        
        int status = resp.getStatus();
        
        if (status == 200) {
            success = true;
        }
        
        logger.log(Level.INFO, "[PaymentHandler] Response status {0}", status);
        
        client.close();
        
        return success;
    }
    
    /* Client filter for basic HTTP auth */
    class AuthClientRequestFilter implements ClientRequestFilter {
        
        
        private final String user;
        private final String password;
        
        public AuthClientRequestFilter(String user, String password) {
            this.user = user;
            this.password = password;
        }
        
        @Override
        public void filter(ClientRequestContext requestContext) throws IOException {
            
            try {
                requestContext.getHeaders().add(
                        "Authorization",
                        "BASIC "
                                + this.user
                                + ":" + AuthenticationUtils.encodeSHA256(this.password));
                
                //orig
//                requestContext.getHeaders().add(
//                        "Authorization",
//                        "BASIC " + DatatypeConverter.printBase64Binary(
//                                   (user+":"+password).getBytes("UTF-8"))
//                );
                
                } catch (UnsupportedEncodingException | NoSuchAlgorithmException ex) {
                    System.out.println("PaymentHandler, AuthClientRequestFilter, "
                            + "filter Exc: " + ex.getMessage());
            
            }
        }
    }
}