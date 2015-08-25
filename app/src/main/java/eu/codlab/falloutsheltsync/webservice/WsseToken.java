package eu.codlab.falloutsheltsync.webservice;

import android.util.Base64;

import org.joda.time.MutableDateTime;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class WsseToken {
    public static final String HEADER_AUTHORIZATION = "Authorization";
    public static final String HEADER_WSSE = "X-WSSE";

    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
    private String _email;
    private String _password;
    private String _nonce; //recompute in getWsseHeader()
    private String _created_at; //recompute in getWsseHeader()
    private String _digest; //recompute in getWsseHeader()

    public WsseToken(String email, String password) {
        _email = email;
        _password = password;
        _created_at = generateTimestamp();
        _nonce = generateNonce();
        _digest = generateDigest();
    }

    public String getPassword() {
        return _password;
    }

    public String getEmail() {
        return _email;
    }

    private String generateNonce() {
        SecureRandom random = new SecureRandom();
        byte seed[] = random.generateSeed(8);
        return bytesToHex(seed);
    }

    public static String bytesToHex(byte[] bytes) {
        final char[] hexArray = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
        char[] hexChars = new char[bytes.length * 2];

        int v;
        for (int j = 0; j < bytes.length; j++) {
            v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    private String generateTimestamp() {
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        MutableDateTime now = new MutableDateTime(new Date());
        now.addMinutes(-1);
        return sdf.format(now.toDate());
    }

    private String generateDigest() {
        String digest = null;
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            StringBuilder sb = new StringBuilder();
            sb.append(this._nonce);
            sb.append(this._created_at);
            sb.append(this._password);
            byte sha[] = md.digest(sb.toString().getBytes());
            digest = Base64.encodeToString(sha, Base64.NO_WRAP);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return digest;
    }

    public String getWsseHeader() {

        _created_at = generateTimestamp();
        _nonce = generateNonce();
        _digest = generateDigest();
        StringBuilder header = new StringBuilder();
        header.append("UsernameToken Username=\"");
        header.append(_email);
        header.append("\", PasswordDigest=\"");
        header.append(_digest);
        header.append("\", Nonce=\"");
        header.append(Base64.encodeToString(_nonce.getBytes(), Base64.NO_WRAP));
        header.append("\", Created=\"");
        header.append(this._created_at);
        header.append("\"");
        return header.toString();
    }

    public String getAuthorizationHeader() {
        return "WSSE profile=\"UsernameToken\"";
    }

}
