package Model;

import apps.appsProxy;
import cache.CacheHelper;
import check.checkHelper;
import database.dbFilter;
import httpClient.request;
import interfaceApplication.ContentGroup;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import json.JSONHelper;
import nlogger.nlogger;
import org.bson.types.ObjectId;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import security.codec;
import session.session;
import string.StringHelper;

public class CommonModel {
    private String APIHost = "";
    private String APIAppid = "";
    private String appid = appsProxy.appidString();
    private session se;
    private JSONObject userInfo = null;
    private String userID = null;
    private String userName = null;

    private final Pattern ATTR_PATTERN = Pattern.compile("<img[^<>]*?\\ssrc=['\"]?(.*?)['\"]?\\s.*?>", 2);

    public CommonModel() {
        this.se = new session();
        this.userInfo = this.se.getDatas();
        if ((this.userInfo != null) && (this.userInfo.size() > 0)) {
            this.userID = this.userInfo.getString("id");
            this.userName = this.userInfo.getString("name");
        }
    }

    /**
     * 获取查询条件（对于主键_id,需要验证id格式是否正确）
     * 
     * @param pkString
     *            主键字段
     * @param ids
     *            ","分隔的多个id
     * @return [{"field":"_id","logic":"=","value":""},{"field":"_id","logic":
     *         "=","value":""}]
     */
    public JSONArray getOrCond(String pkString, String ids) {
        String[] value = null;
        dbFilter filter = new dbFilter();
        if (StringHelper.InvaildString(ids)) {
            value = ids.split(",");
            for (String id : value) {
                if ((!StringHelper.InvaildString(id)) || ((!ObjectId.isValid(id)) && (!checkHelper.isInt(id))))
                    continue;
                filter.eq(pkString, id);
            }
        }
        return filter.build();
    }

    /**
     * 获取查询条件
     * 
     * @param key
     *            查询字段
     * @param ids
     *            ","分隔的多个值
     * @return [{"field":key1,"logic":"=","value":""},{"field":key2,"logic":"=",
     *         "value":""}]
     */
    public JSONArray getOrCondArray(String key, String ids) {
        String[] value = null;
        dbFilter filter = new dbFilter();
        if (StringHelper.InvaildString(ids)) {
            value = ids.split(",");
            for (String id : value) {
                if (StringHelper.InvaildString(id)) {
                    filter.eq(key, value);
                }
            }
        }
        return filter.build();
    }

    /**
     * 设置模版
     * 
     * @param array
     * @return
     */
    @SuppressWarnings("unchecked")
    public JSONArray setTemplate(JSONArray array) {
        long properties = 0L;
        String list = "", content = "", columnName = "";
        array = ContentDencode(array);
        if (array != null && array.size() > 0) {
            JSONObject tempObj = getTemplate(array);
            if ((tempObj != null) && (tempObj.size() > 0)) {
                for (int i = 0; i < array.size(); i++) {
                    JSONObject object = (JSONObject) array.get(i);
                    String value = object.getString("ogid");
                    if (tempObj != null && tempObj.size() != 0) {
                        String temp = tempObj.getString(value);
                        if (StringHelper.InvaildString(temp)) {
                            String[] values = temp.split(",");
                            content = values[0];
                            list = values[1];
                            properties = Long.parseLong(values[2]);
                            columnName = values[3];
                        }
                    }
                    object.put("TemplateContent", content);
                    object.put("Templatelist", list);
                    object.put("ColumnProperty", Long.valueOf(properties));
                    object.put("ColumnName", columnName);
                    array.set(i, object);
                }
            }
        }
        return array;
    }

