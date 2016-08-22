package com.communote.plugins.export.fe.admin;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.osgi.framework.BundleContext;
import org.springframework.web.servlet.mvc.Controller;

import com.communote.plugins.core.views.AdministrationViewController;
import com.communote.plugins.core.views.ViewControllerException;
import com.communote.plugins.core.views.annotations.Page;
import com.communote.plugins.core.views.annotations.UrlMapping;
import com.communote.plugins.export.service.ExportService;
import com.communote.plugins.export.service.ExportStatus;
import com.communote.server.api.core.security.AuthorizationException;

/**
 * Controller to plan an export of Communote data.
 *
 * @author Communote GmbH - <a href="http://www.communote.com/">http://www.communote.com/</a>
 *
 */
@Component
@Provides
@Instantiate(name = "CreateExportController")
@UrlMapping(value = "/*/admin/export/manage")
@Page(menu = "extensions", submenu = "export", menuMessageKey = "plugins.export.administration.menu.title",
jsCategories = { "communote-core", "admin" }, cssCategories = { "admin" })
public class CreateExportController extends AdministrationViewController implements Controller {

    @Requires
    private ExportService exportService;

    public CreateExportController(BundleContext bundleContext) {
        super(bundleContext);
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response,
            Map<String, Object> model) throws ViewControllerException {
        ExportStatus status = exportService.getExportStatus();
        model.put("isPlanned", ExportStatus.PLANNED.equals(status));
        model.put("isRunning", ExportStatus.RUNNING.equals(status));
        model.put("isCompleted", ExportStatus.COMPLETED.equals(status));
        model.put("isFailed", ExportStatus.FAILED.equals(status));
    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response,
            Map<String, Object> model) throws ViewControllerException {
        String action = request.getParameter("action");
        if ("plan".equals(action)) {
            try {
                exportService.planExport();
                doGet(request, response, model);
            } catch (AuthorizationException e) {
                throw new ViewControllerException(HttpServletResponse.SC_FORBIDDEN, e.getMessage(),
                        e);
            }
        }
    }

    @Override
    public String getContentTemplate() {
        return "/vm/create-export.html.vm";
    }

}
