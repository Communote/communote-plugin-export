package com.communote.plugins.export.serializer;

/**
 * Exception to be thrown when the serializer has a problem writing to the stream.
 *
 * @author Communote GmbH - <a href="http://www.communote.com/">http://www.communote.com/</a>
 */
public class SerializerWriteException extends Exception {

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
    public SerializerWriteException(String message, Throwable cause) {
        super(message, cause);
    }
}
