### **分布式配置中心**

包括客户端和服务端组成，服务端称作**配置中心**。是一个独立的微服务，用来连接仓库并为客户端提供配置信息，以及加密/解密等访问接口.

SpringCloud Config的配置仓库默认使用git仓库存储配置信息,使用Spring Cloud Config构建的服务器,支持版本管理.并且可以通过git客户端访问配置内容.

#### 服务端结构

1. 远程Git仓库

   用于存储配置文件

2. Config Server

   分布式配置中心,其中需要指定git仓库的位置以及相关账号密码信息等

3. 本地Git仓库

   在Config Server 文件系统中,每次客户端请求获取配置文件的时候,Config Server从Git仓库的最新配置到本地,然后在git仓库中读取并返回.当远程仓库无法读取的时候,直接返回本地git仓库的内容.

##### git仓库配置

```yml
spring:
  application:
    name: config-server
  cloud:
    config:
      server:
        git:
          uri: https://github.com/xxx/SpringCloud_Learning
          search-paths: config-repo
          username: xxxx
          password: xxxxxx
      profile: default
      label: master
      uri: http://127.0.0.1:8092/
```

##### 属性覆盖

使用属性覆盖的功能,开发者可以配置所有的属性,例如

```properties
spring.cloud.config.server.overrides.name=didi
spring.cloud.config.server.overrides.from=shanghai
```

##### 安全保护

由于配置中心的敏感特性,需要进行一定的安全措施.常用可以使用OAuth2进行安全授权.当然可以配合Spring Security进行使用.

##### 高可用配置

1. 传统模式

   将所有的config服务器指向同一个git仓库,这样这个git仓库就是一个共享的文件系统了.

2. 服务模式

   将Config Server作为一个普通的服务,注册到服务注册中心中.这样可以利用Eureka客户端负载均衡的特性实现高可用.

##### 快速失败与重试

由于加载参数的时间比较长,在一些特定的条件下,需要快速得知Config Server是否获取到了配置信息.所以首先需要判断服务器是否处于正常状态下,如果出现了异常需要快速失败.

```properties
spring.cloud.config.failFast=true
```

如果因为网络或者其他原因导致了间歇性问题,重启的代价就比较大.所以需要提供自动重试的功能.

相关参数如下:

```properties
spring.cloud.config.retry.multiplier=1000
spring.cloud.config.retry.initial-interval=1.1
spring.cloud.config.retry.max-interval=2000
spring.cloud.config.retry.max-attempts=6
```

##### 远程配置的获取方式

提供向配置中心服务器发送GET请求,获得远程配置文件

例如

```http
GET /{application}-{profile}.yml
```

携带有lable信息时如下

```http
GET /lable/{application}-{profile}.yml
```



