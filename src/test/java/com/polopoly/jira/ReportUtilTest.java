package com.polopoly.jira;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Before;
import org.junit.Test;
import org.ofbiz.core.entity.GenericPK;
import org.ofbiz.core.entity.GenericValue;
import org.ofbiz.core.entity.model.ModelEntity;

import com.atlassian.jira.util.ofbiz.GenericValueUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


public class ReportUtilTest {
    private ReportUtil reportUtil;

    @Before
    public void init() {
        reportUtil = new ReportUtil();
    }
    
    @Test
    public void testFilterP() {
        Collection<String> col = reportUtil.filterComponents(Arrays.asList("p:product"));
        assertTrue("Should not have returned p: " + col, col.isEmpty());
    }
    
    @Test
    public void testReturnT() {
        Collection<String> col = reportUtil.filterComponents(Arrays.asList("t:theme"));
        assertFalse("Should have returned t: " + col, col.isEmpty());
    }
    @Test
    public void testReturnMultipleT() {
        Collection<String> col = reportUtil.filterComponents(Arrays.asList("t:theme", "t:theme2", "other", "p:product"));
        assertEquals("Should have returned two t:s: " + col, 2, col.size());
    }
    
    @Test
    public void testNoT() {
        Collection<String> col = reportUtil.filterComponents(Arrays.asList("other", "p:product"));
        assertEquals("Should have returned only other: " + col, 1, col.size());
    }
    
}
