package Test;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.bson.types.ObjectId;
import org.json.simple.JSONObject;

import httpServer.booter;
import nlogger.nlogger;
import privacyPolicy.privacyPolicy;
import security.codec;
import time.TimeHelper;

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
//    	String string = "eyJpbWFnZSI6IiIsImlzdmlzYmxlIjowLCJvZ2lkIjoiNTk5ZTllNDgzNWJkMDkxNDU0NjdiZGE1Iiwic29ydCI6MCwib2lkIjoiIiwicmVhZENvdW50IjowLCJjb250ZW50IjoiYXNoZGlhc2hkYWNqamRqc2JmamFua2RuamFmbnVlaXdod2ZuYWRqc25mIiwic2xldmVsIjowLCJkZXNwIjoiIiwid2JpZCI6IjU5OWU3ODkxMzViZDA5MTdkNDI3ZGNjMyIsIm93bmlkIjowLCJhdHRyaWQiOjAsInNvdWNlIjoi6KeC5rmW56S@w5Yy6Iiwic3Vic3RhdGUiOjAsInN1Yk5hbWUiOm51bGwsIm1haW5OYW1lIjoic2Rhc2RhcyIsIm1hbmFnZWlkIjowLCJmYXRoZXJpZCI6MCwiaXNkZWxldGUiOjAsInN0YXRlIjpudWxsLCJhdXRob3IiOiJzcyIsInRpbWUiOjE1MTA5MjQzODAwMDAsImF0dHJpYnV0ZSI6IjAiLCJpc1N1ZmZpeCI6MH0@m";
//    	System.out.println(codec.DecodeFastJSON(string));
//    	string = codec.DecodeFastJSON(string);
//    	System.out.println(JSONObject.toJSON(string));
        
//        
//        Date dates = null;
//        try {
//            
//            String date = TimeHelper.stampToDate(TimeHelper.nowMillis());
//            System.out.println(date);
//            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
//            dates = sdf.parse(date);
//            System.out.println(sdf.format(dates));
//        } catch (Exception e) {
//            nlogger.logout(e);
//        }
//        System.out.println(Integer.toHexString(200));
//        System.out.println(Integer.parseInt("c8", 16));
    }
}
