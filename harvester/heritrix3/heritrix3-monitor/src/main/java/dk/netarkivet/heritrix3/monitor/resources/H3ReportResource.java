package dk.netarkivet.heritrix3.monitor.resources;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import javax.servlet.ServletContext;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.netarchivesuite.heritrix3wrapper.StreamResult;
import org.netarchivesuite.heritrix3wrapper.jaxb.Job;
import org.netarchivesuite.heritrix3wrapper.jaxb.Report;

import com.antiaction.common.filter.Caching;
import com.antiaction.common.templateengine.TemplateBuilderFactory;

import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.harvester.HarvesterSettings;
import dk.netarkivet.heritrix3.monitor.Heritrix3JobMonitor;
import dk.netarkivet.heritrix3.monitor.NASEnvironment;
import dk.netarkivet.heritrix3.monitor.NASUser;
import dk.netarkivet.heritrix3.monitor.ResourceAbstract;
import dk.netarkivet.heritrix3.monitor.ResourceManagerAbstract;

public class H3ReportResource implements ResourceAbstract {

    private NASEnvironment environment;

    protected int R_REPORT = -1;

    @Override
    public void resources_init(NASEnvironment environment) {
        this.environment = environment;
    }

    @Override
    public void resources_add(ResourceManagerAbstract resourceManager) {
        R_REPORT = resourceManager.resource_add(this, "/job/<numeric>/report/", false);
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
        if (resource_id == R_REPORT) {
            if ("GET".equals(method)) {
                report(req, resp, numerics);
            }
        }
    }

    public void report(HttpServletRequest req, HttpServletResponse resp, List<Integer> numerics) throws IOException {
        Locale locale = resp.getLocale();
        resp.setContentType("text/html; charset=UTF-8");
        ServletOutputStream out = resp.getOutputStream();
        Caching.caching_disable_headers(resp);

        TemplateBuilderFactory<MasterTemplateBuilder> masterTplBuilderFactory = TemplateBuilderFactory.getInstance(environment.templateMaster, "master.tpl", "UTF-8", MasterTemplateBuilder.class);
        MasterTemplateBuilder masterTplBuilder = masterTplBuilderFactory.getTemplateBuilder();

        StringBuilder sb = new StringBuilder();

        String reportStr = req.getParameter("report");

        long jobId = numerics.get(0);
        Heritrix3JobMonitor h3Job = environment.h3JobMonitorThread.getRunningH3Job(jobId);
        Job job;

        if (h3Job != null && h3Job.isReady()) {
            if (h3Job.jobResult != null && h3Job.jobResult.job != null) {
                job = h3Job.jobResult.job;
                Report report;
                for (int i=0; i<job.reports.size(); ++i) {
                    report = job.reports.get(i);
                    if (i > 0) {
                        sb.append("&nbsp;");
                    }
                    sb.append("<a href=\"");
                    sb.append(NASEnvironment.servicePath);
                    sb.append("job/");
                    sb.append(h3Job.jobId);
                    sb.append("/report/?report=");
                    sb.append(report.className);
                    sb.append("\" class=\"btn btn-default\">");
                    sb.append(report.shortName);
                    sb.append("</a>");
                }
                if (reportStr != null && reportStr.length() > 0) {
                    sb.append("<br />\n");
                    sb.append("<h5>");
                    sb.append(reportStr);
                    sb.append("</h5>");
                    sb.append("<pre>");
                    StreamResult anypathResult = h3Job.h3wrapper.path("job/" + h3Job.jobname + "/report/" + reportStr, null, null);
                    byte[] tmpBuf = new byte[8192];
                    int read;
                    try {
                        while ((read = anypathResult.in.read(tmpBuf)) != -1) {
                            sb.append(new String(tmpBuf, 0, read));
                        }
                        anypathResult.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    sb.append("</pre>");
                }
            }
        }

        StringBuilder menuSb = masterTplBuilder.buildMenu(new StringBuilder(), h3Job);

        masterTplBuilder.insertContent("Job "+ jobId + " Reports", menuSb.toString(), environment.generateLanguageLinks(locale), "Job " + jobId + " Reports", sb.toString(),
        		"<meta http-equiv=\"refresh\" content=\""+Settings.get(HarvesterSettings.HARVEST_MONITOR_REFRESH_INTERVAL)+"\"/>\n").write(out);

        out.flush();
        out.close();
    }

}
