### zookeeper内部系统

#### 原子广播

zk的心跳信息是一个原子的消息系统,这个消息系统保持了所有服务器处于同步状态.

##### 保证,配置和定义

The specific guarantees provided by the messaging system used by ZooKeeper are the following

zk的消息系统提供特殊的保证:

+ 可靠的传输

  如果消息`m`使用一个服务器传输,消息`m`会被传递到所有服务器

+  全局有序

  如果消息`a`在消息`b`之前被发送,那么消息`a`的顺序一直会在消息`b`之前.

+ 发送的有序性

  如果消息b在消息a之后由发送器b发送出去.那么消息a一定在消息b之前.如果发送器在发送b之后发送c,那么c必须在b之后

zk消息系统需要是高效的,可信的,且易于实现和维护.需要对消息系统能够每秒处理大量数据.尽管需要k+1台服务器发送新的消息,必须可以从失败中恢复过来.

实现系统的时候,需要很少的一段时间和少量的系统资源.所以需要设置系统设计者可以获取得到的协议.

协议假定可以构造服务器之间点对点的FIFO通道.相似的服务通常假定消息传输时可以丢失和重排序的,假定FIFO通道类似TCP.需要TCP的下述属性:

+ 排序传输

  数据传输时的顺序与数据传入的顺序一致.

+ 关闭之后没有消息

  一旦FIFO通道关闭,就不能再接收到消息

FLP证明了如果在异步的分布式系统中发送了失败,不能够保证数据的一致性.为了保证数据的一致性,使用了传输延时参数.但是依靠生存时间并不是为了其能够执行成功.因此如果延时参数不能正常工作(时钟不能正常工作),消息系统会挂起.

描述zk消息协议的时候,数据包,传输方案和消息的定义:

+ 数据包

  通过FIFO通道的字节列表

+  基本单位(Proposal)

  一致性参数(Agreement)的基本单位,这个参数主要是通过交换zk服务器的quorum完成.大多数proposal中包含消息,但是新leader的proposal是不包含消息的典型例子.

+ 消息

  原子广播到zk服务器中的字节列表.消息放置到proposal中,且传递是通过Agreement交换

zk可以保证消息的全局顺序,也能够保证proposal的全局顺序.zk使用zk事务id(zxid)暴露全局顺序.所有proposal会使用zxid标记.当quorum识别到这个proposal的时候会发送到所有zk服务器上且提交.

这个识别意味着服务器记录proposal到持久化存储器上.且quorum中一对quorum至少有一个是可以使用的.保证至少(n/2+1)个quorum处于运行状态.

zxid中包含两个部分,**界限点**和**计数器**.在实现中zxid是一个64位的数.保证高32位是**界线点**,低32位是**计数器**.因此zxid可以代表元组(epoch,count)的信息.**界限点**参数代表了选举关系的变化,每次新的leader形成的时候,就会拥有自己的**界线点**(epoch)参数.因此使用简单的算法,去指定唯一的zxid:

leader简单的增加zxid,用于获取每个proposal的位置zxid.**选举激活**会保证仅仅leader会使用指定的**界线点**,所以简单算法可以保证全局唯一的zxid.

zk消息包含两个部分:

+ leader激活

  leader完成系统的正确状态,并准备开始发送proposal

+ 消息激活

  leader接受消息,用于对于消息发送做出指导

zk是一个整体性的协议,不会在意个别的proposal.而是将proposal流看做一个整体.严格的顺序保证可以高效的实现,且大幅度的简化协议leader激活可以表达出全局性协议的概念.当follower和leader进行同步之后,leader就激活了/且持有相同的状态.这个状态包含所有的proposal,leader相信这些proposal已经提交,且需要跟随leader.

#### leader激活

Leader activation includes leader election (`FastLeaderElection`).
ZooKeeper messaging doesn't care about the exact method of electing a leader as long as the following holds:

* The leader has seen the highest zxid of all the followers.
* A quorum of servers have committed to following the leader.

Of these two requirements only the first, the highest zxid among the followers
needs to hold for correct operation. The second requirement, a quorum of followers,
just needs to hold with high probability. We are going to recheck the second requirement,
so if a failure happens during or after the leader election and quorum is lost,
we will recover by abandoning leader activation and running another election.

After leader election a single server will be designated as a leader and start
waiting for followers to connect. The rest of the servers will try to connect to
the leader. The leader will sync up with the followers by sending any proposals they
are missing, or if a follower is missing too many proposals, it will send a full
snapshot of the state to the follower.

There is a corner case in which a follower that has proposals, `U`, not seen
by a leader arrives. Proposals are seen in order, so the proposals of `U` will have a zxids
higher than zxids seen by the leader. The follower must have arrived after the
leader election, otherwise the follower would have been elected leader given that
it has seen a higher zxid. Since committed proposals must be seen by a quorum of
servers, and a quorum of servers that elected the leader did not see `U`, the proposals
of `U` have not been committed, so they can be discarded. When the follower connects
to the leader, the leader will tell the follower to discard `U`.

