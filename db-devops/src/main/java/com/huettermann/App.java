package com.huettermann;

import java.lang.System;

/**
 * Hello world!
 */
public class App 
{
    public static void main(String[] args) {
        Runnable r = () -> System.out.println("Hello World!");
        Thread t = new Thread(r);
        t.start();

    }
}

