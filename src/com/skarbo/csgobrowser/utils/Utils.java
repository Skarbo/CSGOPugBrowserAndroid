package com.skarbo.csgobrowser.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

import com.actionbarsherlock.view.MenuItem;
import com.skarbo.csgobrowser.R;

public class Utils {

	public enum Country {
		DE, ES, FR, GB, SE, US
	}

	public static String retrieveContent(File file) throws IOException {
		return retrieveContent(new FileInputStream(file));
	}

	public static String retrieveContent(InputStream inputStream) throws IOException {
		BufferedReader r = new BufferedReader(new InputStreamReader(inputStream));
		StringBuilder lines = new StringBuilder();
		String line;
		while ((line = r.readLine()) != null) {
			lines.append(line);
		}
		return lines.toString();
	}

	public static JSONObject mergeJsonObject(JSONObject jsonObjectLeft, JSONObject jsonObjectRight)
			throws JSONException {
		@SuppressWarnings("rawtypes")
		Iterator keys = jsonObjectRight.keys();
		while (keys.hasNext()) {
			String key = (String) keys.next();
			Object valueRight = jsonObjectRight.get(key);
			if (jsonObjectLeft.has(key)) {
				Object valueLeft = jsonObjectLeft.get(key);
				if (valueLeft instanceof JSONObject && valueRight instanceof JSONObject)
					mergeJsonObject((JSONObject) valueLeft, (JSONObject) valueRight);
				else
					jsonObjectLeft.putOpt(key, valueRight);
			} else
				jsonObjectLeft.putOpt(key, valueRight);
		}
		return jsonObjectLeft;
	}

	public static String trimWhitespace(String str) {
		if (str == null)
			return str;
		return str.replaceAll("\\s+", " ").trim();
	}

	public static int parseInt(String str) {
		return parseInt(str, 0);
	}

	public static int parseInt(String str, int def) {
		try {
			return Integer.parseInt(Utils.trimWhitespace(str));
		} catch (Exception e) {
			return def;
		}
	}

	public static double parseDouble(String str) {
		return parseDouble(str, 0.0);
	}

	public static double parseDouble(String str, double def) {
		try {
			return Double.parseDouble(Utils.trimWhitespace(str));
		} catch (Exception e) {
			return def;
		}
	}

	public static int[] parseSeconds(int seconds) {
		int[] parsedSeconds = new int[4];
		parsedSeconds[0] = seconds % 60;
		seconds /= 60;
		parsedSeconds[1] = seconds % 60;
		seconds /= 60;
		parsedSeconds[2] = seconds % 24;
		seconds /= 24;
		parsedSeconds[3] = seconds;
		return parsedSeconds;
	}

	public static boolean isEmpty(String str) {
		return str == null || str.equalsIgnoreCase("");
	}

	/**
	 * <p>
	 * Unescapes any Java literals found in the <code>String</code>. For
	 * example, it will turn a sequence of <code>'\'</code> and <code>'n'</code>
	 * into a newline character, unless the <code>'\'</code> is preceded by
	 * another <code>'\'</code>.
	 * </p>
	 * 
	 * @param str
	 *            the <code>String</code> to unescape, may be null
	 * @return a new unescaped <code>String</code>, <code>null</code> if null
	 *         string input
	 */
	public static String unescapeJava(String str) {
		if (str == null) {
			return null;
		}
		try {
			StringWriter writer = new StringWriter(str.length());
			unescapeJava(writer, str);
			return writer.toString();
		} catch (Exception e) {
			// this should never ever happen while writing to a StringWriter
			Log.e("unescapeJava", e.getMessage(), e);
			return null;
		}
	}

