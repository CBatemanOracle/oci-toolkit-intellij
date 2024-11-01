package com.oracle.oci.intellij.ui.appstack;


import com.intellij.openapi.application.ModalityState;
import com.oracle.bmc.identity.model.Compartment;
import com.oracle.oci.intellij.account.OracleCloudAccount;
import com.oracle.oci.intellij.account.SystemPreferences;
import com.oracle.oci.intellij.settings.OCIApplicationSettings;
import com.oracle.oci.intellij.ui.appstack.actions.AppStackParametersWizardDialog;
import com.oracle.oci.intellij.ui.appstack.actions.CompartmentCache;
import com.oracle.oci.intellij.ui.appstack.actions.CustomWizardModel;
import com.oracle.oci.intellij.ui.appstack.annotations.VariableMetaData;
import com.oracle.oci.intellij.ui.appstack.models.*;
import com.oracle.oci.intellij.ui.common.UIUtil;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static com.oracle.oci.intellij.ui.appstack.models.Utils.descriptorsState;


public class YamlLoader {
    static List<VariableGroup> varGroups;
    Compartment compartment ;
    CompartmentCache compartmentCache ;



    private boolean isApply = false;

    public  Map<String, PropertyDescriptor> load1(List<VariableGroup> varGroups) throws IntrospectionException {
        LinkedHashMap<String, PropertyDescriptor> descriptorsState = new LinkedHashMap<>();



        for (VariableGroup var:varGroups){

            setupDescriptorAttributes(var, descriptorsState);
        }
        return descriptorsState;
    }

    public void setupDescriptorAttributes(VariableGroup var, LinkedHashMap<String, PropertyDescriptor> descriptorsState) throws IntrospectionException {
        PropertyDescriptor [] propertyDescriptors =  Controller.getInstance().getSortedProertyDescriptorsByVarGroup(var);

        for (PropertyDescriptor pd:propertyDescriptors){

            VariableMetaData annotation = pd.getReadMethod().getAnnotation(VariableMetaData.class);

            if (pd.getName().equals("class") || annotation == null ) {
                continue;
            }
            pd.setDisplayName((annotation.title() != null)? annotation.title() : "");
            pd.setShortDescription((annotation.description() != null) ? annotation.description() :  "" );

            if (annotation.dependsOn() != null && !annotation.dependsOn().isEmpty()) {
                pd.setValue("dependsOn",annotation.dependsOn());
            }

            if (annotation.defaultVal() != null  && !annotation.defaultVal().isEmpty()) {

//                    Object defaultValue =  getDefaultValue(pd, annotation);
//                    System.out.println(pd.getName());
                pd.setValue("default", annotation.defaultVal());

                //  pd.setValue("value",defaultValue);
//                    pd.getWriteMethod().invoke(appVarGroup,defaultValue);
            }


            pd.setValue("required", annotation.required());

            if (annotation.enumValues() != null) {
                if (!annotation.enumValues().isEmpty()){
                    List<String> list = getEnumList(annotation.enumValues());
                    pd.setValue("enum", list);
                }            }
            if (annotation.visible() != null) {
                if (!annotation.visible().isEmpty()){
                    pd.setValue("visible", annotation.visible());
                }
            }
            if (annotation.defaultVal() != null) {
                pd.setValue("type", annotation.type());
            }
            if (annotation.pattern() != null) {
                pd.setValue("pattern", annotation.pattern());
            }
            if (annotation.errorMessage() != null) {
                pd.setValue("errorMessage", annotation.errorMessage());
            }

            descriptorsState.put(pd.getName(),pd);

        }
    }

