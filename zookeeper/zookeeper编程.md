#### zookeeper数据模型

与分布式文件系统类似,zk的命名空间时分层的.不同的是每个阶段可以拥有数据和子节点，就像文件系统中文件可以是文件或者目录类似.阶段路径总是使用绝对路径表示,没有相对引用的使用.

+ 空字符串不能是路径名称

+ 下述字符不能使用

  `\u0001 - \u001F`以及`\u007F-\u009F`

  `\ud800-u8FF`以及`\uFFF0-UFFFF`

+ 字符`.`可以作为名称的一部分,但是`.`和`..`不能够单独使用

##### znodes

zk树中的每个阶段是一个znode.znode维护一个状态数据结构,包含数据改变的版本号, acl的变化.状态数据结构拥有时间戳,状态版本以及时间戳.允许zk去验证缓存以及进行更新.每当znode数据改变的时候,版本号就会增加.

例如,无论何时客户端检索数据,总是会收到数据的版本.且客户端会对其进行更新或者删除.当znode数据变化的时候,必须要提供版本.如果不符合现在的版本号,那么更新就会失败.

##### 观察者

客户端可以观察znode,znode的变化会触发监视,然后会清除监视.当监测器触发的时候,zk给客户端发送一条提示.

##### 数据获取

数据存储在每个znode中,可以原子性的读写.读取会获取znode相关的所有数据,写出会代替所有数据.每个阶段都有访问控制列表(ACL),这个可以限定谁可以访问.

zk没有设计全局数据库或者是大型的存储.相反地,管理zk自己协调数据.这个数据可以通过配置的形式传入.通常数据的属性都是相当地小,可以使用kb来度量.

zk客户端和服务端实现有清晰的检查,用于保证znode至少有1M的数据,但是数据不能小于平均值.相关大数据量的操作会导致耗时过长.且会影响影响到其他操作.

如果需要存储大量的数据,通常的形式是存储在大量的存储系统上,比如NFS或者HDFS.在zk中存储这些数据的存储指针即可.

##### 临时节点

zk有临时节点的概念,znode只要会话就是处于激活状态.当会话结束的时候,znode就会被删除.由于临时节点不允许有子节点.临时节点列表可以使用API getEphemerals()检索.

这个API是检索当前会话的临时节点,如果路径为空,会列举所有的临时节点.

>使用情况：
>
>如果需要收集会话中的临时节点，且你不知道创建的节点名称可以使用这个API检索

##### 序列节点（全局唯一）

当创建一个znode的时候，可以请求zk添加一个监视使用的计数器置于路径最后。这个监视器对于znode的父节点来说是唯一的。这个计数器的形式为`%010d`,是一个十位数的10进制数量.

##### 容器节点

zk 3.6.0中新增的功能,对于znode有特殊的作用,可以作为leader,也可以作为lock.当容器的最后一个节点删除的时候,整个容器也可以被删除.

基于存在这样的参数,可以获取`KeeperException.NoNodeException`.当在容器节点中创建一个子节点的时候需要检查这个异常,且如果存在异常需要重新创建容器.

##### TTL节点

zk 3.6.0开始使用.当创建一个`PERSISTENT`或者`PERSISTENT_SEQUENTIAL`的znode节点的时候,可以设置TTL的znode如果znode在TTL时间内没有被修改,且没有子节点.之后就可以从服务器中删除.

> 注意:
>
> TTL节点必须通过系统属性开启,默认情况下被关闭.如果需要创建TTL节点,且不使用系统参数,会抛出异常

#### zookeeper时间

zk可以使用如下4中方式定位时间

* **Zxid**

  zk的状态改变会使用zxid记录下来.zk的改变会暴露全局顺序.每个变化都有唯一的zxid.如果zxid1小于zxid2.那么zxid1发生在zxid2之前.

* **Version numbers**
  
  版本编号,每个阶段的编号都会引起版本编号的增长.包括znode数据变化版本号,znode子节点变化版本号,ACL变化版本号.
  
* **Ticks**
  
  使用多个服务器的zk的时候,服务器使用tick去定义事件的时间单位.
  
