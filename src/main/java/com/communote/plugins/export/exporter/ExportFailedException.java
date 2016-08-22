package com.communote.plugins.export.exporter;

/**
 * Exception to be thrown when an export failed
 *
 * @author Communote GmbH - <a href="http://www.communote.com/">http://www.communote.com/</a>
 *
 */
public class ExportFailedException extends Exception {

    /**
     * default serial version UID
     */
    private static final long serialVersionUID = 1L;

    /**
     * Create a new exception with a detail message
     *
     * @param message
     *            the detail message
     */
    public ExportFailedException(String message) {
        super(message);
    }

    /**
     * Create a new exception with a detail message and cause
     *
     * @param message
     *            the detail message
     * @param cause
     *            the cause of the exception
     */
    public ExportFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
