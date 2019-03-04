package client.gui;

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

    public PersonalView(String username,String division, String title, List<String> patients) {
        setAlignment(Pos.CENTER);
        setHgap(10);
        setVgap(10);
        setPadding(new Insets(25, 25, 25, 25));
        Label nameLabel = new Label("User: " + username);
        Label divisionLabel = new Label("Division: " + division);
        Label titleLabel = new Label("Title: " + title);
        add(divisionLabel, 0, 0);
        add(titleLabel, 0, 1);

        ObservableList<String> patientsObs = FXCollections.observableArrayList(patients);
        ListView<String> patientView = new ListView<>(patientsObs);

        add(patientView, 0, 2);

        if (title.equals("doctor")) {
            Button createRecordButton = new Button("Create new record");
            createRecordButton.setOnAction(e -> createNewRecord());
            add(createRecordButton, 0, 3);
        }
    }

    private void createNewRecord() {
        //TODO: popup?
    }
}
