package com.example.tetris.helpers;

import com.example.tetris.controllers.ConfigurationController;
import com.example.tetris.services.JsonConfigurationService;
import javafx.fxml.FXMLLoader;

public class FxmlLoaderFactory {

  public static FXMLLoader configurationLoader() {
    FXMLLoader loader = new FXMLLoader(FxmlLoaderFactory.class.getResource("/com/example/tetris/configuration.fxml"));
    loader.setControllerFactory(type -> {
      if (type == ConfigurationController.class) {
        ConfigurationController controller = new ConfigurationController();
        controller.setConfigurationService(new JsonConfigurationService());
        return controller;
      }
      try {
        return type.getDeclaredConstructor().newInstance();
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    });
    return loader;
  }
}
