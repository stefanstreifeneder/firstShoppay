package shoppay.store.ejb;

import shoppayentity.entity.OrderStatus;
import java.io.Serializable;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

/**
 *
 * @author stefan.streifeneder@gmx.de
 */
@Stateless
public class OrderStatusBean extends AbstractFacade<OrderStatus> implements Serializable {
    
    private static final long serialVersionUID = 5199196331433553237L;
    @PersistenceContext
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public OrderStatusBean() {
        super(OrderStatus.class);
    }

    
    //??????????????????????????????????????????????????????????????????????
    // This method does not make sense.
    // The return type should be List<OrderStatus>
    public OrderStatus getStatusByName(String status) {
        Query createNamedQuery = getEntityManager().createNamedQuery(
                "OrderStatus.findByStatus");
        createNamedQuery.setParameter("status", status);
        return (OrderStatus) createNamedQuery.getSingleResult();
    }
}