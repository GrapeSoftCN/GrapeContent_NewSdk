package interfaceApplication;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.bson.types.ObjectId;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.jsoup.Jsoup;

import Model.CommonModel;
import Model.WsCount;
import common.java.Concurrency.distributedLocker;
import common.java.JGrapeSystem.rMsg;
import common.java.apps.appIns;
import common.java.apps.appsProxy;
import common.java.authority.plvDef.UserMode;
import common.java.browser.PhantomJS;
import common.java.cache.CacheHelper;
import common.java.check.checkHelper;
import common.java.database.dbFilter;
import common.java.httpServer.grapeHttpUnit;
import common.java.interfaceModel.GrapeDBSpecField;
import common.java.interfaceModel.GrapeTreeDBModel;
import common.java.json.JSONHelper;
import common.java.nlogger.nlogger;
import common.java.offices.excelHelper;
import common.java.privacyPolicy.privacyPolicy;
import common.java.rpc.execRequest;
import common.java.security.codec;
import common.java.session.session;
import common.java.string.StringHelper;
import common.java.thirdsdk.kuweiCheck;
import common.java.time.TimeHelper;

public class Content {
    private GrapeTreeDBModel content;
    private CommonModel model;
    private session se;
    private JSONObject userInfo = null;
    private String currentWeb = null;
    private Integer userType = null;
    private CacheHelper ch;
    private String pkString = null;;
    private static ExecutorService rs = Executors.newFixedThreadPool(300);

    private static final long delay = 3600;

    private GrapeTreeDBModel getDB() {
        GrapeTreeDBModel _content = new GrapeTreeDBModel();
        GrapeDBSpecField _gDbSpecField = new GrapeDBSpecField();
        _gDbSpecField.importDescription(appsProxy.tableConfig("Content"));
        _content.descriptionModel(_gDbSpecField);
        _content.bindApp();
        return _content;
    }

    public Content() {
        ch = new CacheHelper();
        model = new CommonModel();

        content = getDB();
        pkString = content.getPk();

        se = new session();
        userInfo = se.getDatas();
        if (userInfo != null && userInfo.size() != 0) {
            currentWeb = userInfo.getString("currentWeb"); // 当前用户所属网站id
            userType = userInfo.getInt("userType");// 当前用户身份
        }
    }

    /**
     * 获取某个栏目下的文章数量
     * 
     * @param ogid
     * @return
     */
    protected long getContentCount(String ogid) {
        long count = 0;
        if (StringHelper.InvaildString(ogid)) {
            count = content.eq("ogid", ogid).count();
        }
        return count;
    }

    /**
     * 移动文章到指定栏目[当前网站的栏目]
     * 
     * @project GrapeContent
     * @package interfaceApplication
     * @file Content.java
     * 
     * @param oid
     * @param ogid
     * @return
     *
     */
    public String pushToColumn(String oid, String ogid) {
        JSONObject rs = new JSONObject();
        String result = rMsg.netMSG(100, "文章移动失败");
        if (!StringHelper.InvaildString(oid)) {
            return rMsg.netMSG(1, "无效文章id");
        }
        if (!StringHelper.InvaildString(oid)) {
            return rMsg.netMSG(1, "无效栏目id");
        }
        if (ObjectId.isValid(ogid) || checkHelper.isInt(ogid)) {
            JSONObject obj = new JSONObject("ogid", ogid);
            rs = content.eq(pkString, oid).data(obj).update();
        }
        return (rs != null) ? rMsg.netMSG(0, "文章移动成功") : result;
    }

    /**
     * 删除指定栏目下的文章
     * 
     * @param ogid
     * @return
     */
    protected String deleteByOgid(String ogid) {
        JSONArray condArray = null;
        String result = rMsg.netMSG(1, "删除失败");
        if (StringHelper.InvaildString(ogid)) {
            condArray = model.getOrCondArray("ogid", ogid);
            if (condArray != null && condArray.size() > 0) {
                long code = content.or().where(condArray).deleteAll();
                result = code > 0 ? rMsg.netMSG(0, "删除成功") : result;
            }
        }
        return result;
    }

    /**
     * 添加推送文章到content表
     * 
     * @param object
     * @return
     */
    protected String inserts(JSONObject object) {
        return content.data(object).insertOnce().toString();
    }

    /**
     * 批量添加文章
     * 
     * @project GrapeContent
     * @package interfaceApplication
     * @file Content.java
     * 
     * @param ArticleInfo
     *            参数格式为[{文章数据1},{文章数据2},...]
     * @return
     *
     */
    @SuppressWarnings("unchecked")
    public String AddAllArticle(String ArticleInfo) {
        String tip = rMsg.netMSG(100, "新增失败");
        JSONObject object;
        String info;
        List<String> list = new ArrayList<>();
        long time = 0;
        int code = 99;
        JSONArray array = JSONHelper.string2array(ArticleInfo);
        if (array != null && array.size() != 0) {
            for (Object obj : array) {
                object = (JSONObject) obj;
                if (object.containsKey("time")) {
                    time = Long.parseLong(object.getString("time"));
                    object.put("time", time);
                }
                info = AddAll(object);
                if (info == null) {
                    if (list.size() != 0) {
                        DeleteArticle(StringHelper.join(list));
                    }
                    code = 99;
                    break;
                }
                code = 0;
                list.add(info);
            }
            if (code == 0) {
                tip = rMsg.netMSG(true, batch(list));
            } else {
                tip = rMsg.netMSG(100, "新增失败");
            }
        }
        return tip;
    }

    // 批量查询,类方法内部使用
    private JSONArray batch(List<String> list) {
        JSONArray array = new JSONArray();
        if (list != null && list.size() > 0) {
            String ids = StringHelper.join(list);
            JSONArray condArray = model.getOrCond(pkString, ids);
            if (condArray != null && condArray.size() > 0) {
                array = content.or().where(condArray).select();
            }
        }
        return model.join(model.getImgs(model.ContentDencode(array)));
    }

    // 批量添加
    private String AddAll(JSONObject object) {
        Object tip;
        String info = checkparam(object);
        tip = (JSONHelper.string2json(info) != null && !info.contains("errorcode")) ? tip = content.data(info).autoComplete().insertOnce() : null;
        return tip != null ? tip.toString() : null;
    }

    /**
     * 验证文章是否存在
     * 
     * @param ogid
     *            栏目id
     * @param mainName
     *            文章标题
     * @param contentInfo
     *            文章内容
     * @return 返回0:表示文章不存在；返回1:表示文章已存在；
     */
    public String ContentIsExist(String ogid, String mainName, int type) {
        String contentInfo = "";
        JSONObject object = JSONObject.toJSON(execRequest.getChannelValue(grapeHttpUnit.formdata).toString());
        contentInfo = object.getString("param");
        return ContentIsExist(ogid, mainName, type, contentInfo);
    }

    /**
     * 验证文章是否存在
     * 
     * @param ogid
     *            栏目id
     * @param mainName
     *            文章标题
     * @param contentInfo
     *            文章内容
     * @param type
     *            0:正常文章；1：外链文章
     * @return 返回0:表示文章不存在；返回1:表示文章已存在；
     */
    public String ContentIsExist(String ogid, String mainName, int type, String contentInfo) {
        JSONObject object = null;
        contentInfo = codec.DecodeHtmlTag(contentInfo);
        contentInfo = codec.decodebase64(contentInfo);
        if (StringHelper.InvaildString(ogid)) {
            content.eq("ogid", ogid).eq("mainName", mainName);
            if (type == 0) {
                content.eq("content", contentInfo);
            } else {
                content.eq("contenturl", "contenturl");
            }
            object = content.find();
        }
        return (object != null && object.size() > 0) ? "1" : "0";
    }

    /**
     * 将爬虫爬取的数据添加至数据库
     * 
     * @return
     */
    public String AddCrawlerContent(String infp) {
        String contentInfo = "";
        JSONObject object = JSONObject.toJSON(execRequest.getChannelValue(grapeHttpUnit.formdata).toString());
        contentInfo = object.getString("param");
        return AddCrawlerContent(infp, contentInfo);
    }

    /**
     * 将爬虫爬取的数据添加至数据库
     * 
     * @return
     */
    @SuppressWarnings("unchecked")
    public String AddCrawlerContent(String infp, String contentInfo) {
        int state = 2;
        Object info = null;
        String result = rMsg.netMSG(100, "添加失败");
        contentInfo = codec.DecodeFastJSON(contentInfo);
        JSONObject object = JSONObject.toJSON(contentInfo);
        if (object != null && object.size() > 0) {
            if (object.containsKey("ogid")) { // 文章状态
                String ogid = object.getString("ogid");
                String temp = new ContentGroup().isPublic(ogid);
                if (temp.equals("1")) {
                    state = 0;
                }
                object.put("state", state);
            }
            info = content.data(object).autoComplete().insertOnce();
            result = (info != null) ? rMsg.netMSG(0, "添加成功") : result;
        }
        return result;
    }

