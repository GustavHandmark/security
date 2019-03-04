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

@SuppressWarnings("ALL")
public class PersonalView extends GridPane {

    public PersonalView(clientGUI window, String username, String division, String title, List<String> patients) {
        setAlignment(Pos.CENTER);
        setHgap(10);
        setVgap(10);
        setPadding(new Insets(25, 25, 25, 25));
        Label nameLabel = new Label("User: " + username);
        Label divisionLabel = new Label("Division: " + division);
        Label titleLabel = new Label("Title: " + title);
        add(nameLabel,0,0);
        add(divisionLabel, 0, 1);
        add(titleLabel, 0, 2);

        ObservableList<String> patientsObs = FXCollections.observableArrayList(patients);
        ListView<String> patientView = new ListView<>(patientsObs);

        add(patientView, 0, 3);

        Button viewRecordsButton = new Button("View visits");
        add(viewRecordsButton,0,4);
        viewRecordsButton.setOnAction(e -> window.viewPatientVisits(patientView.getSelectionModel().getSelectedItem()));
    }

}
