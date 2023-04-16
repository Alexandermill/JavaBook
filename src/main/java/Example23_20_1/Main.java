package Example23_20_1;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

public class Main {
    public static void main(String[] args) throws IOException {

        Path path = Path.of("C:\\Users\\user\\Desktop\\JavaBook\\files\\input.txt");

        readFixCharInLines(path, 20);


    }

    static void readFixCharInLines(Path path, int charInLine) throws IOException {

        boolean flag = true;
        String line = " ";
        String temp;
        String lineSepBySpace;
        long skip = 0l;

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

            if(lineSepBySpace.substring(0,1).equals(" ")){
                System.out.println(lineSepBySpace.substring(1, lineSepBySpace.length()));
            } else System.out.println(lineSepBySpace);

            skip += Long.valueOf(lineSepBySpace.length());
        }

        raf.close();


    }

}
