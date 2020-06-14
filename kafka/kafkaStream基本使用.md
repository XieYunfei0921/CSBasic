#### 介绍

对于实时应用和微服务来说最简单的实现方式，当输入和输出数据存储到kafka集群的时候，kafka stream是一个客户端，用于构建应用和微服务。可以使用简单的写操作以及部署标准的java/scala应用到客户端侧.

入门程序(WordCount)

```java
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KTable;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.Produced;
import org.apache.kafka.streams.state.KeyValueStore;
 
import java.util.Arrays;
import java.util.Properties;
 
public class WordCountApplication {
 
    public static void main(final String[] args) throws Exception {
        Properties props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "wordcount-application");
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "kafka-broker1:9092");
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass());
 
        StreamsBuilder builder = new StreamsBuilder();
        KStream<String, String> textLines = builder.stream("TextLinesTopic");
        KTable<String, Long> wordCounts = textLines
            .flatMapValues(textLine -> Arrays.asList(textLine.toLowerCase().split("\\W+")))
            .groupBy((key, word) -> word)
            .count(Materialized.<String, Long, KeyValueStore<Bytes, byte[]>>as("counts-store"));
        wordCounts.toStream().to("WordsWithCountsTopic", Produced.with(Serdes.String(), Serdes.Long()));
 
        KafkaStreams streams = new KafkaStreams(builder.build(), props);
        streams.start();
    }
 
}
```

```scala
import java.util.Properties
import java.util.concurrent.TimeUnit
 
import org.apache.kafka.streams.kstream.Materialized
import org.apache.kafka.streams.scala.ImplicitConversions._
import org.apache.kafka.streams.scala._
import org.apache.kafka.streams.scala.kstream._
import org.apache.kafka.streams.{KafkaStreams, StreamsConfig}
 
object WordCountApplication extends App {
  import Serdes._
 
  val props: Properties = {
    val p = new Properties()
    p.put(StreamsConfig.APPLICATION_ID_CONFIG, "wordcount-application")
    p.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "kafka-broker1:9092")
    p
  }
 
  val builder: StreamsBuilder = new StreamsBuilder
  val textLines: KStream[String, String] = builder.stream[String, String]("TextLinesTopic")
  val wordCounts: KTable[String, Long] = textLines
    .flatMapValues(textLine => textLine.toLowerCase.split("\\W+"))
    .groupBy((_, word) => word)
    .count()(Materialized.as("counts-store"))
  wordCounts.toStream.to("WordsWithCountsTopic")
 
  val streams: KafkaStreams = new KafkaStreams(builder.build(), props)
  streams.start()
 
  sys.ShutdownHookThread {
     streams.close(10, TimeUnit.SECONDS)
  }
}
```

#### 示例程序的运行

1.  下载kafka

   ```shell
   > tar -xzf kafka_2.12-2.5.0.tgz
   > cd kafka_2.12-2.5.0
   ```

2. 启动kafka服务器

   ```shell
   > bin/zookeeper-server-start.sh config/zookeeper.properties
   [2013-04-22 15:01:37,495] INFO Reading configuration from: config/zookeeper.properties (org.apache.zookeeper.server.quorum.QuorumPeerConfig)
   ...
   ```

   ```shell
   > bin/kafka-server-start.sh config/server.properties
   [2013-04-22 15:01:47,028] INFO Verifying properties (kafka.utils.VerifiableProperties)
   [2013-04-22 15:01:47,051] INFO Property socket.send.buffer.bytes is overridden to 1048576 (kafka.utils.VerifiableProperties)
   ...
   ```

