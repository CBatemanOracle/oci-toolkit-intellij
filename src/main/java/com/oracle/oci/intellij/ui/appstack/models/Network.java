package com.oracle.oci.intellij.ui.appstack.models;

import com.oracle.oci.intellij.ui.appstack.actions.PropertyOrder;
import com.oracle.oci.intellij.ui.appstack.annotations.VariableMetaData;

import java.beans.PropertyVetoException;

public class Network extends VariableGroup {

    private boolean create_new_vcn;

    private java.lang.Object vcn_compartment_id;

    private java.lang.Object existing_vcn_id;

    private java.lang.String vcn_cidr;

    private boolean use_existing_app_subnet;

    private java.lang.Object existing_app_subnet_id;

    private java.lang.String app_subnet_cidr;

    private boolean use_existing_db_subnet;
    private java.lang.Object existing_db_subnet_id;

    private java.lang.String db_subnet_cidr;

    private boolean use_existing_lb_subnet;

    private java.lang.Object existing_lb_subnet_id;

    private java.lang.String lb_subnet_cidr;

    private boolean open_https_port;


    private boolean use_reserved_ip_address;
    private String  reserved_ip_address;
    private java.lang.Object certificate_ocid;


    private boolean use_default_lb_configuration;

    private int maximum_bandwidth_in_mbps;

    private int minimum_bandwidth_in_mbps;

    private java.lang.String health_checker_url_path;

    private int health_checker_return_code;

    private boolean enable_session_affinity;
    private static final String DOCUMENTATION_LINK = "https://github.com/oracle-quickstart/appstack/blob/main/usage_instructions.md#network";

    @Override
    public String getDocumentationLink() {
        return DOCUMENTATION_LINK;
    }

    public enum Session_affinity{
        Enable_application_cookie_persistence,
        Enable_load_balancer_cookie_persistence,
    }
    private Session_affinity session_affinity;

    private java.lang.String session_affinity_cookie_name;

    @PropertyOrder(1)
    @VariableMetaData(title="Create new VCN",defaultVal="true",type="boolean",required=true)

    public boolean isCreate_new_vcn() {
        return create_new_vcn;
    }
    @PropertyOrder(2)
    @VariableMetaData(title="The compartment of the existing VCN.",defaultVal="compartment_ocid",type="oci:identity:compartment:id",required=true,visible="not(create_new_vcn)")

    public Object getVcn_compartment_id() {
        return vcn_compartment_id;
    }
    @PropertyOrder(3)
    @VariableMetaData(title="Select to VCN",type="oci:core:vcn:id",dependsOn="{compartmentId=${vcn_compartment_id}}",required=true,visible="not(create_new_vcn)")

    public Object getExisting_vcn_id() {
        return existing_vcn_id;
    }
    @PropertyOrder(4)
    @VariableMetaData(title="VCN IPv4 CIDR Blocks",description="This VCN will be used for all resources created by the stack.",defaultVal="10.0.0.0/24",type="string",required=true,visible="create_new_vcn",pattern="^(25[0-5]|2[0-4][0-9]|1[0-9][0-9]|[1-9]?[0-9]).(25[0-5]|2[0-4][0-9]|1[0-9][0-9]|[1-9]?[0-9]).(25[0-5]|2[0-4][0-9]|1[0-9][0-9]|[1-9]?[0-9]).(25[0-5]|2[0-4][0-9]|1[0-9][0-9]|[1-9]?[0-9])\\/(3[0-2]|[1-2]?[0-9])$")

    public String getVcn_cidr() {
        return vcn_cidr;
    }
    @PropertyOrder(5)
    @VariableMetaData(title="Use existing Application Subnet",defaultVal="false",type="boolean",required=true,visible="not(create_new_vcn)")

    public boolean isUse_existing_app_subnet() {
        return use_existing_app_subnet;
    }
    @PropertyOrder(6)
    @VariableMetaData(title="Select the application subnet",type="oci:core:subnet:id",dependsOn="{compartmentId=${vcn_compartment_id}, vcnId=${existing_vcn_id}, hidePublicSubnet=true}",required=true,visible="use_existing_app_subnet")

