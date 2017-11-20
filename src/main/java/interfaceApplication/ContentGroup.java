package interfaceApplication;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.bson.types.ObjectId;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import JGrapeSystem.rMsg;
import Model.CommonModel;
import apps.appIns;
import apps.appsProxy;
import authority.plvDef.plvType;
import interfaceModel.GrapeDBSpecField;
import interfaceModel.GrapeTreeDBModel;
import nlogger.nlogger;
import session.session;
import string.StringHelper;

public class ContentGroup {
    private GrapeTreeDBModel group;
    private GrapeDBSpecField gDbSpecField;
    private CommonModel model;
    private session se;
    private JSONObject userInfo = null;
    private String currentWeb = null;
    private Integer userType = null;

    public ContentGroup() {
        model = new CommonModel();

        group = new GrapeTreeDBModel();
        gDbSpecField = new GrapeDBSpecField();
        gDbSpecField.importDescription(appsProxy.tableConfig("ContentGroup"));
        group.descriptionModel(gDbSpecField);
        group.bind();
        // group.enableCheck();//开启权限检查

        se = new session();
        userInfo = se.getDatas();
        if (userInfo != null && userInfo.size() != 0) {
            currentWeb = userInfo.getString("currentWeb"); // 当前用户所属网站id
            userType = userInfo.getInt("userType");
        }
    }

    // 新增
    public String GroupInsert(String GroupInfo) {
        String result = rMsg.netMSG(100, "新增栏目失败");
        String wbid = "";
        String contant = "0";
        JSONObject groupInfo = JSONObject.toJSON(GroupInfo);
        if (groupInfo != null && groupInfo.size() != 0) {
            if (groupInfo.containsKey("Contant")) {
                contant = groupInfo.getString("Contant");
            }
        }
        switch (contant) {
        case "0": // 不影响下级网站
            result = Add(groupInfo);
            break;
        case "1": // 下级网站同时新增栏目
            result = AddAllColumn(groupInfo);
            break;
        }
        if (!result.contains("errorcode")) {
            JSONObject object = group.eq("wbid", wbid).eq("name", groupInfo.getString("name")).find();
            result = object.toJSONString();
        }
        return result;
    }

    /**
     * 新增操作(不影响下级网站)
     * 
     * @project GrapeContent
     * @package model
     * @file ContentGroupModel.java
     * 
     * @param groupinfo
     * @return
     *
     */
    private String Add(JSONObject groupinfo) {
        JSONObject obj = checkColumn(groupinfo);
        if (obj != null && obj.size() > 0) {
            return rMsg.netMSG(1, "该栏目已存在");
        }
        JSONObject rMode = new JSONObject(plvType.chkType, plvType.powerVal).puts(plvType.chkVal, 100);// 设置默认查询权限
        JSONObject uMode = new JSONObject(plvType.chkType, plvType.powerVal).puts(plvType.chkVal, 200);
        JSONObject dMode = new JSONObject(plvType.chkType, plvType.powerVal).puts(plvType.chkVal, 300);
        groupinfo.put("rMode", rMode.toJSONString()); // 添加默认查看权限
        groupinfo.put("uMode", uMode.toJSONString()); // 添加默认修改权限
        groupinfo.put("dMode", dMode.toJSONString()); // 添加默认删除权限
        Object info = group.data(groupinfo).autoComplete().insertEx();
        return info != null ? info.toString() : null;
    }

    /**
     * 新增栏目，影响下级网站
     * 
     * @param groupinfo
     * @return
     */
    private String AddAllColumn(JSONObject groupinfo) {
        String result = rMsg.netMSG(100, "新增栏目失败");
        String[] value;
        if (groupinfo != null && groupinfo.size() != 0) {
            if (groupinfo.containsKey("wbid")) {
                String wbid = groupinfo.getString("wbid");
                if (!wbid.equals("")) {
                    value = model.getWeb(wbid);
                    result = AddAll(groupinfo, value);
                }
            }
        }
        return result;
    }

