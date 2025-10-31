package main;
import library.Entity;

public class Main {
    public static void main(String[] args) {
        // your code goes here
    	System.out.println("Hello Server");
    	Entity entity = new Entity();
    	
    	entity.setName("Test Debug Name");

    	System.out.println(entity.getName());
    }
}