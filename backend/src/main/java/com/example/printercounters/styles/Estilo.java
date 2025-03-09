package com.example.printercounters.styles;

import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

public class Estilo {

    public static void aplicarEstiloButton(Button button) {
        button.setStyle("-fx-background-color: lightblue; -fx-text-fill: black; -fx-font-size: 14px;");
    }

    public static void aplicarEstiloTextField(TextField textField) {
        textField.setStyle("-fx-background-color: white; -fx-text-fill: black;");
    }

    public static void aplicarEstiloTextArea(TextArea textArea) {
        textArea.setStyle("-fx-background-color: white; -fx-text-fill: black;");
    }

    public static void aplicarEstiloVBox(VBox vbox) {
        vbox.setStyle("-fx-background-color:rgb(255, 255, 255);");
    }

    public static void aplicarEstiloLoginPane(VBox loginPane) {
        loginPane.setStyle("-fx-background-color:rgb(255, 255, 255);");
    }

    public static void aplicarEstiloRootPane(VBox rootPane) {
        rootPane.setStyle("-fx-background-color:rgb(255, 255, 255);");
    }

    public static void aplicarEstiloPasswordField(TextField passwordField) {
        passwordField.setStyle("-fx-background-color: white; -fx-text-fill: black; -fx-border-color: black; -fx-border-width: 2px;");
    }
}
