package server;

import java.io.*;
import java.math.BigInteger;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.stream.Stream;
import javax.net.*;
import javax.net.ssl.*;
import javax.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.Arrays;
import java.nio.file.Path;
import java.util.stream.Collectors;
import java.util.Collections.*;
import java.util.*;
import java.nio.charset.*;
import java.security.SecureRandom;
import java.security.MessageDigest;
import java.util.Base64;

public class server implements Runnable {
    private ServerSocket serverSocket = null;
    private static int numConnectedClients = 0;
    private static HashMap<BigInteger, List<String>> accessRecord;
    private static HashMap<String, List<String>> passwordFile;

    public server(ServerSocket ss) throws IOException {
        serverSocket = ss;
        newListener();
    }

    public void run() {
        try {
            SSLSocket socket = (SSLSocket) serverSocket.accept();
            newListener();
            PrintWriter out = null;
            BufferedReader in = null;
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String initialMessage = null;
            while ((initialMessage = in.readLine()) != null) {
                String creds = new StringBuilder(initialMessage).toString();
                String[] credentials = creds.split(":");
                if (verifyPassword(credentials[0], credentials[1])) {
                    out.println("Authentication successful");
                    break;
                } else {
                    String tl = timeLeft(credentials[0], credentials[1]);
                    if (tl.equals("credentials")) {
                        out.println("Wrong username or password, try again");
                    } else {
                        out.println("Maximum amount of tries, please wait: " + tl);
                    }
                }
            }

            SSLSession session = socket.getSession();

            numConnectedClients++;
            X509Certificate cert = (X509Certificate) session.getPeerCertificateChain()[0];
            String subject = cert.getSubjectDN().getName();
            String issuer = cert.getIssuerDN().getName();
            BigInteger serialnum = cert.getSerialNumber();

            System.out.println("client connected");
            System.out.println("client name (cert subject DN field): " + subject);
            System.out.println("Issuer: " + issuer);
            System.out.println("Serial number: " + serialnum);
            System.out.println(numConnectedClients + " concurrent connection(s)\n");
            logGrab().forEach(System.out::println);

            String clientMsg = null;
            while ((clientMsg = in.readLine()) != null) {
                String message = new StringBuilder(clientMsg).toString();
                List<String> messageParts = Arrays.asList(message.split(":"));
                switch (messageParts.get(0)) {
                case "ping":
                    ping(out);
                    break;
                case "getTitle":
                    String resTitle = returnTitle(serialnum);
                    out.println(resTitle);
                    break;
                case "getDivision":
                    String resDivision = returnDivision(serialnum);
                    out.println(resDivision);
                    break;
                case "getPatientRecord":
                    if (isAuthorizedToRead(serialnum, messageParts.get(1), messageParts.get(2))) {
                        LinkedHashMap<String, String> resRecord = returnPatientRecord(serialnum, messageParts.get(1),
                                messageParts.get(2));
                        log(returnName(serialnum) + " accessed record for: " + messageParts.get(1)
                                + " Outcome: success");
                        out.println(resRecord.get("note"));
                    } else {
                        log(returnName(serialnum) + " tried accessing record for: " + messageParts.get(1)
                                + " outcome: denied");
                        out.println("Access denied");
                    }
                    break;

                case "getPatients":
                    Set<String> patients = returnPatients(serialnum);
                    out.println(String.join(",", patients));
                    break;

                case "getPatientVisits":
                    List<String> visits = returnPatientVisits(serialnum, messageParts.get(1));
                    out.println(String.join(",", visits));
                    break;

                case "deletePatientRecord":
                    if (isAuthorizedDelete(serialnum)) {
                        if (deleteRecord(serialnum, messageParts.get(1), messageParts.get(2))) {
                            log(returnName(serialnum) + " deleted record (" + messageParts.get(2) + ") for: "
                                    + messageParts.get(1) + " Outcome: success");
                            out.println("Deleted patient record");
                        } else {
                            log(returnName(serialnum) + " deleted record (" + messageParts.get(2) + ") for: "
                                    + messageParts.get(1) + " Outcome: Error");
                            out.println("Error deleting file");
                        }
                    } else {
                        log(returnName(serialnum) + " tried deleting record (" + messageParts.get(2) + ") for: "
                                + messageParts.get(1) + " Outcome: denied");
                        out.println("Access to delete denied");
                    }
                    break;

                case "editPatientRecord":
                    if (isAuthorizedWrite(serialnum, messageParts.get(1), messageParts.get(2))) {
                        if (editPatientRecord(serialnum, messageParts.get(1), messageParts.get(2),
                                messageParts.get(3))) {
                            log(returnName(serialnum) + " edited record (" + messageParts.get(2) + ") for: "
                                    + messageParts.get(1) + " Outcome: success");
                            out.println("Patient record changed");
                        } else {
                            log(returnName(serialnum) + " edited record (" + messageParts.get(2) + ") for: "
                                    + messageParts.get(1) + " Outcome: Error");
                            out.println("Error occured");
                        }
                    } else {
                        log(returnName(serialnum) + " tried editing record (" + messageParts.get(2) + ") for: "
                                + messageParts.get(1) + " Outcome: denied");
                        out.println("Access to edit is denied");
                    }
                    break;

                case "writeNewPatientRecord":
                    if (isAuthorizedCreate(serialnum)) {
                        if (createNewPatientRecord(serialnum, messageParts.get(1), messageParts.get(2),
                                messageParts.get(3), messageParts.get(4))) {
                            log(returnName(serialnum) + " created record (" + messageParts.get(4) + ") for: "
                                    + messageParts.get(1) + " Outcome: success");
                            out.println("Created new patient record");
                        } else {
                            log(returnName(serialnum) + " tried creating record (" + messageParts.get(4) + ") for: "
                                    + messageParts.get(1) + " Outcome: Error");
                            out.println("Error occured");
                        }
                    } else {
                        log(returnName(serialnum) + " tried creating record (" + messageParts.get(4) + ") for: "
                                + messageParts.get(1) + " Outcome: denied");
                        out.println("Access to create new record is denied");
                    }
                    break;

                }
            }
            in.close();
            out.close();
            socket.close();
            numConnectedClients--;
            System.out.println("client disconnected");
            System.out.println(numConnectedClients + " concurrent connection(s)\n");

        } catch (IOException e) {
            System.out.println("Client died: " + e.getMessage());
            e.printStackTrace();
            return;
        }
    }

