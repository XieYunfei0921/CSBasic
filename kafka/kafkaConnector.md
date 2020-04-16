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

Kafka Connect currently supports two modes of execution: standalone (single process) and distributed.

In standalone mode all work is performed in a single process. This configuration is simpler to setup and get started with and may be useful in situations where only one worker makes sense (e.g. collecting log files), but it does not benefit from some of the features of Kafka Connect such as fault tolerance. You can start a standalone process with the following command:

##### 配置kafka连接器

##### 转换器

##### REST API

#### 连接器开发指南

##### 核心概念和API

###### 连接器和任务

###### 流和记录

###### 动态连接器

#### 连接器的开发

##### 连接器实例

##### 任务实例（source）

##### sink任务

##### 恢复上一个偏移量

#### 动态输入/输出流

#### 配置连接的校验

#### 使用schema进行配置

#### kafka连接管理器

#### Kafka Stream



