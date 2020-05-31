#### kafka基本操作

在kafka的`bin/`目录下可以使用这些操作工具,会打印详细的信息.

##### 添加或者移除topic

可以手动地添加或者删除topic,当数据首次发送到不存在的topic的时候,这个topic会自动创建.如果使用的是自动创建的topic,可以调整默认的topic配置.

通过下述指令添加topic:

```shell
> bin/kafka-topics.sh --bootstrap-server broker_host:port --create --topic my_topic_name \ --partitions 20 --replication-factor 3 --config x=y
```

副本因子控制了多少个服务器会对写出的消息进行备份.如果副本因子为3,可以容忍2台服务器的失败,建议使用副本因子为2或者3.可以不中断的在进去直接弹性地运行.

分区计数控制了topic会共享多少日志.对于分区计数器有多种营销.首先每个分区必须要符合单台服务器.也就是说20个分区的数据集处理的服务器不能超过20台.

每个共享的分区日志放置于kafka日志目录中本身的文件夹下.文件夹的名称包含topic的名称,添加了一个`-`分割符,后边就是分区的编号.

由于典型的文件夹名称不能超过255个字符长度,因此对于topic的名称也是由限制的.假定分区数量小于100000.因此,topic的名称不能够超过249个字符.这个留有足够的位置给分隔符和5位的分区号.

##### 修改topic

可以使用下述指令修改topic信息(添加分区到40)

```shell
> bin/kafka-topics.sh --bootstrap-server broker_host:port --alter --topic my_topic_name \
      --partitions 40
```

注意到这个是修改分区的语义,且分区的添加不会改变已经存在数据的分区,如果这样做会影响到依赖与当前分区的消费者.如果分区号使用的是hash取余的方法计算处理,那么分区会通过添加分区进行shuffle.但是kafka不会自动的重新对数据进行分配.

添加配置

```shell
> bin/kafka-configs.sh --bootstrap-server broker_host:port --entity-type topics --entity-name my_topic_name --alter --add-config x=y
```

移除配置

```shell
> bin/kafka-configs.sh --bootstrap-server broker_host:port --entity-type topics --entity-name my_topic_name --alter --delete-config x
```

删除topic

```shell
> bin/kafka-topics.sh --bootstrap-server broker_host:port --delete --topic my_topic_name
```

kafka当前不支持topic分区数量的减少操作.

##### 优雅的关闭方法

kafka集群会自动发现broker的关闭或者失败,并且选举出新的leader.无论服务器是否失败这个都会进行.kafka支持优雅的关闭服务器方法.当服务器使用这种关闭方式的时候,会利用如下两种优化方式:

1. 会将所有**日志同步到磁盘**中,避免在**重启的时候进行日志的重做**.日志恢复需要花费时间,用于加速重启.
2. 这个**会将分区迁移到副本**中,然后去关闭.这个会leader选举快速进行转换,且**缩小每个分区的不可访问时间**.

日志同步在服务器停止的时候可以自动发送,但是控制的leader关系迁移需要特殊的配置:

注意只有broker都有副本的情况下才能进行这种关闭。因为**关闭最后一个副本会使得topic的分区不可用**。

##### leadership的平衡

无论broker什么时候停止或者宕机，broker分区的leadership会传递到副本中。当broker重启的时候，会变成一个follower，意味着不会被客户端进行读写。

为了避免不平衡的情况，kafka存在一个最佳副本位置的语义。如果一个分区的副本列表为1,5,9.那么节点1对于leader来说，优先级就高于5，9.默认情况下，kafka会尝试恢复leadership到副本中。这个行为可以通过这个配置：

```shell
auto.leader.rebalance.enable=true
```

将其设置为false,但是就需要手动地恢复副本了:

```shell
> bin/kafka-preferred-replica-election.sh --zookeeper zk_host:port/chroot
```

##### 机架间的副本均衡

机架感知策略将同样分区的副本分布到不同的机架上.这个保证了kafka提供的broker失败包含了机架失败.限制了数据的损失.可以提供添加下述属性,指定broker的属性:

```shell
broker.rack=my-rack-id
```

当一个topic被创建,修改,重新分布,需要保证分区尽可能均衡的分布在机架上.

