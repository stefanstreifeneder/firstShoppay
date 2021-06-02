package shoppay.store.ejb;

import shoppayentity.entity.OrderDetail;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;

/**
 *
 * @author stefan.streifeneder@gmx.de
 */
@Stateless
public class OrderDetailBean extends AbstractFacade<OrderDetail> 
                                            implements Serializable {
    
    private static final long serialVersionUID = 5199196331433553237L;
    @PersistenceContext
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public OrderDetailBean() {
        super(OrderDetail.class);
    }
    
    public Integer createNewOrderDetailId(){
        TypedQuery<OrderDetail> query = em.createNamedQuery(
                    "OrderDetail.findAll", OrderDetail.class);
        List<OrderDetail> li = query.getResultList();
        List<Integer> liIm = new ArrayList<>();        
        for(OrderDetail i : li){
            liIm.add(i.getIdOrderDetail());
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
     * Example of usage of NamedQuery
     * @param orderId
     * @return 
     */
    public List<OrderDetail> findOrderDetailByOrder(int orderId) {
        List<OrderDetail> details = 
                getEntityManager().createNamedQuery(
                        "OrderDetail.findByIdOrderDetail").setParameter(
                                "idOrderDetail", orderId).getResultList();        
        return details;
    }
}