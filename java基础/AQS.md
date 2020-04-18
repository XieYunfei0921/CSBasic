#### AQS介绍

```markdown
提供阻塞锁的框架实现，且相关的同步器(信号量,事件等).这个可以依赖于FIFO队列.这个类是大多数同步器的基础类.可以使用单个原子性的值代表状态信息.子类必须定义改变状态的protected类型方法.且需要定义状态表名的意义.基于此,这个类的其他方法使用所有队列和阻塞的原理.子类可以维护其他状态属性,但是只能够原子性地更新.子类需要定义为非public类型的内部辅助方法.这个可以用于实现同步属性.
AQS类不会实现任何的同步接口,相反地,定义了一个方法@acquireInterruptibly,可以调用相应的锁和同步器去实现public方法.这个类支持共享锁和排他锁.当工作在排他模式下的时候,不能被其他线程获取锁.共享模式下允许多个线程运行.这个类不了解锁之间的区别,下一个等待线程必须要知道获取的是什么类型的锁.不同模式下的等待线程共享同一个FIFO队列.
通常子类支持一个锁模式的实现,,蚕食都可以使用@ReadWriteLock进行表示.子类只支持排他锁或者是只支持共享锁.这个类定义了一个@ConditionObject 类,可以用于@Condition实现排他锁模式.这个类页提供了对于内部队列的检查,构造和监视的方法.这个类的序列化仅仅存储了底层的原子类型ineteger维护的状态,所以反序列化的时候获取的是一个空的线程队列.典型的子类需要序列化的能力.

 * 使用方法
 是这个类作为基本的同步器,通过检查/修改同步状态,重新定义下述方法
 1. tryAcquire
 2. tryRelease
 3. tryAcquireShared
 4. tryReleaseShared
 5. isHeldExclusively
上述的方法默认情况下会抛出@UnsupportedOperationException异常.这些方法的实现必须是内部线程安全的,且阻塞时间短或者非阻塞.定义这些方法仅仅支持这个类的使用.所有其他的方法定义了final属性,因此不能被独立变量化.
可以从@AbstractOwnableSynchronizer 找到继承的方法，用于追踪持有排他同步器的线程.建议使用这个类,可以开启监视和诊断功能.
用于支持用户找到持有锁的线程.
尽管这个类基于了内部的FIFO队列,但是不会原子性地执行FIFO获取策略.排他同步器的核心使用方式如下

* 获取锁:
while (!tryAcquire(arg)) {
    <em>enqueue thread if it is not already queued</em>;
    <em>possibly block current thread</em>;
}
* 释放锁
if (tryRelease(arg))
	<em>unblock the first queued thread</em>;

共享模式实现相似,但是不涉及到级联信号量
barging: 因为检查是在入队之前进行的,一个新的线程可以跑到其他线程前面并且阻塞.但是如何需要的话,可以定义@tryAcquire,或者@tryAcquireShared 关闭这种抢占模式.大多数公平同步器可以定义@tryAcquire.

默认抢占式的方式的吞吐量和可扩展性都是很高的.默认的抢占策略有
1. 贪心式抢占
2. 拒绝式
3. 护卫包含式抢占
但是不保证公平调度.
这个类提供了高效可扩展的同步器扩展,通过指定范围的使用.这个同步器依靠状态,获取,释放参数,以及内部的FIFO等待队列.
```

#### 属性

```markdown
1. Node head
头结点
2. Node tail
尾节点
3. int state
同步状态
```

#### 常见方法

##### boolean compareAndSetState(int expect, int update)

CAS更新状态

##### Node enq(Node node)

添加节点到队列中

##### Node addWaiter(Node mode)

创建并将当前线程的节点入队

##### void setHead(Node node)

设置队列的头结点

##### void unparkSuccessor(Node node)

唤醒节点的后继者

##### void doReleaseShared()

释放共享锁

