package de.fiereu.ppe.proxy;


import de.fiereu.ppe.forms.ServerControlPane;
import de.fiereu.ppe.util.Callback;
import com.github.maltalex.ineter.base.IPAddress;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.UnresolvedAddressException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProxyServer implements Runnable {

  private static final Logger log = LoggerFactory.getLogger(ProxyServer.class);

  public final ServerControlPane serverControlPane;
  public final ServerType type;
  private final ServerSocketChannel serverSocketChannel;
  private final Selector serverSelector;
  private final InetSocketAddress socketAddress;
  private final Callback onStop;
  private final Map<ProxyClient, Thread> clients = new ConcurrentHashMap<>();
  private Thread serverThread;

  /**
   * Creates a new ServerProxy instance.
   *
   * @param serverControlPane The server control pane.
   * @param proxyIp           The IP address of the local proxy server.
   * @param proxyPort         The port of the local server.
   * @param targetIp          The IP address of the target server.
   * @param targetPort        The port of the target server.
   * @throws IOException If an I/O error occurs.
   */
  public ProxyServer(ServerControlPane serverControlPane, ServerType type, IPAddress proxyIp,
      int proxyPort, IPAddress targetIp, int targetPort, Callback onStop) throws IOException {
    this.serverControlPane = serverControlPane;
    this.type = type;
    this.socketAddress = new InetSocketAddress(targetIp.toInetAddress(), targetPort);
    this.onStop = onStop;
    this.serverSocketChannel = ServerSocketChannel.open();
    this.serverSelector = Selector.open();
    this.serverSocketChannel.bind(new InetSocketAddress(proxyIp.toInetAddress(), proxyPort));
    this.serverSocketChannel.configureBlocking(false);
    this.serverSocketChannel.register(serverSelector, SelectionKey.OP_ACCEPT);
  }

  private void log(String msg, Object... args) {
    serverControlPane.log(this.type, msg, args);
  }

  @Override
  public void run() {
    try {
      var from = this.serverSocketChannel.getLocalAddress();
      var to = this.socketAddress;
      log.info("Starting proxy server {} <-> {}", from, to);
      log("Starting proxy server %s <-> %s", from, to);
    } catch (IOException e) {
      log.error("An error occurred while starting the proxy server.", e);
      return;
    }
    while (!Thread.currentThread().isInterrupted()) {
      try {
        serverSelector.select();
        for (var key : serverSelector.selectedKeys()) {
          if (key.isAcceptable()) {
            var client = serverSocketChannel.accept();
            if (client != null) {
              var proxyClient = new ProxyClient(this, client,
                  SocketChannel.open(this.socketAddress));
              var clientThread = new Thread(proxyClient,
                  "ProxyClient-" + this.type + "-" + clients.size());
              clientThread.start();
              clients.put(proxyClient, clientThread);
              log.trace("Connected client from {}", client.getRemoteAddress());
              log("Connected client from %s", client.getRemoteAddress());
            }
          }
        }
      } catch (IOException e) {
        log.error("An error occurred while accepting a client.", e);
        log("An error occurred while accepting a client.");
      } catch (UnresolvedAddressException e) {
        log.error("Couldn't resolve target address {}", this.socketAddress);
        log("Couldn't resolve target address %s", this.socketAddress);
        break;
      }
      clients.forEach((client, thread) -> {
        if (!thread.isAlive()) {
          clients.remove(client);
        }
      });
    }
    log.info("Stopping proxy server.");
    log("Stopping proxy server.");
    clients.forEach((client, thread) -> {
      thread.interrupt();
      try {
        thread.join();
      } catch (InterruptedException e) {
        log.error("An error occurred while stopping the client.", e);
        log("An error occurred while stopping the client.");
      }
      clients.remove(client);
    });
    try {
      this.serverSocketChannel.close();
      this.serverSelector.close();
    } catch (IOException e) {
      log.error("An error occurred while closing the server socket.", e);
      log("An error occurred while closing the server socket.");
    }
    onStop.call();
  }

  public void start() {
    serverThread = new Thread(this, "ProxyServer-" + this.type);
    serverThread.start();
  }

  public void stop() throws InterruptedException {
    serverThread.interrupt();
    serverThread.join();
  }

  public Set<ProxyClient> getClients() {
    clients.forEach((client, thread) -> {
      if (!thread.isAlive()) {
        clients.remove(client);
      }
    });
    return clients.keySet();
  }
}
