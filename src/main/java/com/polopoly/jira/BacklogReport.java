package com.polopoly.jira;

import com.atlassian.core.util.map.EasyMap;
import com.atlassian.jira.bc.JiraServiceContext;
import com.atlassian.jira.bc.JiraServiceContextImpl;
import com.atlassian.jira.bc.filter.SearchRequestService;
import com.atlassian.jira.bc.issue.search.SearchService;
import com.atlassian.jira.config.properties.APKeys;
import com.atlassian.jira.config.properties.ApplicationProperties;
import com.atlassian.jira.config.properties.LookAndFeelBean;
import com.atlassian.jira.exception.PermissionException;
import com.atlassian.jira.issue.CustomFieldManager;
import com.atlassian.jira.issue.IssueFactory;
import com.atlassian.jira.issue.index.IssueIndexManager;
import com.atlassian.jira.issue.search.ReaderCache;
import com.atlassian.jira.issue.search.SearchException;
import com.atlassian.jira.issue.search.SearchProvider;
import com.atlassian.jira.issue.search.SearchRequest;
import com.atlassian.jira.issue.statistics.FilterStatisticsValuesGenerator;
import com.atlassian.jira.issue.statistics.FixForVersionStatisticsMapper;
import com.atlassian.jira.issue.statistics.StatisticsMapper;
import com.atlassian.jira.issue.statistics.StatsGroup;
import com.atlassian.jira.issue.statistics.util.OneDimensionalDocIssueHitCollector;
import com.atlassian.jira.jql.builder.JqlQueryBuilder;
import com.atlassian.jira.plugin.report.impl.AbstractReport;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.jira.project.version.Version;
import com.atlassian.jira.project.version.VersionManager;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.util.BuildUtilsInfo;
import com.atlassian.jira.util.velocity.DefaultVelocityRequestContextFactory;
import com.atlassian.jira.util.velocity.VelocityRequestContextFactory;
import com.atlassian.jira.web.FieldVisibilityManager;
import com.atlassian.jira.web.action.ProjectActionSupport;
import com.atlassian.jira.web.bean.FieldVisibilityBean;
import com.atlassian.jira.web.bean.PagerFilter;
import com.atlassian.jira.web.util.OutlookDateManager;
import com.atlassian.util.profiling.UtilTimerStack;
import com.opensymphony.user.User;
import com.opensymphony.util.TextUtils;
import static com.atlassian.jira.util.dbc.Assertions.notNull;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.search.HitCollector;

import webwork.action.ActionContext;

import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.http.HttpServletResponse;

public class BacklogReport extends AbstractReport {
    private static Log log = LogFactory.getLog(BacklogReport.class);

    private final SearchProvider searchProvider;
    private final JiraAuthenticationContext authenticationContext;
    private final SearchRequestService searchRequestService;
    private final IssueFactory issueFactory;
    private final CustomFieldManager customFieldManager;
    private final IssueIndexManager issueIndexManager;
    private final SearchService searchService;
    private final FieldVisibilityManager fieldVisibilityManager;
    private final ReaderCache readerCache;
    private final OutlookDateManager outlookDateManager;
    private final ProjectManager projectManager;
    private final VersionManager versionManager;
    private final BuildUtilsInfo buildUtilsInfo;

    private ApplicationProperties applicationProperties;

    public BacklogReport(final SearchProvider searchProvider,
        final JiraAuthenticationContext authenticationContext,
        final SearchRequestService searchRequestService,
        final IssueFactory issueFactory,
        final CustomFieldManager customFieldManager,
        final IssueIndexManager issueIndexManager,
        final SearchService searchService,
        final FieldVisibilityManager fieldVisibilityManager,
        final ReaderCache readerCache,
        final OutlookDateManager outlookDateManager,
        final ProjectManager projectManager,
        final VersionManager versionManager,
        final BuildUtilsInfo buildUtilsInfo,
        final ApplicationProperties applicationProperties)
    {
        this.searchProvider = searchProvider;
        this.authenticationContext = authenticationContext;
        this.searchRequestService = searchRequestService;
        this.issueFactory = issueFactory;
        this.customFieldManager = customFieldManager;
        this.issueIndexManager = issueIndexManager;
        this.searchService = searchService;
        this.fieldVisibilityManager = fieldVisibilityManager;
        this.readerCache = readerCache;
        this.outlookDateManager = outlookDateManager;
        this.projectManager = projectManager;
        this.versionManager = versionManager;
        this.buildUtilsInfo = notNull("buildUtilsInfo", buildUtilsInfo);
        this.applicationProperties = applicationProperties;
    }