    public Object getExisting_app_subnet_id() {
        return existing_app_subnet_id;
    }
    @PropertyOrder(7)
    @VariableMetaData(title="Application Subnet IPv4 CIDR Blocks",description="The container instances running the application will be created in this subnet.",defaultVal="10.0.0.0/25",type="string",required=true,visible="not(use_existing_app_subnet)",pattern="^(25[0-5]|2[0-4][0-9]|1[0-9][0-9]|[1-9]?[0-9]).(25[0-5]|2[0-4][0-9]|1[0-9][0-9]|[1-9]?[0-9]).(25[0-5]|2[0-4][0-9]|1[0-9][0-9]|[1-9]?[0-9]).(25[0-5]|2[0-4][0-9]|1[0-9][0-9]|[1-9]?[0-9])\\/(3[0-2]|[1-2]?[0-9])$")

    public String getApp_subnet_cidr() {
        return app_subnet_cidr;
    }
    @PropertyOrder(8)
    @VariableMetaData(title="Use existing Database Subnet",defaultVal="false",type="boolean",required=true,visible="and(not(create_new_vcn),not(use_existing_database))")

    public boolean isUse_existing_db_subnet() {
        return use_existing_db_subnet;
    }
    @PropertyOrder(9)
    @VariableMetaData(title="Select the database subnet",type="oci:core:subnet:id",dependsOn="{compartmentId=${vcn_compartment_id}, vcnId=${existing_vcn_id}, hidePublicSubnet=true}",required=true,visible="use_existing_db_subnet")

    public Object getExisting_db_subnet_id() {
        return existing_db_subnet_id;
    }
    @PropertyOrder(10)
    @VariableMetaData(title="Database Subnet Creation: IPv4 CIDR Blocks",description="The Autonomous Database will be created in this subnet. For example: 10.0.0.128/26",defaultVal="10.0.0.128/26",type="string",required=true,visible="and(not(use_existing_db_subnet),not(use_existing_database))",pattern="^(25[0-5]|2[0-4][0-9]|1[0-9][0-9]|[1-9]?[0-9]).(25[0-5]|2[0-4][0-9]|1[0-9][0-9]|[1-9]?[0-9]).(25[0-5]|2[0-4][0-9]|1[0-9][0-9]|[1-9]?[0-9]).(25[0-5]|2[0-4][0-9]|1[0-9][0-9]|[1-9]?[0-9])\\/(3[0-2]|[1-2]?[0-9])$")

    public String getDb_subnet_cidr() {
        return db_subnet_cidr;
    }
    @PropertyOrder(11)
    @VariableMetaData(title="Use existing Load Balancer Subnet",defaultVal="false",type="boolean",required=true,visible="not(create_new_vcn)")

    public boolean isUse_existing_lb_subnet() {
        return use_existing_lb_subnet;
    }
    @PropertyOrder(12)
    @VariableMetaData(title="Select the load balancer subnet",type="oci:core:subnet:id",dependsOn="{compartmentId=${vcn_compartment_id}, vcnId=${existing_vcn_id}, hidePublicSubnet=false}",required=true,visible="use_existing_lb_subnet")

    public Object getExisting_lb_subnet_id() {
        return existing_lb_subnet_id;
    }
    @PropertyOrder(13)
    @VariableMetaData(title="Load balancer Subnet IPv4 CIDR Blocks",description="The load balancer will be created in this subnet.",defaultVal="10.0.0.192/26",type="string",required=true,visible="not(use_existing_lb_subnet)",pattern="^(25[0-5]|2[0-4][0-9]|1[0-9][0-9]|[1-9]?[0-9]).(25[0-5]|2[0-4][0-9]|1[0-9][0-9]|[1-9]?[0-9]).(25[0-5]|2[0-4][0-9]|1[0-9][0-9]|[1-9]?[0-9]).(25[0-5]|2[0-4][0-9]|1[0-9][0-9]|[1-9]?[0-9])\\/(3[0-2]|[1-2]?[0-9])$")

