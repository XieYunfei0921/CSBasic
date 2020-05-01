Spring Data
---
+ Spring Data的类型(这里只介绍需要进行演示的)
1. Spring Data JDBC
2. Spring Data JDBC Extension
3. Spring Data JPA
4. Spring Data MongoDB
5. Spring Data Redis
6. Spring Data REST
7. Spring Data For Apache Solr
8. Spring Data For ElasticSearch
---

> Spring Data JPA
0. 综述:
---
这个模块处理基于JPA数据访问层的高级支持.使构建spring程序更加简单,使用了数据访问
层的技术.
应用数据访问层的实现一直是一个麻烦的问题.许多样例代码写到里面,用于执行简单的查询
以及进行分页和编辑的功能.SpringData JPA旨在提升数据访问层的实现效果,主要通过
减小实际需要的数据的消耗.作为一个开发者,你可以写库的接口,包含自定义的查找方法,
Spring会自动提供实现.
特征:
+ 基于Spring和JPA的复杂实现支持
+ 支持Querydsl预测和类型安全的JPA查询
+ 透明化本地类的实现
+ 支持分页,支持动态查询,可以整合数据访问层代码
+ 启动时支持@Query注解
+ 支持实例映射的XML文件
+ 基于库配置的java配置,使用注解@EnableJpaRepositories
---
1. 依赖设置
  在maven项目中,可以在POM文件中这样声明依赖:
  ```xml
  <dependencyManagement>
      <dependencies>
        <dependency>
          <groupId>org.springframework.data</groupId>
          <artifactId>spring-data-releasetrain</artifactId>
          <version>Moore-SR5</version>
          <scope>import</scope>
          <type>pom</type>
        </dependency>
      </dependencies>
  </dependencyManagement>
  ```
当前版本是Moora-SR5版本,版本名称遵循`${name}-${release}`,发行版如下所示:
+ `BUILD-SNAPSHOT`: 当前的版本快照
+ `M1,M2...`: MileStones
+ `RC1,RC2`: 发行版
+ `RELEASE`: GA发行版
+ `SR1,SR2`: 服务发行版
2. 声明一个spring data的依赖
    ```xml
        <dependencies>
          <dependency>
            <groupId>org.springframework.data</groupId>
            <artifactId>spring-data-jpa</artifactId>
          </dependency>
        <dependencies>
    ```

