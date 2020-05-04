#### 介绍

```markdown
一个执行器服务@ExecutorService,使用多个线程池执行提交的任务,正常情况下使用@Executors的工厂方法配置.线程池会导致两个不同的问题,首先当执行大量异步任务的时候会提升执行的效率.由于每个任务的调用开销的减小,当执行任务集合的时候会提供界限并管理资源.每个@ThreadPoolExecutor 同时也维护一些静态参数,例如完成任务的数量.为了可以适用于多数的上下文,这个类提供许多可调节的参数和可扩展的拦截点.但是,程序使用者可以通过@Executors 的工厂方法newCachedThreadPool(这个是无解线程池,且自动进行线程回收).其中方法@newFixedThreadPool 提供了定长的线程池,方法@newSingleThreadExecutor提供了单个后台线程.
核心和最大线程池大小
执行器@ThreadPoolExecutor 会自动的条件线程池的大小,主要是通过调节@corePoolSize 和@maximumPoolSize.当新的任务使用方法@execute(Runnable) 提交的时候.如果线程数量小于@corePoolSize,那么就会创建一个新的线程,用于处理请求,无论此时worker线程是否处于空载状态下.如果线程数量小于@maximumPoolSize,只有当队列满的时候才会创建线程用于处理请求.设置corePoolSize=maximumPoolSize,这样就可以创建一个定长的线程池,通过设置@maximumPoolSize为一个上限值,例如Integer.MAX_VALUE,可以允许线程池处理任意数量的并发任务。
更一般地,可以通过方法@setCorePoolSize和@setMaximumPoolSize 进行动态的设置.
1. 按需求构建
默认情况下,尽管只有当新的任务到达的时候,核心任务才会创建和启动,但是这个可以使用方法@prestartCoreThread或者方法@prestartAllCoreThreads进行重写。如果使用非空队列进行构建，可以对其进行重新启动。
2. 创建新的线程
新线程使用@ThreadFactory 进行创建。如果没有特别地指定,会使用Executors#defaultThreadFactory进行创建.这里线程的优先级都是@NORM_PRIORITY,且没有启动的状态.提供提供不同的@ThreadFactory 可以修改线程组,优先权,启动状态.如果线程工厂类不能够创建线程.执行器会继续,但是不能够执行任何的任务.线程需要控制修改线程.如果工作线程或者是其他线程使用线程池,且没有控制这个权限.那么服务将会降级.配置改变不会按照时序的方式起效.

3. Keep-alive的次数
如果线程池超出了@corePoolSize的数量，超出的线程在空载时间超出指定存活时间的时候会终止。当线程池没有被激活使用的时候会降低资源的消耗。如果线程池之后激活率，新的线程会被构建。这个参数可以通过方法@setKeepAliveTime进行动态的修改。可以使用@Long.MAX_VALUE和参数@TimeUnit#NANOSECONDS 关闭空载线程。默认情况下，keep-alive策略只有当超出@corePoolSize 线程容量的时候才可以使用，但是方法@allowCoreThreadTimeOut 可以用于应用这个超时策略，是由这个@keepAliveTime值不为0即可。

4. 队列
任何阻塞队列可以用户转换和容纳提交的任务，这个队列的使用可以与线程池进行交互
+ 如果运行的线程数量小于@corePoolSize，执行器会添加新的线程。
+ 如果运行的线程实例大于@corePoolSize，执行器会将请求加入队列，而不是添加一个线程
+ 如果请求没有入队列，新的线程之后在到达@maximumPoolSize之后才会创建。这种情况下，任务会被拒绝。

 * 有四种入队策略
 - 直接传送
 默认队列是一个同步队列@SynchronousQueue,可以直接处理任务.这里,如果没有线程可以使用的话,请求任务的请求会立即失败.这样的话,新线程会被构建.这个策略避免了在处理请求集合时,且请求有内部依赖的查找情况.直接查询需要无界的@maximumPoolSizes,从而避免新提交的任务的拒绝情景.当指令到达的速度快于处理的速度的时候会承认无界线程的可能性.
 
 - 无界队列
 使用无界队列会导致新的任务在所有@corePoolSize个线程在忙碌状态情况下处于等待状态.因此,不会创建超过@corePoolSize个线程.在每个任务都独立完成的时候会很有效.
 
 - 有界队列
 有界队列可以防止资源耗尽的情况,这种一般是在有界@maximumPoolSizes情景下发生的情况.但是难以调整和控制.队列大小和最大池大小可以相互进行性能上的交换.使用大队列,小线程池可以最小化CPU使用以及OS资源和上下文切换的开销.但是可能导致低吞吐量的情况.如果任务周期性的阻塞,系统可以调度多个线程.使用小队列需要大线程池的大小,这个会保持CPU处于忙碌状态,但是会导致不必要的调度开销,也会降低系统吞吐量.
 
 5. 拒绝任务
 新任务在执行器已经停止的时候提交执行的话,就会被拒绝.此外,当执行器使用优先的最大线程池大小以及工作队列容量的时候也会发生.默认情况下使用@AbortPolicy策略,拒绝的时候会抛出一个@RejectedExecutionException的运行时异常.使用@CallerRunsPolicy策略时,线程会自己调用@execute,且提供了简单的回调机制,这样会降低任务提交的速度.使用@DiscardPolicy策略会简单的抛弃不能执行的任务使用@DiscardOldestPolicy,如果执行器没有关闭,在工作队列的头部任务会抛弃,然后重新尝试执行可以定义拒绝策略处理器的类@RejectedExecutionHandler,需要注意设计的策略需要工作在特定容量和队列策略下.
 
 6. 拦截点方法
 这个类提供了每个任务执行前后的回调方法.可以用于操作执行环境.例如,重新初始化本地线程变量,收集统计值等.如果拦截点,回调或者阻塞队列方法抛出异常,内部worker线程会失败,立即终止并可能被替换掉.
 
 7. 队列维护
 方法@getQueue() 允许获取工作队列，用于监视和debug。这个方法不建议使用。当大陆队列任务放弃的时候使用@remove或者@purge方法可以用于存储回收。
 
 8. 回收
 一个线程池不在被引用且没有剩余的线程的时候就需要被回收(GC).不需要显式的关闭.可以配置线程池去允许素有线程池中所有未使用的线程最终死亡.注意通过设置keep-alive的时间.
```

