# day4-知识库基础.md

## 1. 一句话总结本章目标

本章的目标是：给 AI 恋爱大师增加 RAG 知识库问答能力，让它在回答用户问题前，先查询我们准备好的恋爱知识文档，再结合这些资料生成回答。

一句话：

```text
Day4 让项目从“普通 AI 聊天”升级为“能参考本地/云端知识库回答问题的 AI 应用”。
```

这一章最重要的不是背 RAG 定义，而是理解一条业务链路：

```text
用户提问
-> 系统先查知识库
-> 把查到的资料交给大模型
-> 大模型基于资料回答
```

## 2. 本章业务场景

### 用户输入什么？

用户可能会问一个具体的恋爱问题，比如：

```text
我已经结婚了，但是婚后关系不太亲密，怎么办？
```

### 系统要完成什么？

系统不应该只让大模型凭通用经验回答，而应该：

```text
1. 先去项目准备好的恋爱知识库里查相关内容
2. 找到“已婚篇”或婚后关系相关的文档片段
3. 把这些文档片段作为上下文交给大模型
4. 让大模型结合知识库内容生成回答
```

也就是说，本章希望系统回答得更像：

```text
我参考了项目中的恋爱知识文档，再给用户一套更贴合业务内容的建议。
```

### 原来的项目为什么做不到？

在 Day3 里，项目已经有了：

```text
ChatClient
Prompt
多轮对话记忆
自定义 Advisor 日志
结构化输出
```

但是 Day3 的 AI 主要还是依赖大模型自身的知识。它不知道我们项目里准备的恋爱问答文档，也不会主动推荐我们文档里写好的课程、服务或固定回答风格。

如果没有 RAG，可能出现这些问题：

```text
1. AI 不知道项目自己的私有知识
2. AI 回答可能比较泛泛
3. AI 可能编造不存在的课程或内容
4. AI 无法稳定复用我们准备好的恋爱问答资料
```

所以 Day4 要解决的核心问题是：

```text
如何让 AI 回答前先“查资料”？
```

## 3. 本章在项目中的作用

### 承接前面什么能力？

Day4 承接的是 Day3 的 AI 应用开发能力。

Day3 已经解决：

```text
用户消息如何交给大模型
系统提示词如何设置
多轮对话记忆如何传递
Advisor 如何增强一次 AI 调用
```

Day4 在这个基础上继续增加：

```text
知识库检索能力
```

也就是说，Day4 不是重新做一个项目，而是在 `LoveApp` 原有聊天能力上加一层 RAG。

可以这样理解：

```text
Day3：AI 会聊天
Day4：AI 会先查资料再聊天
```

### 给后面什么能力做铺垫？

Day4 是 Day5 RAG 进阶的基础。

Day4 先跑通最小闭环：

```text
文档 -> Document -> VectorStore -> QuestionAnswerAdvisor -> AI 回答
```

Day5 会在这个闭环上继续优化：

```text
文档切分
元数据标注
关键词增强
PgVector 持久化向量库
查询重写
检索器配置
自定义 RetrievalAugmentationAdvisor
```

所以学习 Day4 时，先不要急着把 PgVector、HNSW、查询重写全部吃透。它们是后续优化项。Day4 先理解“RAG 为什么能跑起来”。

## 4. 本章实现了什么

用普通话讲，本章实现了三件事：

### 第一件事：准备知识资料

项目里放了几篇恋爱相关的 Markdown 文档：

```text
src/main/resources/document/恋爱常见问题和回答-单身篇.md
src/main/resources/document/恋爱常见问题和问答-恋爱篇.md
src/main/resources/document/恋爱常见问题和回答-已婚篇.md
```

这些文档就是 AI 回答问题时可以参考的“资料库”。

### 第二件事：把资料变成系统能检索的知识库

Markdown 文件本身只是普通文本文件。项目需要把它们读出来，变成 Spring AI 里的 `Document`，再存入 `VectorStore`。

粗略理解：

```text
Markdown 文件
-> Document
-> 向量
-> VectorStore
```

这样用户提问时，系统才能根据语义相似度找到相关资料。

### 第三件事：用户提问时自动查知识库

在 `LoveApp.doChatWithRag` 中加入：

```java
// new QuestionAnswerAdvisor(loveAppVectorStore)
```

它会在调用大模型前，先根据用户问题去 `VectorStore` 检索相关文档，再把检索结果拼进 Prompt。

所以 Day4 的本质是：

```text
让一次普通 AI 调用，多了一步“查知识库”的动作。
```

## 5. 涉及哪些代码

### `src/main/resources/document/*.md`

它是什么：

```text
恋爱知识库的原始文档。
```

为什么需要它：