    /**
     * Get the lucene based search result back, wrapped in a statistical container. 
     */
    public StatsGroup searchMapIssueKeys(SearchRequest request, User searcher,
        StatisticsMapper mapper) throws SearchException
    {
        try {
            UtilTimerStack.push("Search Count Map");
            StatsGroup statsGroup = new StatsGroup(mapper);
            HitCollector hitCollector =
                new OneDimensionalDocIssueHitCollector(mapper
                    .getDocumentConstant(), statsGroup, issueIndexManager
                    .getIssueSearcher().getIndexReader(), issueFactory,
                    fieldVisibilityManager, readerCache);
            searchProvider.searchAndSort((request != null) ? request.getQuery()
                : null, searcher, hitCollector, PagerFilter
                .getUnlimitedFilter());
            return statsGroup;
        } finally {
            UtilTimerStack.pop("Search Count Map");
        }
    }
    
    Map<String, Object> getParams(final ProjectActionSupport action, final Map reqParam) {
        final User remoteUser = action.getRemoteUser();
        final TextUtils textUtils = new TextUtils();
        final Map<String, Object> velocityParams = new HashMap<String, Object>();
        velocityParams.putAll(reqParam);
        velocityParams.put("textUtils", textUtils);
        velocityParams.put("versionManager", versionManager);
        velocityParams.put("remoteUser", remoteUser);
        
        // Excel view params
        final LookAndFeelBean lookAndFeelBean = LookAndFeelBean.getInstance(applicationProperties);

        final VelocityRequestContextFactory contextFactory = new DefaultVelocityRequestContextFactory(applicationProperties);

        String jiraLogo = lookAndFeelBean.getLogoUrl();
        final String jiraBaseUrl = contextFactory.getJiraVelocityRequestContext().getBaseUrl();
        if ((jiraLogo != null) && !jiraLogo.startsWith("http://") && !jiraLogo.startsWith("https://"))
        {
            jiraLogo = jiraBaseUrl + jiraLogo;
        }
        velocityParams.put("jiraLogo", jiraLogo);
        velocityParams.put("jiraLogoWidth", lookAndFeelBean.getLogoWidth());
        velocityParams.put("jiraLogoHeight", lookAndFeelBean.getLogoHeight());
        velocityParams.put("jiraTitle", applicationProperties.getString(APKeys.JIRA_TITLE));
        velocityParams.put("topBgColor", lookAndFeelBean.getTopBackgroundColour());
        velocityParams.put("buildInfo", buildUtilsInfo.getBuildInformation());
        velocityParams.put("buildNumber", buildUtilsInfo.getCurrentBuildNumber());
        velocityParams.put("createDate", new Date());
        velocityParams.put("jiraBaseUrl", jiraBaseUrl);

        return velocityParams;
    }
    