3. 使用Spring Data库
Spring Data库的目的是减少数据访问(用于持久化到存储设备中)层的代码.
+ 核心概念
 使用本地的类去管理并需要本地类的类型作为参数乐西.这个接口作为标记接口的首要参数.用于
 帮助你发现接口,并继承它.类@CrudRepository 提供了基本的CRUD功能,用于管理的实例类.
 ```java
 public interface CrudRepository<T, ID> extends Repository<T, ID> {

  <S extends T> S save(S entity);      // 保存给定实例

  Optional<T> findById(ID primaryKey); // 返回给定ID对应的实例

  Iterable<T> findAll();               // 返回所有的实例

  long count();                        // 返回实例的数量

  void delete(T entity);               // 删除指定实例

  boolean existsById(ID primaryKey);   // 确定指定ID的实例是否存在

  // … more functionality omitted.
}   
 ```
 上述类的位置为@org.springframework.data.repository.CrudRepository
 其中
    T为仓库管理的domain类的类型
    ID为仓库管理器的ID类型
 + 接口@org.springframework.data.repository.PagingAndSortingRepository用
 于添加简单的实例页数的标记
 ```java
 public interface PagingAndSortingRepository<T, ID> extends CrudRepository<T, ID> {

    Iterable<T> findAll(Sort sort); // 返回所有实例,按照指定规则@sort排序

    Page<T> findAll(Pageable pageable);// 返回满足分页要求@pageable的页
 }
 ```
 查找一个20个实例的页,可以这样使用
 ```markdown
    # 获取bean
    PagingAndSortingRepository<User, Long> repository = // … get access to a bean
    # 查找20个实例,形成一个user页
    Page<User> users = repository.findAll(PageRequest.of(1, 20));
 ```
 除了查询方法之外,查询所派生的计数@count和删除@delete都可以进行
 ```java
 interface UserRepository extends CrudRepository<User, Long> {

    long countByLastname(String lastname); // 按照lastName计数
 }
 ```
 
 + 标准CRUD函数库通常会查询底层数据库,使用SpringData,声明查询分为如下4步
 1. 声明一个接口，继承@Repository或者其子接口，且设置domain类型和ID类型.示例如下:
 ```java
 interface UserResposity extends Repository<User,Long>{}
 ```
 2. 声明查询方法
 ```java
 interface UserResposity extends Repository<User,Long>{
	List<User> findByLastName(String name);
 }
 ```
 3. 创建接口的代理实例,使用@JavaConfig或者XML配置
 + 使用@JavaConfig配置如下
 ```java
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@EnableJpaRepositories // 这里可以自定义扫描版,默认为当前注解所在的包
class Config { … }
```
 + 使用JPA的XML文档配置
 ```markdown
 <?xml version="1.0" encoding="UTF-8"?>
    <beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xmlns:jpa="http://www.springframework.org/schema/data/jpa"
       xsi:schemaLocation="http://www.springframework.org/schema/beans
         https://www.springframework.org/schema/beans/spring-beans.xsd
         http://www.springframework.org/schema/data/jpa
         https://www.springframework.org/schema/data/jpa/spring-jpa.xsd">
    
       <jpa:repositories base-package="com.acme.repositories"/>
    
    </beans>
 ```
  JPA命名空间在这个例子中使用,如果使用对其他存储数据块的仓库抽象,需要将其改变为对应
  存储模块的命名空间/换句话说,需要改变jpa去支持例如`mongodb`
  同时,注意到@JavaConfig中没有显示的配置包,因为注解类的包是默认的包.如果需要自定义
  扫描包使用`basePackage`属性指定,形如`Enable${store-name}Repositories`注解.
4. 注入仓库实例，并使用
```java
class SomeClient {

  private final PersonRepository repository;

  SomeClient(PersonRepository repository) {
    this.repository = repository;
  }

  void doSomething() {
    List<Person> persons = repository.findByLastname("Matthews");
  }
}
```

+ 定义仓库接口
1. 仓库@Repository 的定义
典型的,仓库的接口需要继承@Repository,@CrudRepository或者@PagingAndSortingRepository.
另外,除了可以选择继承Spring Data接口,也可以使用@RepositoryDefinition进行仓库接口的注解.
建议选择继承@CrudRepository的方法,可以提供一些了的基本CRUD方法.
2. 选择性的暴露CRUD方法
```java
@NoRepositoryBean
interface MyBaseRepository<T, ID> extends Repository<T, ID> {

  Optional<T> findById(ID id);

  <S extends T> S save(S entity);
}

interface UserRepository extends MyBaseRepository<User, Long> {
  User findByEmailAddress(EmailAddress emailAddress);
}
```

在之前的例子中，定义了一个示例接口，用于所有的domain类。并保留了@findById和@save方法，
这两个方法会被路由到基本仓库的实现中。从而作为存储的选择，因为匹配了@CrudRepository的特征。
所以@UserRepository 现在使用中间接口@MyBaseRepository的功能，可以保存用户信息@sava,且可以
根据ID查找用户,且可以根据地址触发查找.

