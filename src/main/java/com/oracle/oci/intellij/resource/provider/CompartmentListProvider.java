package com.oracle.oci.intellij.resource.provider;

import java.time.Duration;
import java.util.List;

import com.oracle.bmc.identity.model.Compartment;
import com.oracle.oci.intellij.account.tenancy.CacheManager;
import com.oracle.oci.intellij.account.tenancy.TenancyData;
import com.oracle.oci.intellij.account.tenancy.TenancyManager;
import com.oracle.oci.intellij.account.tenancy.TenancyService;
import com.oracle.oci.intellij.resource.Resource;
import com.oracle.oci.intellij.resource.cache.CachedResourceFactory;
import com.oracle.oci.intellij.resource.parameter.ParameterImp;
import com.oracle.oci.intellij.resource.parameter.ParameterSet;
import com.oracle.oci.intellij.resource.parameter.ParametersEnum;
import com.oracle.oci.intellij.util.ServiceAdapter;

public class CompartmentListProvider extends ListProvider<Compartment, Compartment> {
    public static final Duration COMPARTMENT_LIST_EXPIRE_DURATION = Duration.ofMinutes(10); // 10min
    private TenancyData tenancy;
    //private CachedResourceFactory<List<Compartment>> cache;
//    private static final CachedResourceFactory<List<Compartment>> instance = 
//      CachedResourceFactory.create(new CompartmentListProvider());
//    public  static  CachedResourceFactory<List<Compartment>> getInstance() {
//        return instance;
//    }
    private Class<?> source;

    public CompartmentListProvider(TenancyData tenancy, Class<?> source) {
      this.tenancy = tenancy;
      this.source = source;
    }

    @SuppressWarnings("unchecked")
    public Resource<List<Compartment>> list(Compartment parentCompartment) {
      TenancyService service = ServiceAdapter.getInstance().getAppService(TenancyService.class);
      TenancyManager tenancyManager = service.getTenancyManager();
      CacheManager cacheManager = tenancyManager.getCacheManager(this.tenancy);
      CachedResourceFactory<?> cache = cacheManager.getCache(Compartment.class);
      ParameterSet pset = ParameterSet.Builder().add(
                  new ParameterImp(ParametersEnum.PARENT_COMPARTMENT, parentCompartment.getId()));
      return (Resource<List<Compartment>>) cache.request(pset);
    }

}
