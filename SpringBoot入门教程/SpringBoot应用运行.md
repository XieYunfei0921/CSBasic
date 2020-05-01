Spring 应用

---

1.  懒加载
2.  自定义Banner
3.  自定义spring应用程序
4.  链式API使用
5.  应用的时间和监听器
6.  web环境
7.  获取应用参数
8.  使用@CommandLineRunner或者@CommandLineRunner

---
1. 懒加载
spring程序允许懒加载,当懒加载开启的时候,bean之后在需要的时候创建,而非是应用启动的时候创建.
运行懒加载可以减伤应用需要启动的时间,下载web应用中,懒加载会导致web相关的web在发起HTTP请求的时候才能够加载.
懒加载的缺陷是会延迟问题发生的时间,如果没有配置的bean使用懒加载初始化,失败不就就会发送,懒加载发送的时间会延时.
注意到需要考虑JVM分配足够的内存给bean.不仅仅那些启动时初始化的情况.因此,默认情况下不支持懒加载,开启懒加载的时候
注意JVM 的堆内存一定需要足够.
启动方式:
```markdown
spring.main.lazy-initialization=true
```
> 注意: 如果需要对某个参数进行懒加载,可以使用@lazy(true)注解

2. 自定义banner
启动时会打印banner的信息，可以通过在类路径中添加`banner.txt`文件改变设置(设置这个文件的
`spring.banner.location`位置).如果文件编码不是UTF-8,可以使用`spring.banner.charset`属性.
处理这个文件之外,也可以添加`banner.gif`等图片文件.但是需要设置`spring.banner.image.location`属性.

> 注意: 如果在程序中需要产生banner的话,使用`SpringApplication.setBanner()`方法.
> 使用`org.springframework.boot.Banner`接口,实现自己的`printBanner()`方法.

可以配置`spring.main.banner-mode`属性,决定是否需要将参数输出到控制台上.`System.out(console)`
,或者是发送到log中`System.out(log)`,或者是不生成`System.out(off)`

打印的banner会在@springBootBanner 下单例注册

3. 自定义spring程序
如果对默认的spring程序不满意,可以创建一个本地的实例,并自定义,例如,关闭banner,可以这样写:
```java
public  class XXX{
    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(MySpringConfiguration.class);
        app.setBannerMode(Banner.Mode.OFF);
        app.run(args);
    }
}
```

> 注意: 传递给spring应用的构造器参数是bean的配置资源,在大多数情况下,有对@Configuration 的引用.
也可以使用外部配置`application.properties`形式配置,应用程序.

4. 链式API的使用
`SpringApplicationBuilder`可以使用多个方法的链式调用.这个方法可以包含父方法`parent`和
`child`方法.
```markdown
new SpringApplicationBuilder()
        .sources(Parent.class)
        .child(Application.class)
        .bannerMode(Banner.Mode.OFF)
        .run(args);
```

5. 应用的时间和监听器
除了使用spring框架的事件,比如说`ContextRefreshedEvent`和`SpringApplication`.
应用运行的时候,应用事件按照如下的顺序运行:
1. 处理之前,发送启动事件`ApplicationStartingEvent`
2. 发送环境的预处理事件`ApplicationEnvironmentPreparedEvent `,执行时机为上下文中环境的设定
3. 发送`ApplicationContextInitializedEvent `应用初始化事件,在bean调用之前初始化应用上下文.
4. 在bean定义加载之后,刷新之前发送事件`ApplicationPreparedEvent`
5. 在刷新之后,应用执行执行之前调用`ApplicationStartedEvent `
6. 在应用调用之后,发送事件`ApplicationReadyEvent`
7. 如果发送异常,发送应用失败事件`ApplicationFailedEvent`
除了上述事件之外,下述事件在`ApplicationPreparedEvent`和`ApplicationStartedEvent`之间发生:
1. 当`ApplicationContext`上下文刷新的时候,发送`ContextRefreshedEvent`
2. web服务器准备好之后,发送`WebServerInitializedEvent`
通过spring的事件发送机制,这部分保证了事件会被发送到子上下文的监听器中.因此,当你使用层级组织的
spring程序的时候,监听器会就收同个应用类型多个实例.

6. web环境
spring应用请求创建应用上下文的类型@ApplicationContext,决定web应用类型的算法如下:
+ 如果允许MVC,使用@AnnotationConfigServletWebServerApplicationContext
+ 如果使用了WebFlux,使用@AnnotationConfigReactiveWebServerApplicationContext
+ 其他情况,使用@AnnotationConfigApplicationContext
意味着在一个spring程序中同时使用MVC和WebFlux,默认使用MVC.可以通过调用@setWebApplicationType(WebApplicationType)
重新写出.
也可以完全控制应用上下文@ApplicationContext,通过调用@setApplicationContextClass
进行控制.

7. 获取应用参数
如果需要获取传递到`SpringApplication.run(…​)`中的参数,可以注入一个bean,名称叫做
`org.springframework.boot.ApplicationArguments`.@ApplicationArguments接口会
提供转换过的参数,和未转换的参数.示例如下:
```java
import org.springframework.boot.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.stereotype.*;

@Component
public class MyBean {

    @Autowired
    public MyBean(ApplicationArguments args) {
        boolean debug = args.containsOption("debug");
        List<String> files = args.getNonOptionArgs();
        // if run with "--debug logfile.txt" debug=true, files=["logfile.txt"]
    }

}
```
> spring boot 可以使用spring环境变量注册`CommandLinePropertySource`.这样就可以使用
> `@Value`注解注入参数

8. 使用ApplicationRunner/CommandLineRunner
如果spring程序启动的时候需要运行指定的代码,可以使用@ApplicationRunner或者@CommandLineRunner
接口实现,这两个接口都可以提供运行方法,都会在@SpringApplication.run(…​)完成之前调用。
@CommandLineRunner 提供应用参数的获取方法，下述是@CommandLineRunner 的使用方法
```java
import org.springframework.boot.*;
import org.springframework.stereotype.*;

@Component
public class MyBean implements CommandLineRunner {

    public void run(String... args) {
        // Do something...
    }

}
```