package com.communote.plugins.export.service;

/**
 * Status of an export of the data of the current client
 *
 * @author Communote GmbH - <a href="http://www.communote.com/">http://www.communote.com/</a>
 *
 */
public enum ExportStatus {

    /**
     * the export is planned
     */
    PLANNED,
    /**
     * the export is currently running
     */
    RUNNING,
    /**
     * the export is completed
     */
    COMPLETED,

    /**
     * The export was started but failed
     */
    FAILED;
}
