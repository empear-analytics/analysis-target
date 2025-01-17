public class Client {
    private String name;

    public Client(String name) {
        this.name = name;
    }

    public void start(ClientConfig c) {
        if (c != null && c.isEnabled() && c.canCommunicate()) {
           System.out.println(name + " is starting...");
        }
    }

    public static void main(String[] args) {
        Client c = new Client("MainServer");
        c.start();
    }
}
