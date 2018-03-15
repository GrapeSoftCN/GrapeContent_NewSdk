package interfaceApplication;

import org.json.simple.JSONObject;

import common.java.JGrapeSystem.rMsg;
import common.java.apps.appsProxy;
import common.java.interfaceModel.GrapeDBSpecField;
import common.java.interfaceModel.GrapeTreeDBModel;
import common.java.session.session;
import common.java.string.StringHelper;
import common.java.time.TimeHelper;

/**
 * 文章收藏
 * 
 * 
 *
 */
public class ContentCollect {
	private GrapeTreeDBModel _content;
	private GrapeDBSpecField _gDbSpecField;
	private session se;
	private JSONObject userInfo = null;
	private String currentUserID = null;

	public ContentCollect() {
		_content = new GrapeTreeDBModel();
		_gDbSpecField = new GrapeDBSpecField();
		_gDbSpecField.importDescription(appsProxy.tableConfig("ContentCollect"));
		_content.descriptionModel(_gDbSpecField);
		_content.bindApp();

		se = new session();
		userInfo = se.getDatas();
		if (userInfo != null && userInfo.size() > 0) {
			currentUserID = userInfo.getString("_id");
		}
	}

	/**
	 * 收藏文章
	 * 
	 * @param oid
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public String Collect(String oid) {
		Object info = null;
		String result = rMsg.netMSG(100, "收藏失败");
		JSONObject obj = new JSONObject();
		if (!StringHelper.InvaildString(currentUserID)) {
			return rMsg.netMSG(1, "请先登录再进行收藏");
		}
		if (!StringHelper.InvaildString(oid)) {
			return rMsg.netMSG(1, "无效文章id");
		}
		obj.put("userid", currentUserID); // 用户id
		obj.put("oid", oid); // 文章id
		obj.put("time", TimeHelper.nowMillis()); // 收藏时间
		if (obj != null && obj.size() > 0) {
			info = _content.data(obj).autoComplete().insertOnce();
		}
		return (info != null) ? rMsg.netMSG(0, "收藏成功") : result;
	}

	/**
	 * 取消收藏
	 * 
	 * @param oid
	 * @return
	 */
	public String CancelCollect(String oid) {
		int code = -1;
		String result = rMsg.netMSG(100, "取消失败");
		if (!StringHelper.InvaildString(currentUserID)) {
			return rMsg.netMSG(1, "请先登录");
		}
		if (!StringHelper.InvaildString(oid)) {
			return rMsg.netMSG(1, "无效文章id");
		}
		_content.eq("oid", oid).eq("userid", currentUserID);
		if (_content.getCondCount() > 0) {
			// if (_content.nullCondition()==false) {
			code = _content.delete() != null ? 0 : 100;
		}
		return (code == 0) ? rMsg.netMSG(0, "取消收藏") : result;
	}
}
