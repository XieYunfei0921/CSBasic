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

#### 

##### 取消数据目录的自动创建

这个功能是zk 3.5中新增的功能，默认zk服务器时会自动创建数据目录的。这个会非常的不方便,且有时候会非常的危险.考虑到运行服务器期间配置发生了变化,`dataDir`参数就会发生了变化.

 When the ZooKeeper server is
restarted it will create this non-existent directory and begin
serving - with an empty znode namespace. This scenario can
result in an effective "split brain" situation (i.e. data in
both the new invalid directory and the original valid data
store). As such is would be good to have an option to turn off
this autocreate behavior. In general for production
environments this should be done, unfortunately however the
default legacy behavior cannot be changed at this point and
therefore this must be done on a case by case basis. This is
left to users and to packagers of ZooKeeper distributions.

When running **zkServer.sh** autocreate can be disabled
by setting the environment variable **ZOO_DATADIR_AUTOCREATE_DISABLE** to 1.
When running ZooKeeper servers directly from class files this
can be accomplished by setting **zookeeper.datadir.autocreate=false** on
the java command line, i.e. **-Dzookeeper.datadir.autocreate=false**

When this feature is disabled, and the ZooKeeper server
determines that the required directories do not exist it will
generate an error and refuse to start.

A new script **zkServer-initialize.sh** is provided to
support this new feature. If autocreate is disabled it is
necessary for the user to first install ZooKeeper, then create
the data directory (and potentially txnlog directory), and
then start the server. Otherwise as mentioned in the previous
paragraph the server will not start. Running **zkServer-initialize.sh** will create the
required directories, and optionally setup the myid file
(optional command line parameter). This script can be used
even if the autocreate feature itself is not used, and will
likely be of use to users as this (setup, including creation
of the myid file) has been an issue for users in the past.
Note that this script ensures the data directories exist only,
it does not create a config file, but rather requires a config
file to be available in order to execute.

<a name="sc_db_existence_validation"></a>

#### Enabling db existence validation

**New in 3.6.0:** The default
behavior of a ZooKeeper server on startup when no data tree
is found is to set zxid to zero and join the quorum as a
voting member. This can be dangerous if some event (e.g. a
rogue 'rm -rf') has removed the data directory while the
server was down since this server may help elect a leader
that is missing transactions. Enabling db existence validation
will change the behavior on startup when no data tree is
found: the server joins the ensemble as a non-voting participant
until it is able to sync with the leader and acquire an up-to-date
version of the ensemble data. To indicate an empty data tree is
expected (ensemble creation), the user should place a file
'initialize' in the same directory as 'myid'. This file will
be detected and deleted by the server on startup.

Initialization validation can be enabled when running
ZooKeeper servers directly from class files by setting
**zookeeper.db.autocreate=false**
on the java command line, i.e.
**-Dzookeeper.db.autocreate=false**.
Running **zkServer-initialize.sh**
will create the required initialization file.

<a name="sc_performance_options"></a>

#### Performance Tuning Options

**New in 3.5.0:** Several subsystems have been reworked
to improve read throughput. This includes multi-threading of the NIO communication subsystem and
request processing pipeline (Commit Processor). NIO is the default client/server communication
subsystem. Its threading model comprises 1 acceptor thread, 1-N selector threads and 0-M
socket I/O worker threads. In the request processing pipeline the system can be configured
to process multiple read request at once while maintaining the same consistency guarantee
(same-session read-after-write). The Commit Processor threading model comprises 1 main
thread and 0-N worker threads.

The default values are aimed at maximizing read throughput on a dedicated ZooKeeper machine.
Both subsystems need to have sufficient amount of threads to achieve peak read throughput.

* *zookeeper.nio.numSelectorThreads* :
    (Java system property only: **zookeeper.nio.numSelectorThreads**)
    **New in 3.5.0:**
    Number of NIO selector threads. At least 1 selector thread required.
    It is recommended to use more than one selector for large numbers
    of client connections. The default value is sqrt( number of cpu cores / 2 ).

