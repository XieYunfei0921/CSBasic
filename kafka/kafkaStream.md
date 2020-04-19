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

1.  下载kaka

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

      

