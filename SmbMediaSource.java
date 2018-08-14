
package com.ruanke.smb;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.net.MalformedURLException;

import android.content.Context;
import android.util.Log;

import jcifs.smb.SmbFile;
import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbException;

/**
 * Smb server source
 */
public class SmbMediaSource {

    private static final String TAG = SmbMediaSource.class.getSimpleName();
    private static final boolean DEBUG = true;

    private String mPath;
    private String mDomain;
    private String mUser;
    /**
     * Create smb media source
     *
     * @param partId partition ID
     * @param path mount path
     * @param title title
     */
    public SmbMediaSource(String path, String domain, String user, String password) {
        this.mPath = path;
        this.mDomain = domain;
        this.mUser = user;
        this.mPassword = password;
    }

    /**
     * Get mount path
     *
     * @return path
     */
    public String getPath() {
        return mPath;
    }


    @Override
    public static List<String> listSortedItemsWithCount(String parent) {
        try {
            if (!new SmbFile(mPath).exists()) {
                return null;
            }

            String path = parent == null ? mPath : mPath + parent + "/";
            if (DEBUG)
                Log.i(TAG, "Browsing " + path);
            NtlmPasswordAuthentication auth = new NtlmPasswordAuthentication(mDomain, mUser, mPassword);
            SmbFile dir = new SmbFile(path, auth);
            String fileList[] = dir.list();
            Log.i(TAG, "Browsing finished:" + path);
            if (fileList == null) {
                Log.e(TAG, "Unable to read " + path);
                return null;
            }
            return fileList;
        } catch (SmbException e) {
            e.printStackTrace();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } finally {
        }

        return null;
    }



    /**
     * @return scan smb servers
     */
    static public List<String> getSmbServers(Context context) {
        Log.i(TAG, "getSmbServers");
        System.setProperty("jcifs.smb.client.dfs.disabled", "true");
        List<String> smbServers = new ArrayList<>();
        try {
            SmbFile[] domains = (new SmbFile("smb://")).listFiles();
            if (domains == null || domains.length <= 0) {
                Log.i(TAG, "get Smb domain empty.");
                return null;
            }
            Log.i(TAG, "domain count: " + domains.length);
            for (int i = 0; i < domains.length; i++) {
                String domainServer = domains[i].getServer();
                String domainTitle = domains[i].getName();
                String domainPath = domains[i].getPath();
                Log.i(TAG, "domainserver: " + domainServer + ", dotitle: " + domainTitle + ", dopath: " + domainPath);
                SmbFile[] servers = null;
                try {
                    servers = domains[i].listFiles();
                } catch (Exception e) {
                    Log.e(TAG, "get server error: " + e.getMessage());
                    continue;
                }
                if (servers == null || servers.length <= 0) {
                    Log.i(TAG, "get Smb empty.");
                    continue;
                }
                Log.i(TAG, "smb count: " + servers.length);
                for (int j = 0; j < servers.length; j++) {
                    try {
                        if (!servers[j].exists()) {
                            continue;
                        }
                    } catch (SmbException e) {
                        e.printStackTrace();
                        continue;
                    }
                    String origId = servers[j].getServer();
                    String title = servers[j].getName();
                    String path = servers[j].getPath();
                    Log.i(TAG, "origId: " + origId + ", title: " + title + ", path: " + path);
                    smbServers.add(title);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "error:" + e.getMessage());
            e.printStackTrace();
        }
        return smbServers;
    }

}
