package com.saggitt.omega.smartspace;

import android.animation.ValueAnimator;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Handler;
import android.os.Process;
import android.provider.AlarmClock;
import android.provider.CalendarContract;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.launcher3.BubbleTextView;
import com.android.launcher3.ItemInfo;
import com.android.launcher3.Launcher;
import com.android.launcher3.R;
import com.android.launcher3.ResourceUtils;
import com.android.launcher3.Utilities;
import com.android.launcher3.compat.LauncherAppsCompat;
import com.android.launcher3.icons.ShadowGenerator;
import com.android.launcher3.util.Themes;
import com.saggitt.omega.OmegaAppKt;
import com.saggitt.omega.OmegaLauncher;
import com.saggitt.omega.OmegaPreferences;
import com.saggitt.omega.graphics.DoubleShadowTextView;
import com.saggitt.omega.graphics.IcuDateTextView;
import com.saggitt.omega.icons.calendar.DynamicCalendar;
import com.saggitt.omega.smartspace.OmegaSmartspaceController.CardData;
import com.saggitt.omega.smartspace.OmegaSmartspaceController.Line;
import com.saggitt.omega.smartspace.OmegaSmartspaceController.WeatherData;
import com.saggitt.omega.util.OmegaUtilsKt;
import com.saggitt.omega.views.SmartspacePreview;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class SmartspaceView extends FrameLayout implements ISmartspace, ValueAnimator.AnimatorUpdateListener,
        View.OnClickListener, View.OnLongClickListener, Runnable, OmegaSmartspaceController.Listener {
    private TextView mSubtitleWeatherText;
    private final TextPaint dB;
    private TextView mTitleText;
    private ViewGroup mTitleWeatherContent;
    private ImageView mTitleWeatherIcon;
    private DoubleShadowTextView mTitleWeatherText;
    private final ColorStateList dH;
    private final int mSmartspaceBackgroundRes;
    private IcuDateTextView mClockView;
    private IcuDateTextView mClockAboveView;
    private ViewGroup mSmartspaceContent;
    private final SmartspaceController dp;
    private SmartspaceDataContainer dq;
    private BubbleTextView dr;
    private boolean ds;
    private boolean mDoubleLine;
    private final OnClickListener mCalendarClickListener;
    private final OnClickListener mClockClickListener;
    private final OnClickListener mWeatherClickListener;
    private OnClickListener mEventClickListener;
    private View mSubtitleLine;
    private ImageView mSubtitleIcon;
    private TextView mSubtitleText;
    private ViewGroup mSubtitleWeatherContent;
    private ImageView mSubtitleWeatherIcon;
    private boolean mEnableShadow;
    private final Handler mHandler;

    private OmegaSmartspaceController mController;
    private boolean mFinishedInflate;
    private boolean mWeatherAvailable;
    private OmegaPreferences mPrefs;

    private ShadowGenerator mShadowGenerator;

    private int mTitleSize;
    private int mTitleMinSize;
    private int mHorizontalPadding;
    private int mSeparatorWidth;
    private int mWeatherIconSize;

    private Paint mTextPaint = new Paint();
    private Rect mTextBounds = new Rect();

    private boolean mPerformingSetup = false;

    public SmartspaceView(final Context context, AttributeSet set) {
        super(context, set);

        mController = OmegaAppKt.getOmegaApp(context).getSmartspace();
        mPrefs = Utilities.getOmegaPrefs(context);

        mShadowGenerator = new ShadowGenerator(ResourceUtils.pxFromDp(48, getResources().getDisplayMetrics()));

        mCalendarClickListener = v -> {
            final Uri content_URI = CalendarContract.CONTENT_URI;
            final Uri.Builder appendPath = content_URI.buildUpon().appendPath("time");
            ContentUris.appendId(appendPath, System.currentTimeMillis());
            final Intent addFlags = new Intent(Intent.ACTION_VIEW)
                    .setData(appendPath.build())
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
            try {
                Launcher.getLauncher(getContext()).startActivitySafely(v, addFlags, null, null);
            } catch (ActivityNotFoundException ex) {
                LauncherAppsCompat.getInstance(getContext()).showAppDetailsForProfile(
                        new ComponentName(DynamicCalendar.GOOGLE_CALENDAR, ""), Process.myUserHandle(), null, null);
            }
        };

        mClockClickListener = v -> {
            Launcher.getLauncher(getContext()).startActivitySafely(v,
                    new Intent(AlarmClock.ACTION_SHOW_ALARMS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                            | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED), null, null);
        };

        mWeatherClickListener = v -> {
            if (mController != null)
                mController.openWeather(v);
        };

        dp = SmartspaceController.get(context);
        mHandler = new Handler();
        dH = ColorStateList.valueOf(Themes.getAttrColor(getContext(), R.attr.workspaceTextColor));
        ds = dp.cY();
        mSmartspaceBackgroundRes = R.drawable.bg_smartspace;
        dB = new TextPaint();
        dB.setTextSize((float) getResources().getDimensionPixelSize(R.dimen.smartspace_title_size));
        mEnableShadow = !Themes.getAttrBoolean(context, R.attr.isWorkspaceDarkText);

        Resources res = getResources();
        mTitleSize = res.getDimensionPixelSize(R.dimen.smartspace_title_size);
        mTitleMinSize = res.getDimensionPixelSize(R.dimen.smartspace_title_min_size);
        mHorizontalPadding = res.getDimensionPixelSize(R.dimen.smartspace_horizontal_padding);
        mSeparatorWidth = res.getDimensionPixelSize(R.dimen.smartspace_title_sep_width);
        mWeatherIconSize = res.getDimensionPixelSize(R.dimen.smartspace_title_weather_icon_size);

        setClipChildren(false);

        try {
            Launcher launcher = Launcher.getLauncher(getContext());
            if (launcher instanceof OmegaLauncher) {
                ((OmegaLauncher) launcher).registerSmartspaceView(this);
            }
        } catch (IllegalArgumentException ignored) {

        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int size = MeasureSpec.getSize(widthMeasureSpec) - mHorizontalPadding;
        if (!mDoubleLine && mClockView != null && mTitleWeatherText != null && mTitleWeatherText.getVisibility() == View.VISIBLE) {
            int textSize = mTitleSize;
            String title = mClockView.getText().toString() + mTitleWeatherText.getText().toString();
            mTextPaint.set(mClockView.getPaint());
            while (true) {
                mTextPaint.setTextSize(textSize);
                mTextPaint.getTextBounds(title, 0, title.length(), mTextBounds);
                int padding = getPaddingRight() + getPaddingLeft()
                        + mClockView.getPaddingLeft() + mClockView.getPaddingRight()
                        + mTitleWeatherText.getPaddingLeft() + mTitleWeatherText.getPaddingRight();
                if (padding + mSeparatorWidth + mWeatherIconSize + mTextBounds.width() <= size) {
                    break;
                }
                int newSize = textSize - 2;
                if (newSize < mTitleMinSize) {
                    break;
                }
                textSize = newSize;
            }
            setTitleSize(textSize);
        } else {
            setTitleSize(mTitleSize);
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    public final void setTitleSize(int size) {
        if (mClockView != null && ((int) mClockView.getTextSize()) != size) {
            mClockView.setTextSize(TypedValue.COMPLEX_UNIT_PX, size);
        }
        if (mTitleWeatherText != null && ((int) mTitleWeatherText.getTextSize()) != size) {
            mTitleWeatherText.setTextSize(TypedValue.COMPLEX_UNIT_PX, size);
        }
    }

    private void setupIfNeeded() {
        if (!mPerformingSetup && mController.getRequiresSetup()) {
            mPerformingSetup = true;
            mController.startSetup(() -> {
                mPerformingSetup = false;
                return null;
            });
        }
    }

    @Override
    public void onDataUpdated(@Nullable WeatherData weather, @Nullable CardData card) {
        if (mController.getRequiresSetup()) {
            if (getParent() instanceof SmartspacePreview) {
                setupIfNeeded();
            } else {
                List<Line> lines = new ArrayList<>();
                lines.add(new Line(getContext().getString(R.string.smartspace_setup_text)));
                card = new CardData(null, lines, v -> setupIfNeeded(), false);
            }
        }

        mEventClickListener = card != null ? card.getOnClickListener() : null;
        boolean doubleLine = card != null && card.isDoubleLine();
        if (mDoubleLine != doubleLine) {
            mDoubleLine = doubleLine;
            cs();
        }
        setOnClickListener(this);
        setOnLongClickListener(co());
        mWeatherAvailable = weather != null;
        if (mDoubleLine) {
            loadDoubleLine(weather, card);
        } else {
            loadSingleLine(weather, card);
        }
    }

    @SuppressWarnings("ConstantConditions")
    private void loadDoubleLine(@Nullable WeatherData weather, @NotNull CardData card) {
        setOnClickListener(mEventClickListener);
        setBackgroundResource(mSmartspaceBackgroundRes);
        mTitleText.setText(card.getTitle());
        mTitleText.setEllipsize(card.getTitleEllipsize());
        mSubtitleText.setText(card.getSubtitle());
        mSubtitleText.setEllipsize(card.getSubtitleEllipsize());
        mSubtitleIcon.setImageTintList(dH);
        mSubtitleIcon.setImageBitmap(card.getIcon());
        bindWeather(weather, mSubtitleWeatherContent, mSubtitleWeatherText, mSubtitleWeatherIcon);
        bindClockAbove(false);
    }

    @SuppressWarnings("ConstantConditions")
    private void loadSingleLine(@Nullable WeatherData weather, @Nullable CardData card) {
        setOnClickListener(null);
        setBackgroundResource(0);
        bindWeather(weather, mTitleWeatherContent, mTitleWeatherText, mTitleWeatherIcon);
        bindClockAndSeparator(false);
        int clockAboveTextSize;
        if (card != null) {
            mSubtitleLine.setVisibility(View.VISIBLE);
            mSubtitleText.setText(card.getTitle());
            mSubtitleText.setEllipsize(card.getTitleEllipsize());
            mSubtitleText.setOnClickListener(mEventClickListener);

            Bitmap icon = card.getIcon();
            if (icon != null) {
                mSubtitleIcon.setVisibility(View.VISIBLE);
                mSubtitleIcon.setImageTintList(dH);
                mSubtitleIcon.setImageBitmap(icon);
                mSubtitleIcon.setOnClickListener(mEventClickListener);
            } else {
                mSubtitleIcon.setVisibility(View.GONE);
            }

            clockAboveTextSize = R.dimen.smartspace_title_size;
        } else {
            mSubtitleLine.setVisibility(View.GONE);
            mSubtitleText.setOnClickListener(null);
            mSubtitleIcon.setOnClickListener(null);
            clockAboveTextSize = R.dimen.smartspace_clock_above_size;
        }
        mClockAboveView.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                getResources().getDimensionPixelSize(clockAboveTextSize));
    }

    private void bindClockAndSeparator(boolean forced) {
        if (mPrefs.getSmartspaceDate() || mPrefs.getSmartspaceTime()) {
            mClockView.setVisibility(View.VISIBLE);
            mClockView.setOnClickListener(mCalendarClickListener);
            mClockView.setOnLongClickListener(co());
            if (forced)
                mClockView.reloadDateFormat(true);
        } else {
            mClockView.setVisibility(View.GONE);
        }
        bindClockAbove(forced);
    }

    private void bindClockAbove(boolean forced) {
        if (mPrefs.getSmartspaceTime() && mPrefs.getSmartspaceTimeAbove()) {
            mClockAboveView.setVisibility(View.VISIBLE);
            mClockAboveView.setOnClickListener(mClockClickListener);
            mClockAboveView.setOnLongClickListener(co());
            if (forced)
                mClockAboveView.reloadDateFormat(true);
        } else {
            mClockAboveView.setVisibility(GONE);
        }
    }

    private void bindWeather(@Nullable WeatherData weather, View container, TextView title, ImageView icon) {
        mWeatherAvailable = weather != null;
        if (mWeatherAvailable) {
            container.setVisibility(View.VISIBLE);
            container.setOnClickListener(mWeatherClickListener);
            container.setOnLongClickListener(co());
            title.setText(weather.getTitle(
                    Utilities.getOmegaPrefs(getContext()).getWeatherUnit()));
            icon.setImageBitmap(addShadowToBitmap(weather.getIcon()));
        } else {
            container.setVisibility(View.GONE);
        }
    }

    public void reloadCustomizations() {
        if (!mDoubleLine) {
            bindClockAndSeparator(true);
        }
        bindClockAbove(true);
    }

    private Bitmap addShadowToBitmap(Bitmap bitmap) {
        if (mEnableShadow && !bitmap.isRecycled()) {
            Bitmap newBitmap = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Config.ARGB_8888);
            Canvas canvas = new Canvas(newBitmap);
            mShadowGenerator.recreateIcon(bitmap, canvas);
            return newBitmap;
        } else {
            return bitmap;
        }
    }

    private void loadViews() {
        mTitleText = findViewById(R.id.title_text);
        mSubtitleLine = findViewById(R.id.subtitle_line);
        mSubtitleText = findViewById(R.id.subtitle_text);
        mSubtitleIcon = findViewById(R.id.subtitle_icon);
        mTitleWeatherIcon = findViewById(R.id.title_weather_icon);
        mSubtitleWeatherIcon = findViewById(R.id.subtitle_weather_icon);
        mSmartspaceContent = findViewById(R.id.smartspace_content);
        mTitleWeatherContent = findViewById(R.id.title_weather_content);
        mSubtitleWeatherContent = findViewById(R.id.subtitle_weather_content);
        mTitleWeatherText = findViewById(R.id.title_weather_text);
        mSubtitleWeatherText = findViewById(R.id.subtitle_weather_text);
        mClockView = findViewById(R.id.clock);
        mClockAboveView = findViewById(R.id.time_above);
    }

    private String cn() {
        final boolean b = true;
        final SmartspaceCard dp = dq.dP;
        return dp.cC(TextUtils.ellipsize(dp.cB(b), dB, getWidth() - getPaddingLeft()
                - getPaddingRight() - getResources().getDimensionPixelSize(R.dimen.smartspace_horizontal_padding) - dB.measureText(dp.cA(b)), TextUtils.TruncateAt.END).toString());
    }

    private OnLongClickListener co() {
        return this;
    }

    private void cs() {
        final int indexOfChild = indexOfChild(mSmartspaceContent);
        removeView(mSmartspaceContent);
        final LayoutInflater from = LayoutInflater.from(getContext());
        addView(from.inflate(mDoubleLine ?
                R.layout.smartspace_twolines :
                R.layout.smartspace_singleline, this, false), indexOfChild);
        loadViews();
    }

    public void onGsaChanged() {
        ds = dp.cY();
        if (dq != null) {
            postUpdate(dq);
        } else {
            Log.d("SmartspaceView", "onGsaChanged but no data present");
        }
    }

    public void postUpdate(final SmartspaceDataContainer dq2) {
        dq = dq2;
        boolean visible = mSmartspaceContent.getVisibility() == View.VISIBLE;
        if (!visible) {
            mSmartspaceContent.setVisibility(View.VISIBLE);
            mSmartspaceContent.setAlpha(0f);
            mSmartspaceContent.animate().setDuration(200L).alpha(1f);
        }
    }

    public void onAnimationUpdate(final ValueAnimator valueAnimator) {
        invalidate();
    }

    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (mController != null && mFinishedInflate)
            mController.addListener(this);
    }

    public void onClick(final View view) {
        if (dq != null && dq.cS()) {
            dq.dP.click(view);
        }
    }

    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (mController != null)
            mController.removeListener(this);
    }

    protected void onFinishInflate() {
        super.onFinishInflate();
        loadViews();
        mFinishedInflate = true;
        dr = findViewById(R.id.dummyBubbleTextView);
        dr.setTag(new ItemInfo() {
            @Override
            public ComponentName getTargetComponent() {
                return new ComponentName(getContext(), "");
            }
        });
        dr.setContentDescription("");
        if (isAttachedToWindow() && mController != null)
            mController.addListener(this);
        else if (mController != null)
            mController.requestUpdate(this);
    }

    protected void onLayout(final boolean b, final int n, final int n2, final int n3, final int n4) {
        super.onLayout(b, n, n2, n3, n4);
        if (dq != null && dq.cS() && dq.dP.cv()) {
            final String cn = cn();
            if (!cn.equals(mTitleText.getText())) {
                mTitleText.setText(cn);
            }
        }
    }

    public boolean onLongClick(final View view) {
        TextView textView;
        if (mClockView == null || mClockView.getVisibility() != View.VISIBLE) {
            textView = mTitleText;
        } else {
            textView = mClockView;
        }
        if (view == null) {
            return false;
        }
        Rect rect = new Rect();
        float tmp = 0f;
        Launcher launcher = Launcher.getLauncher(getContext());
        launcher.getDragLayer().getDescendantRectRelativeToSelf(view, rect);
        if (textView != null) {
            Paint.FontMetrics fontMetrics = textView.getPaint().getFontMetrics();
            tmp = (((float) view.getHeight()) - (fontMetrics.bottom - fontMetrics.top)) / 2.0f;
        }
        RectF rectF = new RectF();
        float exactCenterX = rect.exactCenterX();
        rectF.right = exactCenterX;
        rectF.left = exactCenterX;
        rectF.top = 0.0f;
        rectF.bottom = ((float) rect.bottom) - tmp;
        OmegaUtilsKt.openPopupMenu(this, rectF, new SmartspacePreferencesShortcut());
        return true;
    }

    public void onPause() {
        mHandler.removeCallbacks(this);
    }

    @Override
    public void run() {
    }

    @Override
    public void setPadding(final int n, final int n2, final int n3, final int n4) {
        super.setPadding(0, 0, 0, 0);
    }
}
