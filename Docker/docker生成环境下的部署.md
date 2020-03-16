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

  可以操作指定输出格式的指令和日志驱动器用于获取模板。

  docker提供一系列操作模板元素的函数。这些都使用`docker inspect`指令,但是许多客户端指令有`--format`标签,且客户端指令引用包含输出形式客户端的引用.

  1. join

     `join`连接字符串列表,用于创建单个字符串,在每个元素之间使用分隔符连接.

     ```shell
     $ docker inspect --format '{{join .Args " , "}}' container
     ```

  2. json

     `json`使用json字符串编码元素

     ```shell
     $ docker inspect --format '{{json .Mounts}}' container
     ```

  3. lower

     `lower`将一个字符串转换成小写形式

     ```shell
     $ docker inspect --format "{{lower .Name}}" container
     ```

  4. split

     `split`按照分隔符将字符串进行分割

     ```shell
     $ docker inspect --format '{{split .Image ":"}}'
     ```

  5. title

     `title`对首字符大写处理

     ```shell
     $ docker inspect --format "{{title .Name}}" container
     ```

  6. upper

     `upper`将字符串转化为大写

     ```shell
     $ docker inspect --format "{{upper .Name}}" container
     ```

  7. println

     `println`打印新行的每个value

     ```shell
     $ docker inspect --format='{{range .NetworkSettings.Networks}}{{println .IPAddress}}{{end}}' container
     ```

     为了查看什么数据打印了处理,可以使用json显示

     ```shell
     $ docker container ls --format='{{json .}}'
     ```

**配置启动器**

---

