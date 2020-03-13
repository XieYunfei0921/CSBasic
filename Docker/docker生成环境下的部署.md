**编制**

---

##### 概述

容器化进程的轻便性和可再现性意味着可以对容器化应用进行移动和扩容，可以通过云端或者数据中心进行操作。容器高效的保证了这些应用在任何位置都同样的运行，允许去快速和简单的利用这些环境变量。此外，当对应用进行扩容的时候，需要溢写工具去辅助这些应用的自动化维护，和能够自动替代失败的容器，此外在容器的生命周期内管理更新和重新配置。

用于管理，扩展和维护容器化应用的叫做**编制工具**。常用的示例就是**Kubernetes**和**Docker Swarm**。docker桌面版本可以不是这些编制工具。

+ 启动Kubernetes

  docker的桌面版可以简单快速的创建k8s，遵循相应的教程可以进行配置。

  1. windows版配置

     + 安装docker桌面版，找到Settings>Kubernetes

     + 找到标签**Enable Kubernetes**,点击**Applay & Restart**.docker桌面版会自动的创建Kubernetes.当你看到绿点的时候,就表示Kubernetes正在运行.

     + 为了确定配置Kubernetes成功,创建一个文本文件,叫做`pod.yaml`,内容如下:

       ```yaml
        apiVersion: v1
        kind: Pod
        metadata:
          name: demo
        spec:
          containers:
          - name: testpod
            image: alpine:3.5
            command: ["ping", "8.8.8.8"]
       ```

       使用pod描述了一个容器,简单的ping 了8.8.8.8

     + 在powershell,运行刚创建好的`pod.yaml`文件

       ```powershell
       kubectl apply -f pod.yaml
       ```

     + 检查pod是否处于运行状态

       ```powershell
        kubectl get pods
       ```

       可以看到如下内容

       ```powershell
       NAME      READY     STATUS    RESTARTS   AGE
        demo      1/1       Running   0          4s
       ```

     + 检查日志

       ```powershell
       kubectl logs demo
       ```

       可以看到下面的日志

       ```shell
       PING 8.8.8.8 (8.8.8.8): 56 data bytes
        64 bytes from 8.8.8.8: seq=0 ttl=37 time=21.393 ms
        64 bytes from 8.8.8.8: seq=1 ttl=37 time=15.320 ms
        64 bytes from 8.8.8.8: seq=2 ttl=37 time=11.111 ms
        ...
       ```

     + 删除测试pod

       ```powershell
        kubectl delete -f pod.yaml
       ```

  2. mac版本配置

     mac版本配置与windows配置流程差不多,只是需要在**Preferences** > **Kubernetes**找到Kubernetes启动位置.

+ 启动Docker Swarm

  1. windows版本

     + 打开powershell,初始化docker swarm模式

       ```powershell
       docker swarm init
       ```

       如果没问题可以看到如下信息

       ```powershell
       Swarm initialized: current node (tjjggogqpnpj2phbfbz8jd5oq) is now a manager.
       
        To add a worker to this swarm, run the following command:
       
            docker swarm join --token SWMTKN-1-3e0hh0jd5t4yjg209f4g5qpowbsczfahv2dea9a1ay2l8787cf-2h4ly330d0j917ocvzw30j5x9 192.168.65.3:2377
       
        To add a manager to this swarm, run 'docker swarm join-token manager' and follow the instructions.
       ```

     + 运行简单的docker服务,使用简单的alpine-based的文件系统,指令为ping 8.8.8.8这个地址

       ```powershell
       docker service create --name demo alpine:3.5 ping 8.8.8.8
       ```

     + 检查创建容器的服务

       ```powershell
       docker service ps demo
       ```

       可以看到如下信息

       ```powershell
       ID                  NAME                IMAGE               NODE                DESIRED STATE       CURRENT STATE           ERROR               PORTS
        463j2s3y4b5o        demo.1              alpine:3.5          docker-desktop      Running             Running 8 seconds ago
       
       ```

     + 检查日志

       ```powershell
       docker service logs demo
       ```

       可以看到如下信息

       ```powershell
       demo.1.463j2s3y4b5o@docker-desktop    | PING 8.8.8.8 (8.8.8.8): 56 data bytes
        demo.1.463j2s3y4b5o@docker-desktop    | 64 bytes from 8.8.8.8: seq=0 ttl=37 time=13.005 ms
        demo.1.463j2s3y4b5o@docker-desktop    | 64 bytes from 8.8.8.8: seq=1 ttl=37 time=13.847 ms
        demo.1.463j2s3y4b5o@docker-desktop    | 64 bytes from 8.8.8.8: seq=2 ttl=37 time=41.296 ms
        ...
       
       ```

     + 删除测试服务

       ```powershell
       docker service rm demo
       ```

  2. mac版本

     流程与windows版本基本一致

