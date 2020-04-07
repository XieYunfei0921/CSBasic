**Zookeeper基础教程**

---



+ 教程
+ 内存屏障
+ 生产者消费者队列
+ 示例
  + 队列测试
  + 屏障测试
  + 资源列表

**介绍**

教程中会介绍内存屏障的简单实现和使用zk实现的生产者消费者队列。可以分别叫做**内存屏障**和**队列**。下述示例假定至少有一个zk服务器处于运行状态中。

下面显示代码的引用部分:

```java
// 使用zk,实现对内存屏障的实现

static ZooKeeper zk = null;
static Integer mutex;

String root;

SyncPrimitive(String address) {
    if(zk == null){
        try {
            System.out.println("Starting ZK:");
            zk = new ZooKeeper(address, 3000, this);
            mutex = new Integer(-1);
            System.out.println("Finished starting ZK: " + zk);
        } catch (IOException e) {
            System.out.println(e.toString());
            zk = null;
        }
    }
}

synchronized public void process(WatchedEvent event) {
    synchronized (mutex) {
        mutex.notify();
    }
}
```

这两个类都会基础`SyncPrimitive`,这种方式下执行的步骤对于`SyncPrimitive`的构造器来说是通用的.为了保证实例简单容易理解,在首次初始化一个内存屏障或者队列对象的时候创建一个zk对象,且会声明一个静态变量,用于引用这个对象.

内存屏障中或者队列中后来的实例需要检查zk对象是否存在.这就使用zk对象实现了同步的功能.此外,可以创建zk对象或者传递一个内存屏障或者队列的构造器来获取应用.

使用`process()`方法用于处理由于观测到的事件所触发的消息.一个观测器(代码)是一个内部的数据结构,运行zk提示客户端更改节点.然后设置观察器,并且等待指定代码的修改,这个可能会预测到等待的结束.



#### 内存屏障

内存屏障是一个原语操作，可以使得一组进程同步执行。实现的主要思路就是：

存在一个屏障节点，可以作为进程节点的父节点运行。假定将屏障节点称作`/b1`.每个进程p会创建节点`/b1/p`.一旦创建了足够的进程节点,就会启动计算.

下述示例中,每个进程初始化了一个内存屏障的对象,且构造器需要如下属性:

+ zk服务器的地址(例如:"zoo1.foo.com:2181")
+ 屏障节点的zk路径(例如:`/b1`)
+ 进程组的大小

内存屏障构造器传递zk服务器的地址到父类构造器中,父类在zk实例不存在的时候,创建一个.内存屏障的构造器然后会在zk上创建一个内存屏障节点,这个父节点是所有进程节点的父节点.称作**根节点**(注意不是zk的根节点`/`)

```java
/**
 * Barrier constructor
 *
 * @param address
 * @param root
 * @param size
 */
Barrier(String address, String root, int size) {
    super(address);
    this.root = root;
    this.size = size;
    // Create barrier node
    if (zk != null) {
        try {
            Stat s = zk.exists(root, false);
            if (s == null) {
                zk.create(root, new byte[0], Ids.OPEN_ACL_UNSAFE,
                        CreateMode.PERSISTENT);
            }
        } catch (KeeperException e) {
            System.out
                    .println("Keeper exception when instantiating queue: "
                            + e.toString());
        } catch (InterruptedException e) {
            System.out.println("Interrupted exception");
        }
    }

    // My node name
    try {
        name = new String(InetAddress.getLocalHost().getCanonicalHostName().toString());
    } catch (UnknownHostException e) {
        System.out.println(e.toString());
    }
}
```

为了进入内容屏障,一个进程需要调用`enter()`方法,进程在根节点下创建一个节点代表内存屏障.使用主机名称去构建节点名称.然后等待到有足够的进程进入内存屏障.一个进程会检查根节点下子节点的数量(使用`getChildren()`),或者等待进程不足的提示.

如果根节点发送了改变,会接收到提示,会有一个进程会被设置为监视状态,且会通过`getChildren()`方法调用.在下述代码中,`getChildren()`包含两个参数,第一个是节点的读取形式,第二个是一个标志,这个标志允许这个进程处于监视状态下.

