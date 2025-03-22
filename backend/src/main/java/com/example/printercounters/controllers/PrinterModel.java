package com.example.printercounters.controllers;

import java.security.SecureRandom;
import java.security.cert.X509Certificate;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.snmp4j.CommunityTarget;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.VariableBinding;

import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

public abstract class PrinterModel {

    protected String ip;
    protected TextField macField;
    protected TextField serialField;
    protected TextField nameprinterField;
    protected TextArea webInfoArea;

    public PrinterModel(String ip, TextField macField, TextField serialField, TextField nameprinterField,
            TextArea webInfoArea) {
        this.ip = ip;
        this.macField = macField;
        this.serialField = serialField;
        this.nameprinterField = nameprinterField;
        this.webInfoArea = webInfoArea;
    }

    public abstract String getMacAddress();

    public abstract String getSerialNumber();

    public abstract String getWebCounters();

    public abstract void fetchPrinterInfo();

    public abstract void fetchWebPageData();

    public static void disableSSLCertificateChecking() {
        try {
            TrustManager[] trustAllCertificates = new TrustManager[] {
                    new X509TrustManager() {
                        public X509Certificate[] getAcceptedIssuers() {
                            return null;
                        }

                        public void checkClientTrusted(X509Certificate[] certs, String authType) {
                        }

                        public void checkServerTrusted(X509Certificate[] certs, String authType) {
                        }
                    }
            };

            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, trustAllCertificates, new SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
            HttpsURLConnection.setDefaultHostnameVerifier((hostname, session) -> true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected String getSnmpValue(String oid, Snmp snmp, CommunityTarget<?> target) {
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
}