    @SuppressWarnings("unchecked")
    private JSONObject getTemplate(JSONArray array) {
        String id = "";
        JSONObject tempobj;
        long properties = 0L;
        String TemplateContent = "";
        String Templatelist = "", columnName = "";
        JSONObject tempObj = new JSONObject();
        if (array != null && array.size() >= 0) {
            for (Object obj : array) {
                tempobj = (JSONObject) obj;
                String temp = tempobj.getString("ogid");
                if (!id.contains(temp)) {
                    id = id + temp + ",";
                }
            }
        }
        if (StringHelper.InvaildString(id)) {
            id = StringHelper.fixString(id, ',');
            String column = new ContentGroup().getGroupById(id);
            array = JSONArray.toJSONArray(column);
        }
        if ((array != null) && (array.size() != 0)) {
            int l = array.size();
            for (int i = 0; i < l; i++) {
                JSONObject object = (JSONObject) array.get(i);
                if ((object != null) && (object.size() != 0)) {
                    if (object.containsKey("TemplateContent")) {
                        TemplateContent = object.getString("TemplateContent");
                    }
                    if (object.containsKey("TemplateList")) {
                        Templatelist = object.getString("TemplateList");
                    }
                    if (object.containsKey("ColumnProperty")) {
                        properties = object.getLong("ColumnProperty");
                    }
                    if (object.containsKey("name")) {
                        columnName = object.getString("name");
                    }
                    String tid = object.getString("_id");
                    tempObj.put(tid, TemplateContent + "," + Templatelist + "," + properties + "," + columnName);
                }
            }
        }
        return tempObj;
    }

    public void setKafka(String id, int mode, int newstate) {
        this.APIHost = getconfig("APIHost");
        if ((!this.APIHost.equals("")) && (!this.APIAppid.equals("")))
            request.Get(this.APIHost + "/sendServer/ShowInfo/getKafkaData/" + id + "/" + this.appid + "/int:1/int:" + mode + "/int:" + newstate);
    }

    public void AddLog(int type, String obj, String func, String condString) {
        String action = "";
        String columnName = getColumnName(obj);
        switch (type) {
        case 0:
            action = "新增[" + columnName + "]栏目";
            break;
        case 1:
            action = "删除[" + columnName + "]栏目," + condString;
            break;
        case 2:
            action = "更新[" + columnName + "]栏目," + condString;
            break;
        case 3:
            break;
        case 4:
            break;
        case 5:
            break;
        case 6:
            break;
        }

        appsProxy.proxyCall("/GrapeLog/Logs/AddLogs/" + this.userID + "/" + this.userName + "/" + action + "/" + func, appsProxy.getCurrentAppInfo());
    }

    private String getColumnName(String ogid) {
        String columnName = ogid;
        if ((StringHelper.InvaildString(ogid)) && (!ogid.equals("0")) && ((ObjectId.isValid(ogid)) || (checkHelper.isInt(ogid)))) {
            JSONObject temp = JSONObject.toJSON(new ContentGroup().getColumnName(ogid));
            if ((temp != null) && (temp.size() > 0))
                columnName = temp.getString(ogid);
            else {
                columnName = "";
            }
        }

        return columnName;
    }

    /**
     * 获取services表中configName配置信息
     * 
     * @param key
     * @return
     */
    public String getconfig(String key) {
        String value = "";
        try {
            JSONObject object = JSONObject.toJSON(appsProxy.configValue().getString("other"));
            if ((object != null) && (object.size() > 0))
                value = object.getString(key);
        } catch (Exception e) {
            nlogger.logout(e);
            value = "";
        }
        return value;
    }

    /**
     * 获取图片相对路径
     * 
     * @param imageURL
     * @return
     */
    public String getImageUri(String imageURL) {
        int i = 0;
        if (imageURL.contains("File//upload")) {
            i = imageURL.toLowerCase().indexOf("file//upload");
            imageURL = "\\" + imageURL.substring(i);
        }
        if (imageURL.contains("File\\upload")) {
            i = imageURL.toLowerCase().indexOf("file\\upload");
            imageURL = "\\" + imageURL.substring(i);
        }
        if (imageURL.contains("File/upload")) {
            i = imageURL.toLowerCase().indexOf("file/upload");
            imageURL = "\\" + imageURL.substring(i);
        }
        return imageURL;
    }

    /**
     * 文章内容解码
     * 
     * @param param
     * @return
     */
    public String dencode(String param) {
        if (StringHelper.InvaildString(param)) {
            param = codec.DecodeHtmlTag(param);
            param = codec.decodebase64(param);
        }
        return param;
    }

