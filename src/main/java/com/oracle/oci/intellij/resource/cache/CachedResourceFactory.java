package com.oracle.oci.intellij.resource.cache;

import java.lang.management.ManagementFactory;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;

import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.jetbrains.annotations.NotNull;

import com.oracle.oci.intellij.account.tenancy.TenancyData;
import com.oracle.oci.intellij.resource.Resource;
import com.oracle.oci.intellij.resource.ResourceFactory;
import com.oracle.oci.intellij.resource.parameter.ParameterSet;
import com.oracle.oci.intellij.util.LogHandler;
/**
 * <p>
 * A factory that caches resources it has previously requested. This class
 * delegates to another {@code ResourceFactory} that requests a resource from an
 * external service, and then caches the resource for subsequent requests.
 * </p><p>
 * The class relies on {@link Resource#isValid()} to determine when a
 * cached resource needs to be evicted. Subclasses are responsible for returning
 * {@code Resource} objects that implement {@code isValid} correctly for the
 * particular type of resource they request.
 * </p><p>
 * The cache constrains the maximum the number of resources it can store at any
 * given time. By default, a maximum of 100 resources will be cached. The least
 * recently used resource is evicted when the cache reaches its maximum size,
 * and a new resource is requested.
 * </p>
 */
public class CachedResourceFactory<TO> implements ResourceFactory<TO>{
    public static int MAX_CACHE = 100 ;
    public static long LOCK_TIMEOUT = 6;
    public static TimeUnit  LOCK_TIMEOUT_UNIT = TimeUnit.MINUTES;
    Map<ParameterSet, Future<Resource<TO>>> values;
    private volatile boolean isCaching;
    private ObjectName objectName;
    private final ResourceFactory<TO> resourceFactory;
    private final ReentrantReadWriteLock lock;
    private final ReentrantReadWriteLock.ReadLock readLock;
    private final ReentrantReadWriteLock.WriteLock writeLock;

    private CachedResourceFactory(String tenancyId, ResourceFactory<TO> resourceFactory) {
        this.values = createObjectCache(tenancyId, resourceFactory);
        this.resourceFactory = resourceFactory;      
        this.lock = new ReentrantReadWriteLock();
        this.readLock = this.lock.readLock();
        this.writeLock = this.lock.writeLock();
    }
    
    private LRUCache<ParameterSet, Future<Resource<TO>>> createObjectCache(String tenancyId, ResourceFactory<TO> resourceFactory) {
   // so we can differentiate between each cache (because we have cache per each factory
      String cacheInstanceId = resourceFactory.getClass().getSimpleName();
      try {
        this.objectName = createObjectName(tenancyId, cacheInstanceId);

        return new LRUCache<ParameterSet, Future<Resource<TO>>>(objectName, MAX_CACHE,resourceFactory);
      }
      catch (MalformedObjectNameException ex) {
        LogHandler.error("Error creating MBean name", ex);
        // this should not be an expected (checked) exception
        // since we should be forming the objectname properly.
        throw new RuntimeException(ex);
      }

    }

    public static ObjectName createObjectName(String tenancyId,
                           String cacheInstanceId) throws MalformedObjectNameException {
      return new ObjectName("com.oracle.oci.intellij.cache."+cacheInstanceId+":type=LRUCache,id="+tenancyId);
    }

    public static <V>CachedResourceFactory<V> create(TenancyData tenancyData, ResourceFactory<V> factory) {
        CachedResourceFactory<V> cachedResources = new CachedResourceFactory<>(tenancyData.getId(), factory);
        cachedResources.setIsCaching(true);
        return cachedResources;
    }

    public ObjectName getObjectName() {
      return this.objectName;
    }

    public void setIsCaching(boolean caching) {
        setCaching(caching);
    }

    // no need to lock since isCaching is volatile.
    public CachedResourceFactory<TO> setCaching(boolean caching) {
        isCaching = caching;
        return this;
    }
    public  Resource<TO> request(final ParameterSet parameterSet) {
//        final String id = this.resourceFactory.getId(parameterSet);

        FutureTask<Resource<TO>> newResourceTask = 
          new FutureTask<>(() -> resourceFactory.request(parameterSet));

        Future<Resource<TO>> existingFuture ;
        if (!isCaching){
            newResourceTask.run();
            return await(newResourceTask,parameterSet);
        }else {
             existingFuture = executeReadOperation(()-> values.get(parameterSet));

            if (existingFuture != null) {
                Resource<TO> resource = await(existingFuture,parameterSet);
                if (resource.isValid()){
                    return resource;
                }
            }

            Future<Resource<TO>> newResourceFuture = executeWriteOperation(()-> values.compute(parameterSet, (key, currentResourceFuture) -> {
                if (currentResourceFuture == existingFuture) {
                    return newResourceTask;
                }
                return currentResourceFuture;
            })) ;

            if (newResourceTask.equals(newResourceFuture)){
                newResourceTask.run();
            }


            return await(newResourceFuture,parameterSet);
        }
    }

