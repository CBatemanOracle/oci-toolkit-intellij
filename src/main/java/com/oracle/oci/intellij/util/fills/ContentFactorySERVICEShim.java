package com.oracle.oci.intellij.util.fills;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.ui.content.ContentFactory;

/**
 * Replace SERVICE.getInstance() in ContentFactory.  It is deprecated in
 * later versions of the API in favor of a direct ContentFactory.getInstance.
 */
@Shim(forType = ContentFactory.class)
public class ContentFactorySERVICEShim {
  @ShimMethod(methodName = "SERVICE.getInstance()")
  public static ContentFactory getInstance() {
    return ApplicationManager.getApplication().getService(ContentFactory.class);
  }
}
