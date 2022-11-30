package org.digma.intellij.plugin.test;

import org.digma.intellij.plugin.common.Retries;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class RetriesTests {

    private static class MyCounter{
        int c = 0;
        void plus(){
            c = c+1;
        }
    }


    private static class MyRunnable implements Runnable{

        private final String className;
        private final MyCounter myCounter;

        public MyRunnable(String className,MyCounter myCounter) {
            this.className = className;
            this.myCounter = myCounter;
        }

        @Override
        public void run() {
            myCounter.plus();
            try {
                Class.forName(className);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException("retry failed");
            }
        }
    }


    @Test
    void simpleRetryThrowsTest(){

        var counter = new MyCounter();

        RuntimeException runtimeException = Assertions.assertThrows(RuntimeException.class,() -> Retries.simpleRetry(new MyRunnable("my.non.existent.Clazz", counter),Throwable.class,10,5));

        Assertions.assertNotNull(runtimeException,"exception should not be null");
        Assertions.assertEquals("retry failed", runtimeException.getMessage(),"exception message should be equals");
        Assertions.assertEquals(5, counter.c, "Counter should be 5");
    }


    @Test
    void simpleRetrySuccessTest(){

        var counter = new MyCounter();

        Retries.simpleRetry(new MyRunnable("java.lang.String", counter),Throwable.class,10,5);

        Assertions.assertEquals(1, counter.c, "Counter should be 1");
    }

}
