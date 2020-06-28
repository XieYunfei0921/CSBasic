### **API网关服务**

#### 请求路由

1.  **传统路由方式**

   只需要对服务增加一个路由规则即可

   ```properties
   zuul.routes.api-a-url.path=/api-a-url/**
   zuul.routes.api-a-url.url=http://localhost:8080/
   ```

2.  **面向服务路由**

   传统路由需要人工手动的配置路径映射关系，在Zuul整合Eureka之后，可以使用服务名称映射路径

   ```properties
   zuul.routes.api-a.path=/api-a/**
   zuul.routes.api-a.serviceId=hello-service
   ```

#### 请求过滤

实现请求路由之后，微服务应用的接口就可以通过统一的API网关被客户端访问到了.

每个客户端用户请求微服务的接口的时候,正常情况下接口是有访问权限的,系统并不会将其开放.

然而,目前服务路由并没有这样的限制,所以所有的请求都会发送给具体的服务去处理.

对于这样的问题,假设网关服务完成非业务性质的校验.由于网关服务的加入,外部客户端在请求到达的时候就进行校验和过滤.而不是转发之后再进行过滤.

Zuul允许开发者在API网关之上,通过定义过滤器实现请求的拦截与过滤,只需要继承ZuulFilter抽象类并实现即可.

```java
public abstract class ZuulFilter implements IZuulFilter, Comparable<ZuulFilter> {
    
    /** 
    	定义过滤器类型的方法
    	主要有如下四种类型:
    	pre	前置过滤器
    	route	路由过滤器
    	post	后置过滤器
    	error	错误时过滤器
    */
    abstract public String filterType();
    
    /**
    	过滤器顺序
    	当这个数值越小,则过滤器处理的优先级越高
    */
    abstract public int filterOrder();
    
    /**
    	是否需要进行过滤,选择为true开启过滤器功能
    */
    boolean shouldFilter();
    
    /**
    	过滤器执行体,只有当过滤器开启的时候才能执行
    */
    Object run() throws ZuulException;
    /**
    	执行过滤器函数
    */
    public ZuulFilterResult runFilter() 
}
```

**过滤器执行逻辑**

```java
/**
	过滤器执行函数
	只要当过滤器可以使用以及开启的情况下才会执行
	即!isFilterDisabled() && shouldFilter()
*/
public ZuulFilterResult runFilter() {
    ZuulFilterResult zr = new ZuulFilterResult();
    if (!isFilterDisabled()) {
        if (shouldFilter()) {
            Tracer t = TracerFactory.instance().startMicroTracer(
                "ZUUL::" + this.getClass().getSimpleName());
            try {
                Object res = run();
                zr = new ZuulFilterResult(res, ExecutionStatus.SUCCESS);
            } catch (Throwable e) {
                t.setName("ZUUL::" + this.getClass().getSimpleName() + " failed");
                zr = new ZuulFilterResult(ExecutionStatus.FAILED);
                zr.setException(e);
            } finally {
                // 结束过滤过程,并记录日志到栈中@Tracer
                t.stopAndLog();
            }
        } else {
            zr = new ZuulFilterResult(ExecutionStatus.SKIPPED);
        }
    }
    return zr;
}
```

**过滤结果结构**

```java
public final class ZuulFilterResult {
    // 过滤结果
    private Object result;
    // 过滤过程中产生的异常
    private Throwable exception;
    // 过滤结果
    private ExecutionStatus status;
}
```

#### 路由配置

1. **传统路由配置**

   + 单实例配置

     ```properties
     zuul.routes.user-service.path=/user-service/**
     zuul.routes.user-service.url=http://localhost:8080/
     ```

   + 多实例配置

     ```properties
     zuul.routes.user-service.path=/user-service/**
     zuul.routes.user-service.serviceId=user-service
     ribbon.eureka.enabled=false
     # 请求会被发送到这两个端口上
     user-service.ribbon.listOfServers=http://localhost:8080,http://localhost:8081
     ```

