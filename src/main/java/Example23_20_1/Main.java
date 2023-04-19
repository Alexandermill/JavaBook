package Example23_20_1;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class Main {
    public static void main(String[] args) throws IOException {

        Path path = Path.of("files\\input.txt");

        // readFixCharInLines(path, 20);

        seekableReader(path, 80);


    }



    static void seekableReader(Path path, int charInLine) throws IOException{
        SeekableByteChannel sbc = Files.newByteChannel(path, StandardOpenOption.READ);
        ByteBuffer bufer = ByteBuffer.allocate(charInLine);

        String separator = System.lineSeparator();

        int i;
        boolean flag = true;
        String line;
        String temp;
        String lineSepBySpace;
        long skip = 0;

        while (flag){
            sbc.position(skip);

            if((i = sbc.read(bufer)) < charInLine){
                ByteBuffer bufer2 = ByteBuffer.allocate(i);
                sbc.position(skip);
                sbc.read(bufer2);
                line = new String(bufer2.array());
                lineSepBySpace = line;
                flag = false;
            } else {
                temp = new String(bufer.array());
                line = temp;

                if(temp.contains("\n")){
                    line = temp.replace(separator, " ");

                }
                
                lineSepBySpace = line.substring(0, line.lastIndexOf(" "));
            }

                      

            if(lineSepBySpace.charAt(0) == ' '){
                lineSepBySpace = lineSepBySpace.substring(1);
                skip += 1;
            }

            skip += lineSepBySpace.length();

            if(lineSepBySpace.length() < charInLine && charInLine - lineSepBySpace.length() != 1){
                int spaces = (charInLine - lineSepBySpace.length())/2;
                lineSepBySpace = " ".repeat(spaces) + lineSepBySpace + " ".repeat(spaces);
            }
            System.out.println(lineSepBySpace);
            bufer.clear();
        }

    }

    static void readFixCharInLines(Path path, int charInLine) throws IOException {

        boolean flag = true;
        String line;
        String temp;
        String lineSepBySpace;
        long skip = 0L;

        RandomAccessFile raf = new RandomAccessFile(path.toFile(), "r");

        while (flag){
            raf.seek(skip);
            line = raf.readLine();

            if (line.length() >= charInLine){
                temp = line.substring(0, charInLine);
                lineSepBySpace = temp.substring(0, temp.lastIndexOf(" "));
            } else {
                lineSepBySpace = line;
                flag = false;
            }

            if(lineSepBySpace.charAt(0) == ' '){
                System.out.println(lineSepBySpace.substring(1));
            } else System.out.println(lineSepBySpace);

            skip += lineSepBySpace.length();
        }

        raf.close();


    }

}
