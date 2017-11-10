package interfaceApplication;

import org.json.simple.JSONObject;

import JGrapeSystem.rMsg;
import Model.CommonModel;
import apps.appsProxy;
import interfaceModel.GrapeDBSpecField;
import interfaceModel.GrapeTreeDBModel;
import session.session;

public class ContentRecord {
    private GrapeTreeDBModel Record;
    private GrapeDBSpecField gDbSpecField;
    private CommonModel model;
    private session se;
    private JSONObject userInfo = null;
    private String currentUser = null;

    public ContentRecord() {
        model = new CommonModel();

        Record = new GrapeTreeDBModel();
        gDbSpecField = new GrapeDBSpecField();
        gDbSpecField.importDescription(appsProxy.tableConfig("ContentRecord"));
        Record.descriptionModel(gDbSpecField);
        Record.bindApp();

        se = new session();
        userInfo = se.getDatas();
        if (userInfo != null && userInfo.size() != 0) {
            currentUser = userInfo.getMongoID("_id"); // 当前用户id
        }
    }

    // 访问记录
    @SuppressWarnings("unchecked")
    public String AddReader(String oid) {
        Object tip = 99;
        JSONObject info = new JSONObject();
        String uid = "";
        info.put("uid", uid); // 用户id
        info.put("oid", oid); // 文章id
        tip = Record.data(info).insertOnce();
        return rMsg.netMSG(tip!=null, "新增成功");
    }
}
