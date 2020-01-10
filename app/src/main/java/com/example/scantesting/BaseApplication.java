/* 
 * Copyright 2014 ShangDao.Ltd  All rights reserved.
 * SiChuan ShangDao.Ltd PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 * 
 * @BaseApplication.java  2014-2-28 上午10:37:51 - Carson
 * @author YanXu
 * @email:981385016@qq.com
 * @version 1.0
 */

package com.example.scantesting;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Application;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.MifareClassic;
import android.nfc.tech.NfcA;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.SparseArray;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;

public class BaseApplication extends Application {

	private static BaseApplication mAppInstance;
	public static int mWidth;
	public static int mHeight;
	public Handler mHomeHandler;

	public static BaseApplication getAppContext() {
		return mAppInstance;
	}

	public static String getCMD() {
		return mAppInstance.getPackageName();
		// + mAppInstance.getString(R.string.app_id);
	}

	@Override
	public void onCreate() {
		super.onCreate();
		mAppInstance = this;
		mAppContext = getApplicationContext();
		try {
			mVersionCode = getPackageManager().getPackageInfo(getPackageName(),
					0).versionName;
		} catch (NameNotFoundException e) {
			Log.d(LOG_TAG, "Version not found.");
		}
		initDeviceType();
//		LogcatHelper.getInstance(this).start();  
	}

	private void initDeviceType() {
		DisplayMetrics dis = getResources().getDisplayMetrics();
		mWidth = dis.widthPixels;
		mHeight = dis.heightPixels;
		double x = Math.pow(mWidth / dis.xdpi, 2);
		double y = Math.pow(mHeight / dis.ydpi, 2);
		double screenInches = Math.sqrt(x + y);
		// if (screenInches > 7.0) {
		// ShareCookie.setPhoneType("1");
		// } else {
		// ShareCookie.setPhoneType("0");
		// }
	}

	// NFC

	/**
	 * True if this is the donate version of MCT.
	 */
	public static final boolean IS_DONATE_VERSION = false;
	/**
	 * The directory name of the root directory of this app (on external
	 * storage).
	 */
	public static final String HOME_DIR = "/MifareClassicTool";

	/**
	 * The directory name of the key files directory. (sub directory of
	 * {@link #HOME_DIR}.)
	 */
	public static final String KEYS_DIR = "key-files";

	/**
	 * The directory name of the dump files directory. (sub directory of
	 * {@link #HOME_DIR}.)
	 */
	public static final String DUMPS_DIR = "dump-files";


	public static final String TMP_DIR = "tmp";

	/**
	 * This file contains some standard Mifare keys.
	 * <ul>
	 * <li>0xFFFFFFFFFFFF - Unformatted, factory fresh tags.</li>
	 * <li>0xA0A1A2A3A4A5 - First sector of the tag (Mifare MAD).</li>
	 * <li>0xD3F7D3F7D3F7 - NDEF formated tags.</li>
	 * </ul>
	 */
	public static final String STD_KEYS = "std.keys";

	/**
	 * Keys taken from SLURP by Anders Sundman anders@4zm.org (and a short
	 * google search).
	 * https://github.com/4ZM/slurp/blob/master/res/xml/mifare_default_keys.xml
	 */
	public static final String STD_KEYS_EXTENDED = "extended-std.keys";

	/**
	 * Possible operations the on a Mifare Classic Tag.
	 */
	public enum Operations {
		Read, Write, Increment, DecTransRest, ReadKeyA, ReadKeyB, ReadAC, WriteKeyA, WriteKeyB, WriteAC
	}

	private static final String LOG_TAG = BaseApplication.class.getSimpleName();


	private static Tag mTag = null;

	private static byte[] mUID = null;

	private static SparseArray<byte[][]> mKeyMap = null;


	private static int mKeyMapFrom = -1;


	private static int mKeyMapTo = -1;

	/**
	 * The version code from the Android manifest.
	 */
	private static String mVersionCode;

	private static NfcAdapter mNfcAdapter;
	private static Context mAppContext;