    /**
     * 发布文章
     * 
     * @param ArticleInfo
     * @return
     */
    @SuppressWarnings("unchecked")
    public String PublishArticle(String ArticleInfo) {
        long state = 2, time = 0;
        String temptime = "";
        long currentTime = TimeHelper.nowMillis();
        ArticleInfo = codec.DecodeFastJSON(ArticleInfo);
        JSONObject object = JSONHelper.string2json(ArticleInfo);
        if (!StringHelper.InvaildString(currentWeb)) {
            return rMsg.netMSG(1, "当前登录信息已失效,请重新登录后再发布文章");
        }
        if (object == null || object.size() <= 0) {
            return rMsg.netMSG(100, "发布失败");
        }
        if (object.containsKey("time")) {
            temptime = object.getString("time");
            if (StringHelper.InvaildString(temptime)) {
                time = Long.parseLong(temptime);
            }
        }
        if (time != 0) {
            time = (time < currentTime) ? time : currentTime;
        } else {
            time = currentTime;
        }
        object.put("time", time);
        object.put("wbid", currentWeb);
        if (object.containsKey("ogid")) { // 文章状态
            String ogid = object.getString("ogid");
            String temp = new ContentGroup().isPublic(ogid);
            if (temp.equals("1")) {
                state = 0;
            }
            object.put("state", state);
        }
        object.put("isvisble", 0);
        object.put("isdelete", 0);
        object.put("subID", RandomNum()); // 副id，即_id不能被使用时，使用subID
        return insert(object);
    }

    /**
     * 生成十进制随机数
     * 
     * @return
     */
    private int RandomNum() {
        int number = new Random().nextInt(2147483647) + 1;
        return number;
    }

    /**
     * 新增操作
     * 
     * @param contentInfo
     * @return
     */
    private String insert(JSONObject contentInfo) {
        String info = null;
        int state = 0;
        JSONObject ro = null;
        String result = rMsg.netMSG(100, "新增文章失败");
        try {
            info = checkparam(contentInfo);
            JSONObject object = JSONObject.toJSON(info);
            if (object != null && object.size() > 0) {
                if (object.containsKey("errorcode")) {
                    return info;
                }
                state = object.getInt("state");
                info = content.data(object).autoComplete().insertOnce().toString();
                ro = findOid(info);

                result = (ro != null && ro.size() > 0) ? rMsg.netMSG(0, ro) : result;
//                appsProxy.proxyCall("/GrapeSendKafka/SendKafka/sendData2Kafka/" + info + "/int:1/int:1/int:" + state);
            }
        } catch (Exception e) {
            nlogger.logout(e);
            result = rMsg.netMSG(100, "新增文章失败");
        }
        return result;
    }

    // 若文章为超链接文章，获取网页缩略图
    /**
     * 发布文章,包含内容错别字检测及隐私数据检测
     * 
     * @param ArticleInfo
     * @return
     */
    @SuppressWarnings("unchecked")
    public String Publish(String ArticleInfo) {
        long state = 2;
        long currentTime = TimeHelper.nowMillis();
        ArticleInfo = codec.DecodeFastJSON(ArticleInfo);
        JSONObject object = JSONHelper.string2json(ArticleInfo);
        if (!StringHelper.InvaildString(currentWeb)) {
            return rMsg.netMSG(1, "当前登录信息已失效,请重新登录后再发布文章");
        }
        if (object == null || object.size() <= 0) {
            return rMsg.netMSG(100, "发布失败");
        }
        if (object.containsKey("time")) {
            long time = Long.parseLong(object.getString("time"));
            if (time > currentTime) {
                object.put("time", currentTime);
            }
        } else {
            object.put("time", currentTime);
        }
        object.put("wbid", currentWeb);
        if (object.containsKey("ogid")) { // 文章状态
            String ogid = object.getString("ogid");
            String temp = new ContentGroup().isPublic(ogid);
            if (temp.equals("1")) {
                state = 0;
            }
            object.put("state", state);
        }
        object.put("subID", RandomNum()); // 副id，即_id不能被使用时，使用subID
        return insertCheckContent(object);
    }

