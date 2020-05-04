**Spring Boot消息与缓存机制**
---

#### 缓存
##### JCache
通过类路径中的`javax.cache.spi.CachingProvider`配置,使用`spring-boot-starter-cache`
提供`JCacheCacheManager`,如果有多个提供器,需要使用下述配置指定:
```markdown
spring.cache.jcache.provider=com.acme.MyCachingProvider
spring.cache.jcache.config=classpath:acme.xml
```
可以使用两种方式自定义`javax.cache.cacheManager`
+ 缓存必须使用`spring.cache.cache-names`属性创建,如果自定义了`javax.cache.configuration.Configuration`
的bean,需要对其进行自定义配置.
+ 使用`org.springframework.boot.autoconfigure.cache.JCacheManagerCustomizer`用于对其进行
缓存配置

##### EhCache
EhCache 2.x在文件`ehcache.xml`中使用,可以在类路径根路径找到.如果EhCache 2.x存在,
使用`spring-boot-starter-cache`启动缓存管理器。可以进行如下配置:
```markdown
spring.cache.ehcache.config=classpath:config/another-config.xml
```

##### Hazelcast
如果配置了`HazelcastInstance`,就会自动包装`CacheManager`

##### Infinispan
Infinispan没有默认的文件位置配置,所以必须显示地指出.否则会使用默认的启动器.
```markdown
spring.cache.infinispan.config=infinispan.xml
```
缓存可以使用`spring.cache.cache-names`属性创建,用于自定义`ConfigurationBuilder`的bean.

##### Couchbase
couchbase缓存的配置
```markdown
spring.cache.cache-names=cache1,cache2
```
定义`@Configuration`去配置额外的`Bucket`和`cache3`缓存
```java
@Configuration(proxyBeanMethods = false)
public class CouchbaseCacheConfiguration {

    private final Cluster cluster;

    public CouchbaseCacheConfiguration(Cluster cluster) {
        this.cluster = cluster;
    }

    @Bean
    public Bucket anotherBucket() {
        return this.cluster.openBucket("another", "secret");
    }

    @Bean
    public CacheManagerCustomizer<CouchbaseCacheManager> cacheManagerCustomizer() {
        return c -> {
            c.prepareCache("cache3", CacheBuilder.newInstance(anotherBucket())
                    .withExpiration(2));
        };
    }

}
```
##### Redis
如果使用了redis作为缓存，就会自动配置`RedisCacheManager`.很可能创建额外的缓存,通过设置
`spring.cache.cache-names`属性。可以通过`spring.cache.redis.*`配置默认缓存。
例如，下面的示例创建了cache1,和cache2两个缓存，存活时间为10min.
```markdown
spring.cache.cache-names=cache1,cache2
spring.cache.redis.time-to-live=600000
```
如果需要自定义，可以设置`RedisCacheManagerBuilderCustomizer`的bean.下述示例显示了自定义的配置.
```markdown
@Bean
public RedisCacheManagerBuilderCustomizer myRedisCacheManagerBuilderCustomizer() {
    return (builder) -> builder
            .withCacheConfiguration("cache1",
                    RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofSeconds(10)))
            .withCacheConfiguration("cache2",
                    RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofMinutes(1)));

}
```

##### Caffeine
这个是Guava的缓存
+ 缓存定义使用`spring.cache.caffeine.spec`属性
+ `com.github.benmanes.caffeine.cache.CaffeineSpec`的bean已被定义
+ `com.github.benmanes.caffeine.cache.Caffeine`的bean已被定义
```markdown
spring.cache.cache-names=cache1,cache2
spring.cache.caffeine.spec=maximumSize=500,expireAfterAccess=600s
```

##### Simple
使用`ConcurrentHashMap`作为缓存的数据结构,下面的示例创建`cache1`和`cache2`的缓存
```markdown
spring.cache.cache-names=cache1,cache2
```

##### None
关闭缓存
```markdown
spring.cache.type=none
```

#### 消息中间件的支持
##### ActiveMQ的支持


##### Kafka的支持