	/**
	 * <p>
	 * Unescapes any Java literals found in the <code>String</code> to a
	 * <code>Writer</code>.
	 * </p>
	 * 
	 * <p>
	 * For example, it will turn a sequence of <code>'\'</code> and
	 * <code>'n'</code> into a newline character, unless the <code>'\'</code> is
	 * preceded by another <code>'\'</code>.
	 * </p>
	 * 
	 * <p>
	 * A <code>null</code> string input has no effect.
	 * </p>
	 * 
	 * @param out
	 *            the <code>Writer</code> used to output unescaped characters
	 * @param str
	 *            the <code>String</code> to unescape, may be null
	 * @throws Exception
	 * @throws IllegalArgumentException
	 *             if the Writer is <code>null</code>
	 */
	public static void unescapeJava(Writer out, String str) throws Exception {
		if (out == null) {
			throw new IllegalArgumentException("The Writer must not be null");
		}
		if (str == null) {
			return;
		}
		int sz = str.length();
		StringBuffer unicode = new StringBuffer(4);
		boolean hadSlash = false;
		boolean inUnicode = false;
		for (int i = 0; i < sz; i++) {
			char ch = str.charAt(i);
			if (inUnicode) {
				// if in unicode, then we're reading unicode
				// values in somehow
				unicode.append(ch);
				if (unicode.length() == 4) {
					// unicode now contains the four hex digits
					// which represents our unicode character
					try {
						int value = Integer.parseInt(unicode.toString(), 16);
						out.write((char) value);
						unicode.setLength(0);
						inUnicode = false;
						hadSlash = false;
					} catch (NumberFormatException nfe) {
						throw new Exception("Unable to parse unicode value: " + unicode, nfe);
					}
				}
				continue;
			}
			if (hadSlash) {
				// handle an escaped value
				hadSlash = false;
				switch (ch) {
				case '\\':
					out.write('\\');
					break;
				case '\'':
					out.write('\'');
					break;
				case '\"':
					out.write('"');
					break;
				case 'r':
					out.write('\r');
					break;
				case 'f':
					out.write('\f');
					break;
				case 't':
					out.write('\t');
					break;
				case 'n':
					out.write('\n');
					break;
				case 'b':
					out.write('\b');
					break;
				case 'u': {
					// uh-oh, we're in unicode country....
					inUnicode = true;
					break;
				}
				default:
					out.write(ch);
					break;
				}
				continue;
			} else if (ch == '\\') {
				hadSlash = true;
				continue;
			}
			out.write(ch);
		}
		if (hadSlash) {
			// then we're in the weird case of a \ at the end of the
			// string, let's output it anyway.
			out.write('\\');
		}
	}

	public static void rotateMenuItem(Context context, MenuItem menuItem, boolean rotate) {
		if (menuItem == null)
			return;
		if (rotate) {
			if (menuItem.getActionView() != null)
				return;

			LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			ImageView iv = (ImageView) inflater.inflate(R.layout.action_view_refresh, null);

			Animation rotation = AnimationUtils.loadAnimation(context, R.anim.rotate);
			rotation.setRepeatCount(Animation.INFINITE);
			iv.startAnimation(rotation);

			menuItem.setActionView(iv);
		} else if (!rotate && menuItem.getActionView() != null) {
			menuItem.getActionView().clearAnimation();
			menuItem.setActionView(null);
		}
	}

	public static class TitleAcronym {
		public String title;
		public String acronym;

		public TitleAcronym(String title, String acronym) {
			this.title = title;
			this.acronym = acronym;
		}

		@Override
		public String toString() {
			return title + " (" + acronym + ")";
		}
	}

	public static Map<String, String> parseStringMap(String[] stringArray) {
		Map<String, String> outputArray = new HashMap<String, String>(stringArray.length);
		for (String entry : stringArray) {
			String[] splitResult = entry.split("\\|", 2);
			outputArray.put(splitResult[0], splitResult[1]);
		}
		return outputArray;
	}

	public static Map<String, TitleAcronym> parseTitleAcronymMap(String[] stringArray) {
		Map<String, TitleAcronym> outputArray = new HashMap<String, TitleAcronym>(stringArray.length);
		for (String entry : stringArray) {
			String[] splitResult = entry.split("\\|", 3);
			outputArray.put(splitResult[0], new TitleAcronym(splitResult[1], splitResult[2]));
		}
		return outputArray;
	}

	public static int mod(int number, int mod) {
		int result = number % mod;
		if (result < 0)
			result = mod - 1;
		return result;
	}

}
