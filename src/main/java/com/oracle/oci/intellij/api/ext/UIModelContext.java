package com.oracle.oci.intellij.api.ext;

import com.oracle.oci.intellij.api.oci.OCIModelObject;

public class UIModelContext {
    private final OCIModelObject contextObject;

    public UIModelContext(OCIModelObject contextObject) {
        super();
        this.contextObject = contextObject;
    }

    public OCIModelObject getContextObject() {
        return contextObject;
    }
    
    
}
