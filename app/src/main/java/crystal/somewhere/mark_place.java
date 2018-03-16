package crystal.somewhere;

import android.graphics.Bitmap;

import java.util.ArrayList;

/**
 * Created by Crystal on 2017/12/23.
 */

public class mark_place {
    private double latitude;
    private double longitude;
    private String name;
    private String description;
    private ArrayList<String> tag;
    private ArrayList<Bitmap> picture;

    public mark_place(double latitude, double longitude, String name, String description) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.name = name;
        this.description = description;
    }

    public mark_place(double latitude, double longitude, String name, String description, ArrayList<String> tag, ArrayList<Bitmap> picture) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.name = name;
        this.description = description;
        this.tag = new ArrayList<>(tag);
        this.picture = new ArrayList<>(picture);
    }

    ArrayList<Double> getLocation() {
        ArrayList<Double> location = new ArrayList<>();
        location.set(0, latitude);
        location.set(1, longitude);
        return location;
    }

    String getName() {
        return name;
    }

    String getDescription() {
        return description;
    }

    ArrayList<String> getTag() {
        return tag;
    }

    ArrayList<Bitmap> getPicture() {
        return picture;
    }
}
