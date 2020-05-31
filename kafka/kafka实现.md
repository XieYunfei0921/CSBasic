#### 网络层

网络层是一个直接的NIO服务器。通过接口`MessageSet`中的`writeTo`方法对消息进行发送.允许消息集使用更高效的`transferTo`实现.线程模型是一个单个的接收器线程,和N个处理线程.

这个协议十分的简单,可以允许其他语言客户端线程的实现.

#### 消息

消息包含编程的header,一个变长的key数组,和一个变长的value数组.`RecordBatch`接口是一个简单的消息迭代器,使用指定的方法可以变量地对NIO通道进行读写.

#### 消息格式

消息总是按照批次写入的,从技术角度来看,一个批次的消息就是一个记录批次,且一个记录批次包含一条或者多条记录.退一步说,如果一个记录批次包含一条记录,记录批次和记录都有自己本身的header.格式如下:

#### 记录批次

##### 记录批次

  下面是记录批次在磁盘上的格式

```shell
  baseOffset: int64
  batchLength: int32
  partitionLeaderEpoch: int32
  magic: int8 (current magic value is 2)
  crc: int32
  attributes: int16
      bit 0~2:
          0: no compression
          1: gzip
          2: snappy
          3: lz4
          4: zstd
      bit 3: timestampType
      bit 4: isTransactional (0 means not transactional)
      bit 5: isControlBatch (0 means not a control batch)
      bit 6~15: unused
  lastOffsetDelta: int32
  firstTimestamp: int64
  maxTimestamp: int64
  producerId: int64
  producerEpoch: int16
  baseSequence: int32
  records: [Record]
```

  注意这里开启了压缩,压缩的记录数据直接被序列化.

  CRC对记录批次进行校验.位于魔数之后,意味着客户端必须在转换批次长度和魔数之前转换魔数.分区leader属性不需要包含在CRC的计算范围之内,主要是避免CRC的重新计算.这里使用的是CRC-32算法.

  计算: 和旧的消息形式不同,新的版本保证日志清除的时候原始批次的首个和最后一个offset位置.这个需要日志重载的时候,恢复生产者的状态.如果没有保留上一个记录的序列编号,生产者将会出现`OutOfSequence`错误.

  基本的序列编号必须要可以进行副本检查.可能会出现当批次记录中的所有记录都被清除的时候,但是仍然保持生产者上一个序列的编号,这样就会出现一个空批次.

###### 批次控制

控制批次包含一个叫做控制记录的记录。控制记录不能传递给应用。相反地，可以使用消费者过滤抛弃的事务消息。

控制记录的key包含下属内容:

```shell
version: int16 (current version is 0)
type: int16 (0 indicates an abort marker, 1 indicates a commit)
```

##### 记录

记录的header概念在kafka 0.11.0版本中引入，磁盘上记录的形式如下

```shell
length: varint
attributes: int8
    bit 0~7: unused
timestampDelta: varint
offsetDelta: varint
keyLength: varint
key: byte[]
valueLen: varint
value: byte[]
Headers => [Header]
```

###### 记录的header形式

```shell
headerKeyLength: varint
headerKey: String
headerValueLength: varint
Value: byte[]
```

##### 旧的消息格式

在kafka 0.11版本之前,消息使用**消息集合**进行转换或者存储.在消息集合中,每条消息都有它的元数据.

下面是消息集合的格式:

```shell
MessageSet (Version: 0) => [offset message_size message]
    offset => INT64
    message_size => INT32
    message => crc magic_byte attributes key value
        crc => INT32
        magic_byte => INT8
        attributes => INT8
            bit 0~2:
                0: no compression
                1: gzip
                2: snappy
            bit 3~7: unused
        key => BYTES
        value => BYTES
```

```shell
MessageSet (Version: 1) => [offset message_size message]
    offset => INT64
    message_size => INT32
    message => crc magic_byte attributes key value
        crc => INT32
        magic_byte => INT8
        attributes => INT8
            bit 0~2:
                0: no compression
                1: gzip
                2: snappy
                3: lz4
            bit 3: timestampType
                0: create time
                1: log append time
            bit 4~7: unused
        timestamp =>INT64
        key => BYTES
        value => BYTES
```