指定副本到broker的算法保证了每个broker的数量是一个常数,无论broker是怎么分布的.这样可以保证平衡分布.

尽管机架可以分配不同的broker,但是副本的数量不一定一致.具有较少broker的机架具有较多的副本,意味着占用更多的存储,存储更多的资源到副本中.

##### 集群间的镜像数据

指的是kafka集群之间的副本数据,用于避免单机群中副本的碰撞情况.kafka使用工具对kafka集群进行镜像数据操作.这个工具会从一个源集群消费,并生产到一个目标集群中.

通常使用就是提高另一个数据中心的副本.可以运行多个镜像数据进程,用于增减吞吐量和容错性.数据可以从源集群中读取,并以相同的名称写出到目标集群中,事实上,镜像制作工具的大小要小于消费者和生产者的总大小.

源集群和目标集群是完全独立的两个实体,可以有不同数量的分区,且偏移量也不一定一致.对于这个原因,镜像集群具有容错性.镜像制作器会暴露并使用分区消息的key,以便以可以基于key进行排序.

下述显示了如何从一个输入集群进行镜像处理:

```shell
> bin/kafka-mirror-maker.sh
      --consumer.config consumer.properties
      --producer.config producer.properties --whitelist my-topic
```

注意到这个topic是具有白名单属性的,这个参数使用了java标准的正则表达式.可以镜像两个topic.如果需要镜像所有topic,需要使用`--whitelist '*'`.可以使用`,`和`|`指定topic列表.使用的时候需要正确的表达白名单列表.

设置`auto.create.topics.enable=true`参数是的副部集群可以自动创建并且赋值所有数据到目标集群中.

##### 检查消费者的位置指针

有时候查看消费者的位置指针是很有用的,可以使用工具显示消费者中所有消费者的位置指针和距离日志还有多少的信息.

```shell
> bin/kafka-consumer-groups.sh --bootstrap-server localhost:9092 --describe --group my-group
 
TOPIC                          PARTITION  CURRENT-OFFSET  LOG-END-OFFSET  LAG        CONSUMER-ID                                       HOST                           CLIENT-ID
my-topic                       0          2               4               2          consumer-1-029af89c-873c-4751-a720-cefd41a669d6   /127.0.0.1                     consumer-1
my-topic                       1          2               3               1          consumer-1-029af89c-873c-4751-a720-cefd41a669d6   /127.0.0.1                     consumer-1
my-topic                       2          2               3               1          consumer-2-42c1abd4-e3b2-425d-a8bb-e1ea49b29bb2   /127.0.0.1                     consumer-2
```

##### 管理消费者组

使用消费者组指令工具,可以列举,显示,和删除消费者组.消费者组可以手动地进行删除,或者在最后一次提交偏移量之后进行自动删除.

手动删除仅仅会在组中没有激活的元素的时候进行.例如,显示所有的消费者组:

```shell
> bin/kafka-consumer-groups.sh --bootstrap-server localhost:9092 --list
 
test-consumer-group
```

查看消费者组

```shell
> bin/kafka-consumer-groups.sh --bootstrap-server localhost:9092 --describe --group my-group
 
TOPIC           PARTITION  CURRENT-OFFSET  LOG-END-OFFSET  LAG             CONSUMER-ID                                    HOST            CLIENT-ID
topic3          0          241019          395308          154289          consumer2-e76ea8c3-5d30-4299-9005-47eb41f3d3c4 /127.0.0.1      consumer2
topic2          1          520678          803288          282610          consumer2-e76ea8c3-5d30-4299-9005-47eb41f3d3c4 /127.0.0.1      consumer2
topic3          1          241018          398817          157799          consumer2-e76ea8c3-5d30-4299-9005-47eb41f3d3c4 /127.0.0.1      consumer2
topic1          0          854144          855809          1665            consumer1-3fc8d6f1-581a-4472-bdf3-3515b4aee8c1 /127.0.0.1      consumer1
topic2          0          460537          803290          342753          consumer1-3fc8d6f1-581a-4472-bdf3-3515b4aee8c1 /127.0.0.1      consumer1
topic3          2          243655          398812          155157          consumer4-117fe4d3-c6c1-4178-8ee9-eb4a3954bee0 /127.0.0.1      consumer4
```

