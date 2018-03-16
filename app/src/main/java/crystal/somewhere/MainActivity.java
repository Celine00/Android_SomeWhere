package crystal.somewhere;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.Point;
import android.os.Environment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;
import com.amap.api.maps.AMap;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.LocationSource;
import com.amap.api.maps.MapView;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.Marker;
import com.amap.api.maps.model.MarkerOptions;
import com.amap.api.maps.model.MyLocationStyle;
import com.karumi.expandableselector.ExpandableItem;
import com.karumi.expandableselector.ExpandableSelector;
import com.karumi.expandableselector.ExpandableSelectorListener;
import com.karumi.expandableselector.OnExpandableItemClickListener;
import com.oguzdev.circularfloatingactionmenu.library.FloatingActionButton;
import com.oguzdev.circularfloatingactionmenu.library.FloatingActionMenu;
import com.oguzdev.circularfloatingactionmenu.library.SubActionButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import cn.bmob.v3.Bmob;
import cn.bmob.v3.BmobUser;
import cn.bmob.v3.listener.SaveListener;
import crystal.somewhere.bean.Place;
import crystal.somewhere.bean.User;


public class MainActivity extends AppCompatActivity implements AMap.OnMapClickListener, AMap.OnMarkerClickListener, AMap.OnInfoWindowClickListener, LocationSource, AMapLocationListener {
    private MapView mMapView = null;    //地图控件
    private AMap aMap = null;      //地图对象
    private AMapLocationClient mLocationClient = null;      //定位发起端
    private AMapLocationClientOption mLocationOption = null;       //定位参数
    private OnLocationChangedListener mListener = null;     //定位监听器
    private double latitude = 0d, longitude = 0d;
    private boolean isFirstLoc = true;  //是否只显示一次定位信息和用户重新定位
    private Marker curShowWindowMarker;
    private InfoWinAdapter adapter;

    private double mLocationLat, mLocationLon;      //自身位置的经纬度
    private String mLocationAddr;       //自身位置的地址

    private DataBaseHelper mDataBaseHelper;
    private SQLiteDatabase database;

    private EditText name_edit, description_edit;

    private MoveImageView food, trip, mall, other;
    private ImageView menu, itemIcon1, itemIcon2, itemIcon3;
    private FloatingActionButton actionButton;
    private FloatingActionMenu actionMenu;
    private RadioGroup type_radio;
    private RadioButton food_button, trip_button, mall_button, other_button;
    private ExpandableSelector iconsExpandableSelector;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // 初始化 Bmob SDK
        Bmob.initialize(this, "9020a320ba34abf2203135dad5de9727");

        initView();
        initLocation();
        initFloatingMenu();
        initializeExpandableSelector();

        //SQLite数据库
        mDataBaseHelper = new DataBaseHelper(this);
        database = mDataBaseHelper.getReadableDatabase();

        //显示地图
        mMapView.onCreate(savedInstanceState);

        //获取地图对象
        if (aMap == null) {
            aMap = mMapView.getMap();
        }

        //显示定位按钮，隐藏缩放控件
        aMap.getUiSettings().setMyLocationButtonEnabled(true);
        aMap.setLocationSource(this);
        aMap.setMyLocationEnabled(true);
        aMap.getUiSettings().setZoomControlsEnabled(false);

        //自定义style
        String root = Environment.getExternalStorageDirectory().toString();
        aMap.setCustomMapStylePath(root+"/custom_config/style.data");
        aMap.setMapCustomEnable(true);

        //aMap绑定监听器
        aMap.setOnMapClickListener(this);
        aMap.setOnMarkerClickListener(this);
        aMap.setOnInfoWindowClickListener(this);

        //自定义marker的信息框infoWindow
        adapter = new InfoWinAdapter(this);
        aMap.setInfoWindowAdapter(adapter);

        //设置定位的小图标,默认蓝点
        MyLocationStyle myLocationStyle = new MyLocationStyle();
        myLocationStyle.myLocationIcon(BitmapDescriptorFactory.fromResource(R.drawable.location));
        aMap.setMyLocationStyle(myLocationStyle);

