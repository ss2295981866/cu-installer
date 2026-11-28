package com.rblh.cu;

import android.app.Activity;
import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;

/**
 * 在线模组：索引（ETag/304 + 本地缓存）、镜像重试下载、size/sha256 校验、按包结构安装。
 */
public class Online {
    public static final String INDEX = "https://rblh.cn/cu/mods.json";
    private static final String UA = "CUInstaller/1.8 (Android)";

    private static long lastFetchAt = 0L;
    private static List<Item> memCache = null;
    private static String memEtag = null;

    public static class Item {
        public String id = "";
        public String name = "";
        public String version = "";
        public String author = "";
        public String summary = "";
        public String category = "";
        public String page = "";
        public String download = "";
        public String bepinexMin = "";
        public String sha256 = "";
        public long size = 0L;
        public List<String> mirrors = new ArrayList<String>();
        public List<String> compat = new ArrayList<String>();

        public String tagText() {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < compat.size(); i++) {
                String c = compat.get(i);
                String t = c;
                if ("release".equalsIgnoreCase(c)) {
                    t = "正式版";
                } else if (c != null && c.toLowerCase().startsWith("demo")) {
                    t = "试玩版 " + c.substring(4).replace("-", ".");
                } else if ("unknown".equalsIgnoreCase(c)) {
                    t = "兼容待确认";
                }
                if (sb.length() > 0) {
                    sb.append("  /  ");
                }
                sb.append(t);
            }
            return sb.toString();
        }