    public String getLb_subnet_cidr() {
        return lb_subnet_cidr;
    }
    @PropertyOrder(14)
    @VariableMetaData(title="Open load balancer's HTTPS port",description="By checking this checkbox you agree to make the load balancer subnet public and to open the HTTPS port of the load balancer to the Internet.",defaultVal="false",type="boolean",required=true)
    public boolean isOpen_https_port() {
        return open_https_port;
    }
    @PropertyOrder(15)
    @VariableMetaData(title="Use a reserved IP address",type="boolean",required=true,visible = "open_https_port")
    public boolean isUse_reserved_ip_address() {
        return use_reserved_ip_address;
    }

    public void setUse_reserved_ip_address(boolean use_reserved_ip_address) {
        this.use_reserved_ip_address = use_reserved_ip_address;
    }
    @PropertyOrder(16)
    @VariableMetaData(title="Reserved IP address",description="Pre-created public IP that will be used as the IP of this load balancer. This reserved IP will not be deleted when load balancer is deleted. This ip should not be already mapped to any other resource.",visible = "and(open_https_port,use_reserved_ip_address)",type="string",required=true)
    public String getReserved_ip_address() {
        return reserved_ip_address;
    }

    public void setReserved_ip_address(String reserved_ip_address) {
        this.reserved_ip_address = reserved_ip_address;
    }

    @PropertyOrder(17)
    @VariableMetaData(title="Certificate OCID",description="You must have a SSL certificate available in OCI Certificates service. Provide the certificate OCID for the host name,that will be used to configure the load balancer.",type="oci:certificatesmanagement:certificate:id",required=false,visible="open_https_port")
    public Object getCertificate_ocid() {
        return certificate_ocid;
    }

    @PropertyOrder(18)
    @VariableMetaData(title="Use default load balancer configuration",defaultVal="true",type="boolean",required=true)

    public boolean isUse_default_lb_configuration() {
        return use_default_lb_configuration;
    }
    @PropertyOrder(19)
    @VariableMetaData(title="Maximum bandwidth (Mbps)",description="10Mbps for always free load balancer",defaultVal="10",type="number",required=true,visible="not(use_default_lb_configuration)")

    public int getMaximum_bandwidth_in_mbps() {
        return maximum_bandwidth_in_mbps;
    }
    @PropertyOrder(20)
    @VariableMetaData(title="Minimum bandwidth (Mbps)",description="10Mbps for always free load balancer",defaultVal="10",type="number",required=true,visible="not(use_default_lb_configuration)")

    public int getMinimum_bandwidth_in_mbps() {
        return minimum_bandwidth_in_mbps;
    }
    @PropertyOrder(21)
    @VariableMetaData(title="URL path (URI)",description="This url will be used by the health checker to verify that the application is running",defaultVal="/",type="string",required=true,visible="not(use_default_lb_configuration)")

    public String getHealth_checker_url_path() {
        return health_checker_url_path;
    }
    @PropertyOrder(22)
    @VariableMetaData(title="Status code",description="Status code returned by the health checker url when the application is running",defaultVal="200",type="number",required=true,visible="not(use_default_lb_configuration)")

    public int getHealth_checker_return_code() {
        return health_checker_return_code;
    }
    @PropertyOrder(23)
    @VariableMetaData(title="Enable cookie-based session persistence",defaultVal="false",type="boolean",required=true,visible="not(use_default_lb_configuration)")

    public boolean isEnable_session_affinity() {
        return enable_session_affinity;
    }
    @PropertyOrder(24)
    @VariableMetaData(title="Session persistence",description="Specify whether the cookie is generated by your application server or by the load balancer.",type="enum",required=true,enumValues ="Enable application cookie persistence, Enable load balancer cookie persistence",visible="enable_session_affinity")

    public Session_affinity getSession_affinity() {
        return session_affinity;
    }
    @PropertyOrder(25)
    @VariableMetaData(title="Cookie name",defaultVal="X-Oracle-BMC-LBS-Route",type="string",required=true,visible="enable_session_affinity")

    public String getSession_affinity_cookie_name() {
        return session_affinity_cookie_name;
    }

