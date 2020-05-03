#### Lock

##### 介绍

```markdown
这个锁实现了排他锁的功能，可以使用sychronized关键字获取。但是允许跟腱灵活的构造，可以有不同的属性。锁是控制多个线程共享资源的工具，通常来说，一个锁提供了共享资源的排他获取方式。同一时间只有一个线程可以获取锁，且共享资源的获取需要首先获取锁。但是，一些锁可以运行并发方法共享资源，例如读取锁中的读写锁@ReadWriteLocksychronized方法提供了每个对象的监视锁，但是这个关键字是一个非公平锁。而Locksychronized当大使得监视程序更加方便，可以帮助避免涉及到锁操作的错误。例如，可以使用算法并发变量数据结构。这个锁可以在不同的作用域内进行锁的获取和释放，且运行多个锁按照任意顺序获取和释放。

当在不同作用域进行加锁和释放锁的操作的时候，需要注意保证代码在执行的时候，受到try-catc的保护。锁实现过程中提供了对synchronized的额外功能，主要是对非阻塞地申请锁。且获取的锁是可以中断的@lockInterruptibly，且可以设置时限@tryLock(long, TimeUnit) .Lock类与普通的锁的区别就是可以保证有序性，非重入性，且可以发现死锁。注意锁实例仅仅是不同的对象，且可以使用synchronized关键字修饰。Lock锁对象的获取与方法的锁定没有特定的关系。

内存同步:
Lock锁的实现必须保证内存同步，失败的加锁或者解锁操作以及重入锁不需要内存同步的效果
关于内存同步的语义描述:
<https://docs.oracle.com/javase/specs/jls/se8/html/jls-17.html#jls-17.4>
```

##### 常用方法

###### void lock()
获取锁:如果锁在当前线程不可以使用,那么当前线在程调度过程中不可以使用.Lock的实现可以发现锁的错误使用，例如可能导致死锁的操作，且会在这种调用者抛出异常
###### void lockInterruptibly()
获取一个lock锁，除非当前线程被中断.立即获取一个锁，如果锁不可用，那么当前线程调度的时候也不可用
###### boolean tryLock(long time, TimeUnit unit)
在规定的时间内获取锁，且当前线程没有被中断
###### void unlock()
释放锁,Lock会利用可以释放锁线程的限制，如果这个限制不满足则会抛出异常。

###### Condition newCondition()
返回一个新的Condition实例，可以用于对Lock实例进行限制。等待lock锁的过程，所必须被当前线程持有。@Condition#await() 的调用会在等待和重新获取锁之前原子性地释放锁

#### ReadWriteLock

##### 介绍

```markdown
读写锁维护了一对Lock锁，且一个是只读的一个是用于写的锁。读取锁可以被多个读取线程同时持有，只要没有写出线程。写出线程是排他的。
所有读取锁的实现必须保证写锁的内存同步的情况。一个线程成功的获取读取锁，可以观察到之前释放的写出锁的更新情况。读写锁运行获取共享数据的并发，这个可以允许互斥量的排他锁。这里假设同一时间只有一个线程可以修改共享数据。在大多数情况下，可以允许任意数量的线程并发读取数据。
理论上，读写锁并发量的增加会增加排他锁的使用。无论读写锁是否会增加排他锁，这个排他锁的使用频率主要数据CAS操作的频率以及读写操作的频率。

尽管读写锁的基本操作是很直接的，但是需要保证需要实现的策略，所以可能影响读写锁的操作。
包括：
写出线程释放写出锁的时候，需要决定是否授权读写锁，什么时候读取/写出线程需要等待。写出性能很普通，因为写出操作是短且低效的。
读取操作会引起写出操作的延迟。
决定在读取器激活状态，且写出器处于等待状态下，读取器是否会申请读取锁。读取操作会对写出操作不确定的延迟。写出器较多的话会降低并发度。
决定锁是否是可重入的，是否持有写出锁的线程可以重新获取锁。是否持有写出锁的线程可以持有读取锁。是否读取锁是可重入的。是否写出锁可以降级为读取锁。是否读取锁可以升级为写出锁。
```

```java
public interface ReadWriteLock {
    /**
     * Returns the lock used for reading.
     *
     * @return the lock used for reading
     */
    Lock readLock();

    /**
     * Returns the lock used for writing.
     *
     * @return the lock used for writing
     */
    Lock writeLock();
}
```

#### ReentrantLock

##### 介绍

```markdown
可重入的互斥锁门带有基本的synchronized行为语义。但是可以继承其他功能。一个重入锁@ReentrantLock 可以被上次获取锁，且没有释放锁的线程持有。当锁没有被其他线程持有的时候，一个调用@lock的线程可以成功的获取锁。在当前线程已经持有锁的时候方法会立即返回。

这个类的构造器会接受一个@fairness参数。当设置为true的时候，锁支持线程的长期等待。否则这个锁不能够保证指定的获取顺序。程序使用多线程的公平锁，可以比默认设置的吞吐量较低。但是，这个锁的公平性不能够保证线程调度的公平性，因此多个线程中的一个线程可以在其他线程正在处理，但是没有获取锁的时候，可以获取多次锁。注意到没有时间设置的@tryLock方法不会设置公平调度属性，如果锁可用就会成功。

除了Lock接口之外，这个类定义了大量public和protected的方法，用于检查锁的状态。 * 这个锁支持同一个线程最大2147483647个锁。超出这个数量则会引发错误.
```

##### 属性

```markdown
1. Sync sync
提供所有实现原理的同步器
```

##### 常见方法

###### int getHoldCount()

