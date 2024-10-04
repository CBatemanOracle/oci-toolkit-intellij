package com.oracle.oci.intellij.resource;

public class NonExpiringResource<T> extends AbstractResource<T>{
    public NonExpiringResource(T content) {
        super(content);
    }

    @Override
    public boolean isValid() {
        return !isStale ;
    }
}
