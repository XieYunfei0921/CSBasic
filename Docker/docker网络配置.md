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

#### 使用覆盖网络

​	使用swarm服务创建覆盖网络,使用下述命令:

```shell
docker network create -d overlay my-overlay
```

使用swarm服务或者独立容器创建覆盖网络,为了与运行在docker上的独立容器进行交互.

```shell
$ docker network create -d overlay --attachable my-attachable-overlay
```

这里你可以指定ip地址范围,子网,网关和其他参数.使用`docker network create --help`查看.

#### 使用主机网络

#### 使用Mac VLAN网络

#### 取消容器的网络

#### 网络配置教程

#### 配置和启动容器

#### 合法网络内容