	/**
	 * Checks if external storage is available for read and write. If not, show
	 * an error Toast.
	 *
	 * @param context
	 *            The Context in which the Toast will be shown.
	 * @return True if external storage is writable. False otherwise.
	 */
	/*public static boolean isExternalStorageWritableErrorToast(Context context) {
		if (Environment.MEDIA_MOUNTED.equals(Environment
				.getExternalStorageState())) {
			return true;
		}
		Toast.makeText(context, R.string.info_no_external_storage,
				Toast.LENGTH_LONG).show();
		return false;
	}
*/
	/**
	 * Read a file line by line. The file should be a simple text file. Empty
	 * lines and lines STARTING with "#" will not be interpreted.
	 *
	 * @param file
	 *            The file to read.
	 * @param readComments
	 *            Whether to read comments or to ignore them. Comments are lines
	 *            STARTING with "#" (and empty lines).
	 * @param context
	 *            The context in which the possible "Out of memory"-Toast will
	 *            be shown.
	 * @return Array of strings representing the lines of the file. If the file
	 *         is empty or an error occurs "null" will be returned.
	 */
	public static String[] readFileLineByLine(File file, boolean readComments,
                                              Context context) {
		BufferedReader br = null;
		String[] ret = null;
		if (file != null && file.exists()) {
			try {
				br = new BufferedReader(new FileReader(file));

				String line;
				ArrayList<String> linesArray = new ArrayList<String>();
				while ((line = br.readLine()) != null) {
					// Ignore empty lines.
					// Ignore comments if readComments == false.
					if (!line.equals("")
							&& (readComments || !line.startsWith("#"))) {
						try {
							linesArray.add(line);
						} catch (OutOfMemoryError e) {
							// Error. File is too big
							// (too many lines, out of memory).
							//Toast.makeText(context, R.string.info_file_to_big,
									//Toast.LENGTH_LONG).show();
							return null;
						}
					}
				}
				if (linesArray.size() > 0) {
					ret = linesArray.toArray(new String[linesArray.size()]);
				} else {
					ret = new String[] { "" };
				}
			} catch (Exception e) {
				Log.e(LOG_TAG,
						"Error while reading from file " + file.getPath() + ".",
						e);
				ret = null;
			} finally {
				if (br != null) {
					try {
						br.close();
					} catch (IOException e) {
						Log.e(LOG_TAG, "Error while closing file.", e);
						ret = null;
					}
				}
			}
		}
		return ret;
	}

	/**
	 * Write an array of strings (each field is one line) to a given file.
	 *
	 * @param file
	 *            The file to write to.
	 * @param lines
	 *            The lines to save.
	 * @param append
	 *            Append to file (instead of replacing its content).
	 * @return True if file writing was successful. False otherwise.
	 */
	public static boolean saveFile(File file, String[] lines, boolean append) {
		boolean noError = true;
		if (file != null && lines != null) {
			if (append) {
				// Append to a existing file.
				String[] newLines = new String[lines.length + 4];
				System.arraycopy(lines, 0, newLines, 4, lines.length);
				newLines[0] = "";
				newLines[1] = "";
				newLines[2] = "# Append #######################";
				newLines[3] = "";
				lines = newLines;
			}

			BufferedWriter bw = null;
			try {
				bw = new BufferedWriter(new FileWriter(file, append));
				int i;
				for (i = 0; i < lines.length - 1; i++) {
					bw.write(lines[i]);
					bw.newLine();
				}
				bw.write(lines[i]);
			} catch (IOException e) {
				Log.e(LOG_TAG, "Error while writing to '" + file.getName()
						+ "' file.", e);
				noError = false;

			} finally {
				if (bw != null) {
					try {
						bw.close();
					} catch (IOException e) {
						Log.e(LOG_TAG, "Error while closing file.", e);
						noError = false;
					}
				}
			}
		} else {
			noError = false;
		}
		return noError;
	}

	/**
	 * Get the shared preferences with application context for saving and
	 * loading ("global") values.
	 *
	 * @return The shared preferences object with application context.
	 */
	public static SharedPreferences getPreferences() {
		return PreferenceManager.getDefaultSharedPreferences(mAppContext);
	}

	/**
	 * Enables the NFC foreground dispatch system for the given Activity.
	 *
	 * @param targetActivity
	 *            The Activity that is in foreground and wants to have NFC
	 *            Intents.
	 * @see #disableNfcForegroundDispatch(Activity)
	 */
	/*public static void enableNfcForegroundDispatch(Activity targetActivity) {
		if (mNfcAdapter != null && mNfcAdapter.isEnabled()) {

			Intent intent = new Intent(targetActivity,
					targetActivity.getClass())
					.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
			PendingIntent pendingIntent = PendingIntent.getActivity(
					targetActivity, 0, intent, 0);
			mNfcAdapter.enableForegroundDispatch(targetActivity, pendingIntent,
					null,
					new String[][] { new String[] { NfcA.class.getName() } });
		}
	}*/