```text
RAG 必须有外部知识来源。没有这些 Markdown 文档，AI 就没有项目自己的资料可以查。
```

什么时候执行：

```text
它们不是代码，不会自己执行。
项目启动初始化知识库时，会被 LoveAppDocumentLoader 读取。
```

它参与哪条业务流程：

```text
项目启动阶段：Markdown 文档 -> Document -> VectorStore。
用户请求阶段：VectorStore 里查到的 Document 会作为上下文参与回答。
```

没有它会怎样：

```text
知识库没有内容，QuestionAnswerAdvisor 检索不到有效资料，RAG 效果就不存在。
```

### `LoveAppDocumentLoader`

位置：

```text
src/main/java/com/example/yuaiagent/rag/LoveAppDocumentLoader.java
```

它是什么：

```text
文档加载器，负责读取 resources/document 目录下的 Markdown 文件。
```

为什么需要它：

```text
Spring AI 的 RAG 流程处理的是 Document，不是直接处理 Markdown 文件。
所以需要一个类把 Markdown 文件读出来，并转换成 List<Document>。
```

什么时候执行：

```text
通常在初始化 VectorStore 时执行。
LoveAppVectorStoreConfig 或 PgVectorVectorStoreConfig 会调用 loadMarkdowns()。
```

它参与哪条业务流程：

```text
项目启动阶段的知识库准备流程。
```

没有它会怎样：

```text
项目不知道去哪里读取知识库文档，也无法把 Markdown 转成 Document。
```

它当前做了什么：

```text
1. 使用 ResourcePatternResolver 查找 classpath:document/*.md
2. 遍历每个 Resource
3. 获取文件名 filename
4. 从文件名中提取 status 元信息，例如单身、恋爱、已婚
5. 用 MarkdownDocumentReader 读取 Markdown
6. 返回所有 Document
```

这里你代码里有一个 `//?`：

```java
//? Resource[] resources是什么，resources又是什么
Resource[] resources = resourcePatternResolver.getResources("classpath:document/*.md");
```

先简单理解：

```text
Resource 是 Spring 对“资源文件”的统一包装。
Resource[] resources 表示匹配到的多个 Markdown 文件。
```

### `LoveAppVectorStoreConfig`

位置：

```text
src/main/java/com/example/yuaiagent/rag/LoveAppVectorStoreConfig.java
```

它是什么：

```text
用于创建本地内存版 VectorStore 的配置类。
```

为什么需要它：

```text
读取出来的 Document 还不能直接用于语义检索。
需要把 Document 写入 VectorStore，后续用户提问时才能相似度搜索。
```

什么时候执行：

```text
在 Spring 创建 VectorStore Bean 的时候执行。
```

它参与哪条业务流程：

```text
项目启动阶段的知识库初始化流程。
```

没有它会怎样：

```text
项目没有本地 VectorStore，QuestionAnswerAdvisor 就没有知识库可以查询。
```

需要注意：

```text
源文档 Day4 的基础写法是使用 SimpleVectorStore。
你当前项目里这个类已经混入了 Day5 的 MyKeywordEnricher 关键词元信息增强。
学习 Day4 时先抓住“创建 VectorStore 并写入文档”这条主线即可。
```

另一个注意点：

```text
当前文件里能看到 @Bean，但没有看到 @Configuration。
通常 @Bean 方法需要放在 Spring 配置类中才会被 Spring 扫描并创建 Bean。
你当前项目中还有 PgVectorVectorStoreConfig 也创建了名为 loveAppVectorStore 的 Bean。
这属于后续排错时要关注的点，Day4 学习主线先不要被它打断。
```

### `PgVectorVectorStoreConfig`

位置：

```text
src/main/java/com/example/yuaiagent/rag/PgVectorVectorStoreConfig.java
```

它是什么：

```text
基于 PostgreSQL + pgvector 创建 VectorStore 的配置类。
```

为什么需要它：

```text
SimpleVectorStore 是内存版知识库，更适合学习和快速验证。
PgVectorStore 可以把向量数据存到 PostgreSQL，重启后数据仍然存在，更接近真实项目。
```

什么时候执行：

```text
Spring 启动创建 loveAppVectorStore Bean 时执行。
```

它参与哪条业务流程：

```text
项目启动阶段的知识库持久化准备流程。
```

没有它会怎样：

```text
项目仍可以用 SimpleVectorStore 跑通 Day4 基础 RAG，但没有 pgvector 持久化能力。
```

学习 Day4 时怎么处理它：

```text
先知道它也是一种 VectorStore 实现即可。
PgVector、HNSW、dimensions、initializeSchema 是 Day5 进阶重点。
Day4 先理解 VectorStore 在 RAG 中的角色。
```

