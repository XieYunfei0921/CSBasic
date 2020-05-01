spring boot 外部配置
---
spring boot使用特定的属性资源顺序,用于分配敏感的value值.属性按照下面的顺序考虑:
> 1. devtools的全局配置,位于目录`$HOME/.config/spring-boot`下
> 2. 测试中的`@TestPropertySource`注解
> 3. 测试中的属性,可以使用注解`@SpringBootTest`配置
> 4. 来自`SPRING_APPLICATION_JSON`的属性
> 5. ServletConfig初始化参数
> 6. ServletContext初始化属性
> 7. 来自`java:comp/env`的JNDI属性
> 8. java系统参数
> 9. 操作系统变量
> 10. 随机属性源@RandomValuePropertySource,形如`random.*`
> 11. 在jar包外指定的应用配置参数,可以是`application-{profile}.properties`和yaml文件参数
> 12. jar中指定的应用参数配置,可以是`application-{profile}.properties`和yaml文件参数
> 13. jar包外的应用参数(application.properties或者yaml配置参数)
> 14. jar中的应用参数(application.properties或者yaml配置参数)
> 15. 在@Configuration的类上使用@PropertySource,注意到这个属性在应用上下文刷新的时候才会被添加
> 到环境中.在刷新启动之前,配置`logging.*`和`spring.main.*`太晚了.
> 16. 默认属性(使用SpringApplication.setDefaultProperties设置的)

1. 配置随机参数
使用@RandomValuePropertySource 注入随机值,可以产生integer,long,uuid,或者string类型
```properties
my.secret=${random.value}
my.number=${random.int}
my.bignumber=${random.long}
my.uuid=${random.uuid}
my.number.less.than.ten=${random.int(10)}
my.number.in.range=${random.int[1024,65536]}
```

2. 获取命令行参数
默认情况下,spring应用转换命令行参数成一个参数,并将其添加到spring 环境中.按照参数优先级,命令行的
参数优先级是最高的.
如果不希望将命令行参数添加到环境中,可以使用`SpringApplication.setAddCommandLineProperties(false)`关闭。

3. 应用属性文件
可以在`application.properties`文件配置参数,可以在下述位置配置,并将其添加到spring环境中.
+ 当前目录的`/config`子目录
+ 当前目录
+ 类路径的`/config`包
+ 类路径根目录
如果不希望使用`application.properties`作为配置文件,可以指定`spring.config.name`环境属性.也可以
使用`spring.config.location`指定位置(可以是使用逗号分割的多个位置).参考下面的示例:
```shell
$ java -jar myproject.jar --spring.config.name=myproject
```
```shell
$ java -jar myproject.jar --spring.config.location=classpath:/default.properties,classpath:/override.properties
```
`spring.config.location`包含的目录需要以`/`结束,且运行的时候名称会有`spring.config.name`产生.
包括指定的配置文件.使用这个参数指定的文件,如果不支持配置文件指定的话,可以被任何配置文件重写.
配置的位置按照反向属性搜寻,默认情况下,配置位置的路径为`classpath:/,classpath:/config/,file:./,file:./config/`
搜寻结果如下:
+ `file:./config/`
+ `file:./`
+ `classpath:/config/`
+ `classpath:/`
如果自定义的配置位置使用`spring.config.location`配置.例如设置的自定义值为`classpath:/custom-config/,file:./custom-config/`。
查找属性如下:
+ `file:./custom-config/`
+ `classpath:custom-config/`
使用`spring.config.additional-location`配置自定义参数的时候,假设添加的位置为`classpath:/custom-config/,file:./custom-config/`
.这个目录的参数会追加到默认配置中.
所有读取顺序为:
+ `file:./custom-config/`
+ `classpath:custom-config/`
+ `file:./config/`
+ `file:./`
+ `classpath:/config/`
+ `classpath:/`
搜索的顺序使得可以在配置文件中指定默认属性值.然后选择性的在另一个文件中对其进行重写.可以在`application.properties`中
提供应用的默认值.默认值可以在运行的时候重写.

