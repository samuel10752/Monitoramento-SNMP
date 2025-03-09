package com.example.printercounters.controllers;

import com.example.printercounters.epson.EpsonL3250;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class PrinterInfoDisplay extends Application {

    private static final String DEFAULT_PASSWORD = "monitoramento"; // Senha padrão
    private TextField ipField; // Campo de entrada do IP
    private TextField macField; // Exibe o endereço MAC
    private TextField serialField; // Exibe o número de série
    private TextField nameprinterField; // Exibe o nome da impressora
    private TextArea webInfoArea; // Exibe informações da página web

    @Override
    public void start(Stage primaryStage) {
        showLoginScreen(primaryStage);
    }

    private void showLoginScreen(Stage stage) {
        // Layout da tela de login
        VBox loginLayout = new VBox(15);
        loginLayout.setPadding(new Insets(15));

        Label passwordLabel = new Label("Digite a senha:");
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Senha...");
        Button loginButton = new Button("Entrar");

        Label messageLabel = new Label(); // Exibe mensagens de erro

        loginButton.setOnAction(event -> {
            String enteredPassword = passwordField.getText();
            if (enteredPassword.equals(DEFAULT_PASSWORD)) {
                // Senha correta, abre a interface principal
                showMainInterface(stage);
            } else {
                // Senha incorreta, exibe mensagem de erro
                messageLabel.setText("Senha incorreta. Tente novamente.");
            }
        });

        loginLayout.getChildren().addAll(passwordLabel, passwordField, loginButton, messageLabel);

        // Configuração da cena de login
        Scene loginScene = new Scene(loginLayout, 300, 200);
        stage.setScene(loginScene);
        stage.setTitle("Login");
        stage.show();
    }

    private void showMainInterface(Stage stage) {
        stage.setTitle("Informações da Impressora");

        // Layout principal
        VBox mainLayout = new VBox(15);
        mainLayout.setPadding(new Insets(15));

        // Campo de entrada do IP e botão de buscar informações
        Label ipLabel = new Label("IP da Impressora:");
        ipField = new TextField();
        ipField.setPromptText("Exemplo: 192.168.1.1");
        Button fetchButton = new Button("Buscar Informações");
        
        macField = new TextField();
        macField.setEditable(false);
        macField.setPromptText("Endereço MAC aparecerá aqui...");

        serialField = new TextField();
        serialField.setEditable(false);
        serialField.setPromptText("Número de Série aparecerá aqui...");

        nameprinterField = new TextField();
        nameprinterField.setEditable(false);
        nameprinterField.setPromptText("Nome da Impressora aparecerá aqui...");

        webInfoArea = new TextArea();
        webInfoArea.setEditable(false);
        webInfoArea.setWrapText(true);

        // Adiciona os componentes ao layout principal
        mainLayout.getChildren().addAll(ipLabel, ipField, fetchButton, new Label("MAC Address:"), macField, new Label("Número de Série:"), serialField,
                new Label("Nome da Impressora:"), nameprinterField, new Label("Contadores da Página Web:"), webInfoArea);

        // Configuração da cena principal
        Scene mainScene = new Scene(mainLayout, 600, 500);
        stage.setScene(mainScene);

        // Ação do botão para buscar informações
        fetchButton.setOnAction(event -> {
            String ip = ipField.getText();
            if (ip.isEmpty()) {
                showMessage("Erro: Por favor, insira um IP válido.", Alert.AlertType.ERROR);
            } else {
                // Limpa os campos antes de buscar informações
                macField.setText("");
                serialField.setText("");
                nameprinterField.setText("");
                webInfoArea.clear();

                // Verifica a marca e o modelo da impressora com base no IP
                // Aqui você pode adicionar lógica para escolher a marca e o modelo com base em outro critério se necessário
                EpsonL3250 epson = new EpsonL3250(ip, macField, serialField, nameprinterField, webInfoArea);
                epson.fetchPrinterInfo();
                epson.fetchWebPageData();
            }
        });
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
