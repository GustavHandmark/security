package client;

import client.gui.LoginScreen;
import client.gui.PersonalView;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

import java.io.IOException;
import java.security.KeyStoreException;
import java.util.ArrayList;

public class clientGUI extends Application {
    private BorderPane root;

    @Override
    public void start(Stage stage) {
        root = new BorderPane();

        LoginScreen loginScreen = new LoginScreen(this);
        root.setCenter(loginScreen);

        stage.setMinWidth(400);
        stage.setMinHeight(600);
        root.setMinWidth(400);
        root.setMinHeight(600);
        Scene scene = new Scene(root);

        stage.setScene(scene);
        stage.show();
    }

    public void login(String username, String password) {
        client client = new client();
        try {
            client.startClient(username, password);
            String res = client.authenticateCredentials(username,"password2");
            String division = null;
            String title = null;
            String name = username;
            System.out.println(res);
            if(res.equals("Authentication successful")) {
                division = client.getDivision();
                title = client.getTitle();
            }

            client.getPatients();
            ArrayList<String> patients = (ArrayList<String>) client.getPatients();
            patients.forEach(System.out::println);

            ArrayList<String> records = new ArrayList<>();


            root.setCenter(new PersonalView(name,division, title, patients));

        } catch (IOException e) {
            serverErrorPopup();
        } catch (KeyStoreException e) {
            failedLoginPopup();
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
        alert.setContentText("Username or password incorrect");
        alert.showAndWait();
    }
}