    /**
     * 生成查询条件
     * 
     * @param info
     *            {key1:value1,key2:value2}或者[{"field":key1,"logic":"=","value":
     *            value1}]
     * @return {"ogid":[{"field":"ogid","logic":"=",
     *         "value=""}],"cond":[{"field":key1,"logic":"=","value=""}]}
     */
    public JSONObject buildCondOgid(String info) {
        JSONObject obj = new JSONObject();

        JSONObject tempObj = JSONObject.toJSON(info);
        if (tempObj != null && tempObj.size() > 0)
            obj = buildObj(tempObj);
        else {
            obj = buildArray(JSONArray.toJSONArray(info));
        }
        return obj;
    }

    /**
     * 封装查询条件
     * 
     * @param object
     *            {key1:value1,key2:value2}
     * @return {"ogid":[{"field":"ogid","logic":"=",
     *         "value=""}],"cond":[{"field":key1,"logic":"=","value=""}]}
     */
    @SuppressWarnings("unchecked")
    private JSONObject buildObj(JSONObject object) {
        JSONArray condArray = new JSONArray();
        JSONArray CondOgid = new JSONArray();
        JSONObject obj = new JSONObject();

        String key;
        dbFilter filter = new dbFilter();
        if (object != null && object.size() > 0) {
            for (Object str : object.keySet()) {
                key = str.toString();
                if (key.equals("ogid")) { // 查询条件包含ogid，则获取链接栏目id
                    String value = object.getString(key);
                    CondOgid = getOgidCond(value);
                } else { // 其他查询条件，不含有ogid字段
                    filter.eq(key, object.get(key));
                }
            }
            condArray = filter.build();
        }
        obj.put("ogid", CondOgid);
        obj.put("cond", condArray);
        return obj;
    }

    /**
     * 封装查询条件
     * 
     * @param array
     *            [{"field":key1,"logic":"=","value":value1}]
     * @return {"ogid":[{"field":"ogid","logic":"=",
     *         "value=""}],"cond":[{"field":key1,"logic":"=","value=""}]}
     */
    @SuppressWarnings("unchecked")
    private JSONObject buildArray(JSONArray array) {
        String key, value;
        JSONArray condArray = new JSONArray();
        JSONArray CondOgid = new JSONArray();
        JSONObject obj = new JSONObject(), temp;

        if (array != null && array.size() > 0) {
            for (Object object : array) {
                temp = (JSONObject) object;
                key = temp.getString("field");
                if (key.equals("ogid")) { // 查询条件包含ogid，则获取链接栏目id
                    value = temp.getString("value");
                    CondOgid = getOgidCond(value);
                } else { // 其他查询条件，不含有ogid字段
                    condArray.add(temp);
                }
            }
        }
        obj.put("ogid", CondOgid);
        obj.put("cond", condArray);
        return obj;
    }

    /**
     * 封装与ogid相关的查询条件,存在链接栏目，则获取链接栏目id
     * 
     * @param value
     *            当前栏目id
     * @return
     */
    private JSONArray getOgidCond(String value) {
        String ogid = "";
        String[] values = null;
        dbFilter filter = new dbFilter();
        ogid = new ContentGroup().getLinkOgid(value);
        if (StringHelper.InvaildString(ogid)) {
            values = ogid.split(",");
            for (String string : values) {
                filter.eq("ogid", string);
            }
        }
        return filter.build();
    }

    public JSONArray buildCond(String Info) {
        JSONArray condArray = null;
        JSONObject object = JSONObject.toJSON(Info);
        dbFilter filter = new dbFilter();
        if (object != null && object.size() > 0) {
            // for (Iterator localIterator = object.keySet().iterator();
            // localIterator.hasNext();) {
            for (Object object2 : object.keySet()) {
                String key = object2.toString();
                Object value = object.get(key);
                filter.eq(key, value);
            }
            condArray = filter.build();
        } else {
            condArray = JSONArray.toJSONArray(Info);
        }
        return condArray;
    }

    /**
     * 获取所有下级站点id，包含当前站点
     * 
     * @param wbid
     * @return
     */
    public String[] getWeb(String wbid) {
        String value = "";
        CacheHelper ch = new CacheHelper();
        String key = "ChildWebId_" + wbid;
        value = ch.get(key);
        if (value != null) {
            return value.split(",");
        }
        wbid = getRWbid(wbid);
        String temp = (String) appsProxy.proxyCall("/GrapeWebInfo/WebInfo/getWebTree/" + wbid);
        ch.setget(key, temp, 86400L);
        return temp.split(",");
    }