### `LoveAppRagCloudAdvisorConfig`

位置：

```text
src/main/java/com/example/yuaiagent/rag/LoveAppRagCloudAdvisorConfig.java
```

它是什么：

```text
基于阿里云百炼云知识库创建 RAG Advisor 的配置类。
```

为什么需要它：

```text
Day4 除了本地知识库，还讲了云知识库方案。
云知识库把文档解析、切片、存储、检索等工作交给平台完成，代码侧主要负责调用平台检索器。
```

什么时候执行：

```text
Spring 启动时创建 loveAppRagCloudAdvisor Bean。
```

它参与哪条业务流程：

```text
用户请求阶段可以替代 QuestionAnswerAdvisor(loveAppVectorStore)，让 AI 从云知识库检索资料。
```

没有它会怎样：

```text
项目只能使用本地知识库方案，不能直接对接阿里云百炼知识库。
```

当前状态：

```text
LoveApp 中注入了 loveAppRagCloudAdvisor，但当前实际调用处是注释状态。
也就是说你当前主线还是本地 VectorStore + QuestionAnswerAdvisor。
```

### `LoveApp.doChatWithRag`

位置：

```text
src/main/java/com/example/yuaiagent/app/LoveApp.java
```

它是什么：

```text
AI 恋爱知识库问答的业务入口。
```

为什么需要它：

```text
普通 doChat 是基础对话。
doChatWithRag 是带知识库检索能力的对话。
```

什么时候执行：

```text
用户发起一次 RAG 问答时执行。
测试中 LoveAppTest.doChatWithRag() 会调用它。
```

它参与哪条业务流程：

```text
用户请求阶段。
```

没有它会怎样：

```text
项目没有单独的 RAG 问答入口，只能走普通聊天能力。
```

当前代码里它做了这些事：

```text
1. 先调用 QueryRewriter 重写用户问题
2. 使用 ChatClient 构造一次 AI 请求
3. 设置 user 为重写后的问题
4. 设置对话记忆参数 chatId 和 retrieve size
5. 加入 MyLoggerAdvisor 打印日志
6. 加入 QuestionAnswerAdvisor(loveAppVectorStore) 启用知识库问答
7. 调用大模型
8. 返回文本结果
```

注意：

```text
QueryRewriter 是 Day5 的查询重写内容。
如果只看 Day4，先把它理解成“用户原始问题可能先被优化了一下”，不要让它打断主线。
```

### `LoveAppTest.doChatWithRag`

位置：

```text
src/test/java/com/example/yuaiagent/app/LoveAppTest.java
```

它是什么：

```text
RAG 问答功能的测试入口。
```

为什么需要它：

```text
用于验证 doChatWithRag 至少能跑通并返回非空结果。
```

什么时候执行：

```text
手动运行单元测试时执行。
```

它参与哪条业务流程：

```text
模拟用户向 AI 恋爱大师提问。
```

没有它会怎样：

```text
功能仍然可以存在，但你缺少一个快速验证 RAG 调用链路的入口。
```

## 6. 执行流程

Day4 一定要分成两个阶段理解，否则很容易乱。

### 项目启动时提前做了什么？

启动阶段是在用户真正发消息之前发生的。它的目标是：先把知识库准备好。

```text
1. Spring Boot 启动 YuAiAgentApplication
2. Spring 扫描组件和配置类
3. 创建 LoveAppDocumentLoader
4. 创建 VectorStore 相关 Bean
5. 调用 loveAppDocumentLoader.loadMarkdowns()
6. 读取 resources/document/*.md
7. MarkdownDocumentReader 把 Markdown 转成 Document
8. EmbeddingModel 把 Document 内容转成向量
9. VectorStore 保存向量和对应文档内容
```

这一阶段可以理解为：

```text
先建好知识库仓库。
```

你画图时可以画成：

```text
Markdown 文件
   ↓
LoveAppDocumentLoader
   ↓
Document 列表
   ↓
EmbeddingModel
   ↓
向量
   ↓
VectorStore
```

### 用户发起请求后发生了什么？

请求阶段是在用户真的问问题时发生的。它的目标是：用已经准备好的知识库辅助回答。

以用户问题为例：

```text
我已经结婚了，但是婚后关系不太亲密，怎么办？
```

执行流程：

