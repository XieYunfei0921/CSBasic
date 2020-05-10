/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.zookeeper.recipes.leader;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * leader选举支持库实现zk选举的recipe
 *
 * 这个支持库意味着可以在zk的排他leader系统进行简单的构建.任何应用都可以编程leader(使用一个可以提供服务的进程即可,需要是排他的).
 * 一旦配置成功,就会调用@start 方法,会让客户端连接到zk并创建一个leader选举的申请.
 * 这个库就可以决定是否被选举为leader,使用下述描述的算法.客户端应用可以通过监听器回调跟随所有状态转换.
 *
 * leader选举算法
 * 这个库起始在START状态.通过状态转换,一个状态的启动和完成事件会被发送到所有监听器上.当调用@start 方法的嘶吼,leader选举就在ZK上
 * 开启了.leader选举是一个短暂有序的节点.这个节点可以表示一个可以作为这个服务leader的进程.
 * 之后会进行所有leader选举的读取,使用较小的序列编号节点会变成leader.被选举的leader会转换为leader状态.其他进程会转换为准备状态.
 * 从内部来说,库四合院序列编号(n-1)在leader上创建一个zk监视器.其中这个n是进程的序列编号.
 * 如果由于进程失败,而使得leader选举请求丢失,观测器会通过leader选举决定是否会成为下一个leader.注意序列id不会因为失败进程而连续.
 * 一个进程可以通过@stop方法在任意时刻撤销leader的选举,从而不能成为leader.
 *
 *
 * 注意:
 * 很可能对于一个创建leader申请的进程,获取了最小的序列ID.但是当维持与zk连接的时候发生了一些不好的事情,但是不提供服务.
 * 这个主要取决于用于对leader出现失败的处理.
 * 可能对于zk设置的超时时间和重试策略,在服务生命周期中扮演重要的角色.换句话说,进程A有最低的序列编号,但是需要读取其他leader申请的
 * 序列编号.这样选举就会变慢.用户需要在决定是否命中指定的SLA的时候使用超时时间设置.
 * 这个库尽最大的努力去发现进程的灾难性失败.
 *
 * 参数列表
 * @zooKeeper zk客户端
 * @state 选举状态
 * @listeners listerns选举状态列表
 * @rootNodeName znode根节点名称
 * @leaderOffer leader申请
 * @hostName 主机名称
 */
public class LeaderElectionSupport implements Watcher {

    private static final Logger LOG = LoggerFactory.getLogger(LeaderElectionSupport.class);

    private ZooKeeper zooKeeper;

    private State state;
    private Set<LeaderElectionAware> listeners;

    private String rootNodeName;
    private LeaderOffer leaderOffer;
    private String hostName;

    public LeaderElectionSupport() {
        state = State.STOP;
        listeners = Collections.synchronizedSet(new HashSet<>());
    }

    /**
     * 启动选举进程.这个方法会创建一个leader申请.决定zk的状态(变成leader或者是ready状态).
     * 如果用户没有配置zk,就会使用连接信息创建一个新的实例
     *
     * 注意: 任何失败的结果会导致失败的事件发送到所有的监听器中
     */
    public synchronized void start() {
        state = State.START;
        dispatchEvent(EventType.START);

        LOG.info("Starting leader election support");

        if (zooKeeper == null) {
            throw new IllegalStateException(
                "No instance of zookeeper provided. Hint: use setZooKeeper()");
        }

        if (hostName == null) {
            throw new IllegalStateException(
                "No hostname provided. Hint: use setHostName()");
        }

        try {
            makeOffer();
            determineElectionStatus();
        } catch (KeeperException | InterruptedException e) {
            becomeFailed(e);
        }
    }

    /**
     * 停止所有选举服务,撤销leader申请,且断开与zk的连接
     */
    public synchronized void stop() {
        state = State.STOP;
        dispatchEvent(EventType.STOP_START);

        LOG.info("Stopping leader election support");

        if (leaderOffer != null) {
            try {
                zooKeeper.delete(leaderOffer.getNodePath(), -1);
                LOG.info("Removed leader offer {}", leaderOffer.getNodePath());
            } catch (InterruptedException | KeeperException e) {
                becomeFailed(e);
            }
        }

        dispatchEvent(EventType.STOP_COMPLETE);
    }

