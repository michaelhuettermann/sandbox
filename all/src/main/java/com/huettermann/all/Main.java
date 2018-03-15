package com.huettermann.all;

/**
 * Created by michaelhuettermann  
 */
public class Main {

    private Main main;

    public Main() {
        System.out.println("hallo");
        System.out.println("hallo");
        System.out.println("hallo");

        int a = 0;
        int b = 0;

        for (int i = 0; i < 10; i++) {
            // ...
        }

        int c = 0;
        int d = 0;

        for (int i = 0; i < 10; i++) {
            // ...
        }
    }

    public static void main ( String[] args ) {
        main = new Main();
    }

    public int getValue() {
        return 42;
    }

    public Main getMain() {
        return main;
    }

} 