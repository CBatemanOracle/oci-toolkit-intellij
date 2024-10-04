package com.oracle.oci.intellij.account.tenancy;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.BiFunction;

import com.oracle.bmc.identity.model.Compartment;
import com.oracle.bmc.identity.model.RegionSubscription;
import com.oracle.oci.intellij.resource.provider.ListProvider;
import com.oracle.oci.intellij.util.MapUtil;

public enum ListTargets {
  
//  TENANCY(Tenant.class, 
//    Map.ofEntries(Map.entry(TenancyData.class, ListProviderFactory::createTenantProvider))), 
  @SuppressWarnings("unchecked")
  REGIONS(RegionSubscription.class, 
    MapUtil.of(Map.entry(TenancyData.class, ListProviderFactory::createRegionsProvider))),
  @SuppressWarnings("unchecked")
  COMPARTMENTS(Compartment.class, 
    MapUtil.of(Map.entry(Compartment.class, ListProviderFactory::createCompartmentProvider)));
//    Map.ofEntries(Map.entry(Compartment.class, ListProviderFactory::createCompartmentProvider)));
  
  private final Class<?> target;
  LinkedHashMap<Class<?>, BiFunction<TenancyData, Class<?>, ListProvider<?,?>>> sources;
  private ListTargets(Class<?> target, 
                      LinkedHashMap<Class<?>, BiFunction<TenancyData, Class<?>, 
                        ListProvider<?,?>>> sourceToTargetFactory) {
    this.target = target;
    this.sources = sourceToTargetFactory;
  }
  protected Class<?> getTarget() {
    return target;
  }
 
  public Map.Entry<Class<?>, BiFunction<TenancyData, Class<?>, ListProvider<?,?>>> getDefault() {
    Iterator<Entry<Class<?>, BiFunction<TenancyData, Class<?>, ListProvider<?,?>>>> iterator =
      this.sources.entrySet().iterator();
    if (iterator.hasNext()) {
      return this.sources.entrySet().iterator().next();
    }
    throw new IllegalStateException("ListTargets should have at least one entry");
  }

  public Map<Class<?>, BiFunction<TenancyData, Class<?>, ListProvider<?,?>>> getSources() {
    return Collections.unmodifiableMap(sources);
  }

}