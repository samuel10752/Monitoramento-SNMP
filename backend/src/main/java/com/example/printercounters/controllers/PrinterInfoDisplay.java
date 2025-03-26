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
import javafx.scene.layout.HBox;

public class PrinterInfoDisplay extends Application {

    private static final String DEFAULT_PASSWORD = "123"; // Senha fixa padrão
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
        LOGGER.info("Carregando tela de login...");
        VBox loginLayout = new VBox(15);
        loginLayout.setPadding(new Insets(15));
        loginLayout.setStyle("-fx-background-color: #e0f7fa;");

        Label passwordLabel = new Label("Digite a senha:");
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Senha...");
        passwordField.setStyle("-fx-font-size: 14px;");

        // Criar botões com tamanho fixo
        Button okButton = new Button("Ok");
        okButton.setStyle("-fx-background-color: #00796b; -fx-text-fill: white; -fx-font-size: 14px;");
        okButton.setPrefWidth(100); // Tamanho fixo do botão
        okButton.setOnAction(event -> {
            String inputPassword = passwordField.getText();
            if (DEFAULT_PASSWORD.equals(inputPassword)) {
                LOGGER.info("Senha correta. Carregando a interface principal...");
                showMainInterface(stage);
            } else {
                LOGGER.warning("Senha incorreta. Acesso negado.");
                showMessage("Senha incorreta. Tente novamente.", Alert.AlertType.ERROR);
            }
        });

        Button cancelButton = new Button("Cancelar");
        cancelButton.setStyle("-fx-background-color: #c62828; -fx-text-fill: white; -fx-font-size: 14px;");
        cancelButton.setPrefWidth(100); // Tamanho fixo do botão
        cancelButton.setOnAction(event -> {
            LOGGER.info("Operação cancelada pelo usuário.");
            stage.close();
        });

        // Organizar botões horizontalmente
        HBox buttonLayout = new HBox(10);
        buttonLayout.setPadding(new Insets(10));
        buttonLayout.getChildren().addAll(okButton, cancelButton);

        loginLayout.getChildren().addAll(passwordLabel, passwordField, buttonLayout);

        stage.setScene(new Scene(loginLayout, 220, 150)); // Define o tamanho inicial da janela
        stage.setResizable(false); // Impede a expansão ou redimensionamento da janela
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

        // Layout principal com espaçamento e padding
        VBox mainLayout = new VBox(20); // Espaçamento entre elementos
        mainLayout.setPadding(new Insets(20));
        mainLayout.setStyle("-fx-background-color: #f5f5f5;"); // Cor de fundo cinza claro

        // Estilo para os rótulos
        String labelStyle = "-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #333333;";

        Label ipLabel = new Label("Digite o IP da impressora:");
        ipLabel.setStyle(labelStyle);
        ipField = new TextField();
        ipField.setPromptText("Endereço IP");
        ipField.setStyle("-fx-font-size: 12px; -fx-padding: 5;");

        Label brandLabel = new Label("Modelo da Impressora:");
        brandLabel.setStyle(labelStyle);
        brandField = new TextField();
        brandField.setEditable(false);
        brandField.setStyle("-fx-font-size: 12px; -fx-padding: 5; -fx-background-color: #e8e8e8;");

        Label macLabel = new Label("Endereço MAC:");
        macLabel.setStyle(labelStyle);
        macField = new TextField();
        macField.setEditable(false);
        macField.setStyle("-fx-font-size: 12px; -fx-padding: 5; -fx-background-color: #e8e8e8;");

        Label serialLabel = new Label("Número de Série:");
        serialLabel.setStyle(labelStyle);
        serialField = new TextField();
        serialField.setEditable(false);
        serialField.setStyle("-fx-font-size: 12px; -fx-padding: 5; -fx-background-color: #e8e8e8;");

        Label infoLabel = new Label("Contadores da Página Web:");
        infoLabel.setStyle(labelStyle);
        webInfoArea = new TextArea();
        webInfoArea.setEditable(false);
        webInfoArea.setWrapText(true);
        webInfoArea.setStyle("-fx-font-size: 12px; -fx-padding: 5; -fx-background-color: #e8e8e8;");

        // Botão para buscar dados
        Button fetchButton = new Button("Buscar Dados");
        fetchButton.setStyle(
                "-fx-background-color:rgb(0, 47, 255); -fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;");
        fetchButton.setOnAction(event -> fetchPrinterData());

        // Botão adicional para recarregar dados
        Button reloadButton = new Button("Recarregar Contadores");
        reloadButton.setStyle(
                "-fx-background-color:rgb(255, 165, 0); -fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;");
        reloadButton.setOnAction(event -> reloadCounters());

        // Adicionar os botões em um layout horizontal
        HBox buttonLayout = new HBox(10); // Espaçamento entre os botões
        buttonLayout.setPadding(new Insets(10));
        buttonLayout.getChildren().addAll(fetchButton, reloadButton);

        // Adicionar elementos ao layout principal
        mainLayout.getChildren().addAll(
                ipLabel, ipField,
                brandLabel, brandField,
                macLabel, macField,
                serialLabel, serialField,
                infoLabel, webInfoArea,
                buttonLayout);

        // Ajustar cena com largura e altura específicas
        stage.setScene(new Scene(mainLayout, 350, 620)); // Tamanho da janela
    }

    // Método para buscar contadores novamente
    private void reloadCounters() {
        LOGGER.info("Recarregando contadores da impressora...");
        // Aqui você adicionará a lógica para recarregar os contadores, conforme
        // necessário
        showMessage("Dados do contador recarregados com sucesso!", Alert.AlertType.INFORMATION);
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
