package client;

import client.gui.CertificateScreen;
import client.gui.LoginScreen;
import client.gui.PatientVisits;
import client.gui.PersonalView;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;

import java.io.IOException;
import java.security.KeyStoreException;
import java.util.ArrayList;
import java.util.Optional;

public class clientGUI extends Application {
    private BorderPane root;
    public client client;
    public String division;
    public String title;
    public String name;

    @Override
    public void start(Stage stage) {
        root = new BorderPane();
        CertificateScreen certScreen = new CertificateScreen(this);
        root.setCenter(certScreen);

        stage.setMinWidth(400);
        stage.setMinHeight(600);
        root.setMinWidth(400);
        root.setMinHeight(600);
        Scene scene = new Scene(root);

        stage.setScene(scene);
        stage.show();
    }

    public void authorizeCertificate(String username, String password) {
        client = new client();
        try {
            client.startClient(username, password);
            root.setCenter(new LoginScreen(this));

        } catch (IOException e) {
            serverErrorPopup();
        } catch (KeyStoreException e) {
            failedLoginPopup();
        }

    }

    public void login(String username, String password) {
        String res = client.authenticateCredentials(username, password);
        division = null;
        title = null;
        name = username;
        System.out.println(res);
        if (res.equals("Authentication successful")) {
            division = client.getDivision();
            title = client.getTitle();
            ArrayList<String> patients = (ArrayList<String>) client.getPatients();

            root.setCenter(new PersonalView(this, name, division, title, patients));

        } else {
            failedCredentials(res);
        }
    }

    public void goBack() {
        ArrayList<String> patients = (ArrayList<String>) client.getPatients();
        root.setCenter(new PersonalView(this,name,division,title,patients));
    }

    public void viewPatientRecord(String patientName, String patientRecordDate) {
        if (patientName != null && patientRecordDate != null) {
            String record = client.getPatientRecord(patientName, patientRecordDate);
            System.out.println(record);
            recordPopup(record);
        }
    }

    public void viewPatientVisits(String patientName) {
        if (patientName != null) {
            ArrayList<String> visits = (ArrayList<String>) client.getPatientVisits(patientName);
            root.setCenter(new PatientVisits(this, patientName, name, division, title, visits));
        }
    }


    /*
        public String writeNewPatientRecord(String patientName, String recordDate, String nurse, String newText) {
        String res = sendAndRecieveMessage("writeNewPatientRecord:"+patientName+":"+nurse+":"+newText+":"+recordDate);
        return res;


     */
    public void createNewRecord(String patientName) {
        if(patientName != null) {
            ArrayList<String> formData = createNewRecordPopup(patientName);
            if(formData.size() == 4) {
                String res = client.writeNewPatientRecord(formData.get(0),formData.get(1),formData.get(2),formData.get(3));
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle(null);
                alert.setHeaderText(null);
                alert.setContentText(res);
                alert.showAndWait();
                viewPatientVisits(patientName);
            }
        }
    }

    public void deletePatientRecord(String patientName, String patientRecordDate) {
        if (patientName != null && patientRecordDate != null) {
            String res = client.deleteRecord(patientName,patientRecordDate);
            if(res != null) {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle(null);
                alert.setHeaderText(null);
                alert.setContentText(res);
                alert.showAndWait();
                viewPatientVisits(patientName);
            }
        }
    }

    public void editPatientRecord(String patientName, String patientRecordDate) {
        if (patientName != null && patientRecordDate != null) {
            String record = client.getPatientRecord(patientName, patientRecordDate);
            String newText = recordPopupEdit(record);
            if (newText != null) {
                String res = client.editExisitingPatientRecord(patientName, patientRecordDate, newText);
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle(null);
                alert.setHeaderText(null);
                alert.setContentText(res);
                alert.showAndWait();
            }

        }
    }

    private void failedCredentials(String res) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(null);
        alert.setHeaderText(null);
        alert.setContentText(res);
        alert.showAndWait();
    }

    private ArrayList<String> createNewRecordPopup(String patientName) {
        // Create the custom dialog.
        Dialog<ArrayList<String>> dialog = new Dialog<>();
        dialog.setTitle("Create new patient record");

        ButtonType createRecord = new ButtonType("Create record", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(createRecord,ButtonType.CANCEL);

        GridPane gridPane = new GridPane();
        gridPane.setHgap(10);
        gridPane.setVgap(10);
        gridPane.setPadding(new Insets(20, 150, 10, 10));
        Label patientLabel = new Label("Create new record for patient : " + patientName);
        TextField recordDate = new TextField();
        recordDate.setPromptText("Date of record: ex 2019-03-08");
        TextField nurse = new TextField();
        nurse.setPromptText("Assign a valid nurse");
        TextField note = new TextField();
        note.setPromptText("Medical information");

        gridPane.add(patientLabel,0,0);
        gridPane.add(recordDate, 0, 1);
        gridPane.add(nurse, 0, 2);
        gridPane.add(note, 0, 3);

        dialog.getDialogPane().setContent(gridPane);

        // Convert the result to a username-password-pair when the login button is clicked.
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == createRecord) {
                ArrayList<String> data = new ArrayList<>();
                data.add(patientName);
                data.add(recordDate.getText());
                data.add(nurse.getText());
                data.add(note.getText());
                return data;
            }
            return null;
        });

        Optional<ArrayList<String>> result = dialog.showAndWait();

        if(result.isPresent()) {
            return result.get();
        }else {
            return null;
        }
    }

    private void recordPopup(String record) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Patient record");
        alert.setHeaderText(null);
        alert.setContentText(record);

        alert.showAndWait();
    }

    private String recordPopupEdit(String record) {
        TextInputDialog dialog = new TextInputDialog(record);
        dialog.setResizable(true);
        dialog.setTitle("Edit patient record");
        dialog.setHeaderText("Change or add to the patient record");

        // Traditional way to get the response value.
        Optional<String> result = dialog.showAndWait();
        if (result.isPresent()) {
            return result.get();
        } else {
            return null;
        }

    }

    private void serverErrorPopup() {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(null);
        alert.setHeaderText(null);
        alert.setContentText("Couldn't connect to server");
        alert.showAndWait();
    }

    private void failedLoginPopup() {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(null);
        alert.setHeaderText(null);
        alert.setContentText("Path to certificate is wrong or keystore password is wrong");
        alert.showAndWait();
    }
}
