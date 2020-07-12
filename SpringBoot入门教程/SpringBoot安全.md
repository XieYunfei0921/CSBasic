Spring Boot 安全
---
如果类路径中配置了spring security，web应用默认情况下就会使用安全配置。spring boot依靠spring安全的内容协调策略，决定使用基本的HTTP还是表单登录。为了给web应用添加方法级别的安全措施。可以添加`@EnableGlobalMethodSecurity`注解.
默认的`UserDetailsService`存在有单个用户,用户名是`user`,密码随机,且会打印到INFO等级的日志上:
显示如下:

```markdown
Using generated security password: 78fa095d-3f4c-48b1-ad50-e24c31d5cf35
```
如果你需要打印密码信息,需要配置`org.springframework.boot.autoconfigure.security`为
INFO级别的日志.否则,默认的密码不会打印出来.

可以设置`spring.security.user.name`和`spring.security.user.password`改变账号和密码.
web应用默认具有如下特性:
+ `UserDetailsService`或者`ReactiveUserDetailsService`(webFlux),使用内存存储这个信息,生成用户的密码
+ 基于表单登录或者基础HTTP安全的应用
+ `DefaultAuthenticationEventPublisher`用户发布授权事件


#### Spring MVC安全
默认的安全配置由`SecurityAutoConfiguration`和`UserDetailsServiceAutoConfiguration`实现,
`SecurityAutoConfiguration`导入了web安全的`SpringBootWebSecurityConfiguration`,并且
`UserDetailsServiceAutoConfiguration`配置了授权信息,这个与非web应用相关.为了关闭

默认的web应用安全配置或者合并spring安全组件(例如Oath2和资源服务器).添加`WebSecurityConfigurerAdapter`(这么做
不会关闭`UserDetailsService`的配置).
可以添加`UserDetailsService`,`AuthenticationProvider`或者`AuthenticationManager`
用于关闭`UserDetailsService`配置

可以通过自定义对`WebSecurityConfigurerAdapter`的重写来定义权限规则.spring boot提供简单的方法
,用户重写权限规则.后台请求`EndpointRequest`会基于`management.endpoints.web.base-path属性`
创建一个请求匹配器`RequestMatcher`.请求路径`PathRequest`用户创建指定资源的请求匹配器.

#### WebFlux安全
可以与spring mvc 安全配置类似地,可以配置webFlux应用的安全配置,需要添加`spring-boot-starter-security`依赖,
默认安全配置由`ReactiveSecurityAutoConfiguration`和`UserDetailsServiceAutoConfiguration`实现.

`ReactiveSecurityAutoConfiguration`引入了`WebFluxSecurityConfiguration`,用于web安全.并且
`UserDetailsServiceAutoConfiguration`用于配置授权信息(这个与非web应用相关).

+ 可以使用`WebFilterChainProxy`类型的bean关闭默认的web应用安全配置.
+ 可以使用`ReactiveUserDetailsService`或者`ReactiveAuthenticationManager`的bean
关闭`UserDetailsService`的配置

可以通过自定义`SecurityWebFilterChain`的bean配置Oath2客户端和资源服务器信息,获取权限规则.
spring boot提供简便的方法,可以用户获取后台的权限规则和静态资源.
后台可以使用`management.endpoints.web.base-path`创建web匹配器`ServerWebExchangeMatcher`.

#### Oath2