3. 准备输入的topic且启动生产者

   + 创建一个输入topic

     ```shell
     > bin/kafka-topics.sh --create \
         --bootstrap-server localhost:9092 \
         --replication-factor 1 \
         --partitions 1 \
         --topic streams-plaintext-input
     Created topic "streams-plaintext-input".
     ```

   + 创建一个输出topic

     ```shell
     > bin/kafka-topics.sh --create \
         --bootstrap-server localhost:9092 \
         --replication-factor 1 \
         --partitions 1 \
         --topic streams-wordcount-output \
         --config cleanup.policy=compact
     Created topic "streams-wordcount-output".
     ```

   +  查看创建的topic信息

     ```shell
     > bin/kafka-topics.sh --bootstrap-server localhost:9092 --describe
      
     Topic:streams-wordcount-output  PartitionCount:1    ReplicationFactor:1 Configs:cleanup.policy=compact,segment.bytes=1073741824
         Topic: streams-wordcount-output Partition: 0    Leader: 0   Replicas: 0 Isr: 0
     Topic:streams-plaintext-input   PartitionCount:1    ReplicationFactor:1 Configs:segment.bytes=1073741824
         Topic: streams-plaintext-input  Partition: 0    Leader: 0   Replicas: 0 Isr: 0
     ```

   2. 启动wordCount程序

      ```shell
      > bin/kafka-run-class.sh org.apache.kafka.streams.examples.wordcount.WordCountDemo
      ```


#### 运行demo程序

定义一个wordCount的java程序,名称叫做`WordCountDemo`

```java
public final class WordCountDemo {

    public static final String INPUT_TOPIC = "streams-plaintext-input";
    public static final String OUTPUT_TOPIC = "streams-wordcount-output";

    static Properties getStreamsConfig() {
        final Properties props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "streams-wordcount");
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(StreamsConfig.CACHE_MAX_BYTES_BUFFERING_CONFIG, 0);
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        return props;
    }

    static void createWordCountStream(final StreamsBuilder builder) {
        final KStream<String, String> source = builder.stream(INPUT_TOPIC);
        final KTable<String, Long> counts = source
            .flatMapValues(value -> Arrays.asList(value.toLowerCase(Locale.getDefault()).split(" ")))
            .groupBy((key, value) -> value)
            .count();
        // need to override value serde to Long type
        counts.toStream().to(OUTPUT_TOPIC, Produced.with(Serdes.String(), Serdes.Long()));
    }
    public static void main(final String[] args) {
        final Properties props = getStreamsConfig();
        final StreamsBuilder builder = new StreamsBuilder();
        createWordCountStream(builder);
        final KafkaStreams streams = new KafkaStreams(builder.build(), props);
        final CountDownLatch latch = new CountDownLatch(1);
        // attach shutdown handler to catch control-c
        Runtime.getRuntime().addShutdownHook(
            new Thread("streams-wordcount-shutdown-hook") {
            @Override
            public void run() {
                streams.close();
                latch.countDown();
            }
        });

        try {
            streams.start();
            latch.await();
        } catch (final Throwable e) {
            System.exit(1);
        }
        System.exit(0);
    }
}
```

kafka Stream是客户端用于构建实时应用和微服务的，输入和输出数据存储在kafka集群中。kafka stream会简单地将写和部署标准java/scala应用到客户端侧.使得应用具有良好的扩展性,可塑性,容错性,分布式性能等等.

##### 下载kafka

```shell
> tar -xzf kafka_2.12-2.5.0.tgz
> cd kafka_2.12-2.5.0
```

##### 启动kafka服务器

启动zookeeper

```shell
> bin/zookeeper-server-start.sh config/zookeeper.properties
[2013-04-22 15:01:37,495] INFO Reading configuration from: config/zookeeper.properties (org.apache.zookeeper.server.quorum.QuorumPeerConfig)
...
```

启动kafka服务器

```shell
> bin/kafka-server-start.sh config/server.properties
[2013-04-22 15:01:47,028] INFO Verifying properties (kafka.utils.VerifiableProperties)
[2013-04-22 15:01:47,051] INFO Property socket.send.buffer.bytes is overridden to 1048576 (kafka.utils.VerifiableProperties)
...
```



##### 准备输入topic并启动kafka生产者

创建一个输入topic为 **streams-plaintext-input**,输出topic为**streams-wordcount-output**的kafka stream

```shell
> bin/kafka-topics.sh --create \
    --bootstrap-server localhost:9092 \
    --replication-factor 1 \
    --partitions 1 \
    --topic streams-plaintext-input
Created topic "streams-plaintext-input".
```

