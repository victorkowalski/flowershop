package today.e_bash.cityrose.tools;


import android.content.Context;
import android.widget.ImageView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.module.AppGlideModule;
import ss.com.bannerslider.ImageLoadingService;

public class GlideImageLoadingService implements ImageLoadingService {
    private Context ctx;

    public GlideImageLoadingService(Context ctx) {
        this.ctx = ctx;
    }

    @Override
    public void loadImage(String url, ImageView imageView) {
        imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        Glide.with(this.ctx).load(url).into(imageView);
    }

    @Override
    public void loadImage(int resource, ImageView imageView) {
        Glide.with(this.ctx).load(resource).into(imageView);
    }

    @Override
    public void loadImage(String url, int placeHolder, int errorDrawable, ImageView imageView) {
        Glide.with(this.ctx).load(url).into(imageView);
    }
}