3. 多个springdata模块使用仓库
  应用中使用唯一的springdata模块使得应用更简单，因为所有仓库接口都被定义在模块中。
  有的时候，应用需要多个应用模块，在这类情况下，仓库定义必须要区别**持久化技术**间的区别。
  当在类路径中发现多个仓库工厂的时候，spring data条目需要严格配置仓库模式。严格配置使用
  仓库上的细节或者domain类去决定spring data模块绑定的仓库定义。
  + 如果使用继承的方式定义仓库，对于指定的spring data模块，是一个合法的候选项。
  + 使用注解的情况下,对于指定的spring data模块，是一个合法的候选项。spring data模块
  接受三方注解(JPA的`@Entity`)或者是提供自身的主机( Spring Data MongoDB 和 
  Spring Data Elasticsearch)
  下面是使用模块指定的方式的仓库定义:
  ```java
  interface MyRepository extends JpaRepository<User, Long> { }
  
  // -------------------------
  @NoRepositoryBean
  interface MyBaseRepository<T, ID> extends JpaRepository<T, ID> { … }

  interface UserRepository extends MyBaseRepository<User, Long> { … }
  ```
  **使用注解的多个仓库定义**
  ```java
    interface PersonRepository extends Repository<Person, Long> { … }
    
    @Entity
    class Person { … }
    
    interface UserRepository extends Repository<User, Long> { … }
    
    @Document
    class User { … }
  ```
  @person使用JPA注解@Entity,所以其属于JPA,@User注解为@Document,属于MongoDB.
  使用domain类,混合使用注解
  ```java
    interface JpaPersonRepository extends Repository<Person, Long> { … }
    
    interface MongoDBPersonRepository extends Repository<Person, Long> { … }
    
    @Entity
    @Document
    class Person { … }
  ```  
  @Person 既可以使用JPA仓库也可以使用MongoDB仓库.
  
  **注解驱动配置包**
  ```java
    @EnableJpaRepositories(basePackages = "com.acme.repositories.jpa")
    @EnableMongoRepositories(basePackages = "com.acme.repositories.mongo")
    class Configuration { … }
  ```
  
4. 定义查询方法
仓库代理有两种方式原语指定方法名的查询.
1. 通过方法名称得出查询
2. 使用手动定义的查询
依赖于存储设置进行配置,但是必须设置策略,这个策略决定了查询创建的方式.下部分内容描述了
可选的配置:
+ 查找策略
下述策略对仓库的基础设置进行查询.通过配置xml,可以配置这个策略的命名空间,通过设置
`query-lookup-strategy`属性。对于java的配置，可以使用查询策略@queryLookupStrategy
属性（启动Enable${store}Repositories注解）。一些策略可能不支持指定的数据存储。
+ `CREATE`请求用于构建来自指定方法名称的查询.通用的方式时移除给定方法前缀的集合.并
转换其余的方法.可与阅读冠以`查询创建`部分的内容.
+ `USE_DECLARED_QUERY`尝试找到声明的查询,且如果没有找到就抛出异常.这个产线可以使用
注解定义.可以参考指导存储器的相关文档.如果仓库没有找到声明的查询的时候就会失败.
+ `CREATE_IF_NOT_FOUND` 将`CREATE`和`USE_DECLARED_QUERY`联合在一起.首先,查找
声明的查询,如果没有查找到,则会创建基于指定名称的查询.如果不设置任何配置时,这个是默认
查找策略.