* **Real time**
  
  实际事件,zk不使用实际事件,除非将时间戳放进状态数据结构中创建znode/修改znode.

#### zookeeper状态数据结构

每个znode的状态数据结构包含如下属性

* **czxid**

  znode创建引起的zxid的变化

* **mzxid**
  
  znode上次修改的zxid
  
* **pzxid**
  
  znode上次修改子节点的zxid编号
  
* **ctime**
  
  znode创建的时间点
  
* **mtime**
  
  znode上次修改的时间点
  
* **version**
  
  znode数据改变的版本
  
* **cversion**
  
  znode子节点改变的版本
  
* **aversion**
  
  znode ACL的变化版本
  
* **ephemeralOwner**
  
  znode的会话编号(如果znode是一个临时节点).如果不是临时节点,返回0
  
* **dataLength**
  
  znode数据域的长度
  
* **numChildren**
  
  znode子节点的数量

#### ZooKeeper 会话

zk客户端完成了与zk服务的会话。通过创建服务处理器。一旦创建成功，处理器会离开连接状态。且客户端尝试去连接处于连接状态的服务器。

在正常的操作中，客户端处理会是两个状态其一。如果发生了为恢复的状态，例如会话超时或者授权失败，或者显示地关闭处理，都会导致错误.

为了创建一个客户端会话,应用代码必须提供链接,这个链接包含逗号分割的链接信息.例如,`127.0.0.1:3000,127.0.0.1:3001,127.0.0.1:3002`.zk客户端会获取任意一个服务器,并且尝试连接.如果连接失败了,或者客户端与服务器断开连接,客户端会自动连接下一个服务器.直到连接完成.

#### zookeeper观察者

zk的所有读取操作，**getData()**, **getChildren()**, 和**exists()**。否有观察者的配置项。这里是zk观察者的定义,观察事件是在一个时刻触发的,当数据的监视改变的时候会,发送到监视的客户端.显示是观察者的几个关键点:

+ 一次性触发

  观察者在数据改变的时候会发送到客户端,例如客户端进行了`getData("/znode1",true)`指令，之后`/znode1`节点删除,客户端会获取观察`/znode1`的观察者.如果`/znode1`改变的时候,只有在客户端完成读取和设置观察者的时候才能停止发送观察者.

+ 发送到客户端

  这个表示事件是客户端的一种方式,但是没有返回操作改变代码的时候是不会发送到客户端的.观察者会异步发送,zk提供了顺序了保证.一个客户端直到首次观测事件之前是不会改变客户端的.

  网络延时或者其他因素会导致不同的观测客户端和不同时间的返回代码.重要的一点是不同的客户端有这一致性的顺序.

+ 观察者设置的位置

  这个指的是节点变化的方式,zk位置两个观察者类别,数据观察者和客户端观察者.`getData`以及`exists`设置的是数据观察器,`getChildren`设置了子观察器.

  不同的是,通过返回的数据类型考虑观察器的类型.`getData`以及`exists`会返回节点数据类型.`getChildren`会返回子节点列表.因此,`setData`会触发znode设置的数据观察者.

  一次`create`成功操作会触发数据观察者和子节点观察者.一次成功的`delete`操作会触发数据观察者和子节点观察者.

  观察者有客户端连接的zk服务器本地维护,运行观察者轻量级设置,维护和分配.当客户端连接到一个新的服务器的时候,观察者会被任何会话事件触发.

  观察者在断开服务器连接的时候是不会接受观察者的。当客户端重新连接的时候，之前注册的观察者会重新注册。

  总体来说，这些动作都是显式地发生。

#### 观察者语义

可以通过读取zk状态位设置观察者，包括`exists`,`getData`,`getChildren`.下述列出了事件的信息:

