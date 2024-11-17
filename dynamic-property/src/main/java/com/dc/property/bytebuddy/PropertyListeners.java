package com.dc.property.bytebuddy;

import java.beans.PropertyChangeListener;
import java.util.List;

/**
 * Adds  property change listener on property change notification
 *
 * @author zy
 */
public interface PropertyListeners {


    void setListeners(List<PropertyChangeListener> listener);


    List<PropertyChangeListener> getListeners();


}