5. 创建查询
基于spring data仓库的查询构建器用于构建对于实例的查询.这个原理实验`find...By`,
`read...By`,`query...By`,`count...By`,`get...By`的方法,且启动转换.这个语法可以
包含更多的表达式.例如`Distinct`用于设置创建查询的唯一性.
但是设个动作实验分隔符分割,用于辨识实际的启动位置.可以在实例实现上定义条件,且使用`And`
或者`Or`进行连接.下面的实例显示了如何使用:
`由方法名称构建查询`
```java
interface PersonRepository extends Repository<Person, Long> {

  List<Person> findByEmailAddressAndLastname(EmailAddress emailAddress, String lastname);

  // Enables the distinct flag for the query
  List<Person> findDistinctPeopleByLastnameOrFirstname(String lastname, String firstname);
  List<Person> findPeopleDistinctByLastnameOrFirstname(String lastname, String firstname);

  // Enabling ignoring case for an individual property
  List<Person> findByLastnameIgnoreCase(String lastname);
  // Enabling ignoring case for all suitable properties
  List<Person> findByLastnameAndFirstnameAllIgnoreCase(String lastname, String firstname);

  // Enabling static ORDER BY for a query
  List<Person> findByLastnameOrderByFirstnameAsc(String lastname);
  List<Person> findByLastnameOrderByFirstnameDesc(String lastname);
}
```
6. 属性表达书
**属性表达式** 可以指定管理实例的直接属性. 在查询创建的时间,可以确定转换的属性是不是管理
的domain类的属性.但是,可以通过遍历属性来定义约束条件.考虑下述方法表达:
```markdown
List<Person> findByAddressZipCode(ZipCode zipCode);
```
假定`Person`类的`address`属性中包含`zipCode`.在这种情况下,方法创建了属性
x.address.zipCode属性.处理算法通过转换`AddressZipCode`开始,使其作为属性,且检查
domain类,找到这个的属性.如果算法执行成功,就会使用其属性.如果没有成功,算法会以驼峰为
分割点.实例`AddressZipCode`会分割成`AddressZip`和`Code`,如果找到这个属性(与head
相同),获取结尾(Code)且从这里构建一颗树.将结尾分割.如果head不匹配,算法则会向左移动.
(Address,ZipCode),且继续.
经过这个在大多数情况下都能正常工作,当时也有可能算法算错了属性值.假定`Person`类有个
addressZop属性的话.这个算法会匹配上钩分割匹配结果,这样可能就选错值了.
为了解决冲突,可以使用`_`分割,指定子属性的嵌套问题,可以如下定义:
```markdown
List<Person> findByAddress_ZipCode(ZipCode zipCode);
```

7. 指定参数的处理
为了解决查询中的属性问题.仓库的基础设施将指定类型视作可分页`Pageable`,且可排序`Sort`
.为了可以动态的进行分页和排序,下述示例证明了这些特征.
+ 使用`Pageable`,`Sort`,`Slice`在查询方法中
```markdown
Page<User> findByLastname(String lastname, Pageable pageable);

Slice<User> findByLastname(String lastname, Pageable pageable);

List<User> findByLastname(String lastname, Sort sort);

List<User> findByLastname(String lastname, Pageable pageable);
```
第一个方法传递@org.springframework.data.domain.Pageable实例给查询方法,且静态地
将分配添加到指定查询中.一个页面可以得知元素的总是和可以获得的分页数量.通过触发计数查询
,用于计算所有的数量,也可以使用@slice,一个切片可以知道下一个切片是什么,这个在运行大量
数据时是高效的.
排序通过@Pageable实例进行处理,如果仅仅需要排序,将@org.springframework.data.domain.Sort
添加到你的方法中。可以看到,返回一个列表,在这种情况下,额外需要的元数据去构建实际分配
@Page实例.相反,它限制了产线指定范围内的实例.

8. 排序和分页
+ 简单排序
```markdown
Sort sort = Sort.by("firstname").ascending()
  .and(Sort.by("lastname").descending());
```

+ 使用类型安全的API定义排序表达式
```markdown
TypedSort<Person> person = Sort.sort(Person.class);

TypedSort<Person> sort = person.by(Person::getFirstname).ascending()
  .and(person.by(Person::getLastname).descending());
```
+ 使用Querydsl API定义排序表达式
```markdown
QSort sort = QSort.by(QPerson.firstname.asc())
  .and(QSort.by(QPerson.lastname.desc()));
```

9. 限制查询结果
使用`Top`,`First`限制查询结果
```markdown
User findFirstByOrderByLastnameAsc();

User findTopByOrderByAgeDesc();

Page<User> queryFirst10ByLastname(String lastname, Pageable pageable);

Slice<User> findTop3ByLastname(String lastname, Pageable pageable);

List<User> findFirst10ByLastname(String lastname, Sort sort);

List<User> findTop10ByLastname(String lastname, Pageable pageable);
```

