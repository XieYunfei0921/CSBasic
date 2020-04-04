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

A barrier is a primitive that enables a group of processes to synchronize the
beginning and the end of a computation. The general idea of this implementation
is to have a barrier node that serves the purpose of being a parent for individual
process nodes. Suppose that we call the barrier node "/b1". Each process "p" then
creates a node "/b1/p". Once enough processes have created their corresponding
nodes, joined processes can start the computation.

In this example, each process instantiates a Barrier object, and its constructor takes as parameters:

- the address of a ZooKeeper server (e.g., "zoo1.foo.com:2181")
- the path of the barrier node on ZooKeeper (e.g., "/b1")
- the size of the group of processes

The constructor of Barrier passes the address of the Zookeeper server to the
constructor of the parent class. The parent class creates a ZooKeeper instance if
one does not exist. The constructor of Barrier then creates a
barrier node on ZooKeeper, which is the parent node of all process nodes, and
we call root (**Note:** This is not the ZooKeeper root "/").

```
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

To enter the barrier, a process calls enter(). The process creates a node under
the root to represent it, using its host name to form the node name. It then wait
until enough processes have entered the barrier. A process does it by checking
the number of children the root node has with "getChildren()", and waiting for
notifications in the case it does not have enough. To receive a notification when
there is a change to the root node, a process has to set a watch, and does it
through the call to "getChildren()". In the code, we have that "getChildren()"
has two parameters. The first one states the node to read from, and the second is
a boolean flag that enables the process to set a watch. In the code the flag is true.

```
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

Note that enter() throws both KeeperException and InterruptedException, so it is
the responsibility of the application to catch and handle such exceptions.

Once the computation is finished, a process calls leave() to leave the barrier.
First it deletes its corresponding node, and then it gets the children of the root
node. If there is at least one child, then it waits for a notification (obs: note
that the second parameter of the call to getChildren() is true, meaning that
ZooKeeper has to set a watch on the root node). Upon reception of a notification,
it checks once more whether the root node has any children.

```
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

<a name="sc_producerConsumerQueues"></a>

## Producer-Consumer Queues

A producer-consumer queue is a distributed data structure that groups of processes
use to generate and consume items. Producer processes create new elements and add
them to the queue. Consumer processes remove elements from the list, and process them.
In this implementation, the elements are simple integers. The queue is represented
by a root node, and to add an element to the queue, a producer process creates a new node,
a child of the root node.

The following excerpt of code corresponds to the constructor of the object. As
with Barrier objects, it first calls the constructor of the parent class, SyncPrimitive,
that creates a ZooKeeper object if one doesn't exist. It then verifies if the root
node of the queue exists, and creates if it doesn't.

```
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

A producer process calls "produce()" to add an element to the queue, and passes
an integer as an argument. To add an element to the queue, the method creates a
new node using "create()", and uses the SEQUENCE flag to instruct ZooKeeper to
append the value of the sequencer counter associated to the root node. In this way,
we impose a total order on the elements of the queue, thus guaranteeing that the
oldest element of the queue is the next one consumed.

```
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

To consume an element, a consumer process obtains the children of the root node,
reads the node with smallest counter value, and returns the element. Note that
if there is a conflict, then one of the two contending processes won't be able to
delete the node and the delete operation will throw an exception.

A call to getChildren() returns the list of children in lexicographic order.
As lexicographic order does not necessarily follow the numerical order of the counter
values, we need to decide which element is the smallest. To decide which one has
the smallest counter value, we traverse the list, and remove the prefix "element"
from each one.

```
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

<a name="Complete+example"></a>

## Complete example

In the following section you can find a complete command line application to demonstrate the above mentioned
recipes. Use the following command to run it.

```
ZOOBINDIR="[path_to_distro]/bin"
. "$ZOOBINDIR"/zkEnv.sh
java SyncPrimitive [Test Type] [ZK server] [No of elements] [Client type]
```

<a name="Queue+test"></a>

### Queue test

Start a producer to create 100 elements

```
java SyncPrimitive qTest localhost 100 p

```

Start a consumer to consume 100 elements

```
java SyncPrimitive qTest localhost 100 c

```

<a name="Barrier+test"></a>

### Barrier test

Start a barrier with 2 participants (start as many times as many participants you'd like to enter)

```
java SyncPrimitive bTest localhost 2

```

<a name="sc_sourceListing"></a>

### Source Listing

#### SyncPrimitive.Java

```
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

