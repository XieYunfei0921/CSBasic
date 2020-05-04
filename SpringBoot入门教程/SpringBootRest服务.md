spring boot REST服务的使用
---

1. 使用RestTemplete调用REST服务
如果需要调用远端REST服务，可以使用spring的`RestTemplate`类,由于`RestTemplate`实例
经常需要使用之前进行配置,spring boot不会提供任何单个配置的`RestTemplate`bean.但是
可以通过`RestTemplateBuilder`进行自动配置.这个可以创建需要的实例.`RestTemplateBuilder`
保证敏感的`HttpMessageConverters`使用倒`RestTemplate`中.
示例程序:
```java
@Service
public class MyService {

    private final RestTemplate restTemplate;

    public MyService(RestTemplateBuilder restTemplateBuilder) {
        this.restTemplate = restTemplateBuilder.build();
    }

    public Details someRestCall(String name) {
        return this.restTemplate.getForObject("/{name}/details", Details.class, name);
    }

}
```

2. 使用web客户端调用REST服务
如果类路径中使用了spring的webflux，可以选择使用web客户端调用远端rest读物。相比较`RestTemplate`,
这个客户端功能更好,用于响应式web服务.
spring boot创建和提前配置`WebClient.Builder`,强烈使用这个去注入组件,并使用其来创建
web客户端实例,spring boot配置的builder需要共享HTTP资源,反射codec创建的方式.
下述示例演示了REST服务的调用:
```java
@Service
public class MyService {

    private final WebClient webClient;

    public MyService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.baseUrl("https://example.org").build();
    }

    public Mono<Details> someRestCall(String name) {
        return this.webClient.get().uri("/{name}/details", name)
                        .retrieve().bodyToMono(Details.class);
    }

}
```

+ 运行中的web客户端
spring boot会自动发现`ClientHttpConnector`用于驱动web客户端,主要是依据与应用类路径的库.
现在支持Reactor Netty和Jetty RS的客户端.
`spring-boot-starter-webflux-starter`默认依赖于`io.projectreactor.netty:reactor-netty`
,这个实现服务端和客户端.如果选择jetty作为响应式服务器,需要额外的添加Jetty响应式HTTP客户端库.
`org.eclipse.jetty:jetty-reactive-httpclient`,建议客户端和服务端使用同一种方式。
开发者可以重写Jetty和Reactor Netty资源配置通过提供提供自定义的`ReactorResourceFactory`和
`JettyResourceFactory`,这个会同时使用在客户端和服务端.
如果需要重写客户端的选择,需要自定义`ClientHttpConnector`实现对其的完全控制.