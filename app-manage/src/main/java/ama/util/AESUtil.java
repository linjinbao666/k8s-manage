package ama.util;

import org.apache.tomcat.util.codec.binary.Base64;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AESUtil {
	private static final String KEY_ALGORITHM = "AES";
	private static final String DEFAULT_CIPHER_ALGORITHM = "AES/ECB/PKCS5Padding";

	private static final String PWD = "80ervEgWoU35iuR$R64eRd#ndOm!";
	private static final String SIGN_ALGORITHMS = "SHA1PRNG";
	/**
	 * AES 加密操作
	 *
	 * @param content  待加密内容
	 * @return 返回Base64转码后的加密数据
	 */
	public static String encrypt(String content) {
		if (null == content || "".equals(content)){
			return null;
		}
		return encrypt(content, PWD);
	}
	
	private static String encrypt(String content, String password) {
		try {
			Cipher cipher = Cipher.getInstance(DEFAULT_CIPHER_ALGORITHM);// 创建密码器

			byte[] byteContent = content.getBytes("utf-8");

			cipher.init(Cipher.ENCRYPT_MODE, getSecretKey(password));// 初始化为加密模式的密码器

			byte[] result = cipher.doFinal(byteContent);// 加密
			
			return Base64.encodeBase64String(result);// 通过Base64转码返回
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		return null;
	}

	/**
	 * AES 解密操作
	 *
	 * @param content
	 * @return
	 */
	public static String decrypt(String content) {
		if(null == content || "".equals(content)){
			return null;
		}
		return decrypt(content, PWD);

	}
	
	private static String decrypt(String content, String password) {

		try {
			// 实例化
			Cipher cipher = Cipher.getInstance(DEFAULT_CIPHER_ALGORITHM);

			// 使用密钥初始化，设置为解密模式
			cipher.init(Cipher.DECRYPT_MODE, getSecretKey(password));

			// 执行操作
			byte[] result = cipher.doFinal(Base64.decodeBase64(content));

			return new String(result, "utf-8");
		} catch (Exception ex) {
			Logger.getLogger(AESUtil.class.getName()).log(Level.SEVERE, null, ex);
			return null;
		}

	}

	/**
	 * 生成加密秘钥
	 *
	 * @return
	 */
	private static SecretKeySpec getSecretKey(final String password) {
		// 返回生成指定算法密钥生成器的 KeyGenerator 对象
		KeyGenerator kg = null;

		try {
			kg = KeyGenerator.getInstance(KEY_ALGORITHM);

			SecureRandom random = SecureRandom.getInstance(SIGN_ALGORITHMS);
	        random.setSeed(password.getBytes("utf-8"));
	        kg.init(128, random);

			// 生成一个密钥
			SecretKey secretKey = kg.generateKey();

			return new SecretKeySpec(secretKey.getEncoded(), KEY_ALGORITHM);// 转换为AES专用密钥
		} catch (NoSuchAlgorithmException ex) {
			ex.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return null;
	}

	public static void main(String[] args) {
		System.out.println("s2:" +  AESUtil.encrypt("root"));
		System.out.println("s2:" +  AESUtil.encrypt("Hw9CeH9lmYbMegH"));
		System.out.println(AESUtil.decrypt("y4/GmOSvMgrUC7tiayi3uw=="));
	}
}
