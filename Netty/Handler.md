#### 功能

处理IO事件或者拦截IO操作. 且在通道流`ChannelPipeline`的情景下发送到下一个处理器中.

#### 类别

本身`ChannelHandler`不支持任何方法(接口).需要选择如下功能

1. [`ChannelInboundHandler`](# ChannelInboundHandler): 通道入站处理器,用于处理入站的IO事件
2. [`ChannelOutBoundHandler`](# ChannelOutBoundHandler): 通道出站处理器,用于处理出站IO事件

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

#### ChannelInboundHandler

`ChannelInboundHandler`通道入站处理器提供了一些回调函数,用于对处理的过程进行控制,允许用户自定义回调逻辑.

```java
// 通道注册到环境时候触发回调
void channelRegistered(ChannelHandlerContext ctx) throws Exception;

// 通道从上下文环境中解除注册的时候触发回调
void channelUnregistered(ChannelHandlerContext ctx) throws Exception;

// 通道激活的时候触发回调
void channelActive(ChannelHandlerContext ctx) throws Exception;

// 通道解除激活状态的时候触发
void channelInactive(ChannelHandlerContext ctx) throws Exception;

// 通道读取的时候触发
void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception;

// 通道读取完毕的时候触发
void channelReadComplete(ChannelHandlerContext ctx) throws Exception;

// 用户事件触发的时候触发
void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception;

// 通道可写性变动得到时候触发
void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception;

// 异常捕获触发
void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception;
```

#### ChannelOutBoundHandler

`ChannelHandler`用于提醒通道输出处理器`ChannelOutBoundHandler`动作.

```java
// 绑定端口时候触发,仅仅触发一次
void bind(
    ChannelHandlerContext ctx, SocketAddress localAddress, ChannelPromise promise) throws Exception;

// 连接操作的时候触发
void connect(
    ChannelHandlerContext ctx, SocketAddress remoteAddress,
    SocketAddress localAddress, ChannelPromise promise) throws Exception;

// 断开连接的时候触发
void disconnect(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception;

// 关闭操作执行的时候触发
void close(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception;

// 从当前时间环@EventLoop 中解除注册的时候触发
void deregister(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception;

// 上下文读取数据的时候触发,拦截器功能
void read(ChannelHandlerContext ctx) throws Exception;

// 触发写操作的时候回调
void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception;

// 刷写操作执行回调
void flush(ChannelHandlerContext ctx) throws Exception;
```

#### SimpleChannelInboundHandler

`SimpleChannelInboundHandler`是`ChannelInboundHandlerAdapter`用于显示地处理特定类型消息的适配器。

```java
public class StringHandler extends SimpleChannelInboundHandler<String>{
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, link String message) throws Exception{
        System.out.println(message);
    }
}
```

这里需要注意到如何构造器中设置`autorelease=true`的情况下，会通过`ReferenceCountUtil`的释放方法对处理过的消息进行释放。如果需要将消息传递到通道流中下一个处理器的时候，需要使用`ReferenceCountUtil`的`Retain`方法进行保留。

#### ChannelInitializer

通道初始化工具，是一种特殊的通道入站处理器，提供一种简单的方法，用于在其注册到事件环`EventLoop`的时候初始化通道。

它的实现通常用于在`Boostrap`的`handler`方法，`ServerBootstrap`的`handler`方法，`ServerBootstrap`的`childHandler`中。用于设置一个通道的通道流。

```java
public class MyChannelInitializer extends ChannelInitializer{
    public void initChannel(Channel channel) {
        channel.pipeline().addLast("myHandler", new MyHandler());
    }
}

ServerBootstrap bootstrap=...;
// 添加通道pipeline到通道中
bootstrap.childHandler(new MyChannelInitializer());
```