* *zookeeper.nio.numWorkerThreads* :
    (Java system property only: **zookeeper.nio.numWorkerThreads**)
    **New in 3.5.0:**
    Number of NIO worker threads. If configured with 0 worker threads, the selector threads
    do the socket I/O directly. The default value is 2 times the number of cpu cores.

* *zookeeper.commitProcessor.numWorkerThreads* :
    (Java system property only: **zookeeper.commitProcessor.numWorkerThreads**)
    **New in 3.5.0:**
    Number of Commit Processor worker threads. If configured with 0 worker threads, the main thread
    will process the request directly. The default value is the number of cpu cores.

* *zookeeper.commitProcessor.maxReadBatchSize* :
    (Java system property only: **zookeeper.commitProcessor.maxReadBatchSize**)
    Max number of reads to process from queuedRequests before switching to processing commits.
    If the value < 0 (default), we switch whenever we have a local write, and pending commits.
    A high read batch size will delay commit processing, causing stale data to be served.
    If reads are known to arrive in fixed size batches then matching that batch size with
    the value of this property can smooth queue performance. Since reads are handled in parallel,
    one recommendation is to set this property to match *zookeeper.commitProcessor.numWorkerThread*
    (default is the number of cpu cores) or lower.

* *zookeeper.commitProcessor.maxCommitBatchSize* :
    (Java system property only: **zookeeper.commitProcessor.maxCommitBatchSize**)
    Max number of commits to process before processing reads. We will try to process as many
    remote/local commits as we can till we reach this count. A high commit batch size will delay
    reads while processing more commits. A low commit batch size will favor reads.
    It is recommended to only set this property when an ensemble is serving a workload with a high
    commit rate. If writes are known to arrive in a set number of batches then matching that
    batch size with the value of this property can smooth queue performance. A generic
    approach would be to set this value to equal the ensemble size so that with the processing
    of each batch the current server will probabilistically handle a write related to one of
    its direct clients.
    Default is "1". Negative and zero values are not supported.

* *znode.container.checkIntervalMs* :
    (Java system property only)
    **New in 3.6.0:** The
    time interval in milliseconds for each check of candidate container
    and ttl nodes. Default is "60000".

* *znode.container.maxPerMinute* :
    (Java system property only)
    **New in 3.6.0:** The
    maximum number of container and ttl nodes that can be deleted per
    minute. This prevents herding during container deletion.
    Default is "10000".

* *znode.container.maxNeverUsedIntervalMs* :
    (Java system property only)
    **New in 3.6.0:** The
    maximum interval in milliseconds that a container that has never had
    any children is retained. Should be long enough for your client to
    create the container, do any needed work and then create children.
    Default is "0" which is used to indicate that containers
    that have never had any children are never deleted.

<a name="sc_debug_observability_config"></a>

#### Debug Observability Configurations

**New in 3.6.0:** The following options are introduced to make zookeeper easier to debug.

* *zookeeper.messageTracker.BufferSize* :
    (Java system property only)
    Controls the maximum number of messages stored in **MessageTracker**. Value should be positive
    integers. The default value is 10. **MessageTracker** is introduced in **3.6.0** to record the
    last set of messages between a server (follower or observer) and a leader, when a server
    disconnects with leader. These set of messages will then be dumped to zookeeper's log file,
    and will help reconstruct the state of the servers at the time of the disconnection and
    will be useful for debugging purpose.

* *zookeeper.messageTracker.Enabled* :
    (Java system property only)
    When set to "true", will enable **MessageTracker** to track and record messages. Default value
    is "false".

<a name="sc_adminserver_config"></a>

#### AdminServer configuration

