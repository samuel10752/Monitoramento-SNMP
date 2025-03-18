package com.example.printercounters.MIBExtractor;

import java.awt.Desktop;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.snmp4j.CommunityTarget;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.TransportMapping;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.*;

import org.snmp4j.transport.DefaultUdpTransportMapping;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class MIBExtractorGUI extends Application {

    private TextArea logArea;
    private File lastGeneratedFile;

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("MIB Extractor");

        // Elementos da interface
        Label label = new Label("Digite o IP da Impressora:");
        TextField ipField = new TextField();
        ipField.setPromptText("Exemplo: 192.168.1.1");

        Button verifyButton = new Button("Verificar MIB");
        Button extractButton = new Button("Extrair MIB");
        Button openFileButton = new Button("Exibir Arquivo");

        openFileButton.setDisable(true);

        logArea = new TextArea();
        logArea.setEditable(false);

        // Ação para verificar a MIB antes da extração
        verifyButton.setOnAction(event -> {
            String ip = ipField.getText();
            if (ip.isEmpty()) {
                showMessage("Erro: Por favor, insira um IP válido.", Alert.AlertType.ERROR);
            } else {
                boolean isReachable = verifyMIB(ip);
                if (isReachable) {
                    showMessage("Conexão SNMP bem-sucedida!", Alert.AlertType.INFORMATION);
                } else {
                    showMessage("Falha na conexão SNMP!", Alert.AlertType.ERROR);
                }
            }
        });

        // Ação para extração da MIB (snmpwalk)
        extractButton.setOnAction(event -> {
            String ip = ipField.getText();
            if (ip.isEmpty()) {
                showMessage("Erro: Por favor, insira um IP válido.", Alert.AlertType.ERROR);
            } else {
                extractMIB(ip);
                openFileButton.setDisable(lastGeneratedFile == null);
            }
        });

        openFileButton.setOnAction(event -> openGeneratedFile(primaryStage));

        // Layout
        HBox buttonBox = new HBox(10, verifyButton, extractButton, openFileButton);
        VBox layout = new VBox(10);
        layout.setPadding(new Insets(15));
        layout.getChildren().addAll(label, ipField, buttonBox, logArea);

        Scene scene = new Scene(layout, 600, 400);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private boolean verifyMIB(String ip) {
        logArea.clear();
        logArea.appendText("Verificando conectividade SNMP para o IP: " + ip + "\n");

        try {
            TransportMapping<UdpAddress> transport = new DefaultUdpTransportMapping();
            Snmp snmp = new Snmp(transport);
            transport.listen();

            Address targetAddress = GenericAddress.parse("udp:" + ip + "/161");
            CommunityTarget target = new CommunityTarget();
            target.setCommunity(new OctetString("public"));
            target.setAddress(targetAddress);
            target.setRetries(2);
            target.setTimeout(1500);
            target.setVersion(SnmpConstants.version2c);

            PDU pdu = new PDU();
            pdu.add(new VariableBinding(new OID("1.3.6.1.2.1.1.1.0")));
            pdu.setType(PDU.GET);

            ResponseEvent response = snmp.get(pdu, target);
            snmp.close();

            if (response.getResponse() == null) {
                logArea.appendText("Nenhuma resposta SNMP recebida.\n");
                return false;
            } else {
                logArea.appendText("Resposta SNMP recebida: " + response.getResponse().get(0).getVariable() + "\n");
                return true;
            }

        } catch (IOException e) {
            logArea.appendText("Erro ao conectar via SNMP: " + e.getMessage() + "\n");
            return false;
        }
    }

    private void extractMIB(String ip) {
        logArea.clear();
        logArea.appendText("Executando snmpwalk na impressora " + ip + "...\n");

        try {
            File directory = new File("output");
            if (!directory.exists()) {
                directory.mkdir();
            }

            String filePath = "output/" + ip.replace(".", "_") + ".txt";
            lastGeneratedFile = new File(filePath);
            FileWriter fileWriter = new FileWriter(lastGeneratedFile);
            fileWriter.write("SNMP Walk - Impressora IP: " + ip + "\n\n");

            TransportMapping<UdpAddress> transport = new DefaultUdpTransportMapping();
            Snmp snmp = new Snmp(transport);
            transport.listen();

            Address targetAddress = GenericAddress.parse("udp:" + ip + "/161");
            CommunityTarget<UdpAddress> target = new CommunityTarget<>();
            target.setCommunity(new OctetString("public"));
            target.setAddress((UdpAddress) targetAddress);
            target.setRetries(2);
            target.setTimeout(1500);
            target.setVersion(SnmpConstants.version2c);

            OID baseOid = new OID("1.3.6.1.2.1"); // Início da MIB
            OID currentOid = new OID(baseOid);

            boolean finished = false;
            while (!finished) {
                PDU pdu = new PDU();
                pdu.add(new VariableBinding(currentOid));
                pdu.setType(PDU.GETNEXT);

                ResponseEvent response = snmp.getNext(pdu, target);
                PDU responsePDU = response.getResponse();

                if (responsePDU == null) {
                    logArea.appendText("Fim da MIB ou nenhuma resposta.\n");
                    finished = true;
                } else {
                    VariableBinding vb = responsePDU.get(0);
                    OID nextOid = vb.getOid();

                    if (!nextOid.startsWith(baseOid)) {
                        finished = true;
                        break;
                    }

                    String value = vb.getVariable().toString();
                    String line = nextOid + " = " + value;
                    logArea.appendText(line + "\n");
                    fileWriter.write(line + "\n");

                    currentOid = nextOid;
                }
            }

            snmp.close();
            fileWriter.close();
            showMessage("Extração concluída! Arquivo salvo em: " + filePath, Alert.AlertType.INFORMATION);

        } catch (IOException e) {
            showMessage("Erro ao extrair a MIB: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void openGeneratedFile(Stage stage) {
        if (lastGeneratedFile != null && lastGeneratedFile.exists()) {
            try {
                Desktop.getDesktop().open(lastGeneratedFile);
            } catch (IOException e) {
                showMessage("Erro ao abrir o arquivo: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        } else {
            showMessage("Nenhum arquivo gerado para exibir.", Alert.AlertType.WARNING);
        }
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