##### 部署到k8s上

+ 前置需求

  1. 下载和安装docker桌面版

  2. 配置容器化应用
  3. 确保开启了kubernetes

+ 介绍

  既然以及证明了单个应用组件可以作为独立的容器运行,name可以使用编制工具对其进行安排和管理.k8s使用许多扩容,网络,安全工具,用于维护容器化应用,远超过容器自身的维护能力.

  为了使容器化应用在k8s运行没问题,需要使用docker桌面版去构建k8s环境,用于部署应用.docker桌面构建的k8s环境配置完好,意味着所有k8s特征化的应用都会享有一个真实的集群环境,可以从开发机器上获取便利.

+ 使用kubernetes yaml描述应用

  1. 在终端中,创建`bb.yaml`,部署应用到kubernetes中

     ```powershell
     kubectl apply -f bb.yaml
     ```

     可以看到如下的输出

     ```powershell
     deployment.apps/bb-demo created
     service/bb-entrypoint created
     ```

  2. 确保kubernetes中的所有都运行良好

     ```shell
     kubectl get deployments
     ```

     如果没有问题,会显示如下信息:

     ```shell
     NAME      DESIRED   CURRENT   UP-TO-DATE   AVAILABLE   AGE
     bb-demo   1         1         1            1           48s
     ```

     这个表名了所有yaml中的pod运行没有问题,同样的对于服务做出如下检查:

     ```shell
     kubectl get services
     
     NAME            TYPE        CLUSTER-IP       EXTERNAL-IP   PORT(S)          AGE
     bb-entrypoint   NodePort    10.106.145.116   <none>        8080:30001/TCP   53s
     kubernetes      ClusterIP   10.96.0.1        <none>        443/TCP          138d
     ```

     除了默认的kubernetes服务之外,可以看到`bb-entrypoint`服务,从TCP30001端口接受数据.

  3. 打开浏览器,访问`localhost:30001`.就可以看到公告牌了,这个与入门教程的一致.

  4. 移除应用

     ```shell
     kubectl delete -f bb.yaml
     ```

**配置所有对象**

---

+ 配置元数据信息

  标签是配置元数据信息的原理,包括:

  > 1. 镜像
  > 2. 容器
  > 3. 本地启动器
  > 4. 数据卷
  > 5. 网络
  > 6. swarm节点
  > 7. swarm服务

  可以使用标签去组织镜像,记录认证信息注释容器,数据卷,网络之间的关系,或者配置对应用产生影响的参数.

  1.  标签的key和value

     标签是一个kv对,以字符串的形式存储,可以指定多个标签到一个对象中.但是每个kv对在一个对象中必须是唯一的.如果一个key含有多个value值,则最新的value会覆盖之前的值.

  2. key的形式

     标签的key在kv对的左边,key是以字母和数字组成的字符串,可以包含`.`和`-`.大多数docker用于使用其他组织创建的镜像,显示指南帮助你去阻止一时疏忽导致的错误标签,尤其是打算使用自动化配置的时候.

     + 三方工具需要在每个key之前加上前缀,使用域名的反向DNS符号,例如`com.example.some-label`.
     + 不要使用没有拥有者权限的域名
     + `com.docker.*`,`io.docker.*`和`org.dockerproject.*`命名空间用于内部使用
     + 首字母需要小写,且只能包含小写的字母和数字和分隔符`.`,`-`.

  3. value参数设置指南

     标签的value值可以使用字符串表示任何的数据类型,包括但不限于`JSON`,`XML`,`CSV`和`YAML`.value的配置需求是,value需要首先序列化为字符串,使用指定数据结构的原理.例如,将json序列化到字符串中,可以使用`JSON.stringify()`的js方法。

     由于docker没有反序列化value值，访问的时候除非使用三方工具构建,否则不能将json/xml当做嵌套数据结构处理.

  4. 对象标签的管理

     每种支持类型的对象有添加和管理的方法，并使用他们管理对象类型。下述链接可以给与使用标签部署的指导。

     镜像，容器，本地启动器，数据卷，网络的标签对于对象的生命周期来说时静态的。为了修改这些标签，重建对象时，必须要改变这些标签。在swarm节点和swarm服务上的标签需要动态更新。

  5. 标签参考表

     |||

