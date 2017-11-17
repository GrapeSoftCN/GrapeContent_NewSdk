package Test;

import org.json.simple.JSONObject;

import httpServer.booter;
import nlogger.nlogger;
import security.codec;

public class TestContent {
    public static void main(String[] args) {
        booter booter = new booter();
        try {
            System.out.println("GrapeContent");
            System.setProperty("AppName", "GrapeContent");
            booter.start(1006);
        } catch (Exception e) {
            nlogger.logout(e);
        } 
    	
//    	String string = "eyJpbWFnZSI6IiIsImlzdmlzYmxlIjowLCJvZ2lkIjoiNTk5ZTllNDgzNWJkMDkxNDU0NjdiZGE1Iiwic29ydCI6MCwib2lkIjoiIiwicmVhZENvdW50IjowLCJjb250ZW50IjoiYXNoZGlhc2hkYWNqamRqc2JmamFua2RuamFmbnVlaXdod2ZuYWRqc25mIiwic2xldmVsIjowLCJkZXNwIjoiIiwid2JpZCI6IjU5OWU3ODkxMzViZDA5MTdkNDI3ZGNjMyIsIm93bmlkIjowLCJhdHRyaWQiOjAsInNvdWNlIjoi6KeC5rmW56S@w5Yy6Iiwic3Vic3RhdGUiOjAsInN1Yk5hbWUiOm51bGwsIm1haW5OYW1lIjoic2Rhc2RhcyIsIm1hbmFnZWlkIjowLCJmYXRoZXJpZCI6MCwiaXNkZWxldGUiOjAsInN0YXRlIjpudWxsLCJhdXRob3IiOiJzcyIsInRpbWUiOjE1MTA5MjQzODAwMDAsImF0dHJpYnV0ZSI6IjAiLCJpc1N1ZmZpeCI6MH0@m";
//    	System.out.println(codec.DecodeFastJSON(string));
//    	string = codec.DecodeFastJSON(string);
//    	System.out.println(JSONObject.toJSON(string));
    }
}