    /**
     * 发送leader申请
     * @throws KeeperException
     * @throws InterruptedException
     */
    private void makeOffer() throws KeeperException, InterruptedException {
        state = State.OFFER;
        dispatchEvent(EventType.OFFER_START);

        LeaderOffer newLeaderOffer = new LeaderOffer();
        byte[] hostnameBytes;
        synchronized (this) {
            newLeaderOffer.setHostName(hostName);
            hostnameBytes = hostName.getBytes();
            newLeaderOffer.setNodePath(zooKeeper.create(rootNodeName + "/" + "n_",
                                                        hostnameBytes, ZooDefs.Ids.OPEN_ACL_UNSAFE,
                                                        CreateMode.EPHEMERAL_SEQUENTIAL));
            leaderOffer = newLeaderOffer;
        }
        LOG.debug("Created leader offer {}", leaderOffer);

        dispatchEvent(EventType.OFFER_COMPLETE);
    }

    private synchronized LeaderOffer getLeaderOffer() {
        return leaderOffer;
    }

    private void determineElectionStatus() throws KeeperException, InterruptedException {

        state = State.DETERMINE;
        dispatchEvent(EventType.DETERMINE_START);

        LeaderOffer currentLeaderOffer = getLeaderOffer();

        String[] components = currentLeaderOffer.getNodePath().split("/");

        currentLeaderOffer.setId(Integer.valueOf(components[components.length - 1].substring("n_".length())));

        List<LeaderOffer> leaderOffers = toLeaderOffers(zooKeeper.getChildren(rootNodeName, false));

        /*
         * For each leader offer, find out where we fit in. If we're first, we
         * become the leader. If we're not elected the leader, attempt to stat the
         * offer just less than us. If they exist, watch for their failure, but if
         * they don't, become the leader.
         */
        for (int i = 0; i < leaderOffers.size(); i++) {
            LeaderOffer leaderOffer = leaderOffers.get(i);

            if (leaderOffer.getId().equals(currentLeaderOffer.getId())) {
                LOG.debug("There are {} leader offers. I am {} in line.", leaderOffers.size(), i);

                dispatchEvent(EventType.DETERMINE_COMPLETE);

                if (i == 0) {
                    becomeLeader();
                } else {
                    becomeReady(leaderOffers.get(i - 1));
                }

                /* Once we've figured out where we are, we're done. */
                break;
            }
        }
    }

    /**
     * 转换为准备状态
     * @param neighborLeaderOffer
     * @throws KeeperException
     * @throws InterruptedException
     */
    private void becomeReady(LeaderOffer neighborLeaderOffer)
        throws KeeperException, InterruptedException {

        LOG.info(
            "{} not elected leader. Watching node: {}",
            getLeaderOffer().getNodePath(),
            neighborLeaderOffer.getNodePath());

        /*
         * Make sure to pass an explicit Watcher because we could be sharing this
         * zooKeeper instance with someone else.
         */
        Stat stat = zooKeeper.exists(neighborLeaderOffer.getNodePath(), this);

        if (stat != null) {
            dispatchEvent(EventType.READY_START);
            LOG.debug(
                "We're behind {} in line and they're alive. Keeping an eye on them.",
                neighborLeaderOffer.getNodePath());
            state = State.READY;
            dispatchEvent(EventType.READY_COMPLETE);
        } else {
            /*
             * If the stat fails, the node has gone missing between the call to
             * getChildren() and exists(). We need to try and become the leader.
             */
            LOG.info(
                "We were behind {} but it looks like they died. Back to determination.",
                neighborLeaderOffer.getNodePath());
            determineElectionStatus();
        }

    }

    /**
     * 将当前状态转换为LEADER状态
     */
    private void becomeLeader() {
        state = State.ELECTED;
        dispatchEvent(EventType.ELECTED_START);

        LOG.info("Becoming leader with node: {}", getLeaderOffer().getNodePath());

        dispatchEvent(EventType.ELECTED_COMPLETE);
    }

    /**
     * 将当前状态转换为失败状态
     */
    private void becomeFailed(Exception e) {
        LOG.error("Failed in state {}", state, e);

        state = State.FAILED;
        dispatchEvent(EventType.FAILED);
    }

    /**
     * Fetch the (user supplied) hostname of the current leader. Note that by the
     * time this method returns, state could have changed so do not depend on this
     * to be strongly consistent. This method has to read all leader offers from
     * ZooKeeper to deterime who the leader is (i.e. there is no caching) so
     * consider the performance implications of frequent invocation. If there are
     * no leader offers this method returns null.
     *
     * @return hostname of the current leader
     * @throws KeeperException
     * @throws InterruptedException
     */
    public String getLeaderHostName() throws KeeperException, InterruptedException {

        List<LeaderOffer> leaderOffers = toLeaderOffers(zooKeeper.getChildren(rootNodeName, false));

        if (leaderOffers.size() > 0) {
            return leaderOffers.get(0).getHostName();
        }

        return null;
    }

