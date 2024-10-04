package com.oracle.oci.intellij.resource.request;

import java.time.Duration;
import java.util.List;

import com.oracle.bmc.auth.BasicAuthenticationDetailsProvider;
import com.oracle.bmc.identity.IdentityClient;
import com.oracle.bmc.identity.model.RegionSubscription;
import com.oracle.bmc.identity.requests.ListRegionSubscriptionsRequest;
import com.oracle.bmc.identity.responses.ListRegionSubscriptionsResponse;
import com.oracle.oci.intellij.account.tenancy.TenancyData;
import com.oracle.oci.intellij.resource.Resource;
import com.oracle.oci.intellij.resource.ResourceFactory;
import com.oracle.oci.intellij.resource.parameter.ParameterSet;

public class RegionSubscriptionListRequestor
  extends OCIModelRequestor<List<RegionSubscription>> {

  private static final Duration LIST_EXPIRE_DURATION = Duration.ofMinutes(10);
  private TenancyData tenancy;

  public RegionSubscriptionListRequestor(TenancyData tenancyData) {
    this.tenancy = tenancyData;
  }

  @Override
  public Resource<List<RegionSubscription>> request(ParameterSet parent) throws Exception {
    BasicAuthenticationDetailsProvider authProvider =
      this.tenancy.toAuthProvider();
    try (IdentityClient identityClient =
      IdentityClient.builder().build(authProvider)) {
      final ListRegionSubscriptionsResponse response =
        identityClient.listRegionSubscriptions(ListRegionSubscriptionsRequest.builder()
                                                                             .tenancyId(tenancy.getId())
                                                                             .build());

      List<RegionSubscription> items = response.getItems();
      return ResourceFactory.createExpiringResource(items,
                                                    LIST_EXPIRE_DURATION);
    }

  }

}
