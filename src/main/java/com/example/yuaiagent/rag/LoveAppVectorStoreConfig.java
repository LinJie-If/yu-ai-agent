package com.example.yuaiagent.rag;

import java.util.List;

import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;

import jakarta.annotation.Resource;


public class LoveAppVectorStoreConfig {

    @Resource
    private MyTokenTextSplitter myTokenTextSplitter;

    @Resource
    private LoveAppDocumentLoader loveAppDocumentLoader;

    @Resource
    private MyKeywordEnricher myKeywordEnricher;

    @Bean
    VectorStore loveAppVectorStore(EmbeddingModel dashscopeEmbeddingModel) {
        SimpleVectorStore simpleVectorStore = SimpleVectorStore.builder(dashscopeEmbeddingModel)
                .build();

        // 加载文档
        List<Document> documents = loveAppDocumentLoader.loadMarkdowns();
        
        // 自主切分
//        List<Document> splitDocuments = myTokenTextSplitter.splitCustomized(documents);

        // 自动补充关键词元信息
        List<Document> enrichedDocument = myKeywordEnricher.enrichDocument(documents);
        simpleVectorStore.add(enrichedDocument);
        return simpleVectorStore;
    }

}
