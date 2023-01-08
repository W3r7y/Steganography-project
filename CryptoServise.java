import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;
import java.util.Random;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;


//***************** AN AES IMPLEMENTATION *******************

public class CryptoServise {

	private static SecretKeySpec secretKey;
	private static byte[] key;

	
	//The function sets the password as key for encryption / decryption.
	//The function sets the needed parameters and algorithms that we use as well.
	public static void setKey(String password) {
		MessageDigest sha = null;
		try {
			key = password.getBytes("UTF-8");	//eight-bit UCS Transformation Format
			sha = MessageDigest.getInstance("SHA-256");
			key = sha.digest(key);
			key = Arrays.copyOf(key, 16);
			secretKey = new SecretKeySpec(key, "AES");
		} catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}

	
	//The function gets string that we want to encrypt and a password that we use for AES,
	//and return the encrypted string (cipher)
	public String encrypt(String strToEncrypt, String password) {
		try {
			setKey(password);
			Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
			cipher.init(Cipher.ENCRYPT_MODE, secretKey);
			return Base64.getEncoder()
					.encodeToString(cipher.doFinal(strToEncrypt.getBytes("UTF-8")));
		} catch (Exception e) {
			System.out.println("Error while encrypting: " + e.toString());
		}
		return null;
	}

	
	//The function get string that we want to decrypt and password for decryption,
	//and return the decrypted string (actual data) if the password is true, and random string of characters if not.
	public String decrypt(String strToDecrypt,String password) {
		try {
			setKey(password);
			Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5PADDING");
			cipher.init(Cipher.DECRYPT_MODE, secretKey);
			return new String(cipher.doFinal(Base64.getDecoder()
					.decode(strToDecrypt)));
			
		} catch (Exception e) {	//if the decryption will have some problem, we distract and cover the exception with random string of char.    	
			Random rand = new Random();

			String str = rand.ints(48,123)		//Creates random string to distract the decode failure because of some reason.
					.filter(num ->(num<58 || num>64)&&(num<91 || num>96))
					.limit(rand.nextInt(256))	//random length (can be changed)
					.mapToObj(c -> (char)c).collect(StringBuffer::new, StringBuffer::append, StringBuffer::append)
					.toString();

			return str;
		}
	}
}