在kafka 0.10之前的版本,支持消息格式版本是0.消息版本1,携带有时间戳信息,在kafka 0.10版本开始支持.

+ 在版本2以及之上的版本,最低位属性代表压缩类型
+ 在版本1中,生产者需要设置时间戳类型位为0.如果topic设置使用了日志添加时间,broker就会重写时间戳类型和消息集合中的时间戳信息
+ 属性的最高位必须设置为0

消息格式版本0和1中,kafka支持循环信息的压缩.这种情况下,必须设置消息属性,用于表示压缩类型和value属性,这个属性会包含压缩类型的消息.

经常称作其为内部消息,且包装后的消息叫做外部消息.注意到外部消息的key必须是空,且offset是最后一个内部消息的offset.

当收到的版本号为0的时候,broker会将其进行解压缩,且每条内部消息都会设置一个offset.在版本1中,为了避免服务器侧的重复压缩,仅仅保证消息会被设置offset.

内部消息包含相关的offset,决定offset可以使用外部消息的offset计算处理,与最后一个内部消息的offset一致.

#### 日志

topic名称为`my_topic`的日志,拥有两个分区,包含两个目录`my_topic_0`和`my_topic_1`.使用包含topic消息的数据文件.日志文件的格式是日志条目的一个序列而已.

每个日志条目是一个4字节的整数N,用于存储消息长度,这个长度运行N个消息的字节长度.每条消息都被64位的offset唯一标识.这个标识符给定了消息在消息流中的位置指针.

每条消息的消息格式如下所示,每个日志文件都已offset和第一条消息相关.所有创建的第一个文件叫做`00000000000.kafka`.每个新的文件都有一个整数的名称,这个名称粗略有S个字节.S是配置给定的最大日志文件大小.

记录的格式是使用版本标记的,且使用标准接口进行维护.所以批次记录可以在生产者,broker和客户端直接进行转换.不需要对其进行重新的复制.

开始打算使用GUID生成生产者唯一的消息编号,且维护一个GUID和每个broker的映射.但是由于消费者必须要委会id和服务器之间的映射关系,且GUID全局标识符提供了空值.

此外,维护这个映射需要重量级的数据结构,必须要使用磁盘进行同步.因此需要使用一个简单的数据结构,且可以使用简单的安装分区的原子计数器.

然而,一旦设置了计数器之后,直接使用offset就会使用的很自然.因为offset对于消费者来说是隐蔽的,需要使用更加高效的方式.

<img src="E:\截图文件\kafka日志.png" style="zoom:67%;" />

##### 写操作

日志允许**串行地添加到文件**的最后,这个文件在达到配置的大小的时候会**滚动刷新**(例如1GB).这个日志有两个配置参数

第一个是M,这个参数是**使用操作系统刷写到磁盘上的消息数量**

第二个参数是S,为刷写的时间设置

##### 读取操作

读取操作使用给定的64位消息逻辑偏移量(offset),和S位最大块大小读取.这个会返回一个消息迭代器.这个带权包含一个S位的缓冲区.

S(**刷新的时间**)需要大于单条消息长度,但是对于不正常的巨大的消息,这个读取操作可以重试多次,每次会对**缓冲区进行翻倍操**作,直到消息可以成功读取为止.

根据最大消息和缓冲区大小,可以指定服务器拒绝大于指定大小的消息,且给定客户端一个界限值,这个界限值是获取一条完整消息的最大值.

很有可能读取的缓冲区局部消息的形式结束,可以被消息的界定规则轻易的辨识.

实际的偏移量读取进程需要定位日志段文件的首地址,数据从这里开始存储,可以从全局偏移量的值计算文本指定的偏移量.然后读取文件的偏移量.通过简单的二叉搜索树进行简单的内存查找.

日志提供获取最新写入消息的方法,用于允许客户端立即启动订阅.这个在几天之内消费失败是有效的.这种情况下,客户端会消费不存在的偏移量数据,会引起`OutOfRangeException`异常.且失败的时候会重置.

