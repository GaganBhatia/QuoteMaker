package com.km.quotemaker.gridquotes;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Cap;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Region;
import android.graphics.Shader.TileMode;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.dexati.adclient.AdManager;
import com.km.drawonphotolib.ColorPicker;
import com.km.drawonphotolib.bean.PropertyBean;
import com.km.drawonphotolib.brushstyles.BrushPreset;
import com.km.drawonphotolib.brushstyles.BrushPropertyListener;
import com.km.drawonphotolib.genericbrushstyles.DashBrush;
import com.km.drawonphotolib.genericbrushstyles.Drawing;
import com.km.gallerywithstickerlibrary.gallery.GalleryLibConstant;
import com.km.gallerywithstickerlibrary.gallery.GalleryTabActivity;
import com.km.gallerywithstickerlibrary.sticker.StickerCategoryActivity;
import com.km.gallerywithstickerlibrary.sticker.StickerConstant;
import com.km.quotemaker.R;
import com.km.quotemaker.gridquotes.bean.Constant;
import com.km.quotemaker.gridquotes.utils.BitmapUtil;
import com.km.quotemaker.gridquotes.utils.EffectSelectListener;
import com.km.quotemaker.gridquotes.utils.PreferenceUtil;
import com.km.quotemaker.gridquotes.utils.ProcessProgressDialog;
import com.km.quotemaker.gridquotes.utils.SaveTask;
import com.km.quotemaker.gridquotes.view.CustomTouchPath.PointInfo;
import com.km.quotemaker.gridquotes.view.ImageObjectPath;
import com.km.quotemaker.gridquotes.view.PIPBlurView;
import com.km.quotemaker.svgparser.PathInfo;
import com.km.quotemaker.svgparser.PathSegment;
import com.km.quotemaker.svgparser.SVGPathLoader;
import com.km.quotemaker.svgparser.SvgUtil;
import com.km.textartlib.TextArtLibActivity;
import com.km.textartlib.TextArtLibConstant;
import com.km.textartlib.TextArtLibTextArtView;
import com.nostra13.universalimageloader.core.ImageLoader;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

