package com.oracle.oci.intellij.util;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

public class MapUtil {

  public static <K,V> LinkedHashMap<K, V> of(Map.Entry<K, V>...entries) {
    LinkedHashMap<K,V> newMap = new LinkedHashMap<K, V>();
    Arrays.stream(entries).forEach(entry -> newMap.put(entry.getKey(), entry.getValue()));
    return newMap;
  }
}
