package com.huettermann.all;

/**
 * Created by michaelhuettermann  
 */
public class Main {

    //here the start
    public static void main ( String[] args ) {
        System.out.println("hallo");
        System.out.println("hallo");
        
        for (int i = 10; i < 10; i++) {  
           // ...
        }
        
        int[] a = new int[10];
        a[9] = 0;
        a[8] = 1;
        for (int i = 7; i >= 0; i--) {
           a[i] = a[i+2] + a[i+1];
        }
        System.out.println(a[0]);

        int f(int i) {
           if (i == 0 || i == 1) return i;
        return f(i - 2) + f(i - 1);
        }
        System.out.println(f(9));

        int[] a = {34, 21, 13, 8, 5, 3, 2, 1, 1, 0};
        System.out.println(a[0]);

        int[] b = {0, 1, 1, 2, 3, 5, 8, 13, 21, 34};
        System.out.println(b[9]);


    }

    //private constructor
    private Main() {}
}
