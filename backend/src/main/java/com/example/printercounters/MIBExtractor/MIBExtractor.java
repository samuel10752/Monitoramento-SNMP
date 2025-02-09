package com.example.printercounters.MIBExtractor;

import org.snmp4j.*;
import org.snmp4j.smi.*;
import org.snmp4j.transport.DefaultUdpTransportMapping;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.event.ResponseEvent;
import java.io.IOException;
import java.io.FileWriter;
import java.io.File;
import java.util.Scanner;

public class MIBExtractor {

    public static void main(String[] args) {
        try {
            // Solicita o IP da impressora
            Scanner scanner = new Scanner(System.in);
            System.out.print("Digite o IP da impressora: ");
            String ip = scanner.nextLine();

            // Cria a pasta de saída se não existir
            File directory = new File("output");
            if (!directory.exists()) {
                directory.mkdir();
            }

            // Caminho do arquivo de saída
            String filePath = "output/" + ip.replace(".", "_") + ".txt";
            FileWriter fileWriter = new FileWriter(filePath);

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
                    System.out.println("Fim da MIB ou nenhuma resposta.");
                    finished = true;
                } else {
                    for (VariableBinding vb : responsePDU.getVariableBindings()) {
                        String line = vb.getOid() + " = " + vb.getVariable();
                        System.out.println(line);
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

            System.out.println("MIB extraída e salva no arquivo: " + filePath);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