**New in 3.6.0:** The following
options are used to configure the [AdminServer](#sc_adminserver).

* *admin.portUnification* :
    (Java system property: **zookeeper.admin.portUnification**)
    Enable the admin port to accept both HTTP and HTTPS traffic.
    Defaults to disabled.

**New in 3.5.0:** The following
options are used to configure the [AdminServer](#sc_adminserver).

* *admin.enableServer* :
    (Java system property: **zookeeper.admin.enableServer**)
    Set to "false" to disable the AdminServer.  By default the
    AdminServer is enabled.

* *admin.serverAddress* :
    (Java system property: **zookeeper.admin.serverAddress**)
    The address the embedded Jetty server listens on. Defaults to 0.0.0.0.

* *admin.serverPort* :
    (Java system property: **zookeeper.admin.serverPort**)
    The port the embedded Jetty server listens on.  Defaults to 8080.

* *admin.idleTimeout* :
    (Java system property: **zookeeper.admin.idleTimeout**)
    Set the maximum idle time in milliseconds that a connection can wait
    before sending or receiving data. Defaults to 30000 ms.

* *admin.commandURL* :
    (Java system property: **zookeeper.admin.commandURL**)
    The URL for listing and issuing commands relative to the
    root URL.  Defaults to "/commands".

### Metrics Providers

**New in 3.6.0:** The following options are used to configure metrics.

 By default ZooKeeper server exposes useful metrics using the [AdminServer](#sc_adminserver).
 and [Four Letter Words](#sc_4lw) interface.

 Since 3.6.0 you can configure a different Metrics Provider, that exports metrics
 to your favourite system.

 Since 3.6.0 ZooKeeper binary package bundles an integration with [Prometheus.io](https://prometheus.io)

* *metricsProvider.className* :
    Set to "org.apache.zookeeper.metrics.prometheus.PrometheusMetricsProvider" to
    enable Prometheus.io exporter.

* *metricsProvider.httpPort* :
    Prometheus.io exporter will start a Jetty server and bind to this port, it default to 7000.
    Prometheus end point will be http://hostname:httPort/metrics.

* *metricsProvider.exportJvmInfo* :
    If this property is set to **true** Prometheus.io will export useful metrics about the JVM.
    The default is true.

<a name="Communication+using+the+Netty+framework"></a>

### Communication using the Netty framework

[Netty](http://netty.io)
is an NIO based client/server communication framework, it
simplifies (over NIO being used directly) many of the
complexities of network level communication for java
applications. Additionally the Netty framework has built
in support for encryption (SSL) and authentication
(certificates). These are optional features and can be
turned on or off individually.

In versions 3.5+, a ZooKeeper server can use Netty
instead of NIO (default option) by setting the environment
variable **zookeeper.serverCnxnFactory**
to **org.apache.zookeeper.server.NettyServerCnxnFactory**;
for the client, set **zookeeper.clientCnxnSocket**
to **org.apache.zookeeper.ClientCnxnSocketNetty**.

<a name="Quorum+TLS"></a>

#### Quorum TLS

*New in 3.5.5*

Based on the Netty Framework ZooKeeper ensembles can be set up
to use TLS encryption in their communication channels. This section
describes how to set up encryption on the quorum communication.

Please note that Quorum TLS encapsulates securing both leader election
and quorum communication protocols.

1. Create SSL keystore JKS to store local credentials

One keystore should be created for each ZK instance.

In this example we generate a self-signed certificate and store it
together with the private key in `keystore.jks`. This is suitable for
testing purposes, but you probably need an official certificate to sign
your keys in a production environment.

Please note that the alias (`-alias`) and the distinguished name (`-dname`)
must match the hostname of the machine that is associated with, otherwise
hostname verification won't work.

```
keytool -genkeypair -alias $(hostname -f) -keyalg RSA -keysize 2048 -dname "cn=$(hostname -f)" -keypass password -keystore keystore.jks -storepass password
```

2. Extract the signed public key (certificate) from keystore

*This step might only necessary for self-signed certificates.*

```
keytool -exportcert -alias $(hostname -f) -keystore keystore.jks -file $(hostname -f).cer -rfc
```

3. Create SSL truststore JKS containing certificates of all ZooKeeper instances

The same truststore (storing all accepted certs) should be shared on
participants of the ensemble. You need to use different aliases to store
multiple certificates in the same truststore. Name of the aliases doesn't matter.

```
keytool -importcert -alias [host1..3] -file [host1..3].cer -keystore truststore.jks -storepass password
```

4. You need to use `NettyServerCnxnFactory` as serverCnxnFactory, because SSL is not supported by NIO.
Add the following configuration settings to your `zoo.cfg` config file:

```
sslQuorum=true
serverCnxnFactory=org.apache.zookeeper.server.NettyServerCnxnFactory
ssl.quorum.keyStore.location=/path/to/keystore.jks
ssl.quorum.keyStore.password=password
ssl.quorum.trustStore.location=/path/to/truststore.jks
ssl.quorum.trustStore.password=password
```

5. Verify in the logs that your ensemble is running on TLS:

```
INFO  [main:QuorumPeer@1789] - Using TLS encrypted quorum communication
INFO  [main:QuorumPeer@1797] - Port unification disabled
...
INFO  [QuorumPeerListener:QuorumCnxManager$Listener@877] - Creating TLS-only quorum server socket
```

<a name="Upgrading+existing+nonTLS+cluster"></a>

#### Upgrading existing non-TLS cluster with no downtime

*New in 3.5.5*

Here are the steps needed to upgrade an already running ZooKeeper ensemble
to TLS without downtime by taking advantage of port unification functionality.

1. Create the necessary keystores and truststores for all ZK participants as described in the previous section

2. Add the following config settings and restart the first node

```
sslQuorum=false
portUnification=true
serverCnxnFactory=org.apache.zookeeper.server.NettyServerCnxnFactory
ssl.quorum.keyStore.location=/path/to/keystore.jks
ssl.quorum.keyStore.password=password
ssl.quorum.trustStore.location=/path/to/truststore.jks
ssl.quorum.trustStore.password=password
```

Note that TLS is not yet enabled, but we turn on port unification.

3. Repeat step #2 on the remaining nodes. Verify that you see the following entries in the logs:

```
INFO  [main:QuorumPeer@1791] - Using insecure (non-TLS) quorum communication
INFO  [main:QuorumPeer@1797] - Port unification enabled
...
INFO  [QuorumPeerListener:QuorumCnxManager$Listener@874] - Creating TLS-enabled quorum server socket
```

You should also double check after each node restart that the quorum become healthy again.

4. Enable Quorum TLS on each node and do rolling restart:

```
sslQuorum=true
portUnification=true
```

5. Once you verified that your entire ensemble is running on TLS, you could disable port unification
and do another rolling restart

```
sslQuorum=true
portUnification=false
```


<a name="sc_zkCommands"></a>

### ZooKeeper Commands

<a name="sc_4lw"></a>

#### The Four Letter Words

ZooKeeper responds to a small set of commands. Each command is
composed of four letters. You issue the commands to ZooKeeper via telnet
or nc, at the client port.

Three of the more interesting commands: "stat" gives some
general information about the server and connected clients,
while "srvr" and "cons" give extended details on server and
connections respectively.

**New in 3.5.3:**
Four Letter Words need to be explicitly white listed before using.
Please refer **4lw.commands.whitelist**
described in [cluster configuration section](#sc_clusterOptions) for details.
Moving forward, Four Letter Words will be deprecated, please use
[AdminServer](#sc_adminserver) instead.

* *conf* :
    **New in 3.3.0:** Print
    details about serving configuration.

* *cons* :
    **New in 3.3.0:** List
    full connection/session details for all clients connected
    to this server. Includes information on numbers of packets
    received/sent, session id, operation latencies, last
    operation performed, etc...

* *crst* :
    **New in 3.3.0:** Reset
    connection/session statistics for all connections.

* *dump* :
    Lists the outstanding sessions and ephemeral nodes.

* *envi* :
    Print details about serving environment

* *ruok* :
    Tests if server is running in a non-error state. The server
    will respond with imok if it is running. Otherwise it will not
    respond at all.
    A response of "imok" does not necessarily indicate that the
    server has joined the quorum, just that the server process is active
    and bound to the specified client port. Use "stat" for details on
    state wrt quorum and client connection information.

* *srst* :
    Reset server statistics.

* *srvr* :
    **New in 3.3.0:** Lists
    full details for the server.

* *stat* :
    Lists brief details for the server and connected
    clients.

* *wchs* :
    **New in 3.3.0:** Lists
    brief information on watches for the server.

* *wchc* :
    **New in 3.3.0:** Lists
    detailed information on watches for the server, by
    session.  This outputs a list of sessions(connections)
    with associated watches (paths). Note, depending on the
    number of watches this operation may be expensive (ie
    impact server performance), use it carefully.

* *dirs* :
    **New in 3.5.1:**
    Shows the total size of snapshot and log files in bytes

* *wchp* :
    **New in 3.3.0:** Lists
    detailed information on watches for the server, by path.
    This outputs a list of paths (znodes) with associated
    sessions. Note, depending on the number of watches this
    operation may be expensive (ie impact server performance),
    use it carefully.

* *mntr* :
    **New in 3.4.0:** Outputs a list
    of variables that could be used for monitoring the health of the cluster.


    $ echo mntr | nc localhost 2185
                  zk_version  3.4.0
                  zk_avg_latency  0.7561              - be account to four decimal places
                  zk_max_latency  0
                  zk_min_latency  0
                  zk_packets_received 70
                  zk_packets_sent 69
                  zk_outstanding_requests 0
                  zk_server_state leader
                  zk_znode_count   4
                  zk_watch_count  0
                  zk_ephemerals_count 0
                  zk_approximate_data_size    27
                  zk_followers    4                   - only exposed by the Leader
                  zk_synced_followers 4               - only exposed by the Leader
                  zk_pending_syncs    0               - only exposed by the Leader
                  zk_open_file_descriptor_count 23    - only available on Unix platforms
                  zk_max_file_descriptor_count 1024   - only available on Unix platforms


The output is compatible with java properties format and the content
may change over time (new keys added). Your scripts should expect changes.
ATTENTION: Some of the keys are platform specific and some of the keys are only exported by the Leader.
The output contains multiple lines with the following format:


    key \t value


* *isro* :
    **New in 3.4.0:** Tests if
    server is running in read-only mode.  The server will respond with
    "ro" if in read-only mode or "rw" if not in read-only mode.

* *hash* :
    **New in 3.6.0:**
    Return the latest history of the tree digest associated with zxid.

* *gtmk* :
    Gets the current trace mask as a 64-bit signed long value in
    decimal format.  See `stmk` for an explanation of
    the possible values.

* *stmk* :
    Sets the current trace mask.  The trace mask is 64 bits,
    where each bit enables or disables a specific category of trace
    logging on the server.  Log4J must be configured to enable
    `TRACE` level first in order to see trace logging
    messages.  The bits of the trace mask correspond to the following
    trace logging categories.

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

    All remaining bits in the 64-bit value are unused and
    reserved for future use.  Multiple trace logging categories are
    specified by calculating the bitwise OR of the documented values.
    The default trace mask is 0b0100110010.  Thus, by default, trace
    logging includes client requests, packets received from the
    leader and sessions.
    To set a different trace mask, send a request containing the
    `stmk` four-letter word followed by the trace
    mask represented as a 64-bit signed long value.  This example uses
    the Perl `pack` function to construct a trace
    mask that enables all trace logging categories described above and
    convert it to a 64-bit signed long value with big-endian byte
    order.  The result is appended to `stmk` and sent
    to the server using netcat.  The server responds with the new
    trace mask in decimal format.


    $ perl -e "print 'stmk', pack('q>', 0b0011111010)" | nc localhost 2181
    250

Here's an example of the **ruok**
command:


    $ echo ruok | nc 127.0.0.1 5111
        imok


<a name="sc_adminserver"></a>

#### The AdminServer

**New in 3.5.0:** The AdminServer is
an embedded Jetty server that provides an HTTP interface to the four
letter word commands.  By default, the server is started on port 8080,
and commands are issued by going to the URL "/commands/\[command name]",
e.g., http://localhost:8080/commands/stat.  The command response is
returned as JSON.  Unlike the original protocol, commands are not
restricted to four-letter names, and commands can have multiple names;
for instance, "stmk" can also be referred to as "set_trace_mask".  To
view a list of all available commands, point a browser to the URL
/commands (e.g., http://localhost:8080/commands).  See the [AdminServer configuration options](#sc_adminserver_config)
for how to change the port and URLs.

The AdminServer is enabled by default, but can be disabled by either:

* Setting the zookeeper.admin.enableServer system
  property to false.
* Removing Jetty from the classpath.  (This option is
  useful if you would like to override ZooKeeper's jetty
  dependency.)

Note that the TCP four letter word interface is still available if
the AdminServer is disabled.

Available commands include:

* *connection_stat_reset/crst*:
    Reset all client connection statistics.
    No new fields returned.

* *configuration/conf/config* :
    Print basic details about serving configuration, e.g.
    client port, absolute path to data directory.

* *connections/cons* :
    Information on client connections to server.
    Note, depending on the number of client connections this operation may be expensive
    (i.e. impact server performance).
    Returns "connections", a list of connection info objects.

* *hash*:
    Txn digests in the historical digest list.
    One is recorded every 128 transactions.
    Returns "digests", a list to transaction digest objects.

* *dirs* :
    Information on logfile directory and snapshot directory
    size in bytes.
    Returns "datadir_size" and "logdir_size".

* *dump* :
    Information on session expirations and ephemerals.
    Note, depending on the number of global sessions and ephemerals
    this operation may be expensive (i.e. impact server performance).
    Returns "expiry_time_to_session_ids" and "session_id_to_ephemeral_paths" as maps.

* *environment/env/envi* :
    All defined environment variables.
    Returns each as its own field.

* *get_trace_mask/gtmk* :
    The current trace mask. Read-only version of *set_trace_mask*.
    See the description of the four letter command *stmk* for
    more details.
    Returns "tracemask".

* *initial_configuration/icfg* :
    Print the text of the configuration file used to start the peer.
    Returns "initial_configuration".

* *is_read_only/isro* :
    A true/false if this server is in read-only mode.
    Returns "read_only".

* *last_snapshot/lsnp* :
    Information of the last snapshot that zookeeper server has finished saving to disk.
    If called during the initial time period between the server starting up
    and the server finishing saving its first snapshot, the command returns the
    information of the snapshot read when starting up the server.
    Returns "zxid" and "timestamp", the latter using a time unit of seconds.

* *leader/lead* :
    If the ensemble is configured in quorum mode then emits the current leader
    status of the peer and the current leader location.
    Returns "is_leader", "leader_id", and "leader_ip".

* *monitor/mntr* :
    Emits a wide variety of useful info for monitoring.
    Includes performance stats, information about internal queues, and
    summaries of the data tree (among other things).
    Returns each as its own field.

* *observer_connection_stat_reset/orst* :
    Reset all observer connection statistics. Companion command to *observers*.
    No new fields returned.

* *ruok* :
    No-op command, check if the server is running.
    A response does not necessarily indicate that the
    server has joined the quorum, just that the admin server
    is active and bound to the specified port.
    No new fields returned.

* *set_trace_mask/stmk* :
    Sets the trace mask (as such, it requires a parameter).
    Write version of *get_trace_mask*.
    See the description of the four letter command *stmk* for
    more details.
    Returns "tracemask".

* *server_stats/srvr* :
    Server information.
    Returns multiple fields giving a brief overview of server state.

* *stats/stat* :
    Same as *server_stats* but also returns the "connections" field (see *connections*
    for details).
    Note, depending on the number of client connections this operation may be expensive
    (i.e. impact server performance).

* *stat_reset/srst* :
    Resets server statistics. This is a subset of the information returned
    by *server_stats* and *stats*.
    No new fields returned.

* *observers/obsr* :
    Information on observer connections to server.
    Always available on a Leader, available on a Follower if its
    acting as a learner master.
    Returns "synced_observers" (int) and "observers" (list of per-observer properties).

* *system_properties/sysp* :
    All defined system properties.
    Returns each as its own field.

* *voting_view* :
    Provides the current voting members in the ensemble.
    Returns "current_config" as a map.

* *watches/wchc* :
    Watch information aggregated by session.
    Note, depending on the number of watches this operation may be expensive
    (i.e. impact server performance).
    Returns "session_id_to_watched_paths" as a map.

* *watches_by_path/wchp* :
    Watch information aggregated by path.
    Note, depending on the number of watches this operation may be expensive
    (i.e. impact server performance).
    Returns "path_to_session_ids" as a map.

* *watch_summary/wchs* :
    Summarized watch information.
    Returns "num_total_watches", "num_paths", and "num_connections".

* *zabstate* :
    The current phase of Zab protocol that peer is running and whether it is a
    voting member.
    Peers can be in one of these phases: ELECTION, DISCOVERY, SYNCHRONIZATION, BROADCAST.
    Returns fields "voting" and "zabstate".


<a name="sc_dataFileManagement"></a>

### Data File Management

ZooKeeper stores its data in a data directory and its transaction
log in a transaction log directory. By default these two directories are
the same. The server can (and should) be configured to store the
transaction log files in a separate directory than the data files.
Throughput increases and latency decreases when transaction logs reside
on a dedicated log devices.

<a name="The+Data+Directory"></a>

#### The Data Directory

This directory has two or three files in it:

* *myid* - contains a single integer in
  human readable ASCII text that represents the server id.
* *initialize* - presence indicates lack of
  data tree is expected. Cleaned up once data tree is created.
* *snapshot.<zxid>* - holds the fuzzy
  snapshot of a data tree.

Each ZooKeeper server has a unique id. This id is used in two
places: the *myid* file and the configuration file.
The *myid* file identifies the server that
corresponds to the given data directory. The configuration file lists
the contact information for each server identified by its server id.
When a ZooKeeper server instance starts, it reads its id from the
*myid* file and then, using that id, reads from the
configuration file, looking up the port on which it should
listen.

The *snapshot* files stored in the data
directory are fuzzy snapshots in the sense that during the time the
ZooKeeper server is taking the snapshot, updates are occurring to the
data tree. The suffix of the *snapshot* file names
is the _zxid_, the ZooKeeper transaction id, of the
last committed transaction at the start of the snapshot. Thus, the
snapshot includes a subset of the updates to the data tree that
occurred while the snapshot was in process. The snapshot, then, may
not correspond to any data tree that actually existed, and for this
reason we refer to it as a fuzzy snapshot. Still, ZooKeeper can
recover using this snapshot because it takes advantage of the
idempotent nature of its updates. By replaying the transaction log
against fuzzy snapshots ZooKeeper gets the state of the system at the
end of the log.

<a name="The+Log+Directory"></a>

#### The Log Directory

The Log Directory contains the ZooKeeper transaction logs.
Before any update takes place, ZooKeeper ensures that the transaction
that represents the update is written to non-volatile storage. A new
log file is started when the number of transactions written to the
current log file reaches a (variable) threshold. The threshold is
computed using the same parameter which influences the frequency of
snapshotting (see snapCount and snapSizeLimitInKb above). The log file's
suffix is the first zxid written to that log.

<a name="sc_filemanagement"></a>

#### File Management

The format of snapshot and log files does not change between
standalone ZooKeeper servers and different configurations of
replicated ZooKeeper servers. Therefore, you can pull these files from
a running replicated ZooKeeper server to a development machine with a
stand-alone ZooKeeper server for troubleshooting.

Using older log and snapshot files, you can look at the previous
state of ZooKeeper servers and even restore that state.

The ZooKeeper server creates snapshot and log files, but
never deletes them. The retention policy of the data and log
files is implemented outside of the ZooKeeper server. The
server itself only needs the latest complete fuzzy snapshot, all log
files following it, and the last log file preceding it.  The latter
requirement is necessary to include updates which happened after this
snapshot was started but went into the existing log file at that time.
This is possible because snapshotting and rolling over of logs
proceed somewhat independently in ZooKeeper. See the
[maintenance](#sc_maintenance) section in
this document for more details on setting a retention policy
and maintenance of ZooKeeper storage.

###### Note
>The data stored in these files is not encrypted. In the case of
storing sensitive data in ZooKeeper, necessary measures need to be
taken to prevent unauthorized access. Such measures are external to
ZooKeeper (e.g., control access to the files) and depend on the
individual settings in which it is being deployed.

<a name="Recovery+-+TxnLogToolkit"></a>

#### Recovery - TxnLogToolkit
More details can be found in [this](http://zookeeper.apache.org/doc/current/zookeeperTools.html#zkTxnLogToolkit)

<a name="sc_commonProblems"></a>

### Things to Avoid

Here are some common problems you can avoid by configuring
ZooKeeper correctly:

* *inconsistent lists of servers* :
    The list of ZooKeeper servers used by the clients must match
    the list of ZooKeeper servers that each ZooKeeper server has.
    Things work okay if the client list is a subset of the real list,
    but things will really act strange if clients have a list of
    ZooKeeper servers that are in different ZooKeeper clusters. Also,
    the server lists in each Zookeeper server configuration file
    should be consistent with one another.

* *incorrect placement of transaction log* :
    The most performance critical part of ZooKeeper is the
    transaction log. ZooKeeper syncs transactions to media before it
    returns a response. A dedicated transaction log device is key to
    consistent good performance. Putting the log on a busy device will
    adversely affect performance. If you only have one storage device,
    increase the snapCount so that snapshot files are generated less often;
    it does not eliminate the problem, but it makes more resources available
    for the transaction log.

* *incorrect Java heap size* :
    You should take special care to set your Java max heap size
    correctly. In particular, you should not create a situation in
    which ZooKeeper swaps to disk. The disk is death to ZooKeeper.
    Everything is ordered, so if processing one request swaps the
    disk, all other queued requests will probably do the same. the
    disk. DON'T SWAP.
    Be conservative in your estimates: if you have 4G of RAM, do
    not set the Java max heap size to 6G or even 4G. For example, it
    is more likely you would use a 3G heap for a 4G machine, as the
    operating system and the cache also need memory. The best and only
    recommend practice for estimating the heap size your system needs
    is to run load tests, and then make sure you are well below the
    usage limit that would cause the system to swap.

* *Publicly accessible deployment* :
    A ZooKeeper ensemble is expected to operate in a trusted computing environment.
    It is thus recommended to deploy ZooKeeper behind a firewall.

<a name="sc_bestPractices"></a>

### Best Practices

For best results, take note of the following list of good
Zookeeper practices:

For multi-tenant installations see the [section](zookeeperProgrammers.html#ch_zkSessions)
detailing ZooKeeper "chroot" support, this can be very useful
when deploying many applications/services interfacing to a
single ZooKeeper cluster.
