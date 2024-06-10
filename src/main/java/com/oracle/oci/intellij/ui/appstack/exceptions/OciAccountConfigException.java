package com.oracle.oci.intellij.ui.appstack.exceptions;

/**
 * Exception thrown when there is an issue with the OCI account configuration file.
 */
public class OciAccountConfigException extends RuntimeException {

    /**
     * Constructs a new OciAccountConfigException with the specified detail message.
     *
     * @param message the detail message
     */
    public OciAccountConfigException(String message) {
        super(message);
    }

    /**
     * Constructs a new OciAccountConfigException with the specified detail message and cause.
     *
     * @param message the detail message
     * @param cause the cause
     */
    public OciAccountConfigException(String message, Throwable cause) {
        super(message, cause);
    }
}
