package com.oracle.oci.intellij.resource;

abstract public class AbstractResource<T> implements Resource<T>{
    final T content;
    volatile boolean isStale;


    public AbstractResource(T content) {
        this.content = content;
    }

    @Override
    public final T getContent() {
        return content;
    }
    @Override
    abstract public boolean isValid() ;

    public void makeStale() {
        this.isStale = true;
    }
}
