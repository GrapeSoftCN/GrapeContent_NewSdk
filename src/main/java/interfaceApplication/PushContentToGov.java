package interfaceApplication;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import JGrapeSystem.rMsg;
import apps.appIns;
import apps.appsProxy;
import nlogger.nlogger;
import security.codec;
import session.session;
import string.StringHelper;
import time.TimeHelper;

/**
 * 推送文章至政府信息公开网
 * 
 * @author win7
 *
 */
public class PushContentToGov {
    private session se;
    private JSONObject userInfo = null;
    private String currentWeb = null;

    public PushContentToGov() {
        se = new session();
        userInfo = se.getDatas();
        if (userInfo != null && userInfo.size() != 0) {
            currentWeb = userInfo.getString("currentWeb"); // 当前用户所属网站id
        }
        currentWeb = "59301f571a4769cbf5b0a0dd";
    }

    /**
     * 获取政府信息公开网的栏目id
     * 
     * @return
     */
    public String getColumnID() {
        String cCode = "";
        JSONObject tempobj;
        // 获取单位组织代码
        if (StringHelper.InvaildString(currentWeb)) {
            tempobj = JSONObject.toJSON((String) appsProxy.proxyCall("/GrapeWebInfo/WebInfo/getGovInfoByID/" + currentWeb));
            if (tempobj != null && tempobj.size() > 0) {
                if (tempobj.containsKey("GovCode")) {
                    cCode = tempobj.getString("GovCode");
                }
            }
        }
        if (!StringHelper.InvaildString(cCode)) {
            return rMsg.netMSG(1, "单位组织代码错误");
        }
        String temp = (String) appsProxy.proxyCall("/tlsGMWeb/wsServer/getChannels/" + cCode);
        return rMsg.netMSG(0, temp);
    }

    /**
     * 推送文章到政府信息公开网
     * 
     * @param object
     *            新增的文章信息
     * @param cols
     *            关联栏目id
     * @return
     */
    public String pushToGov(JSONObject object, String cols) {
        int code = 0;
        String oid = "", cCode = "", CreateUsername = "", temp;
        JSONObject tempobj = null;
        if (object != null && object.size() > 0) {
            oid = object.getString("subID");
            // 获取单位代码，登录用户名（该组织在铜陵信息 公开网系统内登录的账号），通过GrapeWebInfo
            if (StringHelper.InvaildString(currentWeb)) {
                tempobj = JSONObject.toJSON((String) appsProxy.proxyCall("/GrapeWebInfo/WebInfo/getGovInfoByID/" + currentWeb));
                if (tempobj != null && tempobj.size() > 0) {
                    if (tempobj.containsKey("GovUserName")) {
                        CreateUsername = tempobj.getString("GovUserName");
                    }
                    if (tempobj.containsKey("GovCode")) {
                        cCode = tempobj.getString("GovCode");
                    }
                }
            }
            if (!StringHelper.InvaildString(CreateUsername) || !StringHelper.InvaildString(cCode)) {
                return rMsg.netMSG(3, "获取用户名或者单位代码错误");
            }
            // 获取附件信息
            JSONObject filejson = getFilejson(object);
            // 获取文章信息
            temp = getNewArticle(object, CreateUsername);
            if (StringHelper.InvaildString(temp)) {
                if (temp.contains("errorcode")) {
                    return temp;
                }
                JSONObject postParam = new JSONObject("param", codec.encodeFastJSON(temp));
                appIns apps = appsProxy.getCurrentAppInfo();
                temp = (String) appsProxy.proxyCall("/tlsGMWeb/wsServer/pushConent/" + cCode + "/" + cols + "/" + oid + "/" + filejson + "/b:" + true, postParam, apps);
                code = StringHelper.InvaildString(temp) ? Integer.parseInt(temp) : 0;
            }
        }
        return code != 0 ? rMsg.netMSG(0, "同步文章到市政府信息公开网成功") : rMsg.netMSG(0, "同步文章到市政府信息公开网失败");
    }

    /**
     * 封装需要的字段
     * 
     * @param object
     * @param userName
     * @return
     */
    @SuppressWarnings("unchecked")
    private String getNewArticle(JSONObject object, String userName) {
        String title = "", content = "", author = "";
        long time = TimeHelper.nowMillis();
        JSONObject newArticle = new JSONObject();
        if (object != null && object.size() > 0) {
            if (object.containsKey("mainName")) {
                title = object.getString("mainName");
            }
            if (object.containsKey("time")) {
                time = object.getLong("time");
            }
            if (object.containsKey("content")) {
                content = object.getString("content");
            }
            if (object.containsKey("author")) {
                author = object.getString("author");
            }
            if (!StringHelper.InvaildString(title)) {
                return rMsg.netMSG(1, "文章标题不可为空");
            }
            if (!StringHelper.InvaildString(content)) {
                return rMsg.netMSG(2, "文章内容不可为空");
            }
            newArticle.put("title", title);
            newArticle.put("content", content);
            newArticle.put("author", author);
            newArticle.put("CreateUsername", userName);
            newArticle.put("keywords", "");
            newArticle.put("createdate", getTime(time));
        }
        return newArticle.toString();
    }

    /**
     * 获取附件信息，封装成json
     * 
     * @param object
     * @return
     */
    @SuppressWarnings("unchecked")
    private JSONObject getFilejson(JSONObject object) {
        String attr = "", filename = "", filepath = "";
        JSONArray attrArray = null;
        JSONObject tempobj, filejson = null;
        if (object != null && object.size() > 0) {
            if (object.containsKey("attrid")) {
                attr = object.getString("attrid");
                attrArray = JSONArray.toJSONArray(attr);
            }
        }
        if (attrArray != null && attrArray.size() > 0) {
            filejson = new JSONObject();
            for (Object object2 : attrArray) {
                tempobj = (JSONObject) object2;
                filename = tempobj.getString("fileoldname");
                filepath = tempobj.getString("filepath");
                filejson.put(filename, filepath);
            }
        }
        return filejson;
    }

    /**
     * 时间格式转换为 年-月-日
     * 
     * @param time
     * @return
     */
    private String getTime(long time) {
        Date dates = null;
        String times = "";
        try {
            String date = TimeHelper.stampToDate(time);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            dates = sdf.parse(date);
            times = sdf.format(dates);
        } catch (Exception e) {
            nlogger.logout(e);
        }
        return times;
    }

}
