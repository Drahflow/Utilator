package name.drahflow.utilator;

// Thank you: http://code.google.com/searchframe#HWjuhIdT4ps/trunk/src/org/transdroid/util/FakeTrustManager.java&q=SSL%20package:http://transdroid\.googlecode\.com&sq=&ct=rc&cd=9
import java.security.*;
import java.security.cert.*;
import javax.net.ssl.*;
import javax.net.ssl.X509TrustManager;

import android.util.*;

public class TrustManager implements X509TrustManager {
	private final static String CORRECT_CERT = "308202B830820221A003020102020900D4C4739E44CE54ED300D06092A864886F70D010105050030473111300F060355040A130844726168666C6F773111300F060355040B130844726168666C6F77311F301D0603550403131633383032302E76732E77656274726F7069612E636F6D3020170D3132303430323139303335305A180F32323836303131353139303335305A30473111300F060355040A130844726168666C6F773111300F060355040B130844726168666C6F77311F301D0603550403131633383032302E76732E77656274726F7069612E636F6D30819F300D06092A864886F70D010101050003818D0030818902818100D1B0952F4FB19327D4D48650B9A0C380C33260299DFDE6B74A93B8A3BE945E00264CB3DD2B2537F0EF6F153A02957BCA9B16F5043653D29F3872D9470B1F01CABB5BD0B341EC4C7295F28795B30480F8F6195E51B0C8D2CFCE36BF51367F6A0F46CF55BC21D705933C1A58778BB51523A8C85113D4A3B9B2548FDA9D4D2909650203010001A381A93081A6301D0603551D0E04160414C5EBC6F730B862DAA5B4DA051A0B6C1B54C6404A30770603551D230470306E8014C5EBC6F730B862DAA5B4DA051A0B6C1B54C6404AA14BA44930473111300F060355040A130844726168666C6F773111300F060355040B130844726168666C6F77311F301D0603550403131633383032302E76732E77656274726F7069612E636F6D820900D4C4739E44CE54ED300C0603551D13040530030101FF300D06092A864886F70D0101050500038181006DD9E6D4C0E6ED08976C1C1CC95CDFEA92AC7F6879F8EB84B88CF371C64DD9DB52B4A5E1832F98B4EB35B5714B2B23AF8CB5558AED96A5FBE2180CA95D5FE79C949F3AD9008AE0E9112C0390555B98BF691243599964AE775214C5B1E17C968FE710728B7B8845E6E8469D0DD40152219C3125D852DC68988683AA85739212A4";

	@Override public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
		throw new CertificateException("We don't do client certs here...");
	}

	@Override public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
		String cert = hexify(chain[0].getEncoded());

		// Log.i("Utilator", "Received cert: " + cert);
		if(!CORRECT_CERT.equals(cert)) {
			throw new CertificateException("Certificate is not the one expected.");
		}
	}

	@Override public X509Certificate[] getAcceptedIssuers() {
		return new X509Certificate[0];
	}

	private final static String digits = "0123456789ABCDEF";
	private static String hexify (byte bytes[]) {
		StringBuffer r = new StringBuffer(bytes.length * 2);

		for(int i = 0; i < bytes.length; ++i) {
			r.append(digits.charAt((bytes[i] & 0xf0) >> 4));
			r.append(digits.charAt(bytes[i] & 0x0f));
		}

		return r.toString();
	}

	public static void install() {
		try {
			SSLContext sc = SSLContext.getInstance("TLS");
			sc.init(null, new TrustManager[] { new TrustManager() }, new java.security.SecureRandom());
			HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
		} catch (Exception e) {
			throw new Error("TrustManager install failed", e);
		}
	}
}
