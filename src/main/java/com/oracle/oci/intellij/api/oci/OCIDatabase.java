package com.oracle.oci.intellij.api.oci;

import java.util.List;
import java.util.Optional;

public class OCIDatabase extends OCIModelObject {

	private String displayName;

	public OCIDatabase(String id, String compartmentId) {
		super(id, compartmentId, OCIModelPackage.MODELTYPE.DATABASE);
	}
	
	@Override
	public Optional<List<? extends OCIModelObject>> getChildren() {
		return Optional.empty();
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}
	public String getDisplayName() {
		return displayName;
	}

}