下面的指令用于查看消费者组的详细信息

+ `--member`: 提供消费者组中的激活元素

  ```shell
  > bin/kafka-consumer-groups.sh --bootstrap-server localhost:9092 --describe --group my-group --members
   
  CONSUMER-ID                                    HOST            CLIENT-ID       #PARTITIONS
  consumer1-3fc8d6f1-581a-4472-bdf3-3515b4aee8c1 /127.0.0.1      consumer1       2
  consumer4-117fe4d3-c6c1-4178-8ee9-eb4a3954bee0 /127.0.0.1      consumer4       1
  consumer2-e76ea8c3-5d30-4299-9005-47eb41f3d3c4 /127.0.0.1      consumer2       3
  consumer3-ecea43e4-1f01-479f-8349-f9130b75d8ee /127.0.0.1      consumer3       0
  ```

+ `--member --verbose`:提供消费者组每个元素的分区信息

  ```shell
  > bin/kafka-consumer-groups.sh --bootstrap-server localhost:9092 --describe --group my-group --members --verbose
   
  CONSUMER-ID                                    HOST            CLIENT-ID       #PARTITIONS     ASSIGNMENT
  consumer1-3fc8d6f1-581a-4472-bdf3-3515b4aee8c1 /127.0.0.1      consumer1       2               topic1(0), topic2(0)
  consumer4-117fe4d3-c6c1-4178-8ee9-eb4a3954bee0 /127.0.0.1      consumer4       1               topic3(2)
  consumer2-e76ea8c3-5d30-4299-9005-47eb41f3d3c4 /127.0.0.1      consumer2       3               topic2(1), topic3(0,1)
  consumer3-ecea43e4-1f01-479f-8349-f9130b75d8ee /127.0.0.1      consumer3       0               -
  ```

+ `--offset`: 默认输出,与`--describe`一致

+ `--state`提供消费者组级别的信息

  ```shell
  > bin/kafka-consumer-groups.sh --bootstrap-server localhost:9092 --describe --group my-group --state
   
  COORDINATOR (ID)          ASSIGNMENT-STRATEGY       STATE                #MEMBERS
  localhost:9092 (0)        range                     Stable               4
  ```

使用`--delete`手动的删除一个或者多个消费者组

```shell
> bin/kafka-consumer-groups.sh --bootstrap-server localhost:9092 --delete --group my-group --group my-other-group
 
Deletion of requested consumer groups ('my-group', 'my-other-group') was successful.
```

重置消费者组的偏移量可以使用`--reset-offsets`参数.这个配置支持一个时间点支持一个消费者组,需要下述参数:`--all-topics`或者`--topic`参数.必须选择一个作用范围,触发使用`--from-file`脚本.

有下述三种执行参数:

+ 默认显示重置的偏移量
+ `--execute`: 执行重置offset进程
+ `--export`: 导入CSV格式的结果

这个也可以使用脚本进行作用域选择

- --to-datetime <String: datetime> :  重置指定数据的偏移量
- --to-earliest : 重置偏移量为最早偏移量
- --to-latest : 重置偏移量到最晚的偏移量
- --shift-by <Long: number-of-offsets> :  偏移n个位置的偏移量
- --from-file : 重置偏移量为指定的csv文件
- --to-current : Resets offsets to current offset.
- --to-offset : 重置偏移量为指定

例如,重置偏移量为最新

```shell
> bin/kafka-consumer-groups.sh --bootstrap-server localhost:9092 --reset-offsets --group consumergroup1 --topic topic1 --to-latest
 
TOPIC                          PARTITION  NEW-OFFSET
topic1                         0          0
```

如果你使用了旧版本的高级消费者功能，且存储组元数据到zk中

```shell
> bin/kafka-consumer-groups.sh --zookeeper localhost:2181 --list
```

##### 集群扩展

添加服务器到kafka集群很容易,仅仅需要指定唯一的broker即可.且在新的服务器上启动kafka即可.然而新的服务器不会自动地指定数据分区,所以触发分区被移除,否则在topic创建之前是不会进行动作的.

所以,添加机器到集群中,需要迁移数据到这些机器中.