    public  Map load() throws IntrospectionException, InvocationTargetException, IllegalAccessException {
        // start caching compartments
        compartmentCache = CompartmentCache.getInstance();
        compartmentCache.setCaching(true);
        varGroups = init();

        Map<String, VariableGroup> variableGroups = new LinkedHashMap<>(); ;

        Controller.getInstance().setVariableGroups(variableGroups);
        for (VariableGroup appVarGroup : varGroups){
            Controller.getInstance().getVariableGroups().put(appVarGroup.getClass().getSimpleName(),appVarGroup);

            Class<?> appVarGroupClazz = appVarGroup.getClass();
            BeanInfo beanInfo = Introspector.getBeanInfo(appVarGroupClazz);
            PropertyDescriptor[] propertyDescriptors = beanInfo.getPropertyDescriptors();



            for (PropertyDescriptor pd:propertyDescriptors){

                VariableMetaData annotation = pd.getReadMethod().getAnnotation(VariableMetaData.class);

                if (pd.getName().equals("class") || annotation == null ) {
                    continue;
                }

                if (annotation.defaultVal() != null  && !annotation.defaultVal().isEmpty()) {

                    Object defaultValue =  getDefaultValue(pd, annotation);
                    pd.setValue("default", defaultValue);

                //  pd.setValue("value",defaultValue);
                    pd.getWriteMethod().invoke(appVarGroup,defaultValue);
                }

            }

        }
        AtomicReference<Map<String, String>> userInputs = new AtomicReference<>();

        UIUtil.invokeAndWait(()->{
            CustomWizardModel customWizardModel = null;
            try {
                customWizardModel = new CustomWizardModel(varGroups, descriptorsState);
            } catch (IntrospectionException e) {
                throw new RuntimeException(e);
            }
            AppStackParametersWizardDialog dialog = new AppStackParametersWizardDialog(customWizardModel);
            dialog.show();

            if (dialog.isCreateStack()){
                isApply = dialog.isApplyJob();
                userInputs.set(dialog.getUserInput());
            }
        }, ModalityState.defaultModalityState());


        return userInputs.get();


    }

    private  List<String> getEnumList(String enums) {
        String [] items = enums.replaceAll("\\[\\]","").split(",");
        return List.of(items);
    }

    private  Object getDefaultValue(PropertyDescriptor pd,VariableMetaData metaData )  {


        if (metaData.defaultVal().contains("compartment_ocid") || metaData.defaultVal().contains("compartment_id")) {
            if (compartment == null)
                return compartment = OracleCloudAccount.getInstance().getIdentityClient().getCompartment(SystemPreferences.getCompartmentId());
            return compartment;
        }
        if (metaData.type().equals("enum")){
            Class<?> type = pd.getPropertyType();
            String normalizedItem = metaData.defaultVal().trim().replaceAll("\\.","_");
            Enum<?> enumValue = Enum.valueOf((Class<Enum>) type, normalizedItem);
            return enumValue;
        }
        if (metaData.type().equals("boolean")){
            return Boolean.parseBoolean(metaData.defaultVal());
        }
        if (metaData.type().equals("number")){
            return Integer.parseInt(metaData.defaultVal());
        }
        return metaData.defaultVal();
    }

    private  List<VariableGroup> init() {
        List<VariableGroup> varGroups = new ArrayList<>();

        OCIApplicationSettings.State state = null;
        try {
            state = OCIApplicationSettings.getInstance().getState();

        }catch (RuntimeException ex){
            System.out.println(ex.getMessage());
        }

        if (state != null && state.isAppStackIntroductoryStepShow()){
            Introduction introductoryStep = new Introduction();
            varGroups.add(introductoryStep);

        }

        varGroups.add(new Stack_Information());
        varGroups.add(new General_Configuration());
        varGroups.add(new Application());
        varGroups.add(new Stack_Authentication());
        varGroups.add(new Application_Performance_Monitoring());
        varGroups.add(new Database());
        varGroups.add(new Other_Parameters());
        varGroups.add(new Application_Configuration_SSL_Communication());
        varGroups.add(new Application_URL());
        varGroups.add(new Network());
        varGroups.add(new Container_Instance_Configuration());
        return varGroups;
    }

    public boolean isApply() {
        return isApply;
    }
}