A new leader establishes a zxid to start using for new proposals by getting the
epoch, e, of the highest zxid it has seen and setting the next zxid to use to be
(e+1, 0), after the leader syncs with a follower, it will propose a NEW_LEADER
proposal. Once the NEW_LEADER proposal has been committed, the leader will activate
and start receiving and issuing proposals.

It all sounds complicated but here are the basic rules of operation during leader
activation:

* A follower will ACK the NEW_LEADER proposal after it has synced with the leader.
* A follower will only ACK a NEW_LEADER proposal with a given zxid from a single server.
* A new leader will COMMIT the NEW_LEADER proposal when a quorum of followers has ACKed it.
* A follower will commit any state it received from the leader when the NEW_LEADER proposal is COMMIT.
* A new leader will not accept new proposals until the NEW_LEADER proposal has been COMMITTED.

If leader election terminates erroneously, we don't have a problem since the
NEW_LEADER proposal will not be committed since the leader will not have quorum.
When this happens, the leader and any remaining followers will timeout and go back
to leader election.

<a name="sc_activeMessaging"></a>

### Active Messaging

Leader Activation does all the heavy lifting. Once the leader is coronated he can
start blasting out proposals. As long as he remains the leader no other leader can
emerge since no other leader will be able to get a quorum of followers. If a new
leader does emerge,
it means that the leader has lost quorum, and the new leader will clean up any
mess left over during her leadership activation.

ZooKeeper messaging operates similar to a classic two-phase commit.

![Two phase commit](images/2pc.jpg)

All communication channels are FIFO, so everything is done in order. Specifically
the following operating constraints are observed:

* The leader sends proposals to all followers using
  the same order. Moreover, this order follows the order in which requests have been
  received. Because we use FIFO channels this means that followers also receive proposals in order.
* Followers process messages in the order they are received. This
  means that messages will be ACKed in order and the leader will receive ACKs from
  followers in order, due to the FIFO channels. It also means that if message `m`
  has been written to non-volatile storage, all messages that were proposed before
  `m` have been written to non-volatile storage.
* The leader will issue a COMMIT to all followers as soon as a
  quorum of followers have ACKed a message. Since messages are ACKed in order,
  COMMITs will be sent by the leader as received by the followers in order.
* COMMITs are processed in order. Followers deliver a proposal
  message when that proposal is committed.

<a name="sc_summary"></a>

### Summary

So there you go. Why does it work? Specifically, why does a set of proposals
believed by a new leader always contain any proposal that has actually been committed?
First, all proposals have a unique zxid, so unlike other protocols, we never have
to worry about two different values being proposed for the same zxid; followers
(a leader is also a follower) see and record proposals in order; proposals are
committed in order; there is only one active leader at a time since followers only
follow a single leader at a time; a new leader has seen all committed proposals
from the previous epoch since it has seen the highest zxid from a quorum of servers;
any uncommitted proposals from a previous epoch seen by a new leader will be committed
by that leader before it becomes active.

<a name="sc_comparisons"></a>

### Comparisons

Isn't this just Multi-Paxos? No, Multi-Paxos requires some way of assuring that
there is only a single coordinator. We do not count on such assurances. Instead
we use the leader activation to recover from leadership change or old leaders
believing they are still active.

Isn't this just Paxos? Your active messaging phase looks just like phase 2 of Paxos?
Actually, to us active messaging looks just like 2 phase commit without the need to
handle aborts. Active messaging is different from both in the sense that it has
cross proposal ordering requirements. If we do not maintain strict FIFO ordering of
all packets, it all falls apart. Also, our leader activation phase is different from
both of them. In particular, our use of epochs allows us to skip blocks of uncommitted
proposals and to not worry about duplicate proposals for a given zxid.

<a name="sc_consistency"></a>


## Consistency Guarantees