+ **创建事件**: `exists`
+ **删除事件**: `exists`,`getData`,`getChildren`
+ **改变事件:**`exists`,`getData`
+ **子事件**: `getChildren

#### 持久化的观察者

3.6.0版本新特性,持久化监视器可以通过方法`addWatch()`添加,触发的语言保证和标准观察者一致.唯一的不同就是迭代持久化的观察者不会触发子节点改变的时间.

使用`removeWatches`昂发移除观察者.

#### 移除观察者

可以移除注册到znode的观察者,通过调用`removeWatches`方法同样zk客户端可以本地移除.下述时间会在成功移除之后触发.

* **移除子节点:**
  `getChildren`
* **数据移除事件:**
  `exists`  `getData`
* **持久化移除事件:**



#### zookeeper观察者的保证措施

对于观察者,zk做出了如下的保证:

+ 观察者需要考虑到其他事件,其他观察者以及需要异步响应.zk客户端保证按照顺序分配
+ 客户端能够看到znode的观察者
+ zk观测事件的顺序与zk服务更新不同顺序相关

#### 观察者的注意事项

+ 标准观察者是一次性触发的,如果需要对未来的改变获取提示,必须要设置新的观察者
+ 由于标准观察者是一次性触发的,所以获取时间和发送新的观察者获取请求之间是存在延时的,不能希望看到zk节点的每个变化.注意到znode多次改变的情况处理.

* 观察者实例只能触发一次
* 如果断开服务器的连接,直到服务器重新连接之前不能够获取任何的观察者.



### Zookeeper的访问控制列表

zk是ACL控制znode的访问权限.ACL实现类似于unix文件权限.它是由了权限符去开启/关闭操作的权限.与标准Unix不同的是,zk的node没有受到用户,组,其他三项属性的显示.zk没有znode持有者的语义.相反,ACL指定了id的列表和相关ID的权限.

注意到ACL只会从属于指定的阶段.zk支持可插入式授权.id列表使用`schema:expression`形式指定.schema是授权的schema.

当客户端连接到zk和授权的时候,zk连接所有的id列表.当客户端获取节点的时候,这些id列表通过znode的ACL相互检查.ACL的形式为`_(scheme:expression,perms)`.

#### ACL权限

zk支持下述权限

* **CREATE**: 创建子节点
* **READ**: 读取节点的数据和子节点信息
* **WRITE**: 设置节点的数据
* **DELETE**: 删除子节点
* **ADMIN**: 设置权限

#### 内嵌ACL schema

zk的schema中可以设置下述配置

+ **world**,包含单个id,代表任意一个

+ **auth**: 是一种特殊的schema,可以忽视指定的表达式,相反的使用当前用于,授权和schema.

  任何表达式都会被zk服务器在持久化ACL的时候忽略.但是表达式依旧需要提供,因为ACL必须匹配与`scheme:expression:perms`的形式。schema是通常的方式提供，用于创建znode，然后限制znode的权限。如果没有授权用户，schema会设置失败。

+ **digest** 使用`username:password`用于生成MD5 hash值.这个值作为ACL编号.授权通过发送`username:password`信息.当使用ACL表达式会使用SHA1编码.

* **ip**使用客户端IP和ACL ID标识.ACL表达式是`addr/bits`的形式
* **x509**使用X500准则,acl表达式是X500准则.当使用安全端口的时候,客户端自动授权,且设置x509的授权信息.

#### ZooKeeper C 客户端 API

| API                   | 功能                     |
| --------------------- | ------------------------ |
| _int_ ZOO_PERM_READ   | 读取节点的值和子节点列表 |
| int_ ZOO_PERM_WRITE   | 设置节点的值             |
| _int_ ZOO_PERM_CREATE | 创建子节点               |
| _int_ ZOO_PERM_DELETE | 删除子节点               |
| _int_ ZOO_PERM_ADMIN  | 执行`set_acl()`          |
| int_ ZOO_PERM_ALL     | 执行上述功能             |

#### 可插入式zookeeper授权

zk可以运行在不同的环境下，使用可插入式的授权框架。尽管内嵌的授权schema页使用了可插入式授权框架。

为了了解授权框架的工作原理，必须立即两种主要的授权操作。框架首先必须授权客户端，只要客户端连接到服务器，且包含合法的信息就会生成。

第二个操作由框架处理，这个操作是找打ACL的条目信息。ACL条目信息为`<idspec,permission>`。idspec可能是单个字符串。用于匹配相应的授权信息。这个主要是取决于授权插件匹配的实现方案。下面是授权插件的接口，插件必须要对其进行实现。


```java
public interface AuthenticationProvider {
    String getScheme();
    KeeperException.Code handleAuthentication(ServerCnxn cnxn, byte authData[]);
    boolean isValid(String id);
    boolean matches(String id, String aclExpr);
    boolean isAuthenticated();
}
```

`getScheme `返回插件的标识符.因为支持多个授权的方法,授权或者`idspec`总是使用`scheme:`前缀.zk服务器使用授权插件返回的schema.用于决定需要使用那些schema.

`handleAuthentication`在客户端发送授权信息的时候调用,用于联系连接.这个方法指定了schema.zk服务器会传递信息给授权插件.

zk调用`matches`方法去检查ACL.需要匹配客户端授权信息.为了查找客户端的条目信息,zk服务器会找到每个条目的schema.且如果有客户端的授权信息,`matches`会使用指定id集合调用到预先添加的授权信息.

授权插件使用自己的逻辑去匹配schema,决定是否包含在`aclExpr`.

有两种授权插件,`ip`和`digest`.其他插件可以添加系统参数,在启动zk服务器的时候,会查找以`zookeeper.authProvider`开头的系统参数.在类名之前进行重点.

例如`-Dzookeeeper.authProvider.X=com.f.MyAuth`就相当于添加了如下配置:


```shell
authProvider.1=com.f.MyAuth
authProvider.2=com.f.MyAuth2
```

zk 3.6.0新增功能,提供了可插入式授权的可修改抽象


```java
public abstract class ServerAuthenticationProvider implements AuthenticationProvider {
    public abstract KeeperException.Code handleAuthentication(ServerObjs serverObjs, byte authData[]);
    public abstract boolean matches(ServerObjs serverObjs, MatchValues matchValues);
}
```

这里不需要实现`AuthenticationProvider`,而是需要继承`ServerAuthenticationProvider`

* **ZooKeeperServer**
  zk服务器实例
  
* **ServerCnxn**

  当前连接

* **path**
  
  znode路径
  
* **perm**
  
  操作值
  
* **setAcls**
  
  是否开启`setACl()`方法

#### 一致性保障

zk是高性能稳定的服务.读取和写入的操作都是非常快速的,尽管读取要比写入块.原因是,读取的时候,zk可以对旧的数据进行读取.zk一致性保证:

+ 序列一致性

  客户端的更新会按照顺序发送

+ 原子性

  更新要么成功要么失败,不存在局部成功的情况

* 单系统镜像

    无论服务器连接到哪里,客户端都会看到相同的服务视图

* 可靠性
  
    一旦进行更新,就会在客户端重写的时候进行持久化.
    
* 时效性

    客户端保证在指定范围内是最新的.每个系统的变化都可以在规定时限内被客户端看到,或者客户端可以发送服务.

#### 绑定

##### java绑定

###### 客户端配置参数

| 属性                                          | 介绍                                                         |
| --------------------------------------------- | ------------------------------------------------------------ |
| *zookeeper.sasl.client*                       | 设置false则关闭SASL授权，默认为true                          |
| *zookeeper.sasl.clientconfig*                 | 指定IAAS记录文件，默认为`Client`                             |
| *zookeeper.server.principal*                  | zk服务器准则,用于客户端授权,当连接服务器的时候使用.如果开启了kerberos授权,提供了这个参数,zk就不会使用username,hostname.realm参数, |
| *zookeeper.sasl.client.username*              | 典型kerberos v5版本的用户名称.默认服务器为`username/IP@realm` |
| *zookeeper.sasl.client.canonicalize.hostname* | 主机名称                                                     |
| *zookeeper.server.realm*                      | 服务器的域,默认是客户端的域                                  |
| *zookeeper.disableAutoWatchReset*             | 是否关闭自动重置观察者的功能                                 |
| *zookeeper.client.secure*                     | 如果需要理解服务器客户端安全端口,需要设置这个属性为true      |
| *zookeeper.clientCnxnSocket*                  | 指定客户端连接socket端口                                     |
| *zookeeper.ssl.keyStore.location*             | zk SSLkey存储的位置                                          |
| *zookeeper.ssl.keyStore.password*             | zk SSLkey存储的密码                                          |
| *zookeeper.ssl.trustStore.location*           | zk 授权信息的存储位置                                        |
| *zookeeper.ssl.trustStore.password*           | zk授权信息的密码                                             |
| *jute.maxbuffer*                              | 客户端指定服务器端传来的最大缓冲区大小,默认为0xfffff         |
| *zookeeper.kinit*                             | zk kinit路径,默认`/usr/bin/kinit`                            |

##### c绑定

c的绑定包含1单线程和多线程版本.多线程版本方便使用,且相似于java API.这个库会创建IO线程,且使用时间分配线程用于对连接的维护和回调.

单线程版本允许zk用于事件驱动的应用中,主要是通过在多线程环境中保留事件环.

这个包包含了两个共享的库，zk_st和zk_mt.前缀提供异步API.

##### 安装方法

1.  在zk顶层目录(`.../trunk`)运行`ant compile_jute`,这个会在`.../trunk/zookeeper-client/zookeeper-client/zookeeper-client-c`目录

2. 检查这个目录,运行`autoreconf -if`去启动`autoconf`,`automake`和`libtool`,保证字段配置版本要大于2.59版本.跳转到4.

3. 如果使用源码安装,需要对这个目录解压

   `unzip  zookeeper-x.x.x/zookeeper-client/zookeeper-client-c*`

4. 运行`./configure <options>`用于生成makefile.

   + `--enable-debug`

     允许优化并开启编译器的debug信息.默认关闭

   + `--without-syncapi`

     是否关闭异步API.默认开启

   + `--disable-static`

     是否不构建静态资源库(默认开启)

   + `--disable-shared`

     是否不构建共享目录，默认开启

##### 安装C客户端

1. 包含zookeeper的头文件

   `#include <zookeeper/zookeeper.h>`

