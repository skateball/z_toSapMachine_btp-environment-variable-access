/*
 * Copyright (c) 2022 SAP SE or an SAP affiliate company. All rights reserved.
 */

package com.sap.cloud.environment.api;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.ServiceLoader;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public final class DefaultServiceBindingAccessor
{
    @Nonnull
    private static final ReadWriteLock instanceLock = new ReentrantReadWriteLock();
    @Nonnull
    private static ServiceBindingAccessor instance;

    static {
        instance = newDefaultInstance();
    }

    private DefaultServiceBindingAccessor()
    {
        throw new IllegalStateException("This utility class must not be instantiated.");
    }

    @Nonnull
    public static ServiceBindingAccessor getInstance()
    {
        instanceLock.readLock().lock();
        try {
            return instance;
        } finally {
            instanceLock.readLock().unlock();
        }
    }

    public static void setInstance( @Nullable final ServiceBindingAccessor accessor )
    {
        instanceLock.writeLock().lock();
        try {
            if (accessor != null) {
                instance = accessor;
            } else {
                instance = newDefaultInstance();
            }
        } finally {
            instanceLock.writeLock().unlock();
        }
    }

    @Nonnull
    private static ServiceBindingAccessor newDefaultInstance()
    {
        final ClassLoader classLoader = DefaultServiceBindingAccessor.class.getClassLoader();
        final ServiceLoader<ServiceBindingAccessor> serviceLoader = ServiceLoader.load(ServiceBindingAccessor.class,
                                                                                       classLoader);
        final Collection<ServiceBindingAccessor> accessors = StreamSupport.stream(serviceLoader.spliterator(), false)
                                                                          .collect(Collectors.toList());
        final ServiceBindingMerger bindingMerger = new ServiceBindingMerger(accessors,
                                                                            ServiceBindingMerger.KEEP_EVERYTHING);

        return new SimpleServiceBindingCache(bindingMerger);
    }
}