    /**
     * 获取实站id
     * 
     * @param wbid
     * @return
     */
    public String getRWbid(String wbid) {
        // String value = wbid;
        CacheHelper cacheHelper = new CacheHelper();
        String key = "vID2rID_" + wbid;
        String value = cacheHelper.get(key);
        if (value != null) {
            return value;
        }
        String temp = (String) appsProxy.proxyCall("/GrapeWebInfo/WebInfo/VID2RID/" + wbid);
        cacheHelper.setget(key, temp, 86400L);
        return temp;
    }

    @SuppressWarnings("unchecked")
    public JSONObject ContentEncode(JSONObject object) {
        String temp = "";
        if ((object != null) && (object.size() > 0)) {
            if (object.containsKey("content")) {
                object.escapeHtmlPut("content", object.getString("content"));
            }
            if (object.containsKey("image")) {
                temp = object.getString("image");
                if ((temp != null) && (!temp.equals("")) && (!temp.equals("null"))) {
                    temp = codec.DecodeHtmlTag(temp);
                    object.put("image", RemoveUrlPrefix(temp));
                }
            }
        }
        return object;
    }

    @SuppressWarnings("unchecked")
    public JSONArray ContentDencode(JSONArray array) {
        if ((array != null) && (array.size() > 0)) {
            int l = array.size();
            for (int i = 0; i < l; i++) {
                JSONObject object = (JSONObject) array.get(i);
                object = ContentDencode(object);
                array.set(i, object);
            }
        }
        return array;
    }

    @SuppressWarnings("unchecked")
    public JSONObject ContentDencode(JSONObject object) {
        if ((object != null) && (object.size() > 0) && (object.containsKey("content"))) {
            object.put("content", object.escapeHtmlGet("content"));
        }
        return object;
    }

    public String RemoveUrlPrefix(String imageUrl) {
        String image = "";
        if ((imageUrl.equals("")) || (imageUrl == null)) {
            return imageUrl;
        }
        String[] imgUrl = imageUrl.split(",");
        for (String string : imgUrl) {
            if (string.contains("http://")) {
                string = getImageUri(string);
            }
            image = image + string + ",";
        }
        return StringHelper.fixString(image, ',');
    }

    @SuppressWarnings("unchecked")
    public JSONArray getImgs(JSONArray array) {
        if ((array == null) || (array.size() <= 0)) {
            return new JSONArray();
        }
        for (int i = 0; i < array.size(); i++) {
            JSONObject object = (JSONObject) array.get(i);
            object = getImgs(object);
            array.set(i, object);
        }

        return array;
    }

    @SuppressWarnings("unchecked")
    public JSONObject getImgs(JSONObject object) {
        JSONObject imgobj = new JSONObject();
        JSONObject conobj = new JSONObject();
        if ((object == null) || (object.size() == 0)) {
            return new JSONObject();
        }
        String id = object.getMongoID("_id");
        imgobj.put(id, object.get("image"));
        conobj.put(id, object.get("content"));
        imgobj = getImage(imgobj);
        conobj = getContentImgs(conobj);
        object.put("content", conobj.get(id));
        object.put("image", imgobj.get(id));
        return object;
    }

    @SuppressWarnings("unchecked")
    public JSONArray getDefaultImage(String wbid, JSONArray array) {
        CacheHelper ch = new CacheHelper();
        String thumbnail = "";
        String suffix = "";
        String tempString = "0";
        int type = 0;
        if ((!wbid.equals("")) && (array != null) && (array.size() != 0)) {
            int l = array.size();

            String temp = ch.get("DefaultImage_" + wbid);
            if (temp == null) {
                temp = (String) appsProxy.proxyCall("/GrapeWebInfo/WebInfo/getImage/" + wbid);
                ch.setget("DefaultImage_" + wbid, temp, 86400L);
            }
            JSONObject Obj = JSONObject.toJSON(temp);
            if ((Obj != null) && (Obj.size() != 0)) {
                if (Obj.containsKey("thumbnail")) {
                    thumbnail = Obj.getString("thumbnail");
                }
                if (Obj.containsKey("suffix")) {
                    suffix = Obj.getString("suffix");
                }
            }
            for (int i = 0; i < l; i++) {
                Obj = (JSONObject) array.get(i);
                if ((Obj != null) && (Obj.size() > 0)) {
                    if (Obj.containsKey("isSuffix")) {
                        tempString = Obj.getString("isSuffix");
                        if (tempString.contains("$numberLong")) {
                            tempString = JSONObject.toJSON(tempString).getString("$numberLong");
                        }
                        tempString = (tempString == null) || (tempString.equals("")) || (tempString.equals("null")) ? "0" : tempString;
                        type = Integer.parseInt(tempString);
                    }
                    if (type == 0) {
                        suffix = "";
                    }
                    Obj.put("thumbnail", thumbnail);
                    Obj.put("suffix", suffix);
                }
                array.set(i, Obj);
            }
        }
        return array;
    }