	/**
	 * Disable the NFC foreground dispatch system for the given Activity.
	 *
	 * @param targetActivity
	 *            An Activity that is in foreground and has NFC foreground
	 *            dispatch system enabled.
	 * @see (Activity)
	 */
	public static void disableNfcForegroundDispatch(Activity targetActivity) {
		if (mNfcAdapter != null && mNfcAdapter.isEnabled()) {
			//mNfcAdapter.disableForegroundDispatch(targetActivity);
		}
	}

	public static int checkMifareClassicSupport(Tag tag, Context context) {
		if (tag == null || context == null) {
			// Error.
			return -3;
		}

		if (Arrays.asList(tag.getTechList()).contains(
				MifareClassic.class.getName())) {
			// Device and tag support Mifare Classic.
			return 0;
		} else if (context.getPackageManager().hasSystemFeature(
				"com.nxp.mifare")) {
			// Tag does not support Mifare Classic.
			return -2;
		} else {
			// Check if device does not support Mifare Classic.
			// For doing so, check if the ATQA + SAK of the tag indicate that
			// it's a Mifare Classic tag.
			// See: http://www.nxp.com/documents/application_note/AN10833.pdf
			// (Table 5 and 6)
			NfcA nfca = NfcA.get(tag);
			byte[] atqa = nfca.getAtqa();
			if (atqa[1] == 0
					&& (atqa[0] == 4 || atqa[0] == (byte) 0x44 || atqa[0] == 2 || atqa[0] == (byte) 0x42)) {
				// ATQA says it is most likely a Mifare Classic tag.
				byte sak = (byte) nfca.getSak();
				if (sak == 8 || sak == 9 || sak == (byte) 0x18) {
					// SAK says it is most likely a Mifare Classic tag.
					// --> Device does not support Mifare Classic.
					return -1;
				}
			}
			// Nope, it's not the device (most likely).
			// The tag does not support Mifare Classic.
			return -2;
		}
	}


	/**
	 * Check if a (hex) string is pure hex (0-9, A-F, a-f) and 16 byte (32
	 * chars) long. If not show an error Toast in the context.
	 *
	 * @param hexString
	 *            The string to check.
	 * @param context
	 *            The Context in which the Toast will be shown.
	 * @return True if sting is hex an 16 Bytes long, False otherwise.
	 */
	public static boolean isHexAnd16Byte(String hexString, Context context) {
		if (hexString.matches("[0-9A-Fa-f]+") == false) {
			// Error, not hex.
			/*Toast.makeText(context, R.string.info_not_hex_data,
					Toast.LENGTH_LONG).show();*/
			return false;
		}
		if (hexString.length() != 32) {
			// Error, not 16 byte (32 chars).
			/*Toast.makeText(context, R.string.info_not_16_byte,
					Toast.LENGTH_LONG).show();*/
			return false;
		}
		return true;
	}

