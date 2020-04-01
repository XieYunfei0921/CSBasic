#### ZooKeeper 观察者

**观察者: 在不影响写性能的前提下对zookeeper进行扩展**

---

尽管zookeeper直连到客户端(全体投票成员)上效果比较好,这个结构很难横向扩展客户端.问题是一旦投票的成员增加,那么写性能就会下降.因为写操作需要超过半数节点的同意.因此随着投票者的增加这个开销会增加.

下面介绍一种新的zookeeper节点,叫做**观察者**.这个会提高zookeeper的扩展性.观察者不会参与投票,除此之外观察者表现的像个follower一样,客户端会连接到它们,并且对着它发送读写请求.观察者发送请求到leader,同时也会发送到follower上,但是观察者仅仅会监听投票的结果.正因为如此,如果不希望降低投票性能,需要增加观察者的数量.
观察者也有其他的优势,但是他们不会参与投票,它不是zookeeper的重要组成部分.它们的失败或者从集群中断开连接,不会对zookeeper服务操作降低.

为了对用户起到好处,观察者的网络连接要弱于follower.观察者可以与zookeeper服务器进行交互.客户端的观察者会快速读取,因为读取工作都是本地进行的,且写出结果的网络开销也会减少.

**观察者的使用方法**

创建带有观察者的zookeeper集群很简单,需要对配置文件进行两个改变.首先配置每个节点的配置文件为观察者

```shell
peerType=observer
```

这个配置会告诉zookeeper，这个服务器是一个观察者。其次，在每个服务器的配置文件中，必须添加`:observer`给观察者的定义中:

```shell
server.1:localhost:2181:3181:observer
```

This tells every other server that server.1 is an Observer, and that they
should not expect it to vote. This is all the configuration you need to do
to add an Observer to your ZooKeeper cluster. Now you can connect to it as
though it were an ordinary Follower. Try it out, by running:

这个告知了其他服务器`server.1`这个是一个观察者,且不应当参加投票.这个是你在zookeeper集群的操作.

现在可以连接到服务器.

```shell
$ bin/zkCli.sh -server localhost:2181
```

当`localhost:2181`是观察者的主机+端口号名称的时候,需要使用类型`/s`的指令查询zk服务.

**观察者Master的使用方法**

观察者函数仅仅是一个不会参与投票的成员,和follower共享`Learner `接口.且仅仅是不同的内部pipeline.但都是保持了Leader的`quorum`端口的连接,这个会模仿到所有的目标.

By default, Observers connect to the Leader of the quorum along its
quorum port and this is how they learn of all new proposals on the
ensemble. There are benefits to allowing Observers to connect to the
Followers instead as a means of plugging into the commit stream in place
of connecting to the Leader. It shifts the burden of supporting Observers
off the Leader and allow it to focus on coordinating the commit of writes.
This means better performance when the Leader is under high load,
particularly high network load such as can happen after a leader election
when many Learners need to sync. It reduces the total network connections
maintained on the Leader when there are a high number of observers.
Activating Followers to support Observers allow the overall number of
Observers to scale into the hundreds. On the other end, Observer
availability is improved since it will take shorter time for a high
number of Observers to finish syncing and start serving client traffic.

This feature can be activated by letting all members of the ensemble know
which port will be used by the Followers to listen for Observer
connections. The following entry, when added to the server config file,
will instruct Observers to connect to peers (Leaders and Followers) on
port 2191 and instruct Followers to create an ObserverMaster thread to
listen and serve on that port.

    observerMasterPort=2191
<a name="ch_UseCases"></a>

## Example use cases

Two example use cases for Observers are listed below. In fact, wherever
you wish to scale the number of clients of your ZooKeeper ensemble, or
where you wish to insulate the critical part of an ensemble from the load
of dealing with client requests, Observers are a good architectural
choice.

* As a datacenter bridge: Forming a ZK ensemble between two
  datacenters is a problematic endeavour as the high variance in latency
  between the datacenters could lead to false positive failure detection
  and partitioning. However if the ensemble runs entirely in one
  datacenter, and the second datacenter runs only Observers, partitions
  aren't problematic as the ensemble remains connected. Clients of the
  Observers may still see and issue proposals.
* As a link to a message bus: Some companies have expressed an
  interest in using ZK as a component of a persistent reliable message
  bus. Observers would give a natural integration point for this work: a
  plug-in mechanism could be used to attach the stream of proposals an
  Observer sees to a publish-subscribe system, again without loading the
  core ensemble.