    @SuppressWarnings("unchecked")
    private JSONObject getImage(JSONObject objimg) {
        for (Object obj : objimg.keySet()) {
            String key = obj.toString();
            String value = objimg.getString(key);
            if (!value.contains("http://")) {
                objimg.put(key, AddUrlPrefix(value));
            }
        }
        return objimg;
    }

    private String AddUrlPrefix(String imageUrl) {
        if ((imageUrl.equals("")) || (imageUrl == null)) {
            return imageUrl;
        }
        String[] imgUrl = imageUrl.split(",");
        List list = new ArrayList();
        for (String string : imgUrl) {
            if (!string.contains("http://")) {
                string = getconfig("fileHost") + string;
            }
            list.add(string);
        }
        return StringHelper.join(list);
    }

    @SuppressWarnings("unchecked")
    private JSONObject getContentImgs(JSONObject objcontent) {
        int code = 2;
        for (Iterator localIterator = objcontent.keySet().iterator(); localIterator.hasNext();) {
            Object obj = localIterator.next();
            String key = obj.toString();
            String value = objcontent.getString(key);
            if (!value.equals("")) {
                Matcher matcher = this.ATTR_PATTERN.matcher(value.toLowerCase());
                if (value.contains("/File/upload")) {
                    code = matcher.find() ? 0 : (value.contains("/File/upload") ? 1 : 2);
                } else if (value.contains("/") || value.contains("\\")) {
                    code = matcher.find() ? 0 : ((value.startsWith("/") || value.startsWith("\\")) ? 3 : 2);
                }

                switch (code) {
                case 0:
                    value = AddHtmlPrefix(value);
                    break;
                case 1:
                    value = AddUrlPrefix(value);
                    break;
                case 2:
                    break;
                case 3:
                    value = getconfig("fileHost") + value;
                    break;
                }

                objcontent.put(key, value);
            }
        }
        return objcontent;
    }

    private String AddHtmlPrefix(String Contents) {
        String imgurl = getconfig("fileHost");
        String temp = "";
        String newurl = "";
        if ((Contents != null) && (!Contents.equals("")) && (!Contents.equals("null"))) {
            List list = getCommonAddr(Contents);
            if ((list != null) && (list.size() > 0)) {
                int l = list.size();
                for (int i = 0; i < l; i++) {
                    temp = (String) list.get(i);
                    if (!temp.contains("http://")) {
                        if ((!temp.startsWith("/")) || (!temp.startsWith("//")) || (!temp.startsWith("\\"))) {
                            newurl = "\\" + temp;
                        }
                        newurl = imgurl + newurl;
                        Contents = Contents.replace(temp, newurl);
                    }
                }
            }
        }
        return Contents;
    }

    private List<String> getCommonAddr(String contents) {
        Matcher matcher = this.ATTR_PATTERN.matcher(contents);
        List list = new ArrayList();
        while (matcher.find()) {
            list.add(matcher.group(1));
        }
        return list;
    }

    public JSONArray join(JSONArray array) {
        array = getImgs(array);

        if ((array == null) || (array.size() <= 0))
            return null;
        try {
            int len = array.size();
            for (int i = 0; i < len; i++) {
                JSONObject object = (JSONObject) array.get(i);
                object = join(object);
                array.set(i, object);
            }
        } catch (Exception e) {
            System.out.println("content.join:" + e.getMessage());
            array = null;
        }
        return array;
    }

