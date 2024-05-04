package de.fiereu.ppe;

import de.fiereu.ppe.proxy.Direction;
import de.fiereu.ppe.proxy.ServerType;
import de.fiereu.ppe.proxy.packets.Packet;
import java.io.Closeable;
import java.io.File;
import java.net.SocketAddress;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PacketHistory implements Closeable {
  private static final Logger log = LoggerFactory.getLogger(PacketHistory.class);
  private final Connection connection;
  public PacketHistory(File sqliteDB) throws SQLException {
    this.connection = DriverManager.getConnection("jdbc:sqlite:" + sqliteDB.getAbsolutePath());
    setup();
  }

  private void setup() throws SQLException {
    String CREATE_TABLE = """
        CREATE TABLE IF NOT EXISTS %s (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            timestamp INTEGER NOT NULL,
            source TEXT NOT NULL,
            destination TEXT NOT NULL,
            direction TEXT NOT NULL,
            packet_id INTEGER NOT NULL,
            data BLOB NOT NULL
        )
        """.trim();
    this.connection.createStatement().execute(CREATE_TABLE.formatted(ServerType.LOGIN.toString()));
    this.connection.createStatement().execute(CREATE_TABLE.formatted(ServerType.GAME.toString()));
    this.connection.createStatement().execute(CREATE_TABLE.formatted(ServerType.CHAT.toString()));
  }

  public void addPacket(
      ServerType serverType,
      SocketAddress source,
      SocketAddress destination,
      Packet packet
  ) throws SQLException {
    var statement = this.connection.prepareStatement("""
        INSERT INTO %s (timestamp, source, destination, direction, packet_id, data)
        VALUES (?, ?, ?, ?, ?, ?)
        """.formatted(serverType.toString()));
    statement.setLong(1, System.currentTimeMillis());
    statement.setString(2, source.toString());
    statement.setString(3, destination.toString());
    statement.setString(4, packet.direction.toString());
    statement.setInt(5, Byte.toUnsignedInt(packet.id));
    statement.setBytes(6, packet.data);
    statement.execute();
  }

  public void deletePacket(
      ServerType serverType,
      long uid
  ) throws SQLException {
    var statement = this.connection.prepareStatement("""
        DELETE FROM %s WHERE id=?
        """.formatted(serverType.toString()));
    statement.setLong(1, uid);
    statement.execute();
  }

  public void clear(ServerType serverType) throws SQLException {
    var statement = this.connection.prepareStatement("""
        DELETE FROM %s
        """.formatted(serverType.toString()));
    statement.execute();
  }

  public void close() {
    try {
      this.connection.close();
    } catch (SQLException e) {
      log.error("Could not properly close packet database.", e);
      throw new RuntimeException(e);
    }
  }

  public ArrayList<PacketEntry> getPackets(ServerType serverType) throws SQLException {
    String query = "SELECT * FROM "
        + serverType.toString();
    var statement = this.connection.prepareStatement(query);
    var resultSet = statement.executeQuery();
    ArrayList<PacketEntry> packets = new ArrayList<>();
    while (resultSet.next()) {
      packets.add(new PacketEntry(
          resultSet.getLong("id"),
          resultSet.getLong("timestamp"),
          resultSet.getString("source"),
          resultSet.getString("destination"),
          Direction.valueOf(resultSet.getString("direction")),
          resultSet.getInt("packet_id"),
          resultSet.getBytes("data")
      ));
    }
    return packets;
  }

  public record PacketEntry(long uid, long timestamp, String source, String destination,
                            Direction direction, int id, byte[] data) {}
}
