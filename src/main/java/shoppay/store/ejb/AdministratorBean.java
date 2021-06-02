package shoppay.store.ejb;

import shoppayentity.entity.*;
import shoppay.store.web.util.AuthenticationUtils;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;

/**
 *
 * The class is an implemeantion of the abstract class
 * a_test_forest.store.ejb.AbstractFacade. That class possesses a range of
 * non abstract methods, which are full implemented and available.
 * 
 * @author stefan.streifeneder@gmx.de
 */
@Stateless
public class AdministratorBean extends AbstractFacade<Administrator> {
    
    
    @PersistenceContext
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public AdministratorBean() {
        super(Administrator.class);
    }
    
//    @Override
//    public void create(Administrator admin) {
//        
//        System.out.println("AdminstartorBean, create, Start, "
//                + "admin: " + admin.getEmail());
//        try {
//            // password encrypt
//            admin.setPassword(
//                    AuthenticationUtils.encodeSHA256(admin.getPassword()));
//        } catch (UnsupportedEncodingException | NoSuchAlgorithmException ex) {
//            Logger.getLogger(AdministratorBean.class.getName()).log(Level.SEVERE, null, ex);
//        }
//                
//        //dtype
//        admin.setDtype("admin");
//                
//        //id
//        admin.setIdPerson(this.getIdNewPerson());
//                
//        Groups adminGroup = (Groups) em.createNamedQuery("Groups.findByName")
//                .setParameter("name", "admin")
//                .getSingleResult();
//        
//        PersonGroups pg = new PersonGroups();
//        pg.setEmail(admin.getEmail());
//        pg.setGroups(adminGroup);
//        pg.setIdPersonGroups(this.getIdNewPersonGroups()); 
//        pg.setPerson(admin);
//        em.persist(pg);
//        adminGroup.setPersonGroupsCollection(new ArrayList<PersonGroups>());
//        adminGroup.getPersonGroupsCollection().add(pg);
//        admin.setPersonGroupsCollection(new ArrayList<PersonGroups>());
//        admin.getPersonGroupsCollection().add(pg);
//        
//        em.persist(admin);
//        em.merge(adminGroup);
//        em.merge(pg);
//    }
    
    @Override
    public void remove(Administrator admin) {
        
        System.out.println("AdministratorBean, remove, Start, "
                + "admin: " + admin.getEmail());       
        
        Groups group = group = (Groups) em.createNamedQuery("Groups.findByName")
                .setParameter("name", "admin")
                .getSingleResult();
        
        PersonGroups pg = (PersonGroups) em.createNamedQuery("PersonGroups.findByEmail")
                .setParameter("email", admin.getEmail())
                .getSingleResult();
        
        group.getPersonGroupsCollection().remove(pg);
        
        em.remove(pg);
        em.remove(em.merge(admin));
        em.merge(group);  
    }
    
    
    /**
     * Retruns an ID for a new created person.
     * 
     * Author: steve
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
    
}