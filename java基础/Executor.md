### Executor

#### 介绍

```markdown
这个对象可以执行提交的@Runnable任务,这个接口提供每个任务的解耦功能.执行器用于正常的使用,而非去创建线程.例如对于一个{@code new Thread(new RunnableTask()).start()}任务,可以使用:
    Executor executor = anExecutor();
    executor.execute(new RunnableTask1());
    executor.execute(new RunnableTask2());
这个接口不需要执行一定是异步的.
举个简单的例子，执行器可以立即运行提交的任务.更典型的来说，任务是执行在一些线程中，而非是调用者线程中。
执行器的实现利用了任务调度方式，任务调度时间的排序方式。下述的执行器，序列化任务的提交到第二个执行器中。
执行器的实现提供在@ExecutorService中，这个是一个扩展的接口，类@ThreadPoolExecutor.提供了扩展的线程池的实现。@Executors类提供了执行器的简便工厂方法。
```

#### void execute(Runnable command)

以异步方式执行给定的指令，这个指令执行在新的线程中。

### ExecutorService

#### 介绍
```markdown
执行器提供了管理任务结束的方法，以及产生定位一个或者多个异步任务进程的异步任务的方法。一个@ExecutorService 可以被关闭,这样会拒绝新的任务.有两个方法来关闭执行器服务.关闭方法会允许之前提交的任务在结束之前执行.当执行@shutdownNow 方法的时候会防止任务并发的启停执行任务.根据结束的情况,执行器没有任务处于激活执行状态时,没有任务会等待执行,这样就没有新的任务会被提交.这样一个没用的@ExecutorService 需要关闭,去允许资源的重新申请.
方法@submit继承了执行器的@execute(Runnable)方法,通过创建并返回一个异步任务,这个异步任务可以放弃执行或者是等待任务完成.
方法@invokeAny以及方法@invokeAll会进行通用的批量执行,执行一个集合的异步任务,且等待至少一个或者是所有任务的完成.
```
#### 常用方法

##### void shutdown()

在提交的任务执行之后顺序关闭,不会接收到新的任务.如果已经关闭就不会有边界效应.这个方法不会等待之前提交任务的执行完成,使用@awaitTermination可以等待其完成.

##### List<Runnable> shutdownNow()
请求关闭所有激活的执行任务,终止等待任务的执行,且返回任务等待执行列表。请求关闭所有激活的执行任务,终止等待任务的执行,且返回任务等待执行列表。不保证最大女郎去停止处理激活的执行任务.
##### boolean isShutdown()
返回执行器是否被关闭
##### boolean isTerminated()
确定是否所有任务已经被完成,只有这个方法在关闭之前不可能为true
##### boolean awaitTermination(long timeout, TimeUnit unit)
在关闭请求之后,阻塞到所有任务执行完毕,有效等待,防止死锁
##### <T> Future<T> submit(Callable<T> task)
提交返回值任务,用于执行,并返回一个异步任务,代表任务的待定结果值.异步任务的@get方法会根据成功执行的情况返回任务的结果值.如果你希望立即阻塞等待任务,可以使用下述的构建方式
	result = exec.submit(aCallable).get();
##### <T> Future<T> submit(Runnable task, T result)
提交一个Runnabl任务,用于执行,并返回一个异步任务,这个异步任务代表着任务.异步任务的@get方法会根据成功完成的情况,返回一个给定的结果值
1.  task 需要提交的任务
2.  result 需要返回的任务
3.  T 结果的类型
4.  返回参数: 代表待定任务执行的异步任务

##### Future<?> submit(Runnable task)
提交一个可以执行的@Runnable 任务,且返回一个异步任务,代表着这个任务,异步任务的@get方法,在成功执行完毕之后会返回null.

##### <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks)
执行给定的任务,返回异步任务列表,持有完成时的状态和结果值.注意到完成的任务可以是正常完成的或者是抛出异常的任务.方法的结果在给定的集合被修改的时候,其结果是不确定的.

##### <T> T invokeAny(Collection<? extends Callable<T>> tasks)
执行给定的任务,返回一个执行完成的结果.执行完成包括正常执行完成和异常完成,如果集合被修改了,那么执行结果具有不确定性.