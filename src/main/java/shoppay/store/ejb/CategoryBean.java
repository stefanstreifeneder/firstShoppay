package shoppay.store.ejb;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import shoppayentity.entity.Category;

/**
 *
 * The class access the database filed 'Category'.
 * This class does not provide a method to create a new id,
 * this will be done in the controller class web.CategoryController.
 * 
 * 
 * 
 * 
 * @author stefan.streifeneder@gmx.de
 */
@Stateless
public class CategoryBean extends AbstractFacade<Category> {
    @PersistenceContext
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public CategoryBean() {
        super(Category.class);
    }
}