```java
for (;;) {
    Node h = head;
    if (h != null && h != tail) {
        int ws = h.waitStatus;
        if (ws == Node.SIGNAL) {
            if (!h.compareAndSetWaitStatus(Node.SIGNAL, 0))
                continue;            // 循环检查状态
            unparkSuccessor(h);
        }
        else if (ws == 0 &&
                 !h.compareAndSetWaitStatus(0, Node.PROPAGATE))
            continue;                // 循环处理失败的CAS操作
    }
    if (h == head)                   // 循环改变头结点
        break;
}
```

##### boolean tryAcquire(int arg)

尝试获取排他锁,这个方法需要访问对象的状态

##### boolean tryAcquire(int arg)

尝试设置状态为排他模式下的状态

##### int tryAcquireShared(int arg)

尝试获取共享模式下的对象状态

##### boolean release(int arg)

在排他模式下进行锁的释放,释放成功返回true

```java
if (tryRelease(arg)) {
    Node h = head;
    if (h != null && h.waitStatus != 0)
        unparkSuccessor(h);
    return true;
}
return false;
```

##### boolean releaseShared(int arg) 

释放共享锁

```java
if (tryReleaseShared(arg)) {
    doReleaseShared();
    return true;
}
return false;
```

##### Thread getFirstQueuedThread() 

返回队列中的第一个节点

```java
return (head == tail) ? null : fullGetFirstQueuedThread();
```

##### Collection<Thread> getExclusiveQueuedThreads()

返回在排他模式下等待的线程集合

```java
ArrayList<Thread> list = new ArrayList<>();
for (Node p = tail; p != null; p = p.prev) {
    if (!p.isShared()) {
        Thread t = p.thread;
        if (t != null)
            list.add(t);
    }
}
return list;
```

##### Collection<Thread> getSharedQueuedThreads()

返回共享模式下等待获取的线程集合

```java
ArrayList<Thread> list = new ArrayList<>();
for (Node p = tail; p != null; p = p.prev) {
    if (p.isShared()) {
        Thread t = p.thread;
        if (t != null)
            list.add(t);
    }
}
return list;
```



#### Node介绍

这个是等待队列中的节点

```markdown
等待队列节点类
等待队列是一个CLH锁队列(Craig, Landin,Hagersten),CLH 锁正常情况下是一个自旋锁,用于阻塞同步器,但是使用同样的基本持有控制线程信息的策略.每个节点的状态属性会追踪线程是否被阻塞.当处理完毕释放的时候,节点就会被通知到.每个队列中的阶段会按照指定的通知方式的监视器进行工作,这个监视器会持有单个等待的线程.
如果是队列的首个元素可以尝试获取线程.但是不一定会成功,还需要给与权限.
对于CLH锁的入队,可以原子性地新建一个tail指针.为了进行出队的操作需要设置一个head指针.
     *      +------+  prev +-----+       +-----+
     * head |      | <---- |     | <---- |     |  tail
     *      +------+       +-----+       +-----+
插入到CLH队列需要单个原子性操作`tail`,相似的,出队操作涉及到更新`head`.`prev`指针主要处理连接的关系.如果节点被移除了,会被重新连接到没有被抛弃的处理器上.使用`next`实现阻塞的原理,每个阶段的线程id保持在本身节点中,所以线程处理器会通知下一个节点唤醒,注意通过遍历下一个连接,
用于决定是哪一个线程.继承者必须避免与新入队的阶段进行竞争.如果处理完成,需要进行`tail`的原子性更新.删除的过程介绍了一些保守的算法,因为必须轮询其他节点的删除情况,可以忽略提示删除节点的关系信息.
CLH队列需要一个伪头结点去启动,但是不会再创建的时候设置,因为如果没有竞争的情况下就会造成浪费.相反,头尾节点在首次竞争的时候被创建.
```

##### 属性

```markdown
1. Node
SHARED = new Node()
指示节点正在等待共享锁
2. Node EXCLUSIVE
指示节点正在等待排他锁
3. CANCELLED =  1
表示线程已经被删除
4. SIGNAL    = -1
表示继承的线程需要被通知
5. CONDITION = -2
表示线程等的条件的通知
6. int waitStatus
等待状态
7. Node prev
上一个节点
8. Node next
下一个节点
9. Thread thread
排除节点的线程,构造时初始化,使用之后为null
```