+ 删除未使用的对象

  docker采样保守的方法清除未使用的对象(通常情况下指的是垃圾回收的对象).例如说镜像,容器,数据卷和网络.这些对象除非让docker移除否则是不会移除的.这个会占用额外的磁盘空间.对于每种类型的对象,docker提供了移除`prune`指令.可以使用`docker system prune`清除多个类型的对象.这个话题讨论这个指令的使用.

  1.  移除镜像

     指令`docker image prune`可以清除未使用的镜像,默认状态下,`docker image prune`仅仅清理悬空的镜像.悬空镜像值得是没有标签,且没有被任何容器引用的镜像.

     ```shell
     $ docker image prune
     
     WARNING! This will remove all dangling images.
     Are you sure you want to continue? [y/N] y
     ```

     使用`-a`标签移除所有当前存在容器没有使用的镜像

     ```shell
     $ docker image prune -a
     
     WARNING! This will remove all images without at least one container associated to them.
     Are you sure you want to continue? [y/N] y
     ```

     默认情况下如果你需要继续,为了传输请求,使用`-f`或者`--force`标签.

     可以使用`--filter`标签设置过滤表达式,用于限制移除的镜像,下述示例表达的是创建在24h以前的镜像.

     ```shell
     $ docker image prune -a --filter "until=24h"
     ```

     其他过滤表达式都是可行的,请参考相关的参考文档.

     + [本地文档](http://127.0.0.1:4000/engine/reference/commandline/image_prune/)
     + [远程文档]()

  2. 移除容器

     停止容器的时候,除非使用`--rm`标签,否则是不会自动移除容器的.为了能够查看包含停止的容器在内的容器,可以使用`docker ps -a`标签查看.可以发现有很多的容器存在,尤其是在开发环境下.停止的容器可写层仍然占有磁盘空间.可以使用`docker container prune`指令清除.

     ```shell
     $ docker container prune
     
     WARNING! This will remove all stopped containers.
     Are you sure you want to continue? [y/N] y
     ```

     默认情况下,可以使用`-f`或者`--force`标签强制进行.

     可以使用`--filter`设置过滤表达式,下述示例描述需要移除超过24h以前的容器.

     ```shell
     $ docker container prune --filter "until=24h"
     ```

     其他指令请参考相关文档

     + [本地文档](http://127.0.0.1:4000/engine/reference/commandline/container_prune/)
     + [远程文档]()

  3. 移除数据卷

     数据卷可以被容器使用,占据docker主机的空间.数据卷不可以被自动移除,因为这样做会销毁数据.

     ```shell
     $ docker volume prune
     
     WARNING! This will remove all volumes not used by at least one container.
     Are you sure you want to continue? [y/N] y
     ```

     默认情况下,可以使用`-f`或者`--force`强行执行.

     默认所有未使用的数据卷会被移除,可以使用`--filter`标签设置过滤表达式.下述标签移除了非`keep`标签的数据卷.

     ```shell
     $ docker volume prune --filter "label!=keep"
     ```

     其他指令可参考相关文档

     + [本地文档](http://127.0.0.1:4000/engine/reference/commandline/volume_prune/)
     + [远程文档]()

  4. 移除网络

     docker网络不会占用很多磁盘空间,但是会创建路由表,往前网络设备,可以使用`docker network prune`清除容器未使用的网络资源.

     ```shell
     $ docker network prune
     
     WARNING! This will remove all networks not used by at least one container.
     Are you sure you want to continue? [y/N] y
     ```

     默认可以设置`-f`或者`--force`标签强行执行.可以使用`--filter`设置过滤表达式.下述示例描述的是移除超过24h以前的网络.

     ```shell
     $ docker network prune --filter "until=24h"
     ```

     相关配置参考

     + [本地文档](http://127.0.0.1:4000/engine/reference/commandline/network_prune/)
     + [远程文档]()

  5. 移除所有

     `docker system prune`是移除上述所有的简略表达,17.06.0版本以及之前移除所有.但是之后的版本,必须要指定`--volumes`标签才行.

     ```shell
     $ docker system prune
     
     WARNING! This will remove:
             - all stopped containers
             - all networks not used by at least one container
             - all dangling images
             - all build cache
     Are you sure you want to continue? [y/N] y
     ```

     17.06.0之后的版本

     ```shell
     $ docker system prune --volumes
     
     WARNING! This will remove:
             - all stopped containers
             - all networks not used by at least one container
             - all volumes not used by at least one container
             - all dangling images
             - all build cache
     Are you sure you want to continue? [y/N] y
     ```

+ 格式化命令行和日志输出

**配置启动器**

---



**使用外部工具运行**

---



**配置容器**

---

