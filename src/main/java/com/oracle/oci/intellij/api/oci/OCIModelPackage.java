package com.oracle.oci.intellij.api.oci;

public class OCIModelPackage {	
	public enum MODELTYPE {
		ANY("Any", true), DATABASE("Database", true), TENANCY("Tenancy", true), MODEL_CONTAINER("Container", true), COMPARTMENT("Compatment", true);
		
		private boolean allowsChildren;
        private String defaultDisplayName;
		
		MODELTYPE(String defaultDisplayName, boolean allowsChildren) {
		    this.defaultDisplayName = defaultDisplayName;
			this.allowsChildren = allowsChildren;
		}
		
		public String getDefaultDisplayName() {
            return defaultDisplayName;
        }

        public boolean allowsChildren() {
			return allowsChildren;
		}
	}
}
