package com.oracle.oci.intellij.ui.appstack.actions;

import com.intellij.ui.components.JBList;
import com.intellij.ui.wizard.WizardModel;
import com.intellij.ui.wizard.WizardStep;
import com.oracle.bmc.core.model.Subnet;
import com.oracle.bmc.core.model.Vcn;
import com.oracle.bmc.database.model.AutonomousDatabaseSummary;
import com.oracle.bmc.devops.model.RepositoryBranchSummary;
import com.oracle.bmc.devops.model.RepositorySummary;
import com.oracle.bmc.dns.model.ZoneSummary;
import com.oracle.bmc.identity.model.AvailabilityDomain;
import com.oracle.bmc.identity.model.Compartment;
import com.oracle.bmc.keymanagement.model.KeySummary;
import com.oracle.bmc.keymanagement.model.VaultSummary;
import com.oracle.oci.intellij.settings.OCIApplicationSettings;
import com.oracle.oci.intellij.ui.appstack.models.Controller;
import com.oracle.oci.intellij.ui.appstack.models.Introduction;
import com.oracle.oci.intellij.ui.appstack.models.VariableGroup;
import org.jetbrains.annotations.Nullable;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;


public class CustomWizardModel extends WizardModel {
    private final List<WizardStep> mySteps = new ArrayList<>();
    private  JBList<String> groupMenuList = new JBList<>() ;
    private Controller controller = Controller.getInstance();
    private LinkedHashMap<String,String> appStackVariables;

    List<VariableGroup> varGroups;
    LinkedHashMap<String, PropertyDescriptor> descriptorsState;

    public CustomWizardModel( List<VariableGroup> varGroups, Map<String, PropertyDescriptor> descriptorsState) throws IntrospectionException {
        super("App Stack Variable");
        this.varGroups = varGroups;
        this.descriptorsState = (LinkedHashMap<String, PropertyDescriptor>) descriptorsState;

        // create the wizard steps
        initWizardSteps();
    }

    public JBList<String> getGroupMenuList() {
        return groupMenuList;
    }

    public List<VariableGroup> getVarGroups() {
        return varGroups;
    }

    public void setGroupMenuList(JBList<String> groupMenuList) {
        this.groupMenuList = groupMenuList;
    }

    private void initWizardSteps() throws IntrospectionException {
        // initiate the
        initApplicationNames();
        // create introduction Wizard step
        OCIApplicationSettings.State state = getState();

        List<VariableGroup> varGroupListWithoutIntro ;
        if (state != null && state.isAppStackIntroductoryStepShow()){
            // create wizard step for introduction .
            IntroductoryWizardStep varWizardStep = new IntroductoryWizardStep();
            mySteps.add(varWizardStep);
            add(varWizardStep);
            varGroupListWithoutIntro = varGroups.subList(1,varGroups.size()) ;
        }else {
            varGroupListWithoutIntro = varGroups;
        }


        // initiate the list first here
        for (VariableGroup varGroup : varGroupListWithoutIntro) {
            if (varGroup instanceof Introduction) {
                continue;
            }
            createVariableGroupStep(varGroup);

        }
    }

    @Nullable
    private static OCIApplicationSettings.State getState() {
        OCIApplicationSettings.State state = null;
        try {
            state = OCIApplicationSettings.getInstance().getState();

        }catch (RuntimeException ex){
            System.out.println(ex.getMessage());
        }
        return state;
    }

    private void createVariableGroupStep(VariableGroup varGroup) throws IntrospectionException {
        Class<? extends VariableGroup> varGroupClazz = varGroup.getClass();
        BeanInfo beanInfo = Introspector.getBeanInfo(varGroupClazz);
        PropertyDescriptor[] propertyDescriptors = beanInfo.getPropertyDescriptors();

        Arrays.sort(propertyDescriptors, Comparator.comparingInt(pd -> {
            PropertyOrder annotation = pd.getReadMethod().getAnnotation(PropertyOrder.class);
            return (annotation != null) ? annotation.value() : Integer.MAX_VALUE;
        }));

        // create first  wizard step
        VariableWizardStep varWizardStep = new VariableWizardStep(varGroup, propertyDescriptors, descriptorsState);
        mySteps.add(varWizardStep);
        add(varWizardStep);
    }

    private void initApplicationNames() {
        controller.initApplicationNames();
    }

    public LinkedHashMap<String,String> collectVariables(){
        LinkedHashMap<String,String> vars = new LinkedHashMap<>();
        descriptorsState.forEach((key,value)->{
            boolean isEnabled = controller.getVarPanelByName(value.getName()).isVisible();
            if (isEnabled ){
                String mappedValue = mapValue(value);
                vars.put(value.getName(),mappedValue);
            }
        });
        return vars;
    }

    private String mapValue(PropertyDescriptor pd) {
        VariableGroup variableGroup = controller.getVarGroupByName(pd.getName());
        Object value;
        try {
            value = pd.getReadMethod().invoke(variableGroup);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
        if (((String)pd.getValue("type")).startsWith("oci") ){
            if (value instanceof AutonomousDatabaseSummary) {
                AutonomousDatabaseSummary adb = (AutonomousDatabaseSummary) value;
                return adb.getId();
            } else if (value instanceof VaultSummary) {
                VaultSummary adb = (VaultSummary) value;
                return adb.getId();
            }else if (value instanceof RepositorySummary){
                RepositorySummary repositorySummary = (RepositorySummary)value;
                return repositorySummary.getId();
            }else if (value instanceof KeySummary) {
                KeySummary adb = (KeySummary) value;
                return adb.getId() ;
            } else if (value instanceof ZoneSummary) {
                ZoneSummary zoneSummary = (ZoneSummary) value;
                return zoneSummary.getId();
            } else if (value instanceof AvailabilityDomain) {
                AvailabilityDomain adb = (AvailabilityDomain) value;
                return adb.getName() ;
            }else if (value instanceof Subnet) {
                Subnet adb = (Subnet) value;
                return adb.getId()  ;
            }else if (value instanceof Vcn) {
                Vcn adb = (Vcn) value;
                return adb.getId() ;
            }else if (value instanceof Compartment) {
                Compartment adb = (Compartment) value;
                return adb.getId() ;
            }else if(value instanceof RepositoryBranchSummary){
                RepositoryBranchSummary repositoryBranchSummary = (RepositoryBranchSummary) value;
                return repositoryBranchSummary.getRefName();
            }
        }
        if (pd.getName().equals("shape")) {
            return value.toString().replace("_",".");
        } else if (pd.getName().equals("session_affinity")) {
            return value.toString().replace("_"," ");
        }
        return value!= null ?value.toString() : "";
    }




    public List<WizardStep> getMySteps(){
        return mySteps;
    }
}
