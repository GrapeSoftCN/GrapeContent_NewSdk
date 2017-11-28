package interfaceApplication;

import org.json.simple.JSONArray;

import JGrapeSystem.rMsg;

/**
 * 文章内容扫描 是否含有手机号，身份证号，银行卡号
 * 
 *
 */
public class ContentScan {

    public ContentScan() {
    }

    /**
     * 获取隐私信息列表
     * 
     * @param idx
     * @param pageSize
     * @return
     */
    public String getPrivacyInfo(int idx, int pageSize) {
        JSONArray array = null;
        long total = 0;
        return rMsg.netPAGE(idx, pageSize, total, array);
    }

    /**
     * 获取含有隐私信息的文章的详细信息
     * 
     * @param idx
     * @param pageSize
     * @return
     */
    public String getPrivacyDetailedInfo(String PContentID) {

        return null;
    }

}