public class QuoteGridActivity extends AppCompatActivity implements OnClickListener, PIPBlurView.TapListener,
        PIPBlurView.ClickListener, EffectSelectListener, BrushPropertyListener {

    public static final int REQUEST_GALLERY_IMAGE = 100;
    private static final int ACTION_REQUEST_EFFECTS = 200;

    @Override
    public void onPanelClosed(int featureId, Menu menu) {
        super.onPanelClosed(featureId, menu);
    }

    private Point point;

    private PIPBlurView view;
    private View layouttopBarFreeHand;

    private ProgressDialog pd = null;
    protected static final int REQUEST_CUT_PHOTO = 10;
    private Object CurrentObject;
    private int resourceID = 0;
    private int choice;

    private String mOutputFilePath;
    private View layoutTextures;
    private LinearLayout containerTextures;
    private ProgressDialog mProgressDialog;
    Context mContext;
    private ProcessProgressDialog processDialog;
    private final int REQUEST_ADD_STICKER = 130;
    private final int REQUEST_ADD_TEXT = 149;
    private List<PathSegment> mPathList = new ArrayList<PathSegment>();
    private Bitmap bg_btn;
    private SVGPathLoader loader;
    private Toolbar toolbar;

    /**
     * {@inheritDoc}
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quote_grid);
        mContext = this;
        mProgressDialog = new ProgressDialog(mContext);
        mProgressDialog.setMessage(getString(R.string.msg_saving_image));
        mProgressDialog.setCancelable(false);

        loader = new SVGPathLoader(this);
        resourceID = 0;

        toolbar = (Toolbar) findViewById(R.id.sticker_action_bar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().openOptionsMenu();
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_arrow_back_black_24dp);
        getSupportActionBar().setTitle(R.string.title_picture_grid_quotes);


        Bundle bundle = getIntent().getExtras();

        if (bundle != null && bundle.containsKey("frame")) {
            resourceID = bundle.getInt("frame");
        }
        mPosition = getThumbIndex(resourceID);
        view = (PIPBlurView) findViewById(R.id.sticker);
        view.setOnTapListener(this);
        view.setOnButtonClickListener(this);

        // For Textures
        layoutTextures = findViewById(R.id.teture_option);
        containerTextures = (LinearLayout) findViewById(R.id.containerTexturesFreeForm);
        addCategories();
        layoutTextures.setVisibility(view.GONE);
        layouttopBarFreeHand = findViewById(R.id.layouttopBarFreeHand);

        WindowManager wm = ((WindowManager) getSystemService(WINDOW_SERVICE));
        Display display = wm.getDefaultDisplay();
        point = getDisplaySize(display);
        bg_btn = getBitmap(R.drawable.ic_addphoto, false);
        if (resourceID != 0) {
            Bitmap bitmap = getBitmap(R.drawable.grid_3_photo_11, true);
            view.setBitmap(bitmap);
            view.invalidate();
        }
        view.addButtons(resourceID, bg_btn, point);
        view.invalidate();

        view.getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener() {

            @Override
            public void onGlobalLayout() {
                view.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                new ChangeFrameTask().execute();
            }
        });
        pd = new ProgressDialog(QuoteGridActivity.this);
        pd.setTitle(getString(R.string.msg_plz_wait));
        pd.setCancelable(false);
        pd.setMessage(getString(R.string.msg_creating_collage));

        DashBrush circleEffectClass = new DashBrush();
        circleEffectClass.setColor(PreferenceUtil.getColor(this));
        circleEffectClass.setStrokeWidth(15);
        circleEffectClass.setRadius(15);
        circleEffectClass.setBrushType(BrushPreset.ID_DASH);
        circleEffectClass.setAlpha(Color.alpha(PreferenceUtil.getColor(this)));
        view.setDrawingObject(circleEffectClass);
        view.invalidate();

        if (AdManager.isReady(getApplication()))
            AdManager.show();
    }

    public static int getRandom(int[] array) {
        int rnd = new Random().nextInt(array.length);
        return array[rnd];
    }

    class BackgroungTask extends AsyncTask<Void, Void, Integer> {

        @Override
        protected void onPreExecute() {
            pd.show();
        }

        @Override
        protected Integer doInBackground(Void... params) {

            if (demo != null) {

                try {
                    Resources res = getResources();

                    for (int j = 0; j < demo.size(); j++) {
                        Bitmap bmp = null;
                        if (j < demo.size())
                            bmp = BitmapUtil.getBitmap(getBaseContext(), demo.get(j), 300, 300);

                        ImageObjectPath img = new ImageObjectPath(bmp, res);
                        img.setUrl(demo.get(j));
                        img.setClipping(false);
                        img.setBorder(true);
                        view.init(img);

                        view.loadImages(getBaseContext(), null, null);

                    }

                } catch (Exception e) {
                    Log.v("ERROR", e.toString());
                    return 0;
                }
            }
            return 1;
        }

        @Override
        protected void onPostExecute(Integer result) {
            if (pd != null) {
                pd.dismiss();
            }
            if (result == 0) {
                Toast.makeText(QuoteGridActivity.this, getString(R.string.msg_unable_create_collage), Toast.LENGTH_SHORT).show();
                finish();
            } else
                view.invalidate();
        }

    }

    @Deprecated
    @SuppressLint("NewApi")
    private static Point getDisplaySize(final Display display) {
        final Point point = new Point();
        try {
            display.getSize(point);
        } catch (NoSuchMethodError ignore) { // Older device
            point.x = display.getWidth();
            point.y = display.getHeight();
        }
        return point;
    }

    @Override
    public void onClickListener(int layout, int choice) {
        this.choice = choice;
        layouttopBarFreeHand.setVisibility(View.GONE);
        layoutTextures.setVisibility(View.GONE);
        view.setFreHandDrawMode(false);
        openCustomGallery();

    }

    private void openCustomGallery() {
        Intent intent = new Intent(this,
                GalleryTabActivity.class);
        intent.putExtra(GalleryLibConstant.TITLE_OF_GALLERY_SCREEN, getString(R.string.title_add_quotes_on_background));
        startActivityForResult(intent, REQUEST_GALLERY_IMAGE);
    }

    @SuppressWarnings("unchecked")
    ArrayList<String> demo;
    private Drawing mDrawingObject;
    private PropertyBean bean;
    private ColorPicker colorDialog;
    private View mView;
    private RelativeLayout item;
    protected int selectedTextureResId;
    protected int mBorderColor = Color.WHITE;
    private int mPosition;
    private ArrayList<PointF> mAddListSequence;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        try {
            if (resultCode == RESULT_OK) {

                switch (requestCode) {
                    case REQUEST_CUT_PHOTO:
                        int w1, h1;
                        w1 = point.x / 2;
                        h1 = point.y / 2;
                        Bitmap mbitmap1 = BitmapUtil.getBitmap(QuoteGridActivity.this, mOutputFilePath, w1, h1);
                        ArrayList<Object> mImages1 = view.getImages();

                        for (Object image : mImages1) {
                            if (image.equals(CurrentObject)) {
                                ((ImageObjectPath) image).setBitmap(mbitmap1);
                                ((ImageObjectPath) image).setClipping(false);
                                ((ImageObjectPath) image).setBorder(false);
                                ((ImageObjectPath) image).setFirstLoad(false);
                                ((ImageObjectPath) image).load(getResources());
                                ((ImageObjectPath) image).setUrl(mOutputFilePath);
                            }
                        }
                        view.invalidate();
                        break;
                    case 0:
                        demo = (ArrayList<String>) data.getSerializableExtra("list");
                        new BackgroungTask().execute();

                        break;

                    case REQUEST_GALLERY_IMAGE:
                        if (resultCode == RESULT_OK && data != null) {
                            // get the cropped bitmap
                            String resulPath = data.getStringExtra("path");
                            if (resulPath == null) {
                                demo = (ArrayList<String>) data.getStringArrayListExtra("image_list");

                                if (demo != null) {
                                    resulPath = demo.get(0);
                                }
                            }

                            if (resulPath != null) {
                                new AsyncTask<String, Integer, Bitmap>() {
                                    ProgressDialog pdDialog;

                                    protected void onPreExecute() {
                                        pdDialog = new ProgressDialog(QuoteGridActivity.this);
                                        pdDialog.setMessage(getString(R.string.msg_loading_picture_in_frame));
                                        pdDialog.setCancelable(false);
                                        pdDialog.show();

                                    }

                                    @Override
                                    protected Bitmap doInBackground(String... params) {
                                        lenghtyTask(params[0]);
                                        return null;
                                    }

                                    protected void onPostExecute(Bitmap result) {
                                        pdDialog.dismiss();
                                        view.invalidate();
                                        if (result != null) {
                                        }
                                    }
                                }.execute(resulPath);
                            }
                        }
                        break;
                    case ACTION_REQUEST_EFFECTS:
                        break;
                    case REQUEST_ADD_STICKER:
                        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                        ArrayList<String> stickerPath = data.getStringArrayListExtra("StickerpathList");
                        loadStickers(stickerPath);
                        break;

                    case REQUEST_ADD_TEXT:
                        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                        if (resultCode == RESULT_OK && data != null) {
                            String textUrl = data.getStringExtra("textimgurl");
                            if (textUrl != null) {
                                stickerSelect(textUrl);
                            }
                        }
                        break;
                }
            } else {
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                setResult(RESULT_CANCELED);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String getPath(Context context, Uri contentUri) {
        Cursor cursor = null;
        try {
            String[] proj = {MediaStore.Images.Media.DATA};
            cursor = context.getContentResolver().query(contentUri, proj, null, null, null);
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            Log.e("P", "" + cursor.getString(column_index));
            return cursor.getString(column_index);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private void lenghtyTask(String url) {
        Bitmap mbitmap = BitmapUtil.getBitmapFromUri(this, point.x, point.y,
                true, null, url);
        if (mPathList != null && mPathList.size() > 0) {
            Resources res = getResources();
            ImageObjectPath img = new ImageObjectPath(mbitmap, res);
            img.setUrl(url);
            RectF r = new RectF();
            Path path = new Path();
            path = mPathList.get(choice - 1).getPath();
            path.computeBounds(r, true);
            float scaleFactor = r.width() / mbitmap.getWidth();
            if (mbitmap.getHeight() * scaleFactor < r.height())
                scaleFactor = r.height() / mbitmap.getHeight();
            img.setScaleX(scaleFactor);
            img.setScaleY(scaleFactor);
            img.setBorder(false);
            img.setBmp(true);
            view.init(img);
            view.loadImages(getBaseContext(), r, path);
            view.removeButton(choice - 1);
        }
    }

    private void lenghtyTaskForAutoFill(int startIndex, int endIndex) {
        ArrayList<Integer> autoFillIndexList = new ArrayList<Integer>();
        for (int j = 0; j < mPathList.size(); j++) {
            if (mPathList.get(j).getAutofillStatus() == 1) {
                autoFillIndexList.add(j);
            }
        }
        for (int i = 0; i < autoFillIndexList.size(); i++) {
            Log.v("test", "index " + autoFillIndexList.get(i));
        }
        int couter = 0;
        if (mPathList != null && mPathList.size() > 0) {
            Resources res = getResources();
            for (int i = startIndex; i <= endIndex; i++) {

                Bitmap mbitmap = BitmapFactory.decodeResource(res, Constant.AUTO_FILL_IMAGES[i]);
                ImageObjectPath img = new ImageObjectPath(mbitmap, res);
                RectF r = new RectF();
                Path path = new Path();
                path = mPathList.get(autoFillIndexList.get(couter)).getPath();

                path.computeBounds(r, true);
                float scaleFactor = r.width() / mbitmap.getWidth();
                if (mbitmap.getHeight() * scaleFactor < r.height())
                    scaleFactor = r.height() / mbitmap.getHeight();
                img.setScaleX(scaleFactor);
                img.setScaleY(scaleFactor);
                img.setBorder(false);
                img.setAutofill(true);
                img.setBmp(true);
                view.init(img);
                view.loadImages(getBaseContext(), r, path);
                view.removeButton(autoFillIndexList.get(couter));
                couter++;
            }
        }
    }


    @Override
    public void onClick(View v) {
        int id = v.getId();
        switch (id) {
            case R.id.imageViewAddText:
                layouttopBarFreeHand.setVisibility(View.GONE);
                layoutTextures.setVisibility(View.GONE);
                view.setFreHandDrawMode(false);
                Intent intent1 = new Intent(this, TextArtLibActivity.class);

                TextArtLibActivity.TEXT_ART_OBJECT = new TextArtLibTextArtView(QuoteGridActivity.this);
                TextArtLibActivity.TEXT_ART_OBJECT.setInitialFontName("fonts/AlexBrush-Regular.ttf");
                intent1.putExtra(TextArtLibConstant.INTENT_FLAG_IS_LIST_ITEM_SELECTED, false);
                intent1.putExtra(TextArtLibConstant.INTENT_FLAG_BACK_BUTTON, R.drawable.ic_arrow_back_black_24dp);
                intent1.putExtra(TextArtLibConstant.INTENT_FLAG_SAVE_BUTTON, R.drawable.ic_check_mark);
                intent1.putExtra(TextArtLibConstant.INTENT_FLAG_SELECTED_TAB_BG_IMAGE, R.drawable.bottombar);
                intent1.putExtra(TextArtLibConstant.INTENT_FLAG_SELECTED_SUB_TAB_BG, R.drawable.bottombar);
                intent1.putExtra(TextArtLibConstant.INTENT_FLAG_EDIT, true);
                intent1.putExtra(TextArtLibConstant.INTENT_FLAG_TEXTURE_RESOURCE_ID, selectedTextureResId);
                intent1.putExtra(TextArtLibConstant.INTENT_FLAG_SCREEN_ORIENTATION_IS_LAND, false);
                intent1.putExtra(TextArtLibConstant.INTENT_FLAG_SEEKBAR_THUMB, R.drawable.thumb);
                intent1.putExtra(TextArtLibConstant.INTENT_FLAG_SEEKBAR_BG, R.drawable.seekbar);
                intent1.putExtra(TextArtLibConstant.INTENT_FLAG_EDIT_TEXT_INPUT_FIELD_IMAGE, R.drawable.inputtextfield);
                intent1.putExtra(TextArtLibConstant.INTENT_FLAG_FONT_CHECK_BOX, R.drawable.ic_checked);
                intent1.putExtra(TextArtLibConstant.INTENT_FLAG_TITLE_BAR_IMAGE, R.drawable.topbar);
                startActivityForResult(intent1, REQUEST_ADD_TEXT);
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                break;
            case R.id.imageViewDrawFreehand:
                layoutTextures.setVisibility(View.GONE);
                if (layouttopBarFreeHand.isShown()) {
                    layouttopBarFreeHand.setVisibility(View.GONE);
                    layouttopBarFreeHand.startAnimation(
                            AnimationUtils.loadAnimation(getApplicationContext(), R.anim.top_to_bottom_anim));
                } else {
                    showColorPickerDialog();
                    layouttopBarFreeHand.setVisibility(View.VISIBLE);
                    layouttopBarFreeHand.startAnimation(
                            AnimationUtils.loadAnimation(getApplicationContext(), R.anim.bottom_to_up_anim));
                }
                view.setFreHandDrawMode(true);
                break;
            case R.id.imageViewDoneClick:
                view.setFreHandDrawMode(false);
                if (layouttopBarFreeHand.isShown()) {
                    layouttopBarFreeHand.setVisibility(View.GONE);
                }
                break;
            case R.id.imageViewBrushSize:
                view.setFreHandDrawMode(true);
                showColorPickerDialog();
                break;
            case R.id.imageViewUndoClick:
                view.setFreHandDrawMode(true);
                view.onClickUndo();

                break;
            case R.id.imageViewRedoClick:
                view.setFreHandDrawMode(true);
                view.onClickRedo();
                break;

            case R.id.imageViewSticker:
                view.setFreHandDrawMode(false);
                layoutTextures.clearAnimation();
                layoutTextures.setVisibility(View.GONE);
                layouttopBarFreeHand.setVisibility(View.GONE);
                Intent i = new Intent(this,
                        StickerCategoryActivity.class);
                i.putExtra(StickerConstant.INTENT_FLAG_BACK_BUTTON, R.drawable.ic_arrow_back_black_24dp);
                i.putExtra(StickerConstant.INTENT_FLAG_DONE_BUTTON, R.drawable.ic_check_mark);
                i.putExtra(StickerConstant.INTENT_FLAG_TOP_BAR, R.drawable.topbar);
                startActivityForResult(i, REQUEST_ADD_STICKER);
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                break;

            case R.id.imageViewTexture:
                view.setFreHandDrawMode(false);
                layouttopBarFreeHand.setVisibility(View.GONE);
                layoutTextures
                        .startAnimation(AnimationUtils.loadAnimation(getApplicationContext(), R.anim.bottom_to_up_anim));
                layoutTextures.setVisibility(View.VISIBLE);
                break;

            case R.id.imageViewTextureDone:
                if (layoutTextures.isShown()) {
                    Animation animation = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.top_to_bottom_anim);
                    animation.setAnimationListener(new AnimationListener() {
                        @Override
                        public void onAnimationStart(Animation animation) {
                        }

                        @Override
                        public void onAnimationRepeat(Animation animation) {
                        }

                        @Override
                        public void onAnimationEnd(Animation animation) {
                            layoutTextures.setVisibility(View.GONE);

                        }
                    });
                    layoutTextures.startAnimation(animation);
                    view.setFreHandDrawMode(false);
                }
            default:
                break;
        }
    }

    private void addCategories() {
        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        RelativeLayout icon = null;
        for (int i = 0; i < Constant.textures_resources.length; i++) {
            icon = (RelativeLayout) inflater.inflate(R.layout.layout_category, null);
            icon.setId(1000 + i);
            icon.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    int resource = Constant.textures_resources[v.getId() - 1000];
                    selectedTextureResId = resource;
                    Bitmap texture = doSomeTricks(resource);
                    view.setTexture(texture);
                    view.invalidate();
                }
            });
            ImageView imgIcon = (ImageView) icon.findViewById(R.id.imageViewCategoryIcon);
            imgIcon.setImageResource(Constant.textures_resources[i]);
            containerTextures.addView(icon);
        }
    }


    public int getThumbIndex(int resourceId) {
        for (int i = 0; i < com.km.quotemaker.gridquotes.bean.Constant.array_free_grids_shapes.length; i++) {
            if (com.km.quotemaker.gridquotes.bean.Constant.array_free_grids_shapes[i] == resourceId) {
                return i;
            }
        }
        return 0;
    }

    protected Bitmap doSomeTricks(int resource) {
        Bitmap frame = null;
        int width, height;
        frame = getNewTextureOnPath(view.getWidth(), view.getHeight());
        width = frame.getWidth();
        height = frame.getHeight();
        Rect mImageBounds = new Rect(0, 0, width, height);
        BitmapDrawable tile = (BitmapDrawable) getResources().getDrawable(resource);
        tile.setTileModeXY(TileMode.REPEAT, TileMode.REPEAT);
        Bitmap mFogOfWar = Bitmap.createBitmap(mImageBounds.width(), mImageBounds.height(), Config.ARGB_8888);
        Canvas mFogOfWarCanvas = new Canvas(mFogOfWar);
        tile.setBounds(mImageBounds);
        tile.draw(mFogOfWarCanvas);
        tile = null;
        // Change all transparent pixels to black and all non-transparent
        // pixels
        // to transparent
        if (mFogOfWarCanvas != null && frame != null) {
            Paint paint = new Paint();
            paint.setDither(true);
            paint.setXfermode(new PorterDuffXfermode(Mode.DST_IN));
            mFogOfWarCanvas.drawBitmap(frame, null, mImageBounds, paint);
        }
        return mFogOfWar;
    }

    private Bitmap getNewTextureOnPath(int width, int height) {
        Bitmap frame = Bitmap.createBitmap(width, height, Config.ARGB_8888);
        Canvas frameCanvas = new Canvas(frame);
        Paint paintPath = new Paint();
        paintPath.setStrokeWidth(15);
        paintPath.setStyle(Style.STROKE);
        paintPath.setColor(Color.WHITE);
        paintPath.setStrokeCap(Cap.SQUARE);
        paintPath.setAntiAlias(true);
        paintPath.setDither(true);
        if (mPathList != null) {
            for (int i = 0; i < mPathList.size(); i++) {
                frameCanvas.drawPath(mPathList.get(i).getPath(), paintPath);
            }
        }
        return frame;
    }

    private Bitmap getBitmap(int resourceID, boolean scaled) {
        Bitmap bmp = null;
        try {

            BitmapFactory.Options opts = new BitmapFactory.Options();

            opts.inDither = true;
            opts.inPreferredConfig = Config.RGB_565;
            if (scaled)
                opts.inScaled = true; /* Flag for no scalling */
            else
                opts.inScaled = false; /* Flag for no scalling */
            bmp = BitmapFactory.decodeResource(getResources(), resourceID, opts);

        } catch (Exception e) {
            Log.v("KM", "Error Getting Bitmap ", e);
        }
        return bmp;
    }

    @Override
    public void onDoubleTapListener(final Object img, final PointInfo touchPoint, final int position) {
        if (!((ImageObjectPath) img).isAutofill()) {

            if (img != null) {
                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
                // set title
                alertDialogBuilder.setTitle(getString(R.string.choose_your_option));
                if (img instanceof ImageObjectPath && !((ImageObjectPath) img).isSticker()) {
                    final Path path = ((ImageObjectPath) img).getPath();
                    String str[] = new String[3];
                    str = getResources().getStringArray(R.array.Options);
                    alertDialogBuilder.setItems(str, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if (which == 0) {
                                // if this button is clicked, just close
                                // the dialog box and do nothing
                                view.delete(img);
                                // view.addButton(deletedPos);
                                if (img instanceof ImageObjectPath && ((ImageObjectPath) img).isBmp())
                                    lenghtyAddTask(touchPoint, path);
                                view.invalidate();
                            }
                        }
                    });
                } else {
                    alertDialogBuilder.setItems(R.array.Options, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if (which == 0) {
                                view.delete(img);
                                view.invalidate();
                            }
                        }
                    });
                }

                // create alert dialog
                AlertDialog alertDialog = alertDialogBuilder.create();

                // show it
                alertDialog.show();
            }
        }
    }

    private void lenghtyAddTask(PointInfo touchPoint, Path path) {
        Region r = new Region();
        RectF rectF = new RectF();
        path.computeBounds(rectF, true);
        r.setPath(path, new Region((int) rectF.left, (int) rectF.top, (int) rectF.right, (int) rectF.bottom));
        for (int i = 0; i < mAddListSequence.size(); i++) {
            boolean contains = r.contains((int) mAddListSequence.get(i).x, (int) mAddListSequence.get(i).y);
            if (contains) {
                view.addButton(i);
                break;
            }
        }
    }

    @Override
    public void onStickerSelect(int effectDrawableId) {
        Bitmap mStickerBitmap = BitmapFactory.decodeResource(getResources(), effectDrawableId);
        ImageObjectPath imgObject = new ImageObjectPath(mStickerBitmap, getResources());
        view.init(imgObject);
        imgObject.setClipping(false);
        imgObject.setSticker(true);
        int[] cord = {(view.getBitmap().getWidth() / 2), (view.getBitmap().getHeight() / 2)};
        view.loadImages(QuoteGridActivity.this, true, cord);
        view.invalidate();
    }

    @Override
    public void onBackPressed() {
        if (colorDialog != null && colorDialog.isViewVisible()) {
            item.removeView(mView);
            item.setClickable(false);
            colorDialog.setViewInvisible();
        } else if (layouttopBarFreeHand.isShown()) {
            view.setFreHandDrawMode(false);
            layouttopBarFreeHand.setVisibility(View.GONE);
        } else if (layoutTextures.isShown()) {
            layoutTextures.setVisibility(View.GONE);
        } else {
            if (AdManager.isReady(getApplication()))
                AdManager.show();
            showActivityEndAnimation();
        }
    }

    private void loadStickers(ArrayList<String> stickerPath) {

        if (stickerPath != null) {

            for (int i = 0; i < stickerPath.size(); i++) {

                com.nostra13.universalimageloader.core.ImageLoader mImageLoader = com.nostra13.universalimageloader.core.ImageLoader
                        .getInstance();
                Bitmap mStickerBitmap = mImageLoader.loadImageSync(stickerPath.get(i));

                if (mStickerBitmap != null) {
                    ImageObjectPath imgObject = new ImageObjectPath(mStickerBitmap, getResources());
                    view.init(imgObject);
                    imgObject.setSticker(true);
                    imgObject.setClipping(false);
                    int[] cord = {(view.getWidth() / 2 - mStickerBitmap.getWidth() / 2),
                            (view.getHeight() / 2 - mStickerBitmap.getHeight() / 2)};
                    view.loadImages(QuoteGridActivity.this, true, cord);
                }
            }
            view.invalidate();
        }

    }

    private void stickerSelect(String fpath) {
        Bitmap mStickerBitmap = BitmapFactory.decodeFile(fpath);

        if (mStickerBitmap != null) {
            ImageObjectPath imgObject = new ImageObjectPath(mStickerBitmap, getResources());
            view.init(imgObject);
            imgObject.setClipping(true);
            imgObject.setText(true);
            int[] cord = {(view.getWidth() / 2 - mStickerBitmap.getWidth() / 2),
                    (view.getHeight() / 2 - mStickerBitmap.getHeight() / 2)};
            view.loadImages(QuoteGridActivity.this, true, cord);
            view.invalidate();
        }
    }

    @Override
    protected void onDestroy() {
        ImageLoader.getInstance().clearDiscCache();
        ImageLoader.getInstance().clearMemoryCache();
        ImageLoader.getInstance().clearDiskCache();
        if (TextArtLibActivity.BITMAP_PREVIEW != null) {
            TextArtLibActivity.BITMAP_PREVIEW = null;
        }

        if( bg_btn!=null){
            bg_btn.recycle();
            bg_btn = null;
        }

        System.gc();
        super.onDestroy();
    }

    public void showColorPickerDialog() {
        colorDialog = new ColorPicker(QuoteGridActivity.this, PreferenceUtil.getColor(QuoteGridActivity.this), true,
                new ColorPicker.OnAmbilWarnaListener() {
                    @Override
                    public void onOk(ColorPicker dialog, int color) {

                        PreferenceUtil.setColor(QuoteGridActivity.this, color);
                        item.setClickable(false);
                    }

                    @Override
                    public void onCancel(ColorPicker dialog) {
                        item.setClickable(false);
                    }
                }, QuoteGridActivity.this, bean);
        if (colorDialog.isViewVisible()) {
            item.removeView(mView);
            colorDialog.setViewInvisible();
        } else {
            mView = colorDialog.show();
            item = (RelativeLayout) findViewById(R.id.colorRelative);
            item.addView(mView);
            colorDialog.setViewVisible();
            item.setClickable(true);
        }
    }

    @Override
    public void onBrushSelected(Object drawingobj) {
        if (drawingobj != null) {
            view.setDrawingObject(drawingobj);
            mDrawingObject = (Drawing) drawingobj;
            int mColor = mDrawingObject.getColor();
            int mStrokeWidth = mDrawingObject.getStrokeWidth();
            int mRadius = (int) mDrawingObject.getRadius();
            int mAlpha = mDrawingObject.getAlpha();
            int mBrushStyle = mDrawingObject.getBrushType();
            bean = new PropertyBean();
            bean.setColor(mColor);
            bean.setStokewidth(mStrokeWidth);
            bean.setRadius(mRadius);
            bean.setAlpha(mAlpha);
            bean.setBrushStyle(mBrushStyle);
        }
    }

    private void loadPathList() {
        List<Path> list = new ArrayList<Path>();
        mAddListSequence = new ArrayList<PointF>();
        for (int i = 0; i < mPathList.size(); i++) {
            list.add(mPathList.get(i).getPath());
            float[] dst = new float[2];
            dst = mPathList.get(i).getBtnPos();
            PointF centers = new PointF();
            centers = view.mapPoints(point, dst[0], dst[1]);
            mPathList.get(i).setBtnPos(centers.x, centers.y);
            mAddListSequence.add(centers);
        }
        view.setPathList(list);
        view.setAddButtonList(mAddListSequence);
        view.changeFrame(resourceID);
        int startIndex = 0, endIndex = 0;
        switch (mPosition) {
            case 0:
                startIndex = 0;
                endIndex = 0;
                break;
            case 1:
                startIndex = 1;
                endIndex = 1;
                break;
            case 2:
                startIndex = 2;
                endIndex = 4;
                break;
            case 3:
                startIndex = 5;
                endIndex = 5;
                break;
            case 4:
                startIndex = 6;
                endIndex = 6;
                break;
            case 5:
                startIndex = 7;
                endIndex = 7;
                break;
            case 6:
                startIndex = 8;
                endIndex = 8;
                break;
            case 7:
                startIndex = 9;
                endIndex = 9;
                break;
            case 8:
                startIndex = 10;
                endIndex = 10;
                break;
            case 9:
                startIndex = 11;
                endIndex = 11;
                break;
        }
        lenghtyTaskForAutoFill(startIndex, endIndex);
        int resource = getRandom(Constant.textures_resources);
        selectedTextureResId = resource;
        Bitmap texture = doSomeTricks(resource);
        view.setTexture(texture);
    }

    private class ChangeFrameTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected void onPreExecute() {
            mPathList.clear();
            if (processDialog == null) {
                processDialog = new ProcessProgressDialog(QuoteGridActivity.this);
            }
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(Void... params) {
            String svgname = getResources().getResourceName(Constant.SVG_FRAME_ARRAY[mPosition]);
            int svgRes = getResources().getIdentifier(svgname, "drawable", getPackageName());
            PathInfo pathInfo = SvgUtil.readSvg(QuoteGridActivity.this, svgRes);
            mPathList = pathInfo.getPathSegments();
            mPathList = loader.preparePathRegion(point.x, point.y, mPathList);
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            loadPathList();
            if (processDialog != null) {
                processDialog.dismissDialog();
                processDialog = null;
            }
            super.onPostExecute(result);
        }

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_sticker_activity_for_love, menu);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_save_love) {

            layouttopBarFreeHand.setVisibility(View.GONE);
            layoutTextures.setVisibility(View.GONE);
            if (view.getImages().size() > 0) {
                view.setFreHandDrawMode(false);
                view.isSaveClicked = true;
                Bitmap original = view.getFinalBitmap();
                view.isSaveClicked = false;
                new com.km.quotemaker.quotes_creator.utils.SaveTask(this, original).execute();
            } else {
                Toast.makeText(QuoteGridActivity.this, getString(R.string.toast_msg_save_clicked), Toast.LENGTH_LONG)
                        .show();
            }

        } else if (id == android.R.id.home)

        {

            showActivityEndAnimation();

        }
        return false;
    }

    private void showActivityEndAnimation() {
        finish();
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
    }
}