2. 如果构建多线程客户端,使用`DTHREADED`进行编译,然后连接zk_mt库.如果构建一个单线程客户端,不要使用这个参数去编译.

#### zookeeper操作指导

##### zookeeper连接

开始之前,需要运行zk的服务器,以便于可以对客户端进行开发.对于C客户端来说,可以使用多线程库.这里使用C的APU进行连接:

```c
int zookeeper_init(const char *host, watcher_fn fn, int recv_timeout, const clientid_t *clientid, void *context, int flags);
```

假设客户端输出“连接到zk”的信息,表示连接的信息。


```c
#include <stdio.h>
#include <zookeeper/zookeeper.h>
#include <errno.h>
using namespace std;

// Keeping track of the connection state
static int connected = 0;
static int expired   = 0;

// *zkHandler handles the connection with Zookeeper
static zhandle_t *zkHandler;

// watcher function would process events
void watcher(zhandle_t *zkH, int type, int state, const char *path, void *watcherCtx)
{
    if (type == ZOO_SESSION_EVENT) {

        // state refers to states of zookeeper connection.
        // To keep it simple, we would demonstrate these 3: ZOO_EXPIRED_SESSION_STATE, ZOO_CONNECTED_STATE, ZOO_NOTCONNECTED_STATE
        // If you are using ACL, you should be aware of an authentication failure state - ZOO_AUTH_FAILED_STATE
        if (state == ZOO_CONNECTED_STATE) {
            connected = 1;
        } else if (state == ZOO_NOTCONNECTED_STATE ) {
            connected = 0;
        } else if (state == ZOO_EXPIRED_SESSION_STATE) {
            expired = 1;
            connected = 0;
            zookeeper_close(zkH);
        }
    }
}

int main(){
    zoo_set_debug_level(ZOO_LOG_LEVEL_DEBUG);

    // zookeeper_init returns the handler upon a successful connection, null otherwise
    zkHandler = zookeeper_init("localhost:2181", watcher, 10000, 0, 0, 0);

    if (!zkHandler) {
        return errno;
    }else{
        printf("Connection established with Zookeeper. \n");
    }

    // Close Zookeeper connection
    zookeeper_close(zkHandler);

    return 0;
}
```

编译多线程库

`> g++ -Iinclude/ zkClient.cpp -lzookeeper_mt -o Client`

运行客户端

`> ./Client`