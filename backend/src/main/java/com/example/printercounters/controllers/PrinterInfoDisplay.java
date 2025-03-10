package com.example.printercounters.controllers;

import com.example.printercounters.epson.EpsonL3250;
import com.example.printercounters.hp.E52645Flow;
import com.example.printercounters.hp.HP4303;
import com.example.printercounters.styles.Estilo;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

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
        Estilo.aplicarEstiloPasswordField(passwordField);

        Button loginButton = new Button("Entrar");
        loginButton.setPrefSize(130, 30); // Ajusta o tamanho do botão "Entrar"
        Estilo.aplicarEstiloButton(loginButton);

        Button cancelButton = new Button("Cancelar");
        cancelButton.setPrefSize(130, 30); // Ajusta o tamanho do botão "Cancelar"
        Estilo.aplicarEstiloButton(cancelButton);
        cancelButton.setOnAction(event -> {
            // Fecha o programa
            stage.close();
        });

        Label messageLabel = new Label(); // Exibe mensagens de erro

        loginButton.setOnAction(event -> {
            String enteredPassword = passwordField.getText();
            if (enteredPassword.equals(DEFAULT_PASSWORD)) {
                // Senha correta, abre a interface principal
                showMainInterface(stage);
            } else {
                // Senha incorreta, exibe mensagem de erro
                messageLabel.setText("Senha incorreta. Tente novamente.");
                passwordField.setStyle("-fx-background-color: red;");
                animateShake(passwordField); // Animação de tremer
                showAlert("Erro", "Senha incorreta. Tente novamente.");
            }
        });

        // Adiciona os botões em um HBox
        HBox buttonBox = new HBox(10);
        buttonBox.getChildren().addAll(loginButton, cancelButton);
        buttonBox.setAlignment(Pos.CENTER_RIGHT); // Alinha o HBox à direita

        loginLayout.getChildren().addAll(passwordLabel, passwordField, buttonBox, messageLabel);
        Estilo.aplicarEstiloLoginPane(loginLayout);

        // Configuração da cena de login
        Scene loginScene = new Scene(loginLayout, 300, 130);
        stage.setScene(loginScene);
        stage.setTitle("Login");
        
        // Define o tamanho fixo da janela
        stage.setMinWidth(300);
        stage.setMaxWidth(300);
        stage.setMinHeight(200);
        stage.setMaxHeight(200);

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
        Estilo.aplicarEstiloTextField(ipField);

        Button fetchButton = new Button("Buscar Informações");
        Estilo.aplicarEstiloButton(fetchButton);

        macField = new TextField();
        macField.setEditable(false);
        macField.setPromptText("Endereço MAC aparecerá aqui...");
        Estilo.aplicarEstiloTextField(macField);

        serialField = new TextField();
        serialField.setEditable(false);
        serialField.setPromptText("Número de Série aparecerá aqui...");
        Estilo.aplicarEstiloTextField(serialField);

        nameprinterField = new TextField();
        nameprinterField.setEditable(false);
        nameprinterField.setPromptText("Nome da Impressora aparecerá aqui...");
        Estilo.aplicarEstiloTextField(nameprinterField);

        webInfoArea = new TextArea();
        webInfoArea.setEditable(false);
        webInfoArea.setWrapText(true);
        Estilo.aplicarEstiloTextArea(webInfoArea);

        // Adiciona os componentes ao layout principal
        mainLayout.getChildren().addAll(ipLabel, ipField, fetchButton, new Label("MAC Address:"), macField, new Label("Número de Série:"), serialField,
                new Label("Nome da Impressora:"), nameprinterField, new Label("Contadores da Página Web:"), webInfoArea);
        Estilo.aplicarEstiloRootPane(mainLayout);

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
                macField.setText("N/A");
                serialField.setText("N/A");
                nameprinterField.setText("N/A");
                webInfoArea.setText("N/A");

                // Verifica a marca e o modelo da impressora com base no IP
                // Aqui você pode adicionar lógica para escolher a marca e o modelo com base em outro critério se necessário
                EpsonL3250 EpsonL3250 = new EpsonL3250(ip, macField, serialField, nameprinterField, webInfoArea);
                E52645Flow hpE52645Flow = new E52645Flow(ip, macField, serialField, nameprinterField, webInfoArea);
                HP4303 HP4303 = new HP4303(ip, macField, serialField, nameprinterField, webInfoArea);
                EpsonL3250.fetchPrinterInfo();
                EpsonL3250.fetchWebPageData();
                hpE52645Flow.fetchPrinterInfo();
                hpE52645Flow.fetchWebPageData();
                HP4303.fetchPrinterInfo();
                HP4303.fetchWebPageData();
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

    private void animateShake(TextField textField) {
        Timeline timeline = new Timeline(
            new KeyFrame(Duration.millis(0), event -> textField.setTranslateX(0)),
            new KeyFrame(Duration.millis(50), event -> textField.setTranslateX(-10)),
            new KeyFrame(Duration.millis(100), event -> textField.setTranslateX(10)),
            new KeyFrame(Duration.millis(150), event -> textField.setTranslateX(-10)),
            new KeyFrame(Duration.millis(200), event -> textField.setTranslateX(10)),
            new KeyFrame(Duration.millis(250), event -> textField.setTranslateX(0))
        );
        timeline.play();
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