迁移数据的处理需要手动开启,kafka新增的服务器是作为follower进行数据迁移.允许当前分区数据的完全复制,当新的服务器完全复制了分区内容的时候,且同步完副本内容之后,就会删除自身分区的数据.

分区的重新分配可以通过broker移动分区.理想的分区分布可以保证数据加载和所有broker的分区大小.分区重新分配的工具不能够自动学习kafka集群的数据分布.管理者必须弄明白哪个topic或者哪个分区会被移动.

分区重新分配包含三种配置方式

- --generate: 这种模式下,给定的topic列表和broker列表,以及工具会生成候补的重新分配.用于将所有指定topic的分区移动到新的broker中.这个配置提供了简便的方式,用于生成分区重新分配计划.
- --execute: 这种模式下,工具会去除基于给定的重新分配计划的分配分区.这个既可以是自定义的,也可以是系统定义的.
- --verify:这种模式下,工具会检查重新分配的状态.这个状态可以是成功,失败或者是进行中.

###### 自动将数据迁移到新的机器上

分区的重新分配工具可以从当前broker集合中移除一些topic到新增的broker中.在扩展集群的时候很有用,因为相比较移动一个分区来说,移动单个完整的topic到新的broker集合中很简单.副本可以高效地移动到新的broker中.

这么做的时候,用于需要提供topic列表,这些topic需要移动到broker集合中.这个工具会分布所有的分区到给定的topic列表中.移动期间,副本因子保持为常数.所有分区的

例如,下述示例会移动`foo1`,`foo2`中所有分区,移除完毕之后topic `foo1`,`foo2`只会存储在broker 5,6中.

先查看json文件

```shell
> cat topics-to-move.json
{"topics": [{"topic": "foo1"},
            {"topic": "foo2"}],
"version":1
}
```

json文件准备好之后,使用分区重分配工具生成一个候补的分配方案

```shell
> bin/kafka-reassign-partitions.sh --zookeeper localhost:2181 --topics-to-move-json-file topics-to-move.json --broker-list "5,6" --generate
Current partition replica assignment
 
{"version":1,
"partitions":[{"topic":"foo1","partition":2,"replicas":[1,2]},
              {"topic":"foo1","partition":0,"replicas":[3,4]},
              {"topic":"foo2","partition":2,"replicas":[1,2]},
              {"topic":"foo2","partition":0,"replicas":[3,4]},
              {"topic":"foo1","partition":1,"replicas":[2,3]},
              {"topic":"foo2","partition":1,"replicas":[2,3]}]
}
 
Proposed partition reassignment configuration
 
{"version":1,
"partitions":[{"topic":"foo1","partition":2,"replicas":[5,6]},
              {"topic":"foo1","partition":0,"replicas":[5,6]},
              {"topic":"foo2","partition":2,"replicas":[5,6]},
              {"topic":"foo2","partition":0,"replicas":[5,6]},
              {"topic":"foo1","partition":1,"replicas":[5,6]},
              {"topic":"foo2","partition":1,"replicas":[5,6]}]
}
```

这个工具产生一个候补的分配方案,这个分配方案会移除所有topic `foo1`和`foo2`的分区,到broker 5,6中,但是这个时候分区的移动尚未开始.当前分配方案需要保存起来.新的分配方案需要使用json文件保存起来,且可以使用`--execute`参数输入.

```shell
> bin/kafka-reassign-partitions.sh --zookeeper localhost:2181 --reassignment-json-file expand-cluster-reassignment.json --execute
Current partition replica assignment
 
{"version":1,
"partitions":[{"topic":"foo1","partition":2,"replicas":[1,2]},
              {"topic":"foo1","partition":0,"replicas":[3,4]},
              {"topic":"foo2","partition":2,"replicas":[1,2]},
              {"topic":"foo2","partition":0,"replicas":[3,4]},
              {"topic":"foo1","partition":1,"replicas":[2,3]},
              {"topic":"foo2","partition":1,"replicas":[2,3]}]
}
 
Save this to use as the --reassignment-json-file option during rollback
Successfully started reassignment of partitions
{"version":1,
"partitions":[{"topic":"foo1","partition":2,"replicas":[5,6]},
              {"topic":"foo1","partition":0,"replicas":[5,6]},
              {"topic":"foo2","partition":2,"replicas":[5,6]},
              {"topic":"foo2","partition":0,"replicas":[5,6]},
              {"topic":"foo1","partition":1,"replicas":[5,6]},
              {"topic":"foo2","partition":1,"replicas":[5,6]}]
}
```

