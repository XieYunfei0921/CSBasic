### Future

#### 介绍

```markdown
@Future 代表异步计算的结果,犯法提供异步计算完成的检查,等待异步计算,以及异步计算结果的检索方法.结果可以使用方法@get获取,只有准备完成才能解除阻塞.
调用@cancel方法是会取消异步计算.其他方法适用于确定任务是否正常完成还是被放弃的。一旦计算完成，计算就不能被放弃。如果希望使用@Future 获取可取消性，但是不提供可以的结果可以声明类型@Future<?>并返回null
示例程序
interface ArchiveSearcher { String search(String target); }
  class App {
    ExecutorService executor = ...
    ArchiveSearcher searcher = ...
    void showSearch(String target) throws InterruptedException {
      Callable<String> task = () -> searcher.search(target);
      Future<String> future = executor.submit(task);
      displayOtherThings(); // do other things while searching
      try {
        displayText(future.get()); // use future
      } catch (ExecutionException ex) { cleanup(); return; }
    }
}
@FutureTask 是这个类的实现,且可以被@Executor 执行.例如:

FutureTask<String> future = new FutureTask<>(task);
executor.execute(future);
```

#### 常用方法

##### boolean cancel(boolean mayInterruptIfRunning)

请求放弃执行这个任务,如果任务已经计算完成则会失败.
##### boolean isCancelled()
确定任务是否被放弃
##### boolean isDone()
确定任务是否被完成
##### V get()
等待异步计算的结果
##### V get(long timeout, TimeUnit unit)
有限等待异步计算的结果

### FutureTask

#### 介绍

```markdown
可放弃的异步计算,提供了对@Future的实现.一旦计算完成之后,计算就不能被重启或者是安全了,除非是使用@runAndReset方法这个类可以用于包装@Callable类或者是@Runnable类型，且可以提交到@Executor 上执行除了为单独的类提供服务之外，这个类提供受到保护的功能，可以用在创建自定义任务类的情景下。
```

#### 属性

```markdown
任务运行状态，初始时为NEW，运行状态在方法进行转换，并且方法可以设置一次和放弃执行。在完成计算期间，
状态可以编程COMPLETING临时的值或者是INTERRUPTING。由于value值是唯一的且之后不会被修改，所以采用了lazy的方式写入。可能的状态转换：
NEW -> COMPLETING -> NORMAL
NEW -> COMPLETING -> EXCEPTIONAL
NEW -> CANCELLED
NEW -> INTERRUPTING -> INTERRUPTED

1. Callable<V> callable
底层callable,运行完毕为null

2. Object outcome
返回的结果,不是volatile的,由状态进行读写保护

3. Thread runner
运行callable的线程,运行期间是CAS的

4. WaitNode waiters
等待线程栈
```

##### V report(int s)

返回计算任务的结果,可以是具体指也可以是异常

```java
private V report(int s) throws ExecutionException {
    Object x = outcome;
    if (s == NORMAL)
        return (V)x;
    if (s >= CANCELLED)
        throw new CancellationException();
    throw new ExecutionException((Throwable)x);
}
```

##### public FutureTask(Callable<V> callable)

依赖于给定的@Callable任务,创建异步任务

##### public boolean cancel(boolean mayInterruptIfRunning)

放弃异步任务

```java
public boolean cancel(boolean mayInterruptIfRunning) {
    // 非新建且没有进行CAS状态更新(即初始状态)
    if (!(state == NEW && STATE.compareAndSet
          (this, NEW, mayInterruptIfRunning ? INTERRUPTING : CANCELLED)))
        return false;
    try {    // in case call to interrupt throws exception
        // 中断线程
        if (mayInterruptIfRunning) {
            try {
                Thread t = runner;
                if (t != null)
                    t.interrupt();
            } finally { // final state
                STATE.setRelease(this, INTERRUPTED);
            }
        }
    } finally {
        finishCompletion();
    }
    return true;
}
```

##### protected void done()

内部保护方法,当任务状态迁移到@isDone时进行.默认情况下空实现,子类可以实现这个逻辑,在异步任务结束进行完成后的回调函数.注意可以在实现中查询状态,并且决定是否放弃这个任务.

##### void set(V v)

设置异步计算的结果为指定的值,除非这个异步任务已经被设置结果或者是被放弃执行. 这个方法可以在@run方法中设置

