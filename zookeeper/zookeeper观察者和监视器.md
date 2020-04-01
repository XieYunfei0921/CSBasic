

**Zookeeper监视器指南**

---

+ 新建度量系统
  +  Metrics
  +  Prometheus
  +  Grafana
+ JMX


#### 新建度量系统

自从3.6.0版本之后，提供了废弃的度量工具，用于帮助用于监视zookeeper: 包括`znode`,`network`,`disk`,`disk`,`quorum`,`leader选举`,`client`,`security`,`failures`,`watch/session`,`requestProcessor`等。

##### 度量系统

所有度量数据都在`ServerMetrics.java`文件中

##### Prometheus

使用Prometheus用于监视zookeeper的度量信息时一个简单的方式

+ 前置需求：

  + 在zookeeper的`zoo.cfg`中启动Prometheus的度量提供器

    设置`metricsProvider.className=org.apache.zookeeper.metrics.prometheus.PrometheusMetricsProvider`

  + 配置了度量提供器的http端口(默认端口号7000)`metricsProvider.httpPort`

  + 官网下载Prometheous

  + 设置目标zookeeper集群参数

    ```shell
    cat > /tmp/test-zk.yaml <<EOF
    global:
      scrape_interval: 10s
    scrape_configs:
      - job_name: test-zk
        static_configs:
        - targets: ['192.168.10.32:7000','192.168.10.33:7000','192.168.10.34:7000']
    EOF
    cat /tmp/test-zk.yaml
    ```

  + 启动Prometheous处理器

    ```shell
    nohup /tmp/prometheus \
        -config.file /tmp/test-zk.yaml \
        -web.listen-address ":9090" \
        -storage.local.path "test-zk.data" >> /tmp/test-zk.log  2>&1 &
    ```

    这样Prometheous会每10s中汇报zookeeper度量信息

### Grafana

   Grafana建立在Prometheous支持上，仅仅需要添加Prometheus数据源即可

  ```bash
  Name:   test-zk
  Type:   Prometheus
  Url:    http://localhost:9090
  Access: proxy
  ```
- 下载并导入zookeeper的[dashboard](https://grafana.com/dashboards/10465),并进行自定义配置