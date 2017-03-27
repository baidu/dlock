DLock - Distributed Lock
==========================
[In English](README.md)

DLock是由Java实现的，一套高效高可靠的分布式锁方案。
使用Redis存储锁，通过[Lua](https://en.wikipedia.org/wiki/Lua_(programming_language&#41;)脚本进行原子性锁操作，
实现了基于Redis过期机制的[lease](https://en.wikipedia.org/wiki/Lease_(computer_science&#41;)，并提供了一种基于变种CLH队列的进程级锁竞争模型。

依赖版本：[Java8](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html)及以上版本、[Redis-2.6.12](http://redis.cn)及以上版本（使用到Redis Set -> NX PX指令）

架构设计
--------
![DLock](doc/dlock-architecture.png)

#### 特点 ####
* 原子性

  加锁、释放锁、延长租约等锁操作，均通过Lua脚本操作Redis，保证锁操作的原子性。

* 可重入性

  由本地锁对象内部存储持有者重入次数，等于零时释放锁, 从而保证锁的可重入.

* 锁租约

  基于Redis过期机制，实现了锁的租约和自动续租，既保证锁持有者有充足时间完成相应动作, 又避免持有者crash后锁不被释放的情形，提高了锁的可用性。

* 高性能锁模型

  采用lock-free的变种CLH锁队列维护竞争线程，并由重试线程唤醒Head去竞争锁, 从而将锁竞争粒度限定在进程级, 有效避免不必要的锁竞争. 此外还实现了非公平锁，以提升吞吐量。


Quick Start
------------
这里介绍如何在基于Spring的项目中使用DLock，具体流程如下:<br/>

### 步骤1: 安装依赖Java8、Maven、Redis
下载[Java8](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html)、[maven](https://maven.apache.org/download.cgi)和[Redis2.6.12](http://redis.cn)，然后安装部署。
对于Redis，可执行下述脚本来下载,解压和编译;
```sh
wget http://download.redis.io/releases/redis-3.2.6.tar.gz
tar xzf redis-3.2.6.tar.gz
cd redis-3.2.6
make
```
再执行下述脚本来部署Redis;
```sh
src/redis-server
```
至此, Redis节点已在默认端口6379监听服务

#### 设置环境变量
maven无须安装，设置好MAVEN_HOME即可。可像下述脚本这样设置JAVA_HOME和MAVEN_HOME，如已设置请忽略。
```shell
export MAVEN_HOME=/xxx/xxx/software/maven/apache-maven-3.3.9
export PATH=$MAVEN_HOME/bin:$PATH
JAVA_HOME="/Library/Java/JavaVirtualMachines/jdk1.8.0_91.jdk/Contents/Home";
export JAVA_HOME;
```
### 步骤2: 修改Redis连接配置
修改[redis.properties](src/test/resources/dlock/redis.properties)配置中, redis.host和redis.port为本地redis的配置。

### 步骤3: 运行示例单测
DLock实现了JAVA的锁接口`java.util.concurrent.Lock`，其语法与Lock一致，无额外使用成本。<br/>
单测[DLockSimpleTest](src/test/java/com/baidu/fsg/dlock/DLockSimpleTest.java)，展示了锁的基本用法;<br/>
单测[DistributedReentrantLockTest](src/test/java/com/baidu/fsg/dlock/DistributedReentrantLockTest.java)，展示了如重入、多线程/进程竞争下锁等场景


DLock TPS
----------
DLock进程级的锁模型，采用了变种CLH队列维护待竞争线程，仅令单一线程参与竞争，从而有效降低无效锁竞争，提升整体性能。
为此，这里将传统锁模型（所有线程一起去竞争）与DLock的性能进行对比。以单位时间（秒）一次完整锁操作(获取锁 -> 计算 -> 释放锁)作为衡量指标。
在实验时，将获取锁后的计算时间设置为10ms，DLock的锁lease设置为60ms，并统计线程数分别为8至128（间隔为8）时的完整锁操作速度，并以百分比的形式展示，即:<br/>
R = TPS<sub>DLock</sub> / TPS<sub>tradition</sub><br/>
数据如下表所示:

|threads|8|16|24|32|40|48|56|64|72|80|88|96|104|112|120|128
|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|
|R|1.01 |1.07 |1.21 |1.45 |1.56 |1.60 |1.66 |1.67 |1.69 |1.70 |1.71 |1.72 |1.74 |1.74 |1.67 |1.71 |

####注意
由于完整锁操作的TPS值跟持有锁的时间有关，因此，单纯关注TPS值是没有意义的，这里比较TPS的变化趋势。<br/>

![DLock VS Tradition](doc/throughput0.png)

变量R代表两种锁模型的TPS比值，与持有锁的时间无关，其数值是有意义的。<br/>

![R](doc/throughput1.png)

####结论
在并发度很低时，DLock与传统锁模型的性能相当; 随着并发度的不断增加，传统锁模型性能开始下降，但DLock由于会将新增的竞争者添加到CLH队列中
进行等待（因为此时一起去竞争必然会有大量的线程竞争失败），依次参与锁竞争，减少了无效的锁竞争开销，从而使得锁性能保持不变。
同时DLock还支持非公平锁, 增加锁处理的吞吐量。