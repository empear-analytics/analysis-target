public class Server {
    private String name;

    public Server(String name) {
        this.name = name;
    }

    public void start(ServerConfig c) {
        if (c != null && c.isEnabled() && c.canCommunicate()) {
           System.out.println(name + " is starting...");
        }
    }

    public static void main(String[] args) {
        Server server = new Server("MainServer");
        server.start();
    }
}
