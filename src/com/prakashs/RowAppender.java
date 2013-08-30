package com.prakashs;

import java.io.BufferedReader;
import java.io.FileReader;

/**
 * Created with IntelliJ IDEA.
 * User: sanprakash
 * Date: 8/26/13
 * Time: 7:42 PM
 * To change this template use File | Settings | File Templates.
 */
public class RowAppender {

    public static void main(String[] args) throws Exception{

        BufferedReader rdr = new BufferedReader(new FileReader(args[0]));

        String line = null;
        long row = 1;
        while((line = rdr.readLine()) != null){
            System.out.println(row + line);
            row++;
        }
    }
}
