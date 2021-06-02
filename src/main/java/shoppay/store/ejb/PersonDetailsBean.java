/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package shoppay.store.ejb;

import javax.ejb.Stateless;
import javax.faces.bean.SessionScoped;
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import shoppayentity.entity.PersonDetails;

/**
 *
 * @author stefan
 */
@SessionScoped
@Stateless
@Named
public class PersonDetailsBean extends AbstractFacade<PersonDetails> {
    
    
    @PersistenceContext
    private EntityManager em;
    
    
    public PersonDetailsBean() {
        super(PersonDetails.class);
    }
    
    
    public Object getPersonDetailsByRequest(Integer idPersonDetails){
        TypedQuery<PersonDetails> query;
        PersonDetails personDetails;
        query = em.createNamedQuery("PersonDetails.findByIdPersonDetails", PersonDetails.class);
        query.setParameter("idPersonDetails", idPersonDetails);
        try{
            personDetails = query.getSingleResult();
        }catch(javax.persistence.NoResultException e){
            System.out.println("UserBean, getPersonByRequest, Exc: "
                    + e.getMessage());
            return null;
        }
        return personDetails;        
    }

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }
    
}