    /**
     * 同时新增下级网站栏目，0:添加一级栏目；1：非一级栏目
     * 
     * @param groupinfo
     * @param wbid
     * @return
     */
    @SuppressWarnings("unchecked")
    private String AddAll(JSONObject groupinfo, String[] wbid) {
        String fatherid;
        String result = rMsg.netMSG(100, "新增栏目失败");
        if (groupinfo.containsKey("fatherid")) {
            fatherid = groupinfo.getString("fatherid");
            if (fatherid.equals("") || fatherid.equals("0")) { // 添加一级栏目
                if (wbid != null) {
                    for (String id : wbid) {
                        groupinfo.put("wbid", id);
                        result = Add(groupinfo); // 新增操作
                    }
                }
            } else {
                result = AddColumn(groupinfo, fatherid, wbid);
            }
        }
        return result;
    }

    /**
     * 添加非一级栏目
     * 
     * @param groupinfo
     *            待新增的栏目信息
     * @param fatherid
     *            上级栏目id
     * @param wbid
     *            所有受影响的下级网站id
     * @return
     */
    @SuppressWarnings("unchecked")
    private String AddColumn(JSONObject groupinfo, String fatherid, String[] wbid) {
        String result = rMsg.netMSG(100, "新增栏目失败");
        String groupName = "";
        JSONObject temp = find(fatherid); // 获取上级栏目名称
        if (temp != null && temp.size() != 0) {
            groupName = temp.getString("name");
            group.or();
            for (String string : wbid) {
                group.eq("wbid", string);
            }
            group.and().eq("name", groupName);
            JSONArray ColumnArray = group.select();
            JSONObject tempobj = getOgid(ColumnArray);
            if (tempobj != null && tempobj.size() != 0) {
                for (String string : wbid) {
                    groupinfo.put("wbid", string);
                    groupinfo.put("fatherid", tempobj.getString(string));
                    result = Add(groupinfo);
                }
            }
        }
        return result;
    }

    /**
     * 重整栏目id和网站id
     * 
     * @project GrapeContent
     * @package model
     * @file ContentGroupModel.java
     * 
     * @param ColumnArray
     * @return {wbid:ogid,wbid:ogid...}
     *
     */
    @SuppressWarnings("unchecked")
    private JSONObject getOgid(JSONArray ColumnArray) {
        JSONObject object, rObject = new JSONObject();
        String wbid, ogid;
        if (ColumnArray != null && ColumnArray.size() != 0) {
            for (Object obj : ColumnArray) {
                object = (JSONObject) obj;
                wbid = object.getString("wbid");
                ogid = object.getMongoID("_id");
                rObject.put(wbid, ogid);
            }
        }
        return rObject;
    }

    /**
     * 根据栏目名称模糊查询，获取栏目id
     * @project GrapeContent
     * @package interfaceApplication
     * @file ContentGroup.java
     * 
     * @param name
     * @return id,id,id
     *
     */
    public String getOgid(String name) {
        JSONObject obj;
        String ogid = "",temp;
        JSONArray array = group.like("name", name).field("_id,name").select();
        if (array !=null && array.size() > 0) {
            for (Object object2 : array) {
                obj = (JSONObject)object2;
                temp = JSONObject.toJSON(obj.getString("_id")).getString("$oid");
                ogid +=temp+",";
            }
        }
        return StringHelper.fixString(ogid, ',');
    }
    // 根据栏目id查询栏目信息
    public JSONObject find(String ogid) {
        JSONObject object = group.eq("_id", ogid).find();
        return object != null ? object : null;
    }

    /**
     * 验证栏目是否已存在
     * 
     * @param object
     * @return 已存在： 栏目名称，类型，所属网站，所属上级栏目都相同
     */
    private JSONObject checkColumn(JSONObject object) {
        JSONObject obj = null;
        String wbid = "", type = "", Fatherid = "";
        if (object != null && object.size() > 0) {
            if (object.containsKey("wbid") && object.containsKey("type") && object.containsKey("fatherid")) {
                wbid = object.get("wbid").toString();
                type = object.get("type").toString();
                Fatherid = object.get("fatherid").toString();
                obj = group.eq("wbid", wbid).eq("type", type).eq("fatherid", Fatherid).find();
            }
        }
        return obj;
    }

