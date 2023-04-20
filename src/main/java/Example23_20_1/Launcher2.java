package Example23_20_1;


import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
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
public class Launcher2 {


    public static void main(String[] args) throws IOException {

        AtomicLong cursor = new AtomicLong(0);
        BlockingQueue<String> blockingQueue = new ArrayBlockingQueue<>(1024);

        String path = "files\\input.txt";
        SeekableByteChannel channel = Files.newByteChannel(Paths.get(path), StandardOpenOption.READ, StandardOpenOption.WRITE);
        LineExchange lineExchange = new LineExchange(cursor, blockingQueue, channel);


        new Thread(new Reader(lineExchange, path, 100)).start();
//        Thread.sleep(2000);
        new Thread(new Parser(lineExchange, 80, path)).start();


    }

    public static class LineExchange{
        AtomicLong cursor = null;
        BlockingQueue<String> blockingQueue = null;

        SeekableByteChannel channel = null;

        public LineExchange(AtomicLong cursor, BlockingQueue<String> blockingQueue, SeekableByteChannel channel) {
            this.cursor = cursor;
            this.blockingQueue = blockingQueue;
            this.channel = channel;
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
                    wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            return cursor.get();
        }

        public synchronized Long getOldCursor(){
            return cursor.get();
        }

        public synchronized void addCursor(Long newCursor){
            cursor.addAndGet(newCursor);
            notify();
        }

        public synchronized SeekableByteChannel getChannel(){
            return channel;
        }

    }

    public static class Parser implements Runnable {

        LineExchange lineExchange = null;
        int chunkSize;

        String path;

        public Parser(LineExchange lineExchange, int chunkSize, String path) {
            this.lineExchange = lineExchange;
            this.chunkSize = chunkSize;
            this.path = path;
        }

        @Override
        public void run() {
            System.out.println("Parser начал работу." +Thread.currentThread().getName());
            String separator = System.lineSeparator();
            long oldCursor;
            long newCursor;

            SeekableByteChannel channel = lineExchange.getChannel();


            while (true){
//                try {
//                    Thread.sleep(100);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
                String line = null;
                String temp;
                long incrementCursor = 0;
                List<Integer> spaceIndexes = new ArrayList<>();
                line = lineExchange.takeLine(); //получаем строку из файла
                line = line.substring(0, chunkSize); // обрезаем строку до размера chunkSize
                line = line.substring(0, line.lastIndexOf(" ")); // обрезаем строку до последнего пробела
                oldCursor = lineExchange.getOldCursor(); // получаем начальную позицию
                System.out.println(oldCursor);
                newCursor = oldCursor + line.length(); // определяем позицию конца нашей строки - нужно будет чтобы вставить перенос
                int numberSpaces = 0;
                ByteBuffer byteBuffer;

                System.out.println(line);

                if(line.length() < chunkSize) {                        // определяем сколько пробелов нужно будет вставить
                    numberSpaces = chunkSize - line.length();

                    int count =1;
                    int index = line.indexOf(" ");
                    while (index >= 0 && count <= numberSpaces) {                                   // определяем инжексы пробелов в строке
                        spaceIndexes.add(index);
                        index = line.indexOf(" ", index + 1);
                        try {
                            System.out.print("\nstep on pos: "+oldCursor + index);
                            if(index > -1){
                                channel.position(oldCursor + index);                        // расставляем пробелы
                                byteBuffer = ByteBuffer.wrap("  ".getBytes(StandardCharsets.UTF_8));
                                System.out.print(" write \" \" \n");
                                channel.write(byteBuffer);
                                byteBuffer.clear();
                                count++;
                            }

                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }

                newCursor = newCursor + numberSpaces;

                byteBuffer = ByteBuffer.wrap(separator.getBytes(StandardCharsets.UTF_8));
                try {
                    channel.position(newCursor + 1);
                    channel.write(byteBuffer);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                lineExchange.addCursor(newCursor - oldCursor);

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
            SeekableByteChannel sbc = lineExchange.getChannel();
            try {


                ByteBuffer byteBuffer = ByteBuffer.allocate(initBufferSize);
                int buferSize=0;
                long oldCursor = -1;

                while (true) {
//                    Thread.sleep(100);

                    long newCursor = lineExchange.getCursor(oldCursor);
                    sbc.position(newCursor);
                    buferSize = sbc.read(byteBuffer);

                    if(buferSize < 0){
                        lineExchange.addLine("EOF");
                        break;
                    }

                    if(buferSize < byteBuffer.capacity()){
                        byteBuffer.clear();
                        byteBuffer = ByteBuffer.allocate(buferSize);
                        sbc.position(newCursor);
                        sbc.read(byteBuffer);
                    }

                    lineExchange.addLine(new String(byteBuffer.array()));
                    oldCursor = newCursor;
                    byteBuffer.clear();

                }


            } catch (IOException | InterruptedException e) {
                throw new RuntimeException(e);
            }

        }
    }
}

