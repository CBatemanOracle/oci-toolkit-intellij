package com.oracle.oci.intellij.ui.database;

import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

public class ADBConstants {
	public static final String ALWAYS_FREE_STORAGE_TB = "0.02";
	public static final int ALWAYS_FREE_CPU_CORE_COUNT = 1;
	public static final String ALWAYS_FREE_STORAGE_TB_DUMMY = "1";
	
	public static final int CPU_CORE_COUNT_MIN = 1;
	public static final int CPU_CORE_COUNT_MAX = 128;
	public static final int CPU_CORE_COUNT_DEFAULT = 1;
	public static final int CPU_CORE_COUNT_INCREMENT = 1;
	
	public static final int STORAGE_IN_TB_MIN = 1;
	public static final int STORAGE_IN_TB_MAX = 128;
	public static final int STORAGE_IN_TB_DEFAULT = 1;
	public static final int STORAGE_IN_TB_INCREMENT = 1;
	
	/* ADB Actions */
	
	public static final String CREATE_ADW_INSTANCE = "Create ADW Instance";
	public static final String CREATE_ATP_INSTANCE = "Create ATP Instance";
	public static final String REGISTER_DRIVER = "Register JDBC Driver";
			
	public static final String START = "Start";
	public static final String STOP = "Stop";
	public static final String CREATECLONE = "Create Clone";
	public static final String SCALEUPDOWN = "Scale Up/Down";
	public static final String UPDATELICENCETYPE = "Update Licence Type";
	public static final String ADMINPASSWORD = "Admin Password";
	public static final String TERMINATE = "Terminate";
	public static final String DOWNLOADWALLET = "Download Client Credentials (Wallet)";
	public static final String CREATECONNECTION = "Create Connection";
	public static final String RESTORE = "Restore";
	
	private static final Set<String> ACTION_SET = new TreeSet<String>();
	static {
		ACTION_SET.add(START);
		ACTION_SET.add(STOP);
		ACTION_SET.add(CREATECLONE);
		ACTION_SET.add(SCALEUPDOWN);
		ACTION_SET.add(UPDATELICENCETYPE);
		ACTION_SET.add(ADMINPASSWORD);
		ACTION_SET.add(TERMINATE);
		ACTION_SET.add(DOWNLOADWALLET);
		ACTION_SET.add(CREATECONNECTION);
		ACTION_SET.add(RESTORE);
	}
	
	public static Set<String> getSupportedADBActions() {
		return Collections.unmodifiableSet(ACTION_SET);
	}

	public static final String INSTANCE_WALLET = "Instance Wallet";
	public static final String REGIONAL_WALLET = "Regional Wallet";

}
