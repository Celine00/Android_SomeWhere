package crystal.somewhere.bean;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import cn.bmob.v3.BmobObject;
import cn.bmob.v3.datatype.BmobFile;
import crystal.somewhere.utils.StringUtils;

/**
 * Created by Crystal on 2017/12/23.
 */

public class Place extends BmobObject implements Serializable {
    private String creator;
    private double latitude;
    private double longitude;
    private String name;
    private String description;
    private String content;
    private List<String> tags;
    private String type;
    private String coverPageLocalPath;
    private BmobFile coverPage;
    private List<BmobFile> pictures;

    private boolean ispublic;
    private List<String> user;
    private List<String> comments;
    private int likes;
    private boolean isLiked;

    public Place() {
    }

    public Place(String creator, double latitude, double longitude, String type, String name, String description) {
        this.creator = creator;
        this.latitude = latitude;
        this.longitude = longitude;
        this.type = type;
        this.name = name;
        this.description = description;
    }

    public Place(double la, double lo, String t, String n, String d, String c, String co) {
        latitude = la;
        longitude = lo;
        name = n;
        description = d;
        content = c;
        tags = StringUtils.splitTypeToTags(t);
        coverPageLocalPath = co;
    }

    public ArrayList<Double> getLocation() {
        ArrayList<Double> location = new ArrayList<>();
        location.set(0, latitude);
        location.set(1, longitude);
        return location;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public String getCreator() {
        return creator;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getContent() {
        return content;
    }

    public List<String> getTags() {
        return tags;
    }

    public List<BmobFile> getPicture() {
        return pictures;
    }

    public String getType() {
        return type;
    }

    public BmobFile getCoverPage() {
        return coverPage;
    }

    public boolean isPublic(){return ispublic;}

    public List<String> getUser(){return user;}

    public List<String> getComments(){return comments;}

    public int getLikes(){return likes;}

    public boolean isliked(){return isLiked;}

    public String getCoverPageLocalPath() {
        return coverPageLocalPath;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public void setTags(List<String> t) {
        tags.clear();
        for(int i = 0; i < t.size(); ++i)
            tags.add(t.get(i));
    }

    public void setCoverPageLocalPath(String coverPageLocalPath) {
        this.coverPageLocalPath = coverPageLocalPath;
    }

    public void setCoverPage(BmobFile coverPage) {
        this.coverPage = coverPage;
    }

    public void setPictures(List<BmobFile> pictures) {
        this.pictures = pictures;
    }

    public void setIspublic(boolean ispublic) {this.ispublic = ispublic;}
    public void setCommentor(List<String> commentor) {this.user = commentor;}
    public void setComment(List<String> comment) {this.comments = comment;}
    public void setLikes(int likes) {this.likes = likes;}
    public void setIsliked (boolean isliked) {this.isLiked = isliked;}
}