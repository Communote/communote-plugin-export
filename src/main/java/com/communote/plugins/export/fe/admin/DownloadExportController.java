package com.communote.plugins.export.fe.admin;

import java.io.File;
import java.io.FileInputStream;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;

import com.communote.plugins.core.views.annotations.UrlMapping;
import com.communote.plugins.export.service.ExportService;
import com.communote.plugins.export.service.ExportStatus;

/**
 * Controller for downloading a completed export.
 *
 * @author Communote GmbH - <a href="http://www.communote.com/">http://www.communote.com/</a>
 */
@Component
@Provides
@Instantiate(name = "DownloadExportController")
@UrlMapping(value = "/*/admin/export/download")
public class DownloadExportController implements Controller {

    private static final Logger LOGGER = LoggerFactory.getLogger(DownloadExportController.class);

    @Requires
    private ExportService exportService;

    /**
     * Checks, if the file was modified since the last request.
     *
     * @param request
     *            The request.
     * @param file
     *            The file to check.
     * @return True, if the file was modified.
     */
    private boolean checkWasModified(HttpServletRequest request, File file) {
        long modifiedSince = request.getDateHeader("If-Modified-Since");
        return modifiedSince == -1 || modifiedSince < (file.lastModified() / 60000 * 60000);
    }

    @Override
    public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response)
            throws Exception {
        ExportStatus status = exportService.getExportStatus();
        if (ExportStatus.COMPLETED.equals(status)) {
            File file = exportService.getExportResult();
            if (file != null && file.exists()) {

                if (!checkWasModified(request, file)) {
                    response.setStatus(304);
                    return null;
                }
                String contentDispositionHeader = "attachment; filename=\""
                        + file.getName() + "\"; ";
                long filesize = file.length();
                if (filesize > 0) {
                    contentDispositionHeader += "size=" + filesize + ";";
                    response.setContentLength((int) filesize);
                }
                response.setDateHeader("Last-Modified", file.lastModified());
                response.setContentType("application/zip");
                response.setHeader("Content-Disposition", contentDispositionHeader);
                response.setHeader("Content-Description", file.getName());
                response.flushBuffer();

                ServletOutputStream out = response.getOutputStream();
                try (FileInputStream fileInputStream = new FileInputStream(file)) {
                    IOUtils.copy(fileInputStream, out);
                }
                out.flush();
                return null;
            } else {
                LOGGER.warn("Export file {} does not exist", file);
            }
        } else {
            LOGGER.debug("Export not completed");
        }
        response.sendError(HttpServletResponse.SC_NOT_FOUND);
        return null;
    }

}
