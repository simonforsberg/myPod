package org.example;

import javafx.application.Application;

public class App {
    static void main(String[] args) {
        loggerManager.setup();
        Application.launch(MyPod.class, args);
    }
}