    /**
     * 编辑栏目信息
     * 
     * @param ogid
     * @param groupInfo
     * @return
     */
    public String GroupEdit(String ogid, String groupInfo) {
        int code = 99;
        String result = rMsg.netMSG(100, "栏目修改失败");
        String wbid = "";
        String contant = "0", name = "";
        JSONObject groupinfo = JSONObject.toJSON(groupInfo);
        if (groupinfo != null && groupinfo.size() != 0) {
            if (groupinfo.containsKey("Contant")) { // contant:修改文章公开状态是否影响下级网站
                contant = groupinfo.getString("Contant");
                groupinfo.remove("Contant");
            }
            if (groupinfo.containsKey("name")) {
                name = groupinfo.getString("name");
            }
            if (name.equals("")) {
                contant = "0";
            }
            switch (contant) {
            case "0":
                code = group.eq("_id", ogid).data(groupinfo).update() != null ? 0 : 99;
                break;
            case "1": // 修改栏目信息，影响下级网站同名栏目
                JSONObject object = find(ogid);
                if (object != null && object.size() != 0) {
                    wbid = object.getString("wbid");
                    name = object.getString("name");
                }
                if (!wbid.equals("")) {
                    code = updateCid(model.getRWbid(wbid), name, groupinfo);
                }
                break;
            }
            result = code == 0 ? rMsg.netMSG(0, "修改成功") : result;
        }
        return result;
    }

    /**
     * 修改主站，同时修改子站
     * 
     * @project GrapeContent
     * @package model
     * @file ContentGroupModel.java
     * 
     * @param wbid
     *            主站点id
     * @param name
     *            栏目名称
     * @param groupinfo
     *            待修改数据
     * @return
     *
     */
    private int updateCid(String wbid, String name, JSONObject groupinfo) {
        JSONObject info;
        int i = 99;
        String temp = (String) appsProxy.proxyCall("/GrapeWebInfo/WebInfo/getWebTree/" + wbid);// 获取下级网站
        if (!temp.equals("")) {
            String[] value = temp.split(",");
            group.or();
            for (String string : value) {
                group.eq("wbid", string);
            }
            if (name.equals("")) {
                return 99;
            }
            JSONArray array = group.and().eq("name", name).select(); // 获取下级网站信息，同时包含主站
            JSONObject tempobj = getChildData(array, groupinfo);
            if (tempobj != null && tempobj.size() > 0) {
                for (String string : value) {
                    if (tempobj.containsKey(string)) {
                        info = JSONObject.toJSON(tempobj.getString(string));
                        if (info != null && info.size() > 0) {
                            i = group.eq("name", name).eq("wbid", string).data(info).update() != null ? 0 : 99;
                        }
                    }
                }
            }
        }
        return i;
    }

    /**
     * 获取下级栏目数据
     * 
     * @project GrapeContent
     * @package model
     * @file ContentGroupModel.java
     * 
     * @param array
     * @param objects
     * @return
     *
     */
    @SuppressWarnings("unchecked")
    private JSONObject getChildData(JSONArray array, JSONObject objects) {
        JSONObject objtemp, object = new JSONObject();
        String wbid;
        if (array != null && array.size() != 0) {
            for (Object obj : array) {
                objtemp = (JSONObject) obj;
                wbid = objtemp.getString("wbid");
                objtemp.remove("_id");
                objtemp.put("name", objects.getString("name"));
                objtemp.put("contentType", objects.getString("contentType"));
                objtemp.put("tempList", objects.getString("tempContent"));
                objtemp.put("tempContent", objects.getString("tempContent"));
                objtemp.put("isreview", objects.getString("isreview"));
                objtemp.put("slevel", objects.getString("slevel"));
                objtemp.put("sort", objects.getString("sort"));
                objtemp.put("editCount", objects.getString("editCount"));
                objtemp.put("timediff", Long.parseLong(objects.getString("timediff")));
                object.put(wbid, objtemp);
            }
        }
        return object;
    }

    /**
     * 删除栏目
     * 
     * @param ogid
     * @return
     */
    public String GroupDelete(String ogid) {
        String result = rMsg.netMSG(100, "删除失败");
        String[] value = null;
        // 删除该栏目下所有文章
        new Content().deleteByOgid(ogid);
        
        if (StringHelper.InvaildString(ogid)) {
            value = ogid.split(",");
        }
        if (value != null) {
            group.or();
            for (String id : value) {
                if (StringHelper.InvaildString(id)) {
                    group.eq("_id", id);
                }
            }
            long code = group.deleteAll();
            result = code > 0 ? rMsg.netMSG(0, "删除成功") : result;
        }
        return result;
    }

