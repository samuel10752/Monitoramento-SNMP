package com.example.printercounters.oki;

import java.io.IOException;

import org.snmp4j.CommunityTarget;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.UdpAddress;
import org.snmp4j.smi.VariableBinding;
import org.snmp4j.transport.DefaultUdpTransportMapping;

import com.example.printercounters.controllers.PrinterModel;

import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

public class infooki {

    /**
     * Cria a instância do modelo HP com base na escolha do usuário.
     * A seleção (selectedModel) deve vir de uma interface, arquivo de configuração
     * ou outra lógica
     * externa à detecção via SNMP.
     *
     * @param ip            IP da impressora
     * @param selectedModel Modelo selecionado (por exemplo, "ES5112", "E52645Flow",
     *                      etc.)
     * @param macField      Campo de exibição do MAC
     * @param serialField   Campo de exibição do número de série
     * @param brandField    Campo de exibição da marca (será definido como "HP")
     * @param webInfoArea   Área de texto para exibição das informações web
     * @return Instância da classe que estende PrinterModel, de acordo com o modelo
     *         selecionado
     */
    public static PrinterModel createOKIPrinter(String ip, String selectedModel,
            TextField macField, TextField serialField,
            TextField brandField, TextArea webInfoArea) {
        // Define a marca como "HP"
        brandField.setText("OKI");

        switch (selectedModel) {
            case "ES5112":
                return new ES5112(ip, macField, serialField, brandField, webInfoArea);
            case "ES4172LP":
                return new ES4172LP(ip, macField, serialField, brandField, webInfoArea);
            // Adicione outros casos conforme novos modelos forem implementados
            default:
                // Caso o modelo selecionado não seja reconhecido, retorna um modelo padrão
                return new ES4172LP(ip, macField, serialField, brandField, webInfoArea);
        }
    }

    public static String detectPrinterModelOKI(String ip) {
        Snmp snmp = null;
        try {
            snmp = new Snmp(new DefaultUdpTransportMapping());
            snmp.listen();

            CommunityTarget<UdpAddress> target = new CommunityTarget<>();
            target.setCommunity(new OctetString("public"));
            target.setAddress(new UdpAddress(ip + "/161"));
            target.setRetries(2);
            target.setTimeout(3000);
            target.setVersion(org.snmp4j.mp.SnmpConstants.version1);

            // Verificar o OID genérico para modelos OKI
            String modelOID = getSnmpValue("1.3.6.1.2.1.25.3.2.1.3.1", snmp, target);
            if (modelOID != null) {
                if (modelOID.contains("ES5112")) {
                    return "ES5112";
                } else if (modelOID.contains("ES4172LP")) {
                    return "ES4172LP";
                }
            }

            // Verificar OID adicional para nome da impressora
            String nameOID = getSnmpValue("1.3.6.1.2.1.25.3.2.1.3.1", snmp, target);
            if (nameOID != null && nameOID.contains("OKI-ES5112")) {
                return "ES5112";
            }

        } catch (Exception e) {
            System.err.println("Erro ao detectar o modelo OKI: " + e.getMessage());
        } finally {
            try {
                if (snmp != null)
                    snmp.close();
            } catch (IOException e) {
                System.err.println("Erro ao fechar sessão SNMP: " + e.getMessage());
            }
        }
        return "Desconhecido";
    }

    // Método que cria a instância correta dos modelos HP com base no modelo
    // detectado
    public static PrinterModel createHPPrinter(String ip, TextField macField, TextField serialField,
            TextField brandField, TextArea webInfoArea) {
        String model = detectPrinterModelOKI(ip);
        switch (model) {
            case "ES5112":
                return new ES5112(ip, macField, serialField, brandField, webInfoArea);
            case "ES4172LP":
                return new ES4172LP(ip, macField, serialField, brandField, webInfoArea);
            default:
                return new ES4172LP(ip, macField, serialField, brandField, webInfoArea);
        }
    }

    // Método estático para realizar um SNMP GET e retornar o valor
    protected static String getSnmpValue(String oid, Snmp snmp, CommunityTarget<?> target) {
        try {
            PDU pdu = new PDU();
            pdu.add(new VariableBinding(new OID(oid)));
            pdu.setType(PDU.GET);

            ResponseEvent response = snmp.get(pdu, target);
            if (response != null && response.getResponse() != null) {
                VariableBinding vb = response.getResponse().get(0);
                if (vb != null && vb.getVariable() != null) {
                    return vb.getVariable().toString();
                }
            }
        } catch (Exception e) {
            System.err.println("Erro ao buscar OID " + oid + ": " + e.getMessage());
        }
        return null; // Retorna null se nada for encontrado
    }

    // Método estático para realizar um SNMP walk a partir de um OID base e retornar
    // o primeiro valor encontrado
    private static String snmpWalkForValue(String baseOid, Snmp snmp, CommunityTarget<?> target) {
        try {
            OID rootOid = new OID(baseOid);
            OID currentOid = new OID(rootOid);

            while (true) {
                PDU pdu = new PDU();
                pdu.add(new VariableBinding(currentOid));
                pdu.setType(PDU.GETNEXT);

                ResponseEvent response = snmp.getNext(pdu, target);
                PDU responsePDU = response.getResponse();
                if (responsePDU == null) {
                    break;
                }
                VariableBinding vb = responsePDU.get(0);
                OID nextOid = vb.getOid();
                if (!nextOid.startsWith(rootOid)) {
                    break;
                }
                return vb.getVariable().toString();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "Desconhecido";
    }
}
