package com.communote.plugins.export.service;

import java.io.File;

import com.communote.server.api.core.security.AuthorizationException;

/**
 *
 * @author Communote GmbH - <a href="http://www.communote.com/">http://www.communote.com/</a>
 */
public interface ExportService {

    void exportAllData() throws ExportNotPlannedException;

    /**
     * Get the zip file that contains the exported data of the current client.
     * 
     * @return the zip file or null if the export has not yet completed
     * @throws AuthorizationException
     *             in case the current user is not client manager
     */
    File getExportResult() throws AuthorizationException;

    /**
     * @return the status of the export of the current client. If no export was started or planned
     *         null is returned.
     */
    ExportStatus getExportStatus();

    /**
     * Plan an export for the current client.
     *
     * @return true if the export was planned or false if it has already been planned or is running
     * @throws AuthorizationException
     *             in case the current user is not manager of the client
     */
    boolean planExport() throws AuthorizationException;

}
