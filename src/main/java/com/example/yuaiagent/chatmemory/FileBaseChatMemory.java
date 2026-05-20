package com.example.yuaiagent.chatmemory;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import org.objenesis.strategy.StdInstantiatorStrategy;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class FileBaseChatMemory implements ChatMemory {

    private final String BASE_DIR;

    private static final Kryo kryo = new Kryo();


    // 这个是什么写法？
    static {
        kryo.setRegistrationRequired(false);

        // 设置实例化策略
        kryo.setInstantiatorStrategy(new StdInstantiatorStrategy());
    }


    // 构建对象时，指定文件保存目录
    public FileBaseChatMemory(String dir){
        this.BASE_DIR = dir;
        File baseDir = new File(dir);
        if(!baseDir.exists()){
            baseDir.mkdirs();
        }
    }



//    @Override
//    public void add(String conversationId, Message message) {
//        saveConversation(conversationId, List.of(message));
//    }
    // 逻辑有问题, 应该是读取到消息列表, 然后单独添加进去,不是将这个单个消息直接作为一个消息列表持久化
    @Override
    public void add(String conversationId, Message message) {
        List<Message> conversationMessage = getOrCreateConversation(conversationId);
        conversationMessage.add(message);
        saveConversation(conversationId, conversationMessage);
    }

    @Override
    public void add(String conversationId, List<Message> messages) {
        List<Message> messageList = getOrCreateConversation(conversationId);
        messageList.addAll(messages);
        saveConversation(conversationId, messageList);

    }

    @Override
    public List<Message> get(String conversationId, int lastN) {
        List<Message> messageList = getOrCreateConversation(conversationId);
        return messageList.stream()
                .skip(Math.max(0, messageList.size() - lastN))
                .toList();
    }

    @Override
    public void clear(String conversationId) {
        File file = getConversationFile(conversationId);
        if (file.exists()){
            file.delete();
        }
    }

    private List<Message> getOrCreateConversation(String conversationId) {
        File file = getConversationFile(conversationId);
        List<Message> messages = new ArrayList<>();
        if (file.exists()) {
            try (Input input = new Input(new FileInputStream(file))) {
                messages = kryo.readObject(input, ArrayList.class);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return messages;
    }

    private void saveConversation(String conversationId, List<Message> messages) {
        File file = getConversationFile(conversationId);
        try (Output output = new Output(new FileOutputStream(file))) {
            kryo.writeObject(output, messages);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private File getConversationFile(String conversationId) {
        return new File(BASE_DIR, conversationId + ".kryo");
    }


}



