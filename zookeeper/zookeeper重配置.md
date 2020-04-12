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

在上述示例中，如果服务器5处于系统中，但其他端口不是观察者，一旦配置提交，则会变成观察者，且启动新的端口。这个是将其转换成观察者的简单方式。或者在不重启服务器的情况下改变端口。

zk支持两种不同类型的quorum系统(包括简单的单机系统和复制的分层系统,这个分层系统不同服务器有不同的权重).当前只有在上次使用单机系统更新的时候才支持增量式更新.=

可以使用如下代码,解释增量式更新:

```java
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
```

这个是一个异步API,API接受逗号分割的字符串,将其转换成List.参考`src/java/main/org/apache/zookeeper/ZooKeeper.java`.

##### 非增量式模式

非增量式重新配置,客户端可以完全指定新的动态系统配置.新的配置可以是就地配置或者是从文件中读取.

```shell
> reconfig -file newconfig.cfg
```

这里`newconfg.cfg`是一个动态配置文件

    > reconfig -members
    server.1=125.23.63.23:2780:2783:participant;2791,server.2=125.23.63.24:2781:2784:participant;2792,server.3=125.23.63.25:2782:2785:participant;2793}}

The new configuration may use a different Quorum System. For
example, you may specify a Hierarchical Quorum System even if the
current ensemble uses a Majority Quorum System.

新的配置使用了不同的quorum系统,例如可以指定分层系统.

Bulk 模式:

```java
List<String> newMembers = new ArrayList<String>();
newMembers.add("server.1=1111:1234:1235;1236");
newMembers.add("server.2=1112:1237:1238;1239");
newMembers.add("server.3=1114:1240:1241:observer;1242");

byte[] config = zk.reconfig(null, null, newMembers, -1, new Stat());

String configStr = new String(config);
System.out.println(configStr);
```

这是一个异步API,详情参考
`src/java/main/org/apache/zookeeper/ZooKeeper.java`.

##### 条件重配置

有时候新的配置依赖于客户端现在的配置,只有条件满足才可以使用.特殊情况下,只有leader最后一个配置成功才可以重新配置`reconfig`.

```java
> reconfig -file <filename> -v <version>
```

##### 错误情况

除了正常的zk错误情况下之外.重新配置可能由于下述原因失败:

1. 其他重新配置现在正在运行中
2. 处理的变化使得当前集群中少于两个参与者,这个情况下,启动了独立运行模式,或者是在剩余超出一个参与者关闭独立模式.
3. 新的配置没有quorum连接到leader
4. 指定了`-v x`,但是版本`y`的最新版本不是`x`
5. 上一个leader的配置使用了quorum系统,但是却使用增量式重配置
6. 语法错误
7. IO移除
   详情参考`ReconfigFailureCases.java`.

##### 其余参数

**生命周期**:

为了更好的理解增量式重新配置和非增量式的重新配置,假设客户端C1和服务器D和C2和服务器E.在非增量式模式下,每个客户端会调用`conig`去寻找当前的配置,然后本地创建一个服务器.

新的配置可以通过非增量式重新配置指令.都妹纸完毕之后,只有D.E其中一个可以添加.主要是取决于谁对于leader来说是最新的.后边和可以修改前面的配置.这个方法保证系统进程.但是不能保证客户端都会成功.

使用增量式重新配置，变化会leader应用之后生效。因为客户端可以保证产生进程，这个方法保证强壮的生命力。测试环境下，多线程重新配置很少，非增量式重新配置可以动态改变quorum系统。增量式重新配置仅仅在单机系统才可以使用。

#### 客户端连接的重新平衡

zk集群重启的时候，如果客户端给定相同的连接信息，客户端会随机的选择服务器去连接，这就会使得每个服务器连接数量平衡。通过在服务器重新配置的时候维持这个属性来实现。

为了方法能够执行，客户端必须要订阅配置改变。当观察者触发的时候，客户端需要读取新的配置，通过调用`sync`和`getCondig`方法.如果配置正在使用则调用`updateServerList`方法.

为了避免大量客户端同时迁移,最后每个客户端随机睡眠一段时间再调用.

```java
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
```
