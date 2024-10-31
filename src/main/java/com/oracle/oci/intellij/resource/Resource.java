package com.oracle.oci.intellij.resource;

public interface Resource<T> {
    T getContent();
    boolean isValid();
    //TODO: override in sub-class.
    default void dispose(){

    }
    void makeStale();
}