    /**
     * 按类型查找栏目
     * 
     * @param type
     * @param no
     * @return
     */
    public String FindByType(String type, int no) {
        JSONArray array = null;
        if (currentWeb != null && !currentWeb.equals("")) {
            group.eq("wbid", currentWeb);
            array = group.eq("contentType", type).limit(no).select();
        }
        return rMsg.netMSG(true, (array != null && array.size() > 0) ? array : new JSONArray());
    }

    /**
     * 按类型查找栏目，分页
     * 
     * @param type
     * @param no
     * @return
     */
    public String FindByTypePage(int idx, int pageSize, String type) {
        long total = 0;
        JSONArray array = null;
        if (currentWeb != null && !currentWeb.equals("")) {
            group.eq("wbid", currentWeb);
            group.eq("contentType", type);
            array = group.dirty().page(idx, pageSize);
            total = group.count();
        }
        return rMsg.netPAGE(idx, pageSize, total, (array != null && array.size() > 0) ? array : new JSONArray());
    }

    /**
     * 查询
     * 
     * @param groupinfo
     * @return
     */
    public String GroupFind(String groupinfo) {
        JSONArray array = null;
        JSONArray condArray = model.buildCond(groupinfo);
        condArray = (condArray != null && condArray.size() > 0) ? condArray : JSONArray.toJSONArray(groupinfo);
        if (condArray != null && condArray.size() > 0) {
            array = group.where(condArray).select();
        }
        return rMsg.netMSG(true, (array != null && array.size() > 0) ? array : new JSONArray());
    }

    /*-------------------前台---------------------*/
    /**
     * 分页
     * 
     * @param wbid
     * @param idx
     * @param pageSize
     * @return
     */
    public String GroupPage(String wbid, int idx, int pageSize) {
        return GroupPageBy(wbid, idx, pageSize, null);
    }

    /**
     * 条件分页
     * 
     * @param wbid
     * @param idx
     * @param pageSize
     * @param GroupInfo
     * @return
     */
    public String GroupPageBy(String wbid, int idx, int pageSize, String GroupInfo) {
        long total = 0;
        JSONArray array = null;
        if (!StringHelper.InvaildString(wbid)) {
            return rMsg.netPAGE(idx, pageSize, total, new JSONArray());
        }
        wbid = model.getRWbid(wbid);
        if (StringHelper.InvaildString(GroupInfo)) {
            JSONArray condArray = model.buildCond(GroupInfo);
            condArray = (condArray != null && condArray.size() > 0) ? condArray : JSONArray.toJSONArray(GroupInfo);
            if (condArray != null && condArray.size() > 0) {
                group.where(condArray);
            } else {
                return rMsg.netPAGE(idx, pageSize, total, new JSONArray());
            }
        }
        group.eq("wbid", wbid);
        total = group.dirty().count();
        array = group.page(idx, pageSize);
        return rMsg.netPAGE(idx, pageSize, total, join(array));
    }

    /*-------------------后台------*/
    // 分页
    public String GroupPageBack(int idx, int pageSize) {
        return GroupPageBy(currentWeb, idx, pageSize, null);
    }

    // 条件分页
    public String GroupPageByBack(int idx, int pageSize, String GroupInfo) {
        return GroupPageBy(currentWeb, idx, pageSize, GroupInfo);
    }

    /**
     * 获取当前文章所在栏目位置
     * 
     * @param ogid
     *            栏目id
     * @return
     */
    @SuppressWarnings("unchecked")
    public String getPrevCol(String ogid) {
        List<JSONObject> list = new ArrayList<>();
        JSONArray rList = new JSONArray();
        JSONObject temp = new JSONObject();
        String tempID = ogid;
        if (ogid != null && !ogid.equals("")) {
            while (temp != null) {
                if (!tempID.equals("0")) {
                    temp = group.eq("_id", tempID).field("_id,wbid,name,fatherid").find();
                    if (temp != null) {
                        list.add(temp);
                        if (temp.containsKey("fatherid")) {
                            tempID = temp.getString("fatherid");
                            if (tempID.equals("0")) {
                                temp = null;
                            }
                        } else {
                            temp = null;
                        }
                    }
                } else {
                    temp = null;
                }
            }
        }
        Collections.reverse(list);
        for (Object object : list) {
            JSONObject object2 = (JSONObject) object;
            rList.add(object2);
        }
        return rMsg.netMSG(true, rList);
    }

