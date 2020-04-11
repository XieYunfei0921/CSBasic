### Zookeeper动态重配置

#### 配置格式的修改

##### 指定客户端端口

服务器客户端端口时服务器接收客户端连接请求的端口。zk 3.5.0开始使用`client`和`clientPortAddress`来配置属性的方式不再使用,相反这些属性作为服务端关键字属性,这样配置:

```shell
server.<positive id> = <address1>:<port1>:<port2>[:role];[<client port address>:]<client port>
```

客户端端口地址是一个可选项,如果没有指定,默认使用`0.0.0.0`.通常情况下,角色是可以选择的,可以是观察者.

例如:

```shell
server.5 = 125.23.63.23:1234:1235;1236
server.5 = 125.23.63.23:1234:1235:participant;1236
server.5 = 125.23.63.23:1234:1235:observer;1236
server.5 = 125.23.63.23:1234:1235;125.23.63.24:1236
server.5 = 125.23.63.23:1234:1235:participant;125.23.63.23:1236
```

##### 指定多个服务器地址

zk可以绑定多个服务器网络接口,zk,且当出现网络异常的时候可以切换接口.不同的地址使用分隔符`|`分割.
例如:

```shell
server.2=zoo2-net1:2888:3888|zoo2-net2:2889:3889;2188
server.2=zoo2-net1:2888:3888|zoo2-net2:2889:3889|zoo2-net3:2890:3890;2188
server.2=zoo2-net1:2888:3888|zoo2-net2:2889:3889;zoo2-net1:2188
server.2=zoo2-net1:2888:3888:observer|zoo2-net2:2889:3889:observer;2188
```

##### 单机运行标签

在zk 3.5.0之前,可以在单机或者分布式模式下运行zk.这两个模式使用的不是一个栈,且运行期间不能互相切换.,默认情况下**单机运行标签**为`true`.

使用这个默认配置的结果就是以单台服务器启动集群不会增长,使用多台服务器不允许缩小到2台以下.

如果需要运行在分布式栈中,需要设置如下参数:

    standaloneEnabled=false

设置可以启动zk集群,这个集群包含单台参与者,并且可以动态增长.类似可以从多台缩减服务器.

##### 重新启动标签

zk 3.5.0-3.5.3之间.没有关闭动态重新配置功能的方法.基于重新配置标签提供关闭重新配置功能.

当然这里需要考虑到安全问题,就是可能在zk集群中有恶意的修改的问题.

3.5.3版本时`reconfigEnabled`参数用于重新配置,且考虑到安全问题,如果没有授权默认情况下会配置失败.(除非`reconfigEnabled`=true)

可以在`zoo.cfg`中设置配置文件:

    reconfigEnabled=true

#### 动态配置文件

zk 3.5.0开始,开始辨别动态配置的文件,这些文件在运行期间可以改变,添加静态的参数,这个可以在服务器启动的时候读取,且在执行期间不会改变.现在,现在配置关键字时动态配置的重要部分:

分别是：服务器,组和权重

动态配置参数存储在不同的配置文件中，这个文件使用动态配置文件关键字进行静态配置

Dynamic configuration parameters are stored in a separate file on
the server (which we call the dynamic configuration file). This file is
linked from the static config file using the new
_dynamicConfigFile_ keyword.

##### 示例

###### zoo_replicated1.cfg


```shell
tickTime=2000
dataDir=/zookeeper/data/zookeeper1
initLimit=5
syncLimit=2
dynamicConfigFile=/zookeeper/conf/zoo_replicated1.cfg.dynamic
```


###### zoo_replicated1.cfg.dynamic


```shell
server.1=125.23.63.23:2780:2783:participant;2791
server.2=125.23.63.24:2781:2784:participant;2792
server.3=125.23.63.25:2782:2785:participant;2793
```

当集群配置改变的时候,静态配置参数保持不变.动态参数会被zk加载,并重写动态配置文件到所有服务器上.因此,不同服务器上的动态配置文件经常是标识性的.一旦创建,动态配置文件不能够手动改变.改变需要通过新的重写配置指令中指定.

注意改变配置和离线集群信息会导致zk 日志配置信息的不一致.

##### 示例2

###### zoo_replicated1.cfg


    tickTime=2000
    dataDir=/zookeeper/data/zookeeper1
    initLimit=5
    syncLimit=2
    clientPort=

