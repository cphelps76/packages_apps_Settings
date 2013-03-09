<<<<<<< HEAD
package com.android.settings.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;
import java.util.Properties;
=======

package com.android.settings.util;
>>>>>>> a0a393d... Squash Dark UI commits from Slim

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
<<<<<<< HEAD
import android.text.format.DateFormat;
import android.util.Log;
import android.widget.Toast;

import com.android.settings.util.CMDProcessor.CommandResult;

public class Helpers {

    private static final String TAG = "Helpers";
=======
import android.os.SystemProperties;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;

// don't show unavoidable warnings
@SuppressWarnings({
        "UnusedDeclaration",
        "MethodWithMultipleReturnPoints",
        "ReturnOfNull",
        "NestedAssignment",
        "DynamicRegexReplaceableByCompiledPattern",
        "BreakStatement"})
public class Helpers {
    // avoids hardcoding the tag
    private static final String TAG = Thread.currentThread().getStackTrace()[1].getClassName();

    public Helpers() {
        // dummy constructor
    }
>>>>>>> a0a393d... Squash Dark UI commits from Slim

    /**
     * Checks device for SuperUser permission
     *
     * @return If SU was granted or denied
     */
<<<<<<< HEAD
    public static boolean checkSu() {
        if (!new File("/system/bin/su").exists()
                && !new File("/system/xbin/su").exists()) {
            Log.e(TAG, "su does not exist!!!");
            return false; // tell caller to bail...
        }

        try {
            if ((new CMDProcessor().su
                    .runWaitFor("ls /data/app-private")).success()) {
                Log.i(TAG, " SU exists and we have permission");
                return true;
            } else {
                Log.i(TAG, " SU exists but we dont have permission");
                return false;
            }
        } catch (final NullPointerException e) {
            Log.e(TAG, e.getLocalizedMessage().toString());
=======
    @SuppressWarnings("MethodWithMultipleReturnPoints")
    public static boolean checkSu() {
        if (!new File("/system/bin/su").exists()
                && !new File("/system/xbin/su").exists()) {
            Log.e(TAG, "su binary does not exist!!!");
            return false; // tell caller to bail...
        }
        try {
            if (CMDProcessor.runSuCommand("ls /data/app-private").success()) {
                Log.i(TAG, " SU exists and we have permission");
                return true;
            } else {
                Log.i(TAG, " SU exists but we don't have permission");
                return false;
            }
        } catch (NullPointerException e) {
            Log.e(TAG, "NullPointer throw while looking for su binary", e);
>>>>>>> a0a393d... Squash Dark UI commits from Slim
            return false;
        }
    }

    /**
<<<<<<< HEAD
=======
     * Checks device for network connectivity
     *
     * @return If the device has data connectivity
    */
    public static boolean isNetworkAvailable(Context context) {
        boolean state = false;
        if (context != null) {
            ConnectivityManager cm =
                    (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo netInfo = cm.getActiveNetworkInfo();
            if (netInfo != null && netInfo.isConnected()) {
                Log.i(TAG, "The device currently has data connectivity");
                state = true;
            } else {
                Log.i(TAG, "The device does not currently have data connectivity");
                state = false;
            }
        }
        return state;
    }

    /**
>>>>>>> a0a393d... Squash Dark UI commits from Slim
     * Checks to see if Busybox is installed in "/system/"
     *
     * @return If busybox exists
     */
    public static boolean checkBusybox() {
        if (!new File("/system/bin/busybox").exists()
                && !new File("/system/xbin/busybox").exists()) {
            Log.e(TAG, "Busybox not in xbin or bin!");
            return false;
        }
<<<<<<< HEAD

        try {
            if (!new CMDProcessor().su.runWaitFor("busybox mount").success()) {
                Log.e(TAG, " Busybox is there but it is borked! ");
                return false;
            }
        } catch (final NullPointerException e) {
            Log.e(TAG, e.getLocalizedMessage().toString());
=======
        try {
            if (!CMDProcessor.runSuCommand("busybox mount").success()) {
                Log.e(TAG, "Busybox is there but it is borked! ");
                return false;
            }
        } catch (NullPointerException e) {
            Log.e(TAG, "NullpointerException thrown while testing busybox", e);
>>>>>>> a0a393d... Squash Dark UI commits from Slim
            return false;
        }
        return true;
    }

<<<<<<< HEAD
    public static String[] getMounts(final String path)
    {
        try
        {
            BufferedReader br = new BufferedReader(new FileReader("/proc/mounts"), 256);
            String line = null;
            while ((line = br.readLine()) != null)
            {
                if (line.contains(path))
                {
                    return line.split(" ");
                }
            }
            br.close();
        }
        catch (FileNotFoundException e) {
            Log.d(TAG, "/proc/mounts does not exist");
        }
        catch (IOException e) {
            Log.d(TAG, "Error reading /proc/mounts");
=======
    public static String[] getMounts(CharSequence path) {
        BufferedReader bufferedReader = null;
        try {
            bufferedReader = new BufferedReader(new FileReader("/proc/mounts"), 256);
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                if (line.contains(path)) {
                    return line.split(" ");
                }
            }
        } catch (FileNotFoundException ignored) {
            Log.d(TAG, "/proc/mounts does not exist");
        } catch (IOException ignored) {
            Log.d(TAG, "Error reading /proc/mounts");
        } finally {
            if (bufferedReader != null) {
                try {
                    bufferedReader.close();
                } catch (IOException ignored) {
                    // ignored
                }
            }
>>>>>>> a0a393d... Squash Dark UI commits from Slim
        }
        return null;
    }

<<<<<<< HEAD
    public static boolean getMount(final String mount)
    {
        final CMDProcessor cmd = new CMDProcessor();
        final String mounts[] = getMounts("/system");
        if (mounts != null
                && mounts.length >= 3)
        {
            final String device = mounts[0];
            final String path = mounts[1];
            final String point = mounts[2];
            if (cmd.su.runWaitFor("mount -o " + mount + ",remount -t " + point + " " + device + " " + path).success())
            {
                return true;
            }
        }
        return ( cmd.su.runWaitFor("busybox mount -o remount," + mount + " /system").success() );
    }

    public static String getFile(final String filename) {
        String s = "";
        final File f = new File(filename);

        if (f.exists() && f.canRead()) {
            try {
                final BufferedReader br = new BufferedReader(new FileReader(f),
                        256);
                String buffer = null;
                while ((buffer = br.readLine()) != null) {
                    s += buffer + "\n";
                }

                br.close();
            } catch (final Exception e) {
                Log.e(TAG, "Error reading file: " + filename, e);
                s = null;
            }
        }
        return s;
    }

    public static void writeNewFile(String filePath, String fileContents) {
        File f = new File(filePath);
        if (f.exists()) {
            f.delete();
        }

        try{
            // Create file
            FileWriter fstream = new FileWriter(f);
            BufferedWriter out = new BufferedWriter(fstream);
            out.write(fileContents);
            //Close the output stream
            out.close();
        }catch (Exception e){
            Log.d( TAG, "Failed to create " + filePath + " File contents: " + fileContents);
        }
=======
    public static boolean getMount(String mount) {
        String[] mounts = getMounts("/system");
        if (mounts != null && mounts.length >= 3) {
            String device = mounts[0];
            String path = mounts[1];
            String point = mounts[2];
            String preferredMountCmd = new String("mount -o " + mount + ",remount -t " + point + ' ' + device + ' ' + path);
            if (CMDProcessor.runSuCommand(preferredMountCmd).success()) {
                return true;
            }
        }
        String fallbackMountCmd = new String("busybox mount -o remount," + mount + " /system");
        return CMDProcessor.runSuCommand(fallbackMountCmd).success();
    }

    public static String readOneLine(String fname) {
        BufferedReader br = null;
        String line = null;
        try {
            br = new BufferedReader(new FileReader(fname), 1024);
            line = br.readLine();
        } catch (FileNotFoundException ignored) {
            Log.d(TAG, "File was not found! trying via shell...");
            return readFileViaShell(fname, true);
        } catch (IOException e) {
            Log.d(TAG, "IOException while reading system file", e);
            return readFileViaShell(fname, true);
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException ignored) {
                    // failed to close reader
                }
            }
        }
        return line;
    }

    public static String readFileViaShell(String filePath, boolean useSu) {
        String command = new String("cat " + filePath);
        return useSu ? CMDProcessor.runSuCommand(command).getStdout()
                : CMDProcessor.runShellCommand(command).getStdout();
    }

    public static boolean writeOneLine(String filename, String value) {
        FileWriter fileWriter = null;
        try {
            fileWriter = new FileWriter(filename);
            fileWriter.write(value);
        } catch (IOException e) {
            String Error = "Error writing { " + value + " } to file: " + filename;
            Log.e(TAG, Error, e);
            return false;
        } finally {
            if (fileWriter != null) {
                try {
                    fileWriter.close();
                } catch (IOException ignored) {
                    // failed to close writer
                }
            }
        }
        return true;
    }

    public static String[] getAvailableIOSchedulers() {
        String[] schedulers = null;
        String[] aux = readStringArray("/sys/block/mmcblk0/queue/scheduler");
        if (aux != null) {
            schedulers = new String[aux.length];
            for (int i = 0; i < aux.length; i++) {
                schedulers[i] = aux[i].charAt(0) == '['
                        ? aux[i].substring(1, aux[i].length() - 1)
                        : aux[i];
            }
        }
        return schedulers;
    }

    private static String[] readStringArray(String fname) {
        String line = readOneLine(fname);
        if (line != null) {
            return line.split(" ");
        }
        return null;
    }

    public static String getIOScheduler() {
        String scheduler = null;
        String[] schedulers = readStringArray("/sys/block/mmcblk0/queue/scheduler");
        if (schedulers != null) {
            for (String s : schedulers) {
                if (s.charAt(0) == '[') {
                    scheduler = s.substring(1, s.length() - 1);
                    break;
                }
            }
        }
        return scheduler;
>>>>>>> a0a393d... Squash Dark UI commits from Slim
    }

    /**
     * Long toast message
     *
<<<<<<< HEAD
     * @param c Application Context
     * @param msg Message to send
     */
    public static void msgLong(final Context c, final String msg) {
        if (c != null && msg != null) {
            Toast.makeText(c, msg.trim(), Toast.LENGTH_LONG).show();
=======
     * @param context Application Context
     * @param msg Message to send
     */
    public static void msgLong(Context context, String msg) {
        if (context != null && msg != null) {
            Toast.makeText(context, msg.trim(), Toast.LENGTH_LONG).show();
>>>>>>> a0a393d... Squash Dark UI commits from Slim
        }
    }

    /**
     * Short toast message
     *
<<<<<<< HEAD
     * @param c Application Context
     * @param msg Message to send
     */
    public static void msgShort(final Context c, final String msg) {
        if (c != null && msg != null) {
            Toast.makeText(c, msg.trim(), Toast.LENGTH_SHORT).show();
=======
     * @param context Application Context
     * @param msg Message to send
     */
    public static void msgShort(Context context, String msg) {
        if (context != null && msg != null) {
            Toast.makeText(context, msg.trim(), Toast.LENGTH_SHORT).show();
>>>>>>> a0a393d... Squash Dark UI commits from Slim
        }
    }

    /**
     * Long toast message
     *
<<<<<<< HEAD
     * @param c Application Context
     * @param msg Message to send
     */
    public static void sendMsg(final Context c, final String msg) {
        if (c != null && msg != null) {
            msgLong(c, msg);
=======
     * @param context Application Context
     * @param msg Message to send
     */
    public static void sendMsg(Context context, String msg) {
        if (context != null && msg != null) {
            msgLong(context, msg);
>>>>>>> a0a393d... Squash Dark UI commits from Slim
        }
    }

    /**
     * Return a timestamp
     *
<<<<<<< HEAD
     * @param c Application Context
     */
    public static String getTimestamp(final Context context) {
        String timestamp;
        timestamp = "unknown";
        Date now = new Date();
        java.text.DateFormat dateFormat = android.text.format.DateFormat.getDateFormat(context);
        java.text.DateFormat timeFormat = android.text.format.DateFormat.getTimeFormat(context);
        if(dateFormat != null && timeFormat != null) {
            timestamp = dateFormat.format(now) + " " + timeFormat.format(now);
=======
     * @param context Application Context
     */
    @SuppressWarnings("UnnecessaryFullyQualifiedName")
    public static String getTimestamp(Context context) {
        String timestamp = "unknown";
        Date now = new Date();
        java.text.DateFormat dateFormat = android.text.format.DateFormat.getDateFormat(context);
        java.text.DateFormat timeFormat = android.text.format.DateFormat.getTimeFormat(context);
        if (dateFormat != null && timeFormat != null) {
            timestamp = dateFormat.format(now) + ' ' + timeFormat.format(now);
>>>>>>> a0a393d... Squash Dark UI commits from Slim
        }
        return timestamp;
    }

<<<<<<< HEAD
    public static boolean isPackageInstalled(final String packageName,
            final PackageManager pm)
    {
        String mVersion;
        try {
            mVersion = pm.getPackageInfo(packageName, 0).versionName;
            if (mVersion.equals(null)) {
                return false;
            }
        } catch (NameNotFoundException e) {
=======
    public static boolean isPackageInstalled(String packageName, PackageManager pm) {
        try {
            String mVersion = pm.getPackageInfo(packageName, 0).versionName;
            if (mVersion == null) {
                return false;
            }
        } catch (NameNotFoundException notFound) {
            Log.e(TAG, "Package could not be found!", notFound);
>>>>>>> a0a393d... Squash Dark UI commits from Slim
            return false;
        }
        return true;
    }

    public static void restartSystemUI() {
<<<<<<< HEAD
        new CMDProcessor().su.run("pkill -TERM -f com.android.systemui");
    }

    public static void setSystemProp(String prop, String val) {
        new CMDProcessor().su.run("setprop " + prop + " " + val);
    }

    public static String getSystemProp(String prop, String def) {
        String result = getSystemProp(prop);
        return result == null ? def : result;
    }

    private static String getSystemProp(String prop) {
        CommandResult cr = new CMDProcessor().sh.runWaitFor("getprop " + prop);
        if (cr.success()) {
            return cr.stdout;
        } else {
            return null;
        }
    }

    /*
     * Mount System partition
     *
     * @param read_value ro for ReadOnly and rw for Read/Write
     *
     * @returns true for successful mount
     */
    public static boolean mountSystem(String read_value) {
        String REMOUNT_CMD = "busybox mount -o %s,remount -t yaffs2 /dev/block/mtdblock1 /system";
        final CMDProcessor cmd = new CMDProcessor();
        Log.d(TAG, "Remounting /system " + read_value);
        return cmd.su.runWaitFor(String.format(REMOUNT_CMD, read_value)).success();
    }

    /*
     * Find value of build.prop item (/system can be ro or rw)
     *
     * @param prop /system/build.prop property name to find value of
     *
     * @returns String value of @param:prop
     */
    public static String findBuildPropValueOf(String prop) {
        String mBuildPath = "/system/build.prop";
        String DISABLE = "disable";
        String value = null;
        try {
            //create properties construct and load build.prop
            Properties mProps = new Properties();
            mProps.load(new FileInputStream(mBuildPath));
            //get the property
            value = mProps.getProperty(prop, DISABLE);
            Log.d(TAG, String.format("Helpers:findBuildPropValueOf found {%s} with the value (%s)", prop, value));
        } catch (IOException ioe) {
            Log.d(TAG, "failed to load input stream");
        } catch (NullPointerException npe) {
            //swallowed thrown by ill formatted requests
        }

        if (value != null) {
            return value;
        } else {
            return DISABLE;
        }
    }

    // find value of /sys/kernel/fast_charge/force_fast_charge
    public static int isFastCharge() {
        int onOff = 0;
        String line = "";
        final String filename = "/sys/kernel/fast_charge/force_fast_charge";
        final File f = new File(filename);

        if (f.exists() && f.canRead()) {
            try {
                final BufferedReader br = new BufferedReader(new FileReader(f), 256);
                String buffer = null;
                while ((buffer = br.readLine()) != null) {
                    line += buffer + "\n";
                    try {
                        onOff = Integer.parseInt(buffer);
                    } catch (NumberFormatException nfe) {
                        onOff = 0;
                    }
                }
                br.close();
            } catch (final Exception e) {
                Log.e(TAG, "Error reading file: " + filename, e);
                onOff = 0;
            }
        }
        return onOff;
    }

    public static int isETouchWake() {
        int etouchonOff = 0;
        String line = "";
        final String filename = "/sys/class/misc/touchwake/enabled";
        final File f = new File(filename);

        if (f.exists() && f.canRead()) {
            try {
                final BufferedReader br = new BufferedReader(new FileReader(f), 256);
                String buffer = null;
                while ((buffer = br.readLine()) != null) {
                    line += buffer + "\n";
                    try {
                        etouchonOff = Integer.parseInt(buffer);
                    } catch (NumberFormatException nfe) {
                        etouchonOff = 0;
                    }
                }
                br.close();
            } catch (final Exception e) {
                Log.e(TAG, "Error reading file: " + filename, e);
                etouchonOff = 0;
            }
        }
        return etouchonOff;
    }

    public static int isESoundControl() {
        int esoundonOff = 0;
        String line = "";
        final String filename = "/sys/class/misc/soundcontrol/highperf_enabled";
        final File f = new File(filename);

        if (f.exists() && f.canRead()) {
            try {
                final BufferedReader br = new BufferedReader(new FileReader(f), 256);
                String buffer = null;
                while ((buffer = br.readLine()) != null) {
                    line += buffer + "\n";
                    try {
                        esoundonOff = Integer.parseInt(buffer);
                    } catch (NumberFormatException nfe) {
                        esoundonOff = 0;
                    }
                }
                br.close();
            } catch (final Exception e) {
                Log.e(TAG, "Error reading file: " + filename, e);
                esoundonOff = 0;
            }
        }
        return esoundonOff;
    }

=======
        CMDProcessor.startSuCommand("pkill -TERM -f com.android.systemui");
    }

    public static void setSystemProp(String prop, String val) {
        CMDProcessor.startSuCommand("setprop " + prop + " " + val);
    }

    public static String getSystemProp(String prop, String def) {
        String result = null;
        try {
            result = SystemProperties.get(prop, def);
        } catch (IllegalArgumentException iae) {
            Log.e(TAG, "Failed to get prop: " + prop);
        }
        return result == null ? def : result;
    }
>>>>>>> a0a393d... Squash Dark UI commits from Slim
}
