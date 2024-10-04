package com.oracle.oci.intellij.resource;

import java.time.Duration;
import java.time.OffsetDateTime;

public class ExpiringResource<T> extends AbstractResource<T> {

    private final OffsetDateTime creationTime;
    private final Duration expireDuration;


    public ExpiringResource(T value, Duration expireDuration) {
        super(value);
        this.expireDuration = expireDuration;
        this.creationTime = OffsetDateTime.now();
    }

    public boolean isValid() {
        return OffsetDateTime.now().isBefore(creationTime.plus(expireDuration)) && !isStale;
    }
}
