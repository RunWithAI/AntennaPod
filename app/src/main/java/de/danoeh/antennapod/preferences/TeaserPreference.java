package de.danoeh.antennapod.preferences;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;

import androidx.core.content.FileProvider;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import com.bumptech.glide.Glide;

import java.io.File;
import java.io.FileOutputStream;

import de.danoeh.antennapod.R;


public class TeaserPreference extends Preference {

    private ImageView myImageView;
    private String imageUrl;

    private Context context;

    public TeaserPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.context = context;
        setLayoutResource(R.layout.about_teaser);
    }

    public TeaserPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        setLayoutResource(R.layout.about_teaser);
    }

    public TeaserPreference(Context context) {
        super(context);
        this.context = context;
        setLayoutResource(R.layout.about_teaser);
    }

    public void setImageUrl(String url) {
        imageUrl = url;
        if (myImageView != null) {
            loadImage();
        }
    }

    private void loadImage() {
        Glide.with(getContext()).load(imageUrl).into(myImageView);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        myImageView = (ImageView) holder.findViewById(R.id.image_teaser);
        if (imageUrl != null) {
            loadImage();
        }
        myImageView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                saveAndShareQRCode();
                return true;
            }
        });
    }

    private void saveAndShareQRCode() {
        try {
            BitmapDrawable drawable = (BitmapDrawable) myImageView.getDrawable();
            Bitmap bitmap = drawable.getBitmap();
            // Save the image to ExternalStorage
            File cachePath = new File(context.getExternalCacheDir(), "images");
            cachePath.mkdirs();
            FileOutputStream stream = new FileOutputStream(cachePath + "/qrcode.png");
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
            stream.close();

            // Create image File Uri
            File imagePath = new File(context.getExternalCacheDir(), "images");
            File newFile = new File(imagePath, "qrcode.png");
            Uri contentUri = FileProvider.getUriForFile(context, context.getString(R.string.provider_authority), newFile);

            if (contentUri != null) {
                // Create and fire Share Intent
                Intent shareIntent = new Intent();
                shareIntent.setAction(Intent.ACTION_SEND);
                shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                shareIntent.setDataAndType(contentUri, context.getContentResolver().getType(contentUri));
                shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
                context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.transcript_about_choose_an_app)));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}