9. 使用仓库方法返回集合或者迭代器属性
使用`Streambale`作为查询的返回类型.返回的类型为java的`Iterable`,`List`,`Set`.
`Streamable`可以用于迭代器或者任何类型的集合类型.提供简便的方法,用于获取非并行流,
可以使用`...filter(...)`,和`...map(...)`,将`Streambale`连接到其他类型.
+ 使用`Streamable`,用于合并查询方法的结果
```markdown
interface PersonRepository extends Repository<Person, Long> {
  Streamable<Person> findByFirstnameContaining(String firstname);
  Streamable<Person> findByLastnameContaining(String lastname);
}

Streamable<Person> result = repository.findByFirstnameContaining("av")
  .and(repository.findByLastnameContaining("ea"));
```

+ 返回自定义的Streamable包装类型
示例:
```java
class Product {  // 这个类暴露API,用于获取产品的价格@getPrice
  MonetaryAmount getPrice() { … }
}

@RequiredArgConstructor(staticName = "of")
class Products implements Streamable<Product> { 
  // Streamable<Product>的包装类,可以通过Products.of构建(通过lombok注解创建的工厂方法)
  	

  private Streamable<Product> streamable;
  // 额外的API用于计算Streamable<Product>的新值
  public MonetaryAmount getTotal() { 
    return streamable.stream() //
      .map(Priced::getPrice)
      .reduce(Money.of(0), MonetaryAmount::add);
  }
}

interface ProductRepository implements Repository<Product, Long> {
	// 可以用于查询方法的保证类型,直接返回类型的名称@Products,不需要返回@Stremable<Product> 
  Products findAllByDescriptionContaining(String text); 
}
```

10. 支持vrvr集合
`vrvr`是一个java中的功能性程序概念.
|vrvr集合类型|vrvr类型实现|可用的java类型|
|io.vavr.collection.Seq| io.vavr.collection.List |java.util.Iterable|
|io.vavr.collection.Set| io.vavr.collection.LinkedHashSet|java.util.Iterable|
|io.vavr.collection.Map|io.vavr.collection.LinkedHashMap|java.util.Map|

11. 仓库方法空值处理
在spring data 2.0中,仓库的CRUD的方法会返回使用Java 8 聚合的实例@Optional,用于退出可能缺失
的值.除此之外,spring data支持查询方法的保证类型:
这里的@Optional指的是如下类型:
+ `com.google.common.base.Optional`
+ `scala.Option`
+ `io.vavr.control.Option`
此外,还可以不选择包装类型,使用`null`推测查询结果的缺失.正常情况下,查询方法可以返回集合,
包装类,以及包装不返回null的流.参考相应的`仓库的返回类型`.

12. 空值注解
可以在运行时提供空值检查
+ `@NonNullApi`: 使用在包级别上,用于声明参数的默认行为,返回的值不能就收空值,也不能产生空值
+ `@NotNull`: 使用在参数上,返回的值不能是null
+ `@Nullable`: 使用在参数上,可以是空值
spring注解使用元数据进行注解,使用JSR-305注解. 需要激活包级别的非空设置(在`package-info.java`
中使用@NonNullApi  注解),开启查询方法的运行时检查和空值约束条件

13. 流式查询结果
查询结果可以使用java 8 的流式对象@Stream<T>增量式查询.指定存储的查询方法用于在streaming
上运行,而不是包装查询结果,示例如下:
```markdown
@Query("select u from User u")
Stream<User> findAllByCustomQueryAndStream();

Stream<User> readAllByFirstnameNotNull();

@Query("select u from User u")
Stream<User> streamAllPaged(Pageable pageable);
```
使用try...catch处理@Stream<T>
```markdown
try (Stream<User> stream = repository.findAllByCustomQueryAndStream()) {
  stream.forEach(…);
}
```
> 注意: 不是所有的spring data 模块都支持@Stream<T>作为返回类型

