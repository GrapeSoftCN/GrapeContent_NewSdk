package interfaceApplication;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import JGrapeSystem.rMsg;
import Model.CommonModel;
import apps.appsProxy;
import cache.CacheHelper;
import interfaceModel.GrapeDBSpecField;
import interfaceModel.GrapeTreeDBModel;
import json.JSONHelper;
import security.codec;
import session.session;
import string.StringHelper;

public class ContentCache {
    private GrapeTreeDBModel content;
    private GrapeDBSpecField gDbSpecField;
    private CommonModel model;
    private session se;
    private JSONObject userInfo = null;
    private String currentWeb = null;

    public ContentCache() {
        model = new CommonModel();

        content = new GrapeTreeDBModel();
        gDbSpecField = new GrapeDBSpecField();
        gDbSpecField.importDescription(appsProxy.tableConfig("ContentCache"));
        content.descriptionModel(gDbSpecField);
        content.bindApp();
        content.enableCheck();// 开启权限检查

        se = new session();
        userInfo = se.getDatas();
        if (userInfo != null && userInfo.size() != 0) {
            currentWeb = userInfo.getString("currentWeb"); // 当前用户所属网站id
        }
    }

    /**
     * 推送文章到上级站点
     * 
     * @project GrapeContent
     * @package interfaceApplication
     * @file Content.java
     * 
     * @param ArticleInfo
     * @return
     *
     */
    @SuppressWarnings("unchecked")
    public String pushArticle(String wbid, String ArticleInfo) {
        int code = 99;
        String result = rMsg.netMSG(100, "推送失败");
        String oid = "", temp = "0";
        long state = 0;
        JSONObject object = JSONHelper.string2json(ArticleInfo);
        if (wbid != null && !wbid.equals("") && object != null && object.size() != 0) {
            String[] values = wbid.split(",");
            for (String value : values) {
                if (object.containsKey("ogid")) {
                    object.put("ogid", "");
                }
                if (object.containsKey("_id")) {
                    oid = ((JSONObject) object.get("_id")).getString("$oid");
                    object.remove("_id");
                }
                if (object.containsKey("wbid")) {
                    object.remove("wbid");
                }
                object.put("wbid", value);
                object = pushDencode(object);
                if (!CheckContentCache(oid, value)) {
                    if (object.containsKey("state")) {
                        temp = object.getString("state");
                        if (temp.contains("$numberLong")) {
                            temp = JSONObject.toJSON(temp).getString("$numberLong");
                        }
                        state = Long.parseLong(temp);
                    }
                    object.put("state", state);
                    code = content.data(object).autoComplete().insertOnce() != null ? 0 : 99;
                    result = code ==0 ?rMsg.netMSG(0, "推送成功"):result;
                }
            }
        }
        return result;
    }

    /**
     * 推送文章到下级站点的某个栏目【支持多站点多栏目】
     * 
     * @project GrapeContent
     * @package interfaceApplication
     * @file Content.java
     * 
     * @param columns
     *            格式为：{wbid:ogid,ogid,wbid:ogid,ogid,...}
     * @param ArticleInfo
     *            文章内容信息
     * @return
     *
     */
    @SuppressWarnings("unchecked")
    public String pushArticles(String columns, String ArticleInfo) {
        int code = 99;
        String result = rMsg.netMSG(100, "推送失败");
        String ogids, wbid;
        String[] value = null;
        JSONObject object;
        JSONObject columnInfo = JSONObject.toJSON(columns);
        JSONObject ObjContent = JSONObject.toJSON(ArticleInfo);
        if (columnInfo != null && columnInfo.size() != 0) {
            if (ObjContent != null && ObjContent.size() != 0) {
                object = ObjContent;
                for (Object key : columnInfo.keySet()) {
                    wbid = key.toString();
                    ogids = columnInfo.getString(wbid);
                    value = ogids.split(",");
                    if (value != null && !value.equals("")) {
                        for (String string : value) {
                            if (object.containsKey("ogid")) {
                                object.put("ogid", string);
                            }
                            if (object.containsKey("_id")) {
                                object.remove("_id");
                            }
                            if (object.containsKey("wbid")) {
                                object.put("wbid", wbid);
                            }
                            if (object.containsKey("content")) {
                                object = pushDencode(object);
                            }
                        }
                    }
                    code = content.data(object).autoComplete().insertOnce() != null ? 0 : 99;
                    result = code ==0 ?rMsg.netMSG(0, "推送成功"):result;
                }
            }
        }
        return result;
    }

