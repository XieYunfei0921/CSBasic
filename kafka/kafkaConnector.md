#### 介绍

kafka connect是一个可以扩展的流式数据连接工具，用于连接kafka和其他系统。使得可以快照定义**连接器**，这个连接器可以移动大量kafka的数据。kafka连接器可以消费完全的数据库信息，或者收集其他度量信息到kafka topic中华，使得数据可以被系统低延时获取。导出的工作可以连接kafka的topic到存储和查询设备中，用于离线分析。

kafka连接的功能包含如下几种功能：

+ kafka连接器的通用框架
+ 分布式/单机运行模式
+ REST接口
+ 原子性偏移量管理
+ 分布式可扩展
+ 流式/批处理

#### 用户教程

##### 运行kafka连接器

kafka连接器支持两种执行模式，分别是单机模式和分布式模式。

在单机模式中，所有操作都是在一个进程中执行的。配置简单，且适用于一些单机处理的任务，例如日志文件的收集。但是不能够使用容错的措施。

```shell
> bin/connect-standalone.sh config/connect-standalone.properties connector1.properties [connector2.properties ...]
```

第一个参数值worker的配置,包括kafka的连接参数,序列化格式,且提交偏移量的频率.使用了`config/server.properties`的配置文件运行.需要使用不同的配置进行不同的部署.所有的worker需要一些配置:

- `bootstrap.servers`

  kafka服务器列表,用于启动kafka的连接

- `key.converter` 

  用于转换kafka连接格式和序列化形式的转换器.控制消息的格式.通常格式为JSON或者Avro格式

- `value.converter` 

  转换器类,用于转换写入到kafka连接的数据格式.控制了消息的value值.主要的格式为JSON或者Avro.

单机模式下的重要配置为

`offset.storage.file.filename`: 存储偏移量数据的文件

使用kafka连接器配置的生产者和消费者参数,主要包括偏移量和状态topic.对于生成者的配置,可以被kafka source 任务使用.而消费者可以作为kafka sink 任务使用.通常的参数需要不同的前缀.仅仅kafka客户端参数不需要前缀的设置,这个在大多数情况下是高效的.对于安全的集群来说,需要连接的更多参数.这些参数需要管理权限设置.

