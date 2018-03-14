package www.cume.cc.demo;

import android.content.Context;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * $Author: xhunmon
 * $Date: 2018-03-13
 * $Description:
 */

public class Utils {

    private static final String TAG = Utils.class.getSimpleName();
    /**
     * 从Assets读取比特米账号的jsonobject对象
     * @param context 上下文
     * @return 当异常时返回一个空对象
     */
    public static JSONObject readJsonAssets(Context context){
        JSONObject jsonObject2 = null;
        InputStream is = null;
        try {
            is = context.getAssets().open("test1234.btr");
            int lenght = is.available();
            byte[]  buffer = new byte[lenght];
            is.read(buffer);
            String result = new String(buffer, "utf8");
            String enStr = deJsonFile(result);
            jsonObject2 = new JSONObject(enStr.substring(Constant.strWalletPre.length(),enStr.length()));
        } catch (IOException e) {
            Log.e(TAG,e.toString());
        } catch (JSONException e) {
            Log.e(TAG,e.toString());
        }
        return jsonObject2==null ? new JSONObject() : jsonObject2;
    }

    /**
     * 从本地读取比特米账号的jsonobject对象
     * (用于米袋备份)
     * @param path  本地米袋路径
     * @return 当异常时返回一个空对象
     */
    public static JSONObject readJson(String path){
        JSONObject jsonObject2 = null;
        StringBuilder sb = new StringBuilder();
        File file = new File(path);
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file),"UTF-8"));
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            String enStr = deJsonFile(sb.toString());
            jsonObject2 = new JSONObject(enStr.substring(Constant.strWalletPre.length(),enStr.length()));
            br.close();
        } catch (Exception e) {
            Log.e(TAG,e.toString());
        }
        return jsonObject2==null ? new JSONObject() : jsonObject2;
    }

    /**
     * 用来做导入wallet账户的json数据
     * @param strJson 要进行解密的字符串
     */
    public static String deJsonFile(String strJson) {
        String result = null;
        try {
            byte[] priKey = Base64.decode(strJson.getBytes("UTF-8"), Base64.DEFAULT);
            if (priKey == null) {
                return null;
            }
            int len = priKey.length;
            byte[] key = string2MD5(Constant.enFileKey).toUpperCase().getBytes("UTF-8");
            int j = 0;
            for (int i = 0; i < len; i++) {
                priKey[i] = (byte) (priKey[i] ^ key[j]);
                j++;
                if (j == key.length) {
                    j = 0;
                }
            }
            result = new String(priKey, "UTF-8");
        } catch (Exception e) {
            Log.e(TAG,e.toString());
        }
        return result;
    }

    /**
     * MD5加密
     * @param str 要加密的字符串
     */
    public static String string2MD5(String str) {
        String re_md5 = new String();
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(str.getBytes());
            byte b[] = md.digest();

            int i;

            StringBuffer buf = new StringBuffer("");
            for (int offset = 0; offset < b.length; offset++) {
                i = b[offset];
                if (i < 0)
                    i += 256;
                if (i < 16)
                    buf.append("0");
                buf.append(Integer.toHexString(i));
            }

            re_md5 = buf.toString();

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return re_md5;
    }

    /**
     * 将本地保存的私钥密码进行解密
     * 注意：解密拿到返回值需要进行result.startsWith("CMBTR-")判断用户输入的密码是否正确
     *
     * @param privateKey 本地加密秘钥
     * @param pwd        用户密码
     * @return 解密后的结果
     */
    public static String deCodePrivate(String privateKey, String pwd) {
        String result = null;
        try {
            if (TextUtils.isEmpty(pwd)) {
                pwd = "***";//用户输入密码为空，默认解密结果错误
            }
            byte[] priKey = Base64.decode(privateKey, Base64.DEFAULT);
            if (priKey == null) {
                return null;
            }
            int len = priKey.length;
            byte[] key = string2MD5(pwd).toUpperCase().getBytes("UTF-8");
            int j = 0;
            for (int i = 0; i < len; i++) {
                priKey[i] = (byte) (priKey[i] ^ key[j]);
                j++;
                if (j == key.length) {
                    j = 0;
                }
            }
            result = new String(priKey, "UTF-8");
        } catch (Exception e) {
            Log.e(TAG,e.toString());
        }
        return result;
    }
}