	/**
	 * Check if the given block (hex string) is a value block. NXP has PDFs
	 * describing what value blocks are. Google something like
	 * "nxp mifare classic value block" if you want to have a closer look.
	 *
	 * @param hexString
	 *            Block data as hex string.
	 * @return True if it is a value block. False otherwise.
	 */
	public static boolean isValueBlock(String hexString) {
		byte[] b = BaseApplication.hexStringToByteArray(hexString);
		if (b.length == 16) {
			// Google some NXP info PDFs about Mifare Classic to see how
			// Value Blocks are formated.
			// For better reading (~ = invert operator):
			// if (b0=b8 and b0=~b4) and (b1=b9 and b9=~b5) ...
			// ... and (b12=b14 and b13=b15 and b12=~b13) then
			if ((b[0] == b[8] && (byte) (b[0] ^ 0xFF) == b[4])
					&& (b[1] == b[9] && (byte) (b[1] ^ 0xFF) == b[5])
					&& (b[2] == b[10] && (byte) (b[2] ^ 0xFF) == b[6])
					&& (b[3] == b[11] && (byte) (b[3] ^ 0xFF) == b[7])
					&& (b[12] == b[14] && b[13] == b[15] && (byte) (b[12] ^ 0xFF) == b[13])) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Check if all blocks (lines) contain valid data.
	 *
	 * @param lines
	 *            Blocks (incl. their sector header, e.g. "+Sector: 1").
	 * @param ignoreAsterisk
	 *            Ignore lines starting with "*" and move on to the next sector
	 *            (header).
	 * @return <ul>
	 *         <li>0 - Everything is (most likely) O.K.</li>
	 *         <li>1 - Found a sector that hat not 4 or 16 blocks.</li>
	 *         <li>2 - Found a block that has invalid characters (not hex or "-"
	 *         as marker for no key/no data).</li>
	 *         <li>3 - Found a block that has not 16 bytes (32 chars).</li>
	 *         <li>4 - A sector index is out of range.</li>
	 *         <li>5 - Found two times the same sector number (index). Maybe
	 *         this is a file containing multiple dumps (the dump
	 *         editor->save->append function was used)</li>
	 *         <li>6 - There are no lines (lines == null or len(lines) == 0).</li>
	 *         </ul>
	 */
	public static int isValidDump(String[] lines, boolean ignoreAsterisk) {
		ArrayList<Integer> knownSectors = new ArrayList<Integer>();
		int blocksSinceLastSectorHeader = 4;
		boolean is16BlockSector = false;
		if (lines == null || lines.length == 0) {
			// There are no lines.
			return 6;
		}
		for (int i = 0; i < lines.length; i++) {
			if ((is16BlockSector == false && blocksSinceLastSectorHeader == 4)
					|| (is16BlockSector && blocksSinceLastSectorHeader == 16)) {
				// A sector header is expected.
				if (lines[i].matches("^\\+Sector: [0-9]{1,2}$") == false) {
					// Not a valid sector length or not a valid sector header.
					return 1;
				}
				int sector = -1;
				try {
					sector = Integer.parseInt(lines[i].split(": ")[1]);
				} catch (Exception ex) {
					// Not a valid sector header.
					// Should not occur due to the previous check (regex).
					return 1;
				}
				if (sector < 0 || sector > 39) {
					// Sector out of range.
					return 4;
				}
				if (knownSectors.contains(sector)) {
					// Two times the same sector number (index).
					// Maybe this is a file containing multiple dumps
					// (the dump editor->save->append function was used).
					return 5;
				}
				knownSectors.add(sector);
				is16BlockSector = (sector >= 32) ? true : false;
				blocksSinceLastSectorHeader = 0;
				continue;
			}
			if (lines[i].startsWith("*") && ignoreAsterisk) {
				// Ignore line and move to the next sector.
				// (The line was a "No keys found or dead sector" message.)
				is16BlockSector = false;
				blocksSinceLastSectorHeader = 4;
				continue;
			}
			if (lines[i].matches("[0-9A-Fa-f-]+") == false) {
				// Not pure hex (or NO_DATA).
				return 2;
			}
			if (lines[i].length() != 32) {
				// Not 32 chars per line.
				return 3;
			}
			blocksSinceLastSectorHeader++;
		}
		return 0;
	}

	/**
	 * Show a Toast message with error informations according to
	 * {@link #isValidDump(String[], boolean)}.
	 *
	 * @see #isValidDump(String[], boolean)
	 */
	/*public static void isValidDumpErrorToast(int errorCode, Context context) {
		switch (errorCode) {
		case 1:
			Toast.makeText(context, R.string.info_valid_dump_not_4_or_16_lines,
					Toast.LENGTH_LONG).show();
			break;
		case 2:
			Toast.makeText(context, R.string.info_valid_dump_not_hex,
					Toast.LENGTH_LONG).show();
			break;
		case 3:
			Toast.makeText(context, R.string.info_valid_dump_not_16_bytes,
					Toast.LENGTH_LONG).show();
			break;
		case 4:
			Toast.makeText(context, R.string.info_valid_dump_sector_range,
					Toast.LENGTH_LONG).show();
			break;
		case 5:
			Toast.makeText(context, R.string.info_valid_dump_double_sector,
					Toast.LENGTH_LONG).show();
			break;
		case 6:
			Toast.makeText(context, R.string.info_valid_dump_empty_dump,
					Toast.LENGTH_LONG).show();
			break;
		}
	}*/

	/**
	 * Reverse a byte Array (e.g. Little Endian -> Big Endian). Hmpf! Java has
	 * no Array.reverse(). And I don't want to use BaseApplications.Lang
	 * (ArrayUtils) form Apache....
	 *
	 * @param array
	 *            The array to reverse (in-place).
	 */
	public static void reverseByteArrayInPlace(byte[] array) {
		for (int i = 0; i < array.length / 2; i++) {
			byte temp = array[i];
			array[i] = array[array.length - i - 1];
			array[array.length - i - 1] = temp;
		}
	}

	/**
	 * Convert an array of bytes into a string of hex values.
	 *
	 * @param bytes
	 *            Bytes to convert.
	 * @return The bytes in hex string format.
	 */
	public static String byte2HexString(byte[] bytes) {
		String ret = "";
		if (bytes != null) {
			for (Byte b : bytes) {
				ret += String.format("%02X", b.intValue() & 0xFF);
			}
		}
		return ret;
	}

	/**
	 * Convert a string of hex data into a byte array. Original author is: Dave
	 * L. (http://stackoverflow.com/a/140861).
	 *
	 * @param s
	 *            The hex string to convert
	 * @return An array of bytes with the values of the string.
	 */
	public static byte[] hexStringToByteArray(String s) {
		int len = s.length();
		byte[] data = new byte[len / 2];
		try {
			for (int i = 0; i < len; i += 2) {
				data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4) + Character
						.digit(s.charAt(i + 1), 16));
			}
		} catch (Exception e) {
			Log.d(LOG_TAG, "Argument(s) for hexStringToByteArray(String s)"
					+ "was not a hex string");
		}
		return data;
	}

