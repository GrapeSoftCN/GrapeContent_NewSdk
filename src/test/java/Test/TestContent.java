package Test;

import httpServer.booter;
import nlogger.nlogger;

public class TestContent {
    public static void main(String[] args) {
        booter booter = new booter();
        try {
            System.out.println("Content");
            System.setProperty("AppName", "Content");
            booter.start(1006);
        } catch (Exception e) {
            nlogger.logout(e);
        } 
    }
}
