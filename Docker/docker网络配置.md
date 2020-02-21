### **网络配置**

---

#### 网络概述

docker服务如此强大的原因是，你可以互相连接，或者连接到非Docker工作负载上。docker的容器或者服务甚至不需要知道它们部署到Docker上，和它们的节点是否是docker的工作负载。当你的主机在linux或者windows上是，可以使用平台管理。

这个话题定义了基本的docker网络概念，设计和部署应用，去利用这个优势。这些内容大多数安装的时候就应用了，但是[这些是给EE用户准备的特征](https://docs.docker.com/network/#docker-ee-networking-features)

+ Topic的范围

  这个TOPIC,涉及到操作系统指定关于Docker如何运行的细节,所以你找不到docker是如何在linux上执行`iptable`的信息,以及如何他在windows服务器上执行的信息.你也找不到docker是如何组织和压缩数据包,以及处理加密问题的.你可以参考[Docker与iptable](https://docs.docker.com/network/iptables/),和[Docker引用构建](https://docs.docker.com/network/iptables/)获取更深层次的技术细节.

  这里不提供如果创建,管理和使用docker网络.每个部分都有包含相关教程和指令参考.

+ 网络驱动器

  docker的网络子系统时可插拔的。默认状态下存在多种驱动器，这里提供几种核心的网络形式：

  +  桥接网络

    默认网络驱动器，如果你不想指定驱动器，这就是你会创建的驱动器。桥接网络在你的应用处于脱机容器中，且需要网络传输，这里就会使用桥接网络，[详情请看](https://docs.docker.com/network/bridge/)

  +  主机网络

    对于独立运行的容器,移除容器和docker主机之间的网络隔离.直接使用主机的网络,这种方式仅仅在使用`swarm`且在17.06之后的版本才能使用.

  +  覆盖网络

    覆盖网络允许将多个docker启动器连接在一起,允许swarm服务相互交互.可以使用覆盖网络构建swarm服务或者独立容器之间的交互设施.这个策略移除了操作系统层级的程序

  +  MAC LAN网络

    这种模式下,允许你指定容器的MAC地址,是的在网络上以物理层的形式展示.docker启动器通过MAC 地址定位容器.使用这种驱动器对于处理旧有应用,且希望直接通过物理层相连很有效.请参照[MAC LAN网络](https://docs.docker.com/network/macvlan/)

  +  None

    对于容器来说,取消所有网络设置,经常用于连接用户网络驱动器,不可以使用在`swarm`服务中.参考如何[解除网络连接](https://docs.docker.com/network/none)

  +  网络插件

    可以安装和使用三方插件,可以从[DockerHub](https://hub.docker.com/search?category=network&q=&type=plugin)中获取三方插件,可以参照供应商相关的文档,去安装使用网络插件.

#### 使用桥接网络

从网络角度来看,桥接网络是一个数据链路层的服务,沟通两个网段.一个网桥可以是一个硬件设备或者是一个运行在主机内核的软件服务.

从docker的角度来看,网桥使用了软件网桥,允许容器链接到同一个网桥的网络.这里对没有链接到一起的容器提供了容器之间的隔离.docker网桥驱动器自动安装**主机规则**,这样不同网桥的容器就不能互相之间进行直接交流.实现了容器之间的隔离.

网桥会应用到运行同一个docker启动器的主机的容器上.对于与其他主机之间的交互,你可以通过管理操作系统级别的路由,或者你可以使用覆盖型网络.

启动docker时,默认创建网桥网络,新创建的容器会连接到网络中(除非创建时指定网段).你可以创建用于定义的网桥网络.**自定义网桥信息要优先于默认网桥网络**

+  用户定义网桥和默认网桥

  1.  用户定义的网桥提供容器化应用之间更好的隔离和相互操作性
  2.  用户自定义网桥提供容器之间的自动DNS服务
  3.  容器可以通过设置建立或者解除网桥连接
  4.  每个用户定义网络会创建一个可配置的网桥
  5.  默认网桥网络连接的容器共享环境变量

+  管理用户定义网桥

  使用`docker network create`创建网桥

  ```shell
  docker network create my-net
  ```

  你可以指定子网,IP地址范围,网关,以及其他配置项.参考[相关指令](https://docs.docker.com/engine/reference/commandline/network_create/#specify-advanced-options)或者使用`docker network --help`查看细节信息.使用`docker network rm`指令移除用户定义的网桥.如果包含当前网络,先[拆除链接](https://docs.docker.com/network/bridge/#disconnect-a-container-from-a-user-defined-bridge)

  ```shell
  docker network rm my-net
  ```
  
+   连接用户定义网桥的容器

   创建一个容器时，可以指定一个`--network`参数,下述例子时创建一个nginx容器,用于`my-net`网络上.允许的容器的80端口映射到docker主机的8080端口.其他连接到`my-network`网络,可以获取到`my-nginx`的所有端口,反之亦然.

   ```shell
   docker create --name my-nginx \
     --network my-net \
     --publish 8080:80 \
     nginx:latest
   ```

   使用`connect`指令,将运行中容器链接到一个用户指定网桥上.下述指令`mynet`已存在,`mynginx`已经允许

   ```shell
   docker network connect my-net my-nginx
   ```

+   去除连接到指定网络的容器的链接

   ```shell
   docker network disconnect my-net my-nginx
   ```

+   使用IPV6

   If you need IPv6 support for Docker containers, you need to [enable the option](https://docs.docker.com/config/daemon/ipv6/) on the Docker daemon and reload its configuration, before creating any IPv6 networks or assigning containers IPv6 addresses.

   When you create your network, you can specify the `--ipv6` flag to enable IPv6. You can’t selectively disable IPv6 support on the default `bridge` network.

   如果你需要docker容器对IPV6进行支持,需要设置docker[启动参数](https://docs.docker.com/config/daemon/ipv6/),并重载配置,再创建IPV 6网络或者指定容器的IPV 6地址.

   创建网络的时候,可以指定`--ipv6`去启动IPV 6网络,不能选择性的关闭默认网络的IPV 6支持.

+   允许docker容器向外部转发

   By default, traffic from containers connected to the default bridge network is **not** forwarded to the outside world. To enable forwarding, you need to change two settings. These are not Docker commands and they affect the Docker host’s kernel.

   默认情况下,来自默认网络的容器是不能发送数据到外部的.为了开启转发功能,你需要改变两个配置,这个不是docker指令,但是会影响到docker主机的内核.

   1.  配置linux内核,使之允许IP转发

      ```shell
      sysctl net.ipv4.conf.all.forwarding=1
      ```

   2.  改变`iptable`的转发政策

      ```shell
      sudo iptables -P FORWARD ACCEPT
      ```

   注意上述两个配置重启之后失效,放到启动脚本里比较好.

+   使用默认网络

   默认网络看作是docker的实现细节,不推荐生产环境下使用.配置是手动操作,具有一些缺陷.

+   连接默认网络下的容器

   如果没有通过`--network`指令指定网络,你可以指定一个网络驱动器,你的容器就会默认情况下连接到一个网桥网络上了.默认网桥网络的容器之间可以相互交互,仅仅通过IP地址即可通信.除非是指定了[连接方式](https://docs.docker.com/network/links/)

+   配置默认网桥网络

   配置默认网桥网络,需要指定一个`daemon.json`.下面给出一个示例,这里只需要指定你需要的参数即可.

   ```json
   {
     "bip": "192.168.1.5/24",
     "fixed-cidr": "192.168.1.5/25",
     "fixed-cidr-v6": "2001:db8::/64",
     "mtu": 1500,
     "default-gateway": "10.20.1.1",
     "default-gateway-v6": "2001:db8:abcd::89",
     "dns": ["10.20.1.2","10.20.1.3"]
   }
   ```

   重启docker,使上述配置生效.

+   默认网桥网络使用IPV 6

   如果你需要配置[IPV 6支持](https://docs.docker.com/network/bridge/#use-ipv6),默认网桥网络支持自动配置IPV 6.你不可以选择性的关闭默认网桥的IPV 6支持.

#### 使用覆盖网络

覆盖网络驱动器在多个docker启动主机上创建了一个分布式网络,网络位于主机指定的网络上,运行容器链接向它,用于进行安全沟通.docker显式的处理了每个packet的路由,且纠正目标容器.当你初始化一个swar或者加入到指定的swarm时.两个新的网络创建在docker主机上.

当你初始化一个swar或者加入到指定的swarm时.两个新的网络创建在docker主机上.

+ 一个叫做`ingress`的覆盖网络,处理控制和swarm相关的数据定位.当你创建一个swarm服务且没有链接到用户定义的覆盖网络,默认情况下链接到`ingress`.
+ 一个叫做`docker_gwbridge`的网桥网络,连接到单独docker启动器,其他启动器可以在swarm中中加入.

可以使用`docker network create`创建用户定义的网络.同样方式下,可以创建用户定义的网络.同样方式下,你可以创建用户定义的网桥网络,服务或者容器在一个时候可以被多个网络连接.服务或者容器可以通过网络连接.

尽管在覆盖网络下,可以使用度量容器和swarm服务,但是默认行为和配置被认作时不同的.对于这个原因,剩下的topic被划分为用于覆盖网络的操作.这会应用到swarm服务网络和单独运行网络的覆盖网络.

+ 覆盖网络的操作

  1.  创建覆盖网络

     > 前置条件:
     >
     > + docker执行器的防火墙规则
     >
     >   你需要使用下述端口,用于定位docker端口
     >
     >   1. TCP 端口2377,用户集群间信息交互
     >   2.  TCP 和UDP端口7946在节点之间进行交互
     >   3.  UDP端口4789覆盖网络定位
     >
     > + 创建覆盖网络时,需要初始化docker启动器,使用`docker swarm init`或者加入到指定swarm中,使用`docker swarm join`.这些使用`ingress`创建了默认的覆盖网络,用于swarm服务.如果你需要使用swarm服务,之后你可以创建额外的用户定位网络.

​	使用swarm服务创建覆盖网络,使用下述命令:

```shell
docker network create -d overlay my-overlay
```

使用swarm服务或者独立容器创建覆盖网络,为了与运行在docker上的独立容器进行交互.

```shell
$ docker network create -d overlay --attachable my-attachable-overlay
```

这里你可以指定ip地址范围,子网,网关和其他参数.使用`docker network create --help`查看.

+ 加密覆盖网络

  swarm服务管理的信号量默认情况下是加密的。在GCM模式下使用[AES加密算法](https://en.wikipedia.org/wiki/Galois/Counter_Mode)，管理节点每12小时swarm反转key用于加密传播数据。

  加密应用数据和创建覆盖网络时使用`--opt encryted`.允许再vxlan层创建IPSEC加密,加密有一些微小的表现.在生产环境中使用前,需要测试这些参数.

  如果你开启了覆盖层网络加密,docker会在所有节点之间创建IPSEC通道,在GCM模式下使用AES算法,使其能够自动管理节点.

  > 注意不要在覆盖网络中连接windows节点
  >
  > windows节点可以连接上,但是不能进行通信.

+ 覆盖网络swarm模式和独立容器

  可以使用`--opt encrypted --attachable`连接网络中没有管理的容器

  ```scala
  $ docker network create --opt encrypted --driver overlay --attachable my-attachable-multi-host-network
  ```

+ 自定义默认入口网络

  大多数用户永远都不需要对入口网络进行配置,但是docker 17.05之后的版本允许对入口网络进行配置,如果与一个已经存在的网络存在子网冲突,这是使用这个就会自动选择.你也可以自定义底层网络配置(例如MTU).

  自定义入口网络八米宽移除和重建,在你创建swarm服务之前创建,如果之前有服务发布了这个端口,这些服务需要在你移除入口网络之前被移除.

  During the time that no `ingress` network exists, existing services which do not publish ports continue to function but are not load-balanced. This affects services which publish ports, such as a WordPress service which publishes port 80.

  没有入口网络存在的期间,存在的服务不会发布端口,这个会影响服务的帆布端口.

  1.  检查入口网络,移除连接的容器.如果没有移除,下一步将会失败.

     ```shell
     docker network inspect ingress
     ```

  2.  移除指定入口网络

     ```shell
     docker network rm ingress
     ```

  3.  使用`--ingress`标记创建一个新的覆盖网络,可以设置你配置的用户配置.示例将MTU设置到1200端口上,子网为10.11.0.0/16,网关地址为10.11.0.2

     ```shell
     $ docker network create \
       --driver overlay \
       --ingress \
       --subnet=10.11.0.0/16 \
       --gateway=10.11.0.2 \
       --opt com.docker.network.driver.mtu=1200 \
       my-ingress
     ```

  4.  重启之前停止的服务

+ 自定义docker _gwbridge接口

  `docker _gwbridge`是一个连接到覆盖网络的虚拟网桥(包括入口网络),将其连接到docker启动器的物理网络上.当你创建以恶搞swarm或者添加一个docker主机到swarm中时docker自动创建.但是不是docker服务,它存在于docker主机内核中,如果你需要自定义参数,必须要在将docker主机添加到swarm之前,对其进行自定义.获取这是暂时的将主机移除swarm进行自定义.

  1. 停止docker

  2. 删除存在的`docker _gwbridge`接口

  3. 开启docker,加入或者初始化swarm

  4. 手动创建或者重建`docker _gwbridge`网桥,使用自定义参数,使用`docker network create`指令,示例使用子网`10.11.0.0/16`创建子网.对于整个自定义参数,参考[网桥驱动器](https://docs.docker.com/engine/reference/commandline/network_create/#bridge-driver-options)配置

     ```shell
     $ docker network create \
     --subnet 10.11.0.0/16 \
     --opt com.docker.network.bridge.name=docker_gwbridge \
     --opt com.docker.network.bridge.enable_icc=false \
     --opt com.docker.network.bridge.enable_ip_masquerade=true \
     docker_gwbridge
     ```

  5. 初始化或者加入swarm,由于网桥已经存在,docker不需要自动创建.

+ swarm服务操作

  1.  发布覆盖网络的端口

     swarm服务连接到同一个覆盖网络上,暴露所有端口.对于一个可以从外部获取的端口,端口必须使用`-p`或者`--publish`标记.在`docker service create`或者`docker service update`

     | Flag值                                                       | 描述                                                         |
     | ------------------------------------------------------------ | ------------------------------------------------------------ |
     | -p 8080:80<br />-p published=8080,target=80                  | 将TCP 80端口映射到服务的8080端口                             |
     | `-p 8080:80/udp` or<br />-p published=8080,target=80,protocol=udp | 将UDP 80端口,映射到服务8080端口                              |
     | `-p 8080:80/tcp -p 8080:80/udp` or<br />-p published=8080,target=80,protocol=tcp -p published=8080,target=80,protocol=udp | 映射TCP 80端口到服务的8080端口,同时映射UDP的 80端口到服务8080端口 |

  2. swarm服务绕开路由网络

     默认情况下,swarm服务发送端口,通过使用路由当你连接到一个发布的swarm 节点端口(无论是不是给定的服务).显然的重定向到允许服务的执行者身上.docker对你的swarm 服务进行负载均衡.使用虚拟IP模式使用路由,不能保证docker节点服务客户端的请求情况.

     为了绕开路由网络,可以开启一个DNS 的循环(Round Robin). 通过设置`--endpoint-mode`标志给`dnsrr`.必须在服务之前,进行负载均衡.一个DNS任务请求返回了IP地址列表,用于允许任务的节点.配置你的负载均衡器去消费你的列表,且平衡节点间的信号量.

  3.  分离控制和数据信号量

     默认情况下,空值信号量与swarm管理器相关,信号量来自于你的应用.通过swarm信号量控制的加密,你可以配置docker,去使用分离网络接口,用于处理两个不同类型的信号量.当你初始化或者加入swarm时,分别指定`--advertise-addr`和`--datapath-addr`.每个节点加入时必须如此操作.

+  独立容器的操作

  1.  连接独立容器

     入口网络没有`--attachable`标志,这个意味着只有swarm服务可以使用,而不是独立容器.你可以连接独立容器去.你可以连接独立容器到用户定义的覆盖网络上(使用`-attachable`标记创建).这个可以给与独立容器不需要在独立docker启动器上设置路由的情况下,进行通信.

  2.  发布端口

     | flag值                          | 描述                                                         |
     | ------------------------------- | ------------------------------------------------------------ |
     | `-p 8080:80`                    | 映射TCP 80端口到覆盖网络的8080端口                           |
     | `-p 8080:80/udp`                | 映射UPD 80端口到覆盖网络8080端口                             |
     | `-p 8080:80/sctp`               | 映射STCP 80端口到覆盖网络8080端口                            |
     | `-p 8080:80/tcp -p 8080:80/udp` | 映射TCP 80到覆盖网络的8080端口,UDP 80端口到覆盖网络的8080端口 |

  3.  容器发现

     对于大多数情况,你可以连接到服务名称,这个是负载均衡的,使用返回服务的容器处理.获取一列任务列表,用于放置服务信息,进行DNS查找,形如`tasks.<service-name>`

#### 使用主机网络

如果你使用容器的主机网络模式,容器的网络栈没有和docker主机各类.这个容器不会获取自身分配的IP地址.此外,如果你运行一个绑定到80端口的容器,且使用了主机网络,容器的应用可以通过80端口获取服务.

主机模式网络对于优化相当有用.当一个容器需要处理大量端口时,因为它不需要网络地址转换(NAT).且每个端口没有创建用户代理.

主机网络驱动器只工作在linux主机上.不支持其他类型.

你可以使用swarm service的主机网络,通过传递`--network host` 给`docker service host`指令.这种情况下,控制信号量仍然通过覆盖网络发送.但是单个swarm服务容器通过docker启动器的主机网络和端口发送数据.例如,一个服务容器绑定到80端口,仅仅一个服务容器可以运行指定的swarm节点.

#### 使用Mac VLAN网络

+ 创建MAC VLAN网络

  当你创建一个`MAC VLAN`网络时,既可以桥接也可以使用中继网桥接

  1. 在桥接模式下`MAC VLAN`信号量遍布主机上的所有设备
  2.  中继网桥接模式下,允许控制路由和在粒状层级进行控制.

+ 桥接模式

  使用`docker network create`和`-driver macvlan`指令,创建一个桥接所有物理设备接口的`MAC VLAN`网络.你也可以指定`parent`,这个时docker主机会遍布的接口.

  ```shell
  $ docker network create -d macvlan \
    --subnet=172.16.86.0/24 \
    --gateway=172.16.86.1 \
    -o parent=eth0 pub_net
  ```

  如果你需要排除一些ip地址,使用`--aux-address`

  ```shell
  $ docker network create -d macvlan \
    --subnet=192.168.32.0/24 \
    --ip-range=192.168.32.128/25 \
    --gateway=192.168.32.254 \
    --aux-address="my-router=192.168.32.129" \
    -o parent=eth0 macnet32
  ```

+ 802.1q中继线桥接模式

  可以指定`parent`接口,比如`eth0.50`.docker会作为`eth0`对其进行中断,并创建子接口.

  ```shell
  $ docker network create -d macvlan \
      --subnet=192.168.50.0/24 \
      --gateway=192.168.50.1 \
      -o parent=eth0.50 macvlan50
  ```

+ 使用ipvlan代替

  仍旧可以使用L3网桥,可以使用`ipvlan`代替,且获取一个L2网桥,指定`-o ipvlan_mode=l2`

  ```shell
  $ docker network create -d ipvlan \
      --subnet=192.168.210.0/24 \
      --subnet=192.168.212.0/24 \
      --gateway=192.168.210.254 \
      --gateway=192.168.212.254 \
       -o ipvlan_mode=l2 ipvlan210
  ```

+ 使用IPV6

  你可以开启IPV6,使得可以处理IPV4/IPV6的`mac vlan`网络

  ```shell
  $ docker network create -d macvlan \
      --subnet=192.168.216.0/24 --subnet=192.168.218.0/24 \
      --gateway=192.168.216.1 --gateway=192.168.218.1 \
      --subnet=2001:db8:abc8::/64 --gateway=2001:db8:abc8::10 \
       -o parent=eth0.218 \
       -o macvlan_mode=bridge macvlan216
  ```

#### 取消容器的网络

如果你想完全取消容器的网络协议栈,可以在启动容器的时候,使用`--network none`标记.这是只会创建回环服务.

1. 创建容器

   ```shell
   $ docker run --rm -dit \
     --network none \
     --name no-net-alpine \
     alpine:latest \
     ash
   ```

2. 检查容器网络协议栈

   ```shell
   $ docker exec no-net-alpine ip link show
   
   1: lo: <LOOPBACK,UP,LOWER_UP> mtu 65536 qdisc noqueue state UNKNOWN qlen 1
       link/loopback 00:00:00:00:00:00 brd 00:00:00:00:00:00
   2: tunl0@NONE: <NOARP> mtu 1480 qdisc noop state DOWN qlen 1
       link/ipip 0.0.0.0 brd 0.0.0.0
   3: ip6tnl0@NONE: <NOARP> mtu 1452 qdisc noop state DOWN qlen 1
       link/tunnel6 00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00 brd 00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00
   ```

   再执行指令,查看容器路由表

   ```shell
   $ docker exec no-net-alpine ip route
   ```

3. 停止容器,自动移除

   ```shell
   $ docker container rm no-net-alpine
   ```

#### 网络配置教程

1.  桥接网络配置

   + [官方教程](https://docs.docker.com/network/network-tutorial-standalone/)
   + [本地教程](http://127.0.0.1:4000/network/network-tutorial-standalone/)

2. 主机网络配置

   https://docs.docker.com

   + [官方教程](https://docs.docker.com/network/network-tutorial-host/)
   + [本地教程](http://127.0.0.1:4000/network/network-tutorial-host/)

3. 覆盖网络配置

   + [官方教程](https://docs.docker.com/network/network-tutorial-overlay/)
   + [本地教程](http://127.0.0.1:4000/network/network-tutorial-overlay/)

4. MAC VLAN网络配置

   + [官方教程](https://docs.docker.com/network/network-tutorial-macvlan/)
   + [本地教程](http://127.0.0.1:4000/network/network-tutorial-macvlan/)

#### 配置和启动容器

+ 配置IPV6 启动器

  先允许IPV6再docker容器上运行,才能使用docker容器或者swarm服务的IPV6功能.设置完毕之后,可以选择性的使用IPV4,IPV6协议.

  注意: IPV6 只支持Linux下运行

  1. 编写`/etc/docker/daemon.json`设置`ipv6`为`true`

     ```json
     {
         "ipv6": true
     }
     ```

     保存文件

  2. 重载docker配置文件

     ```shell
     $ systemctl reload docker
     ```

     这样就可以创建IPV6网络了,可以指定容器的网络为IPV6网络通过设置`--ip6`标记.

+ docker和ip地址表

  Linux系统上,docker操作ip地址映射表用于进行网络隔离.你不能通过插入到你的`iptables`协议来修改规则.

  + docker之前添加ip地址表协议

    所有docker的`iptables`规则都会被加入到docker任务链中.不要手动的操作这个表,如果你需要添加规则,并重载docker规则.将其添加到`DOCKER_USER`任务链中.这些规则会在docker创建之前自动加载.

  + 限制docker启动器的连接

    默认情况下,外部IP资源允许连接到docker启动器上.为了允许指定ip或者网络,再docker过滤任务链中加入一条反规则,例如,限制外部IP连接,除了192.168.1.1

    ```shell
    $ iptables -I DOCKER-USER -i ext_if ! -s 192.168.1.1 -j DROP
    ```

    注意你需要改变`ext_if`去应对主机实际外部接口.你可以允许资源子网的外部连接,下述规则只允许子网`192.168.1.0/24`

    ```shell
    $ iptables -I DOCKER-USER -i ext_if ! -s 192.168.1.0/24 -j DROP
    ```

    最后,你可以指定外部IP地址范围,使用`--src-range`或者`--dst-range`时记得添加`-m iprange`.

    ```shell
    $ iptables -I DOCKER-USER -m iprange -i ext_if ! --src-range 192.168.1.1-192.168.1.3 -j DROP
    ```

    可以合并`-s`或者`--src-range`使用`-d`或者`--dst-range`去控制资源和目标地址.例如,docker启动器监听到192.168.1.99和10.1.2.3,你可以制作规则监听`10.1.2.3`而不监听`192.168.1.99`.

    `iptable`是复杂的,参考这份[文档](https://www.netfilter.org/documentation/HOWTO/NAT-HOWTO.html)获取更多信息.

  + 阻止docker操作ip地址表

+ 容器网络

+ 配置docker的代理服务器

#### 合法网络内容