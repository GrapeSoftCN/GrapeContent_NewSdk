package interfaceApplication;

import org.json.simple.JSONObject;

import JGrapeSystem.rMsg;
import apps.appsProxy;
import interfaceModel.GrapeDBSpecField;
import interfaceModel.GrapeTreeDBModel;
import string.StringHelper;
import time.TimeHelper;

public class ContentError {
    private GrapeTreeDBModel content;
    private GrapeDBSpecField _gDbSpecField;

    public ContentError() {
        content = new GrapeTreeDBModel();
        _gDbSpecField = new GrapeDBSpecField();
        _gDbSpecField.importDescription(appsProxy.tableConfig("contentError"));
        content.descriptionModel(_gDbSpecField);
        content.bindApp();
    }

    /**
     * 新增错误信息
     * 
     * @param contents
     * @return
     */
    @SuppressWarnings("unchecked")
    public String insert(String contents, String oid) {
        String info = null;
        JSONObject object = new JSONObject();
        object.put("oid", oid);
        object.put("errorContent", contents);
        object.put("createtime", TimeHelper.nowMillis());
        String _id = find(oid);
        if (!StringHelper.InvaildString(_id)) {
            info = (String) content.data(object).autoComplete().insertOnce();
        } else {
            update(_id, object);
            info = _id;
        }
        return info;
    }

    private String find(String oid) {
        String _id = null;
        JSONObject object = content.eq("oid", oid).field("_id").find();
        if (object != null && object.size() > 0) {
            _id = object.getString("_id");
        }
        return _id;
    }

    /**
     * 修改文章id
     * 
     * @param contents
     * @return
     */
    public String update(String cid, JSONObject obj) {
        String result = rMsg.netMSG(1, "添加文章id失败");
        if (StringHelper.InvaildString(cid)) {
            obj = content.eq("_id", cid).data(obj).update();
        }
        return (obj != null) ? rMsg.netMSG(0, "") : result;
    }

    /**
     * 删除错误文章id
     * 
     * @param contents
     * @return
     */
    public String delete(String id) {
        JSONObject object = null;
        String result = rMsg.netMSG(1, "删除失败");
        String _id = find(id);
        if (!StringHelper.InvaildString(_id)) {
            object = content.eq("_id", _id).delete();
        }
        return (object != null) ? rMsg.netMSG(0, "删除成功") : result;
    }

    /**
     * 文章存在，返回_id
     * 
     * @param oid
     * @return
     */
    public String get(String oid) {
        JSONObject object = content.eq("oid", oid).find();
        return rMsg.netMSG(true, object);
    }
}
