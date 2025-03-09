package com.example.printercounters;

import com.example.printercounters.controllers.PrinterInfoDisplay;
import javafx.application.Application;

public class MainPrinterInfo {
    public static void main(String[] args) {
        // Chama o método launch da classe PrinterInfoDisplay para iniciar a interface gráfica
        Application.launch(PrinterInfoDisplay.class, args);
    }
}