```text
1. 用户问题进入 LoveApp.doChatWithRag(message, chatId)
2. QueryRewriter 可能先把问题改写得更适合检索
3. ChatClient.prompt() 开始构造一次 AI 请求
4. .user(rewrittenMessage) 设置用户问题
5. MessageChatMemoryAdvisor 通过 advisor 参数读取会话记忆
6. MyLoggerAdvisor 打印请求和响应日志
7. QuestionAnswerAdvisor 使用 loveAppVectorStore 查询相关文档
8. VectorStore 返回相似度最高的 Document
9. QuestionAnswerAdvisor 把 Document 内容拼进 Prompt
10. ChatClient.call() 调用 DashScope 大模型
11. 取出 chatResponse 的文本内容并返回
```

这一阶段可以理解为：

```text
用户问问题时，AI 先查小抄，再回答。
```

## 7. 白纸复现图

建议你手画两张图，不要一开始就画很复杂。

### 图 1：项目启动阶段

画这个：

```text
【项目启动：准备知识库】

resources/document/*.md
        ↓
LoveAppDocumentLoader
        ↓
MarkdownDocumentReader
        ↓
List<Document>
        ↓
EmbeddingModel
        ↓
VectorStore
```

讲图时说：

```text
项目启动时，会先把 Markdown 知识文档读取成 Document，再通过 EmbeddingModel 转成向量，最后放进 VectorStore。
```

### 图 2：用户提问阶段

画这个：

```text
【用户请求：知识库问答】

用户问题
   ↓
LoveApp.doChatWithRag
   ↓
ChatClient
   ↓
QuestionAnswerAdvisor
   ↓
VectorStore 相似度检索
   ↓
相关 Document
   ↓
拼进 Prompt
   ↓
DashScope 大模型
   ↓
AI 回答
```

讲图时说：

```text
用户提问时，QuestionAnswerAdvisor 会先拿问题去 VectorStore 检索相关文档，再把文档作为上下文拼进 Prompt，最后让大模型回答。
```

### 图 3：本地知识库和云知识库对比

这张图可选，等前两张图画顺了再画。

```text
本地知识库：
Markdown -> Document -> 本地 VectorStore -> QuestionAnswerAdvisor

云知识库：
文档上传云平台 -> 云平台切片和存储 -> DashScopeDocumentRetriever -> RetrievalAugmentationAdvisor
```

## 8. 核心知识点

### RAG

是什么：

```text
RAG 是 Retrieval-Augmented Generation，检索增强生成。
```

为什么需要：

```text
大模型不知道项目私有知识，也可能胡编。
RAG 让模型回答前先检索外部知识，减少泛泛回答和幻觉。
```

本项目怎么用：

```text
AI 恋爱大师在回答恋爱问题前，先检索恋爱知识文档。
```

我的理解：

```text
RAG 不是训练模型，而是回答前给模型递资料。
```

### Document

是什么：

```text
Spring AI 里表示一段文档内容的对象，通常包含文本内容和 metadata。
```

为什么需要：

```text
RAG 流程需要统一处理各种来源的资料，Document 就是 Spring AI 对知识片段的统一包装。
```

本项目怎么用：

```text
LoveAppDocumentLoader 把 Markdown 文件读取成 List<Document>。
```

我的理解：

```text
Document 是知识库里的一个知识片段。
```

### MarkdownDocumentReader

是什么：

```text
Spring AI 提供的 Markdown 文档读取器。
```

为什么需要：

```text
项目的知识文档是 Markdown 格式，需要用它读取并转换成 Document。
```

本项目怎么用：

```text
LoveAppDocumentLoader 中为每个 Resource 创建 MarkdownDocumentReader，然后调用 get() 获取 Document。
```

我的理解：

```text
MarkdownDocumentReader 就是把 md 文件拆成 Spring AI 能理解的知识片段。
```

### EmbeddingModel

是什么：

```text
把文本转换成向量的模型。
```

为什么需要：

```text
向量可以表示文本语义，方便计算用户问题和文档之间的相似度。
```

本项目怎么用：

```text
创建 SimpleVectorStore 或 PgVectorStore 时传入 dashscopeEmbeddingModel。
写入 Document 时，VectorStore 会借助 EmbeddingModel 生成向量。
```

我的理解：

```text
EmbeddingModel 把文字翻译成数学坐标。
```

### VectorStore

是什么：

```text
向量存储接口，负责保存文档向量，并支持相似度搜索。
```

为什么需要：

```text
用户提问时，系统要从很多文档里快速找到最相关的片段。
```

本项目怎么用：

```text
loveAppVectorStore 被注入到 LoveApp，然后交给 QuestionAnswerAdvisor 使用。
```

我的理解：

```text
VectorStore 是 AI 的知识库仓库。
```

### SimpleVectorStore

是什么：

```text
Spring AI 提供的内存版向量库。
```

为什么需要：

```text
学习阶段搭建简单，适合先跑通 RAG 最小闭环。
```

本项目怎么用：

