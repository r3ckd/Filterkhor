package net.postmodernapps.filterkhor.uiutil;

import java.lang.reflect.Method;
import java.text.DateFormat;
import java.util.Date;
import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.PorterDuff.Mode;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.text.InputType;
import android.text.SpannableStringBuilder;
import android.text.style.CharacterStyle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;

import net.postmodernapps.filterkhor.App;

public class UIHelpers
{
	public static final String LOGTAG = "UIHelpers";
	public static final boolean LOGGING = false;	
	
	public static int dpToPx(int dp, Context ctx)
	{
		Resources r = ctx.getResources();
		return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, r.getDisplayMetrics());
	}

	public static String dateDateDisplayString(Date date, Context context)
	{
		if (date == null)
			return "";
		return DateFormat.getDateInstance().format(date);
	}

	public static String dateTimeDisplayString(Date date, Context context)
	{
		if (date == null)
			return "";
		return DateFormat.getTimeInstance().format(date);
	}
	
	public static String dateDiffDisplayString(Date date, Context context, int idStringNever, int idStringRecently, int idStringMinutes, int idStringMinute,
			int idStringHours, int idStringHour, int idStringDays, int idStringDay)
	{
		if (date == null)
			return "";

		Date todayDate = new Date();
		double ti = todayDate.getTime() - date.getTime();
		if (ti < 0)
			ti = -ti;
		ti = ti / 1000; // Convert to seconds
		if (ti < 1)
		{
			return context.getString(idStringNever);
		}
		else if (ti < 60)
		{
			return context.getString(idStringRecently);
		}
		else if (ti < 3600 && (int) Math.round(ti / 60) < 60)
		{
			int diff = (int) Math.round(ti / 60);
			if (diff == 1)
				return context.getString(idStringMinute, diff);
			return context.getString(idStringMinutes, diff);
		}
		else if (ti < 86400 && (int) Math.round(ti / 60 / 60) < 24)
		{
			int diff = (int) Math.round(ti / 60 / 60);
			if (diff == 1)
				return context.getString(idStringHour, diff);
			return context.getString(idStringHours, diff);
		}
		else
		// if (ti < 2629743)
		{
			int diff = (int) Math.round(ti / 60 / 60 / 24);
			if (diff == 1)
				return context.getString(idStringDay, diff);
			return context.getString(idStringDays, diff);
		}
		// else
		// {
		// return context.getString(idStringNever);
		// }
	}

	public static int getRelativeLeft(View myView)
	{
		if (myView.getParent() == myView.getRootView())
			return myView.getLeft();
		else
			return myView.getLeft() + UIHelpers.getRelativeLeft((View) myView.getParent());
	}

	public static int getRelativeTop(View myView)
	{
		if (myView.getParent() == myView.getRootView())
			return myView.getTop();
		else
			return myView.getTop() + UIHelpers.getRelativeTop((View) myView.getParent());
	}

	/**
	 * Get the coordinates of a view relative to another anchor view. The anchor
	 * view is assumed to be in the same view tree as this view.
	 * 
	 * @param anchorView
	 *            View relative to which we are getting the coordinates
	 * @param view
	 *            The view to get the coordinates for
	 * @return A Rect containing the view bounds
	 */
	public static Rect getRectRelativeToView(View anchorView, View view)
	{
		Rect ret = new Rect(getRelativeLeft(view) - getRelativeLeft(anchorView), getRelativeTop(view) - getRelativeTop(anchorView), 0, 0);
		ret.right = ret.left + view.getWidth();
		ret.bottom = ret.top + view.getHeight();
		return ret;
	}

	public static int getStatusBarHeight(Activity activity)
	{
		Rect rectContent = new Rect();
		Window window = activity.getWindow();
		window.getDecorView().getWindowVisibleDisplayFrame(rectContent);
		return rectContent.top;
	}

	// From.
	// http://www.androidengineer.com/2010/08/easy-method-for-formatting-android.html
	/**
	 * Given either a Spannable String or a regular String and a token, apply
	 * the given CharacterStyle to the span between the tokens, and also remove
	 * tokens.
	 * <p>
	 * For example, {@code setSpanBetweenTokens("Hello ##world##!", "##",
	 * new ForegroundColorSpan(0xFFFF0000));} will return a CharSequence
	 * {@code "Hello world!"} with {@code world} in red.
	 * 
	 * @param text
	 *            The text, with the tokens, to adjust.
	 * @param token
	 *            The token string; there should be at least two instances of
	 *            token in text.
	 * @param cs
	 *            The style to apply to the CharSequence. WARNING: You cannot
	 *            send the same two instances of this parameter, otherwise the
	 *            second call will remove the original span.
	 * @return A Spannable CharSequence with the new style applied.
	 * 
	 * @see http://developer.android.com/reference/android/text/style/CharacterStyle
	 *      .html
	 */
	public static CharSequence setSpanBetweenTokens(CharSequence text, String token, CharacterStyle... cs)
	{
		// Start and end refer to the points where the span will apply
		int tokenLen = token.length();
		int start = text.toString().indexOf(token) + tokenLen;
		int end = text.toString().indexOf(token, start);

		if (start > -1 && end > -1)
		{
			// Copy the spannable string to a mutable spannable string
			SpannableStringBuilder ssb = new SpannableStringBuilder(text);
			for (CharacterStyle c : cs)
				ssb.setSpan(c, start, end, 0);

			// Delete the tokens before and after the span
			ssb.delete(end, end + tokenLen);
			ssb.delete(start - tokenLen, start);

			text = ssb;
		}

		return text;
	}

	public static void colorizeDrawable(Context context, int idAttr, Drawable drawable)
	{
		if (drawable == null)
			return;

		TypedValue outValue = new TypedValue();
		context.getTheme().resolveAttribute(idAttr, outValue, true);
		if ((outValue.data & 0xff000000) != 0)
			drawable.setColorFilter(outValue.data, Mode.SRC_ATOP);
		else
			drawable.setColorFilter(null);
	}

	public static void colorizeDrawableWithColor(Context context, int color, Drawable drawable)
	{
		if (drawable == null)
			return;
		drawable.setColorFilter(color, Mode.MULTIPLY);
	}

	public static void hideSoftKeyboard(Activity activity)
	{
		InputMethodManager inputMethodManager = (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
		View view = activity.getCurrentFocus();
		if (view != null)
			inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
	}

	public static Drawable getMirroringDrawable(Context context, Drawable drawable)
	{
		if (Build.VERSION.SDK_INT >= 19)
		{
			drawable.setAutoMirrored(true);
		}
		else
		{
			// Old API does not have the above handy function
			if (drawable instanceof BitmapDrawable)
			{
				BitmapDrawable bd = (BitmapDrawable) drawable;
				Matrix m = new Matrix();
				m.setScale(-1, 1);
				Bitmap src = bd.getBitmap();
				Bitmap dst = Bitmap.createBitmap(src, 0, 0, src.getWidth(), src.getHeight(), m, false);
				dst.setDensity(src.getDensity());
				drawable = new BitmapDrawable(context.getResources(), dst);
			}
		}
		return drawable;
	}

	public static int transformGravity(int gravity, boolean isRTL)
	{
		if (gravity == -1)
			gravity = Gravity.LEFT | Gravity.START;
		else if ((gravity & Gravity.HORIZONTAL_GRAVITY_MASK) == 0)
			gravity |= Gravity.LEFT | Gravity.START;

		if ((gravity & Gravity.START) == Gravity.START || (gravity & Gravity.LEFT) == Gravity.LEFT)
		{
			gravity &= ~Gravity.START;
			if (isRTL)
			{
				gravity &= ~Gravity.LEFT;
				gravity |= Gravity.RIGHT;
			}
			else
			{
				gravity &= ~Gravity.RIGHT;
				gravity |= Gravity.LEFT;
			}
		}
		else if ((gravity & Gravity.END) == Gravity.END || (gravity & Gravity.RIGHT) == Gravity.RIGHT)
		{
			gravity &= ~Gravity.END;
			if (isRTL)
			{
				gravity &= ~Gravity.RIGHT;
				gravity |= Gravity.LEFT;
			}
			else
			{
				gravity &= ~Gravity.LEFT;
				gravity |= Gravity.RIGHT;
			}
		}
		return gravity;
	}

	public static int transformGravityIfNeeded(int gravity)
	{
		if (Build.VERSION.SDK_INT < 17)
		{
			return UIHelpers.transformGravity(gravity, App.getInstance().isRTL());
		}
		return gravity;
	}

	public static void transformViewGravity(View view, boolean isRTL)
	{
		try
		{
			Method mGet = view.getClass().getMethod("getGravity", new Class<?>[] {});
			Method mSet = view.getClass().getMethod("setGravity", new Class<?>[] { int.class });
			Integer gravityInt = (Integer) mGet.invoke(view, (Object[]) null);
			int gravity = gravityInt.intValue();
			mSet.invoke(view, new Object[] { UIHelpers.transformGravity(gravity, isRTL) });
		}
		catch (Exception ex)
		{
		}
	}

	public static void installGravityTransformer(ViewGroup view)
	{
		view.setOnHierarchyChangeListener(new ViewGroup.OnHierarchyChangeListener()
		{
			@Override
			public void onChildViewAdded(View parent, View child)
			{
				boolean isPassword = (child instanceof TextView && (((TextView) child).getInputType() & InputType.TYPE_TEXT_VARIATION_PASSWORD) != 0);
				if (child.getLayoutParams() == null || child.getLayoutParams().width != ViewGroup.LayoutParams.MATCH_PARENT || isPassword || (Build.VERSION.SDK_INT < 11))
					UIHelpers.transformViewGravity(child, true);
			}

			@Override
			public void onChildViewRemoved(View parent, View child)
			{
			}
		});
	}

}
