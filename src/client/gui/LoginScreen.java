package client.gui;

import client.clientGUI;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;

public class LoginScreen extends GridPane {

    public LoginScreen(clientGUI window) {
        setAlignment(Pos.CENTER);
        setHgap(10);
        setVgap(10);
        setPadding(new Insets(25, 25, 25, 25));

        Label usernameLabel = new Label("Path to certificate:");
        add(usernameLabel, 0, 1);

        TextField usernameTextField = new TextField();
        add(usernameTextField, 1, 1);

        Label passwordLabel = new Label("Keystore password:");
        add(passwordLabel, 0, 2);

        PasswordField passwordField = new PasswordField();
        add(passwordField, 1, 2);

        Button loginButton = new Button("Sign in");
        HBox loginButtonBox = new HBox(10);
        loginButtonBox.setAlignment(Pos.BOTTOM_RIGHT);
        loginButtonBox.getChildren().add(loginButton);
        add(loginButtonBox, 1, 4);

        loginButton.setOnAction(e -> window.login(usernameTextField.getText(), passwordField.getText()));
    }
}
