### zookeeper使用教程

#### 单机运行参数

单机模式下启动zk很直接，服务器包括在一个jar文件中.

下载稳定版本的zk,解压,并切换到root目录下.

启动zk时需要配置文件,这里是配置文件`conf/zoo.cfg`的配置


    tickTime=2000
    dataDir=/var/lib/zookeeper
    clientPort=2181

这个文件名称随意,但是为了能够方便的找到,放置在`conf/zoo.cfg`中,可以切换数据目录的位置.

既然配置完成,可以启动zk


```shell
bin/zkServer.sh start
```

zk使用log4j作为日志记录

#### zookeeper 存储管理

对于长期生成的系统,zk存储需要外部管理.

#### zookeeper的连接


```shell
$ bin/zkCli.sh -server 127.0.0.1:2181
```

连接成功可以看到如下信息


    Connecting to localhost:2181
    log4j:WARN No appenders could be found for logger (org.apache.zookeeper.ZooKeeper).
    log4j:WARN Please initialize the log4j system properly.
    Welcome to ZooKeeper!
    JLine support is enabled
    [zkshell: 0]

可以从shell中使用`help`指令获取指令列表


```shell
[zkshell: 0] help
ZooKeeper -server host:port cmd args
addauth scheme auth
close
config [-c] [-w] [-s]
connect host:port
create [-s] [-e] [-c] [-t ttl] path [data] [acl]
delete [-v version] path
deleteall path
delquota [-n|-b] path
get [-s] [-w] path
getAcl [-s] path
getAllChildrenNumber path
getEphemerals path
history
listquota path
ls [-s] [-w] [-R] path
ls2 path [watch]
printwatches on|off
quit
reconfig [-s] [-v version] [[-file path] | [-members serverID=host:port1:port2;port3[,...]*]] | [-add serverId=host:port1:port2;port3[,...]]* [-remove serverId[,...]*]
redo cmdno
removewatches path [-c|-d|-a] [-l]
rmr path
set [-s] [-v version] path data
setAcl [-s] [-v version] [-R] path acl
setquota -n|-b val path
stat [-w] path
sync path
```

从这里可以尝试一些简单的指令,例如使用`ls`指令


    [zkshell: 8] ls /
    [zookeeper]

下一步,创建一个znode`create /zk_test my_data`.这个创建znode,且连接`my_data`和这个节点


```shell
[zkshell: 9] create /zk_test my_data
Created /zk_test
```

使用`ls /`查看znode节点


    [zkshell: 11] ls /
    [zookeeper, zk_test]

注意到`zk_test`节点已经创建完毕

下一步获取znode的内容


```shell
[zkshell: 12] get /zk_test
my_data
cZxid = 5
ctime = Fri Jun 05 13:57:06 PDT 2009
mZxid = 5
mtime = Fri Jun 05 13:57:06 PDT 2009
pZxid = 5
cversion = 0
dataVersion = 0
aclVersion = 0
ephemeralOwner = 0
dataLength = 7
numChildren = 0
```

使用`set`设置zk的数据


    [zkshell: 14] set /zk_test junk
    cZxid = 5
    ctime = Fri Jun 05 13:57:06 PDT 2009
    mZxid = 6
    mtime = Fri Jun 05 14:01:52 PDT 2009
    pZxid = 5
    cversion = 0
    dataVersion = 1
    aclVersion = 0
    ephemeralOwner = 0
    dataLength = 4
    numChildren = 0
    [zkshell: 15] get /zk_test
    junk
    cZxid = 5
    ctime = Fri Jun 05 13:57:06 PDT 2009
    mZxid = 6
    mtime = Fri Jun 05 14:01:52 PDT 2009
    pZxid = 5
    cversion = 0
    dataVersion = 1
    aclVersion = 0
    ephemeralOwner = 0
    dataLength = 4
    numChildren = 0

删除zk节点


    [zkshell: 16] delete /zk_test
    [zkshell: 17] ls /
    [zookeeper]
    [zkshell: 18]

#### zk编程

zk有java编写和c编写的程序.基本是类似的.c编程存在两种类型: 单线程和多线程/这个主要是在消息环完成的情况不同.

#### 运行zookeeper副本

对一些程序,使用单机运行时比较好估测的.但是在生成环境下,需要使用副本模式运行zk.服务器组叫做quorum,在副本模式下,所有quorum的服务器都有同样配置文件的副本.



##### **conf/zoo.cfg**
文件的配置与单机模式下类似,但是配置有一些不同,如下:

    tickTime=2000
    dataDir=/var/lib/zookeeper
    clientPort=2181
    initLimit=5
    syncLimit=2
    server.1=zoo1:2888:3888
    server.2=zoo2:2888:3888
    server.3=zoo3:2888:3888

**initLimit**  是zk连接leader的时间上限值，**syncLimit**是服务器同步leader的时间上限限制。

这些参数都可以使用时间单位`ticktime`来度量,这个例子中5个单位代表5*2000ms,也就是10s.

server.x的配置列举了服务器的配置信息,服务器启动的时候,通过它得知服务器寻找的`myid`文件.这个文件包含服务器编号.

最终需要注意到两个端口号,同伴节点使用端口互相联系.这样的端口用于同级服务器的交互.特别地,zk服务器去使用端口去与leader通信.当前选举出leader的时候,follower需要打开TCP连接,去连接leader.因为默认的leader选举也是由TCP协议.需要另一个端口进行leader选举.这个就是第二个端口3888的意义.

