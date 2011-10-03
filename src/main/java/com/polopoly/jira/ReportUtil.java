package com.polopoly.jira;

import java.util.ArrayList;
import java.util.Collection;

import org.ofbiz.core.entity.GenericValue;

/*
 * A class of it own because I could not get Velocity to honour nested classes.
 */
public class ReportUtil {
    
    /**
     * Filter Components by removing components namespaced with "p:" (product) include only
     * components with "p:t" (theme).
     * If this results in an empty list, return original, but without "p:".
     */
    public Collection<String> filterGenericValueComponents(Collection<GenericValue> col) {
        
        return filterComponents( genericValueToString(col, "name"));
    }
    
    public Collection<String> filterComponents(Collection<String> col) {
        Collection<String> comps = new ArrayList<String>();
        Collection<String> backup = new ArrayList<String>();
        for(String name: col) {
            if(name.startsWith("t:")) {
                comps.add(name);
            }
            if(!name.startsWith("p:")) {
                backup.add(name);
            }
        }
        return comps.isEmpty() ? backup : comps;
        
    }
    
    // I have found no way of unit testing GenericValues
    private Collection<String> genericValueToString(Collection<GenericValue> values, String field) {
        Collection<String> comps = new ArrayList<String>();
        for(GenericValue ge: values) {
            comps.add(ge.getString(field));   
        }
        return comps;
    }
}