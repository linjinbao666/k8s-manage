package ama;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Test1 {

    public static void main(String[] args) {
//        String line = "sdsd,TVlTUUxfUk9PVF9QQVNTV09SRA==";
//        String pattern = "^,,[\\w+(,\\w+)*]";
//        Pattern r = Pattern.compile(pattern);
//        Matcher m = r.matcher(line);
//        System.out.println(m.find());
        String s = ",";
        String[] split = s.split(",");
        for (String s1 : split){
            System.out.println(s1);
        }
        System.out.println(split[0]);
    }
}
