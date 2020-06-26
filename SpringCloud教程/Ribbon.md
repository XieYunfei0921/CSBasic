#### 负载均衡策略

##### 随机策略

```java
public Server choose(ILoadBalancer lb, Object key) {
        if (lb == null) {
            return null;
        }
        Server server = null;
        while (server == null) {
            if (Thread.interrupted()) {
                return null;
            }
            // 获取负载均衡器中的可使用服务器@uplist 以及所有的服务器@allList
            List<Server> upList = lb.getReachableServers();
            List<Server> allList = lb.getAllServers();
            int serverCount = allList.size();
            // 如果不存在有服务器参与,则直接结束
            if (serverCount == 0) {
                return null;
            }
            // 使用服务器数量@serverCount 作为随机种子,随机出服务器编号
            int index = chooseRandomInt(serverCount);\
            // 获取指定编号的服务器(可用状态)
            server = upList.get(index);
            // 如果服务器不可用,则重试
            if (server == null) {
                Thread.yield();
                continue;
            }
            if (server.isAlive()) {
                return (server);
            }
			// 使得编译器happy
            server = null;
            Thread.yield();
        }
        return server;
    }
```

##### RoundRobin

```java
public Server choose(ILoadBalancer lb, Object key) {
        // 边界条件,不存在负载均衡器
    	if (lb == null) {
            log.warn("no load balancer");
            return null;
        }
    
        Server server = null;
        int count = 0;
        while (server == null && count++ < 10) {
            // 获取当前负载均衡中存活的服务器/所有服务器
            List<Server> reachableServers = lb.getReachableServers();
            List<Server> allServers = lb.getAllServers();
            int upCount = reachableServers.size();
            int serverCount = allServers.size();
            // 没有可以参与负载的服务器则直接结束
            if ((upCount == 0) || (serverCount == 0)) {
                log.warn("No up servers available from load balancer: " + lb);
                return null;
            }
            // 获取下一个服务器的编号
            int nextServerIndex = incrementAndGetModulo(serverCount);
            server = allServers.get(nextServerIndex);
            // 如果这个服务器不可用使用,则重新执行
            if (server == null) {
                Thread.yield();
                continue;
            }
            // 可用的服务器.则决定是这个服务器
            if (server.isAlive() && (server.isReadyToServe())) {
                return (server);
            }
            server = null;
        }
    	// 重试上限,10
        if (count >= 10) {
            log.warn(
                "No available alive servers after 10 tries from load balancer: "+ lb);
        }
        return server;
    }
```

RoundRobin 服务器编号选取规则

```java
AtomicInteger nextServerCyclicCounter
...
/**
@module	计数器的上界
*/
private int incrementAndGetModulo(int modulo) {
    for (;;) {
        int current = nextServerCyclicCounter.get();
        // 选取当前计数器的下一个值作为编号
        int next = (current + 1) % modulo;
        // 如果修改成功则返回@next,作为当前负载均衡服务器的编号
        if (nextServerCyclicCounter.compareAndSet(current, next))
            return next;
    }
}
```

##### 重试机制

```java
public Server choose(ILoadBalancer lb, Object key) {
    	// 确定请求时间和重试最大允许的时间@deadline
		long requestTime = System.currentTimeMillis();
		long deadline = requestTime + maxRetryMillis;
		Server answer = null;
		// 使用RoundRobin策略选出服务器
		answer = subRule.choose(key);
		// 如果服务器在可重试的范围内失败或者是没有选出来,则继续选取,直到选出
		if (((answer == null) || (!answer.isAlive()))
				&& (System.currentTimeMillis() < deadline)) {

			InterruptTask task = new InterruptTask(deadline
					- System.currentTimeMillis());

			while (!Thread.interrupted()) {
				answer = subRule.choose(key);
				if (((answer == null) || (!answer.isAlive()))
						&& (System.currentTimeMillis() < deadline)) {
					Thread.yield();
				} else {
					break;
				}
			}
			task.cancel();
		}
		// 返回结果服务器
		if ((answer == null) || (!answer.isAlive())) {
			return null;
		} else {
			return answer;
		}
	}
```

##### 响应时间加权算法

