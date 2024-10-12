package com.oracle.oci.intellij.api.oci;

import com.oracle.bmc.database.model.AutonomousDatabaseSummary;

public class OCIModelFactory {

	public static OCIDatabase createDatabase(AutonomousDatabaseSummary summary) {
		OCIDatabase db = new OCIDatabase(summary.getId(), summary.getCompartmentId());
		db.setDisplayName(summary.getDisplayName());
		return db;
	}
}