```text
LoveAppVectorStoreConfig 中使用 SimpleVectorStore.builder(dashscopeEmbeddingModel).build() 创建。
```

我的理解：

```text
SimpleVectorStore 是临时知识库，方便学习和验证。
```

### QuestionAnswerAdvisor

是什么：

```text
Spring AI 提供的知识库问答 Advisor。
```

为什么需要：

```text
它能在调用大模型前，先从 VectorStore 检索相关 Document，再把这些内容拼进 Prompt。
```

本项目怎么用：

```text
LoveApp.doChatWithRag 中使用 new QuestionAnswerAdvisor(loveAppVectorStore)。
```

我的理解：

```text
QuestionAnswerAdvisor 是帮 AI 查小抄的人。
```

### RetrievalAugmentationAdvisor

是什么：

```text
Spring AI 提供的更灵活的 RAG 增强 Advisor。
```

为什么需要：

```text
QuestionAnswerAdvisor 简单好用，但定制能力有限。
RetrievalAugmentationAdvisor 可以绑定不同的文档检索器、查询转换器、上下文增强器。
```

本项目怎么用：

```text
LoveAppRagCloudAdvisorConfig 中用它对接阿里云百炼知识库。
```

我的理解：

```text
QuestionAnswerAdvisor 是简单版 RAG。
RetrievalAugmentationAdvisor 是可扩展版 RAG。
```

### DocumentRetriever

是什么：

```text
文档检索器，负责根据用户查询返回相关 Document。
```

为什么需要：

```text
云知识库或自定义 RAG 场景下，需要一个组件专门负责“从哪里查文档”。
```

本项目怎么用：

```text
LoveAppRagCloudAdvisorConfig 中使用 DashScopeDocumentRetriever 从阿里云百炼知识库检索文档。
```

我的理解：

```text
DocumentRetriever 是 RAG 里的“查资料执行者”。
```

## 9.Java/ Spring / SpringBoot/ Spring AI 相关的知识补充

### `@Component`

是什么：

```text
告诉 Spring：这个类需要交给 Spring 容器管理。
```

本章哪里出现：

```text
LoveAppDocumentLoader 使用 @Component。
```

为什么重要：

```text
只有成为 Spring Bean，其他类才能通过 @Resource 注入它。
```

### `@Configuration`

是什么：

```text
声明这是一个 Spring 配置类，里面通常会有 @Bean 方法。
```

本章哪里出现：

```text
LoveAppRagCloudAdvisorConfig
PgVectorVectorStoreConfig
```

为什么重要：

```text
配置类负责告诉 Spring 如何创建某些对象，比如 Advisor、VectorStore。
```

### `@Bean`

是什么：

```text
把方法返回的对象注册到 Spring 容器中。
```

本章哪里出现：

```text
loveAppVectorStore
loveAppRagCloudAdvisor
```

为什么重要：

```text
LoveApp 里注入的 VectorStore 和 Advisor，通常就是通过 @Bean 创建出来的。
```

### `@Resource`

是什么：

```text
Java/Jakarta 提供的依赖注入注解，Spring 支持它。
```

本章哪里出现：

```text
LoveApp 中注入 loveAppVectorStore、loveAppRagCloudAdvisor、queryRewriter。
配置类中注入 LoveAppDocumentLoader。
```

为什么重要：

```text
它让你不用手动 new 对象，而是让 Spring 把容器里的 Bean 注入进来。
```

### `@Value`

是什么：

```text
从配置文件或环境变量中读取配置值。
```

本章哪里出现：

```text
LoveAppRagCloudAdvisorConfig 中读取 spring.ai.dashscope.api-key。
```

为什么重要：

```text
云知识库需要 DashScope API Key，不能在代码里写死。
```

### Resource 和 ResourcePatternResolver

是什么：

```text
Resource 是 Spring 对资源文件的统一抽象。
ResourcePatternResolver 可以按路径模式批量查找资源。
```

本章哪里出现：

```java
Resource[] resources = resourcePatternResolver.getResources("classpath:document/*.md");
```

为什么重要：

```text
项目需要一次性读取 resources/document 目录下的多篇 Markdown 文件。
```

我的理解：

```text
ResourcePatternResolver 像一个文件搜索器，Resource 像搜索结果里的每个文件包装对象。
```

### `classpath:document/*.md`

是什么：

```text
表示从项目 classpath 下的 document 目录中查找所有 .md 文件。
```

本项目对应哪里：

```text
src/main/resources/document/*.md
```

为什么重要：

```text
Spring Boot 打包后，resources 里的文件会进入 classpath。
用 classpath 方式读取，比写死本地绝对路径更适合 Java 项目。
```

### ChatClient

是什么：

