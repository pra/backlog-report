package com.polopoly.jira;

import java.util.ArrayList;
import java.util.Collection;

import org.ofbiz.core.entity.GenericValue;

public class ReportUtil {
    public String testMethod(String s) {
        return s;
    }
    public Collection<String> filterComponents(Collection<GenericValue> col) {
        Collection<String> comps = new ArrayList<String>();
        for(GenericValue ge: col) {
            String name = ge.getString("name");
            if(name.startsWith("t:")) {
                comps.add(name);
            }  
        }
        return comps;
        
    }
}