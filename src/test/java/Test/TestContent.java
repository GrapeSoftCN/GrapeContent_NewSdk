package Test;

import httpServer.booter;
import nlogger.nlogger;

public class TestContent {
    public static void main(String[] args) {
        booter booter = new booter();
        try {
            System.out.println("GrapeContent");
            System.setProperty("AppName", "GrapeContent");
            booter.start(1008);
        } catch (Exception e) {
            nlogger.logout(e);
        }
        
//        JSONObject object = new JSONObject("mainName", "测试").puts("content", "测试信息").puts("GovId", "1");
//        System.out.println(object);
//        System.out.println(codec.encodeFastJSON(object.toString()));
    }
}