    /**
     * 推送文章到指定栏目[当前网站的栏目]
     * 
     * @project GrapeContent
     * @package interfaceApplication
     * @file Content.java
     * 
     * @param oid
     * @param data
     * @return
     *
     */
    public String pushToColumn(String oid, String data) {
        Content c = new Content();
        String result = rMsg.netMSG(100, "推送失败");
        JSONObject object = content.eq("_id", oid).find();
        if (object != null && object.size() != 0) {
            object.remove("_id");
            object = remoNumberLong(object);
            String info = c.inserts(object);
            result = c.EditArticle(info, data);
            if (JSONObject.toJSON(result).getString("errorcode").equals("0")) {
                content.eq("_id", oid).delete();
            }
        }
        return result;
    }

    /**
     * 查看推送的文章
     * 
     * @project GrapeContent
     * @package interfaceApplication
     * @file Content.java
     * 
     * @param idx
     * @param pageSize
     * @param condString
     * @return
     *
     */
    public String searchPushArticle(int idx, int pageSize, String condString) {
        JSONArray array = null;
        long total = 0;
        if (StringHelper.InvaildString(currentWeb)) {
            if (!currentWeb.equals("")) {
                content.eq("wbid", currentWeb);
                if (condString != null) {
                    JSONArray condArray = JSONArray.toJSONArray(condString);
                    if (condArray != null && condArray.size() != 0) {
                        content.where(condArray);
                    }
                }
                array = content.dirty().page(idx, pageSize);
                total = content.count();
            }
        }
        return rMsg.netPAGE(idx, pageSize, total, model.getImgs(model.getDefaultImage(currentWeb, array)));
    }

    
    @SuppressWarnings("unchecked")
    private JSONObject remoNumberLong(JSONObject object) {
        String temp;
        String[] param = { "attribute", "sort", "type", "isdelete", "isvisble", "state", "substate", "slevel", "readCount", "u", "r", "d", "time" };
        if (object.containsKey("fatherid")) {
            temp = object.getString("fatherid");
            if (temp.contains("$numberLong")) {
                temp = JSONObject.toJSON(temp).getString("$numberLong");
            }
            object.put("fatherid", temp);
        }
        if (object.containsKey("tempid")) {
            temp = object.getString("tempid");
            if (temp.contains("$numberLong")) {
                temp = JSONObject.toJSON(temp).getString("$numberLong");
            }
            object.put("tempid", temp);
        }
        if (param != null && param.length > 0) {
            for (String value : param) {
                if (object.containsKey(value)) {
                    temp = object.getString(value);
                    if (temp.contains("$numberLong")) {
                        temp = JSONObject.toJSON(temp).getString("$numberLong");
                    }
                    object.put(value, Long.parseLong(temp));
                }
            }
        }
        return object;
    }

    @SuppressWarnings("unchecked")
    private JSONObject pushDencode(JSONObject obj) {
        String value = obj.get("content").toString();
        value = codec.DecodeHtmlTag(value);
        value = codec.decodebase64(value);
        obj.escapeHtmlPut("content", value);
        if (obj.containsKey("image")) {
            String image = obj.getString("image");
            if (!image.equals("") && image != null) {
                image = codec.DecodeHtmlTag(image);
                obj.put("image", model.RemoveUrlPrefix(image));
            }
        }
        if (obj.containsKey("thumbnail")) {
            String image = obj.getString("thumbnail");
            if (!image.equals("") && image != null) {
                image = codec.DecodeHtmlTag(image);
                obj.put("thumbnail", model.RemoveUrlPrefix(image));
            }
        }
        return obj;
    }

    /**
     * 验证文章是否存在
     * 
     * @project GrapeContent
     * @package interfaceApplication
     * @file Content.java
     * 
     * @param oid
     * @param wbid
     * @return
     *
     */
    private boolean CheckContentCache(String oid, String wbid) {
        JSONObject object = content.eq("_id", oid).eq("wbid", wbid).find();
        return (object != null && object.size() != 0);
    }
}
