# 企业级分布式数据处理与对账平台

![Java](https://img.shields.io/badge/Language-Java-blue) ![Spring Boot](https://img.shields.io/badge/Framework-Spring%20Boot-brightgreen) ![XXL-JOB](https://img.shields.io/badge/Scheduler-XXL--JOB-orange) ![MySQL](https://img.shields.io/badge/Database-MySQL-blueviolet) ![Redis](https://img.shields.io/badge/Cache-Redis-red) ![License](https://img.shields.io/badge/License-Apache%202.0-lightgrey)

## 1. 项目简介

本项目是一个专为**高可靠、高性能**的企业级业务场景而设计的**分布式数据处理与对账平台**。它完整地模拟了企业（特别是金融、电商领域）核心的后端数据流转与处理过程，涵盖了从**自动化任务调度、数据处理、性能优化**到**分布式并发控制**的全链路技术实践。

该平台的核心架构，是一个由 **XXL-JOB** 驱动的**离线批处理系统**，负责处理海量数据的对账、报表等核心业务，同时具备向“**批流一体**”混合架构演进的能力。

---

## 2. 系统架构图

```mermaid
graph TD
    %% 1. 定义样式类别
    classDef userStyle fill:#f2f2f2,stroke:#333,stroke-width:2px,color:#333
    classDef adminStyle fill:#e6f3ff,stroke:#0066cc,stroke-width:2px,color:#003366
    classDef executorStyle fill:#e6ffee,stroke:#009933,stroke-width:2px,color:#004d1a
    classDef dbStyle fill:#fff5e6,stroke:#ff9900,stroke-width:2px,color:#995c00
    classDef redisStyle fill:#ffe6e6,stroke:#cc0000,stroke-width:2px,color:#800000 
    classDef mqStyle fill:#f3e6ff,stroke:#6600cc,stroke-width:2px,color:#330066
    classDef apiStyle fill:#e6f9ff,stroke:#00b8e6,stroke-width:2px,color:#005c73

    %% 2. 定义图表节点
    subgraph "<b>外部系统/用户</b>"
        A["开发者/运维人员"]
        H["实时数据源 (如: 合作商户)"]
    end

    subgraph "<b>调度中心 (XXL-JOB Admin)</b>"
        B["Web UI 界面"]
        C["调度服务"]
        D["元数据库 (MySQL)"]
    end

    subgraph "<b>数据处理与对账平台 (data-platform)</b>"
        I["API 接口 (Controller)"]
        J["消息队列 (RabbitMQ)"]
        K["消息消费服务 (Consumer)"]
        L["批处理任务 (XXL-JOB Handler)"]
        F["业务数据库 (MySQL)"]
        G["分布式锁/缓存 (Redis)"]
    end

    %% 3. 定义连接关系 (移除了所有不必要的空行)
    A -- "1.配置/监控<br>离线任务" --> B
    B -- "管理任务信息" --> C
    C -- "读写元数据" --> D
    C -- "2.触发离线任务<br>(心跳/日志)" --> L
    H -- "3.发送实时订单" --> I
    I -- "4.消息入队<br>(异步解耦/削峰)" --> J
    J -- "5.消息出队" --> K
    K -- "6.实时数据写入" --> F
    L -- "7.批量数据处理<br>(对账/报表)" --> F
    K & L -- "读写缓存<br>抢占分布式锁" --> G
    
    %% 4. 将样式应用到节点
    class A,H userStyle;
    class B,C adminStyle;
    class D,F dbStyle;
    class G redisStyle;
    class I apiStyle;
    class J mqStyle;
    class K,L executorStyle;

---

## 3. 核心功能与技术亮点

#### ✨ 高可靠的自动化任务调度
* 基于 **XXL-JOB** 搭建了企业级的分布式任务调度平台，对所有后端批处理任务进行统一的生命周期管理。
* 深入实践了 **失败重试**、**故障转移** 路由策略和 **邮件告警** 机制，确保了核心业务任务在异常情况下的高可靠性。

#### ✨ 数据库性能优化实战
* 针对百万级海量数据的查询场景，通过 **MySQL `EXPLAIN`** 命令精准分析出“全表扫描”的性能瓶颈。
* 通过设计并添加**B+树索引**，成功将目标SQL的查询耗时从 **900+毫秒** 优化至 **3毫秒**，性能提升**超过300倍**，深刻理解了索引在性能调优中的关键作用。

#### ✨ 分布式并发控制
* 针对执行器集群环境下任务可能被重复执行的问题，基于 **Redis** 的 `SETNX` 原子命令，设计并实现了一套**分布式锁**。
* 成功验证了该锁机制能够保证核心业务（如数据对账）在分布式环境下的**幂等性**与**数据一致性**。

#### ✨ 复杂的自动化工作流编排 (DAG)
* 利用 XXL-JOB 的**子任务**特性，实现了“先执行数据对账、成功后再执行报表生成”的自动化**任务依赖**关系，保证了业务流程的严格顺序性。

#### ✨ 海量数据并行处理方案
* 通过在本机模拟**执行器集群**，并采用“**分片广播**”路由策略，成功实现了将单一的海量数据任务“切分”给多个节点**并行处理**的方案，为系统的水平扩展和性能提升打下了基础。

#### ✨ 高并发实时数据接收与异步处理
* 基于 **RabbitMQ 消息队列**，设计并实现了一套**高并发、异步化**的实时数据接收链路。
* 通过**生产者-消费者模式**，成功将前端高并发请求与后端数据库写入操作**解耦**，利用消息队列的缓冲能力实现**削峰填谷**，极大地提升了系统的稳定性和API的响应速度。
---

## 4. 技术栈

| 技术领域 | 具体技术 |
| :--- | :--- |
| **后端主框架** | Spring Boot 2.7.18 |
| **分布式任务调度** | XXL-JOB 2.4.1 |
| **数据库** | MySQL 8.0 |
| **持久层框架** | MyBatis-Plus |
| **缓存/分布式锁** | Redis |
| **消息队列** | **RabbitMQ** |
| **项目构建** | Maven 3.9+ |
| **架构/部署** | **Docker, Docker Compose** *(后续规划)* |
| **开发环境** | IntelliJ IDEA, JDK 17/21 |

---

## 5. 快速开始

#### **5.1 准备依赖环境**
- 安装并启动 MySQL 8.0+
- 安装并启动 Redis

#### **5.2 部署 XXL-JOB 调度中心**
- 从 XXL-JOB 官方 [Gitee Releases](https://gitee.com/xuxueli/xxl-job/releases) 页面下载 `xxl-job-2.4.1.zip` 源码。
- 在 MySQL 中创建 `xxl_job` 数据库，并执行源码包中的 `doc/db/tables_xxl_job.sql` 脚本。
- 修改 `xxl-job-admin` 模块下的 `application.properties` 文件，配置好数据库连接。
- 运行 `XxlJobAdminApplication`，启动调度中心（默认端口 `8080`）。

#### **5.3 部署本项目 (data-platform)**
- 克隆本项目到本地: `git clone https://github.com/sssshine24/data-processing-platform.git`
- 将 `src/main/resources/application.properties.example` 复制一份，重命名为 `application.properties`。
- 在新的 `application.properties` 文件中，填入你自己的数据库、Redis 和 XXL-JOB `accessToken` 配置。
- 在 IntelliJ IDEA 中运行 `DataPlatformApplication`，启动本项目执行器（默认端口 `8081`）。

---

