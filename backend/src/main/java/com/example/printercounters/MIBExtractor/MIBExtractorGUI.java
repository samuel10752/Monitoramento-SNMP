package com.example.printercounters.MIBExtractor;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.snmp4j.*;

import org.snmp4j.event.ResponseEvent;
import org.snmp4j.smi.*;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.transport.DefaultUdpTransportMapping;

import java.awt.*;
import java.io.*;
import java.util.Optional;

public class MIBExtractorGUI extends Application {

    private TextArea logArea; // Para exibir logs de extração
    private File lastGeneratedFile; // Para armazenar o arquivo mais recente gerado

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("MIB Extractor");

        // Elementos da interface
        Label label = new Label("Digite o IP da Impressora:");
        TextField ipField = new TextField();
        ipField.setPromptText("Exemplo: 192.168.1.1");

        Button extractButton = new Button("Extrair MIB");
        Button openFileButton = new Button("Exibir Arquivo");

        // Configuração inicial: botão de exibir desativado
        openFileButton.setDisable(true);

        logArea = new TextArea();
        logArea.setEditable(false);

        // Ação para o botão de extração
        extractButton.setOnAction(event -> {
            String ip = ipField.getText();
            if (ip.isEmpty()) {
                showMessage("Erro: Por favor, insira um IP válido.", Alert.AlertType.ERROR);
            } else {
                extractMIB(ip);
                openFileButton.setDisable(lastGeneratedFile == null); // Ativa o botão se o arquivo foi gerado
            }
        });

        // Ação para o botão de abrir arquivo
        openFileButton.setOnAction(event -> openGeneratedFile(primaryStage));

        // Layout
        HBox buttonBox = new HBox(10, extractButton, openFileButton);
        VBox layout = new VBox(10);
        layout.setPadding(new Insets(15));
        layout.getChildren().addAll(label, ipField, buttonBox, logArea);

        Scene scene = new Scene(layout, 600, 400);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void extractMIB(String ip) {
        logArea.clear(); // Limpa o log para uma nova execução
        logArea.appendText("Iniciando extração de MIB para o IP: " + ip + "\n");

        try {
            // Cria a pasta de saída se não existir
            File directory = new File("output");
            if (!directory.exists()) {
                directory.mkdir();
            }

            // Caminho do arquivo de saída
            String filePath = "output/" + ip.replace(".", "_") + ".txt";
            lastGeneratedFile = new File(filePath);
            FileWriter fileWriter = new FileWriter(lastGeneratedFile);

            // Escreve o IP no arquivo de saída
            fileWriter.write("IP da impressora: " + ip + "\n");

            // Configuração SNMP
            TransportMapping<UdpAddress> transport = new DefaultUdpTransportMapping();
            Snmp snmp = new Snmp(transport);
            transport.listen();

            // Configuração do alvo
            Address targetAddress = GenericAddress.parse("udp:" + ip + "/161");
            CommunityTarget target = new CommunityTarget();
            target.setCommunity(new OctetString("public"));
            target.setAddress(targetAddress);
            target.setRetries(2);
            target.setTimeout(1500);
            target.setVersion(SnmpConstants.version2c);

            // Criação do PDU
            PDU pdu = new PDU();
            pdu.add(new VariableBinding(new OID("1.3.6.1.2.1"))); // OID de exemplo para extrair a MIB
            pdu.setType(PDU.GETNEXT);

            // Envio da requisição SNMP
            boolean finished = false;
            while (!finished) {
                ResponseEvent response = snmp.getNext(pdu, target);
                PDU responsePDU = response.getResponse();

                if (responsePDU == null) {
                    logArea.appendText("Fim da MIB ou nenhuma resposta.\n");
                    finished = true;
                } else {
                    for (VariableBinding vb : responsePDU.getVariableBindings()) {
                        String line = vb.getOid() + " = " + vb.getVariable();
                        logArea.appendText(line + "\n");
                        fileWriter.write(line + "\n");
                        pdu.clear();
                        pdu.add(new VariableBinding(vb.getOid()));
                    }
                    // Verifica se o próximo OID está fora do escopo da MIB
                    if (!responsePDU.get(0).getOid().startsWith(new OID("1.3.6.1.2.1"))) {
                        finished = true;
                    }
                }
            }

            // Encerramento do SNMP
            snmp.close();
            fileWriter.close();

            logArea.appendText("MIB extraída e salva no arquivo: " + filePath + "\n");
            showMessage("Extração concluída com sucesso!", Alert.AlertType.INFORMATION);

        } catch (IOException e) {
            logArea.appendText("Erro: " + e.getMessage() + "\n");
            showMessage("Erro ao extrair a MIB: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void openGeneratedFile(Stage stage) {
        if (lastGeneratedFile != null && lastGeneratedFile.exists()) {
            try{
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Abrir Arquivo Gerado");
            fileChooser.setInitialDirectory(lastGeneratedFile.getParentFile());
            fileChooser.setInitialFileName(lastGeneratedFile.getName());

            // Permitir ao usuário abrir ou exibir o arquivo manualmente
            Optional<File> selectedFile = Optional.ofNullable(fileChooser.showOpenDialog(stage));

            selectedFile.ifPresent(file -> logArea.appendText("Exibindo conteúdo de: " + file.getPath() + "\n"));
                Desktop.getDesktop().open(lastGeneratedFile);
                logArea.appendText("Arquivo aberto: " + lastGeneratedFile.getPath() + "\n");
            } catch (IOException e) {
                logArea.appendText("Erro ao abrir o arquivo: " + e.getMessage() + "\n");
                showMessage("Erro ao abrir o arquivo.", Alert.AlertType.ERROR);
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
