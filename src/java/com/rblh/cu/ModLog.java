package com.rblh.cu;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Typeface;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ModLog {
    static final int MAX_LINES = 8000;
    static final int MAX_DETAIL = 160;

    static class Entry {
        int level;
        String mod;
        String text;
    }

    public static File logFile() {
        return new File(Core.gameDir(), "BepInEx/LogOutput.log");
    }

    static List<Entry> parse() {
        List<Entry> list = new ArrayList<Entry>();
        File f = logFile();
        if (!f.isFile()) {
            return list;
        }
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(f));
            String line;
            int n = 0;
            while ((line = reader.readLine()) != null && n < MAX_LINES) {
                n++;
                if (line.length() < 3 || line.charAt(0) != '[') {
                    continue;
                }
                int close = line.indexOf(']');
                if (close < 0) {
                    continue;
                }
                String head = line.substring(1, close);
                String rest = line.substring(close + 1).trim();
                int colon = head.indexOf(':');
                String level = colon >= 0 ? head.substring(0, colon).trim() : head.trim();
                String mod = colon >= 0 ? head.substring(colon + 1).trim() : "";
                int lv = 0;
                if (level.equalsIgnoreCase("Error")) {
                    lv = 2;
                } else if (level.equalsIgnoreCase("Warning")) {
                    lv = 1;
                } else {
                    continue;
                }
                Entry e = new Entry();
                e.level = lv;
                e.mod = mod.length() == 0 ? "BepInEx" : mod;
                e.text = rest;
                list.add(e);
            }
        } catch (Throwable th) {
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (Throwable th) {
                }
            }
        }
        return list;
    }

    static int loadedCount() {
        File f = logFile();
        if (!f.isFile()) {
            return 0;
        }
        int count = 0;
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(f));
            String line;
            int n = 0;
            while ((line = reader.readLine()) != null && n < MAX_LINES) {
                n++;
                int idx = line.indexOf("] Loading [");
                if (idx >= 0) {
                    count++;
                }
            }
        } catch (Throwable th) {
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (Throwable th) {
                }
            }
        }
        return count;
    }

    public static String summary(Activity activity) {
        try {
            File f = logFile();
            if (!f.isFile()) {
                return "未找到 BepInEx/LogOutput.log\n启动一次游戏后会自动生成。";
            }
            List<Entry> list = parse();
            int err = 0;
            int warn = 0;
            for (int i = 0; i < list.size(); i++) {
                if (list.get(i).level == 2) {
                    err++;
                } else {
                    warn++;
                }
            }
            return "已加载插件：" + loadedCount() + " 个\n错误：" + err + "　警告：" + warn;
        } catch (Throwable th) {
            return "日志读取失败";
        }
    }

    static String translate(String str) {
        if (str == null) {
            return "";
        }
        String s = str.trim();
        String low = s.toLowerCase();
        try {
            Matcher m = Pattern.compile("(?i)could not load \\[(.+?)\\] because it has missing dependencies[:\\s]+(.+)").matcher(s);
            if (m.find()) {
                return "无法加载 [" + m.group(1).trim() + "]：缺少依赖 " + m.group(2).trim();
            }
            m = Pattern.compile("(?i)failed to load plugin[:\\s]+(.+)").matcher(s);
            if (m.find()) {
                return "插件加载失败：" + m.group(1).trim();
            }
            m = Pattern.compile("(?i)could not load file or assembly\\s*'?([^',]+)'?.*").matcher(s);
            if (m.find()) {
                return "缺少程序集/依赖：" + m.group(1).trim();
            }
            m = Pattern.compile("(?i)missing dependency[:\\s]+(.+)").matcher(s);
            if (m.find()) {
                return "缺少依赖：" + m.group(1).trim();
            }
            m = Pattern.compile("(?i)could not resolve dependency[:\\s]+(.+)").matcher(s);
            if (m.find()) {
                return "依赖解析失败：" + m.group(1).trim();
            }
            m = Pattern.compile("(?i)accesstools\\.typebyname:\\s*could not find type named (.+)").matcher(s);
            if (m.find()) {
                return "找不到类型：" + m.group(1).trim() + "（模组与游戏版本不匹配）";
            }
            m = Pattern.compile("(?i)could not find type named (.+)").matcher(s);
            if (m.find()) {
                return "找不到类型：" + m.group(1).trim() + "（模组与游戏版本不匹配）";
            }
            m = Pattern.compile("(?i)type initializer for '(.+)' threw").matcher(s);
            if (m.find()) {
                return "类型初始化失败：" + m.group(1);
            }
            m = Pattern.compile("(?i)could not load type '(.+)'").matcher(s);
            if (m.find()) {
                return "找不到类型：" + m.group(1);
            }
            m = Pattern.compile("(?i)accessory (.+?) sprite (.+?) not loaded").matcher(s);
            if (m.find()) {
                return "配件资源未加载：" + m.group(2).trim();
            }
            m = Pattern.compile("(?i)sprite (.+?) not loaded").matcher(s);
            if (m.find()) {
                return "资源未加载：" + m.group(1).trim();
            }
            if (low.contains("steamworks") && low.contains("init failed")) {
                return "Steamworks 联机组件初始化失败";
            }
            if (low.contains("init failed")) {
                return "初始化失败";
            }
            if (low.contains("deprecated")) {
                return "插件已过时，建议更新";
            }
            if (low.contains("missingmethod") || low.contains("method not found")) {
                return "找不到方法（模组与游戏版本不匹配）";
            }
            if (low.contains("nullreference")) {
                return "空引用异常（模组可能不兼容）";
            }
            if (low.contains("filenotfound")) {
                return "找不到文件（模组文件缺失）";
            }
            if (low.contains("directorynotfound")) {
                return "找不到目录";
            }
            if (low.contains("typeloadexception")) {
                return "类型加载失败（模组与游戏版本不匹配）";
            }
            if (low.contains("unauthorizedaccess")) {
                return "没有文件访问权限";
            }
            if (low.contains("badimageformat")) {
                return "文件格式不正确（可能不是有效的 dll）";
            }
            if (low.contains("outofmemory")) {
                return "内存不足";
            }
            if (low.contains("not compatible") || low.contains("incompatible")) {
                return "不兼容";
            }
        } catch (Throwable th) {
        }
        String g = s;
        g = g.replace("Could not load", "无法加载").replace("could not load", "无法加载");
        g = g.replace("Could not find", "找不到").replace("could not find", "找不到");
        g = g.replace("missing dependencies", "缺少依赖").replace("Missing dependencies", "缺少依赖");
        g = g.replace("not loaded", "未加载").replace("Not loaded", "未加载");
        g = g.replace("init failed", "初始化失败").replace("Init failed", "初始化失败");
        g = g.replace("not found", "未找到").replace("Not found", "未找到");
        g = g.replace("failed", "失败").replace("Failed", "失败");
        return g;
    }

    public static String details() {
        return detailsExt(MAX_DETAIL, false);
    }

    public static String details(boolean raw) {
        return detailsExt(MAX_DETAIL, raw);
    }

    public static String detailsExt(int limit, boolean raw) {
        StringBuilder sb = new StringBuilder();
        File f = logFile();
        if (!f.isFile()) {
            return "未找到 BepInEx/LogOutput.log\n\n路径：" + f.getAbsolutePath();
        }
        List<Entry> list = parse();
        int shown = 0;
        int hidden = 0;
        sb.append("== 错误 ==\n");
        boolean any = false;
        String lastKey = null;
        int dup = 0;
        for (int pass = 2; pass >= 1; pass--) {
            lastKey = null;
            dup = 0;
            boolean passAny = false;
            for (int i = 0; i < list.size(); i++) {
                Entry e = list.get(i);
                if (e.level != pass) {
                    continue;
                }
                String key = e.mod + "\u0001" + e.text;
                if (key.equals(lastKey)) {
                    dup++;
                    continue;
                }
                if (lastKey != null) {
                    appendDup(sb, dup);
                    dup = 0;
                }
                if (shown >= limit) {
                    hidden++;
                    lastKey = null;
                    continue;
                }
                sb.append("[").append(e.mod).append("] ").append(raw ? e.text : translate(e.text)).append("\n");
                shown++;
                passAny = true;
                lastKey = key;
            }
            if (lastKey != null) {
                appendDup(sb, dup);
            }
            if (!passAny) {
                sb.append("（无）\n");
            }
            if (pass == 2) {
                sb.append("\n== 警告 ==\n");
            }
        }
        sb.append("\n共 ").append(list.size()).append(" 条");
        if (hidden > 0) {
            sb.append("，还有 ").append(hidden).append(" 条未显示，可用「导出」查看完整内容");
        }
        return sb.toString();
    }

    private static void appendDup(StringBuilder sb, int dup) {
        if (dup > 0) {
            sb.append("    （重复 ").append(dup).append(" 次）\n");
        }
    }

    static void showRaw(Activity activity) {
        try {
            TextView tv = new TextView(activity);
            tv.setText(details(true));
            tv.setTextSize(12.0f);
            tv.setTextColor(0xFF222222);
            tv.setTypeface(Typeface.MONOSPACE);
            int p = Ui.dp(activity, 16.0f);
            tv.setPadding(p, p, p, p);
            tv.setTextIsSelectable(true);
            ScrollView scroll = new ScrollView(activity);
            scroll.addView(tv, new LinearLayout.LayoutParams(-1, -2));
            scroll.setLayoutParams(new LinearLayout.LayoutParams(-1, (int) (activity.getResources().getDisplayMetrics().heightPixels * 0.55f)));
            new AlertDialog.Builder(activity)
                    .setTitle("运行日志（原文）")
                    .setView(scroll)
                    .setPositiveButton("关闭", (DialogInterface.OnClickListener) null)
                    .show();
        } catch (Throwable th) {
            Ui.toast(activity, "日志读取失败");
        }
    }

    public static void show(final Activity activity) {
        try {
            TextView tv = new TextView(activity);
            tv.setText(details());
            tv.setTextSize(12.0f);
            tv.setTextColor(0xFF222222);
            tv.setTypeface(Typeface.MONOSPACE);
            tv.setLineSpacing(0.0f, 1.15f);
            int p = Ui.dp(activity, 16.0f);
            tv.setPadding(p, p, p, p);
            tv.setTextIsSelectable(true);
            ScrollView scroll = new ScrollView(activity);
            scroll.addView(tv, new LinearLayout.LayoutParams(-1, -2));
            scroll.setLayoutParams(new LinearLayout.LayoutParams(-1, (int) (activity.getResources().getDisplayMetrics().heightPixels * 0.55f)));
            new AlertDialog.Builder(activity)
                    .setTitle("运行日志")
                    .setView(scroll)
                    .setNegativeButton("原文", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            showRaw(activity);
                        }
                    })
                    .setPositiveButton("关闭", (DialogInterface.OnClickListener) null)
                    .setNeutralButton("导出", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            try {
                                String text = detailsExt(2000, true);
                                File dir = new File("/storage/emulated/0/Download/cu-logs");
                                dir.mkdirs();
                                File f = new File(dir, "modlog.txt");
                                java.io.FileOutputStream fos = new java.io.FileOutputStream(f);
                                fos.write(text.getBytes("UTF-8"));
                                fos.close();
                                Ui.toast(activity, "已导出：" + f.getAbsolutePath());
                            } catch (Throwable th) {
                                Ui.toast(activity, "导出失败");
                            }
                        }
                    })
                    .show();
        } catch (Throwable th) {
            Ui.toast(activity, "日志读取失败");
        }
    }
}