The [consistency](https://jepsen.io/consistency) guarantees of ZooKeeper lie between sequential consistency and linearizability. In this section, we explain the exact consistency guarantees that ZooKeeper provides.

Write operations in ZooKeeper are *linearizable*. In other words, each `write` will appear to take effect atomically at some point between when the client issues the request and receives the corresponding response. This means that the writes performed by all the clients in ZooKeeper can be totally ordered in such a way that respects the real-time ordering of these writes. However, merely stating that write operations are linearizable is meaningless unless we also talk about read operations.

Read operations in ZooKeeper are *not linearizable* since they can return potentially stale data. This is because a `read` in ZooKeeper is not a quorum operation and a server will respond immediately to a client that is performing a `read`. ZooKeeper does this because it prioritizes performance over consistency for the read use case. However, reads in ZooKeeper are *sequentially consistent*, because `read` operations will appear to take effect in some sequential order that furthermore respects the order of each client's operations. A common pattern to work around this is to issue a `sync` before issuing a `read`. This too does **not** strictly guarantee up-to-date data because `sync` is [not currently a quorum operation](https://issues.apache.org/jira/browse/ZOOKEEPER-1675). To illustrate, consider a scenario where two servers simultaneously think they are the leader, something that could occur if the TCP connection timeout is smaller than `syncLimit * tickTime`. Note that this is [unlikely](https://www.amazon.com/ZooKeeper-Distributed-Coordination-Flavio-Junqueira/dp/1449361307) to occur in practice, but should be kept in mind nevertheless when discussing strict theoretical guarantees. Under this scenario, it is possible that the `sync` is served by the “leader” with stale data, thereby allowing the following `read` to be stale as well. The stronger guarantee of linearizability is provided if an actual quorum operation (e.g., a `write`) is performed before a `read`.

Overall, the consistency guarantees of ZooKeeper are formally captured by the notion of [ordered sequential consistency](http://webee.technion.ac.il/people/idish/ftp/OSC-IPL17.pdf) or `OSC(U)` to be exact, which lies between sequential consistency and linearizability.

<a name="sc_quorum"></a>

## Quorums

Atomic broadcast and leader election use the notion of quorum to guarantee a consistent
view of the system. By default, ZooKeeper uses majority quorums, which means that every
voting that happens in one of these protocols requires a majority to vote on. One example is
acknowledging a leader proposal: the leader can only commit once it receives an
acknowledgement from a quorum of servers.

If we extract the properties that we really need from our use of majorities, we have that we only
need to guarantee that groups of processes used to validate an operation by voting (e.g., acknowledging
a leader proposal) pairwise intersect in at least one server. Using majorities guarantees such a property.
However, there are other ways of constructing quorums different from majorities. For example, we can assign
weights to the votes of servers, and say that the votes of some servers are more important. To obtain a quorum,
we get enough votes so that the sum of weights of all votes is larger than half of the total sum of all weights.

A different construction that uses weights and is useful in wide-area deployments (co-locations) is a hierarchical
one. With this construction, we split the servers into disjoint groups and assign weights to processes. To form
a quorum, we have to get a hold of enough servers from a majority of groups G, such that for each group g in G,
the sum of votes from g is larger than half of the sum of weights in g. Interestingly, this construction enables
smaller quorums. If we have, for example, 9 servers, we split them into 3 groups, and assign a weight of 1 to each
server, then we are able to form quorums of size 4. Note that two subsets of processes composed each of a majority
of servers from each of a majority of groups necessarily have a non-empty intersection. It is reasonable to expect
that a majority of co-locations will have a majority of servers available with high probability.

With ZooKeeper, we provide a user with the ability of configuring servers to use majority quorums, weights, or a
hierarchy of groups.

<a name="sc_logging"></a>

## Logging

Zookeeper uses [slf4j](http://www.slf4j.org/index.html) as an abstraction layer for logging. [log4j](http://logging.apache.org/log4j) in version 1.2 is chosen as the final logging implementation for now.
For better embedding support, it is planned in the future to leave the decision of choosing the final logging implementation to the end user.
Therefore, always use the slf4j api to write log statements in the code, but configure log4j for how to log at runtime.
Note that slf4j has no FATAL level, former messages at FATAL level have been moved to ERROR level.
For information on configuring log4j for
ZooKeeper, see the [Logging](zookeeperAdmin.html#sc_logging) section
of the [ZooKeeper Administrator's Guide.](zookeeperAdmin.html)

<a name="sc_developerGuidelines"></a>

### Developer Guidelines

Please follow the  [slf4j manual](http://www.slf4j.org/manual.html) when creating log statements within code.
Also read the [FAQ on performance](http://www.slf4j.org/faq.html#logging\_performance), when creating log statements. Patch reviewers will look for the following:

<a name="sc_rightLevel"></a>

#### Logging at the Right Level

There are several levels of logging in slf4j.

It's important to pick the right one. In order of higher to lower severity:

1. ERROR level designates error events that might still allow the application to continue running.
1. WARN level designates potentially harmful situations.
1. INFO level designates informational messages that highlight the progress of the application at coarse-grained level.
1. DEBUG Level designates fine-grained informational events that are most useful to debug an application.
1. TRACE Level designates finer-grained informational events than the DEBUG.

ZooKeeper is typically run in production such that log messages of INFO level
severity and higher (more severe) are output to the log.

<a name="sc_slf4jIdioms"></a>

#### Use of Standard slf4j Idioms

_Static Message Logging_

    LOG.debug("process completed successfully!");

However when creating parameterized messages are required, use formatting anchors.

    LOG.debug("got {} messages in {} minutes",new Object[]{count,time});

_Naming_

Loggers should be named after the class in which they are used.

    public class Foo {
        private static final Logger LOG = LoggerFactory.getLogger(Foo.class);
        ....
        public Foo() {
            LOG.info("constructing Foo");

_Exception handling_

    try {
        // code
    } catch (XYZException e) {
        // do this
        LOG.error("Something bad happened", e);
        // don't do this (generally)
        // LOG.error(e);
        // why? because "don't do" case hides the stack trace
    
        // continue process here as you need... recover or (re)throw
    }