```shell
> bin/kafka-topics.sh --create \
    --bootstrap-server localhost:9092 \
    --replication-factor 1 \
    --partitions 1 \
    --topic streams-wordcount-output \
    --config cleanup.policy=compact
Created topic "streams-wordcount-output".
```

查看topic信息

```shell
> bin/kafka-topics.sh --bootstrap-server localhost:9092 --describe
 
Topic:streams-wordcount-output  PartitionCount:1    ReplicationFactor:1 Configs:cleanup.policy=compact,segment.bytes=1073741824
    Topic: streams-wordcount-output Partition: 0    Leader: 0   Replicas: 0 Isr: 0
Topic:streams-plaintext-input   PartitionCount:1    ReplicationFactor:1 Configs:segment.bytes=1073741824
    Topic: streams-plaintext-input  Partition: 0    Leader: 0   Replicas: 0 Isr: 0
```

##### 启动WordCount应用

运行wordCount程序

```shell
> bin/kafka-run-class.sh org.apache.kafka.streams.examples.wordcount.WordCountDemo
```

这个程序会读取上述输入topic的数据,输出到输出topic中

```shell
> bin/kafka-console-producer.sh --bootstrap-server localhost:9092 --topic streams-plaintext-input
```

```shell
> bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 \
    --topic streams-wordcount-output \
    --from-beginning \
    --formatter kafka.tools.DefaultMessageFormatter \
    --property print.key=true \
    --property print.value=true \
    --property key.deserializer=org.apache.kafka.common.serialization.StringDeserializer \
    --property value.deserializer=org.apache.kafka.common.serialization.LongDeserializer
```

##### 数据处理

通过控制台指定输入topic的数据

```shell
> bin/kafka-console-producer.sh --bootstrap-server localhost:9092 --topic streams-plaintext-input
all streams lead to kafka
```

运行消费者程序,观察输出结果

```shell
> bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 \
    --topic streams-wordcount-output \
    --from-beginning \
    --formatter kafka.tools.DefaultMessageFormatter \
    --property print.key=true \
    --property print.value=true \
    --property key.deserializer=org.apache.kafka.common.serialization.StringDeserializer \
    --property value.deserializer=org.apache.kafka.common.serialization.LongDeserializer
 
all     1
streams 1
lead    1
to      1
kafka   1
```

现在继续给入一组数据

```shell
> bin/kafka-console-producer.sh --bootstrap-server localhost:9092 --topic streams-plaintext-input
all streams lead to kafka
hello kafka streams
```

```shell
> bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 \
    --topic streams-wordcount-output \
    --from-beginning \
    --formatter kafka.tools.DefaultMessageFormatter \
    --property print.key=true \
    --property print.value=true \
    --property key.deserializer=org.apache.kafka.common.serialization.StringDeserializer \
    --property value.deserializer=org.apache.kafka.common.serialization.LongDeserializer
 
all     1
streams 1
lead    1
to      1
kafka   1
hello   1
kafka   2
streams 2
```



#### 应用编写教程

##### 创建Maven项目

使用命令行创建流式项目

```shell
mvn archetype:generate \
    -DarchetypeGroupId=org.apache.kafka \
    -DarchetypeArtifactId=streams-quickstart-java \
    -DarchetypeVersion=2.5.0 \
    -DgroupId=streams.examples \
    -DartifactId=streams.examples \
    -Dversion=0.1 \
    -Dpackage=myapps
```

查看项目结构

```shell
> tree streams.examples
streams-quickstart
|-- pom.xml
|-- src
    |-- main
        |-- java
        |   |-- myapps
        |       |-- LineSplit.java
        |       |-- Pipe.java
        |       |-- WordCount.java
        |-- resources
            |-- log4j.properties
```

现在已经在目录下已经存在一些示例了,可以选择将其删除

```shell
> cd streams-quickstart
> rm src/main/java/myapps/*.java
```

##### 第一个流式程序:Pipe

在`/src/main/java/myapps`目录下创建`Pipe.java`文件

```java
package myapps;
 
public class Pipe {
 
    public static void main(String[] args) throws Exception {
 
    }
}
```

