package shoppay.store.ejb;


import shoppayentity.entity.PersonGroups;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;



/**
 *
 * Custom class by Steve.
 * 
 * 
 * @author stefan.streifeneder@gmx.de
 */
@Stateless
public class PersonGroupsBean extends AbstractFacade<PersonGroups> {
    @PersistenceContext
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public PersonGroupsBean() {
        super(PersonGroups.class);
    }
    
    public int getIdNewPersonGroups(){
        TypedQuery<PersonGroups> query = em.createNamedQuery(
                    "PersonGroups.findAll", PersonGroups.class);
        List<PersonGroups> li = query.getResultList();
        List<Integer> liIm = new ArrayList<>();        
        for(PersonGroups i : li){
            liIm.add(i.getIdPersonGroups());
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
    
}