自从kafka 2.3.0开始,客户端配置的重写可以单独配置.且可以单独地被前缀配置(`producer.override.`和consumer.override`)使用.剩余的参数是连接器的配置文件.如果指定了多个参数,就会在进程中执行多个线程.

分布式模式处理原子性工作的平衡,允许动态的扩展,且对于偏移量提交数据提供默认的容错措施.

```shell
> bin/connect-distributed.sh config/connect-distributed.properties
```

不同的是启动的类不同,且配置参数变化了,这里的参数决定了连接器何处存储配置信息,如何去分配工作已经在什么地方存储偏移量和任务状态信息.在分布式模式下,kafka控制层会存储偏移量,配置和任务状态到kafka topic中.推荐手动创建topic 偏移量,配置以及状态信息,用于获取期望的分区数和副本因子参数.

下述配置参数中,除了通用的配置方法外,注意需要在启动集群之间设置.

- `group.id` 

  集群唯一标识,用于在连接器集群组中构建,之一不要和消费者组ID冲突

- `config.storage.topic` 

  用于存储连接器和任务配置的topic,注意到需要是一个分区,高度的聚合的topic.可以手动的创建topic,保证正确性.

- `offset.storage.topic` 

  用于存储偏移量的topic，可以有多个分区，可以进行备份，可以合并

- `status.storage.topic` 

  用于存储状态，topic可以有分区

注意到分布式模式下，连接器配置不会使用命令行传输。相反使用Rest API创建，修改和销毁连接。

##### 配置kafka连接器

连接器的配置使用简单的kv映射，对于单机模式，这些配置可以使用命令行传输。在分布式模式下，使用json形式请求创建连接器。多数配置都是连接器独立性的。但是有几个共通的配置：

- `name` 

  连接器的唯一标识符，使用相同的名称进行申请会失败

- `connector.class` - The Java class for the connector

  连接器的java类

- `tasks.max`

  任务的最大数量，如果到达不了这个并行度，则会创建一些任务

- `key.converter` 

  worker的key转换器

- `value.converter` 

  worker的value转换器

sink连接器有多个配置控制输入。每个sink的连接器必须设置下述参数

- `topics`

  逗号分割的topic列表，用于作为连接器的输入

- `topics.regex`

  输入连接器的java正则表达式

##### 转换器

连接器可以使用转换器进行轻量级实时修改配置。方便数据的传输和事件的路由。

转换链可以指定在连接器的配置中

- `transforms` 

  转换器列表，指定转换器使用的顺序

- `transforms.$alias.type`

  转换器的名称

- `transforms.$alias.$transformationSpecificConfig`

  转换器的配置属性

例如，使用文件source连接器，且使用转换器添加静态属性。在`standalone.properties`文件中配置下述信息为false

```shell
key.converter.schemas.enable
value.converter.schemas.enable
```

文件source 连接器读取每行的信息,并将其包装为map,添加一个第二属性去辨识时间.这里需要使用两个转换器

+ **HoistField** 是Map中防止输入内容的位置
+ **InsertField** 用于添加静态属性

添加转换器之后,设置`connect-file-source.properties`文件

```shell
name=local-file-source
connector.class=FileStreamSource
tasks.max=1
file=test.txt
topic=connect-test
transforms=MakeMap, InsertSource
transforms.MakeMap.type=org.apache.kafka.connect.transforms.HoistField$Value
transforms.MakeMap.field=line
transforms.InsertSource.type=org.apache.kafka.connect.transforms.InsertField$Value
transforms.InsertSource.static.field=data_source
transforms.InsertSource.static.value=test-file-source
```

使用`transform`开头的内容会添加到转换器中,可以参考参加的两个转换器(InsertSource和MakeMap).每个转换器都有额外的配置.`HoistField`需要field属性,InsertField需要指定属性名称.不使用转换器使用`kafka-console-consumer.sh`的时候结果是

```shell
"foo"
"bar"
"hello world"
```

使用转换器的结果如下

```shell
{"line":"foo","data_source":"test-file-source"}
{"line":"bar","data_source":"test-file-source"}
{"line":"hello world","data_source":"test-file-source"}
```

主要的转换器如下:

- InsertField 

  使用静态属性或者记录元数据添加属性

- ReplaceField 

  过滤器或者重命名属性

- MaskField 

  使用空值或者0代替属性

- ValueToKey

- HoistField

  使用单个结构体或者Map包装整个事件

- ExtractField 

  抓取一个特定的结构体/map

- SetSchemaMetadata 

  修改schema名称或者版本信息

- TimestampRouter

  修改时间戳,基于时间戳进行路由

- RegexRouter

具体情况参考源码`org.apache.kafka.connect.transforms.*`

##### REST API

由于kafka连接器可以作为服务运行,所以提供了REST API用于管理连接器.REST API的服务器可以使用`listen`配置设置.这个属性包括连通器列表`protocol://host:port,protocol2://host2:port2`.当前支持`http/https`协议.

```shell
listeners=http://localhost:8080,https://localhost:8443
```

默认情况下,没有指定监听器,rest服务器在http的8083端口上.当使用HTTPS协议的时候,必须要使用SSL配置.默认情况下,使用`ssl.*`配置.这种情况下,需要对broker使用REST API的不同配置.属性的前缀为`listerners.https`.使用前缀的时候,仅仅会使用前缀属性.`ssl.*`配置会被忽视.下述属性可以配置HTTPS的REST API

- `ssl.keystore.location`
- `ssl.keystore.password`
- `ssl.keystore.type`
- `ssl.key.password`
- `ssl.truststore.location`
- `ssl.truststore.password`
- `ssl.truststore.type`
- `ssl.enabled.protocols`
- `ssl.provider`
- `ssl.protocol`
- `ssl.cipher.suites`
- `ssl.keymanager.algorithm`
- `ssl.secure.random.implementation`
- `ssl.trustmanager.algorithm`
- `ssl.endpoint.identification.algorithm`
- `ssl.client.auth

REST API不仅仅可以被用户使用,用于监控和管理器kafka客户端.也可以用来进行多个集群间的交互.follower节点接受的REST API会被发送到leader节点上.这种情况下会监听给定host的API,使用`rest.advertised.host.name`, `rest.advertised.port`和`rest.advertised.listener`配置可以改变leader的URI地址.当使用HTTP和HTTPS监听的时候,`rest.advertised.listener`可以用于跨集群的交互.

支持下述rest api

- `GET /connectors`

  返回激活的连接器

- `POST /connectors` 

  创建新的连接器,请求体需要是json格式,包含`name`属性,和`config`属性

- `GET /connectors/{name}` 

  获取指定连接器的信息

- `GET /connectors/{name}/config`

  获取指定连接器的配置参数

- `PUT /connectors/{name}/config` 

  更新指定连接器的配置参数

- `GET /connectors/{name}/status` 

  获取连接器当前的状态,包括运行状态(运行,失败,暂停).如果失败则会显示错误信息.

- `GET /connectors/{name}/tasks` 

  获取运行在连接器上的任务列表

- `GET /connectors/{name}/tasks/{taskid}/status`

  获取任务的当前状态,可以是运行,失败,暂停等等

- `PUT /connectors/{name}/pause` 

  暂停连接器和它的任务,这个会停止消息的处理直到连接器恢复位置

- `PUT /connectors/{name}/resume` 

  重启一个暂停的连接器

- `POST /connectors/{name}/restart` 

  重启连接器

- `POST /connectors/{name}/tasks/{taskId}/restart` 

  重启单个任务

- `DELETE /connectors/{name}` 

  删除连接器,会停止所有任务并删除

- `GET /connectors/{name}/topics` 

  获取连接器的topic集合

- `PUT /connectors/{name}/topics/reset` 

  发送清空连接器中topic的请求

- `GET /connector-plugins`

  返回kafka连接器集群安装的连接器插件集合.

- `PUT /connector-plugins/{connector-type}/config/validate` 

  验证配置的给定参数

- `GET /` 返回kafka连接器集群的基本信息

#### 连接器开发指南

##### 核心概念和API

###### 连接器和任务

为了可以kafka实现与其他系统之间的数据复制,对这个系统使用连接器,可以拉取推送的数据.连机器包含两个支持的部分.`SourceConnectors`支持从其他系统中导入数据(例如`JDBCSourceConnector` ).`SinkConnectors` 可以导出数据到系统中(例如`HDFSSinkConnector` ).

连接器不会拷贝数据倍数,连接器将job划分为任务集合.并将任务分发给worker,这些任务会分为`SourceTask` 和`SinkTask`.

每个任务必须要从kafka中拷贝数据的子集.在kafka连接器中,需要使用schema去指定输出/输出流的集合.有时候映射时相当明显的,每个日志未经集合的文件都可以看做时流,且每次都使用同样的schema进行转换,并将偏移量存储在文件中.其他情况下,需要更多的方式去映射模型.

###### 流和记录

每个流需要是kv记录的序列,kv都是复杂的数据结构,可能提供多个类型.运行时数据格式不会假定任何指定的序列化形式.这个会被框架内部处理了.处理kv记录可是之外,记录还有id和偏移量的设置,这个参数由框架来周期性地提交数据偏移量.处理过程可以从上次修改的偏移量开始进行,避免不必要的重新处理和事件复制的情况.

###### 动态连接器

不是所有的job都是静态的,所以连接器实现可以监视外部系统的变化.当变化发送的时候,会提示框架,且框架会更新tasks.

#### 连接器的开发

连接器的开发需要实现两个接口,`Connector`和`Task`接口.一个简单的例子就是就是将其置于kafka源码的`file`的package下.这个连接器可以使用在单机模式下,且可以实现`SourceConnector`/`SourceTask`去读取文件的行文本,且使用`SinkConnector`/`SinkTask`去写出记录到文件中.

##### 连接器示例

首先实现一个`SinkConnector` ,继承`SinkConnector` 接口，添加的属性会存储转换后的配置信息。

```java
public class FileStreamSourceConnector extends SourceConnector {
    private String filename;
    private String topic;
}
```

指定任务所使用的类,定义worker进程会读取的类

```java
@Override
public Class<? extends Task> taskClass() {
    return FileStreamSourceTask.class;
}
```

定义`FileStreamSourceTask` 类,添加标准的生命周期控制方法`start()`,`stop()`

```java
@Override
public void start(Map<String, String> props) {
    // The complete version includes error handling as well.
    filename = props.get(FILE_CONFIG);
    topic = props.get(TOPIC_CONFIG);
}
 
@Override
public void stop() {
    // Nothing to do since no background monitoring is required.
}
```

最后,核心实现就是在`taskConfigs()`方法,这里仅仅处理一个文件,所以尽管需要生成`maxTasks`参数,每次都会返回一个列表.

```java
@Override
public List<Map<String, String>> taskConfigs(int maxTasks) {
    ArrayList<Map<String, String>> configs = new ArrayList<>();
    // Only one input stream makes sense.
    Map<String, String> config = new HashMap<>();
    if (filename != null)
        config.put(FILE_CONFIG, filename);
    config.put(TOPIC_CONFIG, topic);
    configs.add(config);
    return configs;
}
```

尽管这里没有使用,`SourceTask`提供了两个提交偏移量的API.分别是`commit`和`commitRecord`.这些API可以识别消息的原理.重写这些方法可以使得source连接器识别source系统的消息.`commit`API会存储偏移量到source系统中.这个实现会阻塞到提交完成.`commitRecord`在写入到kafka中会保存偏移量到source系统中.因为kafka的连接器会自动记录偏移量,所以`SourceTask`不需要实现.

尽管使用了多个任务,方法的实现非常简单.这个必须要决定输入任务的数量.

##### 任务实例（source）

创建一个类继承`Task`类,需要设置标准的声明周期方法

```java
public class FileStreamSourceTask extends SourceTask {
    String filename;
    InputStream stream;
    String topic;
 
    @Override
    public void start(Map<String, String> props) {
        filename = props.get(FileStreamSourceConnector.FILE_CONFIG);
        stream = openOrThrowError(filename);
        topic = props.get(FileStreamSourceConnector.TOPIC_CONFIG);
    }
 
    @Override
    public synchronized void stop() {
        stream.close();
    }
}
```

首先,`start()`方法不会出来之前偏移量的恢复工作.其次,`stop()`方法是同步的.

下一步设置轮询方法`poll()`,给定输入系统的事件,并返回一个`List<SourceRecord>`

```java
@Override
public List<SourceRecord> poll() throws InterruptedException {
    try {
        ArrayList<SourceRecord> records = new ArrayList<>();
        while (streamValid(stream) && records.isEmpty()) {
            LineAndOffset line = readToNextLine(stream);
            if (line != null) {
                Map<String, Object> sourcePartition = Collections.singletonMap("filename", filename);
                Map<String, Object> sourceOffset = Collections.singletonMap("position", streamOffset);
                records.add(new SourceRecord(sourcePartition, sourceOffset, topic, Schema.STRING_SCHEMA, line));
            } else {
                Thread.sleep(1);
            }
        }
        return records;
    } catch (IOException e) {
        // Underlying stream was killed, probably as a result of calling stop. Allow to return
        // null, and driving thread will handle any shutdown if necessary.
    }
    return null;
}
```

这个`poll()`方法会被重复的调用,每次调用会循环的尝试从文件中读取记录.对于读取的每行内容,可以定位到其在文件中的偏移量.使用这些信息来创建下面4类信息.

+ source分区
+ source的偏移量
+ 输出topic的名称
+ 输出的值

##### sink任务

`SinkTask`的接口方法不太一样,因为sink是将数据push到系统中.

```java
public abstract class SinkTask implements Task {
    public void initialize(SinkTaskContext context) {
        this.context = context;
    }
 
    public abstract void put(Collection<SinkRecord> records);
 
    public void flush(Map<TopicPartition, OffsetAndMetadata> currentOffsets) {
    }
}
```

`put()`方法需要包含大量的实现,包括接受`SinkRecords`记录集,进行需要的转换操作,并存储到指定的系统中.这个方法不需要保证数据完全写入到目标系统中.事实上,大多数情况下,内部的缓冲区是相当有作用的.`SinkRecords`包含基本的信息: kafka的topic,分区,偏移量,kv信息以及header信息.

`flush()`方法在偏移量提交的过程中调用,允许任务从失败中恢复,且从安全点位置重启.这样就不会遗失事件.这个方法将输出存储到目标系统中,且会阻塞到接受数据块完成.偏移量参数可以忽略,但是在某些情况下很有用,提供了**exactly-once**的存储方式.例如,HDFS连接器会使用原子性的移动操作保证`flush()`操作原子性地提交操作到HDFS中.

##### 恢复上一个偏移量

`SourceTask` 实现了包含流式ID(输入文件的名称),偏移量(文件中的偏移量).框架会周期性地提交偏移量,人会会最小化时间重做的情况下进行恢复.提交进程由框架自动完成,但是仅仅连接器会知道如何找到正确的偏移量位置.

为了能够正确地进行重启,任务需要使用`SourceContext` 传输到`initialize()`中,从而获取偏移量数据.

```java
stream = new FileInputStream(filename);
Map<String, Object> offset = context.offsetStorageReader().offset(Collections.singletonMap(FILENAME_FIELD, filename));
if (offset != null) {
    Long lastRecordedOffset = (Long) offset.get("position");
    if (lastRecordedOffset != null)
        seekToOffset(stream, lastRecordedOffset);
}
```

#### 配置连接的校验

`FileStreamSourceConnector` 定义了这个参数并将其暴露给框架

```java
private static final ConfigDef CONFIG_DEF = new ConfigDef()
    .define(FILE_CONFIG, Type.STRING, Importance.HIGH, "Source filename.")
    .define(TOPIC_CONFIG, Type.STRING, Importance.HIGH, "The topic to publish data to");
 
public ConfigDef config() {
    return CONFIG_DEF;
}
```

`ConfigDef`用于只读配置集合.可以指定名称,类型,默认值,文档,组信息内容等等.此外,可以通过重写`Validator`类进行单独配置.除此之外,value的值可以通过其他配置进行改变.

#### 使用schema进行配置

如果需要创建复杂的数据,可以使用kafka连接器的数据API进行处理.大多数结构化的记录需要使用`Schema`和`Struct`类进行交互.

```java
Schema schema = SchemaBuilder.struct().name(NAME)
    .field("name", Schema.STRING_SCHEMA)
    .field("age", Schema.INT_SCHEMA)
    .field("admin", SchemaBuilder.bool().defaultValue(false).build())
    .build();
 
Struct struct = new Struct(schema)
    .put("name", "Barbara Liskov")
    .put("age", 75);
```

如果实现了一个source的连接器,需要决定何时何地去创建一个schema.可能的话,需要避免重新计算的问题.但是许多连接器有动态的schema.一个简单的示例就是数据库的连接器.需要连接器能够快速地进行改变和响应.

#### Kafka Stream