流式应用书写的第一步就是创建一个java的属性类`Properties`用于映射`StreamsConfig`的执行参数.需要的参数是`StreamsConfig.BOOTSTRAP_SERVERS_CONFIG`和`StreamsConfig.APPLICATION_ID_CONFIG`.

```java
Properties props = new Properties();
props.put(StreamsConfig.APPLICATION_ID_CONFIG, "streams-pipe");
props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");    
// assuming that the Kafka broker this application is talking to runs on local machine with port 9092
```

此外,自定义其他配置

```java
props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass());
```

然后开始设置kafka的计算逻辑,使用构建器构建

```shell
final StreamsBuilder builder = new StreamsBuilder();
```

使用逻辑构建器指定输入topic

```shell
KStream<String, String> source = builder.stream("streams-plaintext-input");
```

`KStream`是输入topic的连续记录,这里简单的将其写入到另一个topic中

```shell
source.to("streams-pipe-output");
```

可以将两个内容进行级联

```shell
builder.stream("streams-plaintext-input").to("streams-pipe-output");
```

构建`topology`

```java
final Topology topology = builder.build();
System.out.println(topology.describe());
```

编译运行,输出下述信息

```shell
> mvn clean package
> mvn exec:java -Dexec.mainClass=myapps.Pipe
Sub-topologies:
  Sub-topology: 0
    Source: KSTREAM-SOURCE-0000000000(topics: streams-plaintext-input) --> KSTREAM-SINK-0000000001
    Sink: KSTREAM-SINK-0000000001(topic: streams-pipe-output) <-- KSTREAM-SOURCE-0000000000
Global Stores:
  none
```

注意到有一个source和一个sink,箭头符号指示数据的流向.

可以自定义参数构建kafka流

```java
final KafkaStreams streams = new KafkaStreams(topology, props);
```

通过调用`start()`方法可以触发客户端的执行,除非客户端调用`close()`否则执行不会结束.因此使用CountDownLatch制作一个截止点,在截止点中关闭程序

```java
final CountDownLatch latch = new CountDownLatch(1);
// attach shutdown handler to catch control-c
Runtime.getRuntime().addShutdownHook(new Thread("streams-shutdown-hook") {
    @Override
    public void run() {
        streams.close();
        latch.countDown();
    }
});
try {
    streams.start();
    latch.await();
} catch (Throwable e) {
    System.exit(1);
}
System.exit(0);
```

完整代码如下所示:

```java
package myapps;
 
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;
 
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
 
public class Pipe {
 
    public static void main(String[] args) throws Exception {
        Properties props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "streams-pipe");
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass());
 
        final StreamsBuilder builder = new StreamsBuilder();
 
        builder.stream("streams-plaintext-input").to("streams-pipe-output");
 
        final Topology topology = builder.build();
 
        final KafkaStreams streams = new KafkaStreams(topology, props);
        final CountDownLatch latch = new CountDownLatch(1);
 
        // attach shutdown handler to catch control-c
        Runtime.getRuntime().addShutdownHook(new Thread("streams-shutdown-hook") {
            @Override
            public void run() {
                streams.close();
                latch.countDown();
            }
        });
 
        try {
            streams.start();
            latch.await();
        } catch (Throwable e) {
            System.exit(1);
        }
        System.exit(0);
    }
}
```

编译运行这个类

```shell
> mvn clean package
> mvn exec:java -Dexec.mainClass=myapps.Pipe
```

##### 第二个程序 Line Split

指定输入和输出的topic,并指定转换逻辑

```java
KStream<String, String> source = builder.stream("streams-plaintext-input");
source.flatMapValues(value -> Arrays.asList(value.split("\\W+")))
      .to("streams-linesplit-output");
```

使用``System.out.println(topology.describe())``输出结果如下

```shell
> mvn clean package
> mvn exec:java -Dexec.mainClass=myapps.LineSplit
Sub-topologies:
  Sub-topology: 0
    Source: KSTREAM-SOURCE-0000000000(topics: streams-plaintext-input) --> KSTREAM-FLATMAPVALUES-0000000001
    Processor: KSTREAM-FLATMAPVALUES-0000000001(stores: []) --> KSTREAM-SINK-0000000002 <-- KSTREAM-SOURCE-0000000000
    Sink: KSTREAM-SINK-0000000002(topic: streams-linesplit-output) <-- KSTREAM-FLATMAPVALUES-0000000001
  Global Stores:
    none
```

