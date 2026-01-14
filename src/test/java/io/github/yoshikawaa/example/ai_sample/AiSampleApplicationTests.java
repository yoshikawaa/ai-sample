package io.github.yoshikawaa.example.ai_sample;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class AiSampleApplicationTests {

    @Test
    void testMainMethod() {
        // main メソッドを呼び出してカバレッジを上げる
        AiSampleApplication.main(new String[] {});
    }
}