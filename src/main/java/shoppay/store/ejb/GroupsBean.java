package shoppay.store.ejb;

import shoppayentity.entity.Groups;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 *
 * @author stefan.streifeneder@gmx.de
 */
@Stateless
public class GroupsBean extends AbstractFacade<Groups> {
    @PersistenceContext
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public GroupsBean() {
        super(Groups.class);
    }
    
}