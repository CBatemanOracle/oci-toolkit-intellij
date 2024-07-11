package com.oracle.oci.intellij.ui.appstack.models;

import com.oracle.oci.intellij.ui.appstack.actions.PropertyOrder;
import com.oracle.oci.intellij.ui.appstack.annotations.VariableMetaData;

import java.beans.PropertyVetoException;

public class Container_Instance_Configuration extends VariableGroup {

    private static final String DOCUMENTATION_LINK = "https://github.com/oracle-quickstart/appstack/blob/main/usage_instructions.md#container-instance-configuration";

    @Override
    public String getDocumentationLink() {
        return DOCUMENTATION_LINK;
    }

    public enum Shape{
        CI_Standard_E3_Flex,
        CI_Standard_E4_Flex,
    }
    private Shape shape;

;

    private int memory_in_gbs;

    private int ocpus;

    @PropertyOrder(1)
    @VariableMetaData(title="Container instance shape",description="A shape is a template that determines the number of OCPUs, amount of memory, and other resources that are allocated to a container instance.",defaultVal="CI.Standard.E3.Flex",type="enum",required=true,enumValues ="CI.Standard.E3.Flex, CI.Standard.E4.Flex")
    public Shape getShape() {
        return shape;
    }
    @PropertyOrder(2)
    @VariableMetaData(title="Memory (GB)",description="Min - 1 GB or a value matching the number of OCPUs, whichever is greater. Max - 64 GB per OCPU, up to 1024 GB total",defaultVal="8",type="number",required=true)

    public int getMemory_in_gbs() {
        return memory_in_gbs;
    }
    @PropertyOrder(3)
    @VariableMetaData(title="OCPU",description="Min - 1 OCPU. Max - 64 OCPU",defaultVal="2",type="number",required=true)

    public int getOcpus() {
        return ocpus;
    }

    public void setShape(Shape newValue) throws PropertyVetoException {
        Object oldValue = this.shape;
        this.shape = newValue;
        pcs.firePropertyChange("shape", oldValue, newValue);
        vcp.fireVetoableChange("shape", oldValue, newValue);
    }

    public void setMemory_in_gbs(int newValue) throws PropertyVetoException {
        Object oldValue = this.memory_in_gbs;
        this.memory_in_gbs = newValue;
        pcs.firePropertyChange("memory_in_gbs", oldValue, newValue);
        vcp.fireVetoableChange("memory_in_gbs", oldValue, newValue);
    }

    public void setOcpus(int newValue) throws PropertyVetoException {
        Object oldValue = this.ocpus;
        this.ocpus = newValue;
        pcs.firePropertyChange("ocpus", oldValue, newValue);
        vcp.fireVetoableChange("ocpus", oldValue, newValue);
    }



}