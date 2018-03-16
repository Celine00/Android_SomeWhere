package crystal.somewhere.bean;

import java.util.List;

import cn.bmob.v3.BmobUser;
import cn.bmob.v3.datatype.BmobFile;

/**
 * Created by Celine on 2017/12/26.
 */

public class User extends BmobUser {
    private BmobFile profile;
    private String picName;
    private List<String> tag;
    private List<String> favplaceid;

    public void setTag(List<String> tag_temp) {tag = tag_temp;}

    public List<String> getTag() {return tag;}

    public void setProfile(BmobFile profile) {
        this.profile = profile;
    }

    public BmobFile getProfile() {
        return profile;
    }

    public void setPicName(String picName) {
        this.picName = picName;
    }

    public String getPicName() {
        return  picName;
    }

    public void setFavplaceid(List<String> favplaceid) {
        this.favplaceid = favplaceid;
    }

    public List<String> getFavplaceid(){
        return favplaceid;
    }

}
