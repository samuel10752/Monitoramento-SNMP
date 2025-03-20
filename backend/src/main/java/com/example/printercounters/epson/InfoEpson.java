package com.example.printercounters.epson;

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

public class InfoEpson {

    public static PrinterModel createEpsonPrinter(String ip, String selectedModel,
                                                  TextField macField, TextField serialField,
                                                  TextField brandField, TextArea webInfoArea) {
        brandField.setText("Epson");
        
        switch (selectedModel) {
            case "EpsonL3250":
                return new EpsonL3250(ip, macField, serialField, brandField, webInfoArea);
            default:
                throw new IllegalArgumentException("Modelo Epson desconhecido: " + selectedModel);
        }
    }

    public static String detectPrinterModelEpson(String ip) {
        try {
            Snmp snmp = new Snmp(new DefaultUdpTransportMapping());
            snmp.listen();

            CommunityTarget<UdpAddress> target = new CommunityTarget<>();
            target.setCommunity(new OctetString("public"));
            target.setAddress(new UdpAddress(ip + "/161"));
            target.setRetries(2);
            target.setTimeout(3000);
            target.setVersion(org.snmp4j.mp.SnmpConstants.version2c);

            // Lógica fictícia para buscar modelo via OID. Ajuste conforme necessário.
            String model = getSnmpValue("1.3.6.1.4.1.367.3.2.1.1.5.0", snmp, target); // OID de exemplo
            snmp.close();

            if (model != null && !model.equals("Desconhecido") && !model.isEmpty()) {
                return "EpsonL3250"; // Exemplo de modelo detectado
            } else {
                return "Desconhecido";
            }
        } catch (Exception e) {
            System.err.println("Erro ao detectar o modelo Epson: " + e.getMessage());
        }
        return "Desconhecido";
    }

    protected static String getSnmpValue(String oid, Snmp snmp, CommunityTarget<?> target) {
        try {
            PDU pdu = new PDU();
            pdu.add(new VariableBinding(new OID(oid)));
            pdu.setType(PDU.GET);

            ResponseEvent response = snmp.get(pdu, target);
            if (response != null && response.getResponse() != null) {
                return response.getResponse().get(0).getVariable().toString();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "Desconhecido";
    }

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