##### void setException(Throwable t)

设置异常,在结果已经设置或者任务被放弃时不会执行

##### void run() 

异步任务执行逻辑

```java
public void run() {
        // 非初始情况,直接返回
        if (state != NEW ||
            !RUNNER.compareAndSet(this, null, Thread.currentThread()))
            return;
        try {
            // 通过callbable进行任务运行
            Callable<V> c = callable;
            if (c != null && state == NEW) {
                V result;
                boolean ran;
                try {
                    result = c.call();
                    ran = true;
                } catch (Throwable ex) {
                    result = null;
                    ran = false;
                    setException(ex);
                }
                if (ran)
                    set(result);
            }
        } finally {
            runner = null;
            int s = state;
            if (s >= INTERRUPTING)
                handlePossibleCancellationInterrupt(s);
        }
    }
```

#####  boolean runAndReset()
在不设置计算结果值的情况下执行计算,且重置异步任务到初始状态.如果遇到异常或者是任务被放弃执行则不能这么做.  这个方法用于多次执行的任务.

```java
protected boolean runAndReset() {
        if (state != NEW ||
            !RUNNER.compareAndSet(this, null, Thread.currentThread()))
            return false;
        boolean ran = false;
        int s = state;
        try {
            Callable<V> c = callable;
            if (c != null && s == NEW) {
                try {
                    c.call(); // don't set result
                    ran = true;
                } catch (Throwable ex) {
                    setException(ex);
                }
            }
        } finally {
            // runner must be non-null until state is settled to
            // prevent concurrent calls to run()
            runner = null;
            // state must be re-read after nulling runner to prevent
            // leaked interrupts
            s = state;
            if (s >= INTERRUPTING)
                handlePossibleCancellationInterrupt(s);
        }
        return ran && s == NEW;
    }
```

##### void finishCompletion()

移除且唤醒所有等待线程,并调用回调函数@done()

```java
private void finishCompletion() {
        // assert state > COMPLETING;
        for (WaitNode q; (q = waiters) != null;) {
            if (WAITERS.weakCompareAndSet(this, q, null)) {
                for (;;) {
                    Thread t = q.thread;
                    if (t != null) {
                        q.thread = null;
                        LockSupport.unpark(t);
                    }
                    WaitNode next = q.next;
                    if (next == null)
                        break;
                    q.next = null; // unlink to help gc
                    q = next;
                }
                break;
            }
        }
        done();
        callable = null;        // to reduce footprint
    }
```

##### int awaitDone(boolean timed, long nanos)

在指定时间内等待任务执行完成或者是放弃任务的执行,用于实时性要求较高的系统

```java
private int awaitDone(boolean timed, long nanos)
        throws InterruptedException {
        long startTime = 0L;    // Special value 0L means not yet parked
        WaitNode q = null;
        boolean queued = false;
        for (;;) {
            int s = state;
            if (s > COMPLETING) {
                if (q != null)
                    q.thread = null;
                return s;
            }
            else if (s == COMPLETING)
                Thread.yield();
            else if (Thread.interrupted()) {
                removeWaiter(q);
                throw new InterruptedException();
            }
            else if (q == null) {
                if (timed && nanos <= 0L)
                    return s;
                q = new WaitNode();
            }
            else if (!queued)
                queued = WAITERS.weakCompareAndSet(this, q.next = waiters, q);
            else if (timed) {
                final long parkNanos;
                if (startTime == 0L) { // first time
                    startTime = System.nanoTime();
                    if (startTime == 0L)
                        startTime = 1L;
                    parkNanos = nanos;
                } else {
                    long elapsed = System.nanoTime() - startTime;
                    if (elapsed >= nanos) {
                        removeWaiter(q);
                        return state;
                    }
                    parkNanos = nanos - elapsed;
                }
                if (state < COMPLETING)
                    LockSupport.parkNanos(this, parkNanos);
            }
            else
                LockSupport.park(this);
        }
    }
```

#### Callable

##### 介绍

```markdown
可以返回结果或者是抛出异常的任务.实现者需要定义@call()方法.类似于Runnable,都是用于被其他线程执行的.但是Runnable是不会返回结果且不会抛出检查出的异常的.@Executors 类包含实用方法,用于将转换其他类型的Callable类
```