14. 异步查询
仓库的查询可以异步运行(通过使用Spring异步方法,返回异步指向任务@Future).这个方法在你
将查询提交到spring的@TaskExecutor上然后立即返回.异步查询的方式不同意同步查询,且不
可以混合指向(需要控制好读写锁的问题).参照指定的文档,获取同步查找的支持.下面是示例:
```markdown
// 使用java.util.concurrent.Future 作为返回类型
@Async
Future<User> findByFirstname(String firstname);               

// 使用java8 的@java.util.concurrent.CompletableFuture 作为返回
@Async
CompletableFuture<User> findOneByFirstname(String firstname); 
// 使用spring框架的@org.springframework.util.concurrent.ListenableFuture
// 作为返回方式
@Async
ListenableFuture<User> findOneByLastname(String lastname); 
```

15. 创建仓库实例
在这个部分中，可以创建一个实例，并对于指定的仓库接口创建bean的定义。一种方式是使用
spring的namespace,用于传递每个spring data模块,用于支持仓库,即使如从还是建议使用java配置.
+ xml配置
通过xml启动xml仓库
```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans:beans xmlns:beans="http://www.springframework.org/schema/beans"
  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
  xmlns="http://www.springframework.org/schema/data/jpa"
  xsi:schemaLocation="http://www.springframework.org/schema/beans
    https://www.springframework.org/schema/beans/spring-beans.xsd
    http://www.springframework.org/schema/data/jpa
    https://www.springframework.org/schema/data/jpa/spring-jpa.xsd">

  <repositories base-package="com.acme.repositories" />

</beans:beans>
```

+ 使用过滤器
 默认情况下,会捡起基础指定仓库的每个接口.但是,如果需要更细粒度的空值,可以使用
 `<include-filter />`和<exclude-filter />元素,放在<repositories />中,用于对
 指定元素的空值,比如.参考spring 参考文档获取这些元素的设置.
 例如,将指定的接口配置为bean,可以参考如下配置:
 ```xml
 <repositories base-package="com.acme.repositories">
  <context:exclude-filter type="regex" expression=".*SomeRepository" />
 </repositories>
 ```
 
 + 使用JavaConfig配置
 这个仓库可以使用指定的注解触发(@Enable${store}Repositories),使用在一个java配置类上.
 对于一个基于spring容器的java配置,可以参考spring 参考文档.
 ```java
@Configuration
@EnableJpaRepositories("com.acme.repositories")
class ApplicationConfiguration {

  @Bean
  EntityManagerFactory entityManagerFactory() {
    // …
  }
}
 ```
 
 16. 自定义Spring Data Repositories
 这个部分覆盖了仓库的自定义配置.
 + 自定义单个仓库
 使用自定义配置丰富仓库,必须先定义一个接口和自定义功能的实现.示例如下:
 > 自定义仓库功能接口
 ```java
 interface CustomizedUserRepository {
  void someCustomMethod(User user);
 }
 ```
 > 实现自定义仓库
 ```java
class CustomizedUserRepositoryImpl implements CustomizedUserRepository {

  public void someCustomMethod(User user) {
    // 你的实现
  }
}
 ```
注意: 实现需要添加`Impl`后缀
这个实现自己是不会依赖于spring data的,且可以是普通的spring bean.从结果上来看,可以使用
标准依赖注入,用于注入引用到其他bean中(例如JDBCTemplate).然后可以使你的接口自定义的接口.
```java
interface UserRepository extends CrudRepository<User, Long>, CustomizedUserRepository {

  // Declare query methods here
}
```
继承自定义接口和@CrudRepository,且暴露给客户端.
spring data仓库通过自定义接口实现.这个自定义的接口是基础仓库,功能性方法(QueryDsl).
以及自定义接口和它的实现.每次添加接口到仓库的时候,需要添加片段.基础仓库和仓库方面由spring
data模块实现.
使用片段实现:
```java
interface HumanRepository {
  void someHumanMethod(User user);
}

class HumanRepositoryImpl implements HumanRepository {

  public void someHumanMethod(User user) {
    // Your custom implementation
  }
}

interface ContactRepository {

  void someContactMethod(User user);

  User anotherContactMethod(User user);
}

class ContactRepositoryImpl implements ContactRepository {

  public void someContactMethod(User user) {
    // Your custom implementation
  }

  public User anotherContactMethod(User user) {
    // Your custom implementation
  }
}
```
仓库由多个自定义实现组成,这些按照声明顺序导入.客户端实现由较高的优先级(相对于基本实现
来说).且需要解决两个片段接口的起义.仓库拍脑袋不限于使用在单个仓库接口中.多个仓库使用一个
片段接口,使得你可以重用通过不同仓库的自定义配置.
使用片段覆盖`save()`
```java
interface CustomizedSave<T> {
  <S extends T> S save(S entity);
}

class CustomizedSaveImpl<T> implements CustomizedSave<T> {

  public <S extends T> S save(S entity) {
    // Your custom implementation
  }
}
```

