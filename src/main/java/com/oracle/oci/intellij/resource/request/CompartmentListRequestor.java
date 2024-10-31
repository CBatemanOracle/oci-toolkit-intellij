package com.oracle.oci.intellij.resource.request;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import com.oracle.bmc.auth.BasicAuthenticationDetailsProvider;
import com.oracle.bmc.identity.IdentityClient;
import com.oracle.bmc.identity.IdentityClient.Builder;
import com.oracle.bmc.identity.model.Compartment;
import com.oracle.bmc.identity.requests.ListCompartmentsRequest;
import com.oracle.bmc.identity.responses.ListCompartmentsResponse;
import com.oracle.oci.intellij.account.tenancy.TenancyData;
import com.oracle.oci.intellij.account.tenancy.TenancyManager;
import com.oracle.oci.intellij.account.tenancy.TenancyService;
import com.oracle.oci.intellij.resource.Resource;
import com.oracle.oci.intellij.resource.ResourceFactory;
import com.oracle.oci.intellij.resource.parameter.ParameterSet;
import com.oracle.oci.intellij.resource.parameter.ParametersEnum;
import com.oracle.oci.intellij.util.ServiceAdapter;

public class CompartmentListRequestor extends OCIModelRequestor<List<Compartment>> {

  private static final Duration COMPARTMENT_LIST_EXPIRE_DURATION = Duration.ofMinutes(10);
  private TenancyData tenancy;
  
  public CompartmentListRequestor(TenancyData tenancy) {
    this.tenancy = tenancy;
  }

  @Override
  public Resource<List<Compartment>> request(ParameterSet parent) throws Exception {
    BasicAuthenticationDetailsProvider authProvider = this.tenancy.toAuthProvider();
    Builder builder = IdentityClient.builder();
    TenancyManager tenancyManager = 
      ServiceAdapter.getInstance().getAppService(TenancyService.class).getTenancyManager();
    Optional.ofNullable(tenancyManager.getCurrentRegion()).ifPresent(curRegion -> builder.region(curRegion));
    try(IdentityClient identityClient = builder.build(authProvider)){
        ListCompartmentsRequest request = ListCompartmentsRequest.builder().accessLevel(ListCompartmentsRequest.AccessLevel.Accessible)
                .compartmentId(parent.getParameter(ParametersEnum.PARENT_COMPARTMENT)).limit(1024).build();
        ListCompartmentsResponse listCompartments = identityClient.listCompartments(request);
        return ResourceFactory.createExpiringResource(listCompartments.getItems(), COMPARTMENT_LIST_EXPIRE_DURATION);
    }
  }
}