查询当前线程锁持有的数量.
一个线程可以对每个锁动作持有锁,这个锁动作没有匹配与相应的解锁操作.持有数量信息用于测试或者debug.例如,代码指定的部分不会使用锁进入.
###### boolean hasQueuedThreads()
查询线程是否在等待获取这个锁,注意由于取消操作任何情况下都可以发送.返回true不能表示线程没有持有过锁,这个操作主要是用于监视系统状态
###### boolean hasQueuedThread(Thread thread)
查询指定线程是否获取了这个锁,这个主要是用于监视系统状态.
###### int getQueueLength()
返回等待线程数量的估计值,这个值是一个估计值,因为线程数量可以动态的改变.这个方法用于监控系统状态,不是用于同步控制.
###### Collection<Thread> getQueuedThreads()
返回包含等待获取锁的线程集合,因为线程集合会动态变化,所以这个是一个估计值,这个方法的子类可以提供扩展的监视处理方案.
##### 内部类

###### Sync

**介绍**

  当前锁同步控制的基础类,子类为公平和非公平控制,使用AQS状态去代码这个锁的持有数量

**常用方法**

boolean nonfairTryAcquire(int acquires)
非公平的尝试获取锁,这个方法由子类实现.
输入参数:
@acquires 申请锁的数量

```java
@ReservedStackAccess
        final boolean nonfairTryAcquire(int acquires) {
            final Thread current = Thread.currentThread();
            int c = getState();
            // 更新同步状态的值
            if (c == 0) {
                if (compareAndSetState(0, acquires)) {
                    setExclusiveOwnerThread(current);
                    return true;
                }
            }
            else if (current == getExclusiveOwnerThread()) {
                // 更新持有锁的数量(重入的次数)
                int nextc = c + acquires;
                if (nextc < 0) // overflow
                    throw new Error("Maximum lock count exceeded");
                setState(nextc);
                return true;
            }
            return false;
        }
```

boolean tryRelease(int releases)

尝试是否指定数量的锁,信号量到0,则返回true,否则返回false(仍然持有锁)
```java
@ReservedStackAccess
   protected final boolean tryRelease(int releases) {
       int c = getState() - releases;
       if (Thread.currentThread() != getExclusiveOwnerThread())
           throw new IllegalMonitorStateException();
       boolean free = false;
       if (c == 0) {
           free = true;
           setExclusiveOwnerThread(null);
       }
       setState(c);
       return free;
   }
```

boolean isHeldExclusively()
检查是否为排他锁

Thread getOwner()
获取锁的持有者

int getHoldCount()
获取锁的持有数量

void readObject(java.io.ObjectInputStream s)
反序列化

###### NonfairSync

非公平锁同步对象

###### FairSync

公平锁同步对象

```java
// 平锁版本的锁获取函数,除非递归调用或者没有等待者,否则不会授权
@ReservedStackAccess
protected final boolean tryAcquire(int acquires) {
    final Thread current = Thread.currentThread();
    int c = getState();
    if (c == 0) {
        if (!hasQueuedPredecessors() &&
            compareAndSetState(0, acquires)) {
            setExclusiveOwnerThread(current);
            return true;
        }
    }
    else if (current == getExclusiveOwnerThread()) {
        int nextc = c + acquires;
        if (nextc < 0)
            throw new Error("Maximum lock count exceeded");
        setState(nextc);
        return true;
    }
    return false;
}
```



#### ReentrantReadWriteLock

##### 介绍

```markdown
重入读写锁
 含有如下属性:
Acquisition order
这个类不会使用读取器和写出器引用.但是支持公平锁的特性.

非公平模式
当构建了一个非公平重入锁的时候(默认),读写锁的条目顺序不能够确定.非公平锁可能延迟一个或者多个读写线程.但是
正常情况下的性能要高于公平锁.

公平模式
当使用公平模式的时候,线程使用合适的到达排序策略进行处理.当持有的锁释放的时候,单个写出线程最长等的时间会分配给写出锁.如果一组读取线程等的的时间不长于写出线程,这个组会被设置为读取锁.
线程尝试获取公平读取锁(非重入)的请求会在写出锁持有或者存在有写出线程的时候被阻塞.除非当前写出线程获取并且是否写出锁的时候才可以获取读取锁.当然如果等待的写出线程放弃等待的时候,会使得多个读取线程处于最长等待状态.线程尝试获取公平写出锁,这种情况下会阻塞读取锁和写出锁.

 Reentrancy(重入)
 这个锁允许读取器和写出器重新申请读取或者写出锁,以重入锁的形式.非重入的读取器除非再写出锁释放的情况下,否则不能使用.此外,写出器可以获取读取锁.在其他应用中,重入性是很有用的.如果读取器尝试获取写出锁,则会一直失败.

锁降级
重入锁允许写出锁降级到读取锁,主要是通过获取写出锁,然后是否写出锁,获取读取锁.读取锁升级到写出锁是不可能的.

锁获取的中断
读写锁都支持锁获取节点的中断,写出锁支持Condition而读取锁不支持Condition,否则会抛出异常
```

##### 属性

```markdown
ReentrantReadWriteLock.ReadLock readerLock
ReentrantReadWriteLock.WriteLock writerLock
Sync sync
```

##### 常用方法

###### int getReadLockCount()

查询当前锁的读取锁的数量,这个方法用于监控系统状态
###### boolean isWriteLocked()
检查当前锁是否为一个线程的写出锁,这个方法用于监控,不能用于同步控制
###### int getReadHoldCount()
查询当前锁持有的读取锁的数量
###### Collection<Thread> getQueuedReaderThreads()
返回处于等待状态下的读取锁,因为实际的线程是动态变化的,所以主要用于对锁的监控处理.


