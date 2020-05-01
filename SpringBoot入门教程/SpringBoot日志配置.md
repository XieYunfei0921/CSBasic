spring boot日志配置
---
spring boot使用对于所有内部日志使用通用日志处理,但是对底层的日志实现开发.默认配置提供,
JavaUtil Logging,Log4j2,和logback.每种情况下,日志提取配置,可以控制台输出也可以使用文件输出.
默认情况下,使用启动器会采用logback.合适的logback路由也会被添加,为了保证JUL,log4j的正常运行.

1. 日志的等级分类
`ERROR`,`WARN`,`INFO`,`DEBUG`,`TRACE`

2. 开启控制台输出
```shell
$ java -jar myapp.jar --debug
```

3. 文字个性化输出
如果终端支持ANSI,颜色输出用于提供可读性,可以设置`spring.output.ansi.enabled=true`
重写自动发现。
彩色的代码可以使用`%clr`配置.颜色转换器通过日志等级输出,显示示例如下:
|日志等级|颜色|
|FATAL|红色|
|ERROR|红色|
|WARN| 黄色|
|INFO| 绿色|
|DEBUG| 绿色|
| TRACE| 绿色|
可以使用下述方式设置颜色
```markdown
%clr(%d{yyyy-MM-dd HH:mm:ss.SSS}){yellow}
``` 
支持如下颜色:
`blue`,`cyan`,`faint`,`green`,`magenta`,`red`,`yellow`

4. 文件输出
默认情况下，springboot仅仅会将日志输出到控制台上，并不会写出日志文件，如果需要写出日志事件，需要设置
`logging.file.name`或者`logging.file.path`参数。
参考下表:

| `logging.file.name` | `logging.file.path` | 示例 | 描述 |
|---|---|---|---|
| none | none |  | 仅仅输出到控制台 |
|指定文件|none|`my.log`|写出到指定的日志文件,名称可以是精确的位置|
|none|指定目录|`/var/log`|写出`spring.log`到指定的目录,可以是准确的位置|

当日志文件达到10MB时,日志文件进行循环,默认状态下,控制台输出`ERROR`,`WARN`,`INFO`级别的日志,
这个容量值可以通过`logging.file.max-size`改变.循环的日志文件可以保存7天,使用`logging.file.total-size-cap`
配置,当日志文件总数超出容量的时候,会删除备份,强迫清除日志,这里使用`logging.file.clean-history-on-start`属性.

5. 日志等级的设置
所有支持的日志系统都有日志等级集合,通过使用`logging.level.<level-name>=<level>`进行配置/
根日志处理器可以通过`logging.level.root.`进行配置。
```properties
logging.level.root=warn
logging.level.org.springframework.web=debug
logging.level.org.hibernate=error
```
也可以通过环境变量设置,`LOGGING_LEVEL_ORG_SPRINGFRAMEWORK_WEB=DEBUG `会将`org.springframework.web`设置为
`DEBUG`.



6. 日志组
经常可以将日志组合起来,以便于可以同时进行配置.例如,可以改变tomcat相关的日志处理器,
参数不能简单的记忆顶层的包.
spring boot支持在spring环境中定义日志组.例如,可以以添加tomcat组到配置文件中.
```markdown
logging.group.tomcat=org.apache.catalina, org.apache.coyote, org.apache.tomcat
```
一旦定义了,就可以使用一行改变该类别的所有日志类型:
```markdown
logging.level.tomcat=TRACE
```

spring boot包括下述预先定义的日志组

|名称|日志处理器|
| --- | ---|
| web | org.springframework.core.codec, org.springframework.http, org.springframework.web,<\br>org.springframework.boot.actuate.endpoint.web,<\br>org.springframework.boot.web.servlet.ServletContextInitializerBeans|
| sql | org.springframework.jdbc.core, org.hibernate.SQL, org.jooq.tools.LoggerListener|

6. 自定义日志处理
可以将库置于类路径下,激活日志系统,可以提供合适的配置文件到类路径下,提供用户定义配置,或者放到spring
指定的位置(通过`logging.config.`配置).
可以通过`org.springframework.boot.logging.LoggingSystem`使用指定的日志系统.value需要
完全的边界为@LoggingSystem的实现.
根据不同的日志系统,会加载下述文件:

|日志系统|自定义文件|
|---|---|
|logback| logback-spring.xml, logback-spring.groovy, logback.xml, or logback.groovy|
| log4j2| log4j2-spring.xml or log4j2.xml|
|jdk(jul)| logging.properties|