	/**
	 * Create a colored string.
	 *
	 * @param data
	 *            The text to be colored.
	 * @param color
	 *            The color for the text.
	 * @return A colored string.
	 */
	public static SpannableString colorString(String data, int color) {
		SpannableString ret = new SpannableString(data);
		ret.setSpan(new ForegroundColorSpan(color), 0, data.length(), 0);
		return ret;
	}

	/**
	 * Copy a text to the Android clipboard.
	 *
	 * @param text
	 *            The text that should be stored on the clipboard.
	 * @param context
	 *            Context of the SystemService (and the Toast message that will
	 *            by shown).
	 */
	@SuppressWarnings("deprecation")
	@SuppressLint("NewApi")
	public static void copyToClipboard(String text, Context context) {
		if (text.equals("") == false) {
			if (Build.VERSION.SDK_INT >= 11) {
				// Android API level 11+.
				android.content.ClipboardManager clipboard = (android.content.ClipboardManager) context
						.getSystemService(Context.CLIPBOARD_SERVICE);
				android.content.ClipData clip = android.content.ClipData
						.newPlainText("mifare classic tool data", text);
				clipboard.setPrimaryClip(clip);
			} else {
				// Android API level 10.
				android.text.ClipboardManager clipboard = (android.text.ClipboardManager) context
						.getSystemService(Context.CLIPBOARD_SERVICE);
				clipboard.setText(text);
			}
			/*Toast.makeText(context, R.string.info_copied_to_clipboard,
					Toast.LENGTH_SHORT).show();*/
		}
	}

	/**
	 * Get the content of the Android clipboard (if it is plain text).
	 *
	 * @param context
	 *            Context of the SystemService
	 * @return The content of the Android clipboard. On error (clipboard empty,
	 *         clipboard content not plain text, etc.) null will be returned.
	 */
	@SuppressLint("NewApi")
	@SuppressWarnings("deprecation")
	public static String getFromClipboard(Context context) {
		if (Build.VERSION.SDK_INT >= 11) {
			// Android API level 11+.
			android.content.ClipboardManager clipboard = (android.content.ClipboardManager) context
					.getSystemService(Context.CLIPBOARD_SERVICE);
			if (clipboard.getPrimaryClip() != null
					&& clipboard.getPrimaryClip().getItemCount() > 0
					&& clipboard
							.getPrimaryClipDescription()
							.hasMimeType(
									android.content.ClipDescription.MIMETYPE_TEXT_PLAIN)) {
				return clipboard.getPrimaryClip().getItemAt(0).getText()
						.toString();
			}
		} else {
			// Android API level 10.
			android.text.ClipboardManager clipboard = (android.text.ClipboardManager) context
					.getSystemService(Context.CLIPBOARD_SERVICE);
			if (clipboard.hasText()) {
				return clipboard.getText().toString();
			}
		}

		// Error.
		return null;
	}

	/**
	 * Copy file.
	 *
	 * @param in
	 *            Input file (source).
	 * @param out
	 *            Output file (destination).
	 * @throws IOException
	 */
	public static void copyFile(InputStream in, OutputStream out)
			throws IOException {
		byte[] buffer = new byte[1024];
		int read;
		while ((read = in.read(buffer)) != -1) {
			out.write(buffer, 0, read);
		}
	}

	/**
	 * Get the version code.
	 *
	 * @return The version code.
	 */
	public static String getVersionCode() {
		return mVersionCode;
	}

	public static String getProperty(String key, String defaultValue) {
	    String value = defaultValue;
	    try {
	        Class<?> c = Class.forName("android.os.SystemProperties");
	        Method get = c.getMethod("get", String.class, String.class);
	        value = (String)(get.invoke(c, key, "unknown" ));
	    } catch (Exception e) {
	        e.printStackTrace();
	    } finally {
	        return value;
	    }
	}
}