每台服务器的配置文件会被自动的分成动态的和静态的文件,如果在这种模式下没有准备好.上述的配置文件会被自动转换为两个文件.

注意客户端端口和客户端端口地址会被自动移除.

##### 兼容性

下述配置是可取的,但是不推荐使用

###### zoo_replicated1.cfg

    tickTime=2000
    dataDir=/zookeeper/data/zookeeper1
    initLimit=5
    syncLimit=2
    clientPort=2791
    server.1=125.23.63.23:2780:2783:participant
    server.2=125.23.63.24:2781:2784:participant
    server.3=125.23.63.25:2782:2785:participant

启动期间,创建动态配置文件,且包含配置的动态部分.这种情形下对客户端端口的配置会保存在静态配置文件中.



### zk集群的动态重配置

zk的java和c的API都支持重新配置.且指令都是同步变量和异步变量相结合的.

#### API

java和c的客户端包含下属两种类型的API:

* 重新注册的API:
  
* 获取配置文件的API:

#### 安全配置
zk支持可变配置功能,运行用于选择用户自己的安全需求.使用离散用于去决定何时的安全度量方式.

+ 控制权限

  动态配置存储在特使的znode上,为`/zookeeper/config`节点.这个节点是只读的,超级用于可以获取写权限.

  默认情况下只有超级用于可以对其进行完全控制.其他用户可以通过超级用户设置权限对其进行操作.

* 授权:

    用户授权是用于获取控制权限的,原理是基于zk可插入式授权schema

* 关闭ACL检查:
    
    zk支持跳过ACL的配置,如果设置为true,未授权的用户可以设置重新配置的API

#### 检索当前动态配置

动态配置存储咋znode`/zookeeper/config`中.新的配置客户端指令可以读取这个znode.与普通节点读取一样,可以检索最新的提交值.

```shell
[zk: 127.0.0.1:2791(CONNECTED) 3] config
server.1=localhost:2780:2783:participant;localhost:2791
server.2=localhost:2781:2784:participant;localhost:2792
server.3=localhost:2782:2785:participant;localhost:2793
```

注意输出的最后一行,这个是配置的版本,版本等于重新配置的zxid.当配置写入到动态配置文件中的时候,版本会自动的变成文件名称的一部分.且使用路径去更新静态配置文件.

启动期间,版本由文件名称获取,版本不可以由用户直接改变.这个是系统获取配置更新的参数.修改会导致数据的丢失或者数据不一致.

与`get`指令类似,`config`指令接受`-w`标签,用于设置znode的观察状态.使用`-s`设置znode状态的显示.使用`-c`输出版本和客户端连接信息.

例如:

    [zk: 127.0.0.1:2791(CONNECTED) 17] config -c
    400000003 localhost:2791,localhost:2793,localhost:2792

注意到如果直接使用API的话,这里调用`getConfig`

读取指令返回follower的配置信息给连接的客户端.可能是过期的,可以使用`sync`强行保证:

    zk.sync(ZooDefs.CONFIG_NODE, void_callback, context);
    zk.getConfig(watcher, callback, context);

#### 修改当前动态配置

##### 增量模式

增量模式允许添加和移除当前配置的服务器、允许多项改变：

    > reconfig -remove 3 -add
    server.5=125.23.63.23:1234:1235;1236

添加和删除操作获取逗号分割的参数

    > reconfig -remove 3,4 -add
    server.5=localhost:2111:2112;2113,6=localhost:2114:2115:observer;2116