        //加载当前用户的数据库所有marker
        User user = BmobUser.getCurrentUser(MainActivity.this, User.class);
        String userName = user.getUsername();
        List<Map<String, Object>> result = mDataBaseHelper.queryName(userName);
        if (result != null && result.size() > 0) {
            for (int i = 0; i < result.size(); i++) {
                String lat = result.get(i).get("latitude").toString();
                Double lat1 = Double.parseDouble(lat);
                String lon = result.get(i).get("longitude").toString();
                Double lon1 = Double.parseDouble(lon);
                String type = result.get(i).get("type").toString();
                String name = result.get(i).get("name").toString();
                String description = result.get(i).get("description").toString();
                MarkerOptions marker = new MarkerOptions();
                if (TextUtils.equals(type, "food")) {
                    marker.icon(BitmapDescriptorFactory.fromResource(R.drawable.food_big));
                }
                else if (TextUtils.equals(type, "trip")) {
                    marker.icon(BitmapDescriptorFactory.fromResource(R.drawable.trip_big));
                }
                else if (TextUtils.equals(type, "mall")) {
                    marker.icon(BitmapDescriptorFactory.fromResource(R.drawable.mall_big));
                }
                else if (TextUtils.equals(type, "other")) {
                    marker.icon(BitmapDescriptorFactory.fromResource(R.drawable.other_big));
                }
                marker.position(new LatLng(lat1, lon1)).title(name).snippet(description);
                marker.draggable(false);        //marker不可拖动
                if (aMap != null) {
                    aMap.addMarker(marker);
                }
            }
        }

