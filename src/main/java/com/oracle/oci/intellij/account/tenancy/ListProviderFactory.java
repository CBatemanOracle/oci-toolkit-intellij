package com.oracle.oci.intellij.account.tenancy;

import java.util.Map.Entry;
import java.util.function.BiFunction;

import org.jetbrains.annotations.NotNull;

import com.oracle.oci.intellij.resource.provider.CompartmentListProvider;
import com.oracle.oci.intellij.resource.provider.ListProvider;
import com.oracle.oci.intellij.resource.provider.RegionListProvider;

public class ListProviderFactory {

  public static ListProvider<?, ?> createCompartmentProvider(TenancyData tenancy, Class<?> source) {
    return new CompartmentListProvider(tenancy, source);
  }

  public static ListProvider<?,?> createRegionsProvider(TenancyData tenancy, Class<?> source) {
    return new RegionListProvider(tenancy, source);
  }

  public static ListProvider<?,?> createListProvider(ListTargets target, @NotNull TenancyData tenancy) {
    Class<?> source = getDefaultSource(target);
    BiFunction<TenancyData, Class<?>, ListProvider<?,?>> factoryMethod =
      target.getSources().get(source);
    return factoryMethod.apply(tenancy, source);
  }

  protected static Class<?> getDefaultSource(ListTargets target) {
    Entry<Class<?>, BiFunction<TenancyData, Class<?>, ListProvider<?,?>>> sources = 
      target.getDefault();
    return sources.getKey();
  }
}
