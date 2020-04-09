#### 线程类的介绍

```markdown
线程是程序的执行体，JVM允许多个线程同时并发执行。
每个线程都有优先级，高优先级的线程比起低优先级更可能优先执行。新的线程默认情况下优先级等于创建的线程。
当java虚拟机启动的时候，通常有一个main线程，java虚拟机继续执行线程直到下述情况发生:

{@code Runtime}调用了{@code exit}方法,且安全管理器允许了退出操作.
所有没有启动线程的线程都已经死亡,或者调用{@code run}方法的返回或者抛出异常结束.
有两种方式创建执行线程
1. 一种是声明一个{@code Thread}的子类,这个子类重写{@code run}方法.这样一个子类的实例就可以被分配和启动.
2. 创建一个类，用于实现{@code Runnable}接口，这个类需要实现{@code run}方法，创建线程的时候需要传递参数。每个线程都含有一个线程的标识符.可以有多个线程拥有相同的名称,如果没有指定线程的名称,会自动指定名称.
```

#### 线程类的属性

```markdown
@name 线程名称(可重复)
@priority 线程优先级
@daemon 线程启动标记
@target 真正执行的执行体对象
@group 线程所属的线程组
@contextClassLoader 当前线程的上下文类加载器
@inheritedAccessControlContext 继承的控制上下文
@threadLocals 与当前线程相关的本地线程值,这个map由{@code ThreadLocal}维护
@inheritableThreadLocals 与当前线程相关的可继承的本地线程,由{@code InheritableThreadLocal}维护这个map
@stackSize 当前线程请求的栈深度,如果创建者没有指定的话则为0.这个取决于虚拟机如何处理这个数据,部分虚拟机会忽略这个参数
@nativeParkEventPointer 在本地线程结束之后,JVM的私有状态
@tid 线程编号
@threadStatus java线程状态,默认表示为`not yet started`
@parkBlocker 用于并发调用`#java.util.concurrent.locks.LockSupport.park.`
@blocker 阻塞器,线程中断IO操作的位置,如果存在,阻塞器的中断方法需要在设置线程的中断状态之后调用
@blockerLock 阻塞器锁 
@MIN_PRIORITY 最小优先级
@NORM_PRIORITY 默认优先级
@MAX_PRIORITY 最大优先级
```

#### 线程类常用方法

##### void yield()

```markdown
用于给调度器线索,表示当前线程需要让出当前线程的使用权,调度器可以忽略这条线索.
让出行为是一种启发式的行为,用于促进相关线程之间的进行,这个操作会过度使用CPU.它的使用会结合相关的配置文件和
相关的基准(benchmarking),确保达到预期的目标.
使用这个方法的时机很少,对于debug或者是测试时有用的,可以帮助重新显示由于资源竞争导致的bug.设计并发控制的时候也会起到效果.
```

##### void sleep(long millis)

```markdown
睡眠指定mills ms
```

##### void start()

```markdown
调用这个方法启动执行，JVM调用这个线程的{@code run}方法
这个结果是两个线程并发运行,分别是:
1. 当前线程(返回{@code start}调用结果的线程)
2. 其他线程(执行{@code run}方法的线程)
启动线程两次是不合法的,特别地,在线程完全执行完毕的时候不能再次重启.
如果线程已经启动,这是开启的话会抛出@IllegalThreadStateException异常
```

执行逻辑:

```java
public synchronized void start() {
    /**
    线程状态0,表示新建状态,这个方法由main方法或者系统调用
    */
    if (threadStatus != 0)
        throw new IllegalThreadStateException();
    /**
    将当前线程添加到线程组中,线程组中未启动的线程数量-1
    */
    group.add(this);
    boolean started = false;
        try {
            start0();
            started = true;
        } finally {
            try {
                if (!started) {
                    group.threadStartFailed(this);
                }
            } catch (Throwable ignore) {
                /* do nothing. If start0 threw a Throwable then
                  it will be passed up the call stack */
            }
        }
}
```

##### void run()

```markdown
如果使用{@code Runnable}接口，则会调用Runnable接口的{@code run}方法.否则这个方法直接返回.
{@code Thread}的子类需要重写这个方法
```

#####  void exit()

```markdown
系统调用,在退出之前清理线程资源
```

```java
private void exit() {
        if (threadLocals != null && TerminatingThreadLocal.REGISTRY.isPresent()) {
            TerminatingThreadLocal.threadTerminated();
        }
        if (group != null) {
            group.threadTerminated(this);
            group = null;
        }
        /* Aggressively null out all reference fields: see bug 4006245 */
        target = null;
        /* Speed the release of some of these resources */
        threadLocals = null;
        inheritableThreadLocals = null;
        inheritedAccessControlContext = null;
        blocker = null;
        uncaughtExceptionHandler = null;
}
```

##### void stop()

```markdown
强制线程停止
如果安装了安全管理器,这里就会调用{@code checkAccess}方法,会导致当前线程出现异常{@code SecurityException}
如果这个先出不是当前线程,也就是说需要停止其他线程,安全管理器会检查权限{@code checkPermission}这里也会抛出{@code checkPermission}异常
无论如何,线程都会被强制停止,作为异常类型返回一个{@code ThreadDeath}对象.
允许停止一个还没开始的线程,如果线程启动了则会立即停止.
顶层错误处理器会对未被捕捉的异常进行响应，但是不会打印异常原因。除非异常原因是{@code ThreadDeath}才会提示
注意： 这是一非弃用的方法,这个方法是不安全的,使用这种方式停止线程会引发所有监视器解锁.如果这其中有受到锁保护的对象,那么被摧毁的对象对于其他线程可见,很有可能导致结果的随机性.许多对{@code stop}的可以使用简单修改变量,使得线程结束的方式替代.目标线程周期性检测变量值,如果变量表明需要停止的时候就返回.如果目标线程长期等待,可以使用{@code interupt}停止
```

##### void interrupt()

```markdown
除非当前线程自我中断,否则这个操作是一直被允许的,检查权限{@code checkAccess}方法会被调用,可能抛出安全异常.如果线程由于{@code wait}方法处于阻塞状态.可以使用{@code wait(long)}进行有限等待.
或者使用{@code join}或者{@code sleep}方法。如果等待不到则会清除中断标志位，并抛出中断异常。
1. 如果线程因为IO阻塞{@code InterruptibleChannel},这种情况下会关闭通道并设置中断标志位,线程会接收到异常.
2. 如果线程因为选择器的原因阻塞(NIO).那么设置中断标志位,并返回,可能是一个非零值.
3. 其他情况下,将线程的中断标志位置位
```

##### boolean isInterrupted(boolean ClearInterrupted)

```markdown
测试是否线程已经被中断,中断标志位重置会在传递过来的{@code ClearInterrupted}
```

##### void suspend()

```markdown
暂停当前线程
首先,使用{@code checkAccess}方法,检查可能出现的安全异常,如果线程存活,将其暂停,除非其又被重启.
注意: 这个方法被弃用了,因为可能导致死锁.如果目标线程持有保护系统重要资源的锁,在这种情况下进行暂停的时候,没有资源可以获取到资源,除非线程被重新执行.
```

##### void resume()

```markdown
重启暂停的线程
首先使用{@code checkAccess}方法检查可能出现的安全异常,如果线程存活但是被暂停挂起,这个就会重启线程,允许其继续执行.
注意: 这个方法是弃用的,因为与{@code suspend}配合使用的原因
```

##### int activeCount() 

```markdown
返回当前线程组中激活线程的数量
这个返回值由于在内部数据结构中遍历,所以会动态改变,且会收到当前系统线程的影响.这个方法主要用于debug或者监视的目的.
```

##### void join(long millis, int nanos)

```markdown
等待{@code mills}ms和{@code ns}用于线程死亡.
这个实现使用了存活状态下的循环等待.当一个线程结束的时候,使用{@code notifyAll}唤醒线程.
推荐使用{@code wait},{@code notify},{@code notifyAll}.
```

##### void dumpStack()

```markdown
打印标准错误流下的栈追踪信息,使用在debug模式下
```

##### boolean holdsLock(Object obj)

```markdown
当且仅当当且线程持有obj对象的锁的时候返回true.
这个方法用于运行断言程序持有了某个对象的锁:
	assert Thread.holdsLock(obj);
```

