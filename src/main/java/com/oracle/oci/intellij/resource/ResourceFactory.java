package com.oracle.oci.intellij.resource;

import java.time.Duration;
import java.util.Objects;

import com.oracle.oci.intellij.resource.parameter.ParameterSet;

public interface ResourceFactory<TO> {
    public Resource<TO> request(ParameterSet parent) throws Exception;

    static <T> Resource<T> createExpiringResource(
            T content, Duration expireTime) {
        Objects.requireNonNull(content, "value is null");
        Objects.requireNonNull(expireTime, "expireTime is null");

        return new ExpiringResource<>(content, expireTime);
    }


    static <T> Resource<T> createNonExpiringResource(T value) {
        Objects.requireNonNull(value, "value is null");
        return new NonExpiringResource<>(value);
    }
}
