package bg.sofia.uni.fmi.mjt.cryptowallet;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.nio.channels.Channels;
import java.nio.channels.SocketChannel;
import java.util.Scanner;

public class CryptoClient {
    private static final int SERVER_PORT = 7777;
    private static final String HOST_NAME = "localhost";
    private static final String CS_NAME = "UTF-8";
    private static final String EXIT = "disconnect";
    private static final String LIST_OFFERINGS = "list-offerings";
    private static final String SUMMARY = "get-wallet-summary";
    private static final String SUMMARY_OVERALL = "get-wallet-overall-summary";
    private static final String INVALID_LOGGING = "You have not logged to your profile";
    private static final String SPACE = " ";
    private static final String HELP = "help";
    private static final SaveInformationToFiles saver = new SaveInformationToFiles();

    public static String formatStringOutput(String message, String serverAnswer) {

        if (message.equals(LIST_OFFERINGS) ||
                (message.equals(SUMMARY) && !serverAnswer.equals(INVALID_LOGGING)) ||
                (message.equals(SUMMARY_OVERALL) && !serverAnswer.equals(INVALID_LOGGING))) {

            return serverAnswer.replace(SPACE , System.lineSeparator());
        }

        return serverAnswer;
    }

    public static StringBuilder help() {
        StringBuilder builder = new StringBuilder();
        builder.append("Write: register and your username and password to register to the server");
        builder.append(System.lineSeparator());
        builder.append("Write: login and your username and password to log into your profile");
        builder.append(System.lineSeparator());
        builder.append("Write: deposit-money and the amount of money you want to deposit");
        builder.append(System.lineSeparator());
        builder.append("Write: list-offerings to see the available cryptocurrencies");
        builder.append(System.lineSeparator());
        builder.append("Write: buy , the ID of the certain crypto and the amount of money you want to invest");
        builder.append(System.lineSeparator());
        builder.append("Write: sell and the ID of the crypto you want to sell");
        builder.append(System.lineSeparator());
        builder.append("Write: get-wallet-summary to see your money and active investments");
        builder.append(System.lineSeparator());
        builder.append("Write: get-wallet-overall-summary to see the profit/loss of your investments");
        builder.append(System.lineSeparator());
        builder.append("Write: disconnect to save your current activity and disconnect from the server");

        return builder;
    }

    public static void main(String[] args) throws IOException {
        try (SocketChannel socketChannel = SocketChannel.open();
             BufferedReader reader = new BufferedReader(Channels.newReader(socketChannel, CS_NAME));
             PrintWriter writer = new PrintWriter(Channels.newWriter(socketChannel, CS_NAME), true);
             Scanner scanner = new Scanner(System.in)) {

            socketChannel.connect(new InetSocketAddress(HOST_NAME, SERVER_PORT));

            System.out.println("Connected to the server.");
            System.out.println("You can enter help to see the instructions");

            while (true) {
                System.out.print("Enter message: ");
                String message = scanner.nextLine();
                if (message.equals(HELP)) {
                    System.out.println(help());
                    continue;
                }

                writer.println(message);
                String reply = reader.readLine();

                if (reply.equals(EXIT)) {
                    break;
                }
                if (message.equals(LIST_OFFERINGS) || message.equals(SUMMARY) || message.equals(SUMMARY_OVERALL)) {
                    reply = formatStringOutput(message, reply);
                }

                System.out.println("The server replied: " + System.lineSeparator() + reply + System.lineSeparator());
            }
        } catch (IOException e) {
            saver.saveErrorInfoToFile(e);
            throw new RuntimeException("There is a problem with the network communication", e);
        }
    }
}
