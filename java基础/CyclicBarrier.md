#### 介绍

```markdown
同步措施，需要一组线程等待每个都达到屏障点。@CyclicBarrier 在涉及到指定大小的线程组，且需要互相等待的情景下是非常有用的。@CyclicBarrier 支持可配置的@Runnable命令，在组中最后一个线程达到之后才会运行，之前任何线程都不会被释放。这个动作用于更新共享状态.
如果屏障操作执行的时候不依靠被暂停的线程,那么任何组中的线程都可以执行这个动作.为此,使用@await返回内存屏障中线程到达的编号.这样就可以选择到底是哪个线程可以执行内存屏障的动作了.
@CyclicBarrier 使用全部执行/不执行的模型,如果一个线程由于中断,失败或者超时离开了内存屏障的话,其他线程也会非正常的离开内存屏障.
```

#### 属性

```markdown
1.  ReentrantLock lock
用于守卫内存屏障的锁
2. Condition trip
脱离内存屏障的信号
3. int parties
组的数量
4. Runnable barrierCommand
逃脱内存屏障的指令动作
5. int count
组需要等待的数量，向下计数，每次新建@generation或者@generation.broken=true的时候重置
```

#### 常用方法

##### void nextGeneration()

更新逃脱内存屏障时的状态,且唤醒每个线程,只有当持有锁的时候才可以进行
##### void breakBarrier()
打破内存屏障,并唤醒每个线程,只有持有锁的时候才可以调用.
##### int dowait(boolean timed, long nanos)
内存屏障执行代码
```java
private int dowait(boolean timed, long nanos)
        throws InterruptedException, BrokenBarrierException,
               TimeoutException {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            final Generation g = generation;
            // 内存屏障被破坏,抛出异常
            if (g.broken)
                throw new BrokenBarrierException();
            // 线程中断,遵循全做/不做的原则,破坏内存屏障
            if (Thread.interrupted()) {
                breakBarrier();
                throw new InterruptedException();
            }
            // 计数值减小
            int index = --count;
            // 处理脱离内存屏障的动作
            if (index == 0) {  // tripped
                boolean ranAction = false;
                try {
                    final Runnable command = barrierCommand;
                    if (command != null)
                        command.run();
                    ranAction = true;
                    nextGeneration();
                    return 0;
                } finally {
                    if (!ranAction)
                        breakBarrier();
                }
            }

            // 等待脱离内存屏障/内存屏障破坏/中断/超时
            for (;;) {
                try {
                    if (!timed)
                        trip.await();
                    else if (nanos > 0L)
                        nanos = trip.awaitNanos(nanos);
                } catch (InterruptedException ie) {
                    if (g == generation && ! g.broken) {
                        breakBarrier();
                        throw ie;
                    } else {
                        Thread.currentThread().interrupt();
                    }
                }

                if (g.broken)
                    throw new BrokenBarrierException();

                if (g != generation)
                    return index;

                if (timed && nanos <= 0L) {
                    breakBarrier();
                    throw new TimeoutException();
                }
            }
        } finally {
            lock.unlock();
        }
    }
```

##### int await()

等待所有的组调用内存屏障的@await方法

##### int await(long timeout, TimeUnit unit)

等待所有的组调用内存屏障的@await方法,抢占式调度

##### void reset()

重置内存屏障为初始化状态

##### int getNumberWaiting()

获取正在等待内存屏障结束的组数量