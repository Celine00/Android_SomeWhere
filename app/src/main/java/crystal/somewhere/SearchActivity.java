package crystal.somewhere;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.TextView;

import com.zhy.view.flowlayout.FlowLayout;
import com.zhy.view.flowlayout.TagAdapter;
import com.zhy.view.flowlayout.TagFlowLayout;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import cn.bmob.v3.Bmob;
import cn.bmob.v3.BmobQuery;
import cn.bmob.v3.BmobUser;
import cn.bmob.v3.listener.FindListener;
import crystal.somewhere.bean.User;
import de.hdodenhof.circleimageview.CircleImageView;

/**
 * Created by Celine on 2018/1/4.
 */

public class SearchActivity extends AppCompatActivity implements View.OnClickListener {
    private TagFlowLayout mFlowLayout;
    private List<String> iniTag = new ArrayList<>();
    private List<String> iniType = new ArrayList<>();
    private ImageView tagView;
    private List<Integer> selectTag = new ArrayList<>();
    private List<Integer> selectType = new ArrayList<>();
    private int flag = 1;
    private CircleImageView other_type,food_type, travel_type, mall_type;

    private final int CLICK_OTHER = 0;
    private final int CLICK_FOOD = 1;
    private final int CLICK_TRAVEL = 2;
    private final int CLICK_MALL= 3;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);
        // 初始化 Bmob SDK
        Bmob.initialize(this, "9020a320ba34abf2203135dad5de9727");
        // toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_search);
        setSupportActionBar(toolbar);
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        //toolbar.setNavigationIcon(R.drawable.back);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });
        toolbar.setBackgroundColor(getResources().getColor(R.color.land));

        initData();
        initCircleImgView();
        // button click
        tagView = (ImageView) findViewById(R.id.showTag);
        tagView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (flag == 0) {
                    mFlowLayout.setVisibility(View.VISIBLE);
                    tagView.setImageResource(R.drawable.up_arrow);
                    flag = 1;
                } else if (flag == 1) {
                    mFlowLayout.setVisibility(View.GONE);
                    tagView.setImageResource(R.drawable.down_arrow);
                    flag = 0;
                }
            }
        });

    }
    public void initData() {
        // initialize type
        iniType.add("other");
        iniType.add("food");
        iniType.add("trip");
        iniType.add("mall");
        for (int i = 0; i < 4; i++)
            selectType.add(0);
        selectType.set(1,1);

        BmobQuery<User> query = new BmobQuery<User>();
        query.addQueryKeys("tag");
        query.addWhereEqualTo("username",BmobUser.getCurrentUser(getApplicationContext()).getUsername());
        query.findObjects(this, new FindListener<User>() {
            @Override
            public void onSuccess(List<User> list) {
                //Log.e("Debug", "query_Tag_Success"+Integer.toString(list.size()));
                List<String> tagList = list.get(0).getTag();
                if (tagList != null) {
                    for (int i = 0; i < tagList.size(); i++) {
                        String tagName = tagList.get(i);
                        iniTag.add(tagName);
                        // 初始化所有tag的flag为0，意味着未被选上
                        selectTag.add(0);
                    }
                    initTag();
                }
            }

            @Override
            public void onError(int i, String s) {
                Log.e("Debug", s);
            }
        });

    }
    private void initCircleImgView() {
        food_type = (CircleImageView)findViewById(R.id.food_type);
        travel_type = (CircleImageView)findViewById(R.id.travel_type);
        mall_type = (CircleImageView)findViewById(R.id.mall_type);
        other_type = (CircleImageView)findViewById(R.id.other_type);
        // 设置不同的tag属性
        other_type.setOnClickListener(this);
        other_type.setTag(CLICK_OTHER);
        food_type.setOnClickListener(this);
        food_type.setTag(CLICK_FOOD);
        travel_type.setOnClickListener(this);
        travel_type.setTag(CLICK_TRAVEL);
        mall_type.setOnClickListener(this);
        mall_type.setTag(CLICK_MALL);
    }
    // 利用一个监听器监听所有CircleImgView的点击事件,利用tag属性
    @Override
    public void onClick(View view) {
        int tag = (Integer) view.getTag();
        // 先将所有tag恢复默认状态
        refreshType();
        switch (tag) {
            case CLICK_OTHER:
                Log.e("Debug", "other_tag");
                other_type.setBorderColor(getResources().getColor(R.color.land));
                selectType.set(CLICK_OTHER,1);
                break;
            case CLICK_FOOD:
                Log.e("Debug", "food_tag");
                food_type.setBorderColor(getResources().getColor(R.color.land));
                selectType.set(CLICK_FOOD,1);
                break;
            case CLICK_TRAVEL:
                Log.e("Debug", "travel_tag");
                travel_type.setBorderColor(getResources().getColor(R.color.land));
                selectType.set(CLICK_TRAVEL,1);
                break;
            case CLICK_MALL:
                Log.e("Debug", "mall_tag");
                mall_type.setBorderColor(getResources().getColor(R.color.land));
                selectType.set(CLICK_MALL,1);
                break;
        }
    }
    public void refreshType() {
        // refresh color
        food_type.setBorderColor(getResources().getColor(R.color.white));
        travel_type.setBorderColor(getResources().getColor(R.color.white));
        mall_type.setBorderColor(getResources().getColor(R.color.white));
        other_type.setBorderColor(getResources().getColor(R.color.white));
        // refresh value of flag
        for (int i = 0; i < 4; i++)
            selectType.set(i,0);
    }
    public void initTag() {
        // tag
        String[] mVals = iniTag.toArray(new String[iniTag.size()]);
        final LayoutInflater mInflater = LayoutInflater.from(SearchActivity.this);
        mFlowLayout = (TagFlowLayout) findViewById(R.id.id_flowlayout);
        //mFlowLayout.setMaxSelectCount(3);
        mFlowLayout.setAdapter(new TagAdapter<String>(mVals) {
            @Override
            public View getView(FlowLayout parent, int position, String s) {
                TextView tv = (TextView) mInflater.inflate(R.layout.tv,
                        mFlowLayout, false);
                tv.setText(s);
                return tv;
            }

//            @Override
//            public boolean setSelected(int position, String s) {
//                return s.equals("Android");
//            }
        });
        mFlowLayout.setOnTagClickListener(new TagFlowLayout.OnTagClickListener() {
            @Override
            public boolean onTagClick(View view, int position, FlowLayout parent) {
                //view.setVisibility(View.GONE);
                // 改变对应tag的flag值
                if (selectTag.get(position) == 0) {
                    // 设置flag值为1，选中状态
                    selectTag.set(position,1);
                } else {
                    selectTag.set(position,0);
                }
                return true;
            }
        });
        mFlowLayout.setOnSelectListener(new TagFlowLayout.OnSelectListener() {
            @Override
            public void onSelected(Set<Integer> selectPosSet) {
                SearchActivity.this.setTitle("choose:" + selectPosSet.toString());
            }
        });
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        // Get the SearchView and set the searchable configuration
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        final SearchView searchView = (SearchView) menu.findItem(R.id.search).getActionView();
        // Assumes current activity is the searchable activity
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        // 令搜索框初始为展开状态
        searchView.setIconifiedByDefault(false);
        searchView.setSubmitButtonEnabled(true);
        // 监听器
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextChange(String queryText) {
                Log.e("Debug", "onQueryTextChange = " + queryText);
                return true;
            }
            /*
             * 输入完成后，提交时触发的方法，一般情况是点击输入法中的搜索按钮才会触发。表示现在正式提交了
             *
             * @param queryText
             *
             * @return true to indicate that it has handled the submit request.
             * Otherwise return false to let the SearchView handle the
             * submission by launching any associated intent.
             */
            @Override
            public boolean onQueryTextSubmit(String queryText) {
                Log.e("Debug", "onQueryTextSubmit = " + queryText);

                // 得到输入管理对象
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    //这将让键盘在所有的情况下都被隐藏，但是一般我们在点击搜索按钮后，输入法都会乖乖的自动隐藏的。
                    imm.hideSoftInputFromWindow(searchView.getWindowToken(), 1); // 输入法如果是显示状态，那么就隐藏输入法
                }
                searchView.clearFocus(); // 不获取焦点

                // 获取选中的tag 名称，传递至下一个activity
                List<String> queryTag = new ArrayList<String>();
                for (int i = 0; i < selectTag.size(); i++) {
                    if (selectTag.get(i) == 1) {
                        queryTag.add(iniTag.get(i));
                        Log.e("Debug-TagName",iniTag.get(i));
                    }
                }
                // 获取选中的type名称
                String queryType = null;
                for (int i = 0; i < 4; i++) {
                    if (selectType.get(i) == 1) {
                        queryType = iniType.get(i);
                        Log.e("Debug-TypeName",iniType.get(i));
                        break;
                    }
                }
                Intent intent = new Intent();
                Log.e("Debug", SearchManager.QUERY);
                intent.setClass(SearchActivity.this,SearchResultActivity.class);
                intent.putExtra(SearchManager.QUERY, queryText);
                intent.putExtra("tagSize", queryTag.size());
                intent.putExtra("tag",(Serializable)(queryTag));
                intent.putExtra("type",queryType);
                startActivity(intent);
                return true;
            }
        });
        return true;
    }
}
