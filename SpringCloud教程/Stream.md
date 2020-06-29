### 消息驱动

SpringCloud Stream 是一个用来为微服务应用构建消息驱动能力的框架。通过使用Spring Integration连接消息中间件，从而实现消息驱动。SpringCloud Stream整合了SpringBoot和Spring Integeration，实现了一个轻量级的消息驱动微服务框架。支持RabbitMQ和Kafka两种中间件。

#### 绑定器

Binder绑定器是SpringCloud Stream中的一个重要的组件，这个组件作为SpringBoot与中间件实现直接的桥梁。当中间件实现变化的时候，不需要对中间件进行较大的升级。同时也方便扩展。

通过应用程序向程序暴露Channel通道，使得应用程序不需要考虑不同消息中间件的实现。

#### 发布订阅模式

SpringCloud Stream使用了发布订阅者模式，当一条消息投递到消息中间件之后，通过Topic进行传播,消息的消费者在订阅的主题中收到它，并触发业务逻辑处理。

#### 消费者和消费分区的概念

对于每一个微服务，为了实现高可用以及负载均衡，实际上会部署多个实例。

在多数情况下，当生产者将消息发送给某个具体的微服务的时候，只希望消费一次。为了保证消费一次，需要使用**消费组**的概念。

默认情况下会分配一个匿名的消费者组，但是最好指定其名称

```properties
spring.cloud.stream.bindings.input.group=xxx
```

引入**消费组**之后，可以保证在多个实例的情况下，保证每个组消息只被组内的一个实例消费。但是，不能够确保被组内的哪个具体的实例消费。因此引入**分区**的概念。当生产者将消息数据发送给多个消费者实例的时候，保证拥有共同特征的数据被同一个消费者实例接受并处理。

#### 绑定功能

可以使用注解`@EnableBinding`启动消息驱动的功能。

这个注解是一个复合注解包括如下：

1. `ChannelBindingServiceConfiguration`

   加载消息通道绑定所必须的一些实例

2. `BindingBeansRegistrar`

   在Spring加载Bean的时候调用，用来加载更多的Bean。这些Bean的参数从注解`@EnableBinding`中的value属性获取。例如`@EnableBinding(Sink.class)`这里就会去加载Sink这个类

3. `BindFactoryCondiguration`

   Bean工厂配置，主要用于加载消息中间件的配置。

4. `SpelExpressionConverterConfiguration`

   SpEL表达式转化器配置

**消息通道的绑定**

在接口中可以使用`@Input`或者`@Output`定义消息通道.用于定义绑定消息通道的接口可以使用`@EnableBinding`来指示。

Sink表示通道的输出位置，需要参数，使用`@Input`指示。Source表示通道的输入属性，需要输出参数，使用`@Output`指示。

#### 消息的生产与消费

1. `@StreamListener`

   使用这个注解会注册为消息通道的监听器。当输入通道有消息的时候，会立即触发注解方法的处理逻辑，对消息进行消费。

2. `@ServiceActivator`

   这个注解不具备消息转换的能力，需要通过注解`@Transformer`来对消息的格式进行转换，这样才相当于`@StreamListener`注解

#### 自动化配置

Spring Cloud Stream 通过绑定器SPI的实现，将应用程序逻辑上的输入输出通道连接到物理上的消息中间件。当类路径下有多个绑定器的时候，SpringCloud Stream在为消息做绑定操作的时候，无法判断需要使用哪些绑定器。