    private Resource<TO> await(@NotNull Future<Resource<TO>> future,ParameterSet parameters){
      try {
        return future.get();
      } catch (InterruptedException | ExecutionException e) {
          // if there is a problem with retrieving resource we should remove it from  cache
          // so next time we create new one
        executeWriteOperation(()-> remove(parameters));
        throw new RuntimeException(e);
      }
    }


    public  void clearCache() {
        executeWriteOperation(()->{
            values.clear();
            return null;
        });
    }

    private <R> R executeReadOperation(Supplier<R> operation){
        boolean readLockAcquired = false;
        try {
            readLockAcquired = readLock.tryLock(LOCK_TIMEOUT,LOCK_TIMEOUT_UNIT);
            if (readLockAcquired){
               return operation.get();
            }else {
                LogHandler.error("Failed to acquire read lock within timeout for " );
            }

        } catch (InterruptedException e) {
            if (!Thread.interrupted()){
                Thread.currentThread().interrupt();
            }
            LogHandler.error("Thread interrupted while trying to acquire write lock for clearing cache");
        } finally {
            if (readLockAcquired){
                readLock.unlock();
            }
        }
        return null;
    }

    private <R> R executeWriteOperation(Supplier<R> operation) {
        boolean writeLockAcquired = false;
        try{
            writeLockAcquired = writeLock.tryLock(LOCK_TIMEOUT, LOCK_TIMEOUT_UNIT);
            if (writeLockAcquired){
                return operation.get();
            }else {
                LogHandler.error("Failed to acquire write lock within timeout for clearing cache");
            }
        } catch (InterruptedException e) {
            if (!Thread.interrupted()){
                Thread.currentThread().interrupt();
            }            LogHandler.error("Thread interrupted while trying to acquire write lock for clearing cache");
        } finally {
            if (writeLockAcquired){
                writeLock.unlock();
            }
        }
        return null;
    }

    public  void makeStale(ParameterSet parameters) {
     executeWriteOperation(()->{
         Optional.ofNullable(this.values.get(parameters))
                 .ifPresent(toResource -> await(toResource,parameters).makeStale());
         return null;
     });
    }

    public Future<Resource<TO>> remove(ParameterSet parameters){
       return values.remove(parameters);
    }

    public interface LRUCacheMBean {
        long getHits();
        long getMisses();
        double getCacheHitRatio();
    }
    /**
     * A map that evicts the least recently used (LRU) entry when the number of
     * stored values exceeds a maximum size.
     */
    public static class LRUCache<K, V> extends LinkedHashMap<K,V> implements LRUCacheMBean {

        final int maximumCapacity ;
        private volatile long hits;
        private volatile long misses;
        // The load factor is configured such that the threshold will never be reached.
        // It is noted that HashMap.put(...) executes code similar to this:
        //   if (size++ > threshold)
        //     resize();
        //   removeEldestEntry();
        // This has a resize occur *before* removeEldestEntry is called and has
        // a chance to reduce the size. For this reason, the load factor of 1.1 is
        // given, which should mean the "threshold" = maximumSize * 1.1, which
        // will be larger than the maximumSize.
        public <TO>LRUCache(ObjectName objectName, int cacheSize ,ResourceFactory<TO> resourceFactory){
            super(cacheSize,1.1f,true);
            maximumCapacity = cacheSize;
            this.hits=0;
            this.misses=0;
            try {
                MBeanServer server = ManagementFactory.getPlatformMBeanServer();
                if (!server.isRegistered(objectName)) {
                  server.registerMBean(this,objectName);
                }
            }catch (Exception ex){
              ex.printStackTrace();
                LogHandler.error("Failed to register MBean: " + ex.getMessage());
            }

            // start a thread to log cache statistics periodically
            ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
            scheduler.scheduleAtFixedRate(this::logStatistics,0,1,TimeUnit.MINUTES);
        }

        @Override
        protected boolean removeEldestEntry(@SuppressWarnings("rawtypes") Map.Entry eldest) {
            return this.size() > maximumCapacity ;
        }

        @Override
        public V get(Object key) {
            V value = super.get(key);
            if (value != null){
                hits++;
            }else{
                misses++;
            }
            return value;
        }
        @Override
        public long getHits() {
            return hits;
        }
        @Override
        public long getMisses() {
            return misses;
        }
        @Override
        public double getCacheHitRatio(){
            long total = hits + misses;

            return total ==0 ? 0 :(double) hits /(hits+misses);
        }

        public void logStatistics() {
            LogHandler.info("Cache Hits: " + getHits());
            LogHandler.info("Cache Misses: " + getMisses());
            LogHandler.info("Cache Hit Ratio: " + getCacheHitRatio());
        }
    }
}