        addMarker();
    }

    //初始化控件
    private void initView() {
        mMapView = (MapView) findViewById(R.id.Map);

        food = (MoveImageView) findViewById(R.id.food);
        trip = (MoveImageView) findViewById(R.id.trip);
        mall = (MoveImageView) findViewById(R.id.mall);
        other = (MoveImageView) findViewById(R.id.other);
    }

    //初始化悬浮菜单
    private void initFloatingMenu() {
        //main menu icon
        menu = new ImageView(this);
        menu.setImageResource(R.drawable.star);
        actionButton = new FloatingActionButton.Builder(this)
                .setContentView(menu)
                .setLayoutParams(new FloatingActionButton.LayoutParams(200, 200))
                .build();
       //actionButton.setBackgroundTintList(ColorStateList.valueOf(Color.rgb(240, 240, 240)));

        SubActionButton.Builder itemBuilder = new SubActionButton.Builder(this);
        int subMenuPadding = 20;
        //sub menu icon--发现
        itemIcon1 = new ImageView(this);
        itemIcon1.setPadding(subMenuPadding, subMenuPadding, subMenuPadding, subMenuPadding);
        itemIcon1.setImageResource(R.drawable.find);
        SubActionButton find = itemBuilder.setContentView(itemIcon1)
                .setLayoutParams(new FloatingActionButton.LayoutParams(150, 150))
                .build();
        //find.setBackgroundTintList(ColorStateList.valueOf(Color.rgb(240, 240, 240)));

        //sub menu icon--添加
        itemIcon2 = new ImageView(this);
        itemIcon1.setPadding(subMenuPadding, subMenuPadding, subMenuPadding, subMenuPadding);
        itemIcon2.setImageResource(R.drawable.setting);
        SubActionButton setting = itemBuilder.setContentView(itemIcon2)
                .setLayoutParams(new FloatingActionButton.LayoutParams(150, 150))
                .build();
        //setting.setBackgroundTintList(ColorStateList.valueOf(Color.rgb(240, 240, 240)));

        //sub menu icon--搜索
        itemIcon3= new ImageView(this);
        itemIcon3.setImageResource(R.drawable.search);
        final SubActionButton search = itemBuilder.setContentView(itemIcon3)
                .setLayoutParams(new FloatingActionButton.LayoutParams(150, 150))
                .build();
        //search.setBackgroundTintList(ColorStateList.valueOf(Color.rgb(240, 240, 240)));

        actionMenu = new FloatingActionMenu.Builder(this)
                .addSubActionView(find)
                .addSubActionView(setting)
                .addSubActionView(search)
                .attachTo(actionButton)
                .build();

        //跳转到发现模式
        find.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, ExploreActivity.class);
                startActivity(intent);
            }
        });

        //跳转用户信息设置
        setting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, UserEditActivity.class);
                startActivity(intent);
            }
        });

        // 点击搜索按钮触发事件
        search.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.e("Debug","跳转");
                Intent intent = new Intent();
                intent.setClass(MainActivity.this, SearchActivity.class);
                finish();
                startActivity(intent);
            }
        });
    }

    //初始化marker菜单列表
    private void initializeExpandableSelector() {
        iconsExpandableSelector = (ExpandableSelector) findViewById(R.id.es_icons);
        List<ExpandableItem> expandableItems = new ArrayList<>();
        ExpandableItem item = new ExpandableItem();
        item.setResourceId(R.drawable.add);
        expandableItems.add(item);
        item = new ExpandableItem();
        item.setResourceId(R.drawable.food);
        expandableItems.add(item);
        item = new ExpandableItem();
        item.setResourceId(R.drawable.trip);
        expandableItems.add(item);
        item = new ExpandableItem();
        item.setResourceId(R.drawable.mall);
        expandableItems.add(item);
        item = new ExpandableItem();
        item.setResourceId(R.drawable.other);
        expandableItems.add(item);
        iconsExpandableSelector.showExpandableItems(expandableItems);
        iconsExpandableSelector.setOnExpandableItemClickListener(new OnExpandableItemClickListener() {
            @Override public void onExpandableItemClickListener(int index, View view) {
                if (index == 0 && iconsExpandableSelector.isExpanded()) {
                    iconsExpandableSelector.collapse();
                }
            }
        });

        iconsExpandableSelector.setExpandableSelectorListener(new ExpandableSelectorListener() {
            @Override
            public void onCollapse() {
                food.setVisibility(View.GONE);
                trip.setVisibility(View.GONE);
                mall.setVisibility(View.GONE);
                other.setVisibility(View.GONE);
            }

            @Override
            public void onExpand() { }

            @Override
            public void onCollapsed() { }

            @Override
            public void onExpanded() {
                food.setVisibility(View.VISIBLE);
                trip.setVisibility(View.VISIBLE);
                mall.setVisibility(View.VISIBLE);
                other.setVisibility(View.VISIBLE);
            }
        });
    }


    //给四个Marker注册监听器
    private void addMarker() {
        food.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                switch (motionEvent.getAction()) {
                    case MotionEvent.ACTION_UP:
                        setMarker(1, food);
                        break;
                    default:
                        break;
                }
                return false;
            }
        });
        trip.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                switch (motionEvent.getAction()) {
                    case MotionEvent.ACTION_UP:
                        setMarker(2, trip);
                        break;
                    default:
                        break;
                }
                return false;
            }
        });
        mall.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                switch (motionEvent.getAction()) {
                    case MotionEvent.ACTION_UP:
                        setMarker(3, mall);
                        break;
                    default:
                        break;
                }
                return false;
            }
        });
        other.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                switch (motionEvent.getAction()) {
                    case MotionEvent.ACTION_UP:
                        setMarker(4, other);
                        break;
                    default:
                        break;
                }
                return false;
            }
        });
    }

    private void setMarker(int number, final MoveImageView imageView) {
        imageView.setVisibility(View.GONE);
        //获取marker在屏幕上的横、纵坐标
        int[] location = new int[2];
        imageView.getLocationOnScreen(location);
        final Point point = new Point(location[0]+45, location[1]+40);    //坐标->经纬度的差值
        //marker的经纬度
        LatLng latLng = aMap.getProjection().fromScreenLocation(point);
        latitude = latLng.latitude;
        longitude = latLng.longitude;

        //弹出自定义对话框
        LayoutInflater inflater = LayoutInflater.from(MainActivity.this);
        View view_in = inflater.inflate(R.layout.dialog, null);
        final AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setView(view_in);

        name_edit = view_in.findViewById(R.id.name_edit);
        description_edit = view_in.findViewById(R.id.description_edit);
        type_radio = view_in.findViewById(R.id.radio);
        food_button = view_in.findViewById(R.id.choose_food);
        trip_button = view_in.findViewById(R.id.choose_trip);
        mall_button = view_in.findViewById(R.id.choose_mall);
        other_button = view_in.findViewById(R.id.choose_other);

        //选中的特定位置添加marker
        final MarkerOptions otMarkerOptions = new MarkerOptions();
        if (number == 1) {
            otMarkerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.food_big));
            food_button.setChecked(true);
        }
        else if (number == 2) {
            otMarkerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.trip_big));
            trip_button.setChecked(true);
        }
        else if (number == 3) {
            otMarkerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.mall_big));
            mall_button.setChecked(true);
        }
        else if (number == 4) {
            otMarkerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.other_big));
            other_button.setChecked(true);


        }
        otMarkerOptions.position(latLng);
        otMarkerOptions.draggable(false);        //marker不可拖动
        final Marker marker = aMap.addMarker(otMarkerOptions);

        builder.setPositiveButton("保存", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                if (!TextUtils.equals(name_edit.getText().toString(), "")) {
                    String newName = name_edit.getText().toString();
                    String newDescription = description_edit.getText().toString();
                    String newType = "";
                    int length = type_radio.getChildCount();
                    for (int j = 0; j < length; j++) {
                        RadioButton radioButton = (RadioButton) type_radio.getChildAt(j);
                        if (radioButton.isChecked()) {
                            if (j == 0) {
                                newType = "food";
                            } else if (j == 1) {
                                newType = "trip";
                            } else if (j == 2) {
                                newType = "mall";
                            } else {
                                newType = "other";
                            }
                        }
                    }

                    User user = BmobUser.getCurrentUser(MainActivity.this, User.class);
                    String creator = user.getUsername();
                    Place place = new Place(creator, latitude, longitude, newType, newName, newDescription);
                    place.save(MainActivity.this, new SaveListener() {
                        @Override
                        public void onSuccess() {
                            Toast.makeText(getApplicationContext(),"添加成功", Toast.LENGTH_SHORT).show();
                        }
                        @Override
                        public void onFailure(int code, String arg0) {
                            Toast.makeText(getApplicationContext(),"添加失败", Toast.LENGTH_SHORT).show();
                        }
                    });
                    mDataBaseHelper.insert(creator, latitude, longitude, newType, newName, newDescription);
                }
                else {
                    marker.remove();
                }
                imageView.setVisibility(View.VISIBLE);
            }
        });
        builder.setNegativeButton("删除", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                marker.remove();
                imageView.setVisibility(View.VISIBLE);
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.rgb(48, 158, 186));
        dialog.getButton(DialogInterface.BUTTON_NEGATIVE).setTextColor(Color.rgb(48, 158, 186));
    }

    //开始定位
    private void initLocation() {
        //初始化定位
        mLocationClient = new AMapLocationClient(getApplicationContext());
        mLocationClient.setLocationListener(this);
        //初始化定位参数
        mLocationOption = new AMapLocationClientOption();
        //高精度定位
        mLocationOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);
        //返回定位信息
        mLocationOption.setNeedAddress(true);
        //设置定位间隔
        mLocationOption.setInterval(2000);
        mLocationClient.setLocationOption(mLocationOption);
        mLocationClient.startLocation();
    }

    //定位回调函数
    @Override
    public void onLocationChanged(AMapLocation amapLocation) {
        if (amapLocation != null) {
            if (amapLocation.getErrorCode() == 0) {
                //定位成功回调信息，设置相关消息
                mLocationLat = amapLocation.getLatitude();//获取纬度
                mLocationLon = amapLocation.getLongitude();//获取经度
                mLocationAddr = amapLocation.getAddress();//获取地址

                // 如果不设置标志位，此时再拖动地图时，它会不断将地图移动到当前的位置
                if (isFirstLoc) {
                    //设置缩放级别
                    aMap.moveCamera(CameraUpdateFactory.zoomTo(17));
                    //将地图移动到定位点
                    aMap.moveCamera(CameraUpdateFactory.changeLatLng(new LatLng(amapLocation.getLatitude(), amapLocation.getLongitude())));
                    //点击定位按钮 能够将地图的中心移动到定位点
                    mListener.onLocationChanged(amapLocation);
                    //获取定位信息
                    isFirstLoc = false;
                }
            } else {
                //显示错误信息：ErrCode是错误码，errInfo是错误信息
                Log.e("Error", "location Error, ErrCode:" + amapLocation.getErrorCode() + ", errInfo:" + amapLocation.getErrorInfo());
                Toast.makeText(getApplicationContext(), "定位失败", Toast.LENGTH_SHORT).show();
            }
        }
    }

    //激活定位
    @Override
    public void activate(OnLocationChangedListener listener) {
        mListener = listener;
    }

    //停止定位
    @Override
    public void deactivate() {
        mListener = null;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //销毁地图
        mMapView.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        //重新绘制加载地图
        mMapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        //暂停地图的绘制
        mMapView.onPause();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        //保存地图当前的状态
        mMapView.onSaveInstanceState(outState);
    }

    @Override
    public void onMapClick(LatLng latLng) {
        if (curShowWindowMarker != null) {
            curShowWindowMarker.hideInfoWindow();
        }
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        double latitude1 = marker.getPosition().latitude;
        double longitude1 = marker.getPosition().longitude;
        ArrayList<String> result = mDataBaseHelper.query(latitude1, longitude1);
        if (result != null && result.size() > 0) {
            marker.setTitle(result.get(0));
            marker.setSnippet(result.get(1));
        }
        curShowWindowMarker = marker;
        return false;
    }

    @Override
    public void onInfoWindowClick(Marker marker) {
        marker.hideInfoWindow();
        double latitude1 = marker.getPosition().latitude;
        double longitude1 = marker.getPosition().longitude;
        ArrayList<String> result = mDataBaseHelper.query(latitude1, longitude1);
        Place mp = new Place(latitude1, longitude1, result.get(2), result.get(0), result.get(1), result.get(3), result.get(4));

        Intent intent = new Intent(MainActivity.this, DetailActivity.class);
        Bundle bundle = new Bundle();
        bundle.putSerializable("mp", mp);
        intent.putExtras(bundle);
        startActivityForResult(intent, 888);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (data != null) {
                if (requestCode == 888) {
                    curShowWindowMarker.remove(); //删除当前marker
                    aMap.runOnDrawFrame(); //刷新地图
                }
            }
        }
    }
}
