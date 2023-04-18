package Example23_20_1;


import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
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

        AtomicLong cursor = new AtomicLong();
        BlockingQueue<String> blockingQueue = new ArrayBlockingQueue<>(1024);


    }

    public static class Parser implements Runnable {

        BlockingQueue<String> blockingQueue = null;
        long startPoint;
        int buffer;


        @Override
        public void run() {
            System.out.println("Parser начал работу.");

        }

    }

    public static class Reader implements Runnable {

        BlockingQueue<String> blockingQueue = null;
        int chunkSize;
        AtomicLong cursor = null;
        String path;


        @Override
        public void run() {

            System.out.println("LineReader начал работу");
            SeekableByteChannel sbc = null;
            try {
                sbc = Files.newByteChannel(Paths.get(path));

                ByteBuffer byteBuffer = ByteBuffer.allocate(1024);

                while (sbc.read(byteBuffer) < 0) {
                    sbc.position(cursor.get());
                    blockingQueue.put(new String(byteBuffer.array()));
                    byteBuffer.clear();
                }
                blockingQueue.put("EOF");

            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

        }
    }
}

