### zookeeper管理指南

#### 部署

这个部分包括zk的部署信息，包括下述内容

+ 系统需求
+ 集群构建
+ 单机构建

前两种情况假定你使用zk用于生成环境下,且作为数据中心使用。最后一种情况假定你使用在开发测试环境下.

#### 系统需求

##### 支持的平台

zk包含多个组件,一些组件可以广泛支持,另一些只能部分使用.

+ 客户端是java客户端,用于连接到zk节点
+ 服务端是java服务端,运行zk节点
+ 本地客户端是由C语言实现的客户端,与java客户端类型,用于连接zk节点

* Contrib指的是多项添加组件的配置.

下表显示了平台的支持情况:

###### Support Matrix

| Operating System | Client | Server | Native Client | Contrib |
|------------------|--------|--------|---------------|---------|
| GNU/Linux | Development and Production | Development and Production | Development and Production | Development and Production |
| Solaris | Development and Production | Development and Production | Not Supported | Not Supported |
| FreeBSD | Development and Production | Development and Production | Not Supported | Not Supported |
| Windows | Development and Production | Development and Production | Not Supported | Not Supported |
| Mac OS X | Development Only | Development Only | Not Supported | Not Supported |

#### 需要的软件

zk运行在java上,支持jdk 8,11,12(9和10不支持).以zk服务集群的形式运行,三个zk服务器就可以组成一个最小的zk集群.建议运行在不同的机器上.

#### 集群构建

为了获取可靠的zk服务,可以部署zk到集群中.只要集群启动了,服务就可以使用.因为zk需要集群的启动,集群中机器的数量最好是奇数.

> 例如,4台机器可以容错一台集群故障,而5台可以容错2台.

需要在集群的每台机器上进行如下操作:

1. [安装JDK](http://java.sun.com/javase/downloads/index.jsp)

2. 设置java堆空间大小,防止内存交换,从而降低zk性能.保守每台机器使用3-4G堆内存

3. 安装zk服务器的安装包,可以从官网[下载](http://zookeeper.apache.org/releases.html)

4.  创建配置文件,名称任意,使用下述配置:

   ```shell
   tickTime=2000
   dataDir=/var/lib/zookeeper/
   clientPort=2181
   initLimit=5
   syncLimit=2
   server.1=zoo1:2888:3888
   server.2=zoo2:2888:3888
   server.3=zoo3:2888:3888
   ```

   每台机器需要知道集群中的其他机器信息。可以使用`server.id=host:port:port`来配置.对于每台机器,首先知道Quorum端口,用于zk的leader选举.自从zk 3.6之后,可以指定多个zk地址.

   可以在每台服务器上创建名称叫做`myid`的文件,设置服务器id.这个目录存储在每台服务器的数据目录中,即配置中的`dataDir`.

   + myid文件包含机器ID的单行数据,所以`myid`服务器1会包含文本`1`,且没有其他东西.这个ID必须唯一,且值的范围在1-255之间.

     > 注意:
     >
     > 如果你开启的扩展特征,例如TTL节点,id范围必须是1-254

   + 创建与`myid`相同目录下的初始化文件.这个文件表明希望空数据.当存在的时候,就会创建空的数据库,且标记文件删除.当不存在的时候,空数据目录意味着这个节点不会拥有投票的权利,且不会在于leader沟通之前生成数据目录.

   + 上述配置完成之后,可以其他zk服务器:

     ```shell
     $ java -cp zookeeper.jar:lib/*:conf org.apache.zookeeper.server.quorum.QuorumPeerMain zoo.conf
     ```
   
     QuorumPeerMain会启动一个叫做**JMX**的服务器，管理JMX控制台的Bean。
   
     Zookeeper JMX文档中详细的描述了zk使用JMX的方法。详情参考`bin/zkServer.sh`的启动实例.
   
   + 测试部署情况
   
     ```shell
     $ bin/zkCli.sh -server 127.0.0.1:2181
     ```

#### 单机构建

如果需要在开发环境使用zk,需要按照单机zk实例,然后在开发的机器上按照java/C的客户端.

详情请参考**安装指导**的单机安装。

安装zk客户端，可以参考zk相关的绑定方法。

### Zookeeper 管理

---

#### 设计zk的部署

zk的可靠性依靠于两个基本假设:

1.  只有少数服务器会失败,这里失败指的是服务器宕机,或者是服务器发送了网络错误.
2.  部署机器操作成功.操作成功意味着执行代码成功,且时钟工作正常.且存储和网络没有异常.

下述内容包含zk管理器确保上述条件的最大可能性.其中一些是跨机器的交互,且需要考虑的是每台机器与其他事件的交互.

##### 跨机器需求

对于激活的zk服务,只有没有失败的机器才可以相互通信.为了创建容错性高的部署,如果有F台机器失败,那么至少需要部署2F+1台机器.因此,不是包含了机器处理失败的情况,且部署5台机器可以容错两台.所以zk集群通常含有奇数台机器.

为了获取高可用性能,需要尝试将各台集群的错误独立起来.例如,如果多台机器共享一个开关,name当前故障的时候,就会多台一起失败.所以需要享有不同的电源或者制冷系统等等.

##### 单机需求

如果zk需要和其他应用竞争存储设备,CPU,网络,内存,那么性能就会受到影响.zk具有较高的耐用性,意味着使用存储设备去记录变化(在其运行改变之前).需要注意这些依赖的同时,保证zk操作不会占用你的存储.这里是一些降级措施.

+ zk事务日志必须是一个专用的设备(专用分区不能达到要求).zk写出日志,不会寻找与其他进程的共享日志设备,会导致竞争出现,这样会导致延迟.
+ 不要让zk处于可能发生内存交换的状态下.在使用时间进行排序的时候,不允许内存交换.因此,确保最大堆内存不要大于zk的实际内存.

#### 持续的数据目录清理

zk数据目录包含持久化的znode副本,这些是快照和事务日志.随着znode的变化会添加代事务日志中.当这个日志变大的时候,当前znode状态的快照信息会被写入到系统中,且会创建新的事务日志文件.进行快照的过程中,zk可能继续将事务日志写入到旧的日志文件中.

默认情况下,zk服务器不会移除旧快照和日志文件.每个服务变量都是不同的,因此这些文件的管理需求就是不同的.

PurgeTxnLog提供了简单的实现.

下述示例中,上一个版本和相对应的日志被保留其他的日志被删除.这里的`<count>`需要大于3.这个需要作为定时任务运行,用于清理周期性的清除日志.

```shell
$ java -cp zookeeper.jar:lib/slf4j-api-1.7.5.jar:lib/slf4j-log4j12-1.7.5.jar:lib/log4j-1.2.17.jar:conf org.apache.zookeeper.server.PurgeTxnLog <dataDir> <snapDir> -n <count>
```

快照的自动清除在,zk 3.4.0之后引入.可以通过配置`autopurge.snapRetainCount`和`autopurge.purgeInterval`配置.

#### 清除debug日志(log 4j)

在使用log4j功能的时候,使用日志的滚动添加.配置文件在`conf/log4j.properties`中设置

#### 监督

有的时候需要监控zk服务器,zk服务器设置为快速失败,意味着如果不可恢复错误发生了会立即停止进程.作为实现zk,集群高可用的实现,这个意味着当集群服务器宕机且整个集群仍然在接受请求.此外,集群具有自愈特性,失败的服务器一旦重启就会重新加入集群中.

j使用`daemontools`或者`SMF`管理zk服务器,确保如果进程非正常瑞出,会自动重启并加入集群中.

同时建议配置zk服务器出了停止,和在OOM发生的时候倾倒堆内存.这个可以通过运行JVM的参数实现.可以使用`zkServer.sh`或者`zkServer.cmd`脚本实现:

```shell
-XX:+HeapDumpOnOutOfMemoryError -XX:OnOutOfMemoryError='kill -9 %p'

"-XX:+HeapDumpOnOutOfMemoryError" "-XX:OnOutOfMemoryError=cmd /c taskkill /pid %%%%p /t /f"
```

#### 监视

zk服务可以使用一种或者两种途径进行监视

+ 通过使用4个字母设置命令端口
+ JMX

#### 日志

zk使用了`slf4j 1.7.5`作为日志处理工具。

#### 参数配置

zk的行为有zk配置空间，如果使用了不同的配置文件，注意保证服务器列表在不同的配置中要保持匹配。

> 注意:
>
> 在3.5.0之后的版本,一些参数使用了动态配置.用于取代静态配置,zk会自动移除这些静态配置文件.

##### 最小化配置

下述属性构成最小化配置

+ **客户端端口号**: 客户端连接的端口号,客户端需要连接的端口号

+ **客户端安全端口号**:

  使用SSL监听客户端连接,开启SSL对客户端的安全连接监听.

  > 注意SSL功能需要在`zookeeper.serverCnxnFactory, zookeeper.clientCnxnSocket`中开启

* **观察者master端口**：
  
    观察者监听端口,这个端口观察者会连接.

* **数据目录**:

    zk存储内存数据库的位置,除非特殊指定,否则事务日志都会更新到数据库中

* **标记时间**:
  
    单个标记的时间长度,使用ms度量,zk基本时间单位,用于对心跳信息,超时时间进行度量.

##### 高级配置

| 参数名称               | 备注                                    |java参数|
| ---------------------- | --------------------------------------- |----|
| *dataLogDir*           | 数据日志目录,指定之后事务日志会写到这里 ||
| globalOutstandingLimit | 全局上限值                              |**zookeeper.globalOutstandingLimit**|
| *preAllocSize*         | 预分配内存大小                          |**zookeeper.preAllocSize**|
| *snapCount* | 快照计数值 |**zookeeper.snapCount**|
| *commitLogCount* | 内存保存的请求数量 |**zookeeper.commitLogCount**|
| snapSizeLimitInKb | 快照大小上限 |**zookeeper.snapSizeLimitInKb**|
| txnLogSizeLimitInKb | 用于直接控制事务日志写出,大数据量的时候会导致follower同步速度变慢. |**zookeeper.txnLogSizeLimitInKb**|
| *maxCnxns* | zk服务器并发连接上限 |**zookeeper.maxCnxns**|
| *maxClientCnxns* | 单机客户端最大并发连接上限 ||
| *clientPortAddress* | 客户端端口地址（ipv4,6） ||
| *minSessionTimeout* | 最小会话超时时间,默认为2*ticktime ||
| *maxSessionTimeout* | 最大会话超时时间,默认20*ticktime ||
| *fsync.warningthresholdms* | WAL警告日志写入需要保持的延时 |**zookeeper.fsync.warningthresholdms**|
| maxResponseCacheSize | 最大响应缓存大小，默认400 |**zookeeper.maxResponseCacheSize**|
| *maxGetChildrenResponseCacheSize* | 获取子节点最大响应缓存 |**zookeeper.maxGetChildrenResponseCacheSize**|
| *autopurge.snapRetainCount* | 快照保留最大值，默认为3 ||
| *autopurge.purgeInterval* | 清除任务的周期(单位h) ||
| *syncEnabled* | 是否异步写入事务日志和快照 |**zookeeper.observer.syncEnabled**|
| *extendedTypesEnabled* | 是否支持类型扩展 |**zookeeper.extendedTypesEnabled**|
| *watchManaggerName* | 是否观测manager的名称 |**zookeeper.watchManagerName**|
| *watcherCleanThreadsNum* | 观测器清除线程数量 |**zookeeper.watcherCleanThreadsNum**|
| *watcherCleanThreshold*           | 观测器清理上限 |**zookeeper.watcherCleanThreshold**|
| *watcherCleanIntervalInSeconds*   | 观测器清理周期 |**zookeeper.watcherCleanIntervalInSeconds**|
| *maxInProcessingDeadWatchers*     | 最大死亡观测器处理数量 |**zookeeper.maxInProcessingDeadWatchers**|
| *bitHashCacheSize*                | 批量hash缓存 |**zookeeper.bitHashCacheSize**|
| *flushDelay*                      | 刷新延时 |**zookeeper.flushDelay**|

...

##### 集群参数

| 参数名称                                | 介绍                                                         | java参数                                   |
| --------------------------------------- | ------------------------------------------------------------ | ------------------------------------------ |
| *electionAlg*                           | 选举算法<br />1代表非授权的UDP选举<br />2代表授权的UDP选举<br />3代表TCP选举 |                                            |
| *maxTimeToWaitForEpoch*                 | 分割点(epoch)最大等待时间                                    | **zookeeper.leader.maxTimeToWaitForEpoch** |
| *initLimit*                             | 初始化同步时间上限单位tick                                   |                                            |
| *connectToLearnerMasterLimit*           | 选举后<br />follower连接到leader的时间上限                   | zookeeper.**connectToLearnerMasterLimit**  |
| *leaderServes*                          | leader是否接受客户端连接,默认yes                             | zookeeper.**leaderServes**                 |
| *server.x=[hostname]:nnnnn[:nnnnn] etc* | 服务器配置                                                   |                                            |
| *syncLimit*                             | follower同步时间上限                                         |                                            |
| *group.x=nnnnn[:nnnnn]*                 | 组定义                                                       |                                            |
| *weight.x=nnnnn*                        | 权值定义                                                     |                                            |
| *cnxTimeout*                            | leader选举打开连接的最大时间                                 |                                            |
| *quorumCnxnTimeoutMs*                   | leader选举通知读取的超时时间                                 | zookeeper.**quorumCnxnTimeoutMs**          |
| standaloneEnabled                       | 是否允许独立运行<br />默认false                              |                                            |
| *reconfigEnabled*                       | 是否允许重新配置                                             |                                            |
| *4lw.commands.whitelist*                | 以逗号分隔的4部分<br />表达式白名单列表，其他名称默认是不允许使用的，可以设置为* |                                            |
| *tcpKeepAlive*                          | TCP保持存活状态                                              | **zookeeper.tcpKeepAlive                   |
| *clientTcpKeepAlive*                    | 客户端TCP是否维持存活                                        | **zookeeper.clientTcpKeepAlive**           |
| *electionPortBindRetry*                 | 选举端口绑定重试次数                                         | **zookeeper.electionPortBindRetry**        |
| *observer.reconnectDelayMs*             | 观察者重连延时                                               | **zookeeper.observer.reconnectDelayMs**    |
| *observer.election.DelayMs*             | 观察者选举延时                                               | **zookeeper.observer.election.DelayMs**    |



##### 加密,授权参数

| 参数名称                                     | java参数                                                 |
| -------------------------------------------- | -------------------------------------------------------- |
| *DigestAuthenticationProvider.superDigest*   | **zookeeper.DigestAuthenticationProvider.superDigest**   |
| *X509AuthenticationProvider.superUser*       | **zookeeper.X509AuthenticationProvider.superUser**       |
| *zookeeper.superUser*                        | **zookeeper.superUser**                                  |
| *ssl.authProvider*                           | **zookeeper.ssl.authProvider**                           |
| *zookeeper.ensembleAuthName*                 | **zookeeper.ensembleAuthName**                           |
| *zookeeper.sessionRequireClientSASLAuth*     | **zookeeper.sessionRequireClientSASLAuth**               |
| *sslQuorum*                                  | **zookeeper.sslQuorum**                                  |
| *ssl.keyStore.location*                      | **zookeeper.ssl.keyStore.location**                      |
| ssl.keyStore.password*                       | **zookeeper.ssl.keyStore.password**                      |
| *ssl.quorum.keyStore.location*               | **zookeeper.ssl.quorum.keyStore.location**               |
| *ssl.quorum.keyStore.password*               | **zookeeper.ssl.quorum.keyStore.password**               |
| *ssl.keyStore.type*                          | **zookeeper.ssl.keyStore.type**                          |
| *ssl.quorum.keyStore.type*                   | **zookeeper.ssl.quorum.keyStore.type**                   |
| *ssl.trustStore.location*                    | **zookeeper.ssl.trustStore.location**                    |
| *ssl.trustStore.password*                    | **zookeeper.ssl.trustStore.password**                    |
| *ssl.quorum.trustStore.location*             | **zookeeper.ssl.quorum.trustStore.location**             |
| *ssl.quorum.trustStore.password*             | **zookeeper.ssl.quorum.trustStore.password**             |
| *ssl.trustStore.type*                        | **zookeeper.ssl.trustStore.type**                        |
| *ssl.quorum.trustStore.type*                 | **zookeeper.ssl.quorum.trustStore.type**                 |
| *ssl.protocol*                               | **zookeeper.ssl.protocol**                               |
| *ssl.quorum.protocol*                        | **zookeeper.ssl.quorum.protocol**                        |
| *ssl.enabledProtocols*                       | **zookeeper.ssl.enabledProtocols**                       |
| *ssl.quorum.enabledProtocols*                | **zookeeper.ssl.quorum.enabledProtocols**                |
| *ssl.ciphersuites*                           | **zookeeper.ssl.ciphersuites**                           |
| *ssl.quorum.ciphersuites*                    | **zookeeper.ssl.quorum.ciphersuites**                    |
| *ssl.context.supplier.class*                 | **zookeeper.ssl.context.supplier.class**                 |
| *ssl.quorum.context.supplier.class*          | **zookeeper.ssl.quorum.context.supplier.class**          |
| *ssl.hostnameVerification*                   | **zookeeper.ssl.hostnameVerification**                   |
| *ssl.quorum.hostnameVerification*            | **zookeeper.ssl.quorum.hostnameVerification**            |
| *ssl.crl*                                    | **zookeeper.ssl.crl**                                    |
| *ssl.quorum.crl*                             | **zookeeper.ssl.quorum.crl**                             |
| *ssl.ocsp*                                   | **zookeeper.ssl.ocsp**                                   |
| *ssl.quorum.ocsp*                            | **zookeeper.ssl.quorum.ocsp**                            |
| *ssl.clientAuth*                             | **zookeeper.ssl.clientAuth**                             |
| *ssl.quorum.clientAuth*                      | **zookeeper.ssl.quorum.clientAuth**                      |
| *ssl.handshakeDetectionTimeoutMillis*        | **zookeeper.ssl.handshakeDetectionTimeoutMillis**        |
| *ssl.quorum.handshakeDetectionTimeoutMillis* | **zookeeper.ssl.quorum.handshakeDetectionTimeoutMillis** |
| *client.portUnification*                     | **zookeeper.client.portUnification**                     |
| *authProvider*                               | **zookeeper.authProvider**                               |
| *kerberos.removeHostFromPrincipal*           | **zookeeper.kerberos.removeHostFromPrincipal**           |
| *kerberos.removeRealmFromPrincipal*          | **zookeeper.kerberos.removeRealmFromPrincipal**          |
| *multiAddress.enabled*                       | **zookeeper.multiAddress.enabled**                       |
| *multiAddress.reachabilityCheckTimeoutMs*    | **zookeeper.multiAddress.reachabilityCheckTimeoutMs**    |

##### 实验参数

| 参数名称                | 介绍                                | java参数                 |
| ----------------------- | ----------------------------------- | ------------------------ |
| *Read Only Mode Server* | 是否开启只读模式的服务器，默认false | **readonlymode.enabled** |

##### unsafe参数配置

| 参数名称                                      | 介绍                                                         | java参数                                                  |
| --------------------------------------------- | ------------------------------------------------------------ | --------------------------------------------------------- |
| *forceSync*                                   | 是否在完成更新之前同步事务日志                               | **zookeeper.forceSync**                                   |
| *jute.maxbuffer*                              | 只能通过java参数设置最大缓冲区大小                           | **jute.maxbuffer**                                        |
| *jute.maxbuffer.extrasize*                    | 在持久化事务日志之前是否请求额外信息                         | **zookeeper.jute.maxbuffer.extrasize**                    |
| *skipACL*                                     | 是否跳过ACL检查                                              | **zookeeper.skipACL**                                     |
| *quorumListenOnAllIPs*                        | 是否zk会监视同级ip地址<br />这个会影响处理ZAB协议的过程和快速leader选举过程，默认false |                                                           |
| *multiAddress.<br />reachabilityCheckEnabled* | 是否检查可到达性                                             | **zookeeper.multiAddress.<br />reachabilityCheckEnabled** |



##### 取消数据目录的自动创建

这个功能是zk 3.5中新增的功能，默认zk服务器时会自动创建数据目录的。这个会非常的不方便,且有时候会非常的危险.考虑到运行服务器期间配置发生了变化,`dataDir`参数就会发生了变化.

zk重启的时候,会创建不存在的目录,且启动空的znode命名空间.这种情况下可能会导致***脑裂***的情况(数据既处于新的数据目录中也位于旧的数据存储中).所以自动关闭创建时一件很好地功能.

总体来说,在生成环境下应当去进行配置,但是不幸的是虽然默认的行为不能现在改版,且因此需要根据情况处理.

当运行`zkServer.sh`自动创建会被关闭.通过设置环境变量`ZOO_DATADIR_AUTOCREATE_DISABLE`为1.有类文件直接启动zk的时候需要设置`zookeeper.datadir.autocreate=false`,可以通过java命令行参数控制`-Dzookeeper.datadir.autocreate=false`

当关闭自动创建的功能的时候,zk服务器决定哪些目录不应当存在,且会生成错误且拒绝启动.

新脚本`zkServer-initialize.sh`提供这个功能,如果自动创建关闭,首先安装zk就很重要,然后创建数据目录,然后是启动zk服务器.

否则像前面提到的一样会拒绝启动.运行这个脚本会创建需要的目录,且可能创建`myid`文件(也可以是命令行参数).这个脚本在自动创建没有使用的时候也可以使用,

注意到这个脚本仅仅保证了数据目录存在,不会创建配置文件,但是需要执行的配置文件.

##### 开启数据块存在性校验

zk 3.6.0新增特性: zk服务器默认行为,用于没有数据树的时候启动.将zxid设置为零,且作为参与投票的成员加入`quorum`中.如果服务器宕机的时候数据目录移除了,这时候就危险了.因为服务器帮助选择了的leader,这个leader丢失了事务.

允许数据块存在性校验会改变没有数据时候的启动行为.服务器加入集群的时候是作为一个非投票成员直到可以与leader同步为止才可以投票.且获取集群数据的最新版本数据.

为了表明空数据树排除在外,用户需要初始化一个与`myid`一样的初始化文件.这个文件会启动期间被服务器发现和删除.初始化验证在运行zk服务器的时候开启,通过设置**zookeeper.db.autocreate=false**,也可以通过java参数传入**-Dzookeeper.db.autocreate=false**.运行`zkServer-initialize.sh`创建需要的初始化文件。

##### 性能协调配置

zk 3.5.0新功能，多个子系统用于提示读性能。这个包括多线程NIO子系统和请求处理pipeline.NIO是默认客户端/服务器沟通子系统.线程模型使用了一个接收器,1-N个选择器线程和0-M个IO socket线程.在请求处理的pipeline中,系统可以处理多个读取请求,且能够保证数据一致性的要求.

默认配置旨在最大化zk读取数量.子系统需要承受峰值读取量.

| 参数名称                                             | java参数                                               | 介绍                                          |
| ---------------------------------------------------- | ------------------------------------------------------ | --------------------------------------------- |
| *zookeeper.nio.numSelectorThreads*                   | **zookeeper.nio.numSelectorThreads**                   | zk NIO选择器线程数量,最小为1                  |
| *zookeeper.nio.numWorkerThreads*                     | **zookeeper.nio.numWorkerThreads**                     | NIO worker线程数量,最小0个,默认为cpu核心数2倍 |
| *zookeeper.commitProcessor<br />                     | **zookeeper.commitProcessor<br />.numWorkerThreads**   | 用于提交处理的线程数量，最小为0               |
| *zookeeper.commitProcessor<br />.maxReadBatchSize*   | **zookeeper.<br />commitProcessor.maxReadBatchSize**   | 单次最大读取数量                              |
| *zookeeper.commitProcessor<br />.maxCommitBatchSize* | **zookeeper.<br />commitProcessor.maxCommitBatchSize** | 进行读取之前最大提交数量                      |
| *znode.container.checkIntervalMs*                    |                                                        | 候选容器/TTL节点检查周期                      |
| *znode.container.maxPerMinute*                       |                                                        | 容器/TTL节点每分钟删除最大数量                |
| *znode.container.<br />maxNeverUsedIntervalMs*       |                                                        | 空容器时间间隔                                |

##### 调试可观察的配置

zk 3.6.0新功能: 下述配置可以使得zk方便调试

| 参数名称                              | 介绍                                                   |
| ------------------------------------- | ------------------------------------------------------ |
| *zookeeper.messageTracker.BufferSize* | 控制消息定位器中的最大消息数量。需要是正整数，默认10。 |
| *zookeeper.messageTracker.Enabled*    | 设置为true，则运行消息定位器追踪消息。默认false        |

##### 管理服务器配置

| 参数名称                | java参数                            | 介绍                           |
| ----------------------- | ----------------------------------- | ------------------------------ |
| *admin.portUnification* | **zookeeper.admin.portUnification** | 允许管理端口接受HTTP/HTTPS请求 |
| *admin.enableServer*    | **zookeeper.admin.enableServer**    | 是否启动管理器服务器           |
| *admin.serverAddress*   | **zookeeper.admin.serverAddress**   | 管理器服务器地址,默认0.0.0.0   |
| *admin.serverPort*      | **zookeeper.admin.serverPort**      | 管理器服务器端口,默认8080      |
| *admin.idleTimeout*     | **zookeeper.admin.idleTimeout**     | 最大空载时间,默认30000ms       |
| *admin.commandURL*      | **zookeeper.admin.commandURL**      | 控制URL,默认为`/commands`      |



#### 度量值参数

| 参数                            | 介绍                                                         |
| ------------------------------- | ------------------------------------------------------------ |
| *metricsProvider.className*     | 设置这个为`org.apache.zookeeper.<br />metrics.prometheus.PrometheusMetricsProvider`开启Promethous |
| *metricsProvider.httpPort*      | Promethous会启动一个jetty服务器,默认端口7000                 |
| *metricsProvider.exportJvmInfo* | 设置为true则Promethous会用于JVM参数的度量,默认true           |

#### 使用Netty进行交互

在zk 3.5之后，zk服务器可以使用netty，而不是使用默认的nio。可以通过设置参数`zookeeper.serverCnxnFactory`和`zookeeper.clientCnxnSocket`

##### Quorum TLS

zk 3.5.5新增配置，基于netty框架，zk集群可以使用TLS加密进行通信。这个部分描述如何配置加密。

注意Quorum TLS可以保证leader选举和quorum沟通协议的安全，需要创建SSL的key存储器去存储本地证书。每个zk实例都需要创建一个key存储。

在这个例子中使用自定义的证书，存储私钥到`keystore.jks`中.适合于测试使用,生成环境下需要官方的密钥.

> 注意列表`-alias`和可标识的名称`-dname`必须匹配机器的主机名称.否则主机验证不会生效.

```shell
keytool -genkeypair -alias $(hostname -f) -keyalg RSA -keysize 2048 -dname "cn=$(hostname -f)" -keypass password -keystore keystore.jks -storepass password
```

2. 从key存储中抓取公钥

```shell
keytool -exportcert -alias $(hostname -f) -keystore keystore.jks -file $(hostname -f).cer -rfc
```

3. 创建SSL信任的zk实例JKS认证

```shell
keytool -importcert -alias [host1..3] -file [host1..3].cer -keystore truststore.jks -storepass password
```

4. 使用`NettyServerCnxnFactory`作为serverCnxnFactory,因为SSL不支持NIO.添加下述配置到`zoo.cfg`中

```
sslQuorum=true
serverCnxnFactory=org.apache.zookeeper.server.NettyServerCnxnFactory
ssl.quorum.keyStore.location=/path/to/keystore.jks
ssl.quorum.keyStore.password=password
ssl.quorum.trustStore.location=/path/to/truststore.jks
ssl.quorum.trustStore.password=password
```

5. 验证TLS上的日志信息

```
INFO  [main:QuorumPeer@1789] - Using TLS encrypted quorum communication
INFO  [main:QuorumPeer@1797] - Port unification disabled
...
INFO  [QuorumPeerListener:QuorumCnxManager$Listener@877] - Creating TLS-only quorum server socket
```

##### 非TLS集群的降级

zk 3.5.5的新功能,下述步骤需要在非TLS集群中进行,用于对集群进行降级

1. 创建必要的key存储和信任存储,用在zk的参与者范围内.
2. 添加下述配置,并重启第一个节点

```
sslQuorum=false
portUnification=true
serverCnxnFactory=org.apache.zookeeper.server.NettyServerCnxnFactory
ssl.quorum.keyStore.location=/path/to/keystore.jks
ssl.quorum.keyStore.password=password
ssl.quorum.trustStore.location=/path/to/truststore.jks
ssl.quorum.trustStore.password=password
```

注意这时候TLS还没开枪,需要开启端口统一.

3. 在其他节点重复操作二,验证日志

```
INFO  [main:QuorumPeer@1791] - Using insecure (non-TLS) quorum communication
INFO  [main:QuorumPeer@1797] - Port unification enabled
...
INFO  [QuorumPeerListener:QuorumCnxManager$Listener@874] - Creating TLS-enabled quorum server socket
```

4. 开启每个节点上的Quorum TLS并且进行轮询重启

```
sslQuorum=true
portUnification=true
```

5. 一旦确定所有节点都运行在TLS下,可以关闭端口统一并进行下一轮的轮询重启

```
sslQuorum=true
portUnification=false
```



#### Zookeeper指令

##### 四字指令

| 指令名称 | 功能                                                         |
| -------- | ------------------------------------------------------------ |
| `conf`   | 打印服务配置的详细信息                                       |
| `cons`   | 列举当前服务器所有连接/会话信息                              |
| `crst`   | 重置连接/会话的统计值                                        |
| `dump`   | 列举重要会话和临时节点                                       |
| `envi`   | 打印服务器环境变量信息                                       |
| `ruok`   | 测试服务器是否处于没有错误的状态下，如果正在运行则回复`imok` |
| `srst`   | 重置服务器统计值                                             |
| `srvr`   | 列举服务器详细信息                                           |
| `stat`   | 列举服务器和连接的客户端简要信息                             |
| `wchs`   | 列举服务器的观察者简要信息                                   |
| `wchc`   | 列举服务器观察者详细信息                                     |
| `dirs`   | 显示快照的总大小和日志文件                                   |
| `wchp`   | 服务器观察者的详细信息                                       |
| `mntr`   | 输出监视集群健康信息的变量表                                 |
| `isro`   | 测试服务器是否处于只读模式中                                 |
| `hash`   | 返回zxid相关的最新历史                                       |
| `gtmk`   | 获取64位的追踪码                                             |
| `stmk`   | 获取追踪码                                                   |

###### 追踪码表


| Trace Mask Bit Values |                     |
|-----------------------|---------------------|
| 0b0000000000 | Unused, reserved for future use. |
| 0b0000000010 | Logs client requests, excluding ping requests. |
| 0b0000000100 | Unused, reserved for future use. |
| 0b0000001000 | Logs client ping requests. |
| 0b0000010000 | Logs packets received from the quorum peer that is the current leader, excluding ping requests. |
| 0b0000100000 | Logs addition, removal and validation of client sessions. |
| 0b0001000000 | Logs delivery of watch events to client sessions. |
| 0b0010000000 | Logs ping packets received from the quorum peer that is the current leader. |
| 0b0100000000 | Unused, reserved for future use. |
| 0b1000000000 | Unused, reserved for future use. |

##### 管理服务器

管理服务器是一个嵌入式的jetty服务器,提供了HTTP接口,用于接收四字指令.默认情况下,服务器启动在8080端口上,指令可以通过`/command/[command_name]`访问.指令响应返回一个JSON.

但是这里没有严格的四字限制.

默认情况下开启了管理器服务器,可以通过下述方法关闭:

+ ` zookeeper.admin.enableServer=false`
+ 从类路径中移除jetty

注意到TCP的四字接口在管理服务器中关闭了,可用的指令包括:

+ *connection_stat_reset/crst*

  重置所有客户端统计值

+ *configuration/conf/config*

  打印所有服务器的基本配置

+ *connections/cons*

  客户端给服务器的信息,注意依靠客户端连接的操作开销较大,返回链接

+ *hash*

  返回事务消费列表信息

+ *dirs*

  日志目录和快照目录的大小信息

* *dump* :

    会话和临时节点信息.

* *environment/env/envi* :
  
    所有环境变量信息

* *get_trace_mask/gtmk* :
  
    当前追踪码信息
    
* *initial_configuration/icfg* :
  
    打印配置文件信息

* *is_read_only/isro* :
  
    返回是否是只读

* *last_snapshot/lsnp* :
  
    返回zxid和时间戳信息,最新的快照时间,单位为秒
    
* *leader/lead* :
  
    返回是否是leader,leader的id以及ip地址
    
* *monitor/mntr* :
  
    返回本身的参数
    
* *observer_connection_stat_reset/orst* :
  
    重置所有观察者的统计信息

* *ruok* :
  
    nop操作，检查服务器是否处于运行状态。
    
* *set_trace_mask/stmk* :
  
    返回追踪码
    
* *server_stats/srvr* :
  
    服务器信息，返回多个属性，用于对服务状态进行简单描述

* *stats/stat* :
  
    服务器信息，同时会返回链接信息
    
* *stat_reset/srst* :
  
    重置服务器统计信息，是*status*的子集
    
* *observers/obsr* :
  
    观察者连接到服务器的信息
    
* *system_properties/sysp* :
  
    系统参数

* *voting_view* :
  
    提供当前系统中的投票信息，以map形式返回

* *watches/wchc* :
  
    以会话形式聚合的观察者信息，以map形式返回
    
* *watches_by_path/wchp* :
  
    以路径聚合的观察者信息，以map形式返回
    
* *watch_summary/wchs* :
  
    观察者详细信息

* *zabstate* :
  
    当前ZAB协议状态，可以为ELECTION, DISCOVERY, SYNCHRONIZATION, BROADCAST状态

#### 数据文件管理

zk将数据存储到数据目录中，且将事务日志存储到事务日志目录中。默认情况下，两个目录相同。服务器可以配置存储到不同的位置。

##### 数据目录

数据目录包含下属文件

+ **myid**： 包含单个整数，表示服务器编号
+ **initialize**： 表示缺少数据树，创建的时候需要清理数据树
+ **snapshot.<zxid>**： 持有数据树的模糊快照

每个zk服务器都有唯一的标识符,这个id有两个参数唯一标识.分别是`myid`文件和**配置文件**.`myid`文件表示服务器需要响应给定的数据目录.配置文件列举了服务器的连接信息.

当zk启动的时候,会读取`myid`的id信息,然后使用id从配置文件中查找需要监听的端口.

存储在数据目录中的快照文件某种意义上是模糊的快照.zk服务器可以获取快照,根据数据树进行更新.

快照文件名称的后缀是`zxid`,这个是zk的事务编号.是上次提交的事务快照.

因此,快照会包含快照更新过程中的数据树的更新.这个快照然后就不会响应已经存在数据树的改变了.

然后,zk可以从快照中恢复.通过重新进行实物日志,可以获取日志最后的状态.

##### 日志目录

日志目录包含zk的事务日志.在更新之前,zk保证日志的更新会写出到稳定的存储中.

新的日志文件在达到日志文件写出需要的容量的时候开始写出.这个容量使用了快照参数进行计算.日志文件的后缀是第一个写入到日志的zxid.

##### 文件管理

快照和日志文件不会因为单机模式或者是不同zk服务器配置间发生变化.因此,需要将这些文件从服务器上拉取下来.放在开发环境下进行调试.

如果使用旧的日志和快照文件,可以查看zk服务器的过去状态,甚至可以对其进行恢复.

zk虽然创建了快照和日志文件,但是不会删除.数据和日志文件的保存策略有zk外部实现.zk仅仅需要最新完整版快照,所有日志文件需要跟踪它.

