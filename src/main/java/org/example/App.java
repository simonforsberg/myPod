package org.example;

import javafx.application.Application;
import org.example.logging.EMFHolder;

public class App {
    public static void main(String[] args) {
        EMFHolder.setEntityManagerFactory(PersistenceManager.getEntityManagerFactory());
        Application.launch(MyPod.class, args);
    }
}