    /**
     * 根据栏目id获取栏目信息，支持批量操作
     * 
     * @param ogid
     * @return
     */
    public String getGroupById(String ogid) {
        JSONArray array = null;
        String[] value = ogid.split(",");
        group.or();
        try {
            for (String string : value) {
                group.eq("_id", string);
            }
            array = group.field("_id,tempContent,tempList").select();
        } catch (Exception e) {
            nlogger.logout(e);
            array = null;
        }
        array = join(array);
        return (array != null && array.size() != 0) ? array.toJSONString() : null;
    }

    /**
     * 获取栏目
     * 
     * @project GrapeContent
     * @package interfaceApplication
     * @file ContentGroup.java
     * 
     * @param condString
     * @return
     *
     */
    public String getColumnInfo(int idx, int pageSize, String wbid) {
        long total = 0;
        JSONArray array = null;
        if (StringHelper.InvaildString(wbid)) {
            group.eq("wbid", wbid);
            array = group.dirty().page(idx, pageSize);
            total = group.pageMax(pageSize);
            group.clear();
        }
        return rMsg.netPAGE(idx, pageSize, total, (array != null && array.size() > 0) ? join(array) : new JSONArray());
    }

    /**
     * 获取栏目
     * 
     * @project GrapeContent
     * @package interfaceApplication
     * @file ContentGroup.java
     * 
     * @param condString
     * @return
     *
     */
    public String getColumns(int idx, int pageSize, String wbid) {
        long total = 0;
        JSONArray array = null;
        if (StringHelper.InvaildString(wbid)) {
            // 查询条件含有wbid，判断wbid是否为本站点或者本站点的下级站点
            if (IsWeb(wbid)) {
                group.eq("wbid", wbid);
                array = group.dirty().page(idx, pageSize);
                total = group.pageMax(pageSize);
                group.clear();
            }
        }
        return rMsg.netPAGE(idx, pageSize, total, (array != null && array.size() > 0) ? join(array) : new JSONArray());
    }
    /**
     * 根据栏目id获取栏目名称
     * 
     * @project GrapeContent
     * @package interfaceApplication
     * @file ContentGroup.java
     * 
     * @param ogid
     * @return {ogid:name,ogid:name}
     *
     */
    @SuppressWarnings("unchecked")
    public String getColumnName(String ogid) {
        String[] value = null;
        JSONArray array = null;
        JSONObject object = new JSONObject(), obj;
        String oid = "", name = "";
        if (ogid != null && !ogid.equals("") && !ogid.equals("null")) {
            value = ogid.split(",");
        }
        if (value != null) {
            group.or();
            for (String id : value) {
                if (id != null && !id.equals("") && !id.equals("null")) {
                    group.eq("_id", id);
                }
            }
            array = group.field("_id,name").select();
        }
        if (array != null && array.size() > 0) {
            for (Object object2 : array) {
                obj = (JSONObject) object2;
                oid = obj.getMongoID("_id");
                name = obj.getString("name");
                object.put(oid, name);
            }
        }
        return object.toJSONString();
    }
    @SuppressWarnings("unchecked")
    private JSONArray join(JSONArray array) {
        JSONObject object;
        String content = "", list = "";
        int l = array.size();
        if (array == null || array.size() == 0) {
            return array;
        }
        try {
            JSONObject tempTemplateObj = getTempInfo(array);
            String tContentID = null;
            String tListID = null;
            for (int i = 0; i < l; i++) {
                object = (JSONObject) array.get(i);
                if (tempTemplateObj != null) {
                    tContentID = object.getString("tempContent");
                    if (tempTemplateObj.containsKey(tContentID)) {
                        content = tempTemplateObj.getString(tContentID);
                    }
                    tListID = object.getString("tempList");
                    if (tempTemplateObj.containsKey(tListID)) {
                        list = tempTemplateObj.getString(tListID);
                    }
                }
                object.put("TemplateList", list);
                object.put("TemplateContent", content);
                array.set(i, object);
            }
        } catch (Exception e) {
            nlogger.logout(e);
        }
        return array;
    }