完整代码

```java
package myapps;
 
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.KStream;
 
import java.util.Arrays;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
 
public class LineSplit {
 
    public static void main(String[] args) throws Exception {
        Properties props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "streams-linesplit");
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass());
 
        final StreamsBuilder builder = new StreamsBuilder();
 
        KStream<String, String> source = builder.stream("streams-plaintext-input");
        source.flatMapValues(value -> Arrays.asList(value.split("\\W+")))
              .to("streams-linesplit-output");
 
        final Topology topology = builder.build();
        final KafkaStreams streams = new KafkaStreams(topology, props);
        final CountDownLatch latch = new CountDownLatch(1);
 
        // ... same as Pipe.java above
    }
}
```

##### 第三个程序 WordCount

转换关系

```java
KStream<String, String> source = builder.stream("streams-plaintext-input");
source.flatMapValues(value -> Arrays.asList(value.toLowerCase(Locale.getDefault()).split("\\W+")))
      .groupBy((key, value) -> value)
      .count(Materialized.<String, Long, KeyValueStore<Bytes, byte[]>>as("counts-store"))
      .toStream()
      .to("streams-wordcount-output", Produced.with(Serdes.String(), Serdes.Long()));
```

输出结果

```shell
> mvn clean package
> mvn exec:java -Dexec.mainClass=myapps.WordCount
Sub-topologies:
  Sub-topology: 0
    Source: KSTREAM-SOURCE-0000000000(topics: streams-plaintext-input) --> KSTREAM-FLATMAPVALUES-0000000001
    Processor: KSTREAM-FLATMAPVALUES-0000000001(stores: []) --> KSTREAM-KEY-SELECT-0000000002 <-- KSTREAM-SOURCE-0000000000
    Processor: KSTREAM-KEY-SELECT-0000000002(stores: []) --> KSTREAM-FILTER-0000000005 <-- KSTREAM-FLATMAPVALUES-0000000001
    Processor: KSTREAM-FILTER-0000000005(stores: []) --> KSTREAM-SINK-0000000004 <-- KSTREAM-KEY-SELECT-0000000002
    Sink: KSTREAM-SINK-0000000004(topic: Counts-repartition) <-- KSTREAM-FILTER-0000000005
  Sub-topology: 1
    Source: KSTREAM-SOURCE-0000000006(topics: Counts-repartition) --> KSTREAM-AGGREGATE-0000000003
    Processor: KSTREAM-AGGREGATE-0000000003(stores: [Counts]) --> KTABLE-TOSTREAM-0000000007 <-- KSTREAM-SOURCE-0000000006
    Processor: KTABLE-TOSTREAM-0000000007(stores: []) --> KSTREAM-SINK-0000000008 <-- KSTREAM-AGGREGATE-0000000003
    Sink: KSTREAM-SINK-0000000008(topic: streams-wordcount-output) <-- KTABLE-TOSTREAM-0000000007
Global Stores:
  none
```

完整代码

```java
package myapps;
 
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.KStream;
 
import java.util.Arrays;
import java.util.Locale;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
 
public class WordCount {
 
    public static void main(String[] args) throws Exception {
        Properties props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "streams-wordcount");
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass());
 
        final StreamsBuilder builder = new StreamsBuilder();
 
        KStream<String, String> source = builder.stream("streams-plaintext-input");
        source.flatMapValues(value -> Arrays.asList(value.toLowerCase(Locale.getDefault()).split("\\W+")))
              .groupBy((key, value) -> value)
              .count(Materialized.<String, Long, KeyValueStore<Bytes, byte[]>>as("counts-store"))
              .toStream()
              .to("streams-wordcount-output", Produced.with(Serdes.String(), Serdes.Long()));
 
        final Topology topology = builder.build();
        final KafkaStreams streams = new KafkaStreams(topology, props);
        final CountDownLatch latch = new CountDownLatch(1);
 
        // ... same as Pipe.java above
    }
}
```