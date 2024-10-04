package com.oracle.oci.intellij.account;

import java.lang.management.ManagementFactory;

import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.IntrospectionException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.ReflectionException;

import com.oracle.oci.intellij.resource.cache.CachedResourceFactory;
import com.oracle.oci.intellij.resource.cache.CachedResourceFactory.LRUCacheMBean;

public class CacheUtil {
  public static class LRUCacheMBeanImpl implements LRUCacheMBean {

    private MBeanServer mbeanServer;
    private CachedResourceFactory<?> cachedResourceFactory;
    private ObjectName objectName;

    public LRUCacheMBeanImpl(CachedResourceFactory<?> cachedResourceFactory) {
      this.cachedResourceFactory = cachedResourceFactory;
      this.mbeanServer = ManagementFactory.getPlatformMBeanServer();
      this.objectName = this.cachedResourceFactory.getObjectName();
    }

    @Override
    public long getHits() {
      return (long) getAttribute("Hits");
    }

    @Override
    public long getMisses() {
      return (long) getAttribute("Misses");
    }

    @Override
    public double getCacheHitRatio() {
      return (double) getAttribute("CacheHitRatio");
    }

    protected Object getAttribute(String attrName) {
      try {
        return this.mbeanServer.getAttribute(objectName, attrName);
      } catch (InstanceNotFoundException | AttributeNotFoundException
               | ReflectionException | MBeanException e) {
        throw new RuntimeException(e);
      }
    }

    public MBeanAttributeInfo[] getAttributes() throws IntrospectionException,
                                                InstanceNotFoundException,
                                                ReflectionException {
      MBeanInfo mbeanInfo = this.mbeanServer.getMBeanInfo(objectName);
      return mbeanInfo.getAttributes();
    }
  }
}