```java
List<Double> accumulatedWeights
accumulatedWeights[i]表示accumulatedWeights 0 -> i的累计权重和
...
...
...
public Server choose(ILoadBalancer lb, Object key) {
        if (lb == null) {
            return null;
        }
        Server server = null;

        while (server == null) {
            List<Double> currentWeights = accumulatedWeights;
            if (Thread.interrupted()) {
                return null;
            }
            // 获取所有服务器列表
            List<Server> allList = lb.getAllServers();
            int serverCount = allList.size();
            if (serverCount == 0) {
                return null;
            }
            int serverIndex = 0;
            // 获取权重列表中最后一个的权重值(累计权重值)
            double maxTotalWeight = currentWeights.size() == 0 
                ? 0 : currentWeights.get(currentWeights.size() - 1); 
            // 如果没有初始化(总权重过小),或者没有选中服务器,则使用RoundRobin算法
            if (maxTotalWeight < 0.001d || serverCount != currentWeights.size()) {
                server =  super.choose(getLoadBalancer(), key);
                if(server == null) {
                    return server;
                }
            } else {
                // 生成一个[0,maxTotalWeight)随机数
                double randomWeight = random.nextDouble() * maxTotalWeight;
                // 基于随机index获取服务器编号
                int n = 0;
                for (Double d : currentWeights) {
                    // 获取随机权重列表中的值@d,选出累计权重列表汇总第一次大于随机权重的服务器编号
                    if (d >= randomWeight) {
                        serverIndex = n;
                        break;
                    } else {
                        n++;
                    }
                }
				// 确定服务器编号
                server = allList.get(serverIndex);
            }
			// 没有选中则继续
            if (server == null) {
                Thread.yield();
                continue;
            }
			// 返回服务器编号
            if (server.isAlive()) {
                return (server);
            }
            server = null;
        }
        return server;
    }
```

##### 最佳可用性算法

```java
public Server choose(Object key) {
    	// 边界条件判断,服务器没有状态设置,则使用一般的RoundRobin算法进行选择
        if (loadBalancerStats == null) {
            return super.choose(key);
        }
    	// 获取服务器列表
        List<Server> serverList = getLoadBalancer().getAllServers();
        int minimalConcurrentConnections = Integer.MAX_VALUE;
        long currentTime = System.currentTimeMillis();
        Server chosen = null;
        for (Server server: serverList) {
            // 获取服务器的状态
            ServerStats serverStats = loadBalancerStats.getSingleServerStat(server);
            // 确定当前时刻,服务器是否处于断开状态,如果没有处于断开状态,获取连接,并变量所有服务器
            // 获取其中并发连接次数最小的一个作为负载均衡的结果
            if (!serverStats.isCircuitBreakerTripped(currentTime)) {
                int concurrentConnections=
                    serverStats.getActiveRequestsCount(currentTime);
                if (concurrentConnections < minimalConcurrentConnections) {
                    minimalConcurrentConnections = concurrentConnections;
                    chosen = server;
                }
            }
        }
    	// 如果当前选出来的服务器不存在,则还是采取RoundRobin算法
        if (chosen == null) {
            return super.choose(key);
        } else {
            return chosen;
        }
    }
```

##### 可用性过滤策略

```java
public Server choose(Object key) {
        int count = 0;
    	// 使用RoundRobin选取服务器
        Server server = roundRobinRule.choose(key);
        while (count++ <= 10) {
            if (predicate.apply(new PredicateKey(server))) {
                return server;
            }
            server = roundRobinRule.choose(key);
        }
        return super.choose(key);
    }
```

##### 基于谓词的负载均衡

```java
public Server choose(Object key) {
    // 获取负载均衡器
    ILoadBalancer lb = getLoadBalancer();
    // 使用谓词过滤器进行过滤,之后使用RoundRobin进行负载均衡
    Optional<Server> server = getPredicate().chooseRoundRobinAfterFiltering(lb.getAllServers(), key);
    if (server.isPresent()) {
        return server.get();
    } else {
        return null;
    }       
}
```

```java
public Optional<Server> chooseRoundRobinAfterFiltering(List<Server> servers, Object loadBalancerKey) {
    // 进行谓词过滤
    List<Server> eligible = getEligibleServers(servers, loadBalancerKey);
    if (eligible.size() == 0) {
        return Optional.absent();
    }
    // 进行RoundRobin
    return Optional.of(eligible.get(incrementAndGetModulo(eligible.size())));
}
```

**谓词过滤逻辑**

```java
public List<Server> getEligibleServers(List<Server> servers, Object loadBalancerKey) {
    if (loadBalancerKey == null) {
        return ImmutableList.copyOf(Iterables.filter(servers, this.getServerOnlyPredicate()));            
    } else {
        List<Server> results = Lists.newArrayList();
        for (Server server: servers) {
            if (this.apply(new PredicateKey(loadBalancerKey, server))) {
                results.add(server);
            }
        }
        return results;            
    }
}
```

##### ZoneAvoidance策略

这个是基于**CompositePredicate**进行谓词过滤的负载均衡规则。主要的谓词规则是**ZoneAvoidancePredicate**和**ZoneAvoidancePredicate**