    // 转换为leader数千年过去信息
    private List<LeaderOffer> toLeaderOffers(List<String> strings)
        throws KeeperException, InterruptedException {

        List<LeaderOffer> leaderOffers = new ArrayList<>(strings.size());

        /*
         * Turn each child of rootNodeName into a leader offer. This is a tuple of
         * the sequence number and the node name.
         */
        for (String offer : strings) {
            String hostName = new String(zooKeeper.getData(rootNodeName + "/" + offer, false, null));

            leaderOffers.add(new LeaderOffer(
                Integer.valueOf(offer.substring("n_".length())),
                rootNodeName + "/" + offer, hostName));
        }

        /*
         * We sort leader offers by sequence number (which may not be zero-based or
         * contiguous) and keep their paths handy for setting watches.
         */
        Collections.sort(leaderOffers, new LeaderOffer.IdComparator());

        return leaderOffers;
    }

    /**
     * 处理指定事件的znode删除
     * @param event 事件
     */
    @Override
    public void process(WatchedEvent event) {
        if (event.getType().equals(Watcher.Event.EventType.NodeDeleted)) {
            if (!event.getPath().equals(getLeaderOffer().getNodePath())
                && state != State.STOP) {
                LOG.debug(
                    "Node {} deleted. Need to run through the election process.",
                    event.getPath());
                try {
                    determineElectionStatus();
                } catch (KeeperException | InterruptedException e) {
                    becomeFailed(e);
                }
            }
        }
    }

    /**
     * 将指定事件通过监听器发送到各个观测者中
     * @param eventType 事件类型
     */
    private void dispatchEvent(EventType eventType) {
        LOG.debug("Dispatching event: {}", eventType);

        synchronized (listeners) {
            if (listeners.size() > 0) {
                for (LeaderElectionAware observer : listeners) {
                    observer.onElectionEvent(eventType);
                }
            }
        }
    }

    /**
     * 将指定的监听器添加到监听器列表中
     */
    public void addListener(LeaderElectionAware listener) {
        listeners.add(listener);
    }

    /**
     * 从监听器列表中移除指定监听器@listener
     */
    public void removeListener(LeaderElectionAware listener) {
        listeners.remove(listener);
    }

    @Override
    public String toString() {
        return "{"
            + " state:" + state
            + " leaderOffer:" + getLeaderOffer()
            + " zooKeeper:" + zooKeeper
            + " hostName:" + getHostName()
            + " listeners:" + listeners
            + " }";
    }

    /**
     * 获取这个服务的zk 根阶段
     * <p>
     * For instance, a root node of {@code /mycompany/myservice} would be the
     * parent of all leader offers for this service. Obviously all processes that
     * wish to contend for leader status need to use the same root node. Note: We
     * assume this node already exists.
     * </p>
     *
     * @return a znode path
     */
    public String getRootNodeName() {
        return rootNodeName;
    }

    /**
     * <p>
     * Sets the ZooKeeper root node to use for this service.
     * </p>
     * <p>
     * For instance, a root node of {@code /mycompany/myservice} would be the
     * parent of all leader offers for this service. Obviously all processes that
     * wish to contend for leader status need to use the same root node. Note: We
     * assume this node already exists.
     * </p>
     */
    public void setRootNodeName(String rootNodeName) {
        this.rootNodeName = rootNodeName;
    }

    /**
     * The {@link ZooKeeper} instance to use for all operations. Provided this
     * overrides any connectString or sessionTimeout set.
     */
    public ZooKeeper getZooKeeper() {
        return zooKeeper;
    }

    public void setZooKeeper(ZooKeeper zooKeeper) {
        this.zooKeeper = zooKeeper;
    }

    /**
     * 或进程的主机名称
     */
    public synchronized String getHostName() {
        return hostName;
    }

    public synchronized void setHostName(String hostName) {
        this.hostName = hostName;
    }

    /**
     * 事件类型
     * 启动类型
     * 申请启动
     * 申请完成
     * 决定阶段开始
     * 决定阶段完成
     * 选举开始
     * 选举结束
     * 准备阶段开始
     * 准备阶段结束
     * 失败
     * 停止操作开始
     * 停止操作结束
     */
    public enum EventType {
        START,
        OFFER_START,
        OFFER_COMPLETE,
        DETERMINE_START,
        DETERMINE_COMPLETE,
        ELECTED_START,
        ELECTED_COMPLETE,
        READY_START,
        READY_COMPLETE,
        FAILED,
        STOP_START,
        STOP_COMPLETE,
    }

    /**
     * 内部选举状态
     * 启动
     * 申请
     * 决定
     * 被选举
     * 准备
     * 失败
     * 停止
     */
    public enum State {
        START,
        OFFER,
        DETERMINE,
        ELECTED,
        READY,
        FAILED,
        STOP
    }

}
