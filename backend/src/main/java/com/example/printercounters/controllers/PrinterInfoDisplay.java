package com.example.printercounters.controllers;

import com.example.printercounters.epson.EpsonL3250;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class PrinterInfoDisplay extends Application {

    private static final String DEFAULT_PASSWORD = "123";
    private TextArea webInfoArea;
    private TextField ipField;
    private TextField macField;
    private TextField serialField;
    private TextField brandField;

    @Override
    public void start(Stage primaryStage) {
        showLoginScreen(primaryStage);
    }

    private void showLoginScreen(Stage stage) {
        VBox loginLayout = new VBox(15);
        loginLayout.setPadding(new Insets(15));

        Label passwordLabel = new Label("Digite a senha:");
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Senha...");

        Button loginButton = new Button("Entrar");
        loginButton.setOnAction(event -> {
            if (passwordField.getText().equals(DEFAULT_PASSWORD)) {
                showMainInterface(stage);
            } else {
                showMessage("Senha incorreta. Tente novamente.", Alert.AlertType.ERROR);
            }
        });

        loginLayout.getChildren().addAll(passwordLabel, passwordField, loginButton);
        stage.setScene(new Scene(loginLayout));
        stage.setTitle("Login");
        stage.show();
    }

    private void showMainInterface(Stage stage) {
        stage.setTitle("Informações da Impressora");
        VBox mainLayout = new VBox(15);
        mainLayout.setPadding(new Insets(15));

        Label ipLabel = new Label("Digite o IP da impressora:");
        ipField = new TextField();
        ipField.setPromptText("Endereço IP");

        Button fetchButton = new Button("Buscar Dados");
        fetchButton.setOnAction(event -> fetchPrinterData());

        Label brandLabel = new Label("Marca da Impressora:");
        brandField = new TextField();
        brandField.setEditable(false);

        Label macLabel = new Label("Endereço MAC:");
        macField = new TextField();
        macField.setEditable(false);

        Label serialLabel = new Label("Número de Série:");
        serialField = new TextField();
        serialField.setEditable(false);

        Label infoLabel = new Label("Contadores da Página Web:");
        webInfoArea = new TextArea();
        webInfoArea.setEditable(false);
        webInfoArea.setWrapText(true);

        mainLayout.getChildren().addAll(ipLabel, ipField, fetchButton, brandLabel, brandField, macLabel, macField,
                serialLabel, serialField, infoLabel, webInfoArea);

        stage.setScene(new Scene(mainLayout));
    }

    private void fetchPrinterData() {
        if (ipField == null || ipField.getText().isEmpty()) {
            showMessage("Erro: Por favor, insira um IP válido.", Alert.AlertType.ERROR);
            return;
        }

        String ip = ipField.getText();
        EpsonL3250 printer = new EpsonL3250(ip, macField, serialField, brandField, webInfoArea);
        printer.fetchPrinterInfo();
        printer.fetchWebPageData();
    }

    private void showMessage(String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(type == Alert.AlertType.ERROR ? "Erro" : "Informação");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
