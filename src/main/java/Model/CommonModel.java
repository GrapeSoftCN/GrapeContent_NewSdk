package Model;

import apps.appsProxy;
import cache.CacheHelper;
import check.checkHelper;
import database.dbFilter;
import httpClient.request;
import interfaceApplication.ContentGroup;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
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

public class CommonModel
{
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

  public JSONArray getOrCond(String pkString, String ids)
  {
    String[] value = null;
    dbFilter filter = new dbFilter();
    if (StringHelper.InvaildString(ids)) {
      value = ids.split(",");
      for (String id : value) {
        if ((!StringHelper.InvaildString(id)) || (
          (!ObjectId.isValid(id)) && (!checkHelper.isInt(id)))) continue;
        filter.eq(pkString, id);
      }

    }

    return filter.build();
  }

  public JSONArray getOrCondArray(String key, String ids)
  {
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

  public JSONArray setTemplate(JSONArray array)
  {
    long properties = 0L;
    String list = ""; String content = "";
    array = ContentDencode(array);
    if ((array == null) || (array.size() <= 0)) {
      return array;
    }
    JSONObject tempObj = getTemplate(array);
    if ((tempObj != null) && (tempObj.size() > 0)) {
      for (int i = 0; i < array.size(); i++) {
        JSONObject object = (JSONObject)array.get(i);
        String value = object.getString("ogid");
        if ((tempObj != null) && (tempObj.size() != 0)) {
          String temp = tempObj.getString(value);
          if (StringHelper.InvaildString(temp)) {
            String[] values = temp.split(",");
            content = values[0];
            list = values[1];
            properties = Long.parseLong(values[2]);
          }
        }
        object.put("TemplateContent", content);
        object.put("Templatelist", list);
        object.put("ColumnProperty", Long.valueOf(properties));
        array.set(i, object);
      }
    }
    return array;
  }

  private JSONObject getTemplate(JSONArray array)
  {
    String id = "";
    long properties = 0L;
    String TemplateContent = ""; String Templatelist = "";
    JSONObject tempObj = new JSONObject();
    if ((array != null) && (array.size() >= 0)) {
      for (Iterator localIterator = array.iterator(); localIterator.hasNext(); ) { Object obj = localIterator.next();
        JSONObject object = (JSONObject)obj;
        String temp = object.getString("ogid");
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
        JSONObject object = (JSONObject)array.get(i);
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
          String tid = object.getString("_id");
          tempObj.put(tid, TemplateContent + "," + Templatelist + "," + properties);
        }
      }
    }
    return tempObj;
  }

  public void setKafka(String id, int mode, int newstate)
  {
    this.APIHost = getconfig("APIHost");
    if ((!this.APIHost.equals("")) && (!this.APIAppid.equals("")))
      request.Get(this.APIHost + "/sendServer/ShowInfo/getKafkaData/" + id + "/" + this.appid + "/int:1/int:" + mode + "/int:" + newstate);
  }