        public String stateText() {
            if (Core.bepVersion() != null && bepinexMin.length() > 0) {
                int c = Tools.cmpVer(Tools.ver(Core.bepVersion()), Tools.ver(bepinexMin));
                if (c < 0) {
                    return "需要 BepInEx " + bepinexMin + "，当前 " + Core.bepVersion();
                }
            }
            return "";
        }
    }

    private static final String HOT = "https://rblh.cn/cu/hot.json";
    private static long lastHotAt = 0L;
    private static java.util.Map<String, Integer> hotCache = null;

    /** 下载量（/cu/hot.json），失败返回空表，不影响列表。 */
    public static java.util.Map<String, Integer> counts(Context ctx) {
        long now = System.currentTimeMillis();
        if (hotCache != null && now - lastHotAt < 300000L) {
            return hotCache;
        }
        java.util.Map<String, Integer> map = new java.util.HashMap<String, Integer>();
        try {
            HttpURLConnection c = (HttpURLConnection) new URL(HOT).openConnection();
            c.setConnectTimeout(8000);
            c.setReadTimeout(10000);
            c.setRequestProperty("User-Agent", UA);
            String body = readText(c.getInputStream());
            c.disconnect();
            JSONObject root = new JSONObject(body);
            JSONArray arr = root.optJSONArray("mods");
            if (arr != null) {
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject o = arr.optJSONObject(i);
                    if (o != null) {
                        map.put(o.optString("id", ""), o.optInt("downloads", 0));
                    }
                }
            }
            lastHotAt = now;
            hotCache = map;
        } catch (Throwable th) {
        }
        return hotCache == null ? map : hotCache;
    }

    public interface Callback {
        void progress(String text);
        void done(boolean ok, String message);
    }

    /** 30 秒内不重复拉取；网络失败回退本地缓存。 */
    public static List<Item> fetch(Context ctx, boolean force) throws Exception {
        long now = System.currentTimeMillis();
        if (!force && memCache != null && now - lastFetchAt < 30000L) {
            return memCache;
        }
        File cache = new File(ctx.getCacheDir(), "mods-index.json");
        File etagFile = new File(ctx.getCacheDir(), "mods-index.etag");
        String cached = null;
        if (cache.isFile()) {
            cached = readText(cache);
        }
        String etag = etagFile.isFile() ? readText(etagFile).trim() : (memEtag == null ? "" : memEtag);
        try {
            HttpURLConnection c = (HttpURLConnection) new URL(INDEX).openConnection();
            c.setConnectTimeout(12000);
            c.setReadTimeout(20000);
            c.setRequestProperty("User-Agent", UA);
            c.setRequestProperty("Accept", "application/json");
            if (etag.length() > 0) {
                c.setRequestProperty("If-None-Match", etag);
            }
            int code = c.getResponseCode();
            if (code == 304) {
                c.disconnect();
                if (cached == null) {
                    throw new java.io.IOException("索引 304 但本地没有缓存");
                }
                lastFetchAt = now;
                memCache = parse(cached);
                return memCache;
            }
            if (code != 200) {
                c.disconnect();
                throw new java.io.IOException("索引返回 HTTP " + code);
            }
            String body = readText(c.getInputStream());
            String newEtag = c.getHeaderField("ETag");
            c.disconnect();
            writeText(cache, body);
            if (newEtag != null) {
                writeText(etagFile, newEtag);
                memEtag = newEtag;
            }
            lastFetchAt = now;
            memCache = parse(body);
            return memCache;
        } catch (Exception netErr) {
            if (cached != null) {
                lastFetchAt = now;
                memCache = parse(cached);
                return memCache;
            }
            throw netErr;
        }
    }

    private static List<Item> parse(String json) throws Exception {
        List<Item> list = new ArrayList<Item>();
        JSONObject root = new JSONObject(json);
        JSONArray arr = root.optJSONArray("mods");
        if (arr == null) {
            return list;
        }
        for (int i = 0; i < arr.length(); i++) {
            JSONObject o = arr.optJSONObject(i);
            if (o == null) {
                continue;
            }
            Item it = new Item();
            it.id = o.optString("id", "");
            it.name = o.optString("name", "");
            it.version = o.optString("version", "");
            it.author = o.optString("author", "");
            it.summary = o.optString("summary", "");
            it.category = o.optString("category", "");
            it.page = o.optString("page", "");
            it.download = o.optString("download", "");
            it.sha256 = o.optString("sha256", "");
            it.size = o.optLong("size", 0L);
            it.bepinexMin = o.optString("bepinex_min", "");
            JSONArray mirrors = o.optJSONArray("mirrors");
            if (mirrors != null) {
                for (int k = 0; k < mirrors.length(); k++) {
                    String m = mirrors.optString(k, "");
                    if (m.length() > 0) {
                        it.mirrors.add(m);
                    }
                }
            }
            if (it.download.length() == 0 && !it.mirrors.isEmpty()) {
                it.download = it.mirrors.remove(0);
            }
            JSONArray compat = o.optJSONArray("compat");
            if (compat != null) {
                for (int k = 0; k < compat.length(); k++) {
                    it.compat.add(compat.optString(k, ""));
                }
            }
            if (it.name.length() > 0 && it.download.length() > 0) {
                list.add(it);
            }
        }
        return list;
    }

    public static void install(final Activity activity, final Item item, final Callback cb) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                File tmp = new File(activity.getCacheDir(), "online_" + System.currentTimeMillis());
                File dl = new File(activity.getCacheDir(), "online_" + System.currentTimeMillis() + ext(item.download));
                try {
                    String state = item.stateText();
                    if (state.length() > 0) {
                        throw new java.io.IOException(state + "，请先在模组页更新 BepInEx");
                    }
                    report(cb, "准备下载…");
                    String usedUrl = downloadWithMirrors(item, dl, cb);
                    if (item.size > 0 && dl.length() != item.size) {
                        throw new java.io.IOException("文件大小不符：" + dl.length() + " / " + item.size + "（索引可能过期）");
                    }
                    if (item.sha256 != null && item.sha256.trim().length() > 0) {
                        report(cb, "校验中…");
                        String sha = sha256(dl);
                        if (!sha.equalsIgnoreCase(item.sha256.trim())) {
                            throw new java.io.IOException("sha256 不符，可能是站长还没更新哈希；请稍后再试或联系站长");
                        }
                    }
                    String slug = slugOf(item);
                    String msg;
                    if (dl.getName().toLowerCase().endsWith(".dll")) {
                        report(cb, "安装中…");
                        File dir = new File(Core.pluginsDir(), slug);
                        if (exists(dir)) {
                            deleteTreeRoot(dir);
                            reportDeploy(activity, emptyDir(), dir);
                        }
                        dir.mkdirs();
                        String fn = baseName(item.download);
                        reportDeploy(activity, dl, new File(dir, fn));
                        msg = "已装进 plugins/" + slug + "/" + fn;
                    } else {
                        tmp.mkdirs();
                        report(cb, "解压中…");
                        Packer.extract(dl, tmp, null);
                        msg = deploy(activity, tmp, slug, item.name);
                    }
                    Core.invalidate();
                    String dup = duplicateDlls();
                    if (dup.length() > 0) {
                        msg = msg + "\n注意：plugins 里有同名 dll（" + dup + "），可能互相覆盖";
                    }
                    done(cb, true, msg);
                } catch (Throwable th) {
                    android.util.Log.e("CUOnline", "install failed: " + item.download, th);
                    done(cb, false, String.valueOf(th));
                } finally {
                    deleteTree(tmp);
                    dl.delete();
                }
            }
        }).start();
    }

    private static String downloadWithMirrors(Item item, File out, Callback cb) throws Exception {
        List<String> urls = new ArrayList<String>();
        urls.add(item.download);
        urls.addAll(item.mirrors);
        Exception last = null;
        for (int i = 0; i < urls.size(); i++) {
            String u = urls.get(i);
            try {
                report(cb, (i == 0 ? "下载中…" : "镜像 " + i + " 重试…"));
                download(u, out);
                if (item.size > 0 && out.length() != item.size) {
                    out.delete();
                    last = new java.io.IOException("大小不符");
                    continue;
                }
                return u;
            } catch (Exception e) {
                out.delete();
                last = e;
                android.util.Log.w("CUOnline", "mirror failed: " + u + " -> " + e);
            }
        }
        throw last == null ? new java.io.IOException("下载失败") : last;
    }

    private static void download(String url, File out) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setConnectTimeout(15000);
        c.setReadTimeout(30000);
        c.setInstanceFollowRedirects(true);
        c.setRequestProperty("User-Agent", UA);
        InputStream in = c.getInputStream();
        FileOutputStream fos = new FileOutputStream(out);
        byte[] buf = new byte[65536];
        try {
            int n;
            while ((n = in.read(buf)) > 0) {
                fos.write(buf, 0, n);
            }
        } finally {
            fos.close();
            in.close();
            c.disconnect();
        }
    }

    /** 按包结构决定装到哪；返回提示文案。 */
    private static String deploy(Activity a, File tmp, String slug, String displayName) throws Exception {
        File bep = findDirNamed(tmp, "BepInEx", 0);
        if (bep != null) {
            File dst = new File(Core.gameDir(), "BepInEx");
            reportDeploy(a, bep, dst);
            return "已按游戏根目录铺开（含 BepInEx 结构）";
        }
        File base = tmp;
        String top = singleTopDir(tmp);
        if (top.length() > 0) {
            base = new File(tmp, top);
        }
        File pluginDir = new File(Core.pluginsDir(), slug);
        if (exists(pluginDir)) {
            deleteTreeRoot(pluginDir);
        }
        reportDeploy(a, base, pluginDir);
        if (!pluginDir.isDirectory()) {
            throw new java.io.IOException("安装失败：写不进 plugins/" + slug);
        }
        return "已装进 plugins/" + slug + "/（" + displayName + "）";
    }

    private static File emptyDir() throws Exception {
        File f = File.createTempFile("cu_empty", "");
        f.delete();
        f.mkdirs();
        return f;
    }

    private static void reportDeploy(Activity a, File src, File dst) throws Exception {
        if (Core.isRootPath()) {
            File parent = dst.getParentFile();
            String parentOwner = "$(stat -c %u " + Root.quote(parent.getAbsolutePath()) + ")";
            String copyCmd;
            if (src.isFile()) {
                copyCmd = "mkdir -p " + Root.quote(parent.getAbsolutePath()) + " && cp -f " + Root.quote(src.getAbsolutePath()) + " " + Root.quote(dst.getAbsolutePath());
            } else {
                copyCmd = "mkdir -p " + Root.quote(dst.getAbsolutePath()) + " && cp -rf " + Root.quote(src.getAbsolutePath() + "/.") + " " + Root.quote(dst.getAbsolutePath() + "/");
            }
            String cmd = copyCmd + " && chown -R " + parentOwner + ":" + parentOwner + " " + Root.quote(dst.getAbsolutePath());
            String out = Root.execNs(cmd);
            if (out != null && out.contains("Permission denied")) {
                throw new java.io.IOException("没有写入权限：" + out.trim());
            }
        } else if (src.isFile()) {
            File p = dst.getParentFile();
            if (p != null) {
                p.mkdirs();
            }
            FileInputStream in = new FileInputStream(src);
            FileOutputStream out = new FileOutputStream(dst);
            byte[] buf = new byte[65536];
            int n;
            while ((n = in.read(buf)) > 0) {
                out.write(buf, 0, n);
            }
            out.close();
            in.close();
        } else {
            copyTree(src, dst);
        }
    }

    /** 所有文件都在同一个顶层目录时返回该目录名，否则返回空。 */
    private static String singleTopDir(File dir) {
        File[] children = dir.listFiles();
        if (children == null || children.length == 0) {
            return "";
        }
        String only = null;
        for (int i = 0; i < children.length; i++) {
            if (children[i].getName().startsWith(".")) {
                continue;
            }
            if (!children[i].isDirectory()) {
                return "";
            }
            if (only == null) {
                only = children[i].getName();
            } else {
                return "";
            }
        }
        return only == null ? "" : only;
    }

    private static boolean exists(File f) {
        return Core.isRootPath() ? Root.exists(f.getAbsolutePath()) : f.exists();
    }

    private static void deleteTreeRoot(File f) {
        if (Core.isRootPath()) {
            Root.execNs("rm -rf " + Root.quote(f.getAbsolutePath()));
        } else {
            deleteTree(f);
        }
    }

    private static String duplicateDlls() {
        List<File> all = new ArrayList<File>();
        collectDlls(Core.pluginsDir(), all);
        java.util.Map<String, Integer> counts = new java.util.HashMap<String, Integer>();
        for (int i = 0; i < all.size(); i++) {
            String n = all.get(i).getName().toLowerCase();
            Integer v = counts.get(n);
            counts.put(n, v == null ? 1 : v + 1);
        }
        StringBuilder sb = new StringBuilder();
        for (String k : counts.keySet()) {
            if (counts.get(k) > 1) {
                if (sb.length() > 0) {
                    sb.append(", ");
                }
                sb.append(k);
            }
        }
        return sb.toString();
    }

    private static void report(Callback cb, String s) {
        if (cb != null) {
            cb.progress(s);
        }
    }

    private static void done(Callback cb, boolean ok, String s) {
        if (cb != null) {
            cb.done(ok, s);
        }
    }

    static String slugOf(Item item) {
        String base = item.id != null && item.id.length() > 0 ? item.id : item.name;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < base.length() && sb.length() < 40; i++) {
            char ch = base.charAt(i);
            if ((ch >= 'a' && ch <= 'z') || (ch >= '0' && ch <= '9')) {
                sb.append(ch);
            } else if (ch >= 'A' && ch <= 'Z') {
                sb.append((char) (ch + 32));
            } else if (ch == '-' || ch == '_') {
                sb.append(ch);
            }
        }
        String s = sb.toString();
        return s.length() == 0 ? "mod" : s;
    }

    private static String baseName(String url) {
        String s = url;
        int q = s.indexOf('?');
        if (q >= 0) {
            s = s.substring(0, q);
        }
        int slash = s.lastIndexOf('/');
        if (slash >= 0) {
            s = s.substring(slash + 1);
        }
        try {
            s = java.net.URLDecoder.decode(s, "UTF-8");
        } catch (Throwable th) {
        }
        if (s.length() == 0 || s.indexOf('.') < 0) {
            s = "mod.dll";
        }
        return s;
    }

    private static String ext(String url) {
        String s = url.toLowerCase();
        int q = s.indexOf('?');
        if (q >= 0) {
            s = s.substring(0, q);
        }
        if (s.endsWith(".rar")) {
            return ".rar";
        }
        if (s.endsWith(".7z")) {
            return ".7z";
        }
        if (s.endsWith(".tar.gz")) {
            return ".tar.gz";
        }
        if (s.endsWith(".dll")) {
            return ".dll";
        }
        return ".zip";
    }

    private static String readText(File f) throws Exception {
        return readText(new FileInputStream(f));
    }

    private static String readText(InputStream in) throws Exception {
        java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        try {
            while ((n = in.read(buf)) > 0) {
                bos.write(buf, 0, n);
            }
        } finally {
            in.close();
        }
        return new String(bos.toByteArray(), "UTF-8");
    }

    private static void writeText(File f, String s) throws Exception {
        FileOutputStream fos = new FileOutputStream(f);
        try {
            fos.write(s.getBytes("UTF-8"));
        } finally {
            fos.close();
        }
    }

    private static String sha256(File f) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        FileInputStream in = new FileInputStream(f);
        byte[] buf = new byte[65536];
        int n;
        while ((n = in.read(buf)) > 0) {
            md.update(buf, 0, n);
        }
        in.close();
        byte[] dig = md.digest();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < dig.length; i++) {
            sb.append(String.format("%02x", dig[i]));
        }
        return sb.toString();
    }

    private static File findDirNamed(File dir, String name, int depth) {
        if (depth > 6) {
            return null;
        }
        File[] children = dir.listFiles();
        if (children == null) {
            return null;
        }
        for (int i = 0; i < children.length; i++) {
            if (children[i].isDirectory() && children[i].getName().equalsIgnoreCase(name)) {
                return children[i];
            }
        }
        for (int i = 0; i < children.length; i++) {
            if (children[i].isDirectory()) {
                File r = findDirNamed(children[i], name, depth + 1);
                if (r != null) {
                    return r;
                }
            }
        }
        return null;
    }

    private static void collectDlls(File dir, List<File> out) {
        File[] children = dir.listFiles();
        if (children == null) {
            return;
        }
        for (int i = 0; i < children.length; i++) {
            if (children[i].isDirectory()) {
                collectDlls(children[i], out);
            } else if (children[i].getName().toLowerCase().endsWith(".dll")) {
                out.add(children[i]);
            }
        }
    }

    private static void copyTree(File src, File dst) throws Exception {
        File[] children = src.listFiles();
        if (children == null) {
            throw new java.io.IOException("源目录为空：" + src);
        }
        dst.mkdirs();
        for (int i = 0; i < children.length; i++) {
            File target = new File(dst, children[i].getName());
            if (children[i].isDirectory()) {
                copyTree(children[i], target);
            } else {
                File p = target.getParentFile();
                if (p != null) {
                    p.mkdirs();
                }
                FileInputStream in = new FileInputStream(children[i]);
                FileOutputStream out = new FileOutputStream(target);
                byte[] buf = new byte[65536];
                int n;
                while ((n = in.read(buf)) > 0) {
                    out.write(buf, 0, n);
                }
                out.close();
                in.close();
            }
        }
    }

    private static void deleteTree(File f) {
        if (f == null) {
            return;
        }
        File[] children = f.listFiles();
        if (children != null) {
            for (int i = 0; i < children.length; i++) {
                deleteTree(children[i]);
            }
        }
        f.delete();
    }
}