    /**
     * 新增操作,检测文章内容是否含有错别字或者隐私信息
     * 
     * @param contentInfo
     * @return
     */
    @SuppressWarnings("unchecked")
    private String insertCheckContent(JSONObject contentInfo) {
        String info = null;
        String result = rMsg.netMSG(100, "新增文章失败");
        try {
            contentInfo.put("isvisble", 1);
            info = checkparam(contentInfo);
            JSONObject object = JSONObject.toJSON(info);
            if (object != null && object.size() > 0) {
                if (info.contains("errorcode")) {
                    return info;
                }
                int state = object.getInt("state");
                String _info = content.data(object).autoComplete().insertOnce().toString();
                appIns env = appsProxy.getCurrentAppInfo();
                rs.execute(() -> {
                    appsProxy.setCurrentAppInfo(env);
                    _insertCheck(contentInfo, _info, state);
                });
                result = rMsg.netMSG(0, "文章已进入发布队列");
            }
        } catch (Exception e) {
            nlogger.logout(e);
            result = rMsg.netMSG(100, "新增文章失败");
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    void _insertCheck(JSONObject contentInfo, String info, int state) {
        JSONObject obj = new JSONObject();
        String cid = checkContent(contentInfo, info);
        if (StringHelper.InvaildString(cid)) {
            obj.put("isvisble", 4); // 含有错别字，检测不通过
        } else {
            // 删除错误信息表中的数据
            new ContentError().delete(info);
            obj.put("isvisble", 0); // 直接显示
        }
        update(info, obj);
        // 发送数据到kafka
        appsProxy.proxyCall("/GrapeSendKafka/SendKafka/sendData2Kafka/" + info + "/int:1/int:1/int:" + state);
    }

    private void update(String oid, JSONObject obj) {
        GrapeTreeDBModel model = getDB();
        if (StringHelper.InvaildString(oid) && obj != null && obj.size() > 0) {
            model.eq("_id", oid).data(obj).update();
        }
    }

    /**
     * 查询未通过检测的文章
     * 
     * @param idx
     * @param pageSize
     * @return
     */
    public String searchNotCheck(int idx, int pageSize) {
        GrapeTreeDBModel content = getDB();
        long count = 0;
        content.eq("isvisble", 4);
        count = content.dirty().count();
        JSONArray array = content.page(idx, pageSize);
        return rMsg.netPAGE(idx, pageSize, count, array);
    }

    /**
     * 文章检测
     * 
     * @param object
     * @return
     */
    private String checkContent(JSONObject object, String oid) {
        privacyPolicy pp = new privacyPolicy();
        String contents = "", message, errorcount = "0", info = null;
        String cid = null;
        if (object != null && object.size() > 0) {
            if (object.containsKey("content")) {
                // contents = object.getString("content");
                contents = (String) object.escapeHtmlGet("content");
                if (!StringHelper.InvaildString(contents)) {
                    return rMsg.netMSG(1, "文章内容不允许为空");
                }
                info = pp.scanText(contents);
                JSONObject obj = JSONObject.toJSON(ContentTypos(info)); // 错别字检测
                message = obj.getString("message");
                obj = JSONObject.toJSON(message.toLowerCase());
                errorcount = obj.getString("errorcount");
                if (!errorcount.equals("0") || pp.hasPrivacyPolicy()) {
                    cid = new ContentError().insert(obj.toJSONString(), oid);
                }
            }
        }
        return cid;
    }

    private String ContentTypos(String contents) {
        String result = rMsg.netMSG(100, "错别字识别失败");
        String ukey = "377c9dc160bff6cfa3cc0cbc749bb11a";
        try {
            if (StringHelper.InvaildString(contents)) {
                kuweiCheck check = new kuweiCheck(ukey);
                contents = check.checkContent(contents);
                result = rMsg.netMSG(0, contents);
            }
        } catch (Exception e) {
            nlogger.logout(e);
            result = rMsg.netMSG(101, "服务器连接异常，暂无法识别");
        }
        return result;

    }

    /**
     * 推送文章到政府信息公开网，通过文章设置的政府信息公开网栏目id
     * 
     * @param object
     * @return
     */
    private String PushArticleGov(JSONObject object) {
        String GovId = "";
        String result = rMsg.netMSG(100, "同步文章至政府信息公开网失败");
        if (object != null && object.size() > 0) {
            if (object.containsKey("GovId")) {
                GovId = object.getString("GovId");
            }
            if (StringHelper.InvaildString(GovId) && !GovId.equals("0")) {
                // 同步文章到政府信息公开网
                result = new PushContentToGov().pushToGov(object, GovId);
            }
        }
        return result;
    }

    /**
     * 推送文章到政府信息公开网，通过设置关联栏目id
     * 
     * @param object
     * @return
     */
    private String PushGov(JSONObject object) {
        String result = rMsg.netMSG(100, "同步文章至政府信息公开网失败");
        if (object != null && object.size() > 0) {
            String ogid = getConnColumn(object);
            if (StringHelper.InvaildString(ogid) && !ogid.equals("0")) {
                // 同步文章到政府信息公开网
                result = new PushContentToGov().pushToGov(object, ogid);
            } else {
                return rMsg.netMSG(3, "无关联栏目id，无法同步文章至政府信息公开网");
            }
        }
        return result;
    }

    /**
     * 获取关联的市政府信息公开网栏目id
     * 
     * @param object
     * @return
     */
    private String getConnColumn(JSONObject object) {
        String ogid = "", cols = "";
        JSONObject tempobj;
        if (object != null && object.size() > 0) {
            if (object.containsKey("ogid")) {
                ogid = object.getString("ogid");
            }
            if (StringHelper.InvaildString(ogid)) {
                tempobj = new ContentGroup().find(ogid);
                if (tempobj != null && tempobj.size() > 0) {
                    if (tempobj.containsKey("connColumn")) {
                        cols = tempobj.getString("connColumn");
                    }
                }
            }
        }
        return cols;
    }

    /**
     * 错别字识别
     * 
     * @param contents
     */
    public String Typos(String contents) {
        String result = rMsg.netMSG(100, "错别字识别失败");
        String ukey = "377c9dc160bff6cfa3cc0cbc749bb11a";
        try {
            if (StringHelper.InvaildString(contents)) {
                contents = model.dencode(contents);
                kuweiCheck check = new kuweiCheck(ukey);
                contents = check.checkContent(contents);
                result = rMsg.netMSG(0, contents);
            }
        } catch (Exception e) {
            nlogger.logout(e);
            result = rMsg.netMSG(101, "服务器连接异常，暂无法识别");
        }
        return result;
    }

    /**
     * 修改文章
     * 
     * @param oid
     * @param contents
     * @return
     */
    public String EditArticle(String oid, String contents) {
        Object temp = null;
        String result = rMsg.netMSG(100, "文章更新失败");
        contents = codec.DecodeFastJSON(contents);
        JSONObject infos = JSONHelper.string2json(contents);
        if (userInfo == null || userInfo.size() <= 0) {
            return rMsg.netMSG(1, "当前登录信息已失效,请重新登录后再修改文章");
        }
        if (infos != null && infos.size() > 0) {
            contents = checkparam(infos);
            if (JSONHelper.string2json(contents) != null && contents.contains("errorcode")) {
                return contents;
            }
            temp = content.eq("_id", oid).data(infos).updateEx();
            result = (temp != null) ? rMsg.netMSG(0, "文章更新成功") : result;
        }
        return result;
    }

    /**
     * 修改文章，检测隐私信息，演示
     * 
     * @param oid
     * @param contents
     * @return
     */
    @SuppressWarnings("unchecked")
    public String Edit(String oid, String contents, String flag) {
        Object temp = null;
        String result = rMsg.netMSG(100, "文章更新失败");
        contents = codec.DecodeFastJSON(contents);
        JSONObject infos = JSONHelper.string2json(contents);
        if (userInfo == null || userInfo.size() <= 0) {
            return rMsg.netMSG(1, "当前登录信息已失效,请重新登录后再修改文章");
        }
        if (infos != null && infos.size() > 0) {
            contents = checkparam(infos);
            if (JSONHelper.string2json(contents) != null && contents.contains("errorcode")) {
                return contents;
            }
            if (!infos.containsKey("content")) {
                temp = content.eq(pkString, oid).data(infos).updateEx();
            } else {
                if (flag.equals("0")) {
                    infos.put("isvisble", 0);
                    temp = content.eq(pkString, oid).data(infos).updateEx();
                } else {
                    temp = content.eq(pkString, oid).data(infos).updateEx();
                    appIns env = appsProxy.getCurrentAppInfo();
                    rs.execute(() -> {
                        appsProxy.setCurrentAppInfo(env);
                        _insertCheck(infos, oid, 0);
                    });
                }
            }
            result = rMsg.netMSG(0, "文章已进入发布队列");
        }
        return result;
    }

    /**
     * 删除文章，支持批量删除
     * 
     * @param oid
     * @return
     */
    @SuppressWarnings("unchecked")
    public String DeleteArticle(String oid) {
        int code = -1;
        String result = rMsg.netMSG(100, "删除失败");
        JSONArray condArray = model.getOrCond(pkString, oid);
        if (condArray != null && condArray.size() > 0) {
            JSONObject object = new JSONObject();
            object.put("isdelete", 1);
            code = content.or().where(condArray).data(object).updateAll() > 0 ? 0 : 99;
            // code = content.or().where(condArray).deleteAll() > 0 ? 0 : 99;
            result = (code == 0) ? rMsg.netMSG(0, "删除成功") : result;
        }
        return result;
    }

    /**
     * 导出文章信息
     * 
     * @param ContentInfo
     * @param fileName
     * @return
     */
    public Object ExportContent(String ContentInfo, String fileName) {
        try {
            ContentInfo = SearchExportInfo(ContentInfo);
            if (StringHelper.InvaildString(ContentInfo)) {
                return excelHelper.out(ContentInfo);
            }
        } catch (Exception e) {
            nlogger.logout(e, "导出异常");
        }
        return rMsg.netMSG(false, "导出失败");
    }

    /**
     * 整理查询出的文章数据，导出
     * 
     * @param array
     * @return
     */
    @SuppressWarnings("unchecked")
    private String SearchExportInfo(String condString) {
        JSONObject obj, column;
        String temp, ogids = "";
        JSONArray array = null;
        if (StringHelper.InvaildString(condString)) {
            JSONArray condArray = JSONArray.toJSONArray(condString);
            array = content.eq("wbid", currentWeb).where(condArray).field("mainName,state,souce,author,time,ogid").select();
            if (array != null && array.size() > 0) {
                // 获取栏目id
                for (Object object : array) {
                    obj = (JSONObject) object;
                    temp = obj.getString("ogid");
                    if (!ogids.contains(temp)) {
                        ogids += temp + ",";
                    }
                }
                String ColumnInfo = new ContentGroup().getColumnName(StringHelper.fixString(ogids, ','));
                column = JSONObject.toJSON(ColumnInfo);
                if (column != null && column.size() > 0) {
                    int l = array.size();
                    for (int i = 0; i < l; i++) {
                        obj = (JSONObject) array.get(i);
                        temp = obj.getString("ogid");
                        obj.put("column", column.getString(temp));
                        array.set(i, obj);
                    }
                }
            }
        }
        return CollateInfo(array);
    }

    /**
     * 整理查询出的文章信息，用于导出
     * 
     * @param array
     * @return
     */
    @SuppressWarnings("unchecked")
    private String CollateInfo(JSONArray array) {
        String rString = null;
        JSONObject obj;
        long state = 0, time = 0;
        String statString = "审核通过", timeDate = null, mainName = "", author = "", souce = "";
        if (array != null && array.size() > 0) {
            int l = array.size();
            for (int i = 0; i < l; i++) {
                obj = (JSONObject) array.get(i);
                if (obj.containsKey("state")) {
                    state = obj.getLong("state");
                    statString = (state == 0) ? "待审核" : (state == 1) ? "审核不通过" : "审核通过";
                }
                if (obj.containsKey("time")) {
                    time = obj.getLong("time");
                    timeDate = (time != 0) ? TimeHelper.stampToDate(time) : null;
                }
                if (obj.containsKey("mainName")) {
                    mainName = obj.getString("mainName");
                }
                if (obj.containsKey("author")) {
                    author = obj.getString("author");
                }
                if (obj.containsKey("souce")) {
                    souce = obj.getString("souce");
                }
                obj.put("文章标题", mainName);
                obj.put("来源", souce);
                obj.put("作者", author);
                obj.put("发布时间", timeDate);
                obj.put("文章状态", statString);
                obj.put("所属栏目", obj.getString("column"));
                obj.remove(pkString);
                obj.remove("ogid");
                obj.remove("column");
                obj.remove("mainName");
                obj.remove("state");
                obj.remove("souce");
                obj.remove("author");
                obj.remove("time");
                array.set(i, obj);
            }
            rString = array.toJSONString();
        }
        return rString;
    }

    /**
     * 获取最新公开的文章信息 【查询条件：state:2】当前网站下审核通过的文章
     * 
     * @param wbid
     * @param idx
     * @param pageSize
     * @return
     */
    public String FindNewArc(String wbid, int idx, int pageSize) {
        long total = 0;
        JSONArray array = null;
        content.eq("wbid", model.getRWbid(wbid)).eq("state", 2).eq("isdelete", 0).eq("isvisble", 0).desc("time").desc(pkString);
        array = content.dirty().mask("content").page(idx, pageSize);
        total = content.count();
        array = model.ContentDencode(array);
        array = model.setTemplate(array);
        return rMsg.netPAGE(idx, pageSize, total, (array != null && array.size() > 0) ? model.join(array) : new JSONArray());
    }

    /**
     * 根据oid显示文章 同时显示上一篇文章id，名称， 下一篇文章id，名称,显示文章所有数据信息
     * 
     * @return
     */
    public String findArticle(String oid) {
        String result = rMsg.netMSG(102, "文章不存在");
        GrapeTreeDBModel content = getDB();
        JSONObject obj = content.eq("_id", oid).eq("isdelete", 0).eq("isvisble", 0).find();
        int code = isShow(obj);
        switch (code) {
        case 0:
            result = getSingleArticle(obj, oid);
            // 发送数据到kafka
//            appsProxy.proxyCall("/GrapeSendKafka/SendKafka/sendData2Kafka/" + oid + "/int:1/int:2/int:2");
            break;
        case 1:
            result = rMsg.netMSG(3, "您不属于该单位，无权查看该单位信息");
            break;
        case 2:
            result = rMsg.netMSG(4, "请先登录");
            break;
        }
        return result;
    }

    /**
     * 获取文章所在栏目位置
     * 
     * @param oid
     * @return
     */
    public String getPosition(String oid) {
        JSONObject object;
        String prevCol = "", ogid = "";
        if (!StringHelper.InvaildString(oid)) {
            rMsg.netMSG(1, "无效文章id");
        }
        try {
            object = content.eq("_id", oid).field("_id,ogid,clickcount").find();
            if (object != null && object.size() > 0) {
                ogid = object.getString("ogid");
                if (StringHelper.InvaildString(ogid)) {
                    prevCol = new ContentGroup().getPrevCol(ogid);
                }
            }
        } catch (Exception e) {
            nlogger.logout(e);
            prevCol = "";
        }
        return prevCol;
    }

    /**
     * 前台页面数据显示，显示文章内容
     * 
     * @project GrapeContent
     * @package interfaceApplication
     * @file Content.java
     * 
     * @param wbid
     * @param idx
     * @param pageSize
     * @param condString
     * @return
     *
     */
    public String ShowFront(String wbid, int idx, int pageSize, String condString) {
        long total = 0;
        JSONArray array = new JSONArray();
        JSONArray condArray = new JSONArray(), condOgid = new JSONArray();
        JSONObject obj = model.buildCondOgid(condString);
        if (obj != null && obj.size() > 0) {
            condArray = obj.getJsonArray("cond");
            condOgid = obj.getJsonArray("ogid");
            if (condOgid != null && condOgid.size() > 0) {
                content.or().where(condOgid);
            }
            if (condArray != null && condArray.size() > 0) {
                content.and().where(condArray);
            }
        }
        if (content.getCondCount() > 0) {
            content.and().eq("wbid", wbid).eq("slevel", 0).eq("isdelete", 0).eq("isvisble", 0).desc("time").field("_id,mainName,image,time,content");
            array = content.dirty().page(idx, pageSize);
            total = content.count();
            content.clear();
        }
        return rMsg.netPAGE(idx, pageSize, total, model.getImgs(model.ContentDencode(array)));
    }

    /**
     * 根据内容组id显示文章
     * 
     * @param ogid
     * @return
     */
    public String ShowByGroupId(String wbid, String ogid) {
        return ShowPicByGroupId(wbid, ogid);
    }

    /*----------前台页面图片显示-------------*/
    /**
     * 根据内容组id显示文章
     * 
     * @param ogid
     * @return
     */
    @SuppressWarnings("unchecked")
    public String ShowPicByGroupId(String wbid, String ogid) {
        JSONArray array = null;
        JSONObject object = null;
        String img;
        try {
            wbid = model.getRWbid(wbid);
            ogid = getRogid(ogid); // 获取链接栏目id
            if (ogid.contains("errorcode")) {
                return ogid;
            }
            JSONArray condArray = JSONArray.toJSONArray(ogid);
            if (condArray != null && condArray.size() > 0 && StringHelper.InvaildString(wbid)) {
                array = content.or().where(condArray).and().eq("wbid", wbid).eq("slevel", 0).eq("isdelete", 0).eq("isvisble", 0).field("_id,mainName,ogid,time,image").desc("time").desc("sort").desc("_id").limit(100).select();
                array = model.getImgs(array);
                if (array != null && array.size() > 0) {
                    int l = array.size();
                    for (int i = 0; i < l; i++) {
                        object = (JSONObject) array.get(i);
                        img = object.getString("image");
                        object.put("image", (img != null && !img.equals("")) ? img.split(",")[0] : "");
                        array.set(i, object);
                    }
                }
            }
        } catch (Exception e) {
            nlogger.logout("Content.findPicByGroupID: " + e);
            array = null;
        }
        return rMsg.netMSG(true, array);
    }

    /**
     * 获取链接栏目id，添加至条件
     * 
     * @param ogid
     * @return
     */
    private String getRogid(String ogid) {
        String[] value = null;
        dbFilter filter = new dbFilter();
        ogid = new ContentGroup().getLinkOgid(ogid);
        if (!StringHelper.InvaildString(ogid)) {
            return rMsg.netMSG(1, "无效栏目id");
        }
        if (ogid.contains("errorcode")) {
            return ogid;
        }
        value = ogid.split(",");
        if (value != null) {
            for (String string : value) {
                if (StringHelper.InvaildString(string)) {
                    filter.eq("ogid", string);
                }
            }
        }
        return filter.build().toJSONString();
    }

    /*---------- 前台分页 
     * 				[显示字段：_id,mainName,time,wbid,ogid,image,readCount,souce]
     * ----------
     */
    public String Page(String wbid, int idx, int pageSize) {
        if (idx <= 0) {
            return rMsg.netMSG(false, "页码错误");
        }
        if (pageSize <= 0) {
            return rMsg.netMSG(false, "页长度错误");
        }
        JSONArray array = content.eq("isdelete", 0).eq("isvisble", 0).page(idx, pageSize);
        array = model.setTemplate(array);
        return rMsg.netPAGE(idx, pageSize, content.dirty().count(), array);
    }

    /**
     * 前台按条件分页显示文章数据
     * 
     * @param wbid
     * @param idx
     * @param pageSize
     * @param condString
     * @return
     */
    public String PageBy(String wbid, int idx, int pageSize, String condString) {
        String out = null;
        long total = 0;
        JSONArray array = new JSONArray();
        JSONArray condArray = new JSONArray(), condOgid = new JSONArray();
        if (idx <= 0) {
            return rMsg.netMSG(false, "页码错误");
        }
        if (pageSize <= 0) {
            return rMsg.netMSG(false, "页长度错误");
        }
        if (!StringHelper.InvaildString(condString)) {
            return rMsg.netMSG(1, "无效条件");
        }
        JSONObject obj = model.buildCondOgid(condString);
        // 获取所有下级栏目
        obj = getNextColumn(obj);
        if (obj != null && obj.size() > 0) {
            condArray = obj.getJsonArray("cond");
            condOgid = obj.getJsonArray("ogid");
            if (condOgid != null && condOgid.size() > 0) {
                content.or().where(condOgid);
            }
            if (condArray != null && condArray.size() > 0) {
                content.and().where(condArray);
            }
        }
        if (content.getCondCount() > 0) {
            content.and().eq("wbid", model.getRWbid(wbid)).eq("isdelete", 0).eq("isvisble", 0).eq("state", 2);
            total = content.dirty().count();
            array = content.desc("time").field("_id,mainName,time,wbid,ogid,image,clickcount,souce,contenturl").desc("time").desc("sort").desc("_id").page(idx, pageSize);
            array = model.setTemplate(array); // 设置模版
            out = rMsg.netPAGE(idx, pageSize, total, model.getImgs(model.getDefault(wbid, array)));
        } else {
            out = rMsg.netMSG(false, "无效条件");
        }
        return out;
    }

    /*---------- 后台分页  [显示所有字段]----------*/
    public String PageBack(int idx, int pageSize) {
        long total = 0;
        JSONArray array = null;
        if (idx <= 0) {
            return rMsg.netMSG(false, "页码错误");
        }
        if (pageSize <= 0) {
            return rMsg.netMSG(false, "页长度错误");
        }
        // 判断当前用户身份：系统管理员，网站管理员
        if (UserMode.root > userType && userType >= UserMode.admin) { // 判断是否是网站管理员
            content.eq("wbid", currentWeb);
        }
        total = content.dirty().count();
        array = content.desc("sort").desc("time").page(idx, pageSize);
        array = model.setTemplate(array); // 设置模版
        array = model.getImgs(model.getDefault(currentWeb, array));
        array = model.ContentDencode(array);
        array = FillFileToArray(array);
        return rMsg.netPAGE(idx, pageSize, total, array);
    }

    public String PageByBack(int idx, int pageSize, String condString) {
        long total = 0;
        JSONArray array = null;
        GrapeTreeDBModel content = getDB();
        if (idx <= 0) {
            return rMsg.netMSG(false, "页码错误");
        }
        if (pageSize <= 0) {
            return rMsg.netMSG(false, "页长度错误");
        }
        JSONArray condArray = new JSONArray(), condOgid = new JSONArray();
        // 判断当前用户身份：系统管理员，网站管理员
        if (UserMode.root > userType && userType >= UserMode.admin) { // 判断是否是网站管理员
            content.eq("wbid", currentWeb);
        }
        if (!StringHelper.InvaildString(condString)) {
            return rMsg.netMSG(1, "无效条件");
        }
        JSONObject obj = model.buildCondOgid(condString);
        if (obj != null && obj.size() > 0) {
            condArray = obj.getJsonArray("cond");
            condOgid = obj.getJsonArray("ogid");
            if (condOgid != null && condOgid.size() > 0) {
                content.or().where(condOgid);
            }
            if (condArray != null && condArray.size() > 0) {
                content.and().where(condArray);
            }
        }
        if (content.getCondCount() > 0) {
            content.and().eq("isdelete", 0).eq("isvisble", 0);
            array = content.dirty().desc("sort").desc("time").page(idx, pageSize);
            total = content.count();
            array = model.setTemplate(array); // 设置模版
            array = model.getImgs(model.getDefault(currentWeb, array));
            array = model.ContentDencode(array);
            array = FillFileToArray(array);
        }
        return rMsg.netPAGE(idx, pageSize, total, array);
    }

    /**
     * 附件填充详细文件信息
     * 
     * @project GrapeContent
     * @package model
     * @file ContentModel.java
     * 
     * @return
     *
     */
    @SuppressWarnings("unchecked")
    private JSONObject FillFileToObj(JSONObject object) {
        String attrid;
        JSONObject obj;
        if (object != null && object.size() > 0) {
            attrid = getAttr(object); // 获取附件id
            obj = getFiles(attrid); // 获取附件文件详细信息
            if (obj != null && obj.size() > 0) {
                object.put("attrid", FillInfo(attrid, obj).toJSONString());
            } else {
                object.put("attrid", new JSONArray().toJSONString());
            }
        }
        return object;
    }

    /**
     * 附件填充详细文件信息
     * 
     * @project GrapeContent
     * @package model
     * @file ContentModel.java
     * 
     * @return
     *
     */
    @SuppressWarnings("unchecked")
    private JSONArray FillFileToArray(JSONArray array) {
        String attrid;
        JSONObject obj, tempObj;
        JSONArray tempArray;
        if (array != null && array.size() > 0) {
            int l = array.size();
            attrid = getAttrID(array); // 获取附件id
            obj = getFiles(attrid); // 获取附件文件详细信息
            for (int i = 0; i < l; i++) {
                tempArray = new JSONArray();
                tempObj = (JSONObject) array.get(i);
                if (obj != null && obj.size() > 0) {
                    attrid = getAttr(tempObj);
                    tempArray = FillInfo(attrid, obj);
                }
                tempObj.put("attrid", tempArray.toJSONString());
                array.set(i, tempObj);
            }
        }
        return array;
    }

    /**
     * 获取文件信息
     * 
     * @project GrapeContent
     * @package model
     * @file ContentModel.java
     * 
     * @param attrid
     * @return
     *
     */
    private JSONObject getFiles(String attrid) {
        JSONObject object = null;
        String fileInfo;
        if (attrid != null && !attrid.equals("") && !attrid.equals("null") && !attrid.equals("0")) {
            fileInfo = (String) appsProxy.proxyCall("/GrapeFile/Files/getFileByID/" + attrid); // 查询文件信息
            object = JSONObject.toJSON(fileInfo);
        }
        return object;
    }

    /**
     * 将文章表中attrid的值更换为文件详细信息
     * 
     * @project GrapeContent
     * @package model
     * @file ContentModel.java
     * 
     * @param attrid
     * @param FileInfo
     * @return
     *
     */
    @SuppressWarnings("unchecked")
    private JSONArray FillInfo(String attrid, JSONObject FileInfo) {
        JSONArray attrArray = new JSONArray();
        JSONObject object = null;
        if (attrid != null && !attrid.equals("") && !attrid.equals("null")) {
            if (FileInfo != null && FileInfo.size() > 0) {
                String[] value = attrid.split(",");
                for (String id : value) {
                    object = new JSONObject();
                    object = JSONObject.toJSON(FileInfo.getString(id));
                    if (object != null && object.size() > 0) {
                        attrArray.add(object);
                    }
                }
            }
        }
        return attrArray;
    }

    /**
     * 显示审核文章，condString为null，显示所有的文章
     * 
     * @project GrapeContent
     * @package interfaceApplication
     * @file Content.java
     * 
     * @param idx
     * @param pageSize
     * @param condString
     *
     */
    public String ShowArticle(int idx, int pageSize, String condString) {
        String ogids = "";
        long total = 0;
        JSONArray array = null;
        dbFilter filter = new dbFilter();
        JSONArray webArray = null;
        JSONObject temp;
        JSONArray condArray = JSONArray.toJSONArray(condString);
        if (condArray != null && condArray.size() > 0) {
            // 根据栏目搜索文章，将栏目名称转换为ogid,获取当前站点及下级站点的栏目id
            temp = findByColumnName(JSONArray.toJSONArray(condString));
            if (temp != null && temp.size() > 0) {
                if (temp.containsKey("ogid")) {
                    ogids = temp.getString("ogid");
                    if (StringHelper.InvaildString(ogids)) {
                        JSONArray ogidArray = model.getOrCondArray("ogid", ogids);
                        if (ogidArray != null && ogidArray.size() > 0) {
                            content.or().where(ogidArray);
                        } else {
                            return rMsg.netMSG(1, "无效条件");
                        }
                    }
                } else { // 不包含栏目名称查询，则获取当前站点及下级站点的栏目id
                    String[] wbids = model.getWeb(currentWeb); // 获取下级站点,包含当前站点
                    if (wbids != null) {
                        for (String string : wbids) {
                            filter.eq("wbid", string);
                        }
                        webArray = filter.build(); // 生成jsonarray型查询条件[{"field":"wbid","logic":"=","value":""}]
                        if (webArray != null && webArray.size() > 0) {
                            content.or().where(webArray);
                        } else {
                            return rMsg.netMSG(1, "无效条件");
                        }
                    }
                }
                if (temp.containsKey("condArray")) {
                    JSONArray condarray = temp.getJsonArray("condArray");
                    if (condarray != null && condarray.size() > 0) {
                        content.and().where(condarray);
                    } else {
                        return rMsg.netMSG(1, "无效条件");
                    }
                }
            }
            content.eq("isdelete", 0).eq("isvisble", 0);
            array = content.dirty().desc("_id").page(idx, pageSize);
            total = content.count();
            content.clear();
            array = model.setTemplate(array); // 设置模版
            array = model.ContentDencode(array);
            model.getImgs(model.getDefault(currentWeb, array));
        } else {
            return rMsg.netMSG(1, "无效条件");
        }
        return rMsg.netPAGE(idx, pageSize, total, getColumnName(array));
    }

    /**
     * 根据栏目名称查询文章，查询条件重组,只包含当前站点及下级站点的栏目id
     * 
     * @project GrapeContent
     * @package interfaceApplication
     * @file Content.java
     * 
     * @param condArray
     * @return {"ogid"}
     *
     */
    @SuppressWarnings("unchecked")
    private JSONObject findByColumnName(JSONArray condArray) {
        JSONObject object = null, temp = new JSONObject();
        JSONArray array = null;
        String key, value = null;
        if (condArray != null && condArray.size() != 0) {
            array = new JSONArray();
            for (Object object2 : condArray) {
                object = (JSONObject) object2;
                key = object.getString("field");
                if (key.equals("columnName")) {
                    value = new ContentGroup().getOgid(object.getString("value"));
                    temp.put("ogid", value);
                } else {
                    array.add(object);
                }
            }
            temp.put("condArray", array);
        }
        return temp;
    }

    /**
     * 文章数据添加栏目字段
     * 
     * @project GrapeContent
     * @package interfaceApplication
     * @file Content.java
     * 
     * @param array
     * @return {ogid:name,ogid:name}
     *
     */
    @SuppressWarnings("unchecked")
    private JSONArray getColumnName(JSONArray array) {
        JSONObject object, objs;
        String ogid = "", temp;
        if (array != null && array.size() > 0) {
            for (Object obj : array) {
                object = (JSONObject) obj;
                temp = object.getString("ogid");
                if (temp != null && !temp.equals("") && !temp.equals("null")) {
                    ogid += temp + ",";
                }
            }
            if (ogid != null && !ogid.equals("") && !ogid.equals("null")) {
                ogid = StringHelper.fixString(ogid, ',');
            }
            temp = new ContentGroup().getColumnName(ogid);
            objs = JSONObject.toJSON(temp);
            if (objs != null && objs.size() > 0) {
                int l = array.size();
                for (int i = 0; i < l; i++) {
                    object = (JSONObject) array.get(i);
                    ogid = object.getString("ogid");
                    object.put("columnName", objs.getString(ogid));
                    array.set(i, object);
                }
            }
        }
        return array;
    }

    public String findSiteDesp(String id) {
        JSONObject object = new JSONObject();
        if (StringHelper.InvaildString(id)) {
            if (ObjectId.isValid(id) || checkHelper.isInt(id)) {
                object = content.eq("_id", id).field("_id,mainName,fatherid,ogid,time,content").find();
                object = model.ContentDencode(object);
            }
        }
        return rMsg.netMSG(true, object);
    }

    /*----------------前台搜索-----------------*/
    /**
     * 文章模糊查询,包含链接栏目文章
     * 
     * @param jsonstring
     * @return
     */
    public String SearchArticle(String wbid, int idx, int pageSize, String condString) {
        return search(wbid, idx, pageSize, condString, 1);
    }

    /*----------------后台搜索-----------------*/
    /**
     * 文章模糊查询 包含下级栏目的文章
     * 
     * @param jsonstring
     * @return
     */
    public String SearchArticleBack(int idx, int pageSize, String condString) {
        return search(currentWeb, idx, pageSize, condString, 2);
    }

    /**
     * 搜索
     * 
     * @param wbid
     * @param idx
     * @param pageSize
     * @param condString
     * @param type
     * @return
     */
    private String search(String wbid, int idx, int pageSize, String condString, int type) {
        String out = null;
        long total = 0;
        JSONArray array = new JSONArray();
        JSONArray condArray = new JSONArray(), condOgid = new JSONArray();
        JSONObject obj = model.buildCondOgid(condString);
        obj = getNextColumn(obj);
        if (obj != null && obj.size() > 0) {
            if (!StringHelper.InvaildString(wbid)) {
                rMsg.netMSG(1, "无效站点id");
            }
            condArray = obj.getJsonArray("cond");
            condOgid = obj.getJsonArray("ogid");
            if (condOgid != null && condOgid.size() > 0) {
                content.or().where(condOgid);
            }
            if (condArray != null && condArray.size() > 0) {
                content.and().where(condArray);
            }
        }
        if (content.getCondCount() > 0) {
            content.and().eq("isdelete", 0).eq("isvisble", 0).desc("time").desc("time").desc("sort").desc("_id");
            total = content.dirty().count();
            switch (type) {
            case 1: // 前台搜索
                array = content.field("_id,mainName,time,wbid,ogid,content").page(idx, pageSize);
                break;
            case 2: // 后台搜索
                array = content.page(idx, pageSize);
                array = model.setTemplate(array); // 设置模版
                array = model.getImgs(model.getDefault(wbid, array));
                array = model.ContentDencode(array);
                array = FillFileToArray(array);
                break;
            }
            out = rMsg.netPAGE(idx, pageSize, total, array);
        } else {
            out = rMsg.netMSG(false, "无效条件");
        }
        return out;
    }

    /**
     * 获取所有下级栏目
     * 
     * @param condArray
     * @return
     */
    @SuppressWarnings({ "unchecked" })
    private JSONObject getNextColumn(JSONObject obj) {
        dbFilter filter = new dbFilter();
        JSONObject tempobj;
        String value;
        String[] ogids;
        JSONArray condOgid = new JSONArray();
        if (obj != null && obj.size() > 0) {
            condOgid = obj.getJsonArray("ogid");
            if (condOgid != null && condOgid.size() > 0) { // 查询条件含有ogid
                for (Object object : condOgid) {
                    tempobj = (JSONObject) object;
                    value = new ContentGroup().getAllColumn(tempobj.getString("value"));
                    if (StringHelper.InvaildString(value)) {
                        ogids = value.split(",");
                        for (String string : ogids) {
                            filter.eq("ogid", string);
                        }
                    }
                }
            }
        }
        obj.put("ogid", filter.build());
        return obj;
    }

    /**
     * 增加文章访问量
     * 
     * @param object
     */
    private void AddArticleClick(JSONObject object) {
        String oid;
        long clicktimes = 1;
        if (object != null && object.size() > 0) {
            oid = object.getMongoID("_id");
            if (object.containsKey("clickcount")) {
                clicktimes = Long.parseLong(object.getString("clickcount")) + 1;
            }
            JSONObject obj = new JSONObject("clickcount", clicktimes);
            content.eq("_id", oid).data(obj).update();
        }
    }

    /**
     * 文章统计
     * 
     * @param rootID
     * @return
     */
    public String totalArticle(String rootID) {
        distributedLocker countLocker = distributedLocker.newLocker("totalArticle_" + rootID);
        String rString = "";
        if (countLocker != null) {
            boolean flag = countLocker.lock();
            if (flag) {// 如果锁定成功
                CacheHelper ch = new CacheHelper();
                rString = ch.get("total_COunt_" + rootID);
                if (rString == null || rString.equals("")) {
                    JSONObject json = new JSONObject();
                    JSONObject webInfo = JSONObject.toJSON((String) appsProxy.proxyCall("/GrapeWebInfo/WebInfo/getWebInfo/s:" + rootID));
                    json = new WsCount().getAllCount(json, rootID, webInfo.getString(rootID), "");
                    rString = json.toJSONString();
                    ch.setget("total_COunt_" + rootID, rString, 86400);
                }
                countLocker.unlock();
            }
        }
        return rString;
    }

    /**
     * 文章统计,包含开始时间-结束时间
     * 
     * @project GrapeContent
     * @package interfaceApplication
     * @file Content.java
     * 
     * @param rootID
     * @return
     *
     */
    public String total(String rootID, String starTime, String endTime) {
        distributedLocker countLocker = distributedLocker.newLocker("total_time_Count_" + rootID);
        String rString = "";
        if (countLocker.lock()) {
            rString = ch.get("total_time_Count_" + rootID);
            rootID = model.getRWbid(rootID);
            if (rString == null || rString.equals("")) {
                JSONObject json = new JSONObject();
                JSONObject webInfo = JSONObject.toJSON(appsProxy.proxyCall("/GrapeWebInfo/WebInfo/getWebInfo/s:" + rootID).toString());
                json = new WsCount().getAllCount(json, rootID, webInfo.getString(rootID), "", Long.parseLong(starTime), Long.parseLong(endTime));
                rString = json.toJSONString();
                ch.setget("total_time_Count_" + rootID, rString, 86400);
            }
            countLocker.unlock();
        }
        return rString;
    }

    /**
     * 统计，显示栏目
     * 
     * @project GrapeContent
     * @package interfaceApplication
     * @file Content.java
     * 
     * @param rootID
     * @param starTime
     * @param endTime
     * @return
     *
     */
    public String totalColumn(String rootID, String starTime, String endTime) {
        JSONObject json = new JSONObject();
        distributedLocker countLocker = distributedLocker.newLocker("total_column_Count" + rootID);
        String rString = "";
        if (countLocker.lock()) {
            rString = ch.get("total_column_Count_" + rootID);
            if (rString == null || rString.equals("")) {
                rootID = model.getRWbid(rootID);
                json = new WsCount().getChannleCount(rootID, Long.parseLong(starTime), Long.parseLong(endTime));
                rString = json.toJSONString();
                ch.setget("total_column_Count_" + rootID, rString, 86400);
            }
            countLocker.unlock();
        }
        return rString;
    }

    /**
     * 获取重点公开内容
     * 
     * @param condString
     * @param idx
     * @param PageSize
     * @return
     */
    public String getKeyArticle(String condString, int idx, int PageSize) {
        long total = 0;
        JSONArray array = null;
        JSONArray condArray = new JSONArray(), condOgid = new JSONArray();
        JSONObject obj = model.buildCondOgid(condString);
        if (obj != null && obj.size() > 0) {
            condArray = obj.getJsonArray("cond");
            condOgid = obj.getJsonArray("ogid");
            if (condOgid != null && condOgid.size() > 0) {
                content.or().where(condOgid);
            }
            if (condArray != null && condArray.size() > 0) {
                content.and().where(condArray);
            }
        }
        if (content.getCondCount() > 0) {
            content.desc("time").eq("slevel", 0).field("_id,mainName,ogid,time");
            array = content.dirty().page(idx, PageSize);
            total = content.count();
        }
        return rMsg.netPAGE(idx, PageSize, total, (array != null && array.size() > 0) ? array : new JSONArray());
    }

    /**
     * 获取点击量最高的公开内容
     * 
     * @param condString
     * @param idx
     * @param PageSize
     * @return
     */
    public String getHotArticle(String condString, int idx, int PageSize) {
        long total = 0;
        JSONArray array = null;
        JSONArray condArray = new JSONArray(), condOgid = new JSONArray(), condWeb = new JSONArray();
        if (!StringHelper.InvaildString(condString)) {
            return rMsg.netMSG(1, "无效条件");
        }
        JSONObject obj = buildCond(condString);
        if (obj != null && obj.size() > 0) {
            condArray = obj.getJsonArray("cond");
            condOgid = obj.getJsonArray("ogid");
            condWeb = obj.getJsonArray("wbid");
            if (condWeb != null && condWeb.size() > 0) {
                content.or().where(condWeb);
            }
            if (condOgid != null && condOgid.size() > 0) {
                content.or().where(condOgid);
            }
            if (condArray != null && condArray.size() > 0) {
                content.and().where(condArray);
            }
        }
        if (content.getCondCount() > 0) {
            content.and().eq("slevel", 0).desc("clickcount").desc("_id").field("_id,mainName,ogid,time");
            total = content.dirty().count();
            array = content.page(idx, PageSize);
        }
        return rMsg.netPAGE(idx, PageSize, total, array);
    }

    @SuppressWarnings("unchecked")
    private JSONObject buildCond(String condString) {
        JSONArray cond = JSONArray.toJSONArray(condString);
        JSONArray condWbid = new JSONArray();
        JSONObject obj = model.buildCondOgid(condString);
        String wbids = getAllWeb(cond);
        dbFilter filter = new dbFilter();
        if (StringHelper.InvaildString(wbids)) {
            String[] value = wbids.split(",");
            for (String wbid : value) {
                filter.eq("wbid", wbid);
            }
            condWbid = filter.build();
        }
        obj.put("wbid", condWbid);
        return obj;
    }

    /**
     * 获取所有站点
     * 
     * @param condArray
     * @return
     */
    private String getAllWeb(JSONArray condArray) {
        String wbids = "";
        JSONObject condObj;
        String field;
        if (condArray != null && condArray.size() != 0) {
            int l = condArray.size();
            for (int i = 0; i < l; i++) {
                condObj = (JSONObject) condArray.get(i);
                field = condObj.getString("field");
                if (field.equals("wbid")) {
                    currentWeb = condObj.getString("value");
                    break;
                }
            }
            if (StringHelper.InvaildString(currentWeb)) {
                wbids = appsProxy.proxyCall("/GrapeWebInfo/WebInfo/getWebTree/" + currentWeb).toString();
            }
        }
        return wbids;
    }

    /**
     * 文章审核通过
     * 
     * @param oid
     * @return
     */
    public String ReviewPass(String oid) {
        if (!StringHelper.InvaildString(oid)) {
            return rMsg.netMSG(false, "非法数据");
        }
        return rMsg.netState(Review(oid, 2));
    }

    /**
     * 文章审核不通过
     * 
     * @param oid
     * @return
     */
    public String ReviewNotPass(String oid) {
        if (!StringHelper.InvaildString(oid)) {
            return rMsg.netMSG(false, "非法数据");
        }
        return rMsg.netState(Review(oid, 1));
    }

    /**
     * 文章审核取消，由审核通过改为待审核状态，审核不通过改为待审核状态
     * 
     * @param oid
     * @return
     */
    public String ReviewCancle(String oid) {
        if (!StringHelper.InvaildString(oid)) {
            return rMsg.netMSG(false, "非法数据");
        }
        return rMsg.netState(Review(oid, 0));
    }

    /**
     * 文章审核操作
     * 
     * @param oid
     * @param NewState
     * @return
     */
    @SuppressWarnings("unchecked")
    private boolean Review(String oid, int NewState) {
        boolean rb = true;
        String[] value = oid.split(",");
        JSONObject object = new JSONObject();
        object.put("state", NewState);
        int l = value.length;
        if (l > 0) {
            for (String id : value) {
                content.or().eq("_id", id);
            }
            rb = content.data(object).updateAll() > 0 ? true : false;
//            if (rb) {
//                // 发送数据到kafka
//                appsProxy.proxyCall("/GrapeSendKafka/SendKafka/sendData2Kafka/" + oid + "/int:1/int:3/int:" + NewState);
//            }
        } else {
            rb = false;
        }
        return rb;
    }

    /**
     * 文章点赞
     * 
     * @project GrapeContent
     * @package interfaceApplication
     * @file Content.java
     * 
     * @param oid
     * @return
     *
     */
    public String Fabulous(String oid) {
        if (oid == null || oid.equals("") || oid.equals("null")) {
            return rMsg.netMSG(100, "参数异常");
        }
        int code = MessageArticle(oid, "fabulous") ? 0 : 99;
        return code == 0 ? rMsg.netMSG(0, "点赞成功") : rMsg.netMSG(99, "点赞失败");
    }

    /**
     * 文章倒彩
     * 
     * @project GrapeContent
     * @package interfaceApplication
     * @file Content.java
     * 
     * @param oid
     * @return
     *
     */
    public String Booing(String oid) {
        if (oid == null || oid.equals("") || oid.equals("null")) {
            return rMsg.netMSG(100, "参数异常");
        }
        int code = MessageArticle(oid, "booing") ? 0 : 99;
        return code == 0 ? rMsg.netMSG(0, "成功") : rMsg.netMSG(99, "失败");
    }

    /**
     * 点赞，倒彩操作
     * 
     * @project GrapeContent
     * @package interfaceApplication
     * @file Content.java
     * 
     * @param oid
     * @param field
     * @return
     *
     */
    private boolean MessageArticle(String oid, String field) {
        String temp = "1";
        int fabu;
        JSONObject object = null, obj = null;
        try {
            object = content.eq("_id", oid).field("_id,fabulous,booing").find();
            if (object != null && object.size() > 0) {
                if (object.containsKey(field)) {
                    temp = object.getString(field);
                    if (temp.contains("$numberLong")) {
                        temp = JSONObject.toJSON(temp).getString("$numberLong");
                    }
                }
                fabu = Integer.parseInt(temp) + 1;
                obj = new JSONObject(field, fabu);
                obj = content.eq("_id", oid).data(obj).update();
            }
        } catch (Exception e) {
            nlogger.logout(e);
            obj = null;
        }

        return obj != null;
    }

    /**
     * 设置文章含有后缀
     * 
     * @project GrapeContent
     * @package interfaceApplication
     * @file Content.java
     * 
     * @param oid
     * @return
     *
     */
    public String AddSuffix(String oid) {
        if (oid == null || oid.equals("") || oid.equals("null")) {
            return rMsg.netMSG(100, "参数异常");
        }
        int code = SetSuffix(oid, 1) ? 0 : 99;
        return code == 0 ? rMsg.netMSG(0, "成功") : rMsg.netMSG(99, "失败");
    }

    /**
     * 设置文章不含有后缀
     * 
     * @project GrapeContent
     * @package interfaceApplication
     * @file Content.java
     * 
     * @param oid
     * @return
     *
     */
    public String RemoveSuffix(String oid) {
        if (oid == null || oid.equals("") || oid.equals("null")) {
            return rMsg.netMSG(100, "参数异常");
        }
        int code = SetSuffix(oid, 0) ? 0 : 99;
        return code == 0 ? rMsg.netMSG(0, "成功") : rMsg.netMSG(99, "失败");
    }

    /**
     * 设置文章后缀
     * 
     * @project GrapeContent
     * @package interfaceApplication
     * @file Content.java
     * 
     * @param oid
     * @param isSuffix
     * @return
     *
     */
    private boolean SetSuffix(String oid, int isSuffix) {
        JSONObject object = new JSONObject("isSuffix", isSuffix);
        object = content.eq("_id", oid).data(object).update();
        return object != null;
    }

    /**
     * 追加内容数据
     * 
     * @project GrapeContent
     * @package interfaceApplication
     * @file Content.java
     * 
     * @param id
     * @param info
     * @return
     *
     */
    @SuppressWarnings("unchecked")
    public String AddAppend(String id, String info) {
        int code = 99;
        String result = rMsg.netMSG(100, "追加文档失败");
        String contents = "", oldcontent;
        JSONObject object = content.eq("_id", id).find();
        JSONObject obj = JSONObject.toJSON(info);
        if (obj != null && obj.size() != 0) {
            if (obj.containsKey("content")) {
                contents = obj.getString("content");
                contents = codec.DecodeHtmlTag(contents);
                contents = codec.decodebase64(contents);
                obj.escapeHtmlPut("content", contents);
                oldcontent = object.getString("content");
                oldcontent += obj.getString("content");
                obj.put("content", oldcontent);
            }
            code = content.eq("_id", id).data(obj).update() != null ? 0 : 99;
        }
        return code == 0 ? rMsg.netMSG(0, "追加文档成功") : result;
    }

    /**
     * 批量查询文章，针对于推送文章到微信
     * 
     * @project GrapeContent
     * @package interfaceApplication
     * @file Content.java
     * 
     * @param ids
     * @return
     *
     */
    public String FindWechatArticle(String ids) {
        JSONArray array = null;
        String[] value = null;
        if (ids != null && !ids.equals("") && !ids.equals("null")) {
            value = ids.split(",");
        }
        if (value != null) {
            content.or();
            for (String string : value) {
                content.eq("_id", string);
            }
            array = content.field("_id,mainName,author,content,desp,image,wbid").limit(8).select();
        }
        if (array != null && array.size() > 0) {
            JSONObject object = (JSONObject) array.get(0);
            String wbid = object.getString("wbid");
            array = model.ContentDencode(array);
            array = model.getDefaultImage(wbid, array);
            array = model.getImgs(array);
        }
        return (array != null && array.size() > 0) ? array.toString() : new JSONArray().toJSONString();
    }

    /**
     * 获取当前网站的下级站点的所有文章【提供状态显示】
     * 
     * @project GrapeContent
     * @package interfaceApplication
     * @file Content.java
     * 
     *
     */
    private String[] getAllContent(String wbid) {
        String wbids = "";
        String[] value = null;
        if (wbid != null && !wbid.equals("")) {
            wbids = appsProxy.proxyCall("/GrapeWebInfo/WebInfo/getWebTree/" + model.getRWbid(wbid)).toString();
            if (wbids != null && !wbids.equals("")) {
                value = wbids.split(",");
            }
        }
        return value;
    }

    /**
     * 查询文章信息
     * 
     * @param obj
     * @param id
     * @return
     */
    @SuppressWarnings("unchecked")
    private String getSingleArticle(JSONObject obj, String id) {
        String wbid;
        String ogid = "";
        JSONObject preobj;
        JSONObject nextobj;
        // 从当前数据获取wbid,ogid
        if (obj != null && obj.size() != 0) {
            if (!obj.containsKey("clickcount")) {
                obj.put("clickcount", 0);
            }
            wbid = obj.getString("wbid");
            ogid = obj.getString("ogid");
            // 获取上一篇
            preobj = content.asc("_id").eq("wbid", wbid).eq("ogid", ogid).eq("slevel", 0).gt("_id", id).field("_id,mainName").limit(1).find();
            nextobj = content.desc("_id").eq("wbid", wbid).eq("ogid", ogid).eq("slevel", 0).lt("_id", id).limit(1).field("_id,mainName").find();
            if (preobj != null && preobj.size() != 0) {
                obj.put("previd", preobj.getMongoID("_id"));
                obj.put("prevname", preobj.get("mainName"));
            }
            if (nextobj != null && nextobj.size() != 0) {
                obj.put("nextid", nextobj.getMongoID("_id"));
                obj.put("nextname", nextobj.get("mainName"));
            }
            // 点击次数+1
            AddArticleClick(obj);
            // 增加访问用户记录
            new ContentRecord().AddReader(id);
        }
        obj = model.ContentDencode(obj);
        obj = model.getImgs(model.getDefault(obj));
        obj = FillFileToObj(obj);
        return rMsg.netMSG(true, obj);
    }

    /**
     * 判断该文章是否可以被查看
     * 
     * @param object
     * @return 1:无权查看；2：未登录；0：显示文章信息
     */
    private int isShow(JSONObject object) {
        int code = -1;
        String currentId = "", wbid = "";
        if (object != null && object.size() != 0) {
            String ogid = object.getString("ogid");
            wbid = object.getString("wbid");
            String temp = new ContentGroup().isPublic(ogid);
            if (!temp.equals("0")) {
                if (userInfo != null && userInfo.size() != 0) {
                    // 获取当前站点的全部下级站点
                    currentId = getCurrentId();
                    // currentId = getCWbid(currentWeb);
                    wbid = model.getRWbid(wbid);
                    if (!currentId.contains(wbid)) {
                        code = 1; // 无权查看
                    } else {
                        code = 0;
                    }
                } else {
                    code = 2; // 请登录查看
                }
            } else {
                code = 0;
            }
        }
        return code;
    }

    /**
     * 获取该用户所属的所有网站id
     * 
     * @return
     */
    private String getCurrentId() {
        JSONArray array;
        JSONObject object;
        String Currentwbid = "", wbid;
        if (userInfo != null && userInfo.size() != 0) {
            array = JSONArray.toJSONArray(userInfo.getString("webinfo"));
            if (array != null && array.size() != 0) {
                for (Object object2 : array) {
                    object = (JSONObject) object2;
                    wbid = object.getString("wbid");
                    Currentwbid += wbid + ",";
                }
            }
        }
        return StringHelper.fixString(Currentwbid, ',');
    }

    /**
     * 查询文章信息
     * 
     * @param oid
     *            文章id
     * @return
     */
    private synchronized JSONObject findOid(String oid) {
        JSONObject object = content.eq("_id", oid).find();
        object = model.ContentDencode(object);
        object = model.getImgs(object);
        object = model.getDefault(object);
        object = FillFileToObj(object);
        return object;
    }

    /**
     * 获取文章组附件组id
     * 
     * @project GrapeContent
     * @package model
     * @file ContentModel.java
     * 
     * @param array
     * @return
     *
     */
    private String getAttrID(JSONArray array) {
        JSONObject object;
        String attrid = "", temp;
        if (array != null && array.size() > 0) {
            for (Object obj : array) {
                object = (JSONObject) obj;
                temp = getAttr(object);
                if (temp != null && !temp.equals("") && !temp.equals("null") && !temp.equals("0")) {
                    attrid += getAttr(object) + ",";
                }
            }
        }
        return StringHelper.fixString(attrid, ',');
    }

    /**
     * 获取文章组附件组id
     * 
     * @project GrapeContent
     * @package model
     * @file ContentModel.java
     * 
     * @param array
     * @return
     *
     */
    private String getAttr(JSONObject object) {
        String attrid = "";
        if (object != null && object.size() > 0) {
            if (object.containsKey("attrid")) {
                attrid = object.getString("attrid");
                if (attrid.contains("numberLong")) {
                    attrid = JSONObject.toJSON(attrid).getString("numberLong");
                }
            }
        }
        return attrid;
    }

    /**
     * 验证待添加文章数据内容的合法性
     * 
     * @project GrapeContent
     * @package model
     * @file ContentModel.java
     * 
     * @param content
     * @return
     *
     */
    private String checkparam(JSONObject contentInfo) {
        String mainName = "", fatherid, contents = "";
        if (contentInfo == null || contentInfo.size() <= 0) {
            return rMsg.netMSG(100, "无效数据");
        }
        contentInfo = model.ContentEncode(contentInfo);
        if (contentInfo.containsKey("mainName")) {
            mainName = contentInfo.getString("mainName");
            if (mainName.equals("")) {
                return rMsg.netMSG(2, "请填写正确的文章标题");
            }
        }
        if (contentInfo.containsKey("fatherid")) {
            fatherid = contentInfo.getString("fatherid");
            if (StringHelper.InvaildString(fatherid) && !fatherid.equals("0")) {
                contentInfo.remove("ogid");
            }
        }
        // if (contentInfo.containsKey("content")) {
        // contents = contentInfo.getString("content");
        // if (StringHelper.InvaildString(contents) &&
        // checkHelper.IsUrl(contents)) {
        // //huo
        // }
        // }
        if (contentInfo.containsKey("_id")) {
            contentInfo.remove("_id");
        }
        return contentInfo.toJSONString();
    }

    /**
     * 获取网页缩略图
     * 
     * @param contents
     * @return
     */
    private String getNetImage(String contents) {

        String path = "";
        try {
            PhantomJS pjs = new PhantomJS();

        } catch (Exception e) {
            nlogger.logout(e);
            path = "";
        }
        return path;
    }

    /**
     * 分页获取当前用户所能查询的所有文章信息
     * 
     * @param idx
     * @param pageSize
     * @return
     */
    public String getArticleInfo(int idx, int pageSize) {
        JSONArray array = null;
        long total = 0;
        JSONArray condArray = getCondString();
        if (condArray != null && condArray.size() > 0) {
            content.or().where(condArray);
            total = content.and().eq("isdelete", 0).eq("isvisble", 0).dirty().count();
            array = content.page(idx, pageSize);
        }
        array = model.ContentDencode(array);
        array = model.getImgs(array);
        return rMsg.netPAGE(idx, pageSize, total, array);
    }

    /**
     * 检查全部内容信息是否包含隐私内容
     * 
     * @param perfix
     *            URL前缀
     * @return 事件唯一ID
     */
    @SuppressWarnings("unchecked")
    public String checkAllArticle(String perfix) {
        String _eventName = StringHelper.numUUID() + "_" + StringHelper.shortUUID();
        JSONArray condArray = getCondString();
        boolean rb = false;
        if (condArray != null && condArray.size() > 0) {
            GrapeTreeDBModel content = getDB();
            appIns context = appsProxy.getCurrentAppInfo();
            (new Thread(() -> {
                appsProxy.setCurrentAppInfo(context);
                String eventName = _eventName;
                CacheHelper cache = new CacheHelper();
                JSONArray rArray = content.or().where(condArray).and().eq("state", 2).eq("isdelete", 0).eq("isvisble", 0).scan((_array) -> {
                    String perfixs = codec.DecodeFastJSON(perfix);
                    JSONArray array = model.ContentDencode(_array);
                    JSONObject json, rJson;
                    JSONArray _rArray = new JSONArray();
                    if (array != null && array.size() > 0) {
                        for (Object object : array) {
                            json = (JSONObject) object;
                            System.out.println(json.getString("_id"));
                            privacyPolicy pp = new privacyPolicy();
                            String string = pp.scanText(Jsoup.parse(json.getString("content")).text());
                            System.out.println("....ok");
                            if (string != null) {
                                if (pp.hasPrivacyPolicy()) {
                                    rJson = new JSONObject();
                                    rJson.put("_id", perfixs + json.getString("_id"));
                                    rJson.put("title", json.get("mainName"));
                                    _rArray.add(rJson);
                                }
                            }
                            cache.setget(eventName, rMsg.netMSG(false, perfixs + json.getString("_id")), delay);// 写入当前任务进程
                        }
                    }
                    return _rArray;
                }, 500);
                cache.setget(eventName, rMsg.netMSG(true, rArray), delay);// 完成任务
            })).start();
            rb = true;
        }
        return rMsg.netMSG(true, rb ? _eventName : "");
    }

    /**
     * 获得事件当前处理进度
     * 
     * @param eventName
     *            事件ID
     * @return 事件当前处理的内容
     */
    public String getEventProgress(String eventName) {
        CacheHelper cache = new CacheHelper();
        System.out.println(cache.get(eventName));
        JSONObject rJson = JSONObject.toJSON(cache.get(eventName));
        String content = "";
        if (rJson != null && rJson.containsKey("errorcode") && rJson.getInt("errorcode") == 1) {
            content = rJson.getString("message");
        }
        return rMsg.netMSG(true, content);
    }

    /**
     * 获得事件结果报告
     * 
     * @param eventName
     *            事件名称
     * @return 报告EXCEL或者错误信息
     */
    public Object getEventReport(String eventName, String file) {
        CacheHelper cache = new CacheHelper();
        JSONObject rJson = JSONObject.toJSON(cache.get(eventName));
        String content = "";
        if (rJson != null && rJson.containsKey("errorcode") && rJson.getInt("errorcode") == 0) {
            content = rJson.getString("message");
            if (content != null) {
                try {
                    return excelHelper.out(content);
                } catch (IOException e) {
                    nlogger.login(content);
                    nlogger.logout(e, "导出异常");
                    e.printStackTrace();
                }
            }
        }
        return rMsg.netMSG(false, "error");
    }

    private JSONArray getCondString() {
        JSONArray condArray = null;
        String[] value = null;
        dbFilter filter = new dbFilter();
        String wbids = getCurrentId(); // 获取当前用户所能管辖的所有站点id
        if (StringHelper.InvaildString(wbids)) {
            value = wbids.split(",");
            if (value != null) {
                for (String wbid : value) {
                    if (StringHelper.InvaildString(wbid)) {
                        if (ObjectId.isValid(wbid) || checkHelper.isInt(wbid)) {
                            filter.eq("wbid", wbid);
                        }
                    }
                }
            }
            condArray = filter.build();
        }
        return condArray;
    }
}