  public void AddLog(int type, String obj, String func, String condString)
  {
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

  private String getColumnName(String ogid)
  {
    String columnName = ogid;
    if ((StringHelper.InvaildString(ogid)) && (!ogid.equals("0")) && (
      (ObjectId.isValid(ogid)) || (checkHelper.isInt(ogid)))) {
      JSONObject temp = JSONObject.toJSON(new ContentGroup().getColumnName(ogid));
      if ((temp != null) && (temp.size() > 0))
        columnName = temp.getString(ogid);
      else {
        columnName = "";
      }
    }

    return columnName;
  }

  private String getconfig(String key)
  {
    String value = "";
    try {
      JSONObject object = JSONObject.toJSON(appsProxy.configValue().getString("other"));
      if ((object != null) && (object.size() > 0))
        value = object.getString(key);
    }
    catch (Exception e) {
      nlogger.logout(e);
      value = "";
    }
    return value;
  }

  public String getImageUri(String imageURL)
  {
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

  public String dencode(String param)
  {
    if ((param != null) && (!param.equals("")) && (!param.equals("null"))) {
      param = codec.DecodeHtmlTag(param);
      param = codec.decodebase64(param);
    }
    return param;
  }

  public JSONObject buildCondOgid(String info)
  {
    JSONObject obj = new JSONObject();
    JSONArray CondArray = new JSONArray();
    JSONArray CondOgid = new JSONArray();

    JSONObject tempObj = JSONObject.toJSON(info);
    if ((tempObj != null) && (tempObj.size() > 0))
      obj = buildObj(tempObj, CondOgid, CondArray);
    else {
      obj = buildArray(JSONArray.toJSONArray(info), CondOgid, CondArray);
    }
    return obj;
  }

  private JSONObject buildObj(JSONObject object, JSONArray CondOgid, JSONArray condArray)
  {
    JSONObject obj = new JSONObject();

    String[] values = null;
    dbFilter filter = new dbFilter();
    dbFilter filter1 = new dbFilter();
    if ((object != null) && (object.size() > 0)) {
      for (Iterator localIterator = object.keySet().iterator(); localIterator.hasNext(); ) { Object object2 = localIterator.next();
        String key = object2.toString();
        if (key.equals("ogid")) {
          Object value = getROgid(object.getString(key));
          String temp = (String)value;
          if (temp.contains("errorcode")) {
            return null;
          }
          if (StringHelper.InvaildString(temp)) {
            values = temp.split(",");
            for (String id : values) {
              if ((!StringHelper.InvaildString(id)) || (
                (!ObjectId.isValid(id)) && (!checkHelper.isInt(id)))) continue;
              filter1.eq(key, id);
            }
          }
        }
        else
        {
          Object value = object.get(key);
          filter.eq(key, value);
        }
      }
      CondOgid = filter1.build();
      condArray = filter.build();
      obj.put("ogid", CondOgid);
      obj.put("cond", condArray);
    }
    return obj;
  }

  private JSONObject buildArray(JSONArray array, JSONArray CondOgid, JSONArray condArray)
  {
    JSONObject obj = new JSONObject();

    String[] values = null;
    dbFilter filter = new dbFilter();
    if ((array != null) && (array.size() > 0)) {
      for (Iterator localIterator = array.iterator(); localIterator.hasNext(); ) { Object object = localIterator.next();
        JSONObject temp = (JSONObject)object;
        String key = temp.getString("field");
        if (key.equals("ogid")) {
          String value = temp.getString("value");
          value = getROgid(value);
          if (value.contains("errorcode")) {
            return null;
          }
          if (StringHelper.InvaildString(value)) {
            values = value.split(",");
            for (String id : values) {
              if ((!StringHelper.InvaildString(id)) || (
                (!ObjectId.isValid(id)) && (!checkHelper.isInt(id)))) continue;
              filter.eq(key, id);
            }
          }
        }
        else
        {
          condArray.add(temp);
        }
      }
      CondOgid = filter.build();
      obj.put("ogid", CondOgid);
      obj.put("cond", condArray);
    }
    return obj;
  }

  public JSONArray buildCond(String Info)
  {
    JSONArray condArray = null;
    JSONObject object = JSONObject.toJSON(Info);
    dbFilter filter = new dbFilter();
    if ((object != null) && (object.size() > 0)) {
      for (Iterator localIterator = object.keySet().iterator(); localIterator.hasNext(); ) { Object object2 = localIterator.next();
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

  public String getROgid(String ogid)
  {
    if (StringHelper.InvaildString(ogid)) {
      ogid = new ContentGroup().getLinkOgid(ogid);
    }
    if (ogid.contains("errorcode")) {
      return ogid;
    }
    return ogid;
  }

  public String[] getWeb(String wbid)
  {
    String[] value = null;
    wbid = getRWbid(wbid);
    String temp = (String)appsProxy.proxyCall("/GrapeWebInfo/WebInfo/getWebTree/" + wbid);
    if ((temp != null) && (!temp.equals(""))) {
      value = temp.split(",");
    }
    return value;
  }

  public String getRWbid(String wbid)
  {
    String value = wbid;
    CacheHelper cacheHelper = new CacheHelper();
    String key = "vID2rID_" + wbid;
    Object obj = (String)appsProxy.proxyCall("/GrapeWebInfo/WebInfo/VID2RID/" + wbid);
    if (obj != null) {
      value = obj.toString();
      if (StringHelper.InvaildString(value)) {
        cacheHelper.setget(key, value, 86400L);
      }
    }
    return value;
  }

  public JSONObject ContentEncode(JSONObject object)
  {
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

  public JSONArray ContentDencode(JSONArray array)
  {
    if ((array != null) && (array.size() > 0)) {
      int l = array.size();
      for (int i = 0; i < l; i++) {
        JSONObject object = (JSONObject)array.get(i);
        object = ContentDencode(object);
        array.set(i, object);
      }
    }
    return array;
  }

  public JSONObject ContentDencode(JSONObject object)
  {
    if ((object != null) && (object.size() > 0) && 
      (object.containsKey("content"))) {
      object.put("content", object.escapeHtmlGet("content"));
    }

    return object;
  }

  public String RemoveUrlPrefix(String imageUrl)
  {
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

  public JSONArray getImgs(JSONArray array)
  {
    if ((array == null) || (array.size() <= 0)) {
      return new JSONArray();
    }
    for (int i = 0; i < array.size(); i++) {
      JSONObject object = (JSONObject)array.get(i);
      object = getImgs(object);
      array.set(i, object);
    }

    return array;
  }

  public JSONObject getImgs(JSONObject object)
  {
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

  public JSONArray getDefaultImage(String wbid, JSONArray array)
  {
    CacheHelper ch = new CacheHelper();
    String thumbnail = ""; String suffix = ""; String tempString = "0";
    int type = 0;
    if ((!wbid.equals("")) && (array != null) && (array.size() != 0)) {
      int l = array.size();

      String temp = ch.get("DefaultImage_" + wbid);
      if (temp == null) {
        temp = (String)appsProxy.proxyCall("/GrapeWebInfo/WebInfo/getImage/" + wbid);
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
        Obj = (JSONObject)array.get(i);
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

  private JSONObject getImage(JSONObject objimg)
  {
    for (Iterator localIterator = objimg.keySet().iterator(); localIterator.hasNext(); ) { Object obj = localIterator.next();
      String key = obj.toString();
      String value = objimg.getString(key);
      if (!value.contains("http://")) {
        objimg.put(key, AddUrlPrefix(value));
      }
    }
    return objimg;
  }

  private String AddUrlPrefix(String imageUrl)
  {
    if ((imageUrl.equals("")) || (imageUrl == null)) {
      return imageUrl;
    }
    String[] imgUrl = imageUrl.split(",");
    List list = new ArrayList();
    for (String string : imgUrl) {
      if (string.contains("http:")) {
        string = getImageUri(string);
      }
      string = getconfig("fileHost") + string;
      list.add(string);
    }
    return StringHelper.join(list);
  }

  private JSONObject getContentImgs(JSONObject objcontent)
  {
    for (Iterator localIterator = objcontent.keySet().iterator(); localIterator.hasNext(); ) { Object obj = localIterator.next();
      String key = obj.toString();
      String value = objcontent.getString(key);
      if (!value.equals("")) {
        Matcher matcher = this.ATTR_PATTERN.matcher(value.toLowerCase());
        int code = value.contains("/File/upload") ? 1 : matcher.find() ? 0 : 2;
        switch (code) {
        case 0:
          value = AddHtmlPrefix(value);
          break;
        case 1:
          value = AddUrlPrefix(value);
          break;
        case 2:
          break;
        }

        objcontent.put(key, value);
      }
    }
    return objcontent;
  }

  private String AddHtmlPrefix(String Contents)
  {
    String imgurl = "host";
    String temp = ""; String newurl = "";
    if ((Contents != null) && (!Contents.equals("")) && (!Contents.equals("null"))) {
      List list = getCommonAddr(Contents);
      if ((list != null) && (list.size() > 0)) {
        int l = list.size();
        for (int i = 0; i < l; i++) {
          temp = (String)list.get(i);
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

  private List<String> getCommonAddr(String contents)
  {
    Matcher matcher = this.ATTR_PATTERN.matcher(contents);
    List list = new ArrayList();
    while (matcher.find()) {
      list.add(matcher.group(1));
    }
    return list;
  }

  public JSONArray join(JSONArray array)
  {
    array = getImgs(array);

    if ((array == null) || (array.size() <= 0))
      return null;
    try
    {
      int len = array.size();
      for (int i = 0; i < len; i++) {
        JSONObject object = (JSONObject)array.get(i);
        object = join(object);
        array.set(i, object);
      }
    } catch (Exception e) {
      System.out.println("content.join:" + e.getMessage());
      array = null;
    }
    return array;
  }

  public JSONObject join(JSONObject object)
  {
    JSONObject tmpJSON = object;
    if ((tmpJSON != null) && 
      (tmpJSON.containsKey("tempid")))
    {
      tmpJSON.put("tempContent", getTemplate(tmpJSON.get("tempid").toString()));
    }

    return tmpJSON;
  }

  private String getTemplate(String tid)
  {
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
    }
    catch (Exception e) {
      nlogger.logout(e);
      temp = "";
    }
    return temp;
  }

  public JSONObject getDefaultImage(JSONObject object)
  {
    CacheHelper ch = new CacheHelper();
    String thumbnail = "";
    if ((object != null) && (object.size() != 0)) {
      String wbid = object.getString("wbid");

      String temp = ch.get("DefaultImage_" + wbid);
      if (temp == null) {
        temp = (String)appsProxy.proxyCall("/GrapeWebInfo/WebInfo/getImage/" + wbid);
        ch.setget("DefaultImage_" + wbid, temp, 86400L);
      }
      JSONObject Obj = JSONObject.toJSON(temp);
      if ((Obj != null) && (Obj.size() != 0)) {
        thumbnail = Obj.getString("thumbnail");
      }
      object.put("thumbnail", thumbnail);
    }
    return object;
  }

  public JSONArray getDefaultImage(JSONArray array)
  {
    CacheHelper ch = new CacheHelper();
    String thumbnail = ""; String wbid = "";
    JSONObject Obj = new JSONObject();
    if ((array != null) && (array.size() > 0)) {
      int l = array.size();
      for (Iterator localIterator = array.iterator(); localIterator.hasNext(); ) { Object obj = localIterator.next();
        JSONObject tempObj = (JSONObject)obj;
        String temp = tempObj.getString("wbid");
        if (!wbid.contains(temp)) {
          wbid = wbid + temp + ",";
        }
      }
      if (StringHelper.InvaildString(wbid)) {
        wbid = StringHelper.fixString(wbid, ',');
        String temp = ch.get("DefaultImage_" + wbid);
        if (temp == null) {
          temp = (String)appsProxy.proxyCall("/GrapeWebInfo/WebInfo/getImage/" + getRWbid(wbid));
          ch.setget("DefaultImage_" + wbid, temp, 86400L);
        }
        Obj = JSONObject.toJSON(temp);
      }
      if ((Obj != null) && (Obj.size() > 0)) {
        for (int i = 0; i < l; i++) {
          JSONObject object = (JSONObject)array.get(i);
          wbid = object.getString("wbid");
          thumbnail = Obj.getString(wbid);
          thumbnail = JSONObject.toJSON(thumbnail).getString("thumbnail");
          object.put("thumbnail", thumbnail);
          array.set(i, object);
        }
      }
    }
    return array;
  }
}