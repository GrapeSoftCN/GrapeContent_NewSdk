package Test;

import cache.CacheHelper;
import httpServer.booter;
import nlogger.nlogger;
import string.StringHelper;

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

        // JSONObject object = new JSONObject("mainName", "测试").puts("content",
        // "测试信息").puts("GovId", "1");
        // System.out.println(object);
        // System.out.println(codec.encodeFastJSON(object.toString()));

        // CacheHelper helper = new CacheHelper();
        // String key = "test_key";
        // String value = helper.get(key);
        // if (StringHelper.InvaildString(value)) {
        // System.out.println("1: "+value);
        // }
        // helper.setget(key, "test", 86400);
        // System.out.println("test");
    }
}
