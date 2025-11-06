package org.alexdev.duckhttpd.session;

import org.alexdev.duckhttpd.queries.WebSession;
import org.alexdev.duckhttpd.server.connection.WebConnection;
import org.alexdev.duckhttpd.util.DigestUtils;
import org.alexdev.duckhttpd.util.WebUtilities;
import org.alexdev.duckhttpd.util.config.Settings;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class CookieSession {
    private String fingerprint;
    private long expireTime;

    private WebConnection client;
    private WebSession webSession;

    public CookieSession(WebConnection client) {
        this.client = client;
        this.webSession = new WebSession(this.client);
        String cookieString = client.request().headers().get(HttpHeaderNames.COOKIE);
        String existingId = null;

        if (cookieString != null && !cookieString.isEmpty()) {
            Set<Cookie> cookies = ServerCookieDecoder.LAX.decode(cookieString);
            for (Cookie c : cookies) {
                if (c.name().equals("HTTPSESSID")) {
                    existingId = c.value();
                    break;
                }
            }
        }

        if (existingId != null && !existingId.isEmpty()) {
            this.fingerprint = existingId;
        } else {
            this.generateFingerprint();  // Generate new for fresh sessions
        }

        this.generateExpireTime();
    }

    public void generateExpireTime() {
        this.expireTime = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(CookieSessionManager.getExpireTime()); // TODO: Configure GC collection time
    }

    public void generateFingerprint() {
        this.fingerprint = DigestUtils.sha256Hex(UUID.randomUUID() + String.valueOf(WebUtilities.currentTimeSeconds()));
    }

    public void setFingerprint(String fingerprint) {
        this.fingerprint = fingerprint;
    }

    public File getSessionFile() {
        if (!Settings.getInstance().isSaveSessions() || this.fingerprint == null) {
            return null;

        try {
            return Paths.get(CookieSessionManager.getInstance().getSessionDirectory().getCanonicalPath(), fingerprint).toFile();
        } catch (IOException ignored) {

        }

        return null;
    }

    public String getFingerprint() {
        return fingerprint;
    }

    public long getExpireTime() {
        return expireTime;
    }

    public WebSession getWebSession() {
        return webSession;
    }
}
