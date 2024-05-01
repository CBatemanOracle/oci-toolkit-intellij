package com.oracle.oci.intellij.util.fills;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.CLASS;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Identifies a class that acts as a shim or contains shim methods.  Methods in a 
 * Shim class that act direct that as method call replacements should be marked with
 * the ShimMethod annotation.
 */
@Retention(CLASS)
@Target(TYPE)
public @interface Shim {
  public Class<?> forType() default Object.class;
  
  
  /**
   * @return true if the class is a dedicated shim class
   * or false if the class contains other non-shim methods.
   */
  public boolean dedicated() default true;
}