```java
/**
 * Join barrier
 *
 * @return
 * @throws KeeperException
 * @throws InterruptedException
 */

boolean enter() throws KeeperException, InterruptedException{
    zk.create(root + "/" + name, new byte[0], Ids.OPEN_ACL_UNSAFE,
            CreateMode.EPHEMERAL_SEQUENTIAL);
    while (true) {
        synchronized (mutex) {
            List<String> list = zk.getChildren(root, true);

            if (list.size() < size) {
                mutex.wait();
            } else {
                return true;
            }
        }
    }
}
```

注意到`enter()`方法抛出了`KeeperException `和`InterruptedException`,需要这个进程能够处理这些异常.

一旦计算完成,进程会调用`leave()`,去离开内存屏障.

首先删除相应的节点,然后获取根节点的子节点.如果至少一个子节点,就会等待通知信息(如果开启监视的话).根据监视的结果,检查根节点是否有其他子节点.

```java
/**
 * Wait until all reach barrier
 *
 * @return
 * @throws KeeperException
 * @throws InterruptedException
 */

boolean leave() throws KeeperException, InterruptedException {
    zk.delete(root + "/" + name, 0);
    while (true) {
        synchronized (mutex) {
            List<String> list = zk.getChildren(root, true);
                if (list.size() > 0) {
                    mutex.wait();
                } else {
                    return true;
                }
            }
        }
    }
```



#### 生产者消费者队列

生产者消费者队列是一个分布式的数据结构,进程组用于在这个数据结构中生成和消费对象。生产者进程创建新元素且将其添加到队列中。消费者进程会从列表中移除元素，并对其处理。

在下述示例中,元素仅仅是一个整形,队列由根节点代码,并添加元素到队列中,一个生产者进程会创建一个节点(根节点的子节点).

下述代码演示了对应的数据结构,作为内存屏障的对象,首先需要调用父类的构造器进行同步,如果不存在则会创建zk对象,然后验证是否队列的根节点存在,如果不存在则创建.

```java
/**
 * Constructor of producer-consumer queue
 *
 * @param address
 * @param name
 */
Queue(String address, String name) {
    super(address);
    this.root = name;
    // Create ZK node name
    if (zk != null) {
        try {
            Stat s = zk.exists(root, false);
            if (s == null) {
                zk.create(root, new byte[0], Ids.OPEN_ACL_UNSAFE,
                        CreateMode.PERSISTENT);
            }
        } catch (KeeperException e) {
            System.out
                    .println("Keeper exception when instantiating queue: "
                            + e.toString());
        } catch (InterruptedException e) {
            System.out.println("Interrupted exception");
        }
    }
}
```

生产者进程调用`produce()`,添加元素到队列中,传递一个整形的参数.方法使用`create()`创建一个新节点,添加到队列中,使用`SEQUENCE`标记直到zk添加根节点相关的队列计数器.这种方式下,利用队列中元素的排序,确保队列中最老的元素是下一个消费的(LRU).

```java
/**
 * Add element to the queue.
 *
 * @param i
 * @return
 */

boolean produce(int i) throws KeeperException, InterruptedException{
    ByteBuffer b = ByteBuffer.allocate(4);
    byte[] value;

    // Add child with value i
    b.putInt(i);
    value = b.array();
    zk.create(root + "/element", value, Ids.OPEN_ACL_UNSAFE,
                CreateMode.PERSISTENT_SEQUENTIAL);

    return true;
}
```

消费者进程获取根节点的子节点,用于消费元素.且消费者会读取节点的最小计数值,并返回这个元素.注意到如果存在冲突,那么冲突的进程不会删除节点,如果删除节点就会抛出异常.

`getChildren()`调用返回了子节点的列表(按字典排序的).因为字典排序不需要遵守计数值的数字属性,所以需要决定哪个元素是最小的.为了决定哪个计数值是最小的,需要变量整个列表,移除每个的前缀值.

