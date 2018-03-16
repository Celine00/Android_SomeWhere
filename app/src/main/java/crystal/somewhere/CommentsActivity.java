package crystal.somewhere;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import cn.bmob.v3.Bmob;
import cn.bmob.v3.BmobQuery;
import cn.bmob.v3.listener.FindListener;
import cn.bmob.v3.listener.UpdateListener;
import crystal.somewhere.bean.Place;
import crystal.somewhere.bean.User;
import crystal.somewhere.utils.Utils;
import crystal.somewhere.view.SendCommentButton;

public class CommentsActivity extends AppCompatActivity implements SendCommentButton.OnSendClickListener {
    public static final String ARG_DRAWING_START_LOCATION = "arg_drawing_start_location";

    LinearLayout contentRoot;
    RecyclerView rvComments;
    LinearLayout llAddComment;
    EditText etComment;
    SendCommentButton btnSendComment;

    private CommentsAdapter commentsAdapter;
    private int drawingStartLocation;
    private String placeid;
    private List<Map<String,Object>> dataset = new ArrayList<>();
    //ADAPTER数据
    private boolean queryre = true;
    private boolean startdraw = false;
    private String user;
    private Place place;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comments);

        contentRoot = (LinearLayout) findViewById(R.id.contentRoot);
        rvComments = (RecyclerView) findViewById(R.id.rvComments);
        llAddComment = (LinearLayout) findViewById(R.id.llAddComment);
        etComment = (EditText) findViewById(R.id.etComment);
        btnSendComment = (SendCommentButton) findViewById(R.id.btnSendComment);

        // 初始化 Bmob SDK
        Bmob.initialize(this, "9020a320ba34abf2203135dad5de9727");
        user = (String) User.getObjectByKey(CommentsActivity.this, "username");

        Log.i("debug","开始获取数据");
        place = (Place) getIntent().getSerializableExtra("place");
        placeid = place.getObjectId();
        getDataFromDatabase(placeid);

        drawingStartLocation = getIntent().getIntExtra(ARG_DRAWING_START_LOCATION, 0);
        if (savedInstanceState == null) {
            startdraw = true;
        }

    }

    private void setupComments() {
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        rvComments.setLayoutManager(linearLayoutManager);
        rvComments.setHasFixedSize(true);

        commentsAdapter = new CommentsAdapter(this,dataset);
        rvComments.setAdapter(commentsAdapter);
        rvComments.setOverScrollMode(View.OVER_SCROLL_NEVER);
        rvComments.setOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                    commentsAdapter.setAnimationsLocked(true);
                }
            }
        });
    }

    private void setupSendCommentButton() {
        btnSendComment.setOnSendClickListener(this);
    }

    private void startIntroAnimation() {
        //ViewCompat.setElevation(getToolbar(), 0);
        contentRoot.setScaleY(0.1f);
        contentRoot.setPivotY(drawingStartLocation);
        llAddComment.setTranslationY(200);

        contentRoot.animate()
                .scaleY(1)
                .setDuration(200)
                .setInterpolator(new AccelerateInterpolator())
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        //ViewCompat.setElevation(getToolbar(), Utils.dpToPx(8));
                        animateContent();
                    }
                })
                .start();
        Log.i("debug","setup end");
    }

    private void animateContent() {
        if (queryre) {
            commentsAdapter.updateItems();
        }
        llAddComment.animate().translationY(0)
                .setInterpolator(new DecelerateInterpolator())
                .setDuration(200)
                .start();
    }

    @Override
    public void onBackPressed() {
        //ViewCompat.setElevation(getToolbar(), 0);
        contentRoot.animate()
                .translationY(Utils.getScreenHeight(this))
                .setDuration(200)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        CommentsActivity.super.onBackPressed();
                        overridePendingTransition(0, 0);
                    }
                })
                .start();
    }

    @Override
    public void onSendClickListener(View v) {
        if (validateComment()) {
            commentsAdapter.addItem(etComment.getText().toString(),user);
            commentsAdapter.setAnimationsLocked(false);
            commentsAdapter.setDelayEnterAnimation(false);
            //rvComments.getChildAt(0).getHeight();
            rvComments.smoothScrollBy(0, 50 * commentsAdapter.getItemCount());
            //更新place的user和comments
            List<String> username = place.getUser();
            List<String> comments = place.getComments();
            if (username == null) {
                username = new ArrayList<>();
            }
            if (comments == null){
                comments = new ArrayList<>();
            }
            comments.add(etComment.getText().toString());
            username.add(user);
            Place temp = place;
            temp.setComment(comments);
            temp.setCommentor(username);
            temp.update(CommentsActivity.this, placeid, new UpdateListener() {
                @Override
                public void onSuccess() {
                    Toast.makeText(getApplicationContext(), "评论成功", Toast.LENGTH_SHORT).show();
                }
                @Override
                public void onFailure(int code, String msg) {
                    Toast.makeText(getApplicationContext(), "评论失败"+msg, Toast.LENGTH_SHORT).show();
                }
            });

            etComment.setText(null);
            btnSendComment.setCurrentState(SendCommentButton.STATE_DONE);
        }
    }

    private boolean validateComment() {
        if (TextUtils.isEmpty(etComment.getText())) {
            btnSendComment.startAnimation(AnimationUtils.loadAnimation(this, R.anim.shake_error));
            return false;
        }

        return true;
    }

    private void getDataFromDatabase(String placeid) {
        final BmobQuery<Place> query = new BmobQuery<>();
        query.addWhereEqualTo("objectId",placeid);
        query.setLimit(1);
        query.findObjects(this, new FindListener<Place>() {
            @Override
            public void onSuccess(List<Place> list) {
                Log.e("Debug", "query_Place_Success "+Integer.toString(list.size()));
                // 存在匹配的搜索结果
                if (list.size() > 0) {
                    Place temp = list.get(0);
                    place = temp;
                    Log.e("Debug", "query_Place_Success ");
                    if (temp.getUser() != null && temp.getComments() != null) {
                        for (int i = 0;i<temp.getUser().size();i++) {
                            Map<String,Object> tmp = new LinkedHashMap<>();
                            tmp.put("username",temp.getUser().get(i));
                            tmp.put("comment",temp.getComments().get(i));
                            dataset.add(tmp);
                        }
                        Log.i("debug","hasData");
                        if (dataset.size() == 0) {
                            Log.e("Debug", "no data");
                            queryre = false;
                        }
                    } else {
                        Log.e("Debug", "no data,null");
                        queryre = false;
                    }
                    setupComments();
                    setupSendCommentButton();
                    if (startdraw) {
                        contentRoot.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                            @Override
                            public boolean onPreDraw() {
                                contentRoot.getViewTreeObserver().removeOnPreDrawListener(this);
                                startIntroAnimation();
                                return true;
                            }
                        });
                    }
                } else {
                    Log.i("bmob","加载评论失败");
                }
            }

            @Override
            public void onError(int i, String s) {
                Log.e("Debug", s);
            }
        });
    }
}
