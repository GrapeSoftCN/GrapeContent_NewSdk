package Model;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import apps.appsProxy;
import database.db;
import interfaceApplication.ContentGroup;
import interfaceModel.GrapeDBSpecField;
import interfaceModel.GrapeTreeDBModel;
import string.StringHelper;

/**
 * 文章数据统计
 * 
 *
 */
public class WsCount {
    private GrapeTreeDBModel content;
    private GrapeDBSpecField gDbSpecField;
    private ContentGroup group;
    private CommonModel model;
    
    public WsCount() {
        group = new ContentGroup();
        model = new CommonModel();
        
        content = new GrapeTreeDBModel();
        gDbSpecField = new GrapeDBSpecField();
        gDbSpecField.importDescription(appsProxy.tableConfig("Content"));
        content.descriptionModel(gDbSpecField);
        content.bindApp();
    }
    
    public JSONObject getChannleCount(String wbID,long startUT,long endUT){
        JSONArray channelArray = JSONArray.toJSONArray(group.getPrevColumn(wbID));
//        JSONArray channelArray = JSONArray.toJSONArray( (String) appsProxy.proxyCall("/GrapeContent/ContentGroup/getPrevColumn/s:" + wbID, null, null) );
        JSONArray newTree = rootTree(channelArray,"_id","fatherid");
        appendInfo2Tree(newTree,"_id","children",wbID,startUT,endUT);
        return new JSONObject(wbID,newTree);
    }
    
    @SuppressWarnings("unchecked")
    private void appendInfo2Tree(JSONArray array,String mid,String childid,String wid,long startUT,long endUT){
        JSONObject json,childJson;
        JSONArray childArray;
        String _id;
        long allCnt,argCnt,disArg,chking;
        for(Object _obj : array){
            json = (JSONObject)_obj;
            if( json.containsKey(childid) && !json.get(childid).toString().isEmpty() ){//对象有子对象
                _id = ((JSONObject)json.get(mid)).getString("$oid");
                childArray = (JSONArray)json.get(childid);  //获得子对象
                appendInfo2Tree(childArray,mid,childid,wid,startUT,endUT);      //为子对象获得数据
                //--为当前JSON获得数据
                allCnt = getChannelAllCount(wid, _id,startUT,endUT);
                argCnt = getChannelAgreeCount(wid, _id,startUT,endUT);
                disArg = getChannelDisagreeCount(wid, _id,startUT,endUT);
                chking = allCnt - argCnt - disArg;
                //--遍历子对象1层，把其对应值与本JSON累加，更新数据
                for(Object childObj : childArray){
                    childJson = (JSONObject)childObj;
                    allCnt += childJson.getLong("count");
                    argCnt += childJson.getLong("checked");
                    disArg += childJson.getLong("uncheck");
                    chking += childJson.getLong("checking");
                }
                json.put("count", allCnt);
                json.put("checked", argCnt);
                json.put("uncheck", disArg);
                json.put("checking", chking);
            }
        }
    }
    
    /**
     * @project GrapeContent
     * @package model
     * @file WsCount.java
     * 
     * @param array DATA数据源
     * @param mid   数据ID字段名称
     * @param fid   数据父ID字段名称
     * @return
     * 
     */
    @SuppressWarnings("unchecked")
    private JSONArray rootTree(JSONArray array,String mid,String fid){
        JSONObject json;
        JSONArray childArray;
        JSONArray newArray = new JSONArray();
        for(Object _obj : array){
            json = (JSONObject)_obj;
            if( json.get(fid).toString().equals("0") ){//是根数据
                //rjson.put(json.get(mid).toString(), null);
                childArray = line2tree(((JSONObject)json.get(mid)).getString("$oid"), array, mid, fid);//获得当前JSON对象的子对象组
                //----------这里填充附加对象数据
                json.put("children", childArray);
                //------------------------------
                newArray.add(json);
            }
        }
        return newArray;
    }
    
    //线性jsonarray转树形jsonobjct
    @SuppressWarnings("unchecked")
    private JSONArray line2tree(Object rootID, JSONArray array,String mid,String fid){
        JSONObject json;
        JSONArray newArray = new JSONArray();
        JSONArray childArray;
        //long allCnt,argCnt,disArg,chking;
        for(Object _obj : array){
            json = (JSONObject)_obj;
            
            if( json.get(fid).equals(rootID) ){ //当前数据的父对象是ROOT对象
                childArray = line2tree(((JSONObject)json.get(mid)).getString("$oid"), array, mid, fid);//获得当前JSON对象的子对象组
                //----------这里填充附加对象数据
                json.put("children", childArray);
                //------------------------------
                newArray.add(json);             //为当前ROOT创建子数组
            }
        }
        return newArray;
    }
    
    public JSONObject getAllCount(JSONObject robj, String rootID, String rootName, String fatherID) {
        return getAllCount(robj,rootID,rootName,fatherID,0,0);
    }

