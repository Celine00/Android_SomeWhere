package crystal.somewhere;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v7.widget.LinearLayoutCompat;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextSwitcher;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import cn.bmob.v3.datatype.BmobFile;
import cn.bmob.v3.listener.DownloadFileListener;
import cn.bmob.v3.listener.UpdateListener;
import crystal.somewhere.bean.Place;
import crystal.somewhere.bean.User;
import crystal.somewhere.view.LoadingFeedItemView;


public class FeedAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    public static final String ACTION_LIKE_BUTTON_CLICKED = "action_like_button_button";
    public static final String ACTION_LIKE_IMAGE_CLICKED = "action_like_image_button";

    public static final int VIEW_TYPE_DEFAULT = 1;
    public static final int VIEW_TYPE_LOADER = 2;

    private final List<FeedItem> feedItems = new ArrayList<>();
    private List<Place> places = new ArrayList<>();

    private Context context;
    private OnFeedItemClickListener onFeedItemClickListener;

    private boolean showLoadingView = false;
    private User user;

    //在这里获得公开的地点类，设置信息
    //需要地点的名称，图片，描述，点赞数
    public FeedAdapter(Context context,List<Place> place,User user) {
        this.context = context;
        this.places = place;
        this.user = user;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_DEFAULT) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_feed, parent, false);
            CellFeedViewHolder cellFeedViewHolder = new CellFeedViewHolder(view);
            setupClickableViews(view, cellFeedViewHolder);
            return cellFeedViewHolder;
        } else if (viewType == VIEW_TYPE_LOADER) {
            LoadingFeedItemView view = new LoadingFeedItemView(context);
            view.setLayoutParams(new LinearLayoutCompat.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT)
            );
            return new LoadingCellFeedViewHolder(view);
        }

        return null;
    }

    private void setupClickableViews(final View view, final CellFeedViewHolder cellFeedViewHolder) {
        cellFeedViewHolder.btnComments.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onFeedItemClickListener.onCommentsClick(view, cellFeedViewHolder.getAdapterPosition());
            }
        });
        cellFeedViewHolder.ivFeedCenter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean update;
                int adapterPosition = cellFeedViewHolder.getAdapterPosition();
                if (feedItems.get(adapterPosition).isLiked) {
                    feedItems.get(adapterPosition).likesCount--;
                    feedItems.get(adapterPosition).setLiked(false);
                    places.get(adapterPosition).setIsliked(false);
                    notifyItemChanged(adapterPosition);
                    //更新用户点赞列表
                    update = false;
                } else {
                    feedItems.get(adapterPosition).likesCount++;
                    feedItems.get(adapterPosition).setLiked(true);
                    places.get(adapterPosition).setIsliked(true);
                    notifyItemChanged(adapterPosition, ACTION_LIKE_IMAGE_CLICKED);
                    if (context instanceof ExploreActivity) {
                        ((ExploreActivity) context).showLikedSnackbar();
                    }
                    //更新用户点赞列表
                    update = true;
                }
                places.get(adapterPosition).setLikes(feedItems.get(adapterPosition).likesCount);
                //更新place的点赞数
                Place temp = places.get(adapterPosition);
                String placeid = temp.getObjectId();
                temp.update(context, placeid, new UpdateListener() {
                    @Override
                    public void onSuccess() {
                        Log.i("bmob","点赞数更新成功");
                    }

                    @Override
                    public void onFailure(int i, String s) {
                        Log.i("bmob","点赞更新失败："+i+","+s);
                    }
                });
                updateUser(update,placeid);
            }
        });
        cellFeedViewHolder.btnLike.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean update;
                int adapterPosition = cellFeedViewHolder.getAdapterPosition();
                if (feedItems.get(adapterPosition).isLiked) {
                    feedItems.get(adapterPosition).likesCount--;
                    feedItems.get(adapterPosition).setLiked(false);
                    places.get(adapterPosition).setIsliked(false);
                    notifyItemChanged(adapterPosition);
                    update = false;
                } else {
                    feedItems.get(adapterPosition).likesCount++;
                    feedItems.get(adapterPosition).setLiked(true);
                    places.get(adapterPosition).setIsliked(true);
                    notifyItemChanged(adapterPosition, ACTION_LIKE_BUTTON_CLICKED);
                    if (context instanceof ExploreActivity) {
                        ((ExploreActivity) context).showLikedSnackbar();
                    }
                    update = true;
                }
                places.get(adapterPosition).setLikes(feedItems.get(adapterPosition).likesCount);
                Place temp = places.get(adapterPosition);
                String placeid = temp.getObjectId();
                temp.update(context, placeid, new UpdateListener() {
                    @Override
                    public void onSuccess() {
                        Log.i("bmob","点赞数更新成功");
                    }

                    @Override
                    public void onFailure(int i, String s) {
                        Log.i("bmob","点赞更新失败："+i+","+s);
                    }
                });
                updateUser(update,placeid);
            }
        });

    }
    public void updateUser(boolean update,String favid){
        //更新用户点赞列表
        List<String> id = user.getFavplaceid();
        if (id == null) {
            id = new ArrayList<>();
        }
        if (update) {
            id.add(favid);
        } else {
            int i;
            for (i=0;i<id.size();i++) {
                if (id.get(i).equals(favid)){
                    break;
                }
            }
            id.remove(i);
        }
        user.setFavplaceid(id);
        user.update(context, user.getObjectId(), new UpdateListener() {
            @Override
            public void onSuccess() {
                Log.i("bmob","点赞列表更新成功");
            }

            @Override
            public void onFailure(int i, String s) {
                Log.i("bmob","点赞列表更新失败："+i+","+s);
            }
        });
    }


    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int position) {
        ((CellFeedViewHolder) viewHolder).bindView(feedItems.get(position));

        if (getItemViewType(position) == VIEW_TYPE_LOADER) {
            bindLoadingFeedItem((LoadingCellFeedViewHolder) viewHolder);
        }
    }

    private void bindLoadingFeedItem(final LoadingCellFeedViewHolder holder) {
        holder.loadingFeedItemView.setOnLoadingFinishedListener(new LoadingFeedItemView.OnLoadingFinishedListener() {
            @Override
            public void onLoadingFinished() {
                showLoadingView = false;
                notifyItemChanged(0);
            }
        });
        holder.loadingFeedItemView.startLoading();
    }

    @Override
    public int getItemViewType(int position) {
        if (showLoadingView && position == 0) {
            return VIEW_TYPE_LOADER;
        } else {
            return VIEW_TYPE_DEFAULT;
        }
    }

    @Override
    public int getItemCount() {
        return feedItems.size();
    }

    public void updateItems(boolean animated) {
        //Resources res = getResources();
        feedItems.clear();
        //开始更新
        Log.i("Debug","start update");
        for (int i = 0;i < places.size();i++) {
            Place temp = places.get(i);
            final FeedItem feedItem = new FeedItem(temp.getLikes(),temp.isliked(),temp.getName(),temp.getDescription());
            BmobFile icon = temp.getCoverPage();
            if (icon != null) {
                icon.download(this.context,new DownloadFileListener() {
                    @Override
                    public void onSuccess(String url) {
                        feedItem.setCoverpage(BitmapFactory.decodeFile(url));
                        Log.i("bmob","下载成功");
                        if (url.equals("") || url.isEmpty()) {
                            //设置默认图片
                        }
                    }
                    @Override
                    public void onFailure(int arg0, String arg1) {
                        Log.i("bmob","下载失败");
                        //设置默认图片
                    }
                });
            }
            feedItems.add(feedItem);
        }
        if (animated) {
            notifyItemRangeInserted(0, feedItems.size());
        } else {
            notifyDataSetChanged();
        }
    }

    public void setOnFeedItemClickListener(OnFeedItemClickListener onFeedItemClickListener) {
        this.onFeedItemClickListener = onFeedItemClickListener;
    }

    public void showLoadingView() {
        showLoadingView = true;
        notifyItemChanged(0);
    }

    public static class CellFeedViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.ivFeedCenter)
        ImageView ivFeedCenter;
        @BindView(R.id.ivFeedBottom)
        TextView ivFeedBottom;
        @BindView(R.id.btnComments)
        ImageButton btnComments;
        @BindView(R.id.btnLike)
        ImageButton btnLike;
        @BindView(R.id.vBgLike)
        View vBgLike;
        @BindView(R.id.ivLike)
        ImageView ivLike;
        @BindView(R.id.tsLikesCounter)
        TextSwitcher tsLikesCounter;
        @BindView(R.id.ivUserProfile)
        TextView ivUserProfile;
        @BindView(R.id.vImageRoot)
        FrameLayout vImageRoot;

        FeedItem feedItem;

        public CellFeedViewHolder(View view) {
            super(view);
            ButterKnife.bind(this, view);
        }

        public void bindView(FeedItem feedItem) {
            this.feedItem = feedItem;
            //设置地点名
            ivUserProfile.setText(feedItem.placename);
            //设置图片
            if (feedItem.coverpage != null) {
                ivFeedCenter.setImageBitmap(feedItem.coverpage);
            } else {
                ivFeedCenter.setImageResource(R.drawable.sea);
            }
            //设置描述
            ivFeedBottom.setText(feedItem.description);
            //是否喜欢
            btnLike.setImageResource(feedItem.isLiked ? R.drawable.ic_heart_red : R.drawable.ic_heart_outline_grey);
            //点赞数
            tsLikesCounter.setCurrentText(vImageRoot.getResources().getQuantityString(
                    R.plurals.likes_count, feedItem.likesCount, feedItem.likesCount
            ));
        }

        public FeedItem getFeedItem() {
            return feedItem;
        }

    }

    public static class LoadingCellFeedViewHolder extends CellFeedViewHolder {

        LoadingFeedItemView loadingFeedItemView;

        public LoadingCellFeedViewHolder(LoadingFeedItemView view) {
            super(view);
            this.loadingFeedItemView = view;
        }

        @Override
        public void bindView(FeedItem feedItem) {
            super.bindView(feedItem);
        }
    }

    public static class FeedItem {
        public int likesCount;
        public boolean isLiked;
        public String placename;
        public String description;
        public Bitmap coverpage;

        public FeedItem(int likesCount, boolean isLiked,String placename,String description) {
            this.likesCount = likesCount;
            this.isLiked = isLiked;
            this.placename = placename;
            this.description = description;
        }
        public void setCoverpage(Bitmap bitmap){
            this.coverpage = bitmap;
        }
        public void setLikesCount(int likes) { this.likesCount = likes;}
        public void setLiked(boolean liked){ this.isLiked = liked;}
    }

    public interface OnFeedItemClickListener {
        void onCommentsClick(View v, int position);

    }
}
