package dk.netarkivet.heritrix3.monitor.resources;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import javax.servlet.ServletContext;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.netarchivesuite.heritrix3wrapper.ScriptResult;

import com.antiaction.common.filter.Caching;
import com.antiaction.common.templateengine.TemplateBuilderFactory;

import dk.netarkivet.heritrix3.monitor.Heritrix3JobMonitor;
import dk.netarkivet.heritrix3.monitor.NASEnvironment;
import dk.netarkivet.heritrix3.monitor.NASUser;
import dk.netarkivet.heritrix3.monitor.ResourceAbstract;
import dk.netarkivet.heritrix3.monitor.ResourceManagerAbstract;

public class H3FilterResource implements ResourceAbstract {

    private NASEnvironment environment;

    protected int R_FILTER = -1;
    
    protected int R_BUDGET = -1;

    @Override
    public void resources_init(NASEnvironment environment) {
        this.environment = environment;
    }

    @Override
    public void resources_add(ResourceManagerAbstract resourceManager) {
        R_FILTER = resourceManager.resource_add(this, "/job/<numeric>/filter/", false);
    }

    @Override
    public void resource_service(ServletContext servletContext, NASUser nas_user, HttpServletRequest req, HttpServletResponse resp, int resource_id, List<Integer> numerics, String pathInfo) throws IOException {
        if (NASEnvironment.contextPath == null) {
            NASEnvironment.contextPath = req.getContextPath();
        }
        if (NASEnvironment.servicePath == null) {
            NASEnvironment.servicePath = req.getContextPath() + req.getServletPath() + "/";
        }
        String method = req.getMethod().toUpperCase();
        if(resource_id == R_FILTER) {
        	if ("GET".equals(method) || "POST".equals(method)) {
                filter_add(req, resp, numerics);
            }
        }
    }

    public void filter_add(HttpServletRequest req, HttpServletResponse resp, List<Integer> numerics) throws IOException {
    	Locale locale = resp.getLocale();
    	resp.setContentType("text/html; charset=UTF-8");
        ServletOutputStream out = resp.getOutputStream();
        Caching.caching_disable_headers(resp);

        TemplateBuilderFactory<MasterTemplateBuilder> masterTplBuilderFactory = TemplateBuilderFactory.getInstance(environment.templateMaster, "master.tpl", "UTF-8", MasterTemplateBuilder.class);
        MasterTemplateBuilder masterTplBuilder = masterTplBuilderFactory.getTemplateBuilder();

        StringBuilder sb = new StringBuilder();

        String regex = req.getParameter("regex");
        if (regex == null) {
            regex = "";
        }
        String[] removeIndexes = req.getParameterValues("removeIndex");
        if(removeIndexes == null) {
        	removeIndexes = new String[0];
        }
        
        String initials = "";
        if(req.getParameter("add-filter") != null) {
        	initials = req.getParameter("initials1");
        } else if(req.getParameter("remove-filter") != null) {
        	initials = req.getParameter("initials2");
        }
        if (initials == null) {
    		initials = "";
    	}

        String script = environment.NAS_GROOVY_SCRIPT;

        if (regex.length() > 0 && !initials.isEmpty()) {
        	String[] lines = regex.split(System.getProperty("line.separator"));
        	for(String line : lines) {
        		if(line.endsWith(System.getProperty("line.separator")) || line.endsWith("\r") || line.endsWith("\n")) {
        			line = line.substring(0, line.length() - 1);
        		}
	        	script += "\ninitials = \"" + initials + "\"";
	            script += "\naddFilter '" + line.replace("\\", "\\\\") + "'\n";
        	}
        }
        if(removeIndexes.length > 0 && !initials.isEmpty()) {
        	script += "\ninitials = \"" + initials + "\"";
            script += "\nremoveFilters("+Arrays.toString(removeIndexes)+")\n";
        }
        script += "\nshowFilters()\n";

        long jobId = numerics.get(0);
        Heritrix3JobMonitor h3Job = environment.h3JobMonitorThread.getRunningH3Job(jobId);

        if (h3Job != null && h3Job.isReady()) {
            /* form control */
            /* case submit for delete but no checked regex */
            boolean keepRegexTextArea = false;
            if (req.getParameter("remove-filter") != null && removeIndexes.length == 0) {
                sb.append("<div class=\"notify notify-red\"><span class=\"symbol icon-error\"></span> Check RejectRules to delete!</div>");
            }
            /* case submit for add but no text */
            if (req.getParameter("add-filter") != null && regex.isEmpty()) {
                sb.append("<div class=\"notify notify-red\"><span class=\"symbol icon-error\"></span> RejectRules cannot be empty!</div>");
            }
            /* case no initials */
            if ((req.getParameter("remove-filter") != null || req.getParameter("add-filter") != null) && initials.isEmpty()) {
                sb.append("<div class=\"notify notify-red\"><span class=\"symbol icon-error\"></span> Initials required to add/delete RejectRules!</div>");
                keepRegexTextArea = true;
            }
            
            sb.append("<p>All URIs matching any of the following regular expressions will be rejected from the current job.</p>");

            sb.append("<form class=\"form-horizontal\" action=\"?\" name=\"insert_form\" method=\"post\" enctype=\"application/x-www-form-urlencoded\" accept-charset=\"utf-8\">\n");
            sb.append("<label for=\"regex\" style=\"cursor: default;\">Expressions to reject:</label>");
            sb.append("<textarea rows=\"4\" cols=\"100\" id=\"regex\" name=\"regex\" placeholder=\"regex\">");
            if(keepRegexTextArea) {
            	sb.append(regex);
            }
            sb.append("</textarea>\n");
            sb.append("<label for=\"initials\">User initials:</label>");
            sb.append("<input type=\"text\" id=\"initials1\" name=\"initials1\" value=\"" + initials  + "\" placeholder=\"initials\">\n");
            sb.append("<button type=\"submit\" name=\"add-filter\" value=\"1\" class=\"btn btn-success\"><i class=\"icon-white icon-thumbs-up\"></i> Add</button>\n");
            sb.append("<br/>\n");

            ScriptResult scriptResult = h3Job.h3wrapper.ExecuteShellScriptInJob(h3Job.jobResult.job.shortName, "groovy", script);

            if (scriptResult != null && scriptResult.script != null && scriptResult.script.htmlOutput != null) {
            	sb.append("<div style=\"font-size: 14px; font-weight: normal; line-height: 20px;\">\n");
                sb.append("<p style=\"margin-top: 30px;\">Rejected regex:</p>\n");
            	sb.append(scriptResult.script.htmlOutput);
            	sb.append("</div>\n");
            	sb.append("<label for=\"initials\">User initials:</label>");
                sb.append("<input type=\"text\" id=\"initials2\" name=\"initials2\" value=\"" + initials  + "\" placeholder=\"initials\">\n");
                sb.append("<button type=\"submit\" name=\"remove-filter\" value=\"1\" class=\"btn btn-success\"><i class=\"icon-white icon-remove\"></i> Remove</button>");
            }
            
            sb.append("</form>\n");
        } else {
            sb.append("Job ");
            sb.append(jobId);
            sb.append(" is not running.");
        }

        StringBuilder menuSb = masterTplBuilder.buildMenu(new StringBuilder(), h3Job);

        masterTplBuilder.insertContent("Job " + jobId + " RejectRules", menuSb.toString(), environment.generateLanguageLinks(locale),
        		"Job " + jobId + " RejectRules", sb.toString(), "").write(out);

        out.flush();
        out.close();
    }
    
}