    @SuppressWarnings("unchecked")
    public JSONObject getAllCount(JSONObject robj, String rootID, String rootName, String fatherID,long startUT,long endUT) {
        // appsProxy.
        String[] trees = null;
        JSONObject nObj = new JSONObject();
        //String[] Allweb = getCid(rootID);
        rootID = model.getRWbid(rootID);
        long click = getClick(rootID, startUT, endUT);  //文章阅读量
        long allCnt = getCount(rootID,startUT,endUT);
        long argCnt = getAgreeCount(rootID,startUT,endUT);
        long disArg = getDisagreeCount(rootID,startUT,endUT);
        long chking = allCnt - argCnt - disArg;
        nObj.put("id", rootID);
        nObj.put("fatherid", fatherID);
        nObj.put("name", rootName);
        String tree = (String) appsProxy.proxyCall("/GrapeWebInfo/WebInfo/getChildrenweb/s:" + rootID);
        if (!tree.equals("")) {
            trees = tree.split(",");
        }
        
        //webInfoData = JSONObject.toJSON((String) appsProxy.proxyCall("/GrapeWebInfo/WebInfo/getWebInfo/s:" + tree, null, null));
        JSONObject newJSON = new JSONObject();
        if (trees != null) {
            JSONObject webInfos = JSONObject.toJSON((String) appsProxy.proxyCall("/GrapeWebInfo/WebInfo/getWebInfo/s:" + tree));
            int l = trees.length;
            for (int i = 0; i < l; i++) {
                // newJSON.put(trees[i], webInfos.getString(trees[i]) );//填充网站姓名
                getAllCount(newJSON, trees[i], webInfos.getString(trees[i]), rootID,startUT,endUT);
            }
        }
        /**/
        JSONArray jsonArray = new JSONArray();
        JSONObject json;
        
        for( Object obj : newJSON.keySet()){
            //修正总量，设置排序
            try{
                json = (JSONObject) newJSON.get(obj) ;
                
                allCnt += json.getLong("count");
                argCnt += json.getLong("checked");
                disArg += json.getLong("uncheck");
                chking += json.getLong("checking");
                click +=json.getLong("clickcount");
                
                jsonArray.add(json);
            }
            catch(Exception e){
                e.getMessage();
            }
        }
        sortJson(jsonArray, 0 , jsonArray.size() - 1, "count");
        nObj.put("count", allCnt);
        nObj.put("checked", argCnt);
        nObj.put("uncheck", disArg);
        nObj.put("checking", chking);
        nObj.put("clickcount", click);
        
        nObj.put("children", jsonArray);
        
        robj.put(rootID, nObj);
        return robj;
    }
    
    @SuppressWarnings("unchecked")
    public static int partition(JSONArray array,int lo,int hi ,String keyName){
        //固定的切分方式
        JSONObject json = (JSONObject)array.get(lo);
        long key= json.getLong(keyName);
        while(lo<hi){
            while(((JSONObject)array.get(hi)).getLong(keyName)<=key&&hi>lo){//从后半部分向前扫描
                hi--;
            }
            //json = ((JSONObject)array.get(lo));
            array.set(lo, array.get(hi));
            //((JSONObject)array.get(lo)).put(keyName, ((JSONObject)array.get(hi)).getLong(keyName));
            while(((JSONObject)array.get(lo)).getLong(keyName)>=key&&hi>lo){//从前半部分向后扫描
                lo++;
            }
            //((JSONObject)array.get(hi)).put(keyName, ((JSONObject)array.get(lo)).getLong(keyName));
            array.set(hi, array.get(lo));
        }
        //((JSONObject)array.get(hi)).put(keyName, key);
        array.set(hi,json);
        return hi;
    }
    