1. 客户端
如果在类路径中配置了`spring-security-oauth2-clien`,可以利用一些自动配置使得创建OAuth2连接客户端更简单。
这些配置配置在`OAuth2ClientProperties`下面,同样的属性用在servlet和响应式应用中.
```properties
spring.security.oauth2.client.registration.my-client-1.client-id=abcd
spring.security.oauth2.client.registration.my-client-1.client-secret=password
spring.security.oauth2.client.registration.my-client-1.client-name=Client for user scope
spring.security.oauth2.client.registration.my-client-1.provider=my-oauth-provider
spring.security.oauth2.client.registration.my-client-1.scope=user
spring.security.oauth2.client.registration.my-client-1.redirect-uri=https://my-redirect-uri.com
spring.security.oauth2.client.registration.my-client-1.client-authentication-method=basic
spring.security.oauth2.client.registration.my-client-1.authorization-grant-type=authorization_code

spring.security.oauth2.client.registration.my-client-2.client-id=abcd
spring.security.oauth2.client.registration.my-client-2.client-secret=password
spring.security.oauth2.client.registration.my-client-2.client-name=Client for email scope
spring.security.oauth2.client.registration.my-client-2.provider=my-oauth-provider
spring.security.oauth2.client.registration.my-client-2.scope=email
spring.security.oauth2.client.registration.my-client-2.redirect-uri=https://my-redirect-uri.com
spring.security.oauth2.client.registration.my-client-2.client-authentication-method=basic
spring.security.oauth2.client.registration.my-client-2.authorization-grant-type=authorization_code

spring.security.oauth2.client.provider.my-oauth-provider.authorization-uri=https://my-auth-server/oauth/authorize
spring.security.oauth2.client.provider.my-oauth-provider.token-uri=https://my-auth-server/oauth/token
spring.security.oauth2.client.provider.my-oauth-provider.user-info-uri=https://my-auth-server/userinfo
spring.security.oauth2.client.provider.my-oauth-provider.user-info-authentication-method=header
spring.security.oauth2.client.provider.my-oauth-provider.jwk-set-uri=https://my-auth-server/token_keys
spring.security.oauth2.client.provider.my-oauth-provider.user-name-attribute=name
```

对于OpenId的连接,提供连接发现的服务,需要配置`issuer-uri`,这个URI断言成Issuer Identifier.
例如:
`issue-uri`地址时"https://example.com",OpenID提供器配置请求就是"https://example.com/.well-known/openid-configuration".
结果希望OpenId连接提供器响应。下述示例显示OpenId配置`issue-uri`的连接方法:
```markdown
spring.security.oauth2.client.provider.oidc-provider.issuer-uri=https://dev-123456.oktapreview.com/oauth2/default/
```

默认情况下,spring安全的@OAuth2LoginAuthenticationFilter 仅仅会处理`/login/oauth2/code/*`的URL.如果需要自定义重定向uri,
需要提供配置,用户处理自定义格式.例如,对于servlet应用来说,可以添加自定义的`WebSecurityConfigurerAdapter`,示例如下:
```java
public class OAuth2LoginSecurityConfig extends WebSecurityConfigurerAdapter {
	
	/*
	* 自定义默认处理URI
	* */
    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
            .authorizeRequests()
                .anyRequest().authenticated()
                .and()
            .oauth2Login()
                .redirectionEndpoint()
                    .baseUri("/custom-callback");
    }
}
```

+ 通常情况下,oauth2客户端的注册
对于普通的oauth2和openId提供器,包括google,github,facebook等等,可以使用`provider`属性
配置自定义的提供器. spring boot会推测对应的客户端注册.
```properties
spring.security.oauth2.client.registration.my-client.client-id=abcd
spring.security.oauth2.client.registration.my-client.client-secret=password
spring.security.oauth2.client.registration.my-client.provider=google

spring.security.oauth2.client.registration.google.client-id=abcd
spring.security.oauth2.client.registration.google.client-secret=password
```

2. 资源服务器
如果类路径中配置了`spring-security-oauth2-resource-server`属性,spring boot可以创建
OAuth2资源服务器.对于JWT配置来说,可以进行如下配置:
```properties
spring.security.oauth2.resourceserver.jwt.jwk-set-uri=https://example.com/oauth2/default/v1/keys
spring.security.oauth2.resourceserver.jwt.issuer-uri=https://dev-123456.oktapreview.com/oauth2/default/
```
servlet和响应式应用程序具有相同的配置.
在servlet应用中需要定义`JwtDecoder`的bean,在响应式应用中需要定义`ReactiveJwtDecoder`的bean.
如果使用的是opaque认证而不是JWT,需要进行如下配置:
```properties
spring.security.oauth2.resourceserver.opaquetoken.introspection-uri=https://example.com/check-token
spring.security.oauth2.resourceserver.opaquetoken.client-id=my-client-id
spring.security.oauth2.resourceserver.opaquetoken.client-secret=my-client-secret
```
servlet应用和响应式任务都是一致的
当然可以在servlet应用中自定义`OpaqueTokenIntrospector`的bean,在响应式应用中定义`ReactiveOpaqueTokenIntrospector`
用于设置认证.

3. 授权服务器
当前,spring security 不支持OAuth2 授权服务器,但是在`Spring Security OAuth`项目中可以使用,可以参考相关文档.