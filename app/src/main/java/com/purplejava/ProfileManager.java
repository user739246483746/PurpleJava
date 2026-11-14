package com.purplejava;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.UUID;

public class ProfileManager {

    private static final String PREFS_NAME = "purplejava_profile";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_UUID     = "uuid";

    private final SharedPreferences prefs;

    public ProfileManager(Context ctx) {
        prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public String getUsername() {
        return prefs.getString(KEY_USERNAME, "");
    }

    public void setUsername(String name) {
        prefs.edit().putString(KEY_USERNAME, name).apply();
    }

    public String getOfflineUUID(String username) {
        String stored = prefs.getString(KEY_UUID + "_" + username, null);
        if (stored != null) return stored;

        UUID uuid = UUID.nameUUIDFromBytes(
                ("OfflinePlayer:" + username).getBytes(java.nio.charset.StandardCharsets.UTF_8)
        );
        String uuidStr = uuid.toString();
        prefs.edit().putString(KEY_UUID + "_" + username, uuidStr).apply();
        return uuidStr;
    }
}
