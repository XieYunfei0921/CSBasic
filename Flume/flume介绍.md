#### Flume基本介绍

flume是一个分布式，可靠的，用于高效收集和聚合大量日志源数据到中央数据存储的系统。使用flume不仅可以显示限制日志数据的聚合。因为数据源是可以自定义的，flume可以传输大量事件数据。

##### 系统要求

1.  jdk 1.8+
2. 内存 - 可以高效地进行sources，sink，channel的配置
3. 磁盘空间 -  使用channel或者sink进行高效磁盘空间配置
4. 目录的权限 - 使用代理控制目录的读写权限

##### 基础架构

flume代理是一个JVM进程。flume的source消费的数据来自于外部的web服务器数据源。外部数据源会发送事件到flume，按照指定的flume格式。例如，Avro数据源可以接受avro数据源。

相似的数据量可以使用Thrift Flume数据源去接受Thrift数据源(或者是Thrift的RPC客户端).当Flume源接收到一个事件的时候,会存储到一个或者多个channel中.channel是一个被动的存储,可以保持事件直到被sink消费为止.

文件通道就是一个示例- 可以返回本地的文件系统.sink会移除channel的事件,并将其放置到一个外部的仓库(例如HDFS).或者发送作为下一个Flume Agent的数据源.

<img src="E:\截图文件\Flume基本单元.png" style="zoom:67%;" />

###### 可靠性

事件存储在每个agent的channel中.事件会被分发到下一个agent或者中断存储中.事件只有在传输到下一个环节之后才能够被移除.flume使用事务的方式,保证事件传输的可靠性,source和sink会被压缩到事务中.这个事务,存储和检索事件会分别放置在channel提供的不同事务中.

这个会保证事件集合点对点传输是可靠的.

###### 可恢复性

事件存储在channel中,channel会管理失败恢复的问题.flume支持本地文件系统返回的持久化文件通道.这个也是一个内存的内存通道,这个会简单地将事件存储到内存队列中,这个恢复的速度较快.

#### 搭建flume环境

##### 创建一个Agent

flume的agent存储在本地配置文件中，这是一个text文件，遵守着java属性文件格式。一个或者多个agent的配置可以指定到同样的配置文件下。包含每个source，sink和channel的属性在配置文件中，且指示了如何使用数据流。

##### 配置组件

每个组件（sink,source,channel）都可以使用一个名称表示。例如Avro源需要一个主机和端口的参数用于接收数据。内存通道需要设置队列最大大小，且HDFS的sink需要知道文件系统的URI，用于指示文件创建的位置。

##### 连接组件

Agent需要知道加载的独立组件，且需要知道连接的结构。通过列举每个source，sink，channel的名称。然后知道连接channel的sink和source。

##### 启动Agent

Agent的启动是通过shell脚本执行的，叫做`flume-ng`,这个位于flume的bin目录下.可以指定agent的名称,配置目录和配置文件.

```shell
$ bin/flume-ng agent -n $agent_name -c conf -f conf/flume-conf.properties.template
```

现在Agent就启动了

##### 示例

下述是一个示例配置文件,描述单点Flume部署情况,这个配置使用户产生事件且将事件输出到控制台

```shell
# example.conf: A single-node Flume configuration

# Name the components on this agent
a1.sources = r1
a1.sinks = k1
a1.channels = c1

# Describe/configure the source
a1.sources.r1.type = netcat
a1.sources.r1.bind = localhost
a1.sources.r1.port = 44444

# Describe the sink
a1.sinks.k1.type = logger

# Use a channel which buffers events in memory
a1.channels.c1.type = memory
a1.channels.c1.capacity = 1000
a1.channels.c1.transactionCapacity = 100

# Bind the source and sink to the channel
a1.sources.r1.channels = c1
a1.sinks.k1.channel = c1
```

这个配置定义了一个agent,叫做a1. a1监听端口44444的数据.使用下述指令启动Agent,

```shell
$ bin/flume-ng agent --conf conf --conf-file example.conf --name a1 -Dflume.root.logger=INFO,console
```

Flume可以在日志中使用子环境变量,例如

```shell
a1.sources = r1
a1.sources.r1.type = netcat
a1.sources.r1.bind = 0.0.0.0
a1.sources.r1.port = ${NC_PORT}
a1.sources.r1.channels = c1
```

这里的NC_PORT就是一个子环境变量.

##### 记录行式数据

在大多数生成环境下,记录行式数据流的操作的数据消化方式不是一种期望的方式.因为会导致敏感数据的丢失或者相关配置的安全问题.默认情况下,flume不会记录这些信息.另一方面,如果数据管道破损了,flume可以对问题进行debug.

一种调试问题的方式就是创建一个额外的内存通道,这个通道连接到logger的sink位置,用于输出所有事件的数据到flume日志中.为了启动日志的数据,java的系统参数必须设定在`log4j`中.为了开启配置相关的记录,设置java系统参数,可以设置参数`-Dorg.apache.flume.log.printconfig=true`.这个可以通过命令行传递.

可以设置java参数,`-Dorg.apache.flume.log.rawdata=true`启动数据记录/对于大多数组件来说,log4j的日志级别为DEBUG或者TRACE.

```shell
$ bin/flume-ng agent --conf conf --conf-file example.conf --name a1 -Dflume.root.logger=DEBUG,console -Dorg.apache.flume.log.printconfig=true -Dorg.apache.flume.log.rawdata=true
```

##### 基于zookeeper的配置

flume支持通过zk配置agent,这个是一个实验状态下的功能.

##### 三方插件的使用

插件目录位于`$FLUME_HOME/plugins.d`的位置.这个位置下的文件有三个子目录

1. lib 插件的jar包
2. libext 插件的依赖jar包
3. native 任何需要的本地库,以`.so`结尾的文件

##### 数据消费



##### 设置多个Agent流

将上一个Agent的sink作为当前Agent的source可以连接两个Agent

<img src="E:\截图文件\连接.png" style="zoom:67%;" />

##### Agent合并

这样做之后，合并后的Agent的单点压力很大

<img src="E:\截图文件\流合并原理图.png" style="zoom:67%;" />

##### 流复用

<img src="E:\截图文件\分流设置.png" style="zoom:67%;" />

