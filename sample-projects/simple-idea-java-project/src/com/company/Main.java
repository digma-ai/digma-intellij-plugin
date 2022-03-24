package com.company;

public class Main {

    public static void main(String[] args) {

        Main main = new Main();
        int result = main.multiply(2,2);

        String asString = asString(result);

        System.out.println("got "+asString);

    }

    private static String asString(int number) {
        return Integer.toString(number);
    }


    private int multiply(int x , int y){
        return x*y;
    }


}
