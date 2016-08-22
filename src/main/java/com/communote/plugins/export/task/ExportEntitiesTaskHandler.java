package com.communote.plugins.export.task;

import java.util.Calendar;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.communote.plugins.export.service.ExportNotPlannedException;
import com.communote.plugins.export.service.ExportService;
import com.communote.plugins.export.service.ExportStatus;
import com.communote.plugins.export.service.impl.ExportServiceImpl;
import com.communote.server.api.core.application.CommunoteRuntime;
import com.communote.server.api.core.task.TaskTO;
import com.communote.server.core.tasks.ClientTaskHandler;

/**
 *
 * @author Communote GmbH - <a href="http://www.communote.com/">http://www.communote.com/</a>
 */
public class ExportEntitiesTaskHandler extends ClientTaskHandler {

    // property to provide a reschedule interval in minutes
    private static final String PROPERTY_RESCHEDULE_INTERVAL = "com.communote.plugins.export.task.reschedule";
    // default interval of 30 minutes for saas
    private static final int DEFAULT_RESCHEDULE_INTERVAL_SAAS = 30;
    // default interval of 10 minutes for standalone
    private static final int DEFAULT_RESCHEDULE_INTERVAL_STANDALONE = 10;
    private final Logger LOGGER = LoggerFactory.getLogger(ExportEntitiesTaskHandler.class);

    @Override
    public Date getRescheduleDate(Date now) {
        Calendar nextExecution = Calendar.getInstance();
        nextExecution.setTime(now);
        String property = System.getProperty(PROPERTY_RESCHEDULE_INTERVAL);
        int interval = CommunoteRuntime.getInstance().getApplicationInformation().isStandalone() ? DEFAULT_RESCHEDULE_INTERVAL_STANDALONE
                : DEFAULT_RESCHEDULE_INTERVAL_SAAS;
        if (property != null) {
            try {
                interval = Integer.parseInt(property);
            } catch (NumberFormatException e) {
                LOGGER.warn("The value of '{}' is not an integer ({})",
                        PROPERTY_RESCHEDULE_INTERVAL, property);
            }
        }
        nextExecution.add(Calendar.MINUTE, interval);
        return nextExecution.getTime();
    }

    @Override
    protected void runOnClient(TaskTO task) throws Exception {
        ExportService exportService = ExportServiceImpl.getInstance();
        if (exportService != null) {
            ExportStatus status = exportService.getExportStatus();
            if (ExportStatus.PLANNED.equals(status)) {
                try {
                    exportService.exportAllData();
                } catch (ExportNotPlannedException e) {
                    LOGGER.error("Unexpected exception exporting client data", e);
                }
            }
        } else {
            LOGGER.debug("Skipping task execution because plugin was stopped or removed");
        }
    }

}
