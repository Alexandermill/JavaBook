package Example23_20_1;


import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

/*
Идея такая:
сласс/поток Ридер - читает файл
класс/поток Парсер - обрабатывает строчки
Делаем 2 общих ресурса:
1 - Блокирующая очередь - для передачи строк от Ридера к Парсеру
2 - атомарный int для передачи курсора от парсера к ридеру

что пишем в Очередь ? Одну большую стрингу Строки ? Слова (с разделением по пробелу?):
слова - плохо, будет геморно вычислить курсор в парсере
попробуем одну большую стрингу.

работает так:
ридер считывает кусок текста начиная с Курсора=0 (устанавливаем в Конструкторе) и складывает в Очередь
парсер читает из Очереди и обновляет Курсор
повторяем ---
в коце ридер запиысывает EOF
парсер при чтении EOF останавливает работу

План:
1) пишем Ридер.
2) пишем Парсер.

 */
public class Launcher {


    public static void main(String[] args) throws InterruptedException {

        AtomicLong cursor = new AtomicLong(0);
        BlockingQueue<String> blockingQueue = new ArrayBlockingQueue<>(1024);
        LineExchange lineExchange = new LineExchange(cursor, blockingQueue);

//        new Thread(new Reader(blockingQueue, cursor, "files\\input.txt")).start();
//        new Thread(new Parser(blockingQueue, cursor, 80)).start();

        new Thread(new Reader(lineExchange, "files\\input.txt", 100)).start();
//        Thread.sleep(2000);
        new Thread(new Parser(lineExchange, 80)).start();


    }

    public static class LineExchange{
        AtomicLong cursor = new AtomicLong(0);
        BlockingQueue<String> blockingQueue = new ArrayBlockingQueue<>(2);

        public LineExchange(AtomicLong cursor, BlockingQueue<String> blockingQueue) {
            this.cursor = cursor;
            this.blockingQueue = blockingQueue;
        }

        public void addLine(String line) throws InterruptedException {
            blockingQueue.put(line);

        }

        public String takeLine(){
            try {
                return blockingQueue.take();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        public synchronized Long getCursor(Long oldCursor) {
            while (oldCursor == cursor.get()){
                try {
                    System.out.println("\n" + Thread.currentThread()+ " start wait\n");
                    wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            return cursor.get();
        }

        public synchronized void addCursor(Long newCursor){
            cursor.addAndGet(newCursor);
            notify();
        }

    }

    public static class Parser implements Runnable {

        BlockingQueue<String> blockingQueue = null;
        AtomicLong cursor = null;

        LineExchange lineExchange = null;
        int chunkSize;

        public Parser(BlockingQueue<String> blockingQueue, AtomicLong cursor, int chunkSize) {
            this.blockingQueue = blockingQueue;
            this.cursor = cursor;
            this.chunkSize = chunkSize;
        }

        public Parser(LineExchange lineExchange, int chunkSize) {
            this.lineExchange = lineExchange;
            this.chunkSize = chunkSize;
        }

        @Override
        public void run() {
            System.out.println("Parser начал работу." +Thread.currentThread().getName());
            while (true){
//                try {
//                    Thread.sleep(100);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
                String line = null;
                String temp;
                line = lineExchange.takeLine();

                if(line.length() > chunkSize){
                    temp = line.substring(0, chunkSize);
                    temp = temp.substring(0, temp.lastIndexOf(" "));
                } else temp = line;



                lineExchange.addCursor(Long.valueOf(temp.length()));
//                System.out.print("Parser update cursor on" + temp.length() + " \n");
//                System.out.println(temp);


                if(line.equals("EOF")){
                    break;
                }

            }

        }

    }

    public static class Reader implements Runnable {

        LineExchange lineExchange = null;
        String path;
        int initBufferSize;

        public Reader(LineExchange lineExchange, String path, int bufferSize) {
            this.lineExchange = lineExchange;
            this.path = path;
            this.initBufferSize = bufferSize;
        }

        @Override
        public void run() {

            System.out.println("Reader начал работу в потоке "+Thread.currentThread().getName());
            SeekableByteChannel sbc = null;
            try {
                sbc = Files.newByteChannel(Paths.get(path), StandardOpenOption.READ);

                ByteBuffer byteBuffer = ByteBuffer.allocate(initBufferSize);
                int buferSize=0;
                long oldCursor = -1;

                while (true) {
//                    Thread.sleep(100);
                    byteBuffer.clear();
                    long newCursor = lineExchange.getCursor(oldCursor);
                    sbc.position(newCursor);
                    buferSize = sbc.read(byteBuffer);

//                    System.out.print("bufSize<capacity "+ (buferSize < byteBuffer.capacity())+" "+buferSize+" "+byteBuffer.capacity()+" ");


                    if(buferSize < 0){
                        lineExchange.addLine("EOF");
                        System.out.print(" send in Q: EOF\n");
                        break;
                    }

                    if(buferSize < byteBuffer.capacity()){
                        byteBuffer.clear();
                        byteBuffer = ByteBuffer.allocate(buferSize);
                        sbc.position(newCursor);
                        sbc.read(byteBuffer);
                    }


//                    System.out.print("get cursor: "+ newCursor+ " ");


                    lineExchange.addLine(new String(byteBuffer.array()));
//                    System.out.print(" change cursor from" + oldCursor +" on "+ newCursor + "bufsize: "+buferSize);
                    System.out.print(" send in Q: "+ new String(byteBuffer.array())+"\n");
                    oldCursor = newCursor;






                    }


            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

        }
    }
}

