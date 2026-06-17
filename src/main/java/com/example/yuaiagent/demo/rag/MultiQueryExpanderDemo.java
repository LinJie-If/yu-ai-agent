package com.example.yuaiagent.demo.rag;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.preretrieval.query.expansion.MultiQueryExpander;
import org.springframework.stereotype.Component;


import java.util.List;

/**
 * 查询拓展器demo
 */
@Component
public class MultiQueryExpanderDemo {


    private final ChatClient.Builder chatClientBuilder;

    public MultiQueryExpanderDemo(ChatModel dashcopeChatModel){
        this.chatClientBuilder = ChatClient.builder(dashcopeChatModel);
    }

    public List<Query> expand(String query) {
        MultiQueryExpander queryExpander = MultiQueryExpander.builder()
                .chatClientBuilder(chatClientBuilder)
                .numberOfQueries(3)
                .build();

        List<Query> queries = queryExpander.expand(new Query("谁是程序员鱼皮"));
        return queries;
    }

}
