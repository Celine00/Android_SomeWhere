package crystal.somewhere;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.ImageView;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.OnClick;
import cn.bmob.v3.Bmob;
import cn.bmob.v3.BmobQuery;
import cn.bmob.v3.listener.FindListener;
import crystal.somewhere.bean.Place;
import crystal.somewhere.bean.User;
import crystal.somewhere.utils.Utils;


public class ExploreActivity extends AppCompatActivity implements FeedAdapter.OnFeedItemClickListener {
    public static final String ACTION_SHOW_LOADING_ITEM = "action_show_loading_item";

    private static final int ANIM_DURATION_TOOLBAR = 300;

    RecyclerView rvFeed;
    CoordinatorLayout clContent;

    private FeedAdapter feedAdapter;

    private boolean pendingIntroAnimation;

    private List<Place> places= new ArrayList<>();

    private boolean queryresult = true;

    private String username;

    private User user;

    private List<String> favplace = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_explore);
        rvFeed = (RecyclerView) findViewById(R.id.rvFeed);
        clContent = (CoordinatorLayout) findViewById(R.id.content);
        if (savedInstanceState == null) {
            pendingIntroAnimation = true;
        } else {
            feedAdapter.updateItems(false);
        }
        Log.i("Debug","start query");
        Bmob.initialize(this, "9020a320ba34abf2203135dad5de9727");
        username = (String) User.getObjectByKey(ExploreActivity.this, "username");
        getDataFromDatabase();
    }

    private void setupFeed() {
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this) {
            @Override
            protected int getExtraLayoutSpace(RecyclerView.State state) {
                return 300;
            }
        };
        rvFeed.setLayoutManager(linearLayoutManager);
        Log.i("Debug","query end");
        feedAdapter = new FeedAdapter(this,places,user);
        feedAdapter.setOnFeedItemClickListener(this);
        Log.i("Debug","set adapter");
        rvFeed.setAdapter(feedAdapter);
        rvFeed.setItemAnimator(new FeedItemAnimator());

    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (ACTION_SHOW_LOADING_ITEM.equals(intent.getAction())) {
            showFeedLoadingItemDelayed();
        }
    }

    private void showFeedLoadingItemDelayed() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                rvFeed.smoothScrollToPosition(0);
                feedAdapter.showLoadingView();
            }
        }, 500);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        return true;
    }

    private void startIntroAnimation() {
        startContentAnimation();
    }

    private void startContentAnimation() {
        Log.i("Debug","start draw");
        if (queryresult) {
            feedAdapter.updateItems(true);
        }
    }

    @Override
    public void onCommentsClick(View v, int position) {
        final Intent intent = new Intent(this, CommentsActivity.class);
        int[] startingLocation = new int[2];
        v.getLocationOnScreen(startingLocation);
        intent.putExtra(CommentsActivity.ARG_DRAWING_START_LOCATION, startingLocation[1]);
        //传入地点数据
        Place place = places.get(position);
        Bundle bundle = new Bundle();
        bundle.putSerializable("place",place);
        intent.putExtras(bundle);
        startActivity(intent);
        overridePendingTransition(0, 0);
    }

    public void showLikedSnackbar() {
        Snackbar.make(clContent, "Liked!", Snackbar.LENGTH_SHORT).show();
    }

    private void getDataFromDatabase() {
        //需要一份当前用户对地点的点赞名单
        BmobQuery<User> q = new BmobQuery<>();
        q.addWhereEqualTo("username",username);
        q.findObjects(this, new FindListener<User>() {
            @Override
            public void onSuccess(List<User> list) {
                user = list.get(0);
                favplace = user.getFavplaceid();
                Log.i("bmob","获取用户点赞列表");
            }
            @Override
            public void onError(int i, String s) {
                Log.i("bmob","获取用户数据失败");
                queryresult = false;
            }
        });

        BmobQuery<Place> query = new BmobQuery<>();
        query.addWhereEqualTo("ispublic",true);
        query.setLimit(100);
        query.findObjects(this, new FindListener<Place>() {
            @Override
            public void onSuccess(List<Place> list) {
                Log.i("Debug", "query_Place_Success "+Integer.toString(list.size()));
                Log.i("Debug","places size "+Integer.toString(places.size()));
                // 存在匹配的搜索结果
                if (list.size() > 0) {
                    //更新地点对应的用户点赞表isLisked
                    for (int i = 0;i<list.size();i++) {
                        Place tmp = list.get(i);
                        //这里可能有一点问题
                        if (favplace != null && favplace.contains(tmp.getObjectId())){
                            tmp.setIsliked(true);
                        } else {
                            tmp.setIsliked(false);
                        }
                        places.add(tmp);
                    }
                    //获得公开的地点列表，转到adapter设置
                    Log.i("Debug","places size "+Integer.toString(places.size()));
                    if (places.size() > 0) {
                        queryresult = true;
                    }
                    setupFeed();
                    if (pendingIntroAnimation) {
                        pendingIntroAnimation = false;
                        startIntroAnimation();
                    }
                } else {
                    Log.i("bmob","获取地点数据失败");
                    queryresult = false;
                }
            }

            @Override
            public void onError(int i, String s) {
                Log.e("Debug", s);
            }
        });
    }
}