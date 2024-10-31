package com.oracle.oci.intellij.resource.provider;

import java.util.List;

import com.oracle.bmc.identity.model.Compartment;
import com.oracle.bmc.identity.model.RegionSubscription;
import com.oracle.oci.intellij.account.tenancy.CacheManager;
import com.oracle.oci.intellij.account.tenancy.TenancyData;
import com.oracle.oci.intellij.account.tenancy.TenancyManager;
import com.oracle.oci.intellij.resource.Resource;
import com.oracle.oci.intellij.resource.cache.CachedResourceFactory;
import com.oracle.oci.intellij.resource.parameter.ParameterImp;
import com.oracle.oci.intellij.resource.parameter.ParameterSet;
import com.oracle.oci.intellij.resource.parameter.ParametersEnum;
import com.oracle.oci.intellij.util.ServiceAdapter;

public class RegionListProvider extends ListProvider<RegionSubscription, TenancyData> {

  private TenancyData tenancy;
  private Class<?> source;

  public RegionListProvider(TenancyData tenancy, Class<?> source) {
    this.tenancy = tenancy;
    this.source = source;
  }

  @SuppressWarnings("unchecked")
  @Override
  public Resource<List<RegionSubscription>> list(TenancyData source) {
    TenancyManager service = ServiceAdapter.getInstance().getAppService(TenancyManager.class);
    CacheManager cacheManager = service.getCacheManager(source);
    CachedResourceFactory<?> cache = cacheManager.getCache(RegionSubscription.class);
    ParameterSet pset = ParameterSet.Builder().add(
                new ParameterImp(ParametersEnum.PARENT_COMPARTMENT, source.getId()));
    return (Resource<List<RegionSubscription>>) cache.request(pset);

  }

}