    private void ping(PrintWriter out) {
        out.println("Ping returned");
    }

    private String returnName(BigInteger serialnum) {
        String name = accessRecord.get(serialnum).get(1);
        return name;
    }

    private String returnTitle(BigInteger serialnum) {
        String title = accessRecord.get(serialnum).get(0);
        return title;
    }

    private String returnDivision(BigInteger serialnum) {
        String division = accessRecord.get(serialnum).get(2);
        return division;
    }

    private LinkedHashMap<String, String> returnPatientRecord(BigInteger serialnum, String patientName,
            String recordDate) {
        try (BufferedReader br = new BufferedReader(
                new FileReader("src/server/patients/" + patientName + "/" + recordDate + ".txt"))) {
            String l;
            LinkedHashMap<String, String> record = new LinkedHashMap<String, String>();
            while ((l = br.readLine()) != null) {
                String[] v = l.split(":");
                record.put(v[0], v[1]);
            }
            return record;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // returns all patients which the requesting person has access to read.
    // this is horrible performance wise, we should've used a database.

    private List<Path> returnRecordsPaths() {
        try (Stream<Path> paths = Files.walk(Paths.get("src/server/patients"))) {
            return paths.filter(Files::isRegularFile).collect(Collectors.toList());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return Collections.emptyList();

    }

    private Set<String> returnPatients(BigInteger serialnum) {
        Set<String> patients = new LinkedHashSet<String>();
        List<Path> paths = returnRecordsPaths();
        paths.forEach(filePath -> {
            // get name and record from path.
            String patientName = filePath.getParent().getFileName().toString();
            String recordDate = filePath.getFileName().toString().split("\\.")[0];
            try (BufferedReader br = new BufferedReader(new FileReader(filePath.toString()))) {
                if (isAuthorizedToRead(serialnum, patientName, recordDate)) {
                    patients.add(br.readLine().split(":")[1]);
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        return patients;
    }

    private List<String> returnPatientVisits(BigInteger serialnum, String patientName) {
        try (Stream<Path> paths = Files.walk(Paths.get("src/server/patients/" + patientName))) {
            List<Path> recordPaths = paths.filter(Files::isRegularFile).collect(Collectors.toList());
            List<String> visits = new ArrayList<String>();

            recordPaths.forEach(filePath -> {
                String recordDate = filePath.getFileName().toString().split("\\.")[0];
                try (BufferedReader br = new BufferedReader(new FileReader(filePath.toString()))) {
                    if (isAuthorizedToRead(serialnum, patientName, recordDate)) {
                        visits.add(filePath.getFileName().toString().split("\\.")[0]);
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            return visits;

        } catch (IOException e) {
            e.printStackTrace();
        }
        return Collections.emptyList();

    }

    private boolean editPatientRecord(BigInteger serialnum, String patientName, String recordDate, String newText) {
        LinkedHashMap<String, String> record = returnPatientRecord(serialnum, patientName, recordDate);
        // set new text
        record.put("note", newText);
        List<String> lines = new ArrayList<String>();
        for (Map.Entry<String, String> entry : record.entrySet()) {
            lines.add(entry.getKey() + ":" + entry.getValue());
        }
        Path file = Paths.get("src/server/patients/" + patientName + "/" + recordDate + ".txt");
        try {
            Files.write(file, lines, Charset.forName("utf-8"));
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

    }

    private boolean createNewPatientRecord(BigInteger serialnum, String patientName, String nurse, String note,
            String recordDate) {
        List<String> lines = new ArrayList<String>();
        lines.add("patient:" + patientName);
        lines.add("nurse:" + nurse);
        lines.add("doctor:" + returnName(serialnum));
        lines.add("division:" + returnDivision(serialnum));
        lines.add("note:" + note);
        Path fileName = Paths.get("src/server/patients/" + patientName + "/" + recordDate + ".txt");
        try {
            Files.write(fileName, lines, Charset.forName("UTF-8"));
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

    }

    private boolean deleteRecord(BigInteger serialnum, String patientName, String recordDate) {
        try {
            return Files.deleteIfExists(Paths.get("src/server/patients/" + patientName + "/" + recordDate + ".txt"));

        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

    }

    private boolean isAuthorizedToRead(BigInteger serialnum, String patientName, String recordDate) {
        try (BufferedReader br = new BufferedReader(
                new FileReader("src/server/patients/" + patientName + "/" + recordDate + ".txt"))) {
            String l;
            HashMap<String, String> record = new HashMap<String, String>();
            while ((l = br.readLine()) != null) {
                String[] v = l.split(":");
                record.put(v[0], v[1]);
            }
            String reqName = returnName(serialnum);
            String reqTitle = returnTitle(serialnum);
            String reqDivision = returnDivision(serialnum);
            // If the person requesting is a nurse, and he or she is associated with the
            // record. Allow.
            if (reqTitle.equals("nurse") && reqName.equals(record.get("nurse"))) {
                return true;
            }
            // If the person requesting is a nurse, and record belongs to his or her
            // division. Allow.
            if (reqTitle.equals("nurse") && reqDivision.equals(record.get("division"))) {
                return true;
            }

            // If the person requesting is a doctor, and he or she is associated with the
            // record. Allow.
            if (reqTitle.equals("doctor") && reqName.equals(record.get("doctor"))) {
                return true;
            }
            // If the person requesting is a doctor, and record belongs to his or her
            // division. Allow.
            if (reqTitle.equals("doctor") && reqDivision.equals(record.get("division"))) {
                return true;
            }
            // If the person requesting the record is the patient of the same, access is
            // allowed.
            if (reqName.equals(record.get("patient"))) {
                return true;
            }

            // If the requesting party is a government agency, allow read all.
            if (reqTitle.equals("government")) {
                return true;
            }

            return false;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean isAuthorizedWrite(BigInteger serialnum, String patientName, String recordDate) {
        try (BufferedReader br = new BufferedReader(
                new FileReader("src/server/patients/" + patientName + "/" + recordDate + ".txt"))) {
            String l;
            HashMap<String, String> record = new HashMap<String, String>();
            while ((l = br.readLine()) != null) {
                String[] v = l.split(":");
                record.put(v[0], v[1]);
            }
            String reqName = returnName(serialnum);
            String reqTitle = returnTitle(serialnum);
            // If the person requesting is a nurse, and he or she is associated with the
            // record. Allow write.
            if (reqTitle.equals("nurse") && reqName.equals(record.get("nurse"))) {
                return true;
            }
            // If the person requesting is a doctor, and he or she is associated with the
            // record. Allow write.
            if (reqTitle.equals("doctor") && reqName.equals(record.get("doctor"))) {
                return true;
            }

            return false;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

    }

    private boolean isAuthorizedCreate(BigInteger serialnum) {
        String reqName = returnName(serialnum);
        String reqTitle = returnTitle(serialnum);
        if (reqTitle.equals("doctor")) {
            return true;
        }
        return false;

    }

    private boolean isAuthorizedDelete(BigInteger serialnum) {
        String reqTitle = returnTitle(serialnum);
        // Only allow if government agency.
        if (reqTitle.equals("government")) {
            return true;
        }
        return false;
    }

    private static void readAuthCSV() {
        try (BufferedReader br = new BufferedReader(new FileReader("src/server/accountindex.csv"))) {
            String l;
            while ((l = br.readLine()) != null) {
                String[] values = l.split(",");
                List<String> info = new ArrayList<>();
                for (int i = 1; i < values.length; i++) {
                    info.add(values[i]);
                }
                accessRecord.put(new BigInteger(values[0]), info);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void readRecord(String patientName) {
        String path = "server/patients" + patientName;
        try {
            Scanner scan = new Scanner(new File(path));

        } catch (FileNotFoundException e) {
            e.printStackTrace();
            System.out.println("File not found");
        }
    }

    private void newListener() {
        (new Thread(this)).start();
    } // calls run()

    public static void main(String args[]) {
        System.out.println("\nServer Started\n");
        accessRecord = new HashMap<>();
        passwordFile = new HashMap<>();
        // generatePasswords();
        readAuthCSV();
        initPasswordFile();
        int port = 8080;
        if (args.length >= 1) {
            port = Integer.parseInt(args[0]);
        }
        String type = "TLS";
        try {
            ServerSocketFactory ssf = getServerSocketFactory(type);
            ServerSocket ss = ssf.createServerSocket(port);
            ((SSLServerSocket) ss).setNeedClientAuth(true); // enables client authentication
            new server(ss);
        } catch (IOException e) {
            System.out.println("Unable to start Server: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static ServerSocketFactory getServerSocketFactory(String type) {
        if (type.equals("TLS")) {
            SSLServerSocketFactory ssf = null;
            try { // set up key manager to perform server authentication
                SSLContext ctx = SSLContext.getInstance("TLSv1.2");
                KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
                TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
                KeyStore ks = KeyStore.getInstance("JKS");
                KeyStore ts = KeyStore.getInstance("JKS");
                char[] password = "password".toCharArray();

                ks.load(new FileInputStream("src/server/servercerts/serverkeystore"), password); // keystore password
                // (storepass)
                ts.load(new FileInputStream("src/server/servercerts/servertruststore"), password); // truststore
                                                                                                   // password
                // (storepass)
                kmf.init(ks, password); // certificate password (keypass)
                tmf.init(ts); // possible to use keystore as truststore here
                ctx.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
                ssf = ctx.getServerSocketFactory();
                return ssf;
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            return ServerSocketFactory.getDefault();
        }
        return null;
    }

    private void log(String logText) {
        BufferedWriter writer = null;
        Date date = new Date();
        SimpleDateFormat dateFormatter = new SimpleDateFormat("YY-MM-dd'T'HH:mm:ss");
        String timeOfEntry = dateFormatter.format(date);
        try {
            writer = new BufferedWriter(new FileWriter("src/server/log.txt", true));
            writer.write("[" + timeOfEntry + "] " + logText + "\n");
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        // System.out.println("Logged: [" + timeOfEntry + "] " + logText + " content: "
        // + logGrab());
    }

    private ArrayList<String> logGrab() {
        ArrayList<String> logLines = new ArrayList<>();
        try (Stream<String> stream = Files.lines(Paths.get("src/server/log.txt"), StandardCharsets.UTF_8)) {
            // stream.forEach(line -> logLines.add(line));
            stream.forEach(logLines::add);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return logLines;
    }

    private static void generatePasswords() {
        List<String> names = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader("src/server/accountindex.csv"))) {
            String l;
            while ((l = br.readLine()) != null) {
                String[] values = l.split(",");
                names.add(values[2]);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        List<String> hashes = new ArrayList<String>();
        List<String> salts = new ArrayList<String>();
        for (String name : names) {
            String password = "password2";
            SecureRandom random = new SecureRandom();
            byte[] salt = new byte[16];
            random.nextBytes(salt);
            try {
                MessageDigest md = MessageDigest.getInstance("SHA-512");
                md.update(salt);
                byte[] hashedPassword = md.digest(password.getBytes(StandardCharsets.UTF_8));
                salts.add(Base64.getEncoder().encodeToString(salt));
                hashes.add(Base64.getEncoder().encodeToString(hashedPassword));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        List<String> lines = new ArrayList<String>();
        Date date = new Date();
        SimpleDateFormat dateFormatter = new SimpleDateFormat("YY-MM-dd'T'HH:mm:ss");

        for (int i = 0; i < names.size(); i++) {
            String timeOfEntry = dateFormatter.format(date);
            // set initial last failed, last login and failedattempts, respectively.
            lines.add(names.get(i) + "," + salts.get(i) + "," + hashes.get(i) + "," + timeOfEntry + "," + timeOfEntry
                    + "," + "0");
        }
        lines.forEach(System.out::println);

        Path fileName = Paths.get("src/server/passwords.csv");
        try {
            Files.write(fileName, lines, Charset.forName("utf-8"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void initPasswordFile() {
        try (BufferedReader br = new BufferedReader(new FileReader("src/server/passwords.csv"))) {
            String l;
            while ((l = br.readLine()) != null) {
                String[] values = l.split(",");
                List<String> details = new ArrayList<>();
                for (int i = 1; i < values.length; i++) {
                    details.add(values[i]);
                }
                passwordFile.put(values[0], details);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String timeLeft(String username, String password) {
        Date currentDate = new Date();
        SimpleDateFormat dateFormatter = new SimpleDateFormat("yy-MM-dd'T'HH:mm:ss");
        Date lastFailed = null;
        int failedAttempts;
        try {
            lastFailed = dateFormatter.parse(passwordFile.get(username).get(2));
            failedAttempts = Integer.parseInt(passwordFile.get(username).get(4));
            long msWait = currentDate.getTime() - lastFailed.getTime();
            if (failedAttempts > 4) {
                return new SimpleDateFormat("mm:ss:SSS").format(new Date(30000 - msWait));
            } else {
                return "credentials";
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "credentials";
        }
    }

    private boolean verifyPassword(String username, String password) {
        String hash = null;
        Date currentDate = new Date();
        SimpleDateFormat dateFormatter = new SimpleDateFormat("yy-MM-dd'T'HH:mm:ss");
        String currDateStr = dateFormatter.format(currentDate);

        if (passwordFile.get(username) != null) {
            int failedAttempts = Integer.parseInt(passwordFile.get(username).get(4));
            Date lastFailed = null;
            try {
                lastFailed = dateFormatter.parse(passwordFile.get(username).get(2));
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
            long msWait = currentDate.getTime() - lastFailed.getTime();
            System.out.println(currDateStr + "last failed:" + dateFormatter.format(lastFailed));
            System.out.println(currentDate.getTime() + "last failed ms: " + lastFailed.getTime());
            System.out.println("Failed attempts tota: " + failedAttempts);
            if (failedAttempts <= 4 || msWait > 30000) {
                System.out.println("got inside check mswait: " + msWait);
                byte[] salt = Base64.getDecoder().decode(passwordFile.get(username).get(0));
                try {
                    MessageDigest md = MessageDigest.getInstance("SHA-512");
                    md.update(salt);
                    byte[] hashedPassword = md.digest(password.getBytes(StandardCharsets.UTF_8));
                    hash = Base64.getEncoder().encodeToString(hashedPassword);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                if (passwordFile.get(username).get(1).equals(hash)) {
                    passwordFile.get(username).set(4, "0");
                    passwordFile.get(username).set(3, currDateStr);
                    return true;
                } else {
                    String failedDateTime = dateFormatter.format(currentDate);
                    failedAttempts++;
                    passwordFile.get(username).set(4, Integer.toString(failedAttempts));
                    passwordFile.get(username).set(2, failedDateTime);
                    return false;
                }
                // Failed attempts is 5, start enforce wait timer.
            } else if (msWait <= 30000) {
                System.out
                        .println("Try again in " + new SimpleDateFormat("mm:ss:SSS").format(new Date(30000 - msWait)));
                return false;
            }

            return false;
        }
        return false;

    }
}
