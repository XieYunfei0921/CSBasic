Spring Cloud Consul
---

1. spring cloud consul特征
+ **服务发现**: 实例可以使用Consul代理注册,客户端可以使用spring管理的bean发现实例.
+ **支持Ribbon**: 通过spring cloud netflix对客户端进行负载均衡
+ 支持spring cloud 的负载均衡器(由spring cloud提供的客户端负载均衡)
+ 支持Zuul,通过spring cloud netflix进行动态路由和过滤.
+ **分布式配置**使用Consul的kv存储.

2. 示例程序运行
+ 运行`docker-compose up`
+ 访问`http://localhost:8500`
+ 运行`mvn package`会构建spring cloud的maven仓库
+ 运行jar包`java -jar spring-cloud-consul-sample/target/spring-cloud-consul-sample-${VERSION}.jar`
+ 访问`http://localhost:8080`,验证`{"serviceId":"<yourhost>:8080","host":"<yourhost>","port":8080}`的结果.
+ 运行`java -jar spring-cloud-consul-sample/target/spring-cloud-consul-sample-${VERSION}.jar --server.port=8081`
+ 再次访问`http://localhost:8081`使用`{"serviceId":"<yourhost>:8081","host":"<yourhost>","port":8081}`进行验证.

3. 安装Consul
安装教程: <https://learn.hashicorp.com/consul/getting-started/install.html>

4. consul代理
consul代理客户端必须对于所有的spring cloud consul可用.默认情况下,客户端代理在
`localhost:8500`作为代理.参考代理的文档,用于启动代理客户端,并且确定如何去连接consul
代理类服务器的集群上.在开发情况下,安装consul结束之后,可以使用下述指令启动代理:
```markdown
./src/main/bash/local_run_consul.sh
```

