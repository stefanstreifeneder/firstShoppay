/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package shoppay.store.web.util;

import javax.faces.model.DataModel;


/**
 * 
 * Display a certain amount of items in a web-side
 * 
 * 
 * 
 * 
 * The xhtml file calls methods of the coresponding ControllerClass.
 * 
 * 
 * The Controller class provides variables and methods to manage to display a 
 * certain amount of items.
 * 
 * 
 * Additional to the controller class to accomplish the task to display a certain 
 * amount of items the system provides another class 'AbstractPaginationHelper' 
 * and a enum 'PageNavigation'.
 * 
 * 
 * Controller classes
 * There are the following controller classes:
 * AdministrationController
 * CategoryController
 * CustomerController
 * CustomerOrderController
 * GroupsController
 * OrderDetailController
 * OrderStatusController
 * ProductController
 * UserController (no display)
 * 
 * 
 * 
 * 
 * The controller class provides a method to instantiate an instance of the 
 * abstract class 'AbstractPaginationHelper'.
 * 
 * 
 * 
 * To obtain an object of type 'AbstractPaginationHelper' the Controller class 
 * creates an inner anonymous class of the abstract class 'AbstractPaginationHelper', 
 * whereby it uses the provided constructor of the abstract class. Within the 
 * inner anonymous class two methods has to be implemented, which are declared 
 * abstract by 'AbstractPaginationHelper', createPageDataModel() and 
 * getItemsCount().
 * 
 * 
 * 
 * One of the two abstract methods, which have to be implemented is used to create 
 * an appropriate DataModel object to display the items.
 * The other method, getItemsCount(), is used in the abstract class of methods 
 * which are non-abstract/implemented. It returns the number of items. 
 * The method 'getItemsCount()' is called in the abstract class of the method 
 * 'public boolean isHasNextPage() to evaluate whether there is another side 
 * necessary to display all items.
 * 
 * 
 * 
 * @author stefan.streifeneder@gmx.de
 */
public abstract class AbstractPaginationHelper {

    public static final int DEFAULT_SIZE = 10;
    private transient int  pageSize;
    private transient int page;

    public AbstractPaginationHelper(int pageSize) {
        this.pageSize = pageSize;
    }

    public abstract int getItemsCount();

    public abstract DataModel createPageDataModel();

    public int getPageFirstItem() {
        return page*pageSize;
    }

    public int getPageLastItem() {
        int i = getPageFirstItem() + pageSize -1;
        int count = getItemsCount() - 1;
        if (i > count) {
            i = count;
        }
        if (i < 0) {
            i = 0;
        }
        return i;
    }

    public boolean isHasNextPage() {
        return (page+1)*pageSize+1 <= getItemsCount();
    }

    public void nextPage() {
        if (isHasNextPage()) {
            page++;
        }
    }

    public boolean isHasPreviousPage() {
        return page > 0;
    }

    public void previousPage() {
        if (isHasPreviousPage()) {
            page--;
        }
    }

    public int getPageSize() {
        return pageSize;
    }

}