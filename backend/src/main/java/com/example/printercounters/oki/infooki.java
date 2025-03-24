package com.example.printercounters.oki;

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
        brandField.setText("HP");

        switch (selectedModel) {
            case "HP4303":
                return new ES5112(ip, macField, serialField, brandField, webInfoArea);
            case "E52645Flow":
            //     return new E52645Flow(ip, macField, serialField, brandField, webInfoArea);
            // // Adicione outros casos conforme novos modelos forem implementados
            default:
                // Caso o modelo selecionado não seja reconhecido, retorna um modelo padrão
                return new ES5112(ip, macField, serialField, brandField, webInfoArea);
        }
    }

    public static String detectPrinterModelOKI(String ip) {
        try {
            // Instancia SNMP para realizar a consulta
            Snmp snmp = new Snmp(new DefaultUdpTransportMapping());
            snmp.listen();
    
            CommunityTarget<UdpAddress> target = new CommunityTarget<>();
            target.setCommunity(new OctetString("public"));
            target.setAddress(new UdpAddress(ip + "/161"));
            target.setRetries(2);
            target.setTimeout(3000);
            target.setVersion(org.snmp4j.mp.SnmpConstants.version2c);
    
            // Consultando OID para E52645Flow
            String nameE52645Flow = getSnmpValue("1.3.6.1.2.1.43.5.1.1.16.1", snmp, target);
            if (nameE52645Flow != null && nameE52645Flow.toLowerCase().contains("e52645")) { // Ajuste para "e52645"
                snmp.close();
                return "E52645Flow";
            }
    
            // Consultando OID para 
            // String nameHP4303 = getSnmpValue("1.3.6.1.2.1.25.3.2.1.3.1", snmp, target);
            // if (nameHP4303 != null && nameHP4303.toLowerCase().contains("4303")) { // Ajuste para "hp4303"
            //     snmp.close();
            //     return "HP4303";
            // }
    
            snmp.close();
        } catch (Exception e) {
            System.err.println("Erro ao detectar o modelo HP: " + e.getMessage());
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
            // case "E52645Flow":
            //     return new E52645Flow(ip, macField, serialField, brandField, webInfoArea);
            default:
                return new ES5112(ip, macField, serialField, brandField, webInfoArea);
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
                System.out.println("Requisição para OID " + vb.getOid() + " retornou: " + vb.getVariable());
                return vb.getVariable().toString();
            } else {
                System.err.println("Nenhuma resposta para OID " + oid);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "Desconhecido";
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
