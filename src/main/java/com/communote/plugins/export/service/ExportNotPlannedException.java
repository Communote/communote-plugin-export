package com.communote.plugins.export.service;

/**
 * Exception indicating that an export is not planned although it should be
 * 
 * @author Communote GmbH - <a href="http://www.communote.com/">http://www.communote.com/</a>
 *
 */
public class ExportNotPlannedException extends Exception {

    /**
     * default serial version UID
     */
    private static final long serialVersionUID = 1L;

    /**
     * Create a new exception with a detail message
     *
     * @param message
     *            the details
     */
    public ExportNotPlannedException(String message) {
        super(message);
    }

}