16. 配置
如果使用了命名空间配置,这个配置基础设置尝试自动发现片段的自定义实现,主要通过扫描包下的类.
这个类需要遵守命名构造法(用于添加命名空间的元素的属性到分片接口名称中).这个名称的后缀就是
`Impl`.下述示例显示一个仓库,这个仓库可以使用默认后最,且这个仓库设置了自定义后缀.
```markdown
<repositories base-package="com.acme.repository" />
<repositories base-package="com.acme.repository" repository-impl-postfix="MyPostfix" />
```

17. 解决歧义
如果找到的匹配类名称有多个实现,且在不同的包下,spring data 使用bean名称用于分辨到底使用哪个.
给定下述两个@CustomizedUserRepository的自定义实现.第一个会被使用,其bean名称为
@customizedUserRepositoryImpl,这个匹配了分片名称接口(CustomizedUserRepository)
加上后缀(默认,impl)
> 解决歧义实现
```java
package com.acme.impl.one;

class CustomizedUserRepositoryImpl implements CustomizedUserRepository {

  // Your custom implementation
}
```
```java
package com.acme.impl.two;

@Component("specialCustomImpl")
class CustomizedUserRepositoryImpl implements CustomizedUserRepository {

  // Your custom implementation
}
```
可以看出这两个虽然在不同的包下面,但是都满足了类名匹配的条件.第二个使用了
`@Component("specialCustom")`注解,且bean之后添加了后缀`Impl`,所以选中了第二个.

+ 手动布线
> 管理手动布线的自定义实现
```xml
<repositories base-package="com.acme.repository" />
<!-- id -> 类名称-->
<beans:bean id="userRepositoryImpl" class="…">
  <!-- further configuration -->
</beans:bean>
```

+ 自定义基本仓库
这个方法描述在部分区域进行,如果需要自定义基础仓库,需要每个仓库的自定义接口.这样做所有
的仓库都会受到影响.为了对所有仓库进行修改,可以创建一个实现,这个实现基础了指定仓库的持久化
基础类.这个类按照自定义的基本类运行,用于对仓库进行代理,示例如下:
```java
class MyRepositoryImpl<T, ID>
  extends SimpleJpaRepository<T, ID> {

  private final EntityManager entityManager;

  MyRepositoryImpl(JpaEntityInformation entityInformation,
                          EntityManager entityManager) {
    super(entityInformation, entityManager);

    // Keep the EntityManager around to used from the newly introduced methods.
    this.entityManager = entityManager;
  }

  @Transactional
  public <S extends T> S save(S entity) {
    // implementation goes here
  }
}
```
最后一步需要使得spring data基础组件意识到自定义的仓库基础类的存在，在java配置中，可以
通过使用`@Enable${store}Repositories`注解配置仓库基础类@repositoryBaseClass属性。
示例如下:
```java
@Configuration
@EnableJpaRepositories(repositoryBaseClass = MyRepositoryImpl.class)
class ApplicationConfiguration { … }
```
+ 使用XML配置
```xml
<repositories base-package="com.acme.repository"
     base-class="….MyRepositoryImpl" />
```