```text
Spring AI 提供的高级聊天客户端，用来构造和发送大模型请求。
```

本章哪里出现：

```text
LoveApp.doChatWithRag
```

为什么重要：

```text
RAG 最终还是要调用大模型，ChatClient 是发起这次调用的入口。
```

### Advisor

是什么：

```text
Spring AI 调用链上的增强器，可以在模型调用前后做额外处理。
```

本章哪里出现：

```text
MyLoggerAdvisor
QuestionAnswerAdvisor
RetrievalAugmentationAdvisor
MessageChatMemoryAdvisor
```

为什么重要：

```text
RAG 能接入 ChatClient，靠的就是 Advisor 机制。
QuestionAnswerAdvisor 会在调用模型前检索知识库并增强 Prompt。
```

### Spring Boot 自动配置和手动配置

本章里你会看到两类对象来源：

```text
1. Spring AI Alibaba 根据配置自动创建的模型 Bean
   例如 ChatModel、EmbeddingModel

2. 你自己写配置类手动创建的 Bean
   例如 VectorStore、loveAppRagCloudAdvisor
```

学习时要分清：

```text
不是所有对象都是自己 new 的。
很多对象是 Spring 根据依赖、配置和 @Bean 自动放进容器的。
```

## 10. 我容易不懂的地方（有 // ？的地方）

### A 类：阻塞主流程，立刻解决

这些不懂，会影响你讲清楚 Day4 主流程。

#### 1. `Resource[] resources` 是什么？

解释：

```text
Resource 是 Spring 对资源文件的包装。
Resource[] resources 是多个资源文件组成的数组。
```

在本项目里：

```text
它代表 resources/document 目录下匹配到的多篇 Markdown 文件。
```

你可以这样理解：

```text
resources 不是普通字符串，而是一组“被 Spring 找到的 md 文件对象”。
```

#### 2. Markdown 为什么要变成 Document？

解释：

```text
Spring AI 的 RAG 流程处理的是 Document。
Document 里面有文本内容和 metadata，方便后续切分、向量化、检索。
```

如果不变成 Document：

```text
后面的 VectorStore、QuestionAnswerAdvisor 就无法按 Spring AI 的标准流程使用它。
```

#### 3. VectorStore 里到底存什么？

简化理解：

```text
存的是文档内容对应的向量，以及能找回原文和 metadata 的信息。
```

不要只理解成“存文本”，也不要只理解成“存数字”。它是为了相似度检索服务的知识存储。

#### 4. QuestionAnswerAdvisor 到底做什么？

解释：

```text
它在调用大模型前，先拿用户问题去 VectorStore 查相关 Document。
然后把查到的 Document 作为上下文拼进 Prompt。
```

一句话：

```text
它让普通问答变成知识库问答。
```

#### 5. 为什么用户问题能匹配到文档？

解释：

```text
用户问题和文档都会通过 EmbeddingModel 转成向量。
系统通过向量相似度判断哪些文档和用户问题更相关。
```

### B 类：细节问题，稍后解决

这些会影响理解细节，但不影响你先讲通 Day4 主流程。

#### 1. `MarkdownDocumentReaderConfig` 每个参数什么意思？

当前先记住：

```text
它控制 Markdown 文档怎么被读取，以及给 Document 添加哪些 metadata。
```

比如：

```text
withIncludeCodeBlock(false)：不把代码块作为主要内容读入
withIncludeBlockquote(false)：不把引用块读入
withAdditionalMetadata("filename", filename)：给文档片段添加文件名
withAdditionalMetadata("status", status)：给文档片段添加恋爱状态标签
```

#### 2. `status = filename.substring(...)` 为什么这样写？

当前先理解：

```text
这是从文件名中截取“单身 / 恋爱 / 已婚”这类标签。
```

它属于 Day5 的元数据过滤思路。Day4 只需要知道 metadata 可以帮助后续更精确检索。

#### 3. `CHAT_MEMORY_CONVERSATION_ID_KEY` 和 `CHAT_MEMORY_RETRIEVE_SIZE_KEY` 是什么？

解释：

```text
它们是给 MessageChatMemoryAdvisor 用的参数。
前者告诉系统当前是哪一个会话。
后者告诉系统读取最近多少条历史消息。
```

它属于 Day3 对话记忆内容，在 Day4 中只是和 RAG 一起出现在调用链里。

#### 4. `LoveAppVectorStoreConfig` 和 `PgVectorVectorStoreConfig` 都像是在创建 VectorStore，区别是什么？

先这样记：

```text
LoveAppVectorStoreConfig：内存版 SimpleVectorStore，适合 Day4 基础学习。
PgVectorVectorStoreConfig：PostgreSQL + pgvector，适合 Day5 进阶持久化。
```

