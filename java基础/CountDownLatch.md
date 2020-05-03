#### 介绍

```markdown
同步措施，允许一个或者多个线程等待到一组操作完成。@CountDownLatch 使用给定的计数值初始化，@await方法会阻塞到计数值为0，在这之后会释放。如果你需要重置计数值可以考虑使用@CyclicBarrier@CountDownLatch 是一个通用的同步工具，可以用来实现多种功能，一个@CountDownLatch可以使用计数值进行初始化，所有线程都会调用@await方法等待线程调用@countDown。如果使用计数值N来初始化，就需要等待N个线程的完成， @CountDownLatch 不需要线程调用@countDown 去等待计数值为0.
```

#### 常见方法

##### void await()

使线程等待锁存器计数值降低为0

##### void countDown()

降低锁存器的计数值，如果到达0则释放所有的等待线程。

##### long getCount()

获取当前的计数值
#### Sync类介绍
@CountDownLatch同步控制，使用AQS状态代表计数值