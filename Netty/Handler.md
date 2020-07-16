#### 功能

处理IO事件或者拦截IO操作. 且在通道流`ChannelPipeline`的情景下发送到下一个处理器中.

#### 类别

本身`ChannelHandler`不支持任何方法(接口).需要选择如下功能

1. `ChannelInboundHandler`: 通道入站处理器,用于处理入站的IO事件
2. `ChannelOutBoundHandler`: 通道出站处理器,用于处理出站IO事件

对于此,提供了下列的适配器:

1. `ChannelInboundHandlerAdapter` 处理入站IO事件的适配器
2. `ChannelOutboundHandlerAdapter` 处理出站IO事件的适配器
3. `ChannelDuplexHandler` 双向适配器,出入皆可以处理

#### 介绍

`ChannelHandler`由`ChannelHandlerContext`提供`ChannelHandler`用于连接一个`ChannelHandlerContext`上下文中的实例. 

使用上下文对象, 可以将事件传递到上游/下游, 并可以动态地修改通道流(pipeline).`ChannelHandler`经常需要存储状态信息,建议使用成员变量.

> 注意: 必须要为每个新的通道创建一个新的处理器实例, 避免出现通道的资源争抢情况.

```java
public class DataServerInitializer extends ChannelInitializer{
    @Override
    public void initChannel( Channel channel) {
        channel.pipeline().addLast("handler", new DataServerHandler());
    }
}
```

如果需要创建许多的处理器实例, 可以选择使用`AttributeKey`进行处理

```java
public class DataServerHandler extends SimpleChannelInboundHandler{
    private final AttributeKey<Boolean> auth= AttributeKey.valueOf("auth");
    
    @Override
    public void channelRead(ChannelHandlerContext ctx, Message message) {
    	AttributeKey<Boolean> attr=ctx.attr(auth);
        
        if (message instanceof LoginMessage) {
            authenticate((LoginMessage) o);
            attr.set(true);
        } else if(message instanceof GetDataMessage){
            if (Boolean.TRUE.equals(attr.get())) {
                ctx.writeAndFlush(fetchSecret((GetDataMessage) o));
            } else{
                fail();
            }
        }
    }
}
```

注意到处理器的状态与`ChannelHandlerContext`相连, 所以可以将同一个处理器实例添加到这个环境下的管道中.

```java
public class DataServerInitializer extends ChannelInitializer{
    private static final DataServerHandler SHARED=new DataServerHandler();
    
    @Override
    public void initChannel(Channel channel){
        channel.pipeline().addLast("handler", SHARED);
    }
}
```

#### Shared注解

使用这个注解,就表示只需要创建一个handler实例,就可以将其添加到一个或者多个管道中多次,并且不需要担心竞争的情况.

#### 常用的回调函数

```java
// 当ChannelHandler 添加到上下文环境中触发这个函数
void handlerAdded(ChannelHandlerContext ctx) throws Exception;

// 当ChannelHandler 从上下文环境中移除的时候会触发这个函数
void handlerRemoved(ChannelHandlerContext ctx) throws Exception;

// 如果触发异常的时候会触发这个回调
void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception;
```

