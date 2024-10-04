package com.oracle.oci.intellij.resource.provider;

public class ProviderUtil {
  @SuppressWarnings("unchecked")
  public static <T, S> ListProvider<T, S> cast(ListProvider<?, ?> listProvider) {
    // add optional checking logic
    return (ListProvider<T, S>) listProvider;
  }

}