    /**
     * 批量获取模版名称
     * 
     * @project GrapeContent
     * @package model
     * @file ContentGroupModel.java
     * 
     * @param array
     * @return
     *
     */
    private JSONObject getTempInfo(JSONArray array) {
        JSONObject object = new JSONObject(), tempobj = null;
        String content = null;
        String list = null;
        String tid = "";
        String temp = "";
        if (array != null && array.size() != 0) {
            int l = array.size();
            for (int i = 0; i < l; i++) {
                object = (JSONObject) array.get(i);
                content = object.getString("tempContent");
                list = object.getString("tempList");
                if (StringHelper.InvaildString(content) && !content.equals("0")) {
                    tid += content + ",";
                }
                if (StringHelper.InvaildString(list) && !list.equals("0")) {
                    tid += list + ",";
                }
            }
            if (!tid.equals("") && tid.length() > 0) {
                tid = StringHelper.fixString(tid, ',');
                if (!tid.equals("")) {
                    temp = (String) appsProxy.proxyCall("/GrapeTemplate/TemplateContext/TempFindByTid/s:" + tid);
                    tempobj = JSONObject.toJSON(temp);
                }
            }
        }
        return (tempobj != null && tempobj.size() != 0) ? tempobj : null;
    }

    /**
     * 查询条件若含有wbid，判断wbid是否为本站点或者本站点的下级站点
     * 
     * @project GrapeContent
     * @package interfaceApplication
     * @file ContentGroup.java
     * 
     * @param array
     * @return
     *
     */
    private boolean IsWeb(String wbid) {
        String web = "";
        if (StringHelper.InvaildString(wbid)) {
            // 获取当前站点及当前站点的下级站点
            appIns appIns = appsProxy.getCurrentAppInfo();
            web = appsProxy.proxyCall("/GrapeWebInfo/WebInfo/getWebTree/" + currentWeb, appIns).toString();
        }
        return web.contains(wbid);
    }

    /**
     * 根据栏目id获取该栏目的点击次数
     * 
     * @project GrapeContent
     * @package interfaceApplication
     * @file ContentGroup.java
     * 
     * @param ogid
     * @return
     *
     */
    @SuppressWarnings("unchecked")
    public String getClickCount(String ogid) {
        JSONObject object, rObject = new JSONObject();
        String id, clickCount;
        String[] value = null;
        if (StringHelper.InvaildString(ogid)) {
            value = ogid.split(",");
        }
        group.or();
        if (value != null) {
            for (String tempid : value) {
                if (!tempid.equals("")) {
                    group.eq("_id", tempid);
                }
            }
            JSONArray array = group.field("_id,clickcount").select();
            if (array != null && array.size() != 0) {
                for (Object object2 : array) {
                    object = (JSONObject) object2;
                    id = object.getString("_id");
                    clickCount = object.getString("clickcount");
                    rObject.put(id, Long.parseLong(clickCount));
                }
            }
        }
        return rObject.toJSONString();
    }

    /**
     * 获取关联栏目id
     * 
     * @project GrapeContent
     * @package interfaceApplication
     * @file ContentGroup.java
     * 
     * @param ogid
     *            当前栏目id
     * @return 当前栏目id和关联栏目id，格式为ogid,ogid,ogid
     *
     */
    public String getConnColumns(String ogid) {
        String column = "", columnId = "", id;
        JSONArray array = null;
        if (ogid != null && !ogid.equals("")) {
            JSONObject object = find(ogid);
            if (object != null && object.size() != 0) {
                if (object.containsKey("connColumn")) {
                    column = object.getString("connColumn");
                    if (!column.equals("0")) {
                        array = JSONArray.toJSONArray(column);
                        for (Object obj : array) {
                            object = (JSONObject) obj;
                            id = (object != null && object.size() != 0) ? object.getString("id") : "";
                            columnId += (!id.equals("") ? id : "") + ",";
                        }
                    }
                }
            }
            if (!columnId.equals("")) {
                columnId = StringHelper.fixString(columnId, ',');
                ogid = ogid + "," + columnId;
            }
        }
        return ogid;
    }