    public JSONObject join(JSONObject object) {
        JSONObject tmpJSON = object;
        if ((tmpJSON != null) && (tmpJSON.containsKey("tempid"))) {
            tmpJSON.put("tempContent", getTemplate(tmpJSON.get("tempid").toString()));
        }

        return tmpJSON;
    }

    private String getTemplate(String tid) {
        String temp = "";
        CacheHelper cache = new CacheHelper();
        try {
            if (tid.contains("$numberLong")) {
                tid = JSONHelper.string2json(tid).getString("$numberLong");
            }
            if (!"0".equals(tid))
                if (cache.get(tid) != null) {
                    temp = cache.get(tid).toString();
                } else {
                    temp = appsProxy.proxyCall("/GrapeTemplate/TemplateContext/TempFindByTid/s:" + tid).toString();
                    cache.setget(tid, temp, 36000L);
                }
        } catch (Exception e) {
            nlogger.logout(e);
            temp = "";
        }
        return temp;
    }

    @SuppressWarnings("unchecked")
    public JSONArray getDefault(String wbid, JSONArray array) {
        JSONObject tempObj;
        String ogids = "", temp;
        JSONObject WebImage = getWebThumbnail(wbid); // 获取网站缩略图
        if (array != null && array.size() > 0) {
            for (Object object : array) {
                tempObj = (JSONObject) object;
                temp = tempObj.getString("ogid");
                if (!ogids.contains(temp)) {
                    ogids += temp + ",";
                }
            }
        }
        JSONObject ColumnImage = new ContentGroup().getDefaultById(StringHelper.fixString(ogids, ',')); // 获取栏目缩略图信息
        if (array != null && array.size() > 0) {
            int l = array.size();
            for (int i = 0; i < l; i++) {
                tempObj = (JSONObject) array.get(i);
                tempObj = FillDefaultImage(tempObj, ColumnImage, WebImage);
                array.set(i, tempObj);
            }
        }
        return array;
    }

    /**
     * 填充缩略图及文章小尾巴标识
     * 
     * @param object
     * @return
     */
    public JSONObject getDefault(JSONObject object) {
        String ogids = "", wbid = "";
        JSONObject WebImage = null, ColumnImage = null;
        if (object != null && object.size() > 0) {
            ogids = object.getString("ogid");
            wbid = object.getString("wbid");
        }
        if (StringHelper.InvaildString(ogids) && !ogids.equals("0")) {
            ColumnImage = new ContentGroup().getDefaultById(ogids); // 获取栏目缩略图信息
        }
        if (StringHelper.InvaildString(wbid) && !wbid.equals("0")) {
            WebImage = getWebThumbnail(wbid); // 获取网站缩略图
        }
        return FillDefaultImage(object, ColumnImage, WebImage);
    }

    /**
     * 填充缩略图，通过栏目填充缩略图，若栏目未设置缩略图，则使用网站缩略图
     * 
     * @param contentInfo
     *            文章信息
     * @param ColumnImage
     *            栏目缩略图
     * @param WebThumbnail
     *            网站缩略图
     * @return
     */
    public JSONObject FillDefaultImage(JSONObject contentInfo, JSONObject ColumnImage, JSONObject WebThumbnail) {
        String thumbnail = "", suffix = "";
        // 通过栏目获取缩略图及文章小尾巴标识
        contentInfo = FillColumnImage(contentInfo, ColumnImage);
        if (contentInfo != null && contentInfo.size() > 0) {
            thumbnail = contentInfo.getString("thumbnail");
            suffix = contentInfo.getString("suffix");
        }
        if (!StringHelper.InvaildString(thumbnail) || !StringHelper.InvaildString(suffix)) {
            contentInfo = FillWebImage(contentInfo, WebThumbnail);
        }
        return contentInfo;
    }

