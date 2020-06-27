Spring Cloud Hystrix提供了断路器，线程隔离等一系列的服务保护功能。提供了服务强大的容错能力，具备**服务降级**，**服务熔断**，**线程和信号隔离**，**请求缓存**，**请求合并**，**服务监控**。

#### Hystrix工作流程

Hystrix工作组件

| 组件名称        | 功能                                                         | 备注            |
| --------------- | ------------------------------------------------------------ | --------------- |
| Receiver        | 接受者，用于处理具体的业务逻辑                               |                 |
| Command         | 命令，定义了一个命令对象所具备的一系列操作命令               | execute,do,undo |
| ConcreteCommand | 具体命令的实现，绑定了命令与操作者之间的关系                 |                 |
| Invoker         | 调用者，持有一个命令对象，需要的时候通过命令对象完成具体业务逻辑 |                 |

**断路器功能**

断路器打开，则不会执行服务中的命令，而是会去执行fallBack中的逻辑。

如果断路器关闭，则指向服务中的命令。

**服务降级**

即不去执行服务中的指令，而去执行fallBack中逻辑的分支选择。注意到，在降级服务中，尽量不要依赖于网络。因为当值服务长时间不响应的原因可能就是网络的延迟问题。

当线程池/请求队列/型号量满的时候,新的请求无法执行,则会加入到服务降级中.

> 注意: 在处理写请求或者批处理任务的时候,可以不进行降级处理,只需要通知调用者,即可.

#### 断路器原理

**断路器的结构**

```java
public interface HystrixCircuitBreaker {
    // 每个请求会调用这个方法,去询问是否需要去处理
    // 这个采用的是半开逻辑,可以允许部分请求通过
    public boolean allowRequest();
    
    // 断路器状态,为true表示断路器打开
    public boolean isOpen();
    
    // 在半开状态下成功执行服务之后,会调用这个方法,标记执行成功,主要体现在度量系统中的参量会发生变化
    void markSuccess();
}
```

```java
public void markSuccess() {
    if (circuitOpen.get()) {
        if (circuitOpen.compareAndSet(true, false)) {
            metrics.resetStream();
        }
    }
}
```

**断路器状态确定的方法**

```java
/**
	当前总计请求数量小于容量的时候,表示这时候可以允许新的请求,这时断路器处于关闭状态
	当前错误请求百分比小于容量值的时候,表示可以允许新的请求,断路器关闭
	其他情况,则打开断路器(失败请求过多/并发量过大)
*/
public boolean isOpen() {
    if (circuitOpen.get()) {
        return true;
    }
    HealthCounts health = metrics.getHealthCounts();
    if (health.getTotalRequests() < properties.circuitBreakerRequestVolumeThreshold().get()){
        return false;
    }
    if (health.getErrorPercentage() 
        < properties.circuitBreakerErrorThresholdPercentage().get()) {
        return false;
    } else {
        if (circuitOpen.compareAndSet(false, true)) {
            circuitOpenedOrLastTestedTime.set(System.currentTimeMillis());
            return true;
        } else {
            return true;
        }
    }
}
```

