package org.example.aicoderhelper.ai;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class AiCodeHelperServiceTest {

    @Resource
    private AiCodeHelperService aiCodeHelperService;
    @Test
    void chat() {
        String result = aiCodeHelperService.chat("你好， 我是程序员虞泽贤");
        System.out.println(result);
    }

    @Test
    void chatWithMemory() {
        String result = aiCodeHelperService.chat("你好， 我是程序员虞泽贤");
        System.out.println(result);
        String result2 = aiCodeHelperService.chat("请问我是谁");
        System.out.println(result2);
    }

    @Test
    void chatForReport() {
        AiCodeHelperService.Report report = aiCodeHelperService.chatForReport("你好， 我是程序员虞泽贤");
        System.out.println(report);
    }

    @Test
    void chatWithRag() {
        String result = aiCodeHelperService.chat("怎么学习java，有哪些面试题");
        System.out.println(result);
    }

    @Test
    void chatWithTools() {
        String result = aiCodeHelperService.chat("有哪些常见的计算机网络面试题");
        System.out.println(result);
    }

    @Test
    void chatWithMcp() {
        String result = aiCodeHelperService.chat("什么是程序员鱼皮的编程导航");
        System.out.println(result);
    }

    @Test
    void chatWithGuardrail() {
        String result = aiCodeHelperService.chat("hello");
        System.out.println(result);
    }
}