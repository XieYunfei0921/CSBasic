#### 操作系统要求

> 1. 安装Docker引擎需要centos 7版本
>
> 2.  centos-extras库必须可以使用,默认状态下可以使用,如果你关闭过,请重新开启
> 3.  建议使用`overlay2存储驱动器

#### 卸载旧版本

卸载时需要将其依赖也一并卸载

```shell
sudo yum remove docker \
                  docker-client \
                  docker-client-latest \
                  docker-common \
                  docker-latest \
                  docker-latest-logrotate \
                  docker-logrotate \
                  docker-engine
```

卸载完成之后,yum会提示没有这个包

但是/var/lib/docker/ 目录下,镜像(images),容器,数据卷(volumes),和网络(networks)依旧保留

#### 安装Docker引擎

可以通过如下方式安装Docker

1.  安装Docker的镜像库

   +  创建库

     安装所需要的包,其中yum-utils提供yum-config-manager,而@devicemapper 需要lvm2和device-mapper-persistent-data

     ```shell
     sudo yum install -y yum-utils \
       device-mapper-persistent-data \
       lvm2
     ```

   +  建立稳定的库

     ```shell
     sudo yum-config-manager \
         --add-repo \
         https://download.docker.com/linux/centos/docker-ce.repo
     ```

   +  可选参数配置

     1.  运行夜间模式

        ```shell
        sudo yum-config-manager --enable docker-ce-nightly
        ```

     2.  运行测试通道

        ```shell
        sudo yum-config-manager --enable docker-ce-test
        ```

     3.  取消配置示例

        ```shell
        sudo yum-config-manager --disable docker-ce-nightly
        ```

2. 下载RPM包,手动安装,且手动管理更新

   +  下载最新版本的docker引擎,如果需要安装指定版本的向下看

     ```shell
     sudo yum install docker-ce docker-ce-cli containerd.io
     ```

     如果需要GPG 密钥,验证消息与060A 61C5 1B55 8A7F 742B 77AA C52F EB6B 621E 9F35匹配,那么接受它.

     注意: 如果你之前已经有一个版本的docker,没有指定版本的安装和更新会按照最高版本安装.不一定会满足你对稳定性的要求.
     
   +   安装指定版本得Docker引擎

      1.  列举所有库中的可用docker版本

         ```shell
         yum list docker-ce --showduplicates | sort -r
         ```

      2.  安装指定版本通过其全名,表现形式为包名+ 版本信息.例如:`docker-ce-18.09.1`.

         ```shell
         sudo yum install docker-ce-<VERSION_STRING> docker-ce-cli-<VERSION_STRING> containerd.io
         ```

      这样docker就安装好了,但是还没有运行.

3. 在测试和研发环境下,使用运行脚本安装代价是比较小的

   +  脚本安装

     在docker官网 [get.docker.com](https://get.docker.com/) 提供了安装版本.脚本源代码在docker-install库中.名为install.sh,使用之前需要知道存在的风险:

     1.  脚本需要root权限执行,因此运行前需要小心的去测试和编辑.
     2.  脚本会连接linux系统,配置你的包管理系统.除此之外,脚本不允许你去自定义参数,这可能导致不支持的情况发生.
     3.  脚本安装所有依赖,但是不会向你发送确认消息.这个可能会给你安装一大堆的包文件.这个依赖于当前你的主机上的配置
     4.  一律安装最新版本,不允许去自定义
     5.  请不要在一台使用其他安装方法安装Docker的机器上,运行这个脚本

#### 运行Docker

```shell
# 启动docker
sudo systemctl start docker
# 检查docker是否安装成功
sudo docker run hello-world
```

#### 卸载Docker

```shell
# 卸载docker 包
sudo yum remove docker-ce
# 删除主机上留存的镜像,容器,数据卷以及自定义配置
sudo rm -rf /var/lib/docker
# 编辑的配置必须要手动删除
```

#### 拓展内容

1.  docker的夜间模式(nightly)和测试通道(test channel)

​	