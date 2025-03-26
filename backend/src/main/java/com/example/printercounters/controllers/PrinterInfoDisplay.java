package com.example.printercounters.controllers;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.example.printercounters.epson.InfoEpson;
import com.example.printercounters.hp.InfoHP;
import com.example.printercounters.oki.infooki;

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

    private static final String DEFAULT_PASSWORD = "123";
    private static final Logger LOGGER = Logger.getLogger(PrinterInfoDisplay.class.getName());
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
                LOGGER.info("Senha correta. Carregando a interface principal.");
                showMainInterface(stage);
            } else {
                LOGGER.warning("Senha incorreta. Acesso negado.");
                showMessage("Senha incorreta. Tente novamente.", Alert.AlertType.ERROR);
            }
        });

        loginLayout.getChildren().addAll(passwordLabel, passwordField, loginButton);
        stage.setScene(new Scene(loginLayout));
        stage.setTitle("Login");
        stage.show();
    }

    private void showMessage(String message, Alert.AlertType type) {
        LOGGER.info("Exibindo mensagem: " + message);
        Alert alert = new Alert(type);
        alert.setTitle(type == Alert.AlertType.ERROR ? "Erro" : "Informação");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
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

        Label brandLabel = new Label("Modelo da Impressora:");
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

        mainLayout.getChildren().addAll(ipLabel, ipField, fetchButton, brandLabel,
                brandField,
                macLabel, macField, serialLabel, serialField, infoLabel, webInfoArea);

        stage.setScene(new Scene(mainLayout));
    }

    private void fetchPrinterData() {
        if (ipField == null || ipField.getText().isEmpty()) {
            LOGGER.warning("Campo IP vazio. Nenhum dado será buscado.");
            showMessage("Erro: Por favor, insira um IP válido.", Alert.AlertType.ERROR);
            return;
        }

        String ip = ipField.getText();
        LOGGER.info("Buscando dados da impressora com IP: " + ip);

        try {
            String detectedModel = null;
            PrinterModel printer = null;

            // Verificar modelos HP
            detectedModel = InfoHP.detectPrinterModelHP(ip);
            if (detectedModel != null && (detectedModel.equals("HP4303") || detectedModel.equals("E52645Flow"))) {
                LOGGER.info("Identificado como impressora HP: " + detectedModel);
                printer = InfoHP.createHPPrinter(ip, detectedModel, macField, serialField, brandField, webInfoArea);

                // Verificar modelos Epson
            } else if ((detectedModel = InfoEpson.detectPrinterModelEpson(ip)) != null &&
                    (detectedModel.equals("L3250") || detectedModel.equals("L3150")
                            || detectedModel.equals("L6260"))) {
                LOGGER.info("Identificado como impressora Epson: " + detectedModel);
                printer = InfoEpson.createEpsonPrinter(ip, detectedModel, macField, serialField, brandField,
                        webInfoArea);

                // Verificar modelos OKI
            } else if ((detectedModel = infooki.detectPrinterModelOKI(ip)) != null &&
                    (detectedModel.equals("ES5112") || detectedModel.equals("ES4172LP"))) {
                LOGGER.info("Identificado como impressora OKI: " + detectedModel);
                printer = infooki.createOKIPrinter(ip, detectedModel, macField, serialField, brandField, webInfoArea);
            }

            // Exibir mensagem de erro se o modelo não for detectado
            if (printer == null) {
                LOGGER.warning("Impressora não suportada para o IP: " + ip);
                showMessage("Erro: Impressora não suportada ou modelo não identificado.", Alert.AlertType.ERROR);
                return;
            }

            // Buscar informações específicas do modelo detectado
            LOGGER.info("Buscando informações específicas do modelo detectado: " + detectedModel);
            printer.fetchPrinterInfo();
            printer.fetchWebPageData();

            // Formatar o endereço MAC se disponível
            if (macField.getText() != null && !macField.getText().isEmpty()) {
                String formattedMac = formatMacAddress(macField.getText());
                macField.setText(formattedMac);
                LOGGER.info("Endereço MAC formatado: " + formattedMac);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erro ao buscar os dados da impressora", e);
            showMessage("Erro ao processar os dados da impressora: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    public static String formatMacAddress(String rawMac) {
        // Remove caracteres desnecessários, como pontos ou traços
        String cleanedMac = rawMac.replaceAll("[^a-fA-F0-9]", "");

        // Formatar em pares de caracteres separados por dois pontos
        StringBuilder formattedMac = new StringBuilder();
        for (int i = 0; i < cleanedMac.length(); i += 2) {
            formattedMac.append(cleanedMac.substring(i, i + 2).toUpperCase());
            if (i < cleanedMac.length() - 2) {
                formattedMac.append(":");
            }
        }

        return formattedMac.toString();
    }

}
