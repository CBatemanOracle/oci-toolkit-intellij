package com.oracle.oci.intellij.api.oci;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import com.oracle.oci.intellij.api.oci.OCIModelPackage.MODELTYPE;

public abstract class OCIModelObject {

	private final MODELTYPE modelType;
	private final String compartmentId;
	private final String id;	
	private OCIModelObject parent;

	protected OCIModelObject(String id, String compartmentId, MODELTYPE modelType) {
		this.id = id;
		this.compartmentId = compartmentId;
		this.modelType = modelType;
	}

	public final boolean allowsChildren() {
		return this.modelType.allowsChildren();
	}
	
	public abstract Optional<List<? extends OCIModelObject>> getChildren();
	
	public final int getChildCount() {
		return getChildren().orElse(Collections.emptyList()).size();
	}
	
	public final Object getChildAt(int index) {
		return getChildren().orElseThrow().get(index);
	}
	
	public final OCIModelObject getParent() {
		return this.parent;
	}
	public final MODELTYPE getModelType() {
		return modelType;
	}

	public String getCompartmentId() {
		return compartmentId;
	}
	
	public String getId() {
		return id;
	}
	
	public boolean isContainer() {
		// containers override and set to true
		return false;
	}
	
	public MODELTYPE getContainsType() {
		return null;
	}
	
	public Class<?> getContainsClass() {
		return null;
	}

	public abstract String getDisplayName();

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof OCIModelObject) {
            return ((OCIModelObject)obj).getId().equals(this.getId());
        }
        return false;
    }

    @Override
    public String toString() {
        return getDisplayName() + "("+ modelType.getDefaultDisplayName() + ")";
    }

}