+ 配置并运行docker

  成功启动docker之后,docker按照默认配置启动,这个话题讨论如何自定义配置,手动的操作启动器,并对启动的问题作出讨论.

  1.  使用操作系统应用启动

     On a typical installation the Docker daemon is started by a system utility, not manually by a user. This makes it easier to automatically start Docker when the machine reboots.

     The command to start Docker depends on your operating system. Check the correct page under [Install Docker](http://127.0.0.1:4000/install/). To configure Docker to start automatically at system boot, see [Configure Docker to start on boot](http://127.0.0.1:4000/install/linux/linux-postinstall/#configure-docker-to-start-on-boot).

     docker在典型安装情况下使用了系统应用.不是有用户手动操作的.这个使得重启的时候自动启动docker更简单.

     这个指令依靠操作系统启动docker.检查安装时的错误页.系统如何的启动docker,请参考:

     + [本地教程](http://127.0.0.1:4000/install/linux/linux-postinstall/#configure-docker-to-start-on-boot)
     + [远程教程]()

  2. 手动启动

     如果不想使用系统应用管理docker启动器,或者仅仅只是测试一切东西.可以使用docker命令手动启动.可以使用`sudo`(依赖于操作系统配置).

     当安装这个方法启动docker的时候,这个运行在前台,并直接将日志发送到前台.

     ```shell
     $ dockerd
     
     INFO[0000] +job init_networkdriver()
     INFO[0000] +job serveapi(unix:///var/run/docker.sock)
     INFO[0000] Listening for HTTP on unix (/var/run/docker.sock)
     ```

     如果需要停止,使用`Ctrl+C`指令.

  3. 配置docker启动器

     有两种方式配置docker启动器

     + 使用json配置文件,这个是可选的配置,因为其将所有的配置保存在单个位置上
     + 启动`dockerd`时使用标签

     可以同时使用这两个标签,只要你没有将标签指向json文件即可.如果这么做了,那么docker不会启动.并会打印错误信息.

     为了配置json文件,可以创建`/etc/docker/daemon.json`的linux文件,或者`C:\ProgramData\docker\config\daemon.json`的windows文件.

     下面是文件的配置文件示例:

     ```shell
     {
       "debug": true,
       "tls": true,
       "tlscert": "/var/docker/server.pem",
       "tlskey": "/var/docker/serverkey.pem",
       "hosts": ["tcp://192.168.59.3:2376"]
     }
     ```

     当docker运行在debug模式下的时候,使用TLS监听端口`192.168.59.3:2376`.可以学习一下如何配置

     + [本地教程](http://127.0.0.1:4000/engine/reference/commandline/dockerd/#daemon-configuration-file)
     + [远程教程]()

     可以手动启动docker启动器,并使用标签配置.这样有利于问题的定位.

     下述是docker启动器的配置,使用同样的配置:(标签配置法)

     ```shell
     dockerd --debug \
       --tls=true \
       --tlscert=/var/docker/server.pem \
       --tlskey=/var/docker/serverkey.pem \
       --host tcp://192.168.59.3:2376
     ```

     可以学习如何配置参数,或者运行:

     ```shell
     $ dockerd --help
     ```

     指定的配置参数在docker文档中指定,可以参考下述文件:

     本地文件:

     1. [自动启动容器](http://127.0.0.1:4000/engine/admin/host_integration/)
     2. [限制容器的资源](http://127.0.0.1:4000/engine/admin/resource_constraints/)
     3. [配置存储驱动器](http://127.0.0.1:4000/engine/userguide/storagedriver/)
     4. [容器安全](http://127.0.0.1:4000/engine/security/)

  4. docker 启动目录

     docker启动器将所有数据持久化到单个目录中，着了可以定位所有关于docker的内容，包括容器，镜像，数据卷，服务定义和密钥信息。

     默认情况下目录是

     + linux为`/var/lib/docker`
     + windows下目录为`C:\ProgramData\docker`

     可以使用不同的目录配置docker启动器。使用`data-root`配置属性.

     由于docker主机的状态保存在这个目录上,确保每个启动器可以使用独有的目录.如果两个启动器共享一个目录,例如NFS文件系统共享,那么就会碰到难以解决的bug.

  5. 启动器问题的定位

     可以开启启动器上的debug功能,这样就可以了解运行时启动器的活动,有利于问题定位.如果驱动器完成时无响应的,可以对所有线程进行栈追踪,用于通过发送`SIGUSR`信号到启动器中,从而添加到启动器日志中.

  6. 处理`daemon.json`和启动脚本之间的矛盾

     使用`daemon.json`文件将配置传递到`dockerd`指令中或者使用启动脚本,这些配置时可能产生矛盾点,报错如下:

     ```shell
     If you use a daemon.json file and also pass options to the dockerd command manually or using start-up scripts, and these options conflict, Docker fails to start with an error such as:
     ```

     遇到上面的错误(或者类似的),表示你正在手动配置标签,需要调整标签或者移除`daemon.json`文件.

     如果使用操作系统启动docker,可以重写这些脚本中的默认值.

  7. 在`daemon.json`文件中使用系统配置的host

     值得注意的配置矛盾示例是当你指定不同的启动地址的时候,很难去定位.docker使用socket去定位.在debian或者ubuntu系统中使用`systemd`.这个一位置主机标签`-H`总是在启动`dockerd`时候使用.如果你在`daemon.json`中使用`hosts`键值对,这就会导致启动失败.

     为了解决这个问题,创建一个新文件`/etc/systemd/system/docker.service.d/`

     `docker.conf`(使用下述配置)，去移除`-H`参数.用于启动默认的启动器.

     ```shell
     [Service]
     ExecStart=
     ExecStart=/usr/bin/dockerd
     ```

     当你使用``systemd`配置docker的时候,可以配置HTTP或者HTTPS代理

     在请求启动docker之前,使用`sudo systemctl daemon-reload`指令,如果docker启动成功,`damon.json`中配置的`hosts`中指定监听的ip地址而非是使用socket.

  8. OOM异常

     如果容器请求超过系统可用的内存,将会引发OOM.如果你经历了OOM.且容器或者启动器可能被内核OOM中断.为了防止这个发生.确保应用运行的主机拥有足够的内存

  9.  读取日志

     启动器日志可能会帮助你诊断问题,日志会保存在溢写位置上,主要是依靠操作系统的配置.

     | 操作系统               | 位置                                                         |
     | ---------------------- | ------------------------------------------------------------ |
     | RHEL,Oracle Linux      | `/var/log/messages`                                          |
     | Debian                 | `/var/log/daemon.log`                                        |
     | Unbuntu 16.04+ ,Centos | 使用指令`journalctl -u docker.service`                       |
     | Ubuntu 14.10-          | `/var/log/upstart/docker.log`                                |
     | macOS(Docker 18.01+)   | `~/Library/Containers/com.docker.docker/Data/vms/0/console-ring` |
     | macOS(Docker 18.01-)   | `~/Library/Containers/com.docker.docker/Data/com.docker.driver.amd64-linux/console-ring` |
     | Windows                | `AppData\Local`                                              |

  10. 启动调试

      有如下两种方式开启调试,推荐的方法是在`daemon.json`中设置`debug`为`true`.在每个docker平台上都可以使用。

      1. 编辑`daemon.json`文件,通常位于`/etc/docker/`目录中,可能需要创建这个文件.在mac或者windows平台上,不能直接修改,跳转到**Preferences** / **Daemon** / **Advanced**设置。

      2. 这个文件是空的，添加下述内容

         ```shell
         {
           "debug": true
         }
         ```

         如果文件包含json,仅仅添加`"debug":true`即可,注意json的格式.同时需要确认`log-level`是否设置,将其设置为`info`或者`debug`.默认状况下为`info`或者`debug`.可以选择的还有`warn`,`error`,和`fetal`.

      3. 发送`HUP`信号到启动器,会导致配置的重载,在linux上使用如下指令:

         ```shell
         $ sudo kill -SIGHUP $(pidof dockerd)
         ```

         在windows上重启docker即可.

         ​	可以停止docker启动器，并手动进行重启（使用debug标签`-D`）.范式这个可能导致docker使用不同环境变量重启,这样会使调试根据困难.

  11. 打印栈追踪日志

      如果启动器没有相应,可以发送`SIGUSR1`去获取栈追踪信息

      + linux

      ```shell
      $ sudo kill -SIGUSR1 $(pidof dockerd)
      ```

      + windows
        1. 下载[docker-signal]()
        2. 获取dockerd的进程编号
        3. 使用执行器标签`--pid=<PID of daemon>

      这个会记录栈追踪信息,但是不会停止启动器,启动器日志会显示栈追踪信息,或者包含栈追踪的文件路径(如果记录到文件的情况下).

      启动器在处理`SIGUSER1`信号之后会继续操作,且将栈追踪信息记录到日志中.栈追踪可以使用与决定所有启动器的线程和管理信息.

  12. 检查docker是否在运行

      操作系统检查docker是否允许的方法时询问docker,使用`docker info`指令。

      可以使用操作系统应用，比如`sudo systemctl is-active docker`或者`sudo status docker`或者`sudo service docker status`检查windows应用的服务状态.

      最后,可以检查docker进程的进程列表,使用`ps`或者`top`之类的指令.

+ 使用系统命令控制docker

  许多linux发行版都使用系统命令启动docker启动器,这个文档会演示如何去配置.

  1. 启动docker启动器

     + 手动启动

       一旦docker安装,需要启动docker启动器,大多数linux发行版使用`systemctl`启动服务.如果没有`systemctl`则使用`service`指令.

       ```shell
       # systemctl
       $ sudo systemctl start docker
       # service
       $ sudo service docker start
       ```

     + 系统自动启动

       参考下面的docker的开机启动

       <http://127.0.0.1:4000/install/linux/linux-postinstall//#configure-docker-to-start-on-boot>

     + 配置docker启动器属性

       有许多配置启动器标签和环境变量的方式,推荐使用平台无关性较好的`daemon.json`配置方式.位于`/etc/docker`目录下.参考配置方法:

       + [本地教程](http://127.0.0.1:4000/engine/reference/commandline/dockerd//#daemon-configuration-file)
       + [远程教程]()

       可以使用`daemon.json`配置启动配置下述示例使用了两个配置.不能使用`daemon.json`配置的是HTTP代理.

     + 运行时目录和存储驱动器的配置
     
       设置下述`daemon.json`文件,用于控制docker镜像,容器,数据卷的操盘使用空间.通过将其移动到分开的分区中.
     
       ```shell
       {
           "data-root": "/mnt/docker-data",
           "storage-driver": "overlay2"
       }
       ```
     
     + HTTP/HTTPS代理
     
       docker启动器使用`HTTP_PROXY`,`HTTPS_PROXY`,和`NO_PROXY`环境变量,用于配置HTTP/HTTPS的环境变量.不能使用`daemon.json`配置环境变量.
     
       下述示例覆盖了默认的`docker.service`文件.
     
       如果在HTTP/HTTPS的后台服务器上,可以添加这个配置到docker 系统服务文件中.
     
       1.  创建docker服务的drop-in目录
     
          ```shell
          $ sudo mkdir -p /etc/systemd/system/docker.service.d
          ```
     
       2. 创建`/etc/systemd/system/docker.service.d/http-proxy.conf`文件,并配置`HTTP_PROXY`属性
     
          ```shell
          [Service]
          Environment="HTTP_PROXY=http://proxy.example.com:80/"
          ```
     
          如果在HTTPS代理服务器后面,创建`/etc/systemd/system/docker.service.d/https-proxy.conf`文件,添加`HTTP_PROXY`环境变量.
     
          ```shell
          [Service]
          Environment="HTTPS_PROXY=https://proxy.example.com:443/"
          ```
     
       3. 如果有内部docker注册器,需要不使用代理进行连接,可以指定`NO_PROXY`环境变量.
     
          `NO_PROXY`环境变量指定一个包含逗号分隔符的字符串,这些参数处理主机名都可以指定.
     
          + ip地址(例如:`1.2.3.4`)
          + 域名或者特殊的DNS标签(*)
          + 域名和所有子域名,首字符`.`仅仅匹配子域名,例如`foo.example.com`或者`example.com`
            + `example.com`匹配与`example.com`和`foo.example.com`
            + `.example.com`仅仅会匹配`foo.example.com`
          + 通配符(*)表名没有代理
          + 接受主机:端口号和域名:端口号的配置
     
          配置示例
     
          ```shell
          [Service]    
          Environment="HTTP_PROXY=http://proxy.example.com:80/" "NO_PROXY=localhost,127.0.0.1,docker-registry.example.com,.corp"
          ```
     
          在HTTP代理服务器上这样配置
     
          ```shell
          [Service]    
          Environment="HTTPS_PROXY=https://proxy.example.com:443/" "NO_PROXY=localhost,127.0.0.1,docker-registry.example.com,.corp"
          ```
     
       4. 刷新变化
     
          ```shell
          $ sudo systemctl daemon-reload
          ```
     
       5. 重启docker
     
          ```shell
          $ sudo systemctl restart docker
          ```
     
       6. 验证载入的配置
     
          ```shell
          $ systemctl show --property=Environment docker
          Environment=HTTP_PROXY=http://proxy.example.com:80/
          ```
     
          在HTTPS代理服务器的验证
     
          ```shell
          $ systemctl show --property=Environment docker
          Environment=HTTPS_PROXY=https://proxy.example.com:443/
          ```
     
     + 配置docker监听连接
     
       + [本地教程](# http://127.0.0.1:4000/install/linux/linux-postinstall/#control-where-the-docker-daemon-listens-for-connections)
       + [远程教程]()

**使用外部工具运行**

---

+ 三方监视工具

  可以使用三方工具监控docker

  - [使用Prometheus收集docker的计量信息](http://127.0.0.1:4000/config/thirdparty/prometheus/)
  - [docker发行版使用Sysdig 监控方案](https://success.docker.com/article/sysdig-monitoring)
  - [docker发行版使用DataDog 监控方案](https://success.docker.com/article/datadog-monitoring)

+ 使用Prometheus收集计量信息

  Prometheus 是开源系统监控器且拥有警告工具箱，可以以Prometheus配置docker，这里讨论如何去配置docker，创建Prometheus，使用docker容器运行。使用Prometheus监视docker实例。

  1. 配置docker

     配置docker作为Prometheus值，需要指定`metrics-address`,最好的方式时配置`daemon.json`,这个位于如下位置:

     + linux: `/etc/docker/daemon.json`

     + windows:`C:\ProgramData\docker\config\daemon.json`

     + docker桌面版:

       点击选择**Preferences**>**Daemon**>**Advanced**

     设置配置

     ```shell
     {
       "metrics-addr" : "127.0.0.1:9323",
       "experimental" : true
     }
     ```

  2. 配置和运行Prometheus

     Prometheus以docker服务的形式运行在docker swarm上。
  
     下述配置文件的内容到`/tmp/prometheus.yml`上,或者windows目录`C:\tmp\prometheus.yml`上，这个Prometheus的配置排除了底层docker文件的添加。docker桌面版的配置稍有不同。
  
     ```shell
     # linux版本配置
     # my global config
     global:
       scrape_interval:     15s # Set the scrape interval to every 15 seconds. Default is every 1 minute.
       evaluation_interval: 15s # Evaluate rules every 15 seconds. The default is every 1 minute.
       # scrape_timeout is set to the global default (10s).
     
       # Attach these labels to any time series or alerts when communicating with
       # external systems (federation, remote storage, Alertmanager).
       external_labels:
           monitor: 'codelab-monitor'
     
     # Load rules once and periodically evaluate them according to the global 'evaluation_interval'.
     rule_files:
       # - "first.rules"
       # - "second.rules"
     
     # A scrape configuration containing exactly one endpoint to scrape:
     # Here it's Prometheus itself.
     scrape_configs:
       # The job name is added as a label `job=<job_name>` to any timeseries scraped from this config.
       - job_name: 'prometheus'
     
         # metrics_path defaults to '/metrics'
         # scheme defaults to 'http'.
     
         static_configs:
           - targets: ['localhost:9090']
     
       - job_name: 'docker'
              # metrics_path defaults to '/metrics'
              # scheme defaults to 'http'.
     
         static_configs:
           - targets: ['localhost:9323']
     ```
  
     ```shell
     # mac桌面版本配置
     # my global config
     global:
       scrape_interval:     15s # Set the scrape interval to every 15 seconds. Default is every 1 minute.
       evaluation_interval: 15s # Evaluate rules every 15 seconds. The default is every 1 minute.
       # scrape_timeout is set to the global default (10s).
     
       # Attach these labels to any time series or alerts when communicating with
       # external systems (federation, remote storage, Alertmanager).
       external_labels:
           monitor: 'codelab-monitor'
     
     # Load rules once and periodically evaluate them according to the global 'evaluation_interval'.
     rule_files:
       # - "first.rules"
       # - "second.rules"
     
     # A scrape configuration containing exactly one endpoint to scrape:
     # Here it's Prometheus itself.
     scrape_configs:
       # The job name is added as a label `job=<job_name>` to any timeseries scraped from this config.
       - job_name: 'prometheus'
     
         # metrics_path defaults to '/metrics'
         # scheme defaults to 'http'.
     
         static_configs:
           - targets: ['docker.for.mac.localhost:9090']
     
       - job_name: 'docker'
              # metrics_path defaults to '/metrics'
              # scheme defaults to 'http'.
     
         static_configs:
           - targets: ['docker.for.mac.host.internal:9323']
     ```
  
     ```shell
     # windows桌面版本
     # my global config
     global:
       scrape_interval:     15s # Set the scrape interval to every 15 seconds. Default is every 1 minute.
       evaluation_interval: 15s # Evaluate rules every 15 seconds. The default is every 1 minute.
       # scrape_timeout is set to the global default (10s).
     
       # Attach these labels to any time series or alerts when communicating with
       # external systems (federation, remote storage, Alertmanager).
       external_labels:
           monitor: 'codelab-monitor'
     
     # Load rules once and periodically evaluate them according to the global 'evaluation_interval'.
     rule_files:
       # - "first.rules"
       # - "second.rules"
     
     # A scrape configuration containing exactly one endpoint to scrape:
     # Here it's Prometheus itself.
     scrape_configs:
       # The job name is added as a label `job=<job_name>` to any timeseries scraped from this config.
       - job_name: 'prometheus'
     
         # metrics_path defaults to '/metrics'
         # scheme defaults to 'http'.
     
         static_configs:
           - targets: ['docker.for.win.localhost:9090']
     
       - job_name: 'docker'
              # metrics_path defaults to '/metrics'
              # scheme defaults to 'http'.
     
         static_configs:
           - targets: ['192.168.65.1:9323']
     ```
  
     下一步使用配置文件启动Prometheus服务.
  
     ```shell
     # linux
     $ docker service create --replicas 1 --name my-prometheus \
         --mount type=bind,source=/tmp/prometheus.yml,destination=/etc/prometheus/prometheus.yml \
         --publish published=9090,target=9090,protocol=tcp \
         prom/prometheus
     ```
  
     ```shell
     # mac桌面版本
     $ docker service create --replicas 1 --name my-prometheus \
         --mount type=bind,source=/tmp/prometheus.yml,destination=/etc/prometheus/prometheus.yml \
         --publish published=9090,target=9090,protocol=tcp \
         prom/prometheus
     ```
  
     ```shell
     # windows桌面版
     PS C:\> docker service create --replicas 1 --name my-prometheus
         --mount type=bind,source=C:/tmp/prometheus.yml,destination=/etc/prometheus/prometheus.yml
         --publish published=9090,target=9090,protocol=tcp
         prom/prometheus
     ```
  
  3. 使用Prometheus
  
     创建一个图,点击PrometheusUI中的`Graphs`.选择度量,右键执行.
  
     上述图显示了空载docker实例,这里的图可能和在运行中激活的负载有一些不同.
  
     为了使图更加生动,通过启动任务,这些任务仅仅ping Docker,创建一些网络动作.

**配置容器**

---

1.  自动启动容器

   docker提供重启策略,用于控制容器退出时自动启动.重启策略保证了连接容器按正确的顺序进行重启.docker建议使用重启策略,并没使用进程管理器启动容器.

   重启策略不用于`--live-restore`标签和`docker`指令.使用`--live-restore`可以是容器在docker升级的时候保持运行.尽管这个时候网络和用户输入都停止了.

   + 使用重启策略

     在使用`docker run`指令的同时,使用`--restart`标签配置容器的重启策略,重启标签可以参照如下设置.

     | 标签名称      | 描述                                                         |
     | ------------- | ------------------------------------------------------------ |
     | `no`          | 不会自动重启容器(默认)                                       |
     | `no-failure`  | 如果因为失败退出,则重启容器.这个会发生在退出码不是0的时候.   |
     | `always`      | 停止的时候总是重启容器,如果手动停止,只会在docker启动器重启或者容器自身重启的时候重启. |
     | `unless-stop` | 类似`always`除非容器停止,否则docker启动器重启之后就会重启    |

     下述示例启动了Redis容器,且配置总是重启,除非容器停止或者docker重启.

     ```shell
     $ docker run -dit --restart unless-stopped redis
     ```

   + 重启策略细节

     重启策略有下述特征

     - 重启策略只对成功启动的容器有效,这种情况下,成功启动意味着容器至少启动了10s,且docker启动了监听.阻止没有启动的容器的重启.
     - 如果手动的停止容器,重启策略就会被忽视,直到docker启动器重启或者容器手动重启才能生效.这个用于组织形成重启环的形成.
     - 重启策略仅仅应用在容器中,swarm服务的重启策略配置方式不同.详情参考[相关文档](http://127.0.0.1:4000/engine/reference/commandline/service_create/)

   + 使用进程管理器

     如果重启策略不满足你的需求,比如在docker之外需要依赖docker容器.可以使用进程管理[`upstart`](http://upstart.ubuntu.com/),[`systemd`](http://freedesktop.org/wiki/Software/systemd/)或者[`supervisor`](http://supervisord.org/)。

     > 注意: 不要将重启策略和host级别的进程管理器配置在一起,因为会引起冲突

     为了能够使用进程管理器,配置进程管理器,使用`docker start`或者`docker service`用于启动容器或者服务.可以参考进程管理器的文档寻找更多的细节.

   + 在容器内部使用进程管理器

     无论进程处于运行状态,启动或者重启的状态,进程管理器都可以在容器内部运行.

2. 保持容器在启动器宕机的时候仍旧运行

   默认情况下,docker启动器停止的时候,会关闭运行的容器.在docker引擎1.12之后,可以配置启动器,这样容器就可以在启动器不可用的时候保持运行.这个功能就做**存活恢复**.存活恢复可以减少容器由于启动器故障导致的宕机时间.

   + 开启存活恢复

     有两种方式开启存活恢复,只需要做其中一种就可以了

     + 添加配置文件到启动配置文件中,linux上,默认在`/etc/docker/daemon.json`中.在docker桌面版中选择**Preferences** -> **Daemon** -> **Advanced**配置

       + 使用json开启`live-store`

         ```shell
         {
           "live-restore": true
         }
         ```

       + 重启docker启动器,linux上可以通过重载启动器避免重启.如果使用`systemd`,使用`systemctl reload docker`.否则需要发送`SIGHUP`消息给`dockerd`进程.

   + 升级开启存活恢复

     存活恢复支持容器在启动器升级的时候保持运行.尽管这里有版本限制且不支持低版本的更新.如果在升级期间放弃发布,启动器不会恢复容器的链接.如果不能恢复链接,就不能管理运行中的容器,所以就必须要手动的进行管理.

   + 重启的存活恢复

     存活恢复仅仅在恢复容器的情况下工作,比如说网桥IP地址和图式驱动器没有改变的情况下才能进行恢复.如果启动器级别配置发生改变的时候,存活恢复就不能工作流,必须要手动的关闭容器.

   + 运行容器存活恢复的影响

     如果启动器长期宕机,运行容器可能填满FIFO日志(启动器需要读取的日志).日志满了之后就会阻塞容器进行日志的记录.默认缓冲大小为64K,必须要重启docker用于刷新.

     linux上可以修改内核缓冲区,通过改变`/proc/sys/fs/pipe-max-size`.不能修改桌面版本的缓冲区大小.

   + 存活恢复和swarm模式的关系

     存活恢复只能工作在独立容器中,不能使用在swarm服务中.

3. 容器中运行多个服务

   容器的主要运行进程在`ENTRYPOINT`或者`CMD`中,位于dockerfile的末尾.建议每个容器使用一个服务,并将其分开.服务可以fork到多个进程中,但是为了获取docker的优势,避免一个容器处理多方面的应用.可以使用用户定义的网络和数据卷连接多个容器.

   容器的主进程需要管理器所有启动的进程,一些情况下,主进程没有设计很好的时候,且没有处理好子进程关闭文档的时候.可以在运行容器的时候使用`--init`标签,这个标签添加一个小起始进程,且在容器退出的时候处理所有进程的释放.处理进程的方式优先于使用完全初始化进程(`sysvinit`,`upstart`,或者`systemd`)用于处理容器的进程生命周期.

   如果需要在一个容器中运行超过一个服务,必须按照下述集中方式完成:

   1.  将所有指令放置在一个包装脚本中,使用测试或者debug信息进行完成.类似`CMD`指令的运行包装脚本.首先给支包装脚本.

      ```shell
      #!/bin/bash
      
      # Start the first process
      ./my_first_process -D
      status=$?
      if [ $status -ne 0 ]; then
        echo "Failed to start my_first_process: $status"
        exit $status
      fi
      
      # Start the second process
      ./my_second_process -D
      status=$?
      if [ $status -ne 0 ]; then
        echo "Failed to start my_second_process: $status"
        exit $status
      fi
      
      # Naive check runs checks once a minute to see if either of the processes exited.
      # This illustrates part of the heavy lifting you need to do if you want to run
      # more than one service in a container. The container exits with an error
      # if it detects that either of the processes has exited.
      # Otherwise it loops forever, waking up every 60 seconds
      
      while sleep 60; do
        ps aux |grep my_first_process |grep -q -v grep
        PROCESS_1_STATUS=$?
        ps aux |grep my_second_process |grep -q -v grep
        PROCESS_2_STATUS=$?
        # If the greps above find anything, they exit with 0 status
        # If they are not both 0, then something is wrong
        if [ $PROCESS_1_STATUS -ne 0 -o $PROCESS_2_STATUS -ne 0 ]; then
          echo "One of the processes has already exited."
          exit 1
        fi
      done
      ```

      下一步设置dockerfile

      ```dockerfile
      FROM ubuntu:latest
      COPY my_first_process my_first_process
      COPY my_second_process my_second_process
      COPY my_wrapper_script.sh my_wrapper_script.sh
      CMD ./my_wrapper_script.sh
      ```

   2. 如果有一个需要首先启动的主进程,且保持运行但是需要短暂地需要运行其他进程(可能是需要与主进程进行通信).可以使用bash进行控制

      ```shell
      #!/bin/bash
      
      # turn on bash's job control
      set -m
      
      # Start the primary process and put it in the background
      ./my_main_process &
      
      # Start the helper process
      ./my_helper_process
      
      # the my_helper_process might need to know how to wait on the
      # primary process to start before it does its work and returns
      
      
      # now we bring the primary process back into the foreground
      # and leave it there
      fg %1
      ```

      设置dockerfile

      ```dockerfile
      FROM ubuntu:latest
      COPY my_main_process my_main_process
      COPY my_helper_process my_helper_process
      COPY my_wrapper_script.sh my_wrapper_script.sh
      CMD ./my_wrapper_script.sh
      ```

   3. 使用进程管理器`supervisord`,因为需要打包`supervisord`.镜像中的配置,和其管理的不同应用.所以这个管理器是重量级别的.下述是一个使用`supervisord`管理器进程的示例.假定预先写了`supervisord.conf`,`my_first_process`和`my_second_process`文件,都位于同一个目录中.

      ```dockerfile
      FROM ubuntu:latest
      RUN apt-get update && apt-get install -y supervisor
      RUN mkdir -p /var/log/supervisor
      COPY supervisord.conf /etc/supervisor/conf.d/supervisord.conf
      COPY my_first_process my_first_process
      COPY my_second_process my_second_process
      CMD ["/usr/bin/supervisord"]
      ```

4. 容器运行时计量值

   