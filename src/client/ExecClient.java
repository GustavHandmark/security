package client;

import client.client;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import javax.net.ssl.*;
import java.security.KeyStoreException;
import java.util.List;

public class ExecClient {
	SSLSocket socket;

	public static void main(String[] args) {
		client c = new client();
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		while (true) {
			try {
				System.out.print("Certificate Name:");
				String s;
				s = br.readLine();
				System.out.print("Password to keystore:");
				String pwd;
				pwd = br.readLine();
				System.out.println(s + "\npwd:" + pwd);

				SSLSocket socket = c.startClient(s, pwd);
				boolean authenticated = false;
				while(!authenticated) {
					System.out.print("Username:");
					String s2;
					s2 = br.readLine();
					System.out.print("Password:");
					String pwd2;     
					pwd2 = br.readLine();
					String res0 = c.authenticateCredentials(s2,pwd2);
					if(res0.equals("Authentication successful")) {
						System.out.println(res0);
						authenticated = true;
					} else {
						System.out.println(res0);
					}
				}
				String res = c.sendAndRecieveMessage("ping");
				System.out.println("response: " + res);
				String res2 = c.sendAndRecieveMessage("ping");
				System.out.println("response: " + res2);
				String res3 = c.getTitle();
				System.out.println(res3);
				String res4 = c.getPatientRecord("jackoneil", "2019-03-03");

				System.out.println(res4);
				String res5 = c.getPatientRecord("samanthacarter", "2019-03-04");
				System.out.println(res5);

				List<String> res6 = c.getPatients();
				System.out.println(res6.toString());

				List<String> res7 = c.getPatientVisits("jackoneil");
				System.out.println(res7);
				// String res8 = c.deleteRecord("jackoneil", "2019-03-03");
				// System.out.println(res8);
				String res9 = c.editExisitingPatientRecord("jackoneil", "2019-03-03",
						"Patient changed his mind, he is no longer suffering from pain");
				System.out.println(res9);
				String res10 = c.writeNewPatientRecord("jackoneil", "2019-03-05", "nurselifesaver",
						"Patient arrived again, suspecting hypochondria");
				System.out.println(res10);

				String res11 = c.getPatientRecord("jackoneil", "2019-03-05");
				System.out.println(res11);

			} catch (IOException e1) {
				System.out.println("Server error");
			} catch (KeyStoreException e2) {
				e2.printStackTrace();
				System.out.println("Wrong password or username");
			}
		}

	}

}