    /**
     * 网站id和栏目名称，获取栏目信息
     * 
     * @param wbid
     * @param name
     * @return
     */
    public String getColumnInfo(String wbid, String name) {
        JSONArray array = null;
        String[] value = null;
        if (wbid != null && !wbid.equals("")) {
            value = wbid.split(",");
            group.or();
            for (String string : value) {
                group.eq("wbid", string);
            }
            group.and();
            group.eq("name", name);
            array = group.select();
        }
        JSONObject rsObject = JoinObj(value, array);
        return (rsObject != null && rsObject.size() != 0) ? rsObject.toString() : null;
    }

    @SuppressWarnings("unchecked")
    private JSONObject JoinObj(String[] value, JSONArray array) {
        JSONObject rsObject = null, object;
        String id, wbid;
        if (array != null && array.size() != 0) {
            rsObject = new JSONObject();
            int l = array.size();
            for (int i = 0; i < l; i++) {
                object = (JSONObject) array.get(i);
                // id = ((JSONObject) object.get("_id")).getString("$oid");
                id = object.getString("_id");
                wbid = object.getString("wbid");
                for (String string : value) {
                    if (wbid.equals(string)) {
                        rsObject.put(string, id);
                    }
                }
            }
        }
        return rsObject;
    }

    /**
     * 检测是否为公开栏目，公开：0，非公开：1
     * 
     * @param ogid
     * @return
     */
    public String isPublic(String ogid) {
        String slevel = "0", temp;
        JSONObject object = group.eq("_id", ogid).find();
        if (object != null && object.size() != 0) {
            temp = object.getString("slevel");
            slevel = temp;
        }
        return slevel;
    }

    /**
     * 获取当前栏目的下级所有栏目，包含自身栏目
     * 
     * @project GrapeContent
     * @package interfaceApplication
     * @file ContentGroup.java
     * 
     * @param ogid
     * @return
     *
     */
    public String getAllColumn(String ogid) {
        JSONArray data = group.eq("fatherid", ogid).select();
        JSONObject object;
        String tmpWbid;
        String rsString = ogid;
        for (Object obj : data) {
            object = (JSONObject) obj;
            tmpWbid = ((JSONObject) object.get("_id")).get("$oid").toString();
            rsString = rsString + "," + getAllColumn(tmpWbid);
        }
        return StringHelper.fixString(rsString, ',');
    }

    /**
     * 设置栏目允许接受推送["ispush":"0"]
     * 
     * @param ogid
     * @param data
     * @return
     */
    public String SetPushState(String ogid, String data) {
        long code = 0;
        String result = rMsg.netMSG(100, "设置失败");
        String[] value = null;
        if (JSONObject.toJSON(data) != null) {
            if (ogid != null && !ogid.equals("")) {
                value = ogid.split(",");
            }
            if (value != null) {
                group.or();
                for (String string : value) {
                    group.eq("_id", string);
                }
                code = group.data(data).updateAll();
                result = code == 0 ? rMsg.netMSG(0, "设置成功") : result;
            }
        }
        return result;
    }

    /**
     * 获取某网站下的所有允许被推送文章的栏目信息
     * 
     * @project GrapeContent
     * @package interfaceApplication
     * @file ContentGroup.java
     * 
     * @param wbid
     * @return
     *
     */
    public String getPushArticle(String wbid) {
        JSONArray array = group.eq("wbid", wbid).eq("ispush", "0").select();
        return rMsg.netMSG(true, (array != null && array.size() > 0) ? array : new JSONArray());
    }

    /**
     * 获取指定网站下的所有栏目数据
     * 
     * @param wbid
     * @return
     */
    public String getPrevColumn(String wbid) {
        JSONArray array = group.eq("wbid", wbid).select();
        return (array != null && array.size() > 0) ? array.toJSONString() : new JSONArray().toJSONString();
    }
    
    /**
     * 获取指定网站下的所有栏目数据
     * 
     * @param wbid
     * @return
     */
    public String getOgidByName(String wbid,String columnName) {
        String ogid = "";
        if (StringHelper.InvaildString(wbid) && StringHelper.InvaildString(columnName)) {
            JSONObject object = group.eq("name", columnName).eq("wbid", wbid).field("_id").find();
            if (object != null && object.size() > 0) {
               ogid = object.getMongoID("_id"); 
            }
        }
        return ogid;
    }
}