The format of the server statement is exactly the same as
described in the section [Specifying the client port](#sc_reconfig_clientport) and
includes the client port. Notice that here instead of "server.5=" you
can just say "5=". In the example above, if server 5 is already in the
system, but has different ports or is not an observer, it is updated
and once the configuration commits becomes an observer and starts
using these new ports. This is an easy way to turn participants into
observers and vice versa or change any of their ports, without
rebooting the server.

ZooKeeper supports two types of Quorum Systems – the simple
Majority system (where the leader commits operations after receiving
ACKs from a majority of voters) and a more complex Hierarchical
system, where votes of different servers have different weights and
servers are divided into voting groups. Currently, incremental
reconfiguration is allowed only if the last proposed configuration
known to the leader uses a Majority Quorum System
(BadArgumentsException is thrown otherwise).

Incremental mode - examples using the Java API:

    List<String> leavingServers = new ArrayList<String>();
    leavingServers.add("1");
    leavingServers.add("2");
    byte[] config = zk.reconfig(null, leavingServers, null, -1, new Stat());
    
    List<String> leavingServers = new ArrayList<String>();
    List<String> joiningServers = new ArrayList<String>();
    leavingServers.add("1");
    joiningServers.add("server.4=localhost:1234:1235;1236");
    byte[] config = zk.reconfig(joiningServers, leavingServers, null, -1, new Stat());
    
    String configStr = new String(config);
    System.out.println(configStr);

There is also an asynchronous API, and an API accepting comma
separated Strings instead of List<String>. See
src/java/main/org/apache/zookeeper/ZooKeeper.java.

<a name="sc_reconfig_nonincremental"></a>

#### Non-incremental mode

The second mode of reconfiguration is non-incremental, whereby a
client gives a complete specification of the new dynamic system
configuration. The new configuration can either be given in place or
read from a file:

    > reconfig -file newconfig.cfg

//newconfig.cfg is a dynamic config file, see [Dynamic configuration file](#sc_reconfig_file)

    > reconfig -members
    server.1=125.23.63.23:2780:2783:participant;2791,server.2=125.23.63.24:2781:2784:participant;2792,server.3=125.23.63.25:2782:2785:participant;2793}}

The new configuration may use a different Quorum System. For
example, you may specify a Hierarchical Quorum System even if the
current ensemble uses a Majority Quorum System.

Bulk mode - example using the Java API:

    List<String> newMembers = new ArrayList<String>();
    newMembers.add("server.1=1111:1234:1235;1236");
    newMembers.add("server.2=1112:1237:1238;1239");
    newMembers.add("server.3=1114:1240:1241:observer;1242");
    
    byte[] config = zk.reconfig(null, null, newMembers, -1, new Stat());
    
    String configStr = new String(config);
    System.out.println(configStr);

There is also an asynchronous API, and an API accepting comma
separated String containing the new members instead of
List<String>. See
src/java/main/org/apache/zookeeper/ZooKeeper.java.

<a name="sc_reconfig_conditional"></a>

#### Conditional reconfig

Sometimes (especially in non-incremental mode) a new proposed
configuration depends on what the client "believes" to be the current
configuration, and should be applied only to that configuration.
Specifically, the `reconfig` succeeds only if the
last configuration at the leader has the specified version.

    > reconfig -file <filename> -v <version>

In the previously listed Java examples, instead of -1 one could
specify a configuration version to condition the
reconfiguration.

<a name="sc_reconfig_errors"></a>

#### Error conditions

In addition to normal ZooKeeper error conditions, a
reconfiguration may fail for the following reasons:

1. another reconfig is currently in progress
    (ReconfigInProgress)
1. the proposed change would leave the cluster with less than 2
    participants, in case standalone mode is enabled, or, if
    standalone mode is disabled then its legal to remain with 1 or
    more participants (BadArgumentsException)
1. no quorum of the new configuration was connected and
    up-to-date with the leader when the reconfiguration processing
    began (NewConfigNoQuorum)
1. `-v x` was specified, but the version
`y` of the latest configuration is not
`x` (BadVersionException)
1. an incremental reconfiguration was requested but the last
    configuration at the leader uses a Quorum System which is
    different from the Majority system (BadArgumentsException)
1. syntax error (BadArgumentsException)
1. I/O exception when reading the configuration from a file
    (BadArgumentsException)

Most of these are illustrated by test-cases in
*ReconfigFailureCases.java*.

<a name="sc_reconfig_additional"></a>

#### Additional comments

**Liveness:** To better understand
the difference between incremental and non-incremental
reconfiguration, suppose that client C1 adds server D to the system
while a different client C2 adds server E. With the non-incremental
mode, each client would first invoke `config` to find
out the current configuration, and then locally create a new list of
servers by adding its own suggested server. The new configuration can
then be submitted using the non-incremental
`reconfig` command. After both reconfigurations
complete, only one of E or D will be added (not both), depending on
which client's request arrives second to the leader, overwriting the
previous configuration. The other client can repeat the process until
its change takes effect. This method guarantees system-wide progress
(i.e., for one of the clients), but does not ensure that every client
succeeds. To have more control C2 may request to only execute the
reconfiguration in case the version of the current configuration
hasn't changed, as explained in the section [Conditional reconfig](#sc_reconfig_conditional). In this way it may avoid blindly
overwriting the configuration of C1 if C1's configuration reached the
leader first.

