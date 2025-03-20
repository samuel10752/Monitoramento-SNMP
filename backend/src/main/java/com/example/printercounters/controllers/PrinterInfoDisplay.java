package com.example.printercounters.controllers;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.snmp4j.CommunityTarget;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.UdpAddress;
import org.snmp4j.smi.VariableBinding;
import org.snmp4j.transport.DefaultUdpTransportMapping;

import com.example.printercounters.epson.InfoEpson;
import com.example.printercounters.hp.InfoHP;

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
            LOGGER.warning("Campo IP vazio. Nenhum dado será buscado.");
            showMessage("Erro: Por favor, insira um IP válido.", Alert.AlertType.ERROR);
            return;
        }

        String ip = ipField.getText();
        LOGGER.info("Buscando dados da impressora com IP: " + ip);

        String detectedBrand = detectPrinterBrand(ip);
        if (detectedBrand == null) {
            LOGGER.warning("Não foi possível determinar a marca da impressora para o IP: " + ip);
            showMessage("Erro: Não foi possível determinar a marca da impressora.", Alert.AlertType.ERROR);
            return;
        }

        brandField.setText(detectedBrand);
        LOGGER.info("Marca da impressora detectada: " + detectedBrand);

        try {
            PrinterModel printer;
            if (detectedBrand.equalsIgnoreCase("HP")) {
                LOGGER.info("Criando instância de impressora HP.");
                String selectedModel = "HP4303";
                printer = InfoHP.createHPPrinter(ip, selectedModel, macField, serialField, brandField, webInfoArea);
            } else if (detectedBrand.equalsIgnoreCase("Epson")) {
                LOGGER.info("Criando instância de impressora Epson.");
                String selectedModel = "EpsonL3250";
                printer = InfoEpson.createEpsonPrinter(ip, selectedModel, macField, serialField, brandField, webInfoArea);
            } else {
                LOGGER.warning("Marca da impressora não suportada: " + detectedBrand);
                showMessage("Erro: Marca da impressora não suportada.", Alert.AlertType.ERROR);
                return;
            }

            printer.fetchPrinterInfo();
            printer.fetchWebPageData();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erro ao processar os dados da impressora", e);
            showMessage("Erro ao processar os dados da impressora: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private String detectPrinterBrand(String ip) {
        LOGGER.info("Detectando a marca da impressora para o IP: " + ip);

        String printerModel = getSnmpValue("1.3.6.1.2.1.1.5.0", ip);
        if (printerModel != null && !printerModel.isEmpty()) {
            LOGGER.info("Modelo da impressora detectado via SNMP: " + printerModel);
            if (printerModel.toLowerCase().contains("epson")) {
                return "Epson";
            } else if (printerModel.toLowerCase().contains("hp")) {
                return "HP";
            }
        }

        String brandFromWeb = detectBrandFromWeb(ip);
        if (brandFromWeb != null) {
            LOGGER.info("Marca detectada via página web: " + brandFromWeb);
            return brandFromWeb;
        }

        LOGGER.warning("Não foi possível determinar a marca da impressora para o IP: " + ip);
        return null;
    }

    private String getSnmpValue(String oid, String ip) {
        try {
            LOGGER.info("Consultando valor SNMP para OID: " + oid + " no IP: " + ip);
            Snmp snmp = new Snmp(new DefaultUdpTransportMapping());
            snmp.listen();

            CommunityTarget<UdpAddress> target = new CommunityTarget<>();
            target.setCommunity(new OctetString("public"));
            target.setAddress(new UdpAddress(ip + "/161"));
            target.setRetries(2);
            target.setTimeout(3000);
            target.setVersion(org.snmp4j.mp.SnmpConstants.version2c);

            PDU pdu = new PDU();
            pdu.add(new VariableBinding(new OID(oid)));
            pdu.setType(PDU.GET);

            ResponseEvent response = snmp.get(pdu, target);
            if (response != null && response.getResponse() != null) {
                String value = response.getResponse().get(0).getVariable().toString();
                LOGGER.info("Valor SNMP capturado: " + value);
                snmp.close();
                return value;
            }

            LOGGER.warning("Resposta SNMP nula para o OID: " + oid);
            snmp.close();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erro ao buscar valor SNMP para OID: " + oid, e);
        }
        return null;
    }

    private String detectBrandFromWeb(String ip) {
        String url = "http://" + ip;
        try {
            LOGGER.info("Conectando à página web da impressora no IP: " + ip);
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0")
                    .ignoreHttpErrors(true)
                    .ignoreContentType(true)
                    .timeout(5000)
                    .get();

            String title = doc.title();
            LOGGER.info("Título da página capturado: " + title);
            if (title.toLowerCase().contains("epson")) {
                return "Epson";
            } else if (title.toLowerCase().contains("hp")) {
                return "HP";
            }

            String bodyText = doc.body().text();
            LOGGER.info("Texto do corpo da página capturado: " + bodyText);
            if (bodyText.toLowerCase().contains("epson")) {
                return "Epson";
            } else if (bodyText.toLowerCase().contains("hp")) {
                return "HP";
            }
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Erro ao acessar a página web no IP: " + ip, e);
        }
        return null;
    }
}