    /**
     * 将栏目的默认缩略图添加至文章中
     * 
     * @param ContentInfo
     * @param ColumnImage
     * @return
     */
    @SuppressWarnings("unchecked")
    private JSONObject FillColumnImage(JSONObject ContentInfo, JSONObject ColumnImage) {
        JSONObject temp;
        String ogid = "", thumbnail = "", suffix = "";
        if (ContentInfo != null && ContentInfo.size() > 0) {
            ogid = ContentInfo.getString("ogid");
            if (StringHelper.InvaildString(ogid)) {
                if (ColumnImage != null && ColumnImage.size() > 0) {
                    temp = ColumnImage.getJson(ogid); // {"thumbnail":"","suffix":""}
                    if (temp != null && temp.size() > 0) {
                        if (temp.containsKey("thumbnail")) {
                            thumbnail = temp.getString("thumbnail");
                        }
                        if (temp.containsKey("suffix")) {
                            suffix = temp.getString("suffix");
                        }
                    }
                }
            }
        }
        ContentInfo.put("thumbnail", getRandomImage(thumbnail));
        ContentInfo.put("suffix", suffix);
        return ContentInfo;
    }

    /**
     * 若栏目设置的缩略图为多个，则随机选择一个
     * 
     * @param thumbnails
     * @return
     */
    private String getRandomImage(String thumbnails) {
        String[] value = null;
        String thumbnail = thumbnails;
        if (StringHelper.InvaildString(thumbnails)) {
            value = thumbnails.split(",");
            if (value != null) {
                int l = value.length;
                if (l > 1) {
                    thumbnail = value[new Random().nextInt(l)];
                }
            }
        }
        if (StringHelper.InvaildString(thumbnail)) {
            if (!thumbnail.contains("http://")) {
                thumbnail = getconfig("fileHost") + thumbnail;
            }
        }
        return thumbnail;
    }

    /**
     * 将网站的默认缩略图添加至文章中
     * 
     * @param ContentInfo
     * @param ColumnImage
     * @return
     */
    @SuppressWarnings("unchecked")
    private JSONObject FillWebImage(JSONObject ContentInfo, JSONObject WebImage) {
        JSONObject temp;
        String wbid = "", thumbnail = "", suffix = "", contents = "", tempSuffix = "";
        String webThumbnail = "", webSuffix = "";
        int isSuffix = 0;
        if (ContentInfo != null && ContentInfo.size() > 0) {
            wbid = ContentInfo.getString("wbid"); // 文章所属网站id
            contents = ContentInfo.getString("content"); // 获取文章内容
            if (ContentInfo.containsKey("isSuffix")) {
                tempSuffix = ContentInfo.getString("isSuffix");
            }
            isSuffix = (StringHelper.InvaildString(tempSuffix)) ? Integer.parseInt(tempSuffix) : 0; // 是否添加小尾巴表示
            thumbnail = ContentInfo.getString("thumbnail"); // 缩略图
            suffix = ContentInfo.getString("suffix"); // 小尾巴表示信息
            if (StringHelper.InvaildString(wbid)) {
                if (WebImage != null && WebImage.size() > 0) {
                    temp = WebImage.getJson(wbid); // {"thumbnail":"","suffix":""}
                    if (temp != null && temp.size() > 0) {
                        if (temp.containsKey("thumbnail")) {
                            webThumbnail = temp.getString("thumbnail");
                        }
                        if (temp.containsKey("suffix")) {
                            suffix = temp.getString("suffix");
                        }
                    }
                }
            }
        }
        if (isSuffix != 0) {
            suffix = (StringHelper.InvaildString(suffix)) ? suffix : webSuffix;
            ContentInfo.put("content", contents + suffix);
        }
        if (!StringHelper.InvaildString(thumbnail)) {
            ContentInfo.put("thumbnail", webThumbnail);
        }
        ContentInfo.remove("suffix");
        return ContentInfo;
    }

    /**
     * 获取网站缩略图，支持查询多个网站【远程调用，调用网站服务接口】
     * 
     * @param wbid
     * @return {wbid:{"thumbnail":"","suffix":""},wbid:{"thumbnail":"","suffix":
     *         ""}}
     */
    private JSONObject getWebThumbnail(String wbid) {
        JSONObject Obj = null;
        if (StringHelper.InvaildString(wbid) && !wbid.equals("0")) {
            String temp = (String) appsProxy.proxyCall("/GrapeWebInfo/WebInfo/getImage/" + wbid);
            if (StringHelper.InvaildString(temp)) {
                Obj = JSONObject.toJSON(temp);
            }
        }
        return Obj;
    }
}