    public void setCreate_new_vcn(boolean newValue) throws PropertyVetoException {
        Object oldValue = this.create_new_vcn;
        this.create_new_vcn = newValue;
        pcs.firePropertyChange("create_new_vcn", oldValue, newValue);
        vcp.fireVetoableChange("create_new_vcn", oldValue, newValue);
    }

    public void setVcn_compartment_id(Object newValue) throws PropertyVetoException {
        Object oldValue = this.vcn_compartment_id;
        this.vcn_compartment_id = newValue;
        pcs.firePropertyChange("vcn_compartment_id", oldValue, newValue);
        vcp.fireVetoableChange("vcn_compartment_id", oldValue, newValue);
    }

    public void setExisting_vcn_id(Object newValue) throws PropertyVetoException {
        Object oldValue = this.existing_vcn_id;
        this.existing_vcn_id = newValue;
        pcs.firePropertyChange("existing_vcn_id", oldValue, newValue);
        vcp.fireVetoableChange("existing_vcn_id", oldValue, newValue);
    }

    public void setVcn_cidr(String newValue) throws PropertyVetoException {
        Object oldValue = this.vcn_cidr;
        this.vcn_cidr = newValue;
        pcs.firePropertyChange("vcn_cidr", oldValue, newValue);
        vcp.fireVetoableChange("vcn_cidr", oldValue, newValue);
    }

    public void setUse_existing_app_subnet(boolean newValue) throws PropertyVetoException {
        Object oldValue = this.use_existing_app_subnet;
        this.use_existing_app_subnet = newValue;
        pcs.firePropertyChange("use_existing_app_subnet", oldValue, newValue);
        vcp.fireVetoableChange("use_existing_app_subnet", oldValue, newValue);
    }

    public void setExisting_app_subnet_id(Object newValue) throws PropertyVetoException {
        Object oldValue = this.existing_app_subnet_id;
        this.existing_app_subnet_id = newValue;
        pcs.firePropertyChange("existing_app_subnet_id", oldValue, newValue);
        vcp.fireVetoableChange("existing_app_subnet_id", oldValue, newValue);
    }

    public void setApp_subnet_cidr(String newValue) throws PropertyVetoException {
        Object oldValue = this.app_subnet_cidr;
        this.app_subnet_cidr = newValue;
        pcs.firePropertyChange("app_subnet_cidr", oldValue, newValue);
        vcp.fireVetoableChange("app_subnet_cidr", oldValue, newValue);
    }

    public void setUse_existing_db_subnet(boolean newValue) throws PropertyVetoException {
        Object oldValue = this.use_existing_db_subnet;
        this.use_existing_db_subnet = newValue;
        pcs.firePropertyChange("use_existing_db_subnet", oldValue, newValue);
        vcp.fireVetoableChange("use_existing_db_subnet", oldValue, newValue);
    }

    public void setExisting_db_subnet_id(Object newValue) throws PropertyVetoException {
        Object oldValue = this.use_existing_db_subnet;
        this.existing_db_subnet_id = newValue;
        pcs.firePropertyChange("existing_db_subnet_id", oldValue, newValue);
        vcp.fireVetoableChange("existing_db_subnet_id", oldValue, newValue);

    }

    public void setDb_subnet_cidr(String newValue) throws PropertyVetoException {
        Object oldValue = this.db_subnet_cidr;
        this.db_subnet_cidr = newValue;
        pcs.firePropertyChange("db_subnet_cidr", oldValue, newValue);
        vcp.fireVetoableChange("db_subnet_cidr", oldValue, newValue);
    }

    public void setUse_existing_lb_subnet(boolean newValue) throws PropertyVetoException {
        Object oldValue = this.use_existing_lb_subnet;
        this.use_existing_lb_subnet = newValue;
        pcs.firePropertyChange("use_existing_lb_subnet", oldValue, newValue);
        vcp.fireVetoableChange("use_existing_lb_subnet", oldValue, newValue);
    }

    public void setExisting_lb_subnet_id(Object newValue) throws PropertyVetoException {
        Object oldValue = this.existing_lb_subnet_id;
        this.existing_lb_subnet_id = newValue;
        pcs.firePropertyChange("existing_lb_subnet_id", oldValue, newValue);
        vcp.fireVetoableChange("existing_lb_subnet_id", oldValue, newValue);
    }

