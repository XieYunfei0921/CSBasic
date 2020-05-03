#### 介绍

```markdown
计数信号量，每次获取锁在条件允许的情况下则获取锁。每条释放锁操作添加了一个信号量。但是并没有实际的信号量对象，所以@Semaphore 仅仅保持信号量的计数.信号量经常用于显示线程的数量。例如，下述示例使用信号量控制线程池的数量
在获取实例之前，每个线程必须要获取一个信号量，保证这个实例可以使用。当线程拿取实例完成的时候，将其返回池中，且将信号量返回信号量集中。
以便其他线程可以获取这个实例。注意到当调用@acquire方法的时候没有持有同步锁。信号量将同步封装起来，以便于去限制池的访问权限。信号量初始值为1，至多允许一个信号量的通过，这个可以当做排他锁使用。经常使用的是二进制信号量，可以拥有两个状态：
二进制信号量含有@Lock 属性，可以被非自身线程释放锁（信号量没有持有权的语义）。这个有特殊的作用，例如死锁发现。
这个类的构造器接受一个fairness参数，设置为false的时候，不能保证线程获取信号量的顺序。特别多，当启动@barging的时候，线程调用@acquire 的时候，可以将信号量给等待队列的第一个线程，逻辑上来说，新的线程放置在等待队列头部。当fairness=true的时候，可以保证线程获取信号量按照FIFO方式。注意到可能出现一个线程先调用@acquire方法但是在后边返回。此外@tryAcquire 方法也会有公平调度的选项，但是会获取任意可用的信号量。
总体上来说，信号用于控制资源获取权限，保证线程不会饥饿。当使用信号用于其他类型的同步控制的时候，非公平调度的方式提供了更高的吞吐量。这个类提供了@acquire(int),和@release(int)方法,用于同时处理多个信号量.这些方法比循环效率高.但是不会完成引用排序.例如A线程调用了@acquire(3),B线程调用了@acquire(2),两个信号量都可用,但是不保证B就一定会在A之前运行完毕.
内存一致性:
线程中的动作要优先于调用@release方法
```

#### Sync的实现

信号量的实现，使用AQS状态表示信号量，可以选择公平和非公平两种方式

##### 常用方法

###### int nonfairTryAcquireShared(int acquires)

非公平尝试获取共享锁,返回剩余的状态值

```java
final int nonfairTryAcquireShared(int acquires) {
    for (;;) {
        int available = getState();
        int remaining = available - acquires;
        if (remaining < 0 ||
            compareAndSetState(available, remaining))
            return remaining;
    }
}
```

###### boolean tryReleaseShared(int releases)

尝试释放指定数量的共享锁

```java
final boolean tryReleaseShared(int releases) {
    for (;;) {
        int current = getState();
        int next = current + releases;
        if (next < current) // overflow
            throw new Error("Maximum permit count exceeded");
        if (compareAndSetState(current, next))
            return true;
    }
}
```

###### void reducePermits(int reductions)

减少指定数量的信号量

```java
final void reducePermits(int reductions) {
    for (;;) {
        int current = getState();
        int next = current - reductions;
        if (next > current) // underflow
            throw new Error("Permit count underflow");
        if (compareAndSetState(current, next))
            return;
    }
}
```

###### int drainPermits()

排空信号量,设置当前信号量值为0

```java
final int drainPermits() {
    for (;;) {
        int current = getState();
        if (current == 0 || compareAndSetState(current, 0))
            return current;
    }
}
```

#### 属性

```markdown
1. final Sync sync
同步属性，具有AQS基本特征
```

#### 常用方法

##### public Semaphore(int permits, boolean fair)

创建指定信号量数量的信号量集,并指定是否为公平调度

##### public void acquire()

从信号量集中获取一个信号量,在获取到信号量前都是阻塞的
##### void acquireUninterruptibly()
获取信号量集中的信号量,在获取一个可用信号量之前都是可用的
##### boolean tryAcquire()
获取信号量集中的一个信号量,仅仅在调用是可用的可以返回
##### boolean tryAcquire(long timeout, TimeUnit unit)
获取信号量集中的信号量,如果在给定的等待时间内可用且当前线程没有被中断则返回
##### void release()
释放信号量,将其返回被信号量集中
##### void acquire(int permits)
获取给定数量的信号量,在获取成功之前是阻塞的
##### void acquireUninterruptibly(int permits)
获取给定数量的信号量,在获取成功之前是阻塞的
##### boolean tryAcquire(int permits)
获取指定数量的信号量
##### void release(int permits)
释放给定数量的信号量,将其返还给信号量集
##### int availablePermits()
获取信号量集中的可用信号量数量
##### int drainPermits()
将信号量排空
##### void reducePermits(int reduction)
减少指定数量的信号量