5. 使用consul进行服务发现
服务发现是基于框架的微服务的主要特征,常数处理每个客户端的配置,或者规定的形式很难去处理,或者是
非常脆弱的.consul通过http api和dns提供服务发现的服务.spring cloud consul用于平衡http
api用于服务的注册和发现.
这个不会阻止非springcloud应用的DNS接口平衡问题.(也就是这些应用也会参与平衡操作).consul
代理服务器运行在集群中,可以使用流言传播协议进行交互并使用一致性协议.
+ 激活的方法
使用组@org.springframework.cloud中的@spring-cloud-starter-consul-discovery
激活consul的服务发现.查看spring cloud文档,并使用当前spring cloud版本构建系统.
+ 使用consul注册
如果客户端使用consul注册,提供自己的元数据(比如host,port,id,name,tag等).默认创建
HTTP检查,consul会每个10s方位/health的后台端点.如果健康检查失败,服务的实例会标记为
`critical`.
**示例consul程序**
```java
@SpringBootApplication
@RestController
public class Application {

    @RequestMapping("/")
    public String home() {
        return "Hello world";
    }

    public static void main(String[] args) {
        new SpringApplicationBuilder(Application.class).web(true).run(args);
    }

}
```
consul的客户端位于`localhost:8500`,这个配置需要定位客户端.
`application.yml`
```yml
spring:
  cloud:
    consul:
      host: localhost
      port: 8500
```
> 注意: 使用[spring cloud consul的配置](https://github.com/spring-cloud/spring-cloud-consul/blob/master/docs/src/main/asciidoc/spring-cloud-consul.adoc#spring-cloud-consul-config)
> 获取配置值,需要将配置放在`bootstrap.yml`中,而非是`application.yml`
默认服务名称,实例编号和端口号来自于环境中,这个的名字为`${spring.application.name}`.
spring 上下文ID和服务器端口号`${server.port}`.
为了关闭consul的服务发现,可以设置`spring.cloud.consul.discovery.enabled=false`
可以通过设置`spring.cloud.consul.discovery.register=false`设置注册的关闭.

6. 使用分离的服务注册管理信息
当管理器服务器端口设置的和应用端口不一致的使用,通过设置@management.server.port 属性，
管理服务会被当做单个服务这次而不是应用服务。
例如:
`application.yml`
```yml
spring:
  application:
    name: myApp
management:
  server:
    port: 4452

```
上述配置包含了两个服务
+ 应用服务
```yml
ID: myApp
Name: myApp
```
+ 管理服务
```yml
ID: myApp-management
Name: myApp-management
```

管理服务会继承应用服务的`instanceId`和`serviceName`
例如:
`application.yml`
```yml
spring:
  application:
    name: myApp
management:
  server:
    port: 4452
spring:
  cloud:
    consul:
      discovery:
        instance-id: custom-service-id
        serviceName: myprefix-${spring.application.name}
```
上述配置包含两个服务
+ 应用服务
```yml
ID: custom-service-id
Name: myprefix-myApp
```
+ 管理服务
```yml
ID: custom-service-id-management
Name: myprefix-myApp-management
```
更多的配置可以按照下述配置设置
```markdown
/** Port to register the management service under (defaults to management port) */
spring.cloud.consul.discovery.management-port

/** Suffix to use when registering management service (defaults to "management" */
spring.cloud.consul.discovery.management-suffix

/** Tags to use when registering management service (defaults to "management" */
spring.cloud.consul.discovery.management-tags
```

7. HTTP健康检查
consul实例的健康检查默认在"/health",这个是spring boot Actuator应用的使用的后台.
你需要对其作出改变,甚至改变Actuator应用.consul的时间间隔用于检查健康检查的后端.
分别是10s和1m.
`application.yml`
```yml
spring:
  cloud:
    consul:
      # 配置health检查的时间周期为15s
      discovery: 
        healthCheckPath: ${management.server.servlet.context-path}/health
        healthCheckInterval: 15s
```
可以通过设置`management.health.consul.enabled=false`关闭检查

8. 元数据和consul标签
consul不支持服务的元数据,spring cloud 的服务实例的元数据以@Map<String, String> 
存储，spring cloud consul 使用consul标签去估算元数据，直到consul支持元数据为止。
使用形式`key==value`的标签会分割并使用map的key和value.tage中没有`equal=sign`的会
作为key和value存储.
`application.yml`
```yml
spring:
  cloud:
    consul:
      discovery:
        tags: foo=bar, baz
```
上述产生了一个`foo->bar`和`baz->baz`的map.

9. consul实例ID唯一化
默认情况下,consul实例使用ID注册,这个id等于spring 应用上下文的编号.默认情况下,spring
应用上下文ID使用`${spring.application.name}:comma,separated,profiles:${server.port}`
在大多数情况下,如果需要考虑到id的全局性,使用spring cloud可以提供一个唯一的标识符.
范围为`spring.cloud.consul.discovery.instanceId`.例如:
`application.yml`
```yml
spring:
  cloud:
    consul:
      discovery:
        instanceId: ${spring.application.name}:${vcap.application.instance_id:${spring.application.instance_id:${random.value}}}
```
使用这个元数据,多个部署在本机的服务实例,随机值会会使得这个实例获取唯一性.早spring boot
程序中@vcap.application.instance_id 会被字段装入。这样就不需要随机值。

10. 健康检查请求使用header
header可以用于健康检查的请求，例如，可以尝试注册后台spring cloud config服务器。
`application.yml`
```yml
spring:
  cloud:
    consul:
      discovery:
        # 设置健康检查的header
        health-check-headers:
          X-Config-Token: 6442e58b-d1ea-182e-cfa5-cf9cddef0722
```
通过标准的http请求,header可以使用多个value值
`application.yml`
```yml
spring:
  cloud:
    consul:
      discovery:
        health-check-headers:
          X-Config-Token:
            - "6442e58b-d1ea-182e-cfa5-cf9cddef0722"
            - "Some other value"
```

11. 查找服务
+ 使用负载均衡
spring cloud 支持Feign(REST客户端构建器),且spring RestTemplate会使用逻辑服务名
称/id(不是实际的物理地址)查找服务.无论是Feign还是RestTemplate都支持Ribbon,用于客户
端的负载均衡.
> 使用@RestTemplate 获取服务存储
```java
public class XXX{
    @LoadBalanced
    @Bean
    public RestTemplate loadbalancedRestTemplate() {
         return new RestTemplate();
    }
}
```
> 使用下述方式进行服务查找(使用逻辑服务名称/id,不要使用全域名)
```java
public class XXX{
	@Autowired
    RestTemplate restTemplate;
    
    public String getFirstProduct() {
       return this.restTemplate.getForObject("https://STORES/products/1", String.class);
    }
}
```
如果你拥有多个数据中心的consul集群,且你希望获取其他数据层的服务,仅仅以服务名称/id是不够的.
这种情况下,需要设置参数@spring.cloud.consul.discovery.datacenters.STORES=dc-west
.这个存储的服务名称/id是STORES,`dc-west`是数据中心(STORES服务存储的位置).
--> 也就是服务属于数据中心.
> 现在的spring cloud也支持负载均衡
在spring cloud ribbon运行的时候,建议使用`@spring.cloud.loadbalancer.ribbon.enabled=false`
,以便于使用阻塞式负载均衡客户端@BlockingLoadBalancerClient，而不是Ribbon的负载均衡器。
+ 使用负载发现的客户端
可以使用`org.springframework.cloud.client.discovery.DiscoveryClient`提供API,
用于服务发现客户端.(没有指定到Netflix的)
```markdown
@Autowired
private DiscoveryClient discoveryClient;

public String serviceUrl() {
    List<ServiceInstance> list = discoveryClient.getInstances("STORES");
    if (list != null && list.size() > 0 ) {
        return list.get(0).getUri();
    }
    return null;
}
```

12. consul的目录监控
consul目录监控利用了consul去监控服务.目录监控有一个阻塞的consul http请求API.用于在
服务改变的时候调用.如果有新的服务数据,那么就会发起一个心跳事件.为了改变配置控制器的频率,
通过改变`spring.cloud.consul.config.discovery.catalog-services-watch-delay`。
默认时间是1000，单位ms，延时是上一个执行代下一次执行的时间间隔。
通过设置`spring.cloud.consul.discovery.catalogServicesWatch.enabled=false.`
关闭服务监控.
监视器使用spring 任务调度器@TaskScheduler 调度consul.默认情况下@ThreadPoolTaskScheduler
池的大小为1.为了改变任务调度器@TaskScheduler,使用常数`@ConsulDiscoveryClientConfiguration.CATALOG_WATCH_TASK_SCHEDULER_NAME`
创建类型为@TaskScheduler 的bean。

13. 使用consul进行分布式配置
consul提供一个kv存储，用于存储配置和其他元数据，spring cloud consul配置是一个配置服务器
和客户端的可选方案。在启动项中配置加载到spring环境变量中，多个属性源@PropertySource
的实例基于应用名称创建，且配置文件中模仿了spring cloud config解决属性的顺序。例如，名称为`testApp`
应用,并带有`dev`的配置文件,或按照下述配置创建:
```markdown
config/testApp,dev/
config/testApp/
config/application,dev/
config/application/
```
最特别的属性源在上面,使用consul去配置所有的应用.在目录`config/testApp`配置`testApp`
服务的实例.
这个配置是基于应用的启动,发送HTTP POST请求给`/refresh`,会导致应用的重启.**配置监听器**
可以自动发现变化,并重载应用上下文.

+ 激活配置功能
为了获取consul配置，使用组@org.springframework.cloud 和@spring-cloud-starter-consul-config
启动。spring cloud 项目使用当前发行版构建系统。
这个会启动自动配置，这个配置会构建spring cloud consul 配置。

+ 自定义
可以进行如下的配置:
`bootstrap.yml`
```yml
spring:
  cloud:
    consul:
      config:
        enabled: true # 开启/停止consul配置
        prefix: configuration # 设置配置基础目录
        defaultContext: apps # 设置所有应用的目录名称
        profileSeparator: '::' ## 配置文件分割符
```

+ 配置监听器
consul配置监听器利用consul的能力,去监视key的前缀.配置监视器调用阻塞式consul HTTP API
,用于确定是否当前应用的相关配置数据已经改变.如果出现了新的配置数据,那么就会发送一个刷新的事件.
这个就等于是调用刷新后台执行器的功能.
可以使用`spring.cloud.consul.config.watch.delay`配置监视器的频率,默认值为1000ms.
这个延时是上次调用的结束到本地调用开始的时间.
使用`spring.cloud.consul.config.watch.enabled=false`关闭配置监视器.
监视器使用spring的任务调度器去调度consul的调用函数.默认情况下,线程池@ThreadPoolTaskScheduler
容量为1.为了改变任务调度器@TaskScheduler,创建一个任务调度器@TaskScheduler,名称在
@ConsulConfigAutoConfiguration.CONFIG_WATCH_TASK_SCHEDULER_NAME配置.

+ 使用yaml配置
使用yaml存储格式(不使用kv对设置),可以更加方便.设置`spring.cloud.consul.config.format`
属性为`YAML`或者`PROPERTIES`.例如:
`bootstrap.yml`
```yml
spring:
  cloud:
    consul:
      config:
        format: YAML
```
yml文件中必须设置consul的`data`key属性.
```markdown
config/testApp,dev/data
config/testApp/data
config/application,dev/data
config/application/data
```
可以在任意形式的key列表中存储yaml文件
可以通过配置`spring.cloud.consul.config.data-key`属性来改变data key位置.

+ 配置git2consul
git2consul是一个consul社区项目,可以从git仓库加载文件到consul中去.默认情况下,key的名称
是这些文件的名称.yaml和属性@Properties 支持`.yml`和`.properties`的文件扩展.可以设置
`spring.cloud.consul.config.format`属性.
`bootstrap`
```yml
spring:
  cloud:
    consul:
      config:
        format: FILES
```
在`/config`中进行如下配置,`development`配置文件的名称为`foo`
```markdown
.gitignore
application.yml
bar.properties
foo-development.properties
foo-production.yml
foo.properties
master.ref
```
下述的属性源会被创建
```
config/foo-development.properties
config/foo.properties
config/application.yml
```

14. 快速失败
**快速失败**在某些情况下很有用(比如说,本地开发或者指定的测试情况),如果不能获取consul就会直接失败.
设置在启动参数中设置`spring.cloud.consul.config.failFast=false`，会配置模块记录warning
日志而非抛出异常。这个会运行应用正常的构建。

15. consul的重试
 如果你希望偶然失败的consul代理在你的应用启动的时候重启，可以设置是啊比的重新尝试。可以添加
 `spring-retry`和`spring-boot-starter-aop`到类路径下。默认尝试6次，时间间隔为1000ms.
 可以使用`spring.cloud.consul.retry.*`配置这些参数.这个与spring consul配置和服务注册同时运行.
 
16. 使用consul配置spring cloud bus(总线)
 + 启动
使用`org.springframework.cloud`下的`spring-cloud-starter-consul-bus`启动consul 总线.
可以查看[spring cloud bus 文档](https://projects.spring.io/spring-cloud/spring-cloud.html#_circuit_breaker_hystrix_clients)中获取可用的监视端口和发送自定义消息.

17. 配置断路器
应用可以使用spring cloud netflix配置的断路器.通过添加指定的pom依赖,
`spring-cloud-starter-hystrix`.断路器机构（Hystrix）不会依赖于路由发现客户端。@EnableHystrix
注解需要使用配置类进行配置。这个方法可以使用@HystrixCommand断路器(circuit breaker)进行配置。
可以参考相关的文档。

18. 使用Turbine和consul配置断路器机构(Hystrix)度量器进行聚合
Turbine(spring cloud netflix项目中组件),对多个断路器机构(hystrix)度量流进行合并.
dashboard可以显示合并之后的结果,turbine使用服务客户端@DiscoveryClient 接口去查找相关实例.
使用consul配合turbine,按照类似下述的方式配置Turbine应用.
`pox.xml`
```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-netflix-turbine</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-consul-discovery</artifactId>
</dependency>
```
注意到这里的turbine依赖不是启动器,驱动器包含对Eureka的支持.
`application.yml`
```yml
spring.application.name: turbine
applications: consulhystrixclient
turbine:
  aggregator:
    clusterConfig: ${applications}
  appConfig: ${applications}
```
集群配置`clusterConfig`和应用配置`appConfig`必须要匹配,所以使用逗号分割的服务编号列表到各个配置属性中.
`Turbine.java`
```java
@EnableTurbine
@SpringBootApplication
public class Turbine {
    public static void main(String[] args) {
        SpringApplication.run(DemoturbinecommonsApplication.class, args);
    }
}
```

19. 相关参数配置
<https://cloud.spring.io/spring-cloud-static/spring-cloud-consul/2.2.2.RELEASE/reference/html/appendix.html>