最后,使用`--verify`参数检测分区重分区的状态.

```shell
> bin/kafka-reassign-partitions.sh --zookeeper localhost:2181 --reassignment-json-file expand-cluster-reassignment.json --verify
Status of partition reassignment:
Reassignment of partition [foo1,0] completed successfully
Reassignment of partition [foo1,1] is in progress
Reassignment of partition [foo1,2] is in progress
Reassignment of partition [foo2,0] completed successfully
Reassignment of partition [foo2,1] completed successfully
Reassignment of partition [foo2,2] completed successfully
```

###### 自定义分区重分配

分区重分配工具可以将分区副本移动到broker集合中.当这样使用的时候,假定用于知道重新分配的计划,且不需要工具去生成候选方案,使用`--generate`高效跳过.例如,下述示例会移除topic `foo1`的分区0到broker 5,6中,且将`foo2`的分区1移动到broker 2,3中.

第一步就是除了自定义的重分配计划.

```shell
> cat custom-reassignment.json
{"version":1,"partitions":[{"topic":"foo1","partition":0,"replicas":[5,6]},{"topic":"foo2","partition":1,"replicas":[2,3]}]}
```

使用这个json启动重分配工作

```shell
> bin/kafka-reassign-partitions.sh --zookeeper localhost:2181 --reassignment-json-file custom-reassignment.json --execute
Current partition replica assignment
 
{"version":1,
"partitions":[{"topic":"foo1","partition":0,"replicas":[1,2]},
              {"topic":"foo2","partition":1,"replicas":[3,4]}]
}
 
Save this to use as the --reassignment-json-file option during rollback
Successfully started reassignment of partitions
{"version":1,
"partitions":[{"topic":"foo1","partition":0,"replicas":[5,6]},
              {"topic":"foo2","partition":1,"replicas":[2,3]}]
}
```

`--verify`参数可以用于检查分区重分配情况

```shell
> bin/kafka-reassign-partitions.sh --zookeeper localhost:2181 --reassignment-json-file custom-reassignment.json --verify
Status of partition reassignment:
Reassignment of partition [foo1,0] completed successfully
Reassignment of partition [foo2,1] completed successfully
```

##### 解除broker

分区重分配工具不能自动生成已经解除的broker的重分配方案.管理者可以想出一个重分配方案,去移动所有分区的副本到剩余的broker上.这个需要保证所有副本没有从解除的broker上移动.为了使得进程高效处理,计划将工具对于解除broker的支持在将来的版本中添加.

##### 增加副本因子

副本因子的增加时很简单的,仅仅需要指定额外的重分配json文件即可,且使用`-execute`参数执行,进而增加副本因子.例如,下述示例将分区0的副本有1增加到3.

在增加副本因子之前,分区的唯一副本存储在 broker 5上.增加分区之后,会存储副本到6,7之上.

```shell
> cat increase-replication-factor.json
{"version":1,
"partitions":[{"topic":"foo","partition":0,"replicas":[5,6,7]}]}
```

使用json文件指定新的副本因子

```shell
> bin/kafka-reassign-partitions.sh --zookeeper localhost:2181 --reassignment-json-file increase-replication-factor.json --execute
Current partition replica assignment
 
{"version":1,
"partitions":[{"topic":"foo","partition":0,"replicas":[5]}]}
 
Save this to use as the --reassignment-json-file option during rollback
Successfully started reassignment of partitions
{"version":1,
"partitions":[{"topic":"foo","partition":0,"replicas":[5,6,7]}]}
```

使用`--verify`检查分区重分配的状态

```shell
> bin/kafka-reassign-partitions.sh --zookeeper localhost:2181 --reassignment-json-file increase-replication-factor.json --verify
Status of partition reassignment:
Reassignment of partition [foo,0] completed successfully
```

使用kafka-topic工具增加副本数量

