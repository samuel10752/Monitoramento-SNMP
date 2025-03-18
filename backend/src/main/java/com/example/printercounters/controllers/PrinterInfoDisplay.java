package com.example.printercounters.controllers;

import com.example.printercounters.epson.EpsonL3250;
import com.example.printercounters.hp.E52645Flow;
import com.example.printercounters.hp.HP4303;
import com.example.printercounters.oki.ES5112;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import java.util.HashMap;
import java.util.Map;

public class PrinterInfoDisplay extends Application {

    private static final String DEFAULT_PASSWORD = "123";
    private TextArea webInfoArea;
    private TextField ipField;
    private TextField macField;
    private TextField serialField;
    private TextField nameprinterField;

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

        Label macLabel = new Label("Endereço MAC:");
        macField = new TextField();
        macField.setEditable(false);

        Label serialLabel = new Label("Número de Série:");
        serialField = new TextField();
        serialField.setEditable(false);

        Label namePrinterLabel = new Label("Modelo da Impressora:");
        nameprinterField = new TextField();
        nameprinterField.setEditable(false);

        Label infoLabel = new Label("Contadores da Página Web:");
        webInfoArea = new TextArea();
        webInfoArea.setEditable(false);
        webInfoArea.setWrapText(true);

        mainLayout.getChildren().addAll(ipLabel, ipField, fetchButton, macLabel, macField,
                serialLabel, serialField, namePrinterLabel, nameprinterField, infoLabel, webInfoArea);

        stage.setScene(new Scene(mainLayout));
    }

    private void fetchPrinterData() {
        if (ipField == null || ipField.getText().isEmpty()) {
            showMessage("Erro: Por favor, insira um IP válido.", Alert.AlertType.ERROR);
            return;
        }
    
        String ip = ipField.getText();
    
        try {
            String model = fetchPrinterModelFromWeb(ip);
            if (model == null || model.isEmpty()) {
                showMessage("Não foi possível identificar o modelo da impressora.", Alert.AlertType.WARNING);
                return;
            }
    
            Map<String, PrinterModel> printerInstances = new HashMap<>();
            printerInstances.put("OKI-ES5112-BECC12", new ES5112(ip, macField, serialField, nameprinterField, webInfoArea));
            printerInstances.put("HP LaserJet Flow MFP E52645", new E52645Flow(ip, macField, serialField, nameprinterField, webInfoArea));
            printerInstances.put("HP Color LaserJet Pro MFP 4303", new HP4303(ip, macField, serialField, nameprinterField, webInfoArea));
            printerInstances.put("Epson L3250", new EpsonL3250(ip, macField, serialField, nameprinterField, webInfoArea));
    
            PrinterModel printer = printerInstances.get(model);
            if (printer != null) {
                macField.setText(printer.getMacAddress());
                serialField.setText(printer.getSerialNumber());
                nameprinterField.setText(model);
                webInfoArea.setText(printer.getWebCounters());
            } else {
                showMessage("Modelo não suportado: " + model, Alert.AlertType.WARNING);
            }
        } catch (Exception e) {
            showMessage("Erro ao buscar informações: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }
    
    private String fetchPrinterModelFromWeb(String ip) {
        // Simulação de busca do modelo da impressora via IP
        return "HP LaserJet Flow MFP E52645"; // Exemplo de retorno
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