```java
/**
 * Remove first element from the queue.
 *
 * @return
 * @throws KeeperException
 * @throws InterruptedException
 */
int consume() throws KeeperException, InterruptedException{
    int retvalue = -1;
    Stat stat = null;

    // Get the first element available
    while (true) {
        synchronized (mutex) {
            List<String> list = zk.getChildren(root, true);
            if (list.size() == 0) {
                System.out.println("Going to wait");
                mutex.wait();
            } else {
                Integer min = new Integer(list.get(0).substring(7));
                for(String s : list){
                    Integer tempValue = new Integer(s.substring(7));
                    //System.out.println("Temporary value: " + tempValue);
                    if(tempValue < min) min = tempValue;
                }
                System.out.println("Temporary value: " + root + "/element" + min);
                byte[] b = zk.getData(root + "/element" + min,
                            false, stat);
                zk.delete(root + "/element" + min, 0);
                ByteBuffer buffer = ByteBuffer.wrap(b);
                retvalue = buffer.getInt();

                return retvalue;
                }
            }
        }
    }
}
```




### 完整实例

---

运行下述指令

```shell
ZOOBINDIR="[path_to_distro]/bin"
. "$ZOOBINDIR"/zkEnv.sh
java SyncPrimitive [Test Type] [ZK server] [No of elements] [Client type]
```

**生成者消费者队列测试**

+ 启动生产者,创建100个元素

```shell
$ java SyncPrimitive qTest localhost 100 p
```

+ 启动消费者,消费100个元素

```shell
$ java SyncPrimitive qTest localhost 100 c
```

##### 内存屏障测试

其他内存屏障,使用两个参与者

```shell
$ java SyncPrimitive bTest localhost 2
```



#### 源码

---

##### SyncPrimitive.Java