4. 指定配置文件的属性
除了文件`application.properties`文件之外,使用指定配置文件可以使用下述命名:
`convention: application-{profile}.properties`.环境变量如果在不激活外部配置的情况下
使用的是默认的配置信息。知乎回去加载`application-default.properties`.
指定配置的参数文件如从`application.properties`指定的位置加载.使用指定的配置文件总是会重写
没有指定的文件,无论是否在包外.
如果指定了多个配置,采用最后一个胜利的策略.例如,使用`spring.profiles.active`配置的属性.
在通过@SpringApplication API配置之后,会采用被优先采用(优先级高).

5. 参数中的占位符
使用存在的环境变量填充`application.properties`的值,可以参考之前定义的值(例如:系统参数)
```properties
app.name=MyApp
app.description=${app.name} is a Spring Boot application
```

6. 加密属性
spring boot不提供对于加密属性的支持,但是提供修改spring环境的切入点,@EnvironmentPostProcessor
接口运行在应用启动之前操作环境变量.
如果你需要安全的存储授权密钥,可以参考[Spring Cloud Vault](https://cloud.spring.io/spring-cloud-vault/reference/html/)

7. 使用yaml代替属性文件
yaml是json的超集,例如,对于分层数据是相当好的配置.spring应用程序类自动支持yaml,作为
properties的替换方案.
+ 加载yaml
spring框架提供两种方便的方式加载yaml文档.@YamlPropertiesFactoryBean将yaml加载成
@Properties,而@YamlMapFactoryBean加载yaml为map形式.
例如,考虑下面的yaml文档:
```yaml
environments:
    dev:
        url: https://dev.example.com
        name: Developer Setup
    prod:
        url: https://another.example.com
        name: My Cool App
```
上面的yaml会被转换为下面的配置
```properties
environments.dev.url=https://dev.example.com
environments.dev.name=Developer Setup
environments.prod.url=https://another.example.com
environments.prod.name=My Cool App
```
yaml文档如下:
```yaml
my:
   servers:
       - dev.example.com
       - another.example.com
```
会被转换为如下形式
```properties
my.servers[0]=dev.example.com
my.servers[1]=another.example.com
```
属性的绑定是通过spring boot的`Binder`实现,这个就是`@ConfigurationProperties`注解的功能.
需要在目标bean上设置这个属性.且需要提供一个setter或者对其进行value的初始化.例如,下述例子绑定
到之前显示的属性上.这个java类就是对上述my进行配置.
```java
@ConfigurationProperties(prefix="my")
public class Config {

    private List<String> servers = new ArrayList<String>();

    public List<String> getServers() {
        return this.servers;
    }
}
```
+ 将yaml暴露到spring环境中
@YamlPropertySourceLoader 用于使用@PropertySource 形式暴露yaml到spring环境中.
这样做就可以使用@Value注解获取yaml配置.

+ 多个配置的yaml文档
```yaml
server:
    address: 192.168.1.100
---
spring:
    profiles: development
server:
    address: 127.0.0.1
---
spring:
    profiles: production & eu-central
server:
    address: 192.168.1.120
```
如果当应用上下文启动的时候,没有明确的激活,那么默认的配置文件就会被激活.下述yaml文件中,设置了一个
`spring.security.user.password`的值.
```yaml
server:
  port: 8000
---
spring:
  profiles: default
  security:
    user:
      password: weak
```
如果没有发现任何配置文件,那么就会变成如下形式
```yaml
server:
  port: 8000
spring:
  security:
    user:
      password: weak
```
+ yaml的缺陷
不能通过使用@PropertySource加载yaml文件,所有,这种情况下需要使用`.properties`文件.
使用多个yaml文档属性在指定的yaml文档中可能会导致不同的行为,例如下面的例子:
`application-dev.yaml`
```yaml
server:
  port: 8000
---
spring:
  profiles: "!test"
  security:
    user:
      password: "secret"
```
配置运行参数为`--spring.profiles.active=dev`,期待`security.user.password =secret`,
但是现在的情况不是如此.
因为主文件为`application-dev.yml`,已经被认作为指定配置的参数,指定的文档就会被忽略.

