package shoppay.store.ejb;

import shoppayentity.entity.*;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

/**
 *
 * This class accesses the database by using a 
 * javax.persistence.PersistenceContext object.
 * The id for a new product will be created in web.ProductController.java.
 * 
 * 
 * @author stefan.streifeneder@gmx.de
 */
@Stateless
public class ProductBean extends AbstractFacade<Product> {

    private static final Logger logger = 
            Logger.getLogger(ProductBean.class.getCanonicalName());
    
    @PersistenceContext
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public ProductBean() {
        super(Product.class);
    }

    public List<Product> findByCategory(int[] range, int categoryId) {       
         Category cat = new Category();
         cat.setIdCategory(categoryId);
         
         CriteriaBuilder qb = em.getCriteriaBuilder();
         CriteriaQuery<Product> query = qb.createQuery(Product.class);
         Root<Product> product = query.from(Product.class);
         query.where(qb.equal(product.get("category"), cat));

         List<Product> result = this.findRange(range, query);
         
         logger.log(Level.FINEST, "Product List size: {0}", result.size());
         
        return result;
    }
}