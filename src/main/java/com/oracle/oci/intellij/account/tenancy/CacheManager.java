package com.oracle.oci.intellij.account.tenancy;

import java.util.HashMap;
import java.util.Map;

import com.oracle.bmc.identity.model.Compartment;
import com.oracle.bmc.identity.model.RegionSubscription;
import com.oracle.oci.intellij.resource.cache.CachedResourceFactory;
import com.oracle.oci.intellij.resource.request.CompartmentListRequestor;
import com.oracle.oci.intellij.resource.request.RegionSubscriptionListRequestor;

public class CacheManager {

  private Map<Class<?>, CachedResourceFactory<?>> caches = new HashMap<>();
  
  
  public CacheManager(TenancyData tenancyData) {
    // init caches
    caches.put(Compartment.class, 
                CachedResourceFactory.create(tenancyData, new CompartmentListRequestor(tenancyData)));
    caches.put(RegionSubscription.class, 
                CachedResourceFactory.create(tenancyData, new RegionSubscriptionListRequestor(tenancyData)));
  }


  public synchronized CachedResourceFactory<?> getCache(Class<?> targetType) {
    return caches.get(targetType);
  }
}
