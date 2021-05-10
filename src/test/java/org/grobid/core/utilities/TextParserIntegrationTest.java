package org.grobid.core.utilities;

import org.grobid.core.data.Sentence;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;

@Ignore
public class TextParserIntegrationTest {

    TextParser target;

    @Before
    public void setUp() throws Exception {
        target = TextParser.getInstance();
    }

    /** This test is specific to test a concurrency issue with clear NLP **/
    @Test
    public void testSafeThreadWhenParsingSentence_shouldWork() throws Exception {

        Runnable thread1 = new FakeThread(target);
        FakeThread thread2 = new FakeThread(target);
        FakeThread thread3 = new FakeThread(target);

        final Thread thread = new Thread(thread1);
        thread.start();
        final Thread thread4 = new Thread(thread2);
        thread4.start();
        final Thread thread5 = new Thread(thread3);
        thread5.start();

        thread5.join();

        
    }

}

class FakeThread implements Runnable{

    TextParser parser; 

    public FakeThread(TextParser parser) {
        this.parser = parser;
    }

    @Override
    public void run() {
        final Sentence parsedSentence = parser.parse("The decision of one food bank in the western city of Essen to stop signing up more foreigners after migrants gradually became the majority of its users has prompted a storm of reaction in Essen, a former coal town in Germany’s rust belt, and across the country. ");

        System.out.println(parsedSentence.toString());
    }
}