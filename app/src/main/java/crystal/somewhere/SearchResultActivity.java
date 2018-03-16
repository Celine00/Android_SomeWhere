package crystal.somewhere;

import android.app.SearchManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.ramotion.foldingcell.FoldingCell;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cn.bmob.v3.Bmob;
import cn.bmob.v3.BmobQuery;
import cn.bmob.v3.BmobUser;
import cn.bmob.v3.listener.FindListener;
import crystal.somewhere.bean.Place;

/**
 * Created by Celine on 2018/1/6.
 */

public class SearchResultActivity extends AppCompatActivity{
    private String searchContent;
    private List<String> searchTag = new ArrayList<>();
    private List<Place> placeList = new ArrayList<>();
    private List<Place> placeListResult = new ArrayList<>();
    private int tagSize;
    private String searchType;
    private ListView theListView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.search_result);
        // get our folding cell
        theListView = (ListView) findViewById(R.id.mainListView);

        // 初始化 Bmob SDK
        Bmob.initialize(this, "9020a320ba34abf2203135dad5de9727");

        // 获取搜索内容
        Intent intent = getIntent();
        searchContent = intent.getStringExtra(SearchManager.QUERY);
        // 获取设定的tag
        tagSize = intent.getIntExtra("tagSize",-1);
        Log.e("Debug-tagSize", Integer.toString(tagSize));
        searchTag = (List<String>)intent.getSerializableExtra("tag");
        // 获取设定的type
        searchType = intent.getStringExtra("type");
        Log.e("Debug-type", searchType);
        // 对数据表进行搜索
        getDataFromDatabase();
    }
    private void getDataFromDatabase() {
        BmobQuery<Place> query = new BmobQuery<>();
        //query.addWhereGreaterThanOrEqualTo("name",searchContent);
        // 检索tag
        if (tagSize > 0) {
            query.addWhereContainsAll("tag", searchTag);
        }
        // 检索type
        query.addWhereEqualTo("type",searchType);
        query.addWhereEqualTo("creator", BmobUser.getCurrentUser(getApplicationContext()).getUsername());
        query.findObjects(this, new FindListener<Place>() {
            @Override
            public void onSuccess(List<Place> list) {
                Log.e("Debug", "query_Place_Success"+Integer.toString(list.size()));
                // 存在匹配的搜索结果
                if (list.size() > 0) {
                    for (int i = 0; i < list.size(); i++) {
                        placeList.add(list.get(i));
                    }
                    // 关键字匹配
                    contentMatch();
                    // initialize searchView
                    iniSearchView();
                } else {
                    new AlertDialog.Builder(SearchResultActivity.this)
                            .setTitle("查询结果")
                            .setMessage("很遗憾，未找到您搜索的内容，请换个条件再试试n(*≧▽≦*)n")
                            .setNegativeButton("返回", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    // 点击关闭后结束当前activity
                                    finish();
                                }
                            })
                            .show();

                }
            }

            @Override
            public void onError(int i, String s) {
                Log.e("Debug", s);
            }
        });
    }
    private void contentMatch() {
//        // 英文检索
//        Pattern pattern = Pattern.compile(searchContent);
//        for (int i = 0; i < placeList.size(); i++) {
//            String placeName = placeList.get(i).getName();
//            Matcher matcher = pattern.matcher(placeName);
//            if (matcher.matches()) {
//                placeListResult.add( placeList.get(i));
//            }
//        }
        // 中文检索
        Pattern pattern1 = Pattern.compile("(.{0,5}" + searchContent + ".{0,5})");
        for (int i = 0; i < placeList.size(); i++) {
            String placeName = placeList.get(i).getName();
            Matcher matcher = pattern1.matcher(placeName);
            if (matcher.matches()) {
                placeListResult.add(placeList.get(i));
            }
        }
    }
    private void iniSearchView() {
        if (placeListResult.size() == 0) {
            new AlertDialog.Builder(SearchResultActivity.this)
                    .setTitle("查询结果")
                    .setMessage("很遗憾，未找到您搜索的内容，请换个条件再试试n(*≧▽≦*)n")
                    .setNegativeButton("返回", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            // 点击关闭后结束当前activity
                            finish();
                        }
                    })
                    .show();
        }
        // create custom adapter that holds elements and their state (we need hold a id's of unfolded elements for reusable elements)
        final FoldingCellListAdapter adapter = new FoldingCellListAdapter(this, placeListResult);

        // add default btn handler for each request btn on each item if custom handler not found
        adapter.setDefaultRequestBtnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getApplicationContext(), "DEFAULT HANDLER FOR ALL BUTTONS", Toast.LENGTH_SHORT).show();
            }
        });
        // set elements to adapter
        theListView.setAdapter(adapter);
        // set on click event listener to list view
        theListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int pos, long l) {
                // toggle clicked cell state
                ((FoldingCell) view).toggle(false);
                // register in adapter that state for selected cell is toggled
                adapter.registerToggle(pos);
            }
        });
        /*theListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {
                Log.e("Debug", placeListResult.get(i).getName());
                Toast.makeText(getApplicationContext(),placeListResult.get(i).getName(),Toast.LENGTH_SHORT).show();
                Intent intent = new Intent();
                intent.putExtra("place",(Serializable)placeListResult.get(i));
                boolean fromSearch = true;
                intent.putExtra("fromSearch", fromSearch);
                intent.setClass(SearchResultActivity.this, EditActivity.class);
                startActivity(intent);
                return false;
            }
        });*/
    }
}