```java
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Random;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.data.Stat;

public class SyncPrimitive implements Watcher {

    static ZooKeeper zk = null;
    static Integer mutex;
    String root;

    SyncPrimitive(String address) {
        if(zk == null){
            try {
                System.out.println("Starting ZK:");
                zk = new ZooKeeper(address, 3000, this);
                mutex = new Integer(-1);
                System.out.println("Finished starting ZK: " + zk);
            } catch (IOException e) {
                System.out.println(e.toString());
                zk = null;
            }
        }
        //else mutex = new Integer(-1);
    }

    synchronized public void process(WatchedEvent event) {
        synchronized (mutex) {
            //System.out.println("Process: " + event.getType());
            mutex.notify();
        }
    }

    /**
     * Barrier
     */
    static public class Barrier extends SyncPrimitive {
        int size;
        String name;

        /**
         * Barrier constructor
         *
         * @param address
         * @param root
         * @param size
         */
        Barrier(String address, String root, int size) {
            super(address);
            this.root = root;
            this.size = size;

            // Create barrier node
            if (zk != null) {
                try {
                    Stat s = zk.exists(root, false);
                    if (s == null) {
                        zk.create(root, new byte[0], Ids.OPEN_ACL_UNSAFE,
                                CreateMode.PERSISTENT);
                    }
                } catch (KeeperException e) {
                    System.out
                            .println("Keeper exception when instantiating queue: "
                                    + e.toString());
                } catch (InterruptedException e) {
                    System.out.println("Interrupted exception");
                }
            }

            // My node name
            try {
                name = new String(InetAddress.getLocalHost().getCanonicalHostName().toString());
            } catch (UnknownHostException e) {
                System.out.println(e.toString());
            }

        }

        /**
         * Join barrier
         *
         * @return
         * @throws KeeperException
         * @throws InterruptedException
         */

        boolean enter() throws KeeperException, InterruptedException{
            zk.create(root + "/" + name, new byte[0], Ids.OPEN_ACL_UNSAFE,
                    CreateMode.EPHEMERAL_SEQUENTIAL);
            while (true) {
                synchronized (mutex) {
                    List<String> list = zk.getChildren(root, true);

                    if (list.size() < size) {
                        mutex.wait();
                    } else {
                        return true;
                    }
                }
            }
        }

        /**
         * Wait until all reach barrier
         *
         * @return
         * @throws KeeperException
         * @throws InterruptedException
         */
        boolean leave() throws KeeperException, InterruptedException{
            zk.delete(root + "/" + name, 0);
            while (true) {
                synchronized (mutex) {
                    List<String> list = zk.getChildren(root, true);
                        if (list.size() > 0) {
                            mutex.wait();
                        } else {
                            return true;
                        }
                    }
                }
            }
        }

    /**
     * Producer-Consumer queue
     */
    static public class Queue extends SyncPrimitive {

        /**
         * Constructor of producer-consumer queue
         *
         * @param address
         * @param name
         */
        Queue(String address, String name) {
            super(address);
            this.root = name;
            // Create ZK node name
            if (zk != null) {
                try {
                    Stat s = zk.exists(root, false);
                    if (s == null) {
                        zk.create(root, new byte[0], Ids.OPEN_ACL_UNSAFE,
                                CreateMode.PERSISTENT);
                    }
                } catch (KeeperException e) {
                    System.out
                            .println("Keeper exception when instantiating queue: "
                                    + e.toString());
                } catch (InterruptedException e) {
                    System.out.println("Interrupted exception");
                }
            }
        }

        /**
         * Add element to the queue.
         *
         * @param i
         * @return
         */

        boolean produce(int i) throws KeeperException, InterruptedException{
            ByteBuffer b = ByteBuffer.allocate(4);
            byte[] value;

            // Add child with value i
            b.putInt(i);
            value = b.array();
            zk.create(root + "/element", value, Ids.OPEN_ACL_UNSAFE,
                        CreateMode.PERSISTENT_SEQUENTIAL);

            return true;
        }

        /**
         * Remove first element from the queue.
         *
         * @return
         * @throws KeeperException
         * @throws InterruptedException
         */
        int consume() throws KeeperException, InterruptedException{
            int retvalue = -1;
            Stat stat = null;

            // Get the first element available
            while (true) {
                synchronized (mutex) {
                    List<String> list = zk.getChildren(root, true);
                    if (list.size() == 0) {
                        System.out.println("Going to wait");
                        mutex.wait();
                    } else {
                        Integer min = new Integer(list.get(0).substring(7));
                        String minNode = list.get(0);
                        for(String s : list){
                            Integer tempValue = new Integer(s.substring(7));
                            //System.out.println("Temporary value: " + tempValue);
                            if(tempValue < min) {
                                min = tempValue;
                                minNode = s;
                            }
                        }
                        System.out.println("Temporary value: " + root + "/" + minNode);
                        byte[] b = zk.getData(root + "/" + minNode,
                        false, stat);
                        zk.delete(root + "/" + minNode, 0);
                        ByteBuffer buffer = ByteBuffer.wrap(b);
                        retvalue = buffer.getInt();

                        return retvalue;
                    }
                }
            }
        }
    }

    public static void main(String args[]) {
        if (args[0].equals("qTest"))
            queueTest(args);
        else
            barrierTest(args);
    }

    public static void queueTest(String args[]) {
        Queue q = new Queue(args[1], "/app1");

        System.out.println("Input: " + args[1]);
        int i;
        Integer max = new Integer(args[2]);

        if (args[3].equals("p")) {
            System.out.println("Producer");
            for (i = 0; i < max; i++)
                try{
                    q.produce(10 + i);
                } catch (KeeperException e){

                } catch (InterruptedException e){

                }
        } else {
            System.out.println("Consumer");

            for (i = 0; i < max; i++) {
                try{
                    int r = q.consume();
                    System.out.println("Item: " + r);
                } catch (KeeperException e){
                    i--;
                } catch (InterruptedException e){
                }
            }
        }
    }

    public static void barrierTest(String args[]) {
        Barrier b = new Barrier(args[1], "/b1", new Integer(args[2]));
        try{
            boolean flag = b.enter();
            System.out.println("Entered barrier: " + args[2]);
            if(!flag) System.out.println("Error when entering the barrier");
        } catch (KeeperException e){
        } catch (InterruptedException e){
        }

        // Generate random integer
        Random rand = new Random();
        int r = rand.nextInt(100);
        // Loop for rand iterations
        for (int i = 0; i < r; i++) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
            }
        }
        try{
            b.leave();
        } catch (KeeperException e){

        } catch (InterruptedException e){

        }
        System.out.println("Left barrier");
    }
}

```

