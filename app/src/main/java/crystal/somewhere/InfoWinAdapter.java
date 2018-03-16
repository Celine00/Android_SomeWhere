package crystal.somewhere;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.amap.api.maps.AMap;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.Marker;


/**
 * Created by Crystal on 2017/12/18.
 */

public class InfoWinAdapter implements AMap.InfoWindowAdapter{
    private Context mContext;
    private LatLng latLng;
    private String title, snippet;
    private TextView nameText, descriptionText;

    public InfoWinAdapter(Context context) {
        mContext = context;
    }

    @Override
    public View getInfoWindow(Marker marker) {
        initData(marker);
        View view = initView();
        return view;
    }
    @Override
    public View getInfoContents(Marker marker) {
        return null;
    }

    private void initData(Marker marker) {
        latLng = marker.getPosition();
        title = marker.getTitle();
        snippet = marker.getSnippet();
    }

    @NonNull
    private View initView() {
        View view = LayoutInflater.from(mContext).inflate(R.layout.inforwindow, null);
        nameText = view.findViewById(R.id.name_text);
        descriptionText = view.findViewById(R.id.description_text);
        nameText.setText(title);
        descriptionText.setText(snippet);
        return view;
    }
}