18. Querydsl插件
`Querydsl`可以通过API配置类型sql的查询.多个spring data 模块可以使用`Querydsl`
整合@QuerydslPredicateExecutor,示例如下:
```java
public interface QuerydslPredicateExecutor<T> {
	// 查询满足@predicate的单个实例
    Optional<T> findById(Predicate predicate);  
    // 返回满足所有@predicate的实例
    Iterable<T> findAll(Predicate predicate);   
    // 返回所有@predicate的计数值
    long count(Predicate predicate);            
    // 返回满足@predicate的实例是否存在
    boolean exists(Predicate predicate);
    // … more functionality omitted.
}
```
利用`Querydsl`支持仓库接口上的`QuerydslPredicateExecutor `,示例如下:
```java
interface UserRepository extends CrudRepository<User, Long>, QuerydslPredicateExecutor<User> {
}
```

19. web支持插件
+ 使用JavaConfig启动spring data 的web支持
```java
@Configuration
@EnableWebMvc
@EnableSpringDataWebSupport
class WebConfiguration {}
```
+ 使用XML配置spring data配置web支持
```xml
<bean class="org.springframework.data.web.config.SpringDataWebConfiguration" />

<!-- If you use Spring HATEOAS, register this one *instead* of the former -->
<bean class="org.springframework.data.web.config.HateoasAwareSpringDataWebConfiguration" />
```

20. 基础web支持
上个配置注册了一些组件
+ `DomainClassConverter` : 让springMVC从请求参数中处理基于仓库管理的domain类
> 可以在springMVC的controller中直接设置,这样就可以手动通过仓库查找实例
```java
@Controller
@RequestMapping("/users")
class UserController {

  @RequestMapping("/{id}")
  String showUserForm(@PathVariable("id") User user, Model model) {

    model.addAttribute("user", user);
    return "userForm";
  }
}
```
这样就可以查找`User`实例,这个实例通过springMVC转换路径变量成domain的id类型,最终通过`findById(...)`查找实例
+ `HandlerMethodArgumentResolvers `: 让spring MVC处理请求参数中的pageable和sort示例

> pageable请求参数预估

|page|size|sort|

|需要检索的页号,默认为0|页的大小,默认为20|需要参与排序的属性,形式为`property,property(,ASC|DESC).`
默认为升序.如果需要使用多个参数进行排序时需要选择,例如`?sort=firstname&sort=lastname,asc`|

21. 页超媒体支持
Spring HATEOAS 代表的类@PagedResources可以扩充类，使得页额元数据连接到客户端指定的页。
Page-> PagedResources 由spring HATEOAS的`ResourceAssembler`接口下面的`PagedResourcesAssembler`
实现.下面的示例显示了`PagedResourcesAssembler`作为控制器方法的使用.
```java
@Controller
class PersonController {
  @Autowired PersonRepository repository;
  @RequestMapping(value = "/persons", method = RequestMethod.GET)
  HttpEntity<PagedResources<Person>> persons(Pageable pageable,
    PagedResourcesAssembler assembler) {
    Page<Person> persons = repository.findAll(pageable);
    return new ResponseEntity<>(assembler.toResources(persons), HttpStatus.OK);
  }
}
```

22. web数据端口支持
可以使用json路径表达式描述spring data的请求情况
如下：
使用jsonPath或者XPath描述HTTP负载绑定
```java
@ProjectedPayload
public interface UserPayload {

  @XBRead("//firstname")
  @JsonPath("$..firstname")
  String getFirstname();

  @XBRead("/lastname")
  @JsonPath({ "$.lastname", "$.user.lastname" })
  String getLastname();
}
```

23. QSLdsl的web支持
考虑下面的查询
```shell
?firstname=Dave&lastname=Matthews
```
给定用户对象,可以使用@QuerydslPredicateArgumentResolver 处理查询
```shell
QUser.user.firstname.eq("Dave").and(QUser.user.lastname.eq("Matthews"))
```

添加@QuerydslPredicate 到方法上,就可以提供@Predicate,可以使用@QuerydslPredicateExecutor运行

