package com.example.yuaiagent.app;

import com.example.yuaiagent.advisor.MyLoggerAdvisor;
import com.example.yuaiagent.advisor.ReReadingAdvisor;
import com.example.yuaiagent.chatmemory.FileBaseChatMemory;
import com.example.yuaiagent.rag.LoveAppRagCloudAdvisorConfig;
import com.example.yuaiagent.rag.LoveAppRagCustomAdvisorFactory;
import com.example.yuaiagent.rag.QueryRewriter;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.QuestionAnswerAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.vectorstore.VectorStore;
//import org.springframework.aop.Advisor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Vector;

import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY;
import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_RETRIEVE_SIZE_KEY;

@Component
@Slf4j
public class LoveApp {

    private final ChatClient chatClient;

    private static final String SYSTEM_PROMPT = "扮演深耕恋爱心理领域的专家。 开场向用户表明身份， 告知用户可倾诉恋爱难题。\n" +
            "“围绕单身、恋爱、已婚三种状态提问：单身状态询问社交圈拓展及追求心仪对象的困扰：” +\n" +
            "“恋爱状态询问沟通、习惯差异引发的矛盾：已婚状态询问家庭责任与亲属关系处理的问题。”\n" +
            "“引导用户详述事情经过、对方反应及自身想法，以便给出专属解决方案。";

    // 初始化ai-client
    public LoveApp(ChatModel dashscopeChatModel){
        // 初始化基于文件的对话记忆
        String fileDir = System.getProperty("user.dir") + "/tmp/chat-memory";
        ChatMemory chatMemory = new FileBaseChatMemory(fileDir);

        // 初始化基于内存的对话记忆
//        ChatMemory chatMemory = new InMemoryChatMemory();
        chatClient = ChatClient.builder(dashscopeChatModel)
                .defaultSystem(SYSTEM_PROMPT)
                .defaultAdvisors(
                        new MessageChatMemoryAdvisor(chatMemory),
                        // 自定义拦截器
                        new MyLoggerAdvisor()
                        // re2
//                        new ReReadingAdvisor()
                )
                .build();

    }

    // AI基础对话（支持多轮记忆）
    public String doChat(String message, String chatId){
        ChatResponse chatResponse = chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 1))
                .call().chatResponse();
        String content = chatResponse.getResult().getOutput().getText();
        log.info("content: {}", content);
        return content;

    }

    record LoveReport(String title, List<String> suggestions){

    }

    /**
     * AI 恋爱报告功能（结构化输出）
     * @param message
     * @param chatId
     * @return
     */
    public LoveReport doChatWithReport(String message, String chatId){
        LoveReport loveReport = chatClient
                .prompt()
                .system(SYSTEM_PROMPT + "每次对话后都要生成恋爱结果，标题为{用户名}的恋爱报告，内容为建议列表")
                .user(message)
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 1))
                .call()
                .entity(LoveReport.class);
            log.info("loveReport: {}", loveReport);
            return loveReport;

    }


    // rag-AI 恋爱知识问答功能
    @Resource
    private VectorStore loveAppVectorStore;

    @Resource
    private Advisor loveAppRagCloudAdvisor;

    @Resource
    private VectorStore pgVectorVectorStore;

    @Resource
    private QueryRewriter queryRewriter;

    public String doChatWithRag(String message, String chatId){
        // 查询改写
        String rewrittenMessage = queryRewriter.doQueryRewrite(message);

        ChatResponse chatResponse = chatClient
                .prompt()
//                .user(message)
                // 使用改写后的查询
                .user(rewrittenMessage)
                // ? 保存对话记忆，并且加载最近的10条
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId).param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                .advisors(new MyLoggerAdvisor())
                // 应用 rag知识库问答
                .advisors(new QuestionAnswerAdvisor(loveAppVectorStore))
//                .advisors(loveAppRagCloudAdvisor)
                // 启用 rag 检索增强服务，调用本地知识库 pgVector 向量存储
//                .advisors(new QuestionAnswerAdvisor(pgVectorVectorStore))

                // 应用 自定义的rag检索增强服务，文档查询器+上下文增强器
//                .advisors(
//                        LoveAppRagCustomAdvisorFactory.createLoveAppRagCustomAdvisor(
//                                loveAppVectorStore, "恋爱"
//                        )
//                )
                .call()
                .chatResponse();
        String content = chatResponse.getResult().getOutput().getText();
        log.info("content: {}", content);
        return content;


    }


    // ai调用工具功能
    @Resource
    private ToolCallback[] allTools;

    // ai恋爱报告功能-支持工具调用
    public String doChatWithTools(String message, String chatId){
        ChatResponse chatResponse = chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 1))
                .advisors(new MyLoggerAdvisor())
                .tools(allTools)
                .call()
                .chatResponse();
        String content = chatResponse.getResult().getOutput().getText();
        log.info("content: {}", content);
        return content;
    }

    // ai调用mcp服务
    @Resource
    private ToolCallbackProvider toolCallbackProvider;


    public String doChatWithMcp(String message, String chatId){
        ChatResponse chatResponse = chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 1))
                .advisors(new MyLoggerAdvisor())
                .tools(toolCallbackProvider)
                .call()
                .chatResponse();
        String content = chatResponse.getResult().getOutput().getText();
        log.info("content: {}", content);
        return content;
    }


}