    public void setLb_subnet_cidr(String newValue) throws PropertyVetoException {
        Object oldValue = this.lb_subnet_cidr;
        this.lb_subnet_cidr = newValue;
        pcs.firePropertyChange("lb_subnet_cidr", oldValue, newValue);
        vcp.fireVetoableChange("lb_subnet_cidr", oldValue, newValue);
    }

    public void setOpen_https_port(boolean newValue) throws PropertyVetoException {
        Object oldValue = this.open_https_port;
        this.open_https_port = newValue;
        pcs.firePropertyChange("open_https_port", oldValue, newValue);
        vcp.fireVetoableChange("open_https_port", oldValue, newValue);
    }

    public void setUse_default_lb_configuration(boolean newValue) throws PropertyVetoException {
        Object oldValue = this.use_default_lb_configuration;
        this.use_default_lb_configuration = newValue;
        pcs.firePropertyChange("use_default_lb_configuration", oldValue, newValue);
        vcp.fireVetoableChange("use_default_lb_configuration", oldValue, newValue);
    }

    public void setMaximum_bandwidth_in_mbps(int newValue) throws PropertyVetoException {
        Object oldValue = this.maximum_bandwidth_in_mbps;
        this.maximum_bandwidth_in_mbps = newValue;
        pcs.firePropertyChange("maximum_bandwidth_in_mbps", oldValue, newValue);
        vcp.fireVetoableChange("maximum_bandwidth_in_mbps", oldValue, newValue);
    }

    public void setMinimum_bandwidth_in_mbps(int newValue) throws PropertyVetoException {
        Object oldValue = this.minimum_bandwidth_in_mbps;
        this.minimum_bandwidth_in_mbps = newValue;
        pcs.firePropertyChange("minimum_bandwidth_in_mbps", oldValue, newValue);
        vcp.fireVetoableChange("minimum_bandwidth_in_mbps", oldValue, newValue);
    }

    public void setHealth_checker_url_path(String newValue) throws PropertyVetoException {
        Object oldValue = this.health_checker_url_path;
        this.health_checker_url_path = newValue;
        pcs.firePropertyChange("health_checker_url_path", oldValue, newValue);
        vcp.fireVetoableChange("health_checker_url_path", oldValue, newValue);
    }

    public void setHealth_checker_return_code(int newValue) throws PropertyVetoException {
        Object oldValue = this.health_checker_return_code;
        this.health_checker_return_code = newValue;
        pcs.firePropertyChange("health_checker_return_code", oldValue, newValue);
        vcp.fireVetoableChange("health_checker_return_code", oldValue, newValue);
    }

    public void setEnable_session_affinity(boolean newValue) throws PropertyVetoException {
        Object oldValue = this.enable_session_affinity;
        this.enable_session_affinity = newValue;
        pcs.firePropertyChange("enable_session_affinity", oldValue, newValue);
        vcp.fireVetoableChange("enable_session_affinity", oldValue, newValue);
    }

    public void setSession_affinity(Session_affinity newValue) throws PropertyVetoException {
        Object oldValue = this.session_affinity;
        this.session_affinity = newValue;
        pcs.firePropertyChange("session_affinity", oldValue, newValue);
        vcp.fireVetoableChange("session_affinity", oldValue, newValue);
    }

    public void setSession_affinity_cookie_name(String newValue) throws PropertyVetoException {
        Object oldValue = this.session_affinity_cookie_name;
        this.session_affinity_cookie_name = newValue;
        pcs.firePropertyChange("session_affinity_cookie_name", oldValue, newValue);
        vcp.fireVetoableChange("session_affinity_cookie_name", oldValue, newValue);
    }



    public void setCertificate_ocid(Object newValue) throws PropertyVetoException {
        Object oldValue = this.certificate_ocid;
        this.certificate_ocid = newValue;
        pcs.firePropertyChange("certificate_ocid", oldValue, newValue);
        vcp.fireVetoableChange("certificate_ocid", oldValue, newValue);
    }



}