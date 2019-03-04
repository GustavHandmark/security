package client.gui;

import client.clientGUI;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.GridPane;

import java.util.List;



public class PatientVisits extends GridPane {

    public PatientVisits(clientGUI window,String patientName, String username, String division, String title, List<String> patientVisits) {
        setAlignment(Pos.CENTER);
        setHgap(10);
        setVgap(10);
        setPadding(new Insets(25, 25, 25, 25));
        Label nameLabel = new Label("User: " + username);
        Label divisionLabel = new Label("Division: " + division);
        Label titleLabel = new Label("Title: " + title);
        Label patientLabel = new Label("Patient name: " + patientName);
        add(nameLabel,0,0);
        add(divisionLabel, 0, 1);
        add(titleLabel, 0, 2);
        add(patientLabel,0,3);
        ObservableList<String> patientsObs = FXCollections.observableArrayList(patientVisits);
        ListView<String> patientView = new ListView<>(patientsObs);

        add(patientView, 0, 4);

        Button viewRecordsButton = new Button("View Record");
        add(viewRecordsButton,0,5);

        Button editRecordsButton = new Button ("Edit Record");
        add(editRecordsButton,0,6);

        Button goBack = new Button ("Back");
        add(goBack,1,6);

        goBack.setOnAction(e -> window.goBack());

        viewRecordsButton.setOnAction(e -> window.viewPatientRecord(patientName,patientView.getSelectionModel().getSelectedItem()));
        editRecordsButton.setOnAction(e -> window.editPatientRecord(patientName,patientView.getSelectionModel().getSelectedItem()));
        if (title.equals("doctor")) {
            Button createRecordButton = new Button("Create new record");
            createRecordButton.setOnAction(e -> window.createNewRecord(patientName));
            add(createRecordButton, 0, 7);
        }
        if(title.equals("government")) {
            Button deleteRecordButton = new Button("Delete patient record");
            deleteRecordButton.setOnAction(e -> window.deletePatientRecord(patientName,patientView.getSelectionModel().getSelectedItem()));
            add(deleteRecordButton,0,9);
        }
    }

    private void createNewRecord() {
        //TODO: popup?
    }
}
