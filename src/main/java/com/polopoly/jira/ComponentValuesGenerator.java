package com.polopoly.jira;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.ofbiz.core.entity.GenericValue;

import com.atlassian.configurable.ValuesGenerator;
import com.atlassian.jira.bc.project.component.ProjectComponent;
import com.atlassian.jira.bc.project.component.ProjectComponentManager;
import com.atlassian.jira.component.ComponentAccessor;
/**
 * Generate a Map of component filtered on prefix to use for building a query.
 * 
 * @author pra
 *
 */
public abstract class ComponentValuesGenerator implements ValuesGenerator {

    public static final long ALL_ID = 0L;
    public static final String ALL = "-- All --";
    

    @Override
    public Map getValues(Map params)
    {
        System.err.println("DEBUG params " + params);
        
        GenericValue projectGV = (GenericValue) params.get("project");
        ProjectComponentManager componentManager =  ComponentAccessor.getProjectComponentManager();
        Collection<ProjectComponent> components =  componentManager.findAllForProject(projectGV.getLong("id"));
        Map<Long, String> products = new HashMap<Long, String>();
        products.put(ALL_ID, ALL);
        for(ProjectComponent pc: components) {
            if(pc.getName().startsWith(getPrefixFilter())) {
                products.put(pc.getId(), getProductDesc(pc)); 
            }
          
        }
        return products;
        
    }
    
    String getProductDesc(ProjectComponent pc) {
        String desc = pc.getDescription();
        String name = pc.getName();
        if (desc != null && desc.length() > 0) {
            return desc + " (" + name + ")";
        }
        else {
            return name;
        }
    }
    
    public abstract String getPrefixFilter();

}
