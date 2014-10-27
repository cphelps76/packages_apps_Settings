package com.android.settings.matricom;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.settings.R;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeSet;

/**
 * Created by bmc on 6/27/14.
 */
public class PackageListAdapter extends BaseAdapter implements Runnable {
    private PackageManager mPm;
    private LayoutInflater mInflater;
    private List<PackageItem> mInstalledPackages = new LinkedList<PackageItem>();

    private boolean mShowAppInfo = false;

    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            PackageItem item = (PackageItem) msg.obj;
            int index = Collections.binarySearch(mInstalledPackages, item);
            if (index < 0) {
                mInstalledPackages.add(-index - 1, item);
            } else {
                mInstalledPackages.get(index).activityTitles.addAll(item.activityTitles);
            }
            notifyDataSetChanged();
        }
    };

    public static class PackageItem implements Comparable<PackageItem> {
        public final String packageName;
        public final CharSequence title;
        private final TreeSet<CharSequence> activityTitles = new TreeSet<CharSequence>();
        public final Drawable icon;
        public final Intent launchIntent;

        PackageItem(String packageName, CharSequence title, Drawable icon, Intent intent) {
            this.packageName = packageName;
            this.title = title;
            this.icon = icon;
            this.launchIntent = intent;
        }

        @Override
        public int compareTo(PackageItem another) {
            int result = title.toString().compareToIgnoreCase(another.title.toString());
            return result != 0 ? result : packageName.compareTo(another.packageName);
        }
    }

    public PackageListAdapter(Context context) {
        mPm = context.getPackageManager();
        mInflater = LayoutInflater.from(context);
        reloadList();
    }

    @Override
    public int getCount() {
        synchronized (mInstalledPackages) {
            return mInstalledPackages.size();
        }
    }

    @Override
    public PackageItem getItem(int position) {
        synchronized (mInstalledPackages) {
            return mInstalledPackages.get(position);
        }
    }

    public int getItemPosition(String packageName) {
        synchronized (mInstalledPackages) {
            return mInstalledPackages.indexOf(packageName);
        }
    }

    public Intent getLaunchIntent(String packageName) {
        return mPm.getLaunchIntentForPackage(packageName);
    }

    @Override
    public long getItemId(int position) {
        synchronized (mInstalledPackages) {
            // package name is guaranteed to be unique in mInstalledPackages thanks to Comparator
            return mInstalledPackages.get(position).packageName.hashCode();
        }
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView != null) {
            holder = (ViewHolder) convertView.getTag();
        } else {
            convertView = mInflater.inflate(R.layout.preference_icon, null, false);
            holder = new ViewHolder();
            convertView.setTag(holder);
            holder.title = (TextView) convertView.findViewById(com.android.internal.R.id.title);
            holder.summary = (TextView) convertView.findViewById(com.android.internal.R.id.summary);
            holder.icon = (ImageView) convertView.findViewById(R.id.icon);
        }

        PackageItem applicationInfo = getItem(position);
        holder.title.setText(applicationInfo.title);
        holder.icon.setImageDrawable(applicationInfo.icon);

        boolean needSummary = applicationInfo.activityTitles.size() > 0;
        if (applicationInfo.activityTitles.size() == 1) {
            if (TextUtils.equals(applicationInfo.title, applicationInfo.activityTitles.first())) {
                needSummary = false;
            }
        }

        if (!mShowAppInfo) {
            showInfo(false, holder);
        } else {
            if (needSummary) {
                holder.summary.setText(TextUtils.join(", ", applicationInfo.activityTitles));
                holder.summary.setVisibility(View.VISIBLE);
            } else {
                holder.summary.setVisibility(View.GONE);
            }
        }

        return convertView;
    }

    public void reloadList() {
        mInstalledPackages.clear();
        new Thread(this).start();
    }

    public void showAppInfo(boolean show) {
        mShowAppInfo = show;
    }

    private void showInfo(boolean show, ViewHolder holder) {
        if (show) {
            holder.title.setVisibility(View.VISIBLE);
            holder.summary.setVisibility(View.VISIBLE);
        } else {
            holder.title.setVisibility(View.GONE);
            holder.summary.setVisibility(View.GONE);
        }
    }

    @Override
    public void run() {
        final Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> installedAppsInfo = mPm.queryIntentActivities(mainIntent, 0);

        for (ResolveInfo info : installedAppsInfo) {
            ApplicationInfo appInfo = info.activityInfo.applicationInfo;
            final PackageItem item = new PackageItem(appInfo.packageName,
                    appInfo.loadLabel(mPm), appInfo.loadIcon(mPm),
                    mPm.getLaunchIntentForPackage(appInfo.packageName));
            item.activityTitles.add(info.loadLabel(mPm));
            mHandler.obtainMessage(0, item).sendToTarget();
        }
    }

    private static class ViewHolder {
        TextView title;
        TextView summary;
        ImageView icon;
    }
}