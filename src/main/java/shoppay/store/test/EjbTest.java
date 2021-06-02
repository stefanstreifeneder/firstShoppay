/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package shoppay.store.test;

import shoppayentity.entity.*;
import java.util.List;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaQuery;

//@Stateless
@Named
@ManagedBean
//@RequestScoped//does not work
@SessionScoped
//@DeclareRoles({"admin, user"})
//@RolesAllowed({"admin, user"})
//@ServletSecurity(
// @HttpConstraint(transportGuarantee = ServletSecurity.TransportGuarantee.CONFIDENTIAL,
//    rolesAllowed = {"admin, user"}))
public class EjbTest {
    
    
    @PersistenceContext//(unitName="xtest.forest.entities_xtest_d_forest_entities_war_1.0PU")
    private EntityManager em;
    
//    private CriteriaBuilder cb;
    
    private String test = "Shoppay-SHOP";

    /**
     * Get the value of test
     *
     * @return the value of test
     */
    public String getTest() {
        return test;
    }

    /**
     * Set the value of test
     *
     * @param test new value of test
     */
    public void setTest(String test) {
        this.test = test;
    }

    
    
//    @PostConstruct
//    private void init() {
//        cb = em.getCriteriaBuilder();
//    }
    
//    @RolesAllowed({"admin, user"})
    public String testDB(){
//        cb = em.getCriteriaBuilder();
        
        System.out.println("ShoppayTest, testDB, START, "
                + "em: " + em
//                + " - cb: " + cb
        );
        
        String toReturn = "";
        
        
        CriteriaQuery<Person> cq = null;
        TypedQuery<Person> q = null;
        List<Person> personList = null;
        
        
//        try{
//            cq = cb.createQuery(Person.class);
////            cq = em.getCriteriaBuilder().createQuery(Person.class);
//        }catch(Exception e){
//            e.printStackTrace();
//        }
//
//        System.out.println("EjbTest, testDB, START - 1");
//        
//        try{
//            q = em.createQuery(cq);
//        }catch(Exception e){
//            e.printStackTrace();
//        }

        System.out.println("EjbTest, testDB, START - 2");
        try{
            TypedQuery<Person> tq = em.createNamedQuery("Person.findAll", Person.class);
            
            personList = tq.getResultList();
        }catch(Exception e){
            e.printStackTrace();
        }
        
        System.out.println("EjbTest, testDB, START - 3"
                + " - personList: " + personList);
        
        try{
            for(Person p : personList){
                System.out.println("EjbTest, testDB, person: " + p.getEmail());
                toReturn = p.getEmail();
            }
        }catch(Exception e){
            e.printStackTrace();
        }
        
        
        
        System.out.println("EjbTest, testDB, ende");
        
        //return toReturn;
        return "testEntity.xhtml";
    }
    
    
    
    
}
