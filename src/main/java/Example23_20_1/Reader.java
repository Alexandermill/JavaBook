package Example23_20_1;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.sql.SQLOutput;
import java.util.concurrent.Exchanger;

public class Reader {

    private static final Exchanger<String> EXCHANGER = new Exchanger<>();

    public static void main(String[] args) {

        new Thread(new Parser(0, 200)).start();
        new Thread(new LineReader(80)).start();


    }

    public static class Parser implements Runnable {

        long startPoint;
        int buffer;
        String result;

        public Parser(long startPoint, int buffer) {
            this.startPoint = startPoint;
            this.buffer = buffer;
        }

        @Override
        public void run() {
            System.out.printf("Parser начал работу. Читаю %d символов, начиная с %d ", buffer, startPoint);
            try {
//                result = seekableReader(Paths.get("files\\input.txt"), buffer, startPoint);
//                System.out.printf("Parser прочитал: %s", result);
                for (int i = 0; i < 6; i++) {
                    result = seekableReader(Paths.get("files\\input.txt"), buffer, startPoint);
//                    System.out.printf("Parser прочитал: %s", result);
                    try {
                        String strStartPoint = EXCHANGER.exchange(result);
                        startPoint = Long.valueOf(strStartPoint);
//                        System.out.println("=============== Parser отдал result и обновил startpoint на -" + startPoint);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }

            } catch (IOException e) {
                throw new RuntimeException(e);
            } finally {

            }





        }

        public String seekableReader(Path path, int buffer, long startPoint) throws IOException {
            SeekableByteChannel sbc = Files.newByteChannel(path, StandardOpenOption.READ);
            ByteBuffer byteBuffer = ByteBuffer.allocate(buffer);
            StringBuilder sb = new StringBuilder();
            String separator = System.lineSeparator();
            String result;

            sbc.position(startPoint);
            sbc.read(byteBuffer);
            result =  new String(byteBuffer.array());

            return result.replace(separator, " ");
        }

    }

    public static class LineReader implements Runnable {

        String input;
        int charNumber;
        long skip = 0L;

        public LineReader(int charNumber) {
            this.charNumber = charNumber;
        }

        @Override
        public void run() {

            System.out.println("LineReader начал работу");

            for (int i = 0; i < 6; i++) {

                try {

                    input = EXCHANGER.exchange(String.valueOf(skip));
                    lineRead(input, charNumber);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }

        }

        public void lineRead (String line, int charInLine){

            char[] chars = line.toCharArray();
            String temp = "";
            StringBuilder stringBuilder = new StringBuilder();
            int count = 0;

            for (int i = 0; i < chars.length; i++) {
                temp = temp + chars[i];
                if(count == charInLine){
//                    System.out.println("LineReader: " + getSepLine(temp));
                    stringBuilder.append(getSepLine(temp) + " " + skip);
                    count = 0;
                    temp = "";
                }

                count++;
            }

            System.out.println(stringBuilder.toString());
            skip += stringBuilder.length();

        }

        private String getSepLine (String line){
            return line.substring(0, line.lastIndexOf(" "));
        }


        }
}