#### 线程控制状态

```markdown
线程池主要控制状态,是一个原子型的integer类型,包括两个重要的属性:
	workerCount 线程有效数量
    runState    表示是否处于运行/关闭状态
为了保证成int类型,限制workerCount的值为2^29-1个线程,而不是2^31-1线程.如果在异步任务中存在问题,变量可以转换为@AtomicLong.但是,如果不是需要要求,这个已经足够使用了.参数@workerCount 是允许启动但是不允许停止的worker数量.与存活线程的概念不同.例如,当一个@ThreadFactory 创建线程失败的时候,且存在线程在停止之前仍然进行存储.用户角度的线程池大小就和worker集合大小一致.
允许状态提供了主要的生命周期控制,值为如下:
    RUNNING:  接受新任务,并处理入队任务
    SHUTDOWN: 不接受新任务,但是处理入队任务
    STOP:  不接受新任务,不处理入队任务,且中断处理任务
    TIDYING:  所有任务结束,worker计数器为0,线程会调用@terminated的方法
	TERMINATED: terminated() 调用完成的状态
状态转换表
     RUNNING -> SHUTDOWN
         shutdown()
     (RUNNING or SHUTDOWN) -> STOP
        shutdownNow()
     SHUTDOWN -> TIDYING
        队列和线程池为空
     STOP -> TIDYING
      线程池为空
     TIDYING -> TERMINATED
```

#### 属性与方法

```markdown
1. BlockingQueue<Runnable> workQueue
持有任务的队列，放置worker线程。
2. HashSet<Worker> workers
包含线程池中的所有工作线程，只有持有锁的时候可以获取
3. Condition termination
等待条件
4. int largestPoolSize
最大线程池大小，仅仅在持有锁的情况下可以获取
5. long completedTaskCount
完成任务的计数器，仅仅在工作线程结束的时候可以更新。必须持有锁才能获取
6. RejectedExecutionHandler handler
线程池饱和或者关闭时的处理器 --> 拒绝策略处理器
7. long keepAliveTime
线程空载等待时限.
8. boolean allowCoreThreadTimeOut
是否空载是线程保持存活,为false,则一直存活,否则超过空载等待时限时则会放弃
9. void reject(Runnable command)
对指定命令采取拒绝执行的策略.
10. void onShutdown()
关闭时候的回调函数
```

