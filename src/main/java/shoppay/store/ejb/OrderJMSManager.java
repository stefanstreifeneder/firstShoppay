/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package shoppay.store.ejb;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.Stateless;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.ObjectMessage;
import javax.jms.Session;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import shoppayentity.entity.CustomerOrder;

/**
 *
 * The class is regular created, but the methods 
 * 'void sendJMSMessageToMyForestQueue()' and 
 * 'javax.jms.Message createJMSMessageForjmsMyForestQueue()'
 * are created by insert Code - Send JMS Message.
 * 
 * @author stefan.streifeneder@gmx.de
 */
@Stateless
public class OrderJMSManager {

    private javax.jms.Message createJMSMessageForjmsMyShopPayQueue(javax.jms.Session session, Object messageData) throws javax.jms.JMSException {
        // TODO create and populate message to send
        javax.jms.TextMessage tm = session.createTextMessage();
        tm.setText(messageData.toString());
        return tm;
    }

    private void sendJMSMessageToMyShopPayQueue(CustomerOrder messageData) throws javax.jms.JMSException, NamingException {
        Context c = new InitialContext();
        javax.jms.ConnectionFactory cf = 
                (javax.jms.ConnectionFactory) c.lookup(
                        "java:comp/DefaultJMSConnectionFactory");
        javax.jms.Connection conn = null;
        javax.jms.Session s = null;
        try {
            conn = cf.createConnection();
            s = conn.createSession(false, Session.AUTO_ACKNOWLEDGE);
            javax.jms.Destination destination = 
                    (javax.jms.Destination) c.lookup(
                            "jms/myShopPayQueue");
            javax.jms.MessageProducer mp = s.createProducer(destination);
            ObjectMessage message = s.createObjectMessage(); 
            
            System.out.println("ShopPay - OrderJMSManager, "
                 + "sendJMSMessageToShopOneQueue, "
                + "order: " + messageData);
            
            
            message.setObject(messageData);
            mp.send(message);
        }catch(JMSException | NamingException e){            
            System.out.println("ShopPay - OrderJMSManager, "
                    + "sendJMSMessageToMyShopOneQueue, Exc: " + e.getMessage());
        } finally {
            if (s != null) {
                try {
                    s.close();
                } catch (Exception e) {
                    Logger.getLogger(this.getClass().getName()).log(Level.WARNING, "Cannot close session", e);
                }
            }
            if (conn != null) {
                conn.close();
            }
        }
    }
    
    public void sendMessage(CustomerOrder customerOrder) {
        System.out.println("ShopPay OderJMSManager, sendMessage");
        try {
            this.sendJMSMessageToMyShopPayQueue(customerOrder);
        } catch (JMSException | NamingException ex) {
            Logger.getLogger(OrderJMSManager.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public void deleteMessage(int orderID) throws Exception {
        
        System.out.println("ShopPay OderJMSManager, deleteMessage, Start, id: "
                + orderID);
        
        Context c = new InitialContext();
        javax.jms.ConnectionFactory cf = 
                (javax.jms.ConnectionFactory) c.lookup(
                        "java:comp/DefaultJMSConnectionFactory");
        javax.jms.Connection conn = null;
        javax.jms.Session s = null;
        try {
            conn = cf.createConnection();
            s = conn.createSession(false, Session.AUTO_ACKNOWLEDGE);
            conn.start();
            javax.jms.Destination destination = 
                    (javax.jms.Destination) c.lookup(
                            "jms/myShopPayQueue");
            
            MessageConsumer mc = s.createConsumer(destination);
            
            Message m = mc.receive(2000);
            
            System.out.println("ShopPay OderJMSManager, deleteMessage, msg: " + m
                        + " - consumer: " + mc);
            // orig
//            javax.jms.MessageProducer mp = s.createProducer(destination);
//            ObjectMessage message = s.createObjectMessage();   
//            message.setObject(messageData);
//            mp.send(message);
            
            
            
        }catch(JMSException | NamingException e){            
            System.out.println("ShopPay - OrderJMSManager, "
                    + "sendJMSMessageToShopOneQueue, Exc: " + e.getMessage());
        } finally {
            if (s != null) {
                try {
                    s.close();
                } catch (Exception e) {
                    Logger.getLogger(this.getClass().getName()).log(Level.WARNING, "Cannot close session", e);
                }
            }
            if (conn != null) {
                conn.close();
            }
        }
    }
    
    
    
}
