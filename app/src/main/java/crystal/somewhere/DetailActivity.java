package crystal.somewhere;
//reference: http://blog.csdn.net/gaoshouxiaodi/article/details/50519344
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import cn.bmob.v3.BmobQuery;
import cn.bmob.v3.datatype.BmobFile;
import cn.bmob.v3.listener.DeleteListener;
import cn.bmob.v3.listener.DownloadFileListener;
import cn.bmob.v3.listener.FindListener;
import cn.bmob.v3.listener.UpdateListener;
import co.lujun.androidtagview.TagContainerLayout;
import crystal.somewhere.bean.Place;
import crystal.somewhere.utils.StringUtils;
import rx.Observable;
import rx.Observer;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

import com.sendtion.xrichtext.RichTextView;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.List;

public class DetailActivity extends AppCompatActivity {
    private EditText et_new_title, et_new_description;
    private RichTextView tv_new_content;
    private TagContainerLayout mTagContainerLayout;
    private ImageView coverPage;

    private Place mp;
    private DataBaseHelper mdbh;
    private String returnedId;

    private Subscription subsLoading, subsInsert;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);
        init();
    }

    private void init() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_new);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        toolbar.setNavigationIcon(R.drawable.left_arrow);

        et_new_title = (EditText) findViewById(R.id.et_new_title);
        et_new_description = (EditText) findViewById(R.id.et_new_description);
        tv_new_content = (RichTextView) findViewById(R.id.tv_edit_content);
        mTagContainerLayout = (TagContainerLayout) findViewById(R.id.tagcontainerLayout);
        //mTagContainerLayout.setTagBackgroundColor(Color.TRANSPARENT);
        coverPage = (ImageView) findViewById(R.id.cover_page);

        Bundle bundle = getIntent().getExtras();
        mp = (Place) bundle.getSerializable("mp");
        mTagContainerLayout.setTags(mp.getTags());
        et_new_title.setText(mp.getName());
        et_new_description.setText(mp.getDescription());
        queryId();

        if(!mp.getCoverPageLocalPath().equals("")) {
            Bitmap bitmap = getBitmap(mp.getCoverPageLocalPath());
            coverPage.setImageBitmap(bitmap);
        }
        mdbh = new DataBaseHelper(this);

        if(mp.getContent() != null) {
            tv_new_content.post(new Runnable() {
                @Override
                public void run() {
                    tv_new_content.clearAllLayout();
                    showDataSync(mp.getContent());
                }
            });
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_detail, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_share:
                mp.setIspublic(true);
                mp.update(this, returnedId, new UpdateListener() {
                    @Override
                    public void onSuccess() {
                        showToast("更新成功");
                    }
                    @Override
                    public void onFailure(int i, String s) {
                        Log.i("bmob","更新失败："+i+","+s);
                    }
                });
                break;
            case R.id.action_edit:
                Intent intent = new Intent(DetailActivity.this, EditActivity.class);
                Bundle bundle = new Bundle();
                bundle.putSerializable("mp", mp);
                intent.putExtras(bundle);
                startActivityForResult(intent, 2333);
                break;
            case R.id.action_delete:
                mdbh.delete(mp.getLatitude(), mp.getLongitude());
                mp.delete(DetailActivity.this, returnedId, new DeleteListener() {
                    @Override
                    public void onSuccess() {
                        showToast("删除成功"+returnedId);
                    }
                    @Override
                    public void onFailure(int i, String s) {
                        Log.i("bmob","更新失败："+i+","+s);
                    }
                });
                Intent intent1 = new Intent();
                setResult(RESULT_OK, intent1);
                finish();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showDataSync(final String html){
        subsLoading = Observable.create(new Observable.OnSubscribe<String>() {
            @Override
            public void call(Subscriber<? super String> subscriber) {
                showEditData(subscriber, html);
            }
        })
                .onBackpressureBuffer()
                .subscribeOn(Schedulers.io())//生产事件在io
                .observeOn(AndroidSchedulers.mainThread())//消费事件在UI线程
                .subscribe(new Observer<String>() {
                    @Override
                    public void onCompleted() {
                    }

                    @Override
                    public void onError(Throwable e) {
                        showToast("解析错误：图片不存在或已损坏");
                    }

                    @Override
                    public void onNext(String text) {
                        if (text.contains("/storage/")){
                            tv_new_content.addImageViewAtIndex(tv_new_content.getLastIndex(), text);
                        } else {
                            tv_new_content.addTextViewAtIndex(tv_new_content.getLastIndex(), text);
                        }
                    }
                });
    }

    protected void showEditData(Subscriber<? super String> subscriber, String html) {
        try{
            List<String> textList = StringUtils.cutStringByImgTag(html);
            for (int i = 0; i < textList.size(); i++) {
                String text = textList.get(i);
                if (text.contains("<img")) {
                    String imagePath = StringUtils.getImgSrc(text);
                    if (new File(imagePath).exists()) {
                        subscriber.onNext(imagePath);
                    }
                    else showToast("图片"+i+"已丢失，请重新插入！");
                } else {
                    subscriber.onNext(text);
                }
            }
            subscriber.onCompleted();
        }catch (Exception e){
            e.printStackTrace();
            subscriber.onError(e);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (data != null) {
                if (requestCode == 2333) {
                    Bundle bundle = data.getExtras();
                    mp = (Place) bundle.getSerializable("mp");
                    mTagContainerLayout.setTags(mp.getTags());
                    et_new_title.setText(mp.getName());
                    et_new_description.setText(mp.getDescription());
                    if(mp.getContent() != null) {
                        tv_new_content.post(new Runnable() {
                            @Override
                            public void run() {
                                tv_new_content.clearAllLayout();
                                showDataSync(mp.getContent());
                            }
                        });
                    }
                    if(mp.getCoverPageLocalPath().equals("")) coverPage.setImageResource(R.drawable.sea);
                    else {
                        Bitmap bitmap = getBitmap(mp.getCoverPageLocalPath());
                        coverPage.setImageBitmap(bitmap);
                    }
                }
            }
        }
    }

    private void queryId() {
        BmobQuery<Place> query = new BmobQuery<Place>();
        query.addWhereEqualTo("latitude", mp.getLatitude());
        query.addWhereEqualTo("longitude", mp.getLongitude());
        query.findObjects(DetailActivity.this, new FindListener<Place>() {
            @Override
            public void onSuccess(List<Place> object) {
                returnedId = object.get(0).getObjectId();
            }
            @Override
            public void onError(int i, String s) {
                Log.i("bmob","查询失败："+i+","+s);
            }
        });
    }

    public void showToast(String text) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }

    //路径转Bitmap
    private Bitmap getBitmap(String path) {
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inJustDecodeBounds = true;
        opts.inPreferredConfig = Bitmap.Config.ARGB_8888;
        BitmapFactory.decodeFile(path, opts);
        int width = opts.outWidth;
        int height = opts.outHeight;
        float scaleWidth = 0.f, scaleHeight = 0.f;
        int windowWidth = getWindowManager().getDefaultDisplay().getWidth();
        if (width > windowWidth || height > 250) {
            scaleWidth = ((float) width) / windowWidth;
            scaleHeight = ((float) height) / 350;
        }
        opts.inJustDecodeBounds = false;
        float scale = Math.max(scaleWidth, scaleHeight);
        opts.inSampleSize = (int) scale;
        WeakReference<Bitmap> weak = new WeakReference<Bitmap>(
                BitmapFactory.decodeFile(path, opts));
        return Bitmap.createScaledBitmap(weak.get(), windowWidth, 250, true);
    }
}