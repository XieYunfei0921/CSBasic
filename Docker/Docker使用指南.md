**Docker使用指南**

---

1.  [创建Docker环境](# 创建Docker环境)
2.  [制作镜像并作为容器运行](# 制作镜像并作为容器运行)
3.  [开发环境下设置和使用Kubernates](# 开发环境下设置和使用Kubernates)
4.  [开发环境下设置和使用Swarm](# 开发环境下设置和使用Swarm)
5.  [在Dcoker Hub上共享容器化应用](# 在Dcoker Hub上共享容器化应用)

---

#### 创建Docker环境

1.  docker概念

   docker时给开发人员和系统管理员设置的平台,使用容器构建,分享,和组件的应用.容器使用者部署应用称作**容器化**.容器不是新的,但是他们对于简单部署的应用是新的.

   容器化变得越来越流行是因为:

   +  灵活性: 最复杂的应用都可以被容器化
   +  轻量级: 容器的杠杆作用(leverage)且共享主机内核.从系统资源的角度来说是比**虚拟机**更加的高效.
   +  可携带性: 你可以本地构建,发布到云端,且可以在任何地方运行.
   +  松耦合: 容器具有高度的自效性和密封性.允许在不影响其他容器的情况下允许你去替代和更新它们.
   +  高度伸缩性(scalable): 通过数据中心可以增加或者自动分配容器的副本.
   +  安全性:  容器使用竞争性的限制和隔离措施,使得不需要任何用户端的配置即可允许.

2.  镜像和容器

   从根本上来说,容器仅仅是一个运行的进程,这个进程进行了协议密封措施,主要是为了保证其和其他容器或者主机的**隔离性**.

   容器最重要的隔离措施是,每个容器与它自有的私用文件系统进行交互.docker提供的这种文件系统叫做**镜像**.一个镜像中包含运行任务的所有内容.包括代码,运行时参数,依赖以及其他文件系统对象.

3.  容器和虚拟机(VM)

   容器本地运行在linux上,与其他容器共享主机的内核.运行一个非连续的进程,花费的内存不超过其他可执行应用,使得其轻量化.

   相反的,虚拟机(VM)使用虚拟化运行一个成熟的客户操作系统,通过管理程序获取主机资源.总体来说,虚拟机在运行用户的逻辑应用时,产生了很多开销.

   <img src="E:\截图文件\容器和虚拟机的执行区别.png" style="zoom:67%;" />

4.  编制(Orchestration)

   容器化进程的便携性和可重塑性意味着我们可以移动或者扩大容器化的应用,通过数据中心或者云端.容器高效的保证了这些应用会以相同的方式运行在不同的地方.允许我们快速便捷的使用它们的环境.此外,当我们扩充应用时,需要一些工具去自动维护这些应用,能够自动替换失败的容器,管理更新的部署,在生命周期内部重新配置这些容器.

   用于管理,扩大,和维护容器化应用的工具称作**编制**(Orchestration),通用的两个工具就是Kubernetes和Docekr Swarm. 开发环境通过Docker Destop部署这些编制.这个过程中使用这个编制去创建首个参与编制的容器化应用.

5.  安装docker桌面版本

   启动开发容器化应用的最好范式就是使用Docker Destop,对于OSX和windows系统.桌面工具可以简易的安装k8s和swarm在你本机的开发环境上.你能够使用立刻使用开发应用的编制功能.并不需要集群的参与.安装指导:

   +  [OSX](# https://docs.docker.com/docker-for-mac/install/)
   +  [Windows](# https://docs.docker.com/docker-for-windows/install/)

   

6.  启动Kubernetes

   docker左面可以简单快速的安装K8S,这里只介绍windows系统的

   +  安装完毕docker的桌面工具后,右击图标,选择**Settings -> Kubernetes**.

   +  检查复选框是否*Enable Kubernetes*,点击**Apply**,桌面客户端就会自动的按照k8s.注意这需要大量的时间(20min).安装完成之后点击菜单栏,点击setting,就可以看到一个绿点: ‘Kubernetes is running’.

   +  创建一个pod.yaml去确认k8s已经开始运行.内容如下:

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

     这里描述了单个容器的pod,隔离地址为8.8.8.8

   +  创建pod

     ```shell
      kubectl apply -f pod.yaml
     ```

   +  检查你的pod是否允许起来了

     ```shell
      kubectl get pods
     ```

     你会看到下面的内容

     ```shell
      NAME      READY     STATUS    RESTARTS   AGE
      demo      1/1       Running   0          4s
     ```

   +  检查日志

     ```shell
     kubectl logs demo
     ```

     你可以看到如下内容

     ```shell
      PING 8.8.8.8 (8.8.8.8): 56 data bytes
      64 bytes from 8.8.8.8: seq=0 ttl=37 time=21.393 ms
      64 bytes from 8.8.8.8: seq=1 ttl=37 time=15.320 ms
      64 bytes from 8.8.8.8: seq=2 ttl=37 time=11.111 ms
      ...
     ```

   +  最后删除掉测试pod

     ```shell
     kubectl delete -f pod.yaml
     ```

7.  启动docker Swarm

   这里只介绍windows版本安装

   +  初始化swarm模式

     ```shell
      docker swarm init
     ```

   +  执行成功,你可以看到下述类型信息

     ```shell
     Swarm initialized: current node (tjjggogqpnpj2phbfbz8jd5oq) is now a manager.
      To add a worker to this swarm, run the following command:
          docker swarm join --token SWMTKN-1-3e0hh0jd5t4yjg209f4g5qpowbsczfahv2dea9a1ay2l8787cf-2h4ly330d0j917ocvzw30j5x9 192.168.65.3:2377
      To add a manager to this swarm, run 'docker swarm join-token manager' and follow the instructions.
     ```

   +  运行简单的docker服务,使用alpine-based文件系统

     ```shell
      docker service create --name demo alpine:3.5 ping 8.8.8.8
     ```

   +  检查运行配置

     ```shell
      docker service ps demo
     ```

     可以看到如下内容

     ```shell
     ID                  NAME                IMAGE               NODE                DESIRED STATE       CURRENT STATE           ERROR               PORTS
      463j2s3y4b5o        demo.1              alpine:3.5          docker-desktop      Running             Running 8 seconds ago
     ```

   +  检查日志

     ```shell
     docker service logs demo
     ```

     可以看到如下日志

     ```shell
     demo.1.463j2s3y4b5o@docker-desktop    | PING 8.8.8.8 (8.8.8.8): 56 data bytes
      demo.1.463j2s3y4b5o@docker-desktop    | 64 bytes from 8.8.8.8: seq=0 ttl=37 time=13.005 ms
      demo.1.463j2s3y4b5o@docker-desktop    | 64 bytes from 8.8.8.8: seq=1 ttl=37 time=13.847 ms
      demo.1.463j2s3y4b5o@docker-desktop    | 64 bytes from 8.8.8.8: seq=2 ttl=37 time=41.296 ms
      ...
     ```

   +  删掉测试服务

     ```shell
     docker service rm demo
     ```

#### 制作镜像并作为容器运行

1.  介绍

   既然在开发环境中,以及获取了编制工具.我们就可以开始开发容器化应用了.总体来说,开发工作流如下:

   >1.  首次创建镜像时,创建你的应用每个组件的测试容器
   >2.  组装你的容器,支持基础设置到一个完成的应用中,可以使用Dcoker Stack File或者 K8S YAML来表述.
   >3.  测试,共享和发布你已经完成的容器化应用.

   在这里,关注工作流的第一步,创建容器基于的镜像.记住,docker镜像捕捉私有文件系统.我们的容器化进程就会运行在这个文件系统上.所以我们只需要创建一个镜像,这个镜像仅仅包含应用需要运行的资源即可.

   > 一旦你学会了如何端构建镜像,容器化的开发环境很容易搭建.因为容器化开发环境会隔离所有依赖,这些依赖是你的程序需要的且在docker镜像中.这种情况下,你能简单的开发不同类型的应用,而不用改变开发机器上的环境.

2. 创建

   +  克隆一个示例项目

     ```shell
     git clone -b v1 https://github.com/docker-training/node-bulletin-board
     cd node-bulletin-board/bulletin-board-app
     ```

     这是一个简单的看板程序,使用node.js写的.示例中,你不需要对它容器化,开始想象你写的app.

   +  看看文件的DockerFile,这个文件描述了如何去组装文件系统,你也可以加入一些元数据,用于描述在镜像中如何去运行容器.这个App的Dockerfile如下

     ```dockerfile
     FROM node:6.11.5    
     
     WORKDIR /usr/src/app
     COPY package.json .
     RUN npm install    
     COPY . .
     
     CMD [ "npm", "start" ]    
     ```

     写Dockerfile的第一步时去对应用进行容器化.你可以考虑这些指令一步一步执行,内容是关于如何构建镜像.这个任务包含如下几步:

     ```dockerfile
     # 获取node:6.11.5的镜像,这是一个官方镜像,使用nodeJs构建.进过docker验证,成为一个高质量的镜像,包含node:6.11.5 和一些基本的依赖
     FROM node:6.11.5    
     # WORKDIR 指定子动作来自于的目录位置为映像文件系统目录 /usr/src/app(不是主机的文件系统)
     WORKDIR /usr/src/app
     # 拷贝文件 package.json 到你主机映像当前位置(.)[这种情况下为/usr/src/app/package.json]
     COPY package.json .
     # 在你的镜像文件系统中安装依赖
     RUN npm install    
     # 拷贝应用中其他源码,从主机拷贝到镜像文件系统中
   COPY . .
     
     CMD [ "npm", "start" ]    
     ```
     
     你可注意到这些大多都是相同的步骤(与你在主机上操作和安装的步骤).但是使用dockerfile可以允许我们去做同样的事情在一个可移动的,相互隔离的docker镜像中.
     
     上述步骤组件了镜像的文件系统.大叔还有最后一条命令.CMD指令是第一个指定元数据信息到镜像中的指令.描述了如何基于这个镜像运行容器.在这种情况下,镜像下容器化进程意味着支持`npm start` 指令.
     
     上述值组件dockerfile的简单例子,总是以`FROM` 指令开头,下面就是用于描述你新建的私有文件系统信息,且包含溢写需要指定的元数据信息.更多的docker指令,请参考: [docker指令](# https://docs.docker.com/engine/reference/builder/)

3. 构建和测试镜像

   既然以及有源码和docker文件.这时候就可以构建第一个镜像了,确保容器运行在正常状态下.

   以看板程序为示例:

   +  确保你在目录`node-bulletin-board/bulletin-board-app` 下,使用终端工具,构建镜像

     ```shell
     docker image build -t bulletinboard:1.0 .
     ```

     你可以看到dockerfile中每条指令的执行过程信息,如果成功则会看到如下信息

     ``Successfully tagged bulletinboard:1.0`.`

     注意: windows用户在这一步会受到安全警告信息(显示的是你没有对指定文件的权限).这个示例中忽视这个信息.

   +  在镜像下运行容器

     ```shell
     docker container run --publish 8000:8080 --detach --name bb bulletinboard:1.0
     ```

     在这里使用下列标记:

     > +  --publish : 将主机的8000端口迁移到容器的8000端口(容器具有自己的端口集合)
     > +  --detach : 询问docker是否后台运行容器
     > +  --name : 指定名称为bb(这条指令)

   +  浏览器访问`localhost:8000` .你就可以看到看板程序运行的情况,这一步我们需要保证容器运行正常,现在,就可以有时间运行单元测试了.

   +  当你觉得运行的没问题,就可以删除这个容器了

     ```shell
     docker container rm --force bb
     ```

#### 开发环境下设置和使用Kubernates



#### 开发环境下设置和使用Swarm



#### 在Dcoker Hub上共享容器化应用