### C 类：进阶内容，先记录

这些是后续章节重点，Day4 先记录，不要卡住。

```text
1. PgVector 是什么，为什么要用 PostgreSQL 存向量？
2. HNSW 索引是什么？
3. dimensions=1536 为什么要和 Embedding 模型匹配？
4. QueryRewriter 查询重写为什么能提升检索效果？
5. RetrievalAugmentationAdvisor 和 QuestionAnswerAdvisor 怎么选？
6. 云知识库和本地知识库在工程上的取舍是什么？
7. metadata 过滤如何提高 RAG 精准度？
```

## 11. 自测题

### 初级

1. Day4 给项目新增了什么能力？
2. RAG 是为了解决什么问题？
3. 本项目里的恋爱知识文档放在哪里？
4. `LoveAppDocumentLoader` 的作用是什么？
5. `Document` 是什么？
6. `VectorStore` 是什么？
7. `QuestionAnswerAdvisor` 的作用是什么？
8. `doChat` 和 `doChatWithRag` 最大区别是什么？
9. 为什么普通 AI 回答不一定适合项目自己的业务？
10. Day4 为什么先用 Markdown 文档做知识库？

### 中级

1. 为什么 Markdown 文件不能直接等于 RAG 知识库？
2. 为什么需要把文本转成向量？
3. 用户问题和文档片段是如何匹配上的？
4. `EmbeddingModel` 在写入 VectorStore 和检索时分别起什么作用？
5. `QuestionAnswerAdvisor` 是如何影响 Prompt 的？
6. 为什么说 RAG 不是训练模型？
7. 项目启动阶段和用户请求阶段分别做了什么？
8. 本地知识库方案和云知识库方案有什么区别？
9. `metadata` 在 RAG 中有什么潜在作用？
10. 如果知识库没有相关内容，AI 回答可能会出现什么问题？

### 高级

1. 如果没有 `QuestionAnswerAdvisor`，`doChatWithRag` 还算 RAG 吗？为什么？
2. 如果 VectorStore 初始化失败，用户请求阶段会受到什么影响？
3. 如果 Embedding 维度和向量库配置维度不一致，可能会出现什么问题？
4. 为什么 `SimpleVectorStore` 适合学习，但不适合正式生产？
5. 为什么云知识库能简化代码，但会带来成本和数据隐私问题？
6. `QuestionAnswerAdvisor` 和 `RetrievalAugmentationAdvisor` 的定位有什么区别？
7. 为什么文档切片质量会影响最终回答质量？
8. 如果用户问“她为什么不回我消息”，RAG 检索可能会查到哪些文档？
9. 如果你要排查 RAG 没有效果，会先检查哪几个点？
10. 如果面试官问“你项目里的 RAG 怎么实现的”，你能按启动阶段和请求阶段讲出来吗？

## 12. 面试题

### 项目介绍型

#### 问：你这个项目里的 RAG 知识库是做什么的？

答：

```text
在 AI 恋爱大师项目中，RAG 知识库用于让 AI 回答用户恋爱问题前，先检索我们准备好的恋爱问答文档。
这样模型不是只依赖自己的通用知识，而是能结合项目私有资料生成更贴合业务的回答。
```

#### 问：你是怎么实现本地知识库问答的？

答：

```text
我把恋爱相关 Markdown 文档放在 resources/document 目录下。
项目启动时，通过 LoveAppDocumentLoader 使用 MarkdownDocumentReader 读取这些文档，并转换成 Spring AI 的 Document。
然后把 Document 写入 VectorStore。
用户调用 doChatWithRag 时，QuestionAnswerAdvisor 会根据用户问题从 VectorStore 检索相关文档，并把文档内容拼接到 Prompt 中，最后交给大模型生成回答。
```

#### 问：这块功能在项目中解决了什么业务问题？

答：

```text
它解决的是 AI 缺少项目私有知识的问题。
比如恋爱课程、常见问题、不同恋爱状态下的建议，这些不一定存在于大模型通用知识中。
通过 RAG，AI 可以回答得更贴合我们自己的知识库。
```

### 原理型

#### 问：RAG 的核心流程是什么？

答：

```text
RAG 核心流程可以分为四步：
第一，收集和处理文档；
第二，把文档通过 Embedding 模型转成向量并存入向量库；
第三，用户提问时从向量库检索相关文档；
第四，把检索到的文档作为上下文拼进 Prompt，让大模型基于上下文回答。
```

#### 问：为什么要用 Embedding？

答：

```text
Embedding 可以把文本转换成向量，让系统可以计算用户问题和文档内容之间的语义相似度。
这样即使用户问题和文档不是完全相同的关键词，也可能检索到语义相关的内容。
```