    public static void sortJson(JSONArray array,int lo ,int hi,String keyName){
        if(lo>=hi){
            return ;
        }
        int index=partition(array,lo,hi,keyName);
        sortJson(array,lo,index-1, keyName);
        sortJson(array,index+1,hi, keyName); 
    }
    /*
    @SuppressWarnings("unchecked")
    private void sortJson(JSONArray array,int low, int hight, String key){
        JSONObject json;
        int i, j;
        long index;
        if (low > hight) {
            return;
        }
        i = low;
        j = hight;
        index = ((JSONObject)array.get(i)).getLong(key); // 用子表的第一个记录做基准
        while (i < j) { // 从表的两端交替向中间扫描
            while (i < j && ((JSONObject)array.get(j)).getLong(key) >= index)
                j--;
            
            if (i < j){// 用比基准小的记录替换低位记录
                json = (JSONObject)array.get(i++);
                json.put(key, ((JSONObject)array.get(j)).getLong(key) );
                array.set(i++, json);
            }
            while (i < j && ((JSONObject)array.get(i)).getLong(key) < index)
                i++;
            
            if (i < j){ // 用比基准大的记录替换高位记录
                json = (JSONObject)array.get(j--);
                json.put(key, ((JSONObject)array.get(i)).getLong(key) );
                array.set(j--, json);
            }
        }
        
        json = (JSONObject)array.get(i);
        json.put(key, index );
        array.set(i, json);
        
        sortJson(array, low, i - 1,key); // 对低子表进行递归排序
        sortJson(array, i + 1, hight, key); // 对高子表进行递归排序
    }
    */
    private long getChannelAllCount(String wid,String cid){
        
        return 0;
    }
    private long getChannelAllCount(String wid,String cid,long startUT,long endUT){
        long count = 0;
        if (wid != null) {
            if( startUT > 0 ){
                content.and().gte("time", startUT);
            }
            if( endUT > 0 ){
                content.and().lte("time", endUT);
            }
            count = content.eq("wbid", wid).eq("ogid", cid).count();
        }
        return count;
    }
    private long getChannelAgreeCount(String wid) {
        return 0;
    }
    private long getChannelAgreeCount(String wid,String cid,long startUT,long endUT){
        long count = 0;
        if (wid != null) {
            if( startUT > 0 ){
                content.and().gte("time", startUT);
            }
            if( endUT > 0 ){
                content.and().lte("time", endUT);
            }
            count = content.and().eq("wbid", wid).eq("ogid", cid).eq("state", 2).count();
        }
        return count;
    }
    private long getChannelDisagreeCount(String wid) {
        return 0;
    }
    private long getChannelDisagreeCount(String wid,String cid,long startUT,long endUT){
        long count = 0;
        if (wid != null) {
            if( startUT > 0 ){
                content.and().gte("time", startUT);
            }
            if( endUT > 0 ){
                content.and().lte("time", endUT);
            }
            count = content.and().eq("wbid", wid).eq("ogid", cid).eq("state", 1).count();
        }
        return count;
    }
    
    
    
    private long getCount(String wid) {
        return getCount(wid,0,0);
    }
    
    private long getCount(String wid,long startUT,long endUT) {
        long count = 0;
        if (wid != null) {
            if( startUT > 0 ){
                content.and().gte("time", startUT);
            }
            if( endUT > 0 ){
                content.and().lte("time", endUT);
            }
            count = content.eq("wbid", wid).count();
        }
        return count;
    }

    private long getAgreeCount(String wid) {
        return getAgreeCount(wid,0,0);
    }
    private long getAgreeCount(String wid,long startUT,long endUT) {
        long count = 0;
        if (wid != null) {
            if( startUT > 0 ){
                content.and().gte("time", startUT);
            }
            if( endUT > 0 ){
                content.and().lte("time", endUT);
            }
            count = content.and().eq("wbid", wid).eq("state", 2).count();
        }
        return count;
    }

    private long getDisagreeCount(String wid) {
        return getDisagreeCount(wid,0,0);
    }
    
    private long getDisagreeCount(String wid,long startUT,long endUT) {
        long count = 0;
        if (wid != null) {
            if( startUT > 0 ){
                content.and().gte("time", startUT);
            }
            if( endUT > 0 ){
                content.and().lte("time", endUT);
            }
            count = content.and().eq("wbid", wid).eq("state", 1).count();
        }
        return count;
    }
    /**
     * 获取点击量
     * 
     * @project GrapeContent
     * @package model
     * @file WsCount.java
     * 
     * @param wid
     * @param startUT
     * @param endUT
     * @return
     *
     */
    private long getClick(String wid, long startUT, long endUT) {
        int count = 0, temp = 0;
        String tempString = "0";
        JSONArray array = null;
        if (wid != null) {
            db db = content.bind();
            if (startUT > 0) {
                db.and().gte("time", startUT);
            }
            if (endUT > 0) {
                db.and().lte("time", endUT);
            }
            array = db.and().eq("wbid", wid).field("clickcount,readCount").select();
        }
        if (array != null && array.size() > 0) {
            JSONObject object;
            for (Object obj : array) {
                object = (JSONObject) obj;
                tempString = object.getString("clickcount");
                tempString = (!StringHelper.InvaildString(tempString)) ? "0" : tempString;
                if (tempString.contains("$numberLong")) {
                    tempString = JSONObject.toJSON(tempString).getString("$numberLong");
                }
                temp = Integer.parseInt(tempString);
                count += temp;
            }
        }
        return count;
    }
    // 获取网站id，包含自身网站id
    private String[] getCid(String wid) {
        String[] trees = null;
        if (wid != null && !wid.equals("")) {
            // 获取子站点
            wid = (String) appsProxy.proxyCall("/GrapeWebInfo/WebInfo/getWebTree/s:" + wid);
            if (!wid.equals("")) {
                trees = wid.split(",");
            }
        }
        return trees;
    }
}