2. **服务路由配置**

   需要整合服务注册中心组件Eureka

   ```properties
   zuul.routes.user-service.path=/user-service/**
   zuul.routes.user-service.serviceId=user-service
   ```

   简便配置如下

   ```properties
   zuul.routes.user-service=/user-service/**
   ```

   忽略表达式的设置

   ```properties
   zuul.routes.user-service.path=/api-a/**
   zuul.routes.user-service.serviceId=hello-service
   # 过滤/hello接口的路由
   zuul.ignored-patterns=/**/hello/**
   ```

   路由前缀设置

   ```properties
   zuul.routes.user-service.path=/api-a/**
   zuul.routes.user-service.serviceId=hello-service
   
   zuul.routes.user-service.path=/api-b/**
   zuul.routes.user-service.serviceId=hello-service
   
   zuul.routes.user-service.path=/api-c/**
   zuul.routes.user-service.serviceId=hello-service
   
   # 添加前缀之前是api-a/b/c会路由到hello-service上,添加之后是/test/api-a(b,c)路由到hello-service上
   zuul.prefix=/test
   ```

   本地跳转

   ```properties
   zuul.routes.user-service.path=/api-a/**
   zuul.routes.user-service.url=http://localhost:8081/
   
   zuul.routes.user-service.path=/api-b/**
   zuul.routes.user-service.serviceId=forward:/local
   ```

#### 过滤器

| 过滤器名称              | 优先级 | 作用                                                         | 备注       |
| ----------------------- | ------ | ------------------------------------------------------------ | ---------- |
| DebugFilter             | 1      | 设置Debug属性为true                                          | 前置过滤器 |
| FormBodyWrapperFilter   | -1     | 转换为格式化数据并编码给下游服务                             | 前置过滤器 |
| LocationRewriteFilter   | 900    | 重写位置头部为Zuul URL                                       |            |
| PreDecorationFilter     | 5      | 前置装饰过滤器<br />基于路由位置RouteLocator,决定何处以及如何进行路由 | 前置过滤器 |
| PostFilter              | 2000   | 后置过滤器                                                   |            |
| PreFilter               | 100    | 前置过滤器                                                   |            |
| RibbonRoutingFilter     | 10     | 使用Ribbon/Hystrix和相关HTTP客户端发送请求                   |            |
| SendErrorFilter         | 0      | 发送错误的处理器                                             | 错误过滤器 |
| SendForwardFilter       | 500    | 使用请求分发器RequestDispatcher将请求发送到应用后端          | 路由过滤器 |
| SendResponseFilter      | 1000   | 发送响应的过滤器                                             | 后置过滤器 |
| Servlet30WrapperFilter  | -2     | Servlet 3.0过滤器                                            | 前置过滤器 |
| ServletDetectionFilter  | -3     | Servlet发现过滤器,起始状态下确认是否有过滤器正在工作         | 前置过滤器 |
| SimpleHostRoutingFilter | 100    | 简单的主机路由                                               | 路由过滤器 |
| StaticResponseFilter    | 0      | 静态响应过滤器                                               |            |
| SurgicalDebugFilter     | 99     | 路由满足特定形式的请求给Eureka进行debug<br />需要通过zuul.debug.vip或者zuul.debug.host进行配置 | 路由过滤器 |

#### 动态路由

作为外部的网关,必须具备动态更新内部逻辑的能力,比如修改路由规则,动态添加/删除过滤器的功能.

动态路由的实现需要依赖于分布式配置中心,即SpringCloud Config的参与,间接需要配置一个git仓库

在git仓库中新建一个网关的配置文件,名称叫做`api-gateway.properties`

```properties
zuul.routes.user-service-a.path=/user-service-a/**
zuul.routes.user-service-a.serviceId=hello-service

zuul.routes.user-service-b.path=/user-service-b/**
zuul.routes.user-service-b.url=http://localhost:8001/
```

#### 动态过滤器

动态过滤器需要借助于,JVM动态语言实现,这里使用groovy

引入依赖

```xml
<dependency>
    <groupId>org.codehaus.groovy</groupId>
    <artifactId>groovy-all</artifactId>
    <version>2.4.4</version>
</dependency>
```

