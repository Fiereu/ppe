package de.fiereu.ppe;

import de.fiereu.ppe.forms.MainForm;
import com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatMaterialDarkerIJTheme;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.apache.commons.configuration2.builder.fluent.Parameters;
import org.apache.commons.configuration2.ex.ConfigurationException;

public class Application {

  private static final String SQLITE_DB = "packets.db";
  private static final String CONFIG_FILE = "config.conf";

  public static void main(String[] args) {
    try {
      FlatMaterialDarkerIJTheme.setup();
    } catch (Exception e) {
      ErrorHandler.handle(null, "Could not set Look&Feel.", e);
    }

    Configuration config = null;
    try {
      config = loadConfig();
    } catch (ConfigurationException e) {
      ErrorHandler.handle(null, "Could not load configuration.", e);
      return;
    } catch (IOException e) {
      ErrorHandler.handle(null, "Could not create configuration file.", e);
      return;
    }
    PacketHistory packetHistory;
    try {
      packetHistory = loadPacketHistory();
    } catch (IOException | SQLException e) {
      ErrorHandler.handle(null, "Could not load PacketHistory.", e);
      return;
    }
    new MainForm(config, packetHistory).setVisible(true);
  }

  private static Configuration loadConfig() throws ConfigurationException, IOException {
    File file = new File(CONFIG_FILE);
    if (!file.exists()) {
      if (!file.createNewFile()) {
        throw new IOException("Could not create configuration file.");
      }
    }
    Configurations configs = new Configurations();
    var builder = configs.propertiesBuilder().configure(
        new Parameters().fileBased()
            .setFile(new File(CONFIG_FILE))
            .setThrowExceptionOnMissing(false)
    );
    builder.setAutoSave(true);
    return builder.getConfiguration();
  }

  private static PacketHistory loadPacketHistory() throws IOException, SQLException {
    File sqliteDB = new File(SQLITE_DB);
    if (!sqliteDB.exists()) {
      if(!sqliteDB.createNewFile()) {
        throw new IOException("Could not create SQLite database.");
      }
    }
    return new PacketHistory(new File(SQLITE_DB));
  }
}
