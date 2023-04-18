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


    public static void main(String[] args) {

        AtomicLong cursor = new AtomicLong(0);
        BlockingQueue<String> blockingQueue = new ArrayBlockingQueue<>(1024);

        new Thread(new Reader(blockingQueue, cursor, "files\\input.txt")).start();
        new Thread(new Parser(blockingQueue, cursor, 80)).start();


    }

    public static class Parser implements Runnable {

        BlockingQueue<String> blockingQueue = null;
        AtomicLong cursor = null;
        int chunkSize;

        public Parser(BlockingQueue<String> blockingQueue, AtomicLong cursor, int chunkSize) {
            this.blockingQueue = blockingQueue;
            this.cursor = cursor;
            this.chunkSize = chunkSize;
        }

        @Override
        public void run() {
            System.out.println("Parser начал работу." +Thread.currentThread().getName());
            while (true){
                String line = null;
                String temp;
                try {
                    line = blockingQueue.take();
                    temp = line.substring(0, chunkSize);
                    temp = temp.substring(0, temp.lastIndexOf(" "));

                    System.out.println("Parser update cursor from " + cursor + " ");
                    cursor.addAndGet(Long.valueOf(temp.length()));
                    System.out.print("on" + cursor + " \n");
                    System.out.println(temp);


                    if(line.equals("EOF")){
                        break;
                    }

                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }

        }

    }

    public static class Reader implements Runnable {

        BlockingQueue<String> blockingQueue = null;
        AtomicLong cursor = null;
        String path;

        public Reader(BlockingQueue<String> blockingQueue, AtomicLong cursor, String path) {
            this.blockingQueue = blockingQueue;
            this.cursor = cursor;
            this.path = path;
        }

        @Override
        public void run() {

            System.out.println("Reader начал работу в потоке "+Thread.currentThread().getName());
            SeekableByteChannel sbc = null;
            try {
                sbc = Files.newByteChannel(Paths.get(path), StandardOpenOption.READ);

                ByteBuffer byteBuffer = ByteBuffer.allocate(100);
                int buferSize=0;
                long oldCursor = 0;

                while (true) {

                    if (oldCursor == cursor.get()){
                        System.out.println("Cursor not updated " + oldCursor);
                    }
                    System.out.println("start on cursor "+cursor);
                    sbc.position(cursor.get());
                    buferSize = sbc.read(byteBuffer);
                    blockingQueue.put(new String(byteBuffer.array()));
//                    System.out.println(new String(byteBuffer.array()));
                    oldCursor = cursor.get();

                    byteBuffer.clear();
                        if(buferSize < 0){
                            blockingQueue.put("EOF");
                            break;
                        }

                    }


            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

        }
    }
}

