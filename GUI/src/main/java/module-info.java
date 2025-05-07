module com.example {
    // JavaFX modules
    requires javafx.controls;
    requires javafx.fxml;
    requires java.net.http;
    requires org.json;

    // Spring Framework modules
    requires spring.context;  // This already includes transaction support
    requires spring.web;
    requires spring.core;
    requires spring.beans;
    requires spring.boot;

    // Spring Boot autoconfiguration and starters
    requires spring.boot.autoconfigure;
    requires spring.boot.starter.web;
    requires spring.boot.starter.websocket;
    requires spring.boot.starter.test;

    // Spring Messaging module
    requires spring.messaging;
    requires spring.websocket;

    // Allows Spring's reflection-based configuration
    opens com.example to javafx.fxml, spring.core;

    // Export your package
    exports com.example;
}
