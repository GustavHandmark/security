package client;

import java.util.stream.Collectors;
import java.util.stream.Collectors.*;
import java.net.*;
import java.math.BigInteger;
import java.io.*;
import javax.net.ssl.*;
import javax.security.cert.X509Certificate;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.cert.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

/*
 * This example shows how to set up a key manager to perform client
 * authentication.
 *
 * This program assumes that the client is not inside a firewall.
 * The application can be modified to connect to a server outside
 * the firewall by following SSLSocketClientWithTunneling.java.
 */
public class client {
    private PrintWriter pwout;
    private BufferedReader brin;

    private InputStream din;
    private OutputStream dout;

    public SSLSocket startClient(String username, String pwd) throws KeyStoreException, IOException {

        String host = "localhost";
        int port = 8080;

        /* set up a key manager for client authentication */
        SSLSocketFactory factory = null;
        // System.out.println("working dir = "+System.getProperty("user.dir"));
        try {
            char[] password = pwd.toCharArray();
            String path_to_cert = "src/client/clientcerts/" + username + ".jks";

            KeyStore ks = KeyStore.getInstance("JKS");
            KeyStore ts = KeyStore.getInstance("JKS");
            KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
            TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
            SSLContext ctx = SSLContext.getInstance("TLS");
            ks.load(new FileInputStream(path_to_cert), password); // keystore password is password for all keystores
            ts.load(new FileInputStream("src/client/clientcerts/clienttruststore"), "password".toCharArray()); // truststore
            kmf.init(ks, password); // user password (keypass)
            tmf.init(ts); // keystore can be used as truststore here
            ctx.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
            factory = ctx.getSocketFactory();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            throw new KeyStoreException(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            throw new KeyStoreException(e.getMessage());
        }
        try {
            SSLSocket socket = (SSLSocket) factory.createSocket(host, port);
            System.out.println("\nsocket before handshake:\n" + socket + "\n");

            /*
             * send http request
             *
             * See SSLSocketClient.java for more information about why there is a forced
             * handshake here when using PrintWriters.
             */
            socket.startHandshake();
            System.out.println("\n socket after handshake:\n" + socket + "\n");
            System.out.println(socket.getSession().getProtocol());
            din = socket.getInputStream();
            dout = socket.getOutputStream();
            pwout = new PrintWriter(dout, true);
            brin = new BufferedReader(new InputStreamReader(din));
            return socket;
        } catch (Exception e) {
            throw new IOException("Server error");
        }
    }

    public String sendAndRecieveMessage(String message) {
        try {
            pwout.println(message);
            pwout.flush();
            StringBuilder response = new StringBuilder();
            response.append(brin.readLine());
            return response.toString();

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public String authenticateCredentials(String username, String password) {
        String res = sendAndRecieveMessage(username+":"+password);
        return res;
    }

    public String getTitle() {
        String res = sendAndRecieveMessage("getTitle");
        return res;
    }

    public String getDivision() {
        String res = sendAndRecieveMessage("getDivision");
        return res;
    }

    public List<String> getPatients() {
        String res = sendAndRecieveMessage("getPatients");
        List<String> patients = new ArrayList<String>(Arrays.asList(res.split(",")));
        return patients;
    }

    public String getPatientRecord(String patientName, String recordDate) {
        String res = sendAndRecieveMessage("getPatientRecord:" + patientName + ":" + recordDate);
        return res;
    }

    public List<String> getPatientVisits(String patientName) {
        String res = sendAndRecieveMessage("getPatientVisits:" + patientName);
        List<String> visits = new ArrayList<String>(Arrays.asList(res.split(",")));
        return visits;
    }

    public String editExisitingPatientRecord(String patientName, String recordDate, String newText) {
        String res = sendAndRecieveMessage("editPatientRecord:" + patientName + ":" + recordDate + ":" + newText);
        return res;
    }

    public String writeNewPatientRecord(String patientName, String recordDate, String nurse, String newText) {
        String res = sendAndRecieveMessage("writeNewPatientRecord:"+patientName+":"+nurse+":"+newText+":"+recordDate);
        return res;

    }

    public String deleteRecord(String patientName, String recordDate) {
        String res = sendAndRecieveMessage("deletePatientRecord:" + patientName + ":" + recordDate);
        return res;
    }

    /*
     * public String readMessage(SSLSocket socket) { try { SSLSession session =
     * socket.getSession(); BufferedReader read = new BufferedReader(new
     * InputStreamReader(System.in)); PrintWriter out = new
     * PrintWriter(socket.getOutputStream(), true); BufferedReader in = new
     * BufferedReader(new InputStreamReader(socket.getInputStream()));
     * 
     * 
     * } catch (Exception e) { e.printStackTrace(); } }
     */
    public void sendMessages(SSLSocket socket) {
        try {
            SSLSession session = socket.getSession();
            X509Certificate cert = (X509Certificate) session.getPeerCertificateChain()[0];
            String subject = cert.getSubjectDN().getName();
            String issuer = cert.getIssuerDN().getName();
            BigInteger serialnum = cert.getSerialNumber();
            System.out.println(session.getCipherSuite());
            System.out.println(
                    "certificate name (subject DN field) on certificate received from server:\n" + subject + "\n");
            System.out.println("Issuer: " + issuer);
            System.out.println("Serial number: " + serialnum);
            System.out.println("socket after handshake:\n" + socket + "\n");
            System.out.println("secure connection established\n\n");

            BufferedReader read = new BufferedReader(new InputStreamReader(System.in));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String msg;
            for (;;) {
                System.out.print(">");
                msg = read.readLine();
                if (msg.equalsIgnoreCase("quit")) {
                    break;
                }
                System.out.print("sending '" + msg + "' to server...");
                out.println(msg);
                out.flush();
                System.out.println("done");

                System.out.println("received '" + in.readLine() + "' from server\n");
            }
            in.close();
            out.close();
            read.close();
            socket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