With incremental reconfiguration, both changes will take effect as
they are simply applied by the leader one after the other to the
current configuration, whatever that is (assuming that the second
reconfig request reaches the leader after it sends a commit message
for the first reconfig request -- currently the leader will refuse to
propose a reconfiguration if another one is already pending). Since
both clients are guaranteed to make progress, this method guarantees
stronger liveness. In practice, multiple concurrent reconfigurations
are probably rare. Non-incremental reconfiguration is currently the
only way to dynamically change the Quorum System. Incremental
configuration is currently only allowed with the Majority Quorum
System.

**Changing an observer into a
follower:** Clearly, changing a server that participates in
voting into an observer may fail if error (2) occurs, i.e., if fewer
than the minimal allowed number of participants would remain. However,
converting an observer into a participant may sometimes fail for a
more subtle reason: Suppose, for example, that the current
configuration is (A, B, C, D), where A is the leader, B and C are
followers and D is an observer. In addition, suppose that B has
crashed. If a reconfiguration is submitted where D is said to become a
follower, it will fail with error (3) since in this configuration, a
majority of voters in the new configuration (any 3 voters), must be
connected and up-to-date with the leader. An observer cannot
acknowledge the history prefix sent during reconfiguration, and
therefore it does not count towards these 3 required servers and the
reconfiguration will be aborted. In case this happens, a client can
achieve the same task by two reconfig commands: first invoke a
reconfig to remove D from the configuration and then invoke a second
command to add it back as a participant (follower). During the
intermediate state D is a non-voting follower and can ACK the state
transfer performed during the second reconfig command.

<a name="ch_reconfig_rebalancing"></a>

## Rebalancing Client Connections

When a ZooKeeper cluster is started, if each client is given the same
connection string (list of servers), the client will randomly choose a
server in the list to connect to, which makes the expected number of
client connections per server the same for each of the servers. We
implemented a method that preserves this property when the set of servers
changes through reconfiguration. See Sections 4 and 5.1 in the [paper](https://www.usenix.org/conference/usenixfederatedconferencesweek/dynamic-recon%EF%AC%81guration-primarybackup-clusters).

In order for the method to work, all clients must subscribe to
configuration changes (by setting a watch on /zookeeper/config either
directly or through the `getConfig` API command). When
the watch is triggered, the client should read the new configuration by
invoking `sync` and `getConfig` and if
the configuration is indeed new invoke the
`updateServerList` API command. To avoid mass client
migration at the same time, it is better to have each client sleep a
random short period of time before invoking
`updateServerList`.

A few examples can be found in:
*StaticHostProviderTest.java* and
*TestReconfig.cc*

Example (this is not a recipe, but a simplified example just to
explain the general idea):

    public void process(WatchedEvent event) {
        synchronized (this) {
            if (event.getType() == EventType.None) {
                connected = (event.getState() == KeeperState.SyncConnected);
                notifyAll();
            } else if (event.getPath()!=null &&  event.getPath().equals(ZooDefs.CONFIG_NODE)) {
                // in prod code never block the event thread!
                zk.sync(ZooDefs.CONFIG_NODE, this, null);
                zk.getConfig(this, this, null);
            }
        }
    }
    
    public void processResult(int rc, String path, Object ctx, byte[] data, Stat stat) {
        if (path!=null &&  path.equals(ZooDefs.CONFIG_NODE)) {
            String config[] = ConfigUtils.getClientConfigStr(new String(data)).split(" ");   // similar to config -c
            long version = Long.parseLong(config[0], 16);
            if (this.configVersion == null){
                 this.configVersion = version;
            } else if (version > this.configVersion) {
                hostList = config[1];
                try {
                    // the following command is not blocking but may cause the client to close the socket and
                    // migrate to a different server. In practice it's better to wait a short period of time, chosen
                    // randomly, so that different clients migrate at different times
                    zk.updateServerList(hostList);
                } catch (IOException e) {
                    System.err.println("Error updating server list");
                    e.printStackTrace();
                }
                this.configVersion = version;
            }
        }
    }