    /**
     * Get the params to populate the velocity context with.
     */
    Map getViewParams(ProjectActionSupport action, Map params) 
    throws Exception{
        // This seems to work only with Jira 4.4>=
        Long projectId = action.getSelectedProjectId();
        //System.err.println("DEBUG prId " + projectId);
        
        //XXX Might be needed for Jira 4.4<
        //String filterId = (String) params.get("filterid");
        
        String pId = (String)params.get("product");
        Long productId = Long.valueOf(pId);
        
        final StatisticsMapper mapper =
            new FixForVersionReleaseStatisticsMapper(versionManager);
        
        
        // Get search result
        SearchRequest request = null;
        final Project project = projectManager.getProjectObj(projectId);
        if (project != null)
        {
            request = makeProjectSearchRequest(project.getKey(), productId);
        } else {
            // XXX No TDD here
            throw new IllegalStateException("filterInsteadOfProject not supported. Might be in future");
        }        
        StatsGroup statsGroup = searchMapIssueKeys(request, authenticationContext
                                           .getUser(), mapper);
        
        // Prepare context
        final JiraServiceContext ctx =  
            new JiraServiceContextImpl(authenticationContext.getUser());
        final User remoteUser = action.getRemoteUser();
        final TextUtils textUtils = new TextUtils();
        
        
        // Excel view params
        final LookAndFeelBean lookAndFeelBean = LookAndFeelBean.getInstance(applicationProperties);

        final VelocityRequestContextFactory contextFactory = new DefaultVelocityRequestContextFactory(applicationProperties);
        
                    
            final Map startingParams =
                EasyMap.build("action", action, 
                              "statsGroup", statsGroup, 
                              "searchRequest", request, 
                              "customFieldManager", customFieldManager,
                              "fieldVisibility", new FieldVisibilityBean(),
                              "searchService", searchService, 
                              "portlet", this,
                              "textUtils", textUtils,
                              "versionManager", versionManager,
                              "remoteUser", remoteUser,
                              "projectId", projectId,
                              "reportUtil", new ReportUtil(),                             
                              "jiraTitle", applicationProperties.getString(APKeys.JIRA_TITLE),
                              "topBgColor", lookAndFeelBean.getTopBackgroundColour(),
                              "buildInfo", buildUtilsInfo.getBuildInformation(),
                              "buildNumber", buildUtilsInfo.getCurrentBuildNumber(),
                              "createDate", new Date());
            startingParams.put("outlookDate", outlookDateManager
                .getOutlookDate(authenticationContext.getLocale()));
            
            return getParams(action, startingParams);
        
        /*
        final SearchRequest request =
            searchRequestService.getFilter(ctx, new Long(filterId));
          */  
/*
        final Map startingParams = new HashMap();
        return descriptor.getHtml("view", startingParams);
        */       
    }
    

    public String generateReportHtml(ProjectActionSupport action, Map params)
        throws Exception
    {
        try {
            return descriptor.getHtml("view", getViewParams(action, params));
        } catch (PermissionException e) {
            log.error(e, e);
            return null;
        }

    }
    
    // Generate an EXCEL view of report
    @Override
    public String generateReportExcel(final ProjectActionSupport action, final Map reqParams) throws Exception
    {
        final StringBuffer contentDispositionValue = new StringBuffer(50);
        contentDispositionValue.append("attachment;filename=\"");
        contentDispositionValue.append(getDescriptor().getName()).append(".xls\";");

        // Add header to fix JRA-8484
        final HttpServletResponse response = ActionContext.getResponse();
        response.addHeader("content-disposition", contentDispositionValue.toString());
        return descriptor.getHtml("excel", getViewParams(action, reqParams));
    }
    
    @Override
    public boolean isExcelViewSupported()
    {
        return true;
    }

    public void validate(ProjectActionSupport action, Map params)
    {
        super.validate(action, params);
        /* XXX Here for memory 
        if (StringUtils.isEmpty((String) params.get("filterid"))) {
            action.addError("filterid", action
                .getText("report.singlelevelgroupby.filter.is.required"));
        }
        */
    }
    
    private SearchRequest makeProjectSearchRequest(String projectKey, Long productId)
    {   
        if(productId != ComponentValuesGenerator.ALL_ID) {
            return new SearchRequest(JqlQueryBuilder.newBuilder().where().project(projectKey).and().component(productId).buildQuery());        
        } else {
            return new SearchRequest(JqlQueryBuilder.newBuilder().where().project(projectKey).buildQuery());
        }
    }

    /**
     * A version lucene result mapper that only includes unreleased versions.
     */
    class FixForVersionReleaseStatisticsMapper extends FixForVersionStatisticsMapper {

        public FixForVersionReleaseStatisticsMapper(VersionManager versionManager)
        {
            super(versionManager, false);
 
        }
        
        public boolean isValidValue(Object value)
        {
            boolean valid = super.isValidValue(value);
            if(valid && value != null) {
                return !((Version)value).isReleased();
            } else {
                return valid;
            }
        }
    }
    
}
