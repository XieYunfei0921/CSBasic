#### 线程组介绍

```markdown
线程组是一个线程的集合.此外,一个线程组可以包含其他的线程组.除了初始线程组之外,其余的线程组构成了一棵树.
一个线程允许获取自己线程组的信息,但是不能获取父线程组以及其他线程组的信息.
```

```markdown
这里的锁策略是尝试对树的一层进行加锁.对于子线程组到父线程组.这里限制了锁的数量,这个操作可以限制获取根线程组的操作.这个策略会导致对线程组进行快照,并清理快照,而非在子线程组工作时,持有父线程组的锁.
```

#### 线程组属性

```markdown
@parent 父线程组
@name 线程组名称
@maxPriority 最大优先级
@destroyed 线程组是否被撤销
@daemon 线程组是否启动
@nUnstartedThreads 未启动的线程数量
@nthreads 线程数量
@threads 线程队列
@ngroups 线程组数量
@groups 线程组队列
```

#### 线程组常用方法

##### void add(ThreadGroup g)

```markdown
添加指定的线程组到当前线程组中
```

##### void remove(ThreadGroup g)

```markdown
从当前线程组中移除指定线程组
```

##### void add(Thread t)

```markdown
添加指定线程到当前线程组
```

##### void threadStartFailed(Thread t)

```markdown
提示线程组指定线程t启动失败
```

```java
 void threadStartFailed(Thread t) {
        synchronized(this) {
            remove(t);
            nUnstartedThreads++;
        }
 }
```

##### void threadTerminated(Thread t)

```markdown
提示线程组指定线程执行完毕
```

```java
void threadTerminated(Thread t) {
        synchronized (this) {
            remove(t);
            if (nthreads == 0) {
                notifyAll();
            }
            if (daemon && (nthreads == 0) &&
                (nUnstartedThreads == 0) && (ngroups == 0))
            {
                destroy();
            }
        }
    }
```

##### void remove(Thread t)

```markdown
从当前线程组中移除指定线程
```

##### void list()

```markdown
打印线程组信息到控制台
```

##### void list(PrintStream out, int indent)

```markdown
打印线程组信息到指定输出中
```

