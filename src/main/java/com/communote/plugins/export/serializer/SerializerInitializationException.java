package com.communote.plugins.export.serializer;

/**
 * Exception to be thrown if the initialization of a serializer failed
 *
 * @author Communote GmbH - <a href="http://www.communote.com/">http://www.communote.com/</a>
 *
 */
public class SerializerInitializationException extends Exception {

    /**
     * default serial version UID
     */
    private static final long serialVersionUID = 1L;

    /**
     * Create a new exception with a detail message and cause
     * 
     * @param message
     *            the detail message
     * @param cause
     *            the cause of the exception
     */
    public SerializerInitializationException(String message, Throwable cause) {
        super(message, cause);
    }
}