```shell
> bin/kafka-topics.sh --bootstrap-server localhost:9092 --topic foo --describe
Topic:foo   PartitionCount:1    ReplicationFactor:3 Configs:
  Topic: foo    Partition: 0    Leader: 5   Replicas: 5,6,7 Isr: 5,6,7
```

##### 数据迁移过程中限制带宽

kafka允许限制流量的措施,在传输副本的时候可以通过设置带宽上限值达到这个相关.最简单,最安全的方式是调用`kafka-reassign-partitions.sh`脚本,但是`kafka-configs.sh`也可以查看和修改这个值.

例如,如果需要执行重新平衡的时候,通过下述指令,移动数据的时候带宽设置为50MB/S.

```shell
$ bin/kafka-reassign-partitions.sh --zookeeper localhost:2181 --execute --reassignment-json-file bigger-cluster.json --throttle 50000000
```

执行脚本可以看到如下结果

```shell
The throttle limit was set to 50000000 B/s
Successfully started reassignment of partitions.
```

可以重新运行指令传递相同的参数实现

```shell
$ bin/kafka-reassign-partitions.sh --zookeeper localhost:2181  --execute --reassignment-json-file bigger-cluster.json --throttle 700000000
  There is an existing assignment running.
  The throttle limit was set to 700000000 B/s
```

一旦重新平衡玩冲浪,就可以使用`--verify`检查状态位.如果重新平衡已经完成,限流措施就会移除.通过执行`--verufy`参数可以移除限流措施.

当这个指令执行完成的时候,重新分配能够完成,这个脚本会确定限流已经完成.

```shell
> bin/kafka-reassign-partitions.sh --zookeeper localhost:2181  --verify --reassignment-json-file bigger-cluster.json
Status of partition reassignment:
Reassignment of partition [my-topic,1] completed successfully
Reassignment of partition [mytopic,0] completed successfully
Throttle was removed.
```

管理者也会使用` kafka-configs.sh`验证配置值.有两对限流参数值去管理限流处理.可以使用动态参数配置设置broker级别的参数.

```shell
leader.replication.throttled.rate
follower.replication.throttled.rate
```

这是一个限流参数的副本值

```shell
leader.replication.throttled.replicas
follower.replication.throttled.replicas
```

查看限流参数值

```shell
> bin/kafka-configs.sh --describe --zookeeper localhost:2181 --entity-type brokers
Configs for brokers '2' are leader.replication.throttled.rate=700000000,follower.replication.throttled.rate=700000000
Configs for brokers '1' are leader.replication.throttled.rate=700000000,follower.replication.throttled.rate=700000000
```

这里显示了leader和follower的限流参数,默认情况下具有相同的限流值

```shell
> bin/kafka-configs.sh --describe --zookeeper localhost:2181 --entity-type topics
Configs for topic 'my-topic' are leader.replication.throttled.replicas=1:102,0:101,
    follower.replication.throttled.replicas=1:101,0:102
```

默认情况下,`kafka-reassign-partitions.sh`会使用leader的限流措施去进行备份.

###### 限流副本的安全使用方法

1. 限流移除

   一旦重新分配完成,就需要移除限流(可以通过`kafka-reassign-partitions.sh --verify`处理)

2. 确保处于运行状态

   如果限流的量太小,很有可能备份不能进行这种在`max(BytesInPerSec) > throttle`可能发生.

   管理者可以监视副本处理的位置,进行如下配置

   ```shell
   kafka.server:type=FetcherLagMetrics,name=ConsumerLag,clientId=([-.\w]+),topic=([-.\w]+),partition=([0-9]+)
   ```

   

##### 设置定额

1. 配置客户端定额(`user=user1, client-id=clientA`)

   ```shell
   > bin/kafka-configs.sh  --zookeeper localhost:2181 --alter --add-config 'producer_byte_rate=1024,consumer_byte_rate=2048,request_percentage=200' --entity-type users --entity-name user1 --entity-type clients --entity-name clientA
   Updated config for entity: user-principal 'user1', client-id 'clientA'.
   ```

2. 配置客户端定额(`user=user1`)

   ```shell
   > bin/kafka-configs.sh  --zookeeper localhost:2181 --alter --add-config 'producer_byte_rate=1024,consumer_byte_rate=2048,request_percentage=200' --entity-type users --entity-name user1
   Updated config for entity: user-principal 'user1'.
   ```

