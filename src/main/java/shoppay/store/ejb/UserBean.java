package shoppay.store.ejb;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.ejb.Stateless;
import javax.faces.bean.SessionScoped;
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import shoppayentity.entity.*;

@SessionScoped
@Stateless
@Named
public class UserBean  extends AbstractFacade<Customer> {
    
    
    @PersistenceContext
    private EntityManager em;
    
    
    public UserBean() {
        super(Customer.class);
    }
    
    
    public Object getPersonByRequest(String email){
        TypedQuery<Person> query;
        Person user;
        query = em.createNamedQuery("Person.findByEmail", Person.class);
        query.setParameter("email", email);
        try{
            user = query.getSingleResult();
        }catch(javax.persistence.NoResultException e){
            System.out.println("UserBean, getPersonByRequest, Exc: "
                    + e.getMessage());
            return null;
        }
        return user;        
    }

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }
    
    /**
     * Retruns an ID for a new created person.
     * 
     * @return int New ID for a created person.
     */
    public int getIdNewPerson(){
        TypedQuery<Person> query = em.createNamedQuery(
                    "Person.findAll", Person.class);
        List<Person> li = query.getResultList();
        List<Integer> liIm = new ArrayList<>();        
        for(Person i : li){
            liIm.add(i.getIdPerson());
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
    
    
    @Override
    public void remove(Customer user) {        
        Groups group = (Groups) em.createNamedQuery("Groups.findByName")
                .setParameter("name", "user")
                .getSingleResult();        
        PersonGroups pg = (PersonGroups) em.createNamedQuery("PersonGroups.findByEmail")
                .setParameter("email", user.getEmail())
                .getSingleResult();        
        group.getPersonGroupsCollection().remove(pg);
        
        
        PersonDetails pd = (PersonDetails) em.createNamedQuery("PersonDetails.findByIdPersonDetails")
                .setParameter("idPersonDetails", user.getIdPerson())
                .getSingleResult();       
        
        
        em.remove(pd);
        em.remove(pg);
        em.remove(em.merge(user));
        em.merge(group);        
    }
    
}