#### 问：VectorStore 的作用是什么？

答：

```text
VectorStore 负责保存文档向量，并提供相似度搜索能力。
在 RAG 中，它相当于知识库的检索层，用户问题进来后，会通过它找到最相关的文档片段。
```

#### 问：QuestionAnswerAdvisor 做了什么？

答：

```text
QuestionAnswerAdvisor 是 Spring AI 提供的 RAG Advisor。
它会在大模型调用前，根据用户问题去 VectorStore 检索相关文档，然后把检索到的内容附加到 Prompt 中，让模型基于这些上下文回答。
```

#### 问：RAG 和模型微调有什么区别？

答：

```text
RAG 不改变模型参数，而是在回答前检索外部知识并提供上下文。
微调是用训练数据调整模型参数。
RAG 更适合接入经常变化的业务知识，成本也更低。
```

### 排错型

#### 问：如果 RAG 没有检索到知识库内容，你会怎么排查？

答：

```text
我会先检查文档是否放在 resources/document 目录下；
再检查 LoveAppDocumentLoader 是否成功读取到 Document；
然后检查 VectorStore 是否成功创建并写入文档；
再检查 doChatWithRag 是否真正使用了 QuestionAnswerAdvisor；
最后看日志或 Debug，确认检索结果是否为空。
```

#### 问：如果项目启动时报数据库连接失败，和 Day4 RAG 有什么关系？

答：

```text
如果当前使用的是 PgVectorStore，那么项目启动时会连接 PostgreSQL，并把文档写入 pgvector。
如果数据库没有启动或端口配置不对，VectorStore Bean 创建失败，Spring 容器启动就可能失败。
如果只是使用 SimpleVectorStore，则不依赖 PostgreSQL。
```

#### 问：如果 AI 回答还是很泛泛，没有体现知识库，可能是什么原因？

答：

```text
可能是知识库文档质量不够；
也可能是文档没有成功写入 VectorStore；
还可能是检索没有命中相关文档；
或者 QuestionAnswerAdvisor 没有真正加入 ChatClient 调用链。
另外，文档切片过长或过短也会影响检索效果。
```

## 13. 简历表达

### 这部分面试官可能怎么考察？

面试官一般不会只问“RAG 是什么”，更可能这样问：

```text
1. 你项目里的 RAG 是怎么实现的？
2. 用户提问后，代码链路是怎么走的？
3. 你为什么要用向量库？
4. Embedding 在其中起什么作用？
5. 你用的是本地知识库还是云知识库？
6. 如果 RAG 不生效，你会怎么排查？
7. SimpleVectorStore 和 PgVectorStore 有什么区别？
8. 这块功能对业务有什么价值？
```

### 我应该怎么回答？

可以用这段作为项目介绍：

```text
我在 AI 恋爱大师项目中实现了 RAG 知识库问答能力。
具体做法是：先将恋爱相关的 Markdown 问答文档放到 resources/document 目录下，项目启动时通过 Spring AI 的 MarkdownDocumentReader 读取成 Document，再通过 EmbeddingModel 转成向量并写入 VectorStore。
用户提问时，会进入 LoveApp 的 doChatWithRag 方法，ChatClient 在调用大模型前会通过 QuestionAnswerAdvisor 从 VectorStore 检索相关文档，并把检索结果作为上下文拼接到 Prompt 中，最后让大模型基于这些资料生成回答。
这个功能解决了普通大模型不了解项目私有知识、回答容易泛泛或幻觉的问题。
```

如果面试官继续追问“你在里面负责了什么”，可以这样讲：

```text
我主要理解并实现了 RAG 的基础链路，包括本地 Markdown 文档加载、Document 转换、VectorStore 初始化，以及在 ChatClient 调用链中接入 QuestionAnswerAdvisor。
同时我也了解了云知识库方案，可以通过 DashScopeDocumentRetriever 和 RetrievalAugmentationAdvisor 对接阿里云百炼知识库。
```

如果简历要写，可以写成：

```text
基于 Spring AI 实现 AI 恋爱知识库问答能力：通过 MarkdownDocumentReader 加载本地恋爱知识文档，结合 EmbeddingModel 与 VectorStore 完成文档向量化存储，并使用 QuestionAnswerAdvisor 在 ChatClient 调用前检索相关知识片段，提升 AI 回答的领域准确性和业务相关性。
```

更口语一点的面试表达：

```text
这块我理解成给 AI 加了一个“查资料再回答”的能力。项目启动时先把 Markdown 恋爱文档处理成向量知识库；用户提问时，Advisor 先从知识库里找相关内容，再把这些内容交给大模型生成回答。
```