3. 配置客户端定额(`client-id=clientA:`)

   ```shell
   > bin/kafka-configs.sh  --zookeeper localhost:2181 --alter --add-config 'producer_byte_rate=1024,consumer_byte_rate=2048,request_percentage=200' --entity-type clients --entity-name clientA
   Updated config for entity: client-id 'clientA'.
   ```

4. 配置默认客户端id参数(`user=userA`)

   ```shell
   > bin/kafka-configs.sh  --zookeeper localhost:2181 --alter --add-config 'producer_byte_rate=1024,consumer_byte_rate=2048,request_percentage=200' --entity-type users --entity-name user1 --entity-type clients --entity-default
   Updated config for entity: user-principal 'user1', default client-id.
   ```

5. 配置默认客户端id参数

   ```shell
   > bin/kafka-configs.sh  --zookeeper localhost:2181 --alter --add-config 'producer_byte_rate=1024,consumer_byte_rate=2048,request_percentage=200' --entity-type users --entity-default
   Updated config for entity: default user-principal.
   ```

6. 配置默认客户端id参数

   ```shell
   > bin/kafka-configs.sh  --zookeeper localhost:2181 --alter --add-config 'producer_byte_rate=1024,consumer_byte_rate=2048,request_percentage=200' --entity-type clients --entity-default
   Updated config for entity: default client-id.
   ```

7. 显示配置参数

   ```shell
   > bin/kafka-configs.sh  --zookeeper localhost:2181 --describe --entity-type users --entity-name user1 --entity-type clients --entity-name clientA
   Configs for user-principal 'user1', client-id 'clientA' are producer_byte_rate=1024,consumer_byte_rate=2048,request_percentage=200
   ```

8. 描述给定的用户

   ```shell
   > bin/kafka-configs.sh  --zookeeper localhost:2181 --describe --entity-type users --entity-name user1
   Configs for user-principal 'user1' are producer_byte_rate=1024,consumer_byte_rate=2048,request_percentage=200
   ```

9. 描述给定的默认客户端

   ```shell
   > bin/kafka-configs.sh  --zookeeper localhost:2181 --describe --entity-type clients --entity-name clientA
   Configs for client-id 'clientA' are producer_byte_rate=1024,consumer_byte_rate=2048,request_percentage=200
   ```

10. 描述所有用户

    ```shell
    > bin/kafka-configs.sh  --zookeeper localhost:2181 --describe --entity-type users
    Configs for user-principal 'user1' are producer_byte_rate=1024,consumer_byte_rate=2048,request_percentage=200
    Configs for default user-principal are producer_byte_rate=1024,consumer_byte_rate=2048,request_percentage=200
    ```

    类似于

    ```shell
    > bin/kafka-configs.sh  --zookeeper localhost:2181 --describe --entity-type users --entity-type clients
    Configs for user-principal 'user1', default client-id are producer_byte_rate=1024,consumer_byte_rate=2048,request_percentage=200
    Configs for user-principal 'user1', client-id 'clientA' are producer_byte_rate=1024,consumer_byte_rate=2048,request_percentage=200
    ```



#### kafka配置

##### 生产者配置

```shell
# ZooKeeper
zookeeper.connect=[list of ZooKeeper servers]
 
# Log configuration
num.partitions=8
default.replication.factor=3
log.dir=[List of directories. Kafka should have its own dedicated disk(s) or SSD(s).]
 
# Other configurations
broker.id=[An integer. Start with 0 and increment by 1 for each new broker.]
listeners=[list of listeners]
auto.create.topics.enable=false
min.insync.replicas=2
queued.max.requests=[number of concurrent requests]
```

##### jdk配置

建议使用最少jdk 1.8,且使用jdk 1.8 u5可以使用G1收集器.

```shell
-Xmx6g -Xms6g -XX:MetaspaceSize=96m -XX:+UseG1GC
-XX:MaxGCPauseMillis=20 -XX:InitiatingHeapOccupancyPercent=35 -XX:G1HeapRegionSize=16M
-XX:MinMetaspaceFreeRatio=50 -XX:MaxMetaspaceFreeRatio=80
```