下述是发送到消费者的形式:

```shell
MessageSetSend (fetch result)
 
total length     : 4 bytes
error code       : 2 bytes
message 1        : x bytes
...
message n        : x bytes
```

```shell
MultiMessageSetSend (multiFetch result)
 
total length       : 4 bytes
error code         : 2 bytes
messageSetSend 1
...
messageSetSend n
```

##### 删除

在一个时间点上,数据通过删除一个日志片段进行删除.日志管理器运行可插入式的删除策略,可以选择哪些文件会被删除.**当前策略会删除带有修改时间超过N天的日志**.(尽管日志可以保留之前的N GB的内容)

为了避免允许删除修改文件段列表(使用coW策略)的时候锁定读取,允许删除处理的时候对一个可变的静态快照进行二分查找.

##### 保证

日志提供了参数M的配置,这个会控制消息的最大数量.启动日志恢复进程就是对所有消息进行迭代,且需要验证每条消息是否可用.

消息条目在总数的大小和偏移量小于文件大小且消息的CRC32校验码与消息匹配的时候,说明消息可用.

#### 分布式

##### 消费者偏移量定位

kafka消费者定位了每个分区最大的消费偏移量,且留有余量去提交偏移量.以便于可以从这些偏移量中恢复.kafka提供参数去给每个消费者组存储所有的偏移量,讲这些存储到指定消费者组的broker中,这个叫做组协作者.

任何消费者组中的消费者实例需要发送提交的偏移量,且获取组协作者(broker).消费者需要指定协作者的组名称.一个消费者可以通过向任意一个kafka broker发送`FindCoordinatorRequest`查找相应的协作者.且读取响应`FindCoordinatorResponse`.这个响应体会包含协作的详细信息.

消费者然后会提交并且获取协作者broker的偏移量/当协作者移动的时候,消费者会重新发现协作者,消费者提交可以自动或者手动的由消费者实例完成.

当组协作者接收到一个偏移量的提交请求`OffsetCommitRequest`的时候,会添加请求到指定的kafka topic中(叫做*__consumer_offsets*中).broker在偏移量topic的副本接收到偏移量之后,会发送成功提交的响应给消费者.

如果在配置的超时时间内,不能够进行备份,偏移量的提交会失败,且消费者会重新提交偏移量.

broker会周期性地合并偏移量topic,因为需要在每个分区中保持最新的offset提交情况.

协作者可以将偏移量缓存到内存表中,为了可以服务可以快速获取.

当协作者接收到偏移量获取请求的时候，会简单地从缓存中返回上次提交的偏移量向量表。这种情况下，协作者仅仅会启动。或者会变成新的消费者组集合的协作者，这样的话需要加载偏移量分区信息到缓存中。

如果加载失败会抛出`CoordinatorLoadInProgressException`异常,且消费者会重试`OffsetFetchRequest`

##### zookeeper目录

###### 语义

一个节点的路径为`xyz`,意味着xyz不是固定的,且事实上有一个zk的znode对应于这个xyz.例如,`/topics/[topic]`在目录`/topics`目录下的topic子目录中.数字范围在0-5之间.

###### Broker Topic的注册

```shell
/brokers/topics/[topic]/partitions/[0...N]/state --> {"controller_epoch":...,"leader":...,"version":...,"leader_epoch":...,"isr":[...]} (ephemeral node)
```

每个broker会自我注册,维护和存储topic的分区数量

###### 集群编号

集群编号是一个唯一的不可变的标识符,用于kafka集群.这个集群编号参数由最大22个字符,允许范围`[a-zA-Z0-9_\-]+`.当集群启动的时候会自动生成.

智能实现: 在kafka 0.10.1版本之后,broker启动的时候会自动生成.broker尝试从`/cluster/id`znode中字段获取集群id.如果znode不存在,broker会自动生成集群编号,并且创建这个znode.

###### Broker 节点注册

broker节点是独立的,所以仅仅会发布其自身的信息.当broker加入的时候,会进行自我注册,且写入主机和端口信息.broker也会注册存在的topic信息和topic注册的逻辑分区信息.新的topic可以动态注册.