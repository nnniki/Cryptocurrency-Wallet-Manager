package bg.sofia.uni.fmi.mjt.cryptowallet;

import bg.sofia.uni.fmi.mjt.cryptowallet.coinapi.dto.CryptoInformation;
import bg.sofia.uni.fmi.mjt.cryptowallet.coinapi.dto.Cryptocurrencies;
import bg.sofia.uni.fmi.mjt.cryptowallet.user.User;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.nio.channels.Channels;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;

public class CryptoServerTest {
    private static CryptoServer server;
    private static Thread serverStarterThread;
    private static final String SPACE = " ";
    private static final String ID = "ID:";
    private static final String NAME = "Name:";
    private static final String BOUGHT = "boughtPrice:";
    private static final String MONEY = "Money: ";
    private static final String COUNT = "boughtCount:";
    private static final String ACTIVE_INVESTMENTS = "ActiveInvestments: ";

   @BeforeAll
    public static void setUp() throws InterruptedException {
       serverStarterThread = new Thread(() -> {
           try {
               CryptoServer cryptoServer = new CryptoServer();
               server = cryptoServer;
               server.start();
           } catch (IOException e) {
               System.out.println("An error has occured");
               e.printStackTrace();
           }
       });
       serverStarterThread.start();
       Thread.sleep(2000);
   }

   @AfterAll
    public static void stopServer() {
       server.stop();
       serverStarterThread.interrupt();
   }

   @BeforeEach
   public void set() {
       server.setRegisterUser(new User("Petar","123456"));
   }


    private String sendRequest(String msg) {
        String response = "fail";

        try (SocketChannel socketChannel = SocketChannel.open();
             BufferedReader reader = new BufferedReader(Channels.newReader(socketChannel, StandardCharsets.UTF_8));
             PrintWriter writer = new PrintWriter(Channels.newWriter(socketChannel, StandardCharsets.UTF_8), true) ){

            socketChannel.connect(new InetSocketAddress("localhost", 7777));

            System.out.println("Connected to the server.");

            String[] messages = msg.split("&&");
            for (String message : messages) {
                writer.println(message.strip());
                response = reader.readLine();
            }

        }
        catch (IOException e) {
            throw new RuntimeException("There is a problem with the network communication", e);
        }

        return response;
    }

   @Test
    public void testRegisterUserSuccessfully() {
       Set<User> registeredUsers = new HashSet<>();
       User user = new User("ivan", "123456");
       registeredUsers.add(new User("Petar","123456"));
       registeredUsers.add(user);

      assertEquals("User registered successfully", sendRequest("register ivan 123456"),
                  "Successfully registration was expected");
      assertIterableEquals(registeredUsers, server.getRegisteredUsers(),
              "Correct registered users were expected");
   }

   @Test
    public void testRegisterInvalidUsername() {
       Set<User> registeredUsers = new HashSet<>();
       User user = new User("niki", "123456");
       registeredUsers.add(user);
       server.setRegisterUser(user);

       assertEquals("Invalid username, choose another one", sendRequest("register niki 7777"),
               "Invalid registration expected when using already existed username");
       assertIterableEquals(registeredUsers, server.getRegisteredUsers(),
               "Correct registered users were expected");
   }

    @Test
    public void testLoginSuccessfully() {
        Set<User> registeredUsers = new HashSet<>();
        User user = new User("niki", "123456");
        registeredUsers.add(user);
        server.setRegisterUser(user);

        assertEquals("User logged successfully", sendRequest("login niki 123456"),
                "Successful login was expected");
    }

    @Test
    public void testLoginWrongPassword() {
        Set<User> registeredUsers = new HashSet<>();
        User user = new User("niki", "123456");
        registeredUsers.add(user);
        server.setRegisterUser(user);

        assertEquals("Invalid logging", sendRequest("login niki 1234"),
                "Invalid logging was expected when using wrong password");
    }

    @Test
    public void testListOfferingsSuccessfully() {
        CryptoInformation crypto = new CryptoInformation("BTC", "Bitcoin",1,20025.123);
        Set<CryptoInformation> s = new HashSet<>();
        s.add(crypto);
        Cryptocurrencies cryptocurrencies = Cryptocurrencies.of(s, "2023-02-15 11:00:00");
        server.setCryptocurrencies(cryptocurrencies);
        StringBuilder builder = new StringBuilder();
        builder.append("ID:BTC ");
        builder.append("Name:Bitcoin ");
        builder.append("Price:20025.123  ");

        assertEquals(builder.toString(), sendRequest("list-offerings"),
                "Correct offerings were expected");
    }

    @Test
    public void testDepositMoneySuccessfully() {
       assertEquals("Money are deposit successfully", sendRequest("login Petar 123456 && deposit-money 500"),
               "Successfully deposit was expected");
       Set<User> users = server.getRegisteredUsers();

       for (var currUser : users) {
           assertEquals(500,currUser.getMoney(), "User's money are expected to be 500 after deposit");
       }
    }

    @Test
    public void testDepositMoneyNegativeAmount() {
        assertEquals("You can't deposit zero or negative amount of money ",
                sendRequest("login Petar 123456 && deposit-money -500"),
                "Invalid deposit was expected when depositing negative amount of money");
    }

    @Test
    public void testDepositMoneyUserNotLogged() {
        assertEquals("You have not logged to your profile",
                sendRequest("deposit-money -500"),
                "Invalid deposit was expected when user is not logged");
    }

    @Test
    public void testDepositMoneyInvalidInput() {
        assertEquals("User's input is invalid, check the help menu",
                sendRequest("deposit-money "), "Invalid depositing was expected when input is invalid");

    }

    @Test
    public void testBuyCryptoSuccessfully() {
        CryptoInformation crypto = new CryptoInformation("BTC", "Bitcoin",1,20000.00);
        Set<CryptoInformation> s = new HashSet<>();
        s.add(crypto);
        Cryptocurrencies cryptocurrencies = Cryptocurrencies.of(s, "2023-02-15 11:00:00");
        server.setCryptocurrencies(cryptocurrencies);

        assertEquals("You successfully bought BTC", sendRequest("login Petar 123456 && deposit-money 1000" +
                " && buy BTC 500"), "Successfully buying was expected ");

    }

    @Test
    public void testBuyCryptoNotEnoughMoney() {
        CryptoInformation crypto = new CryptoInformation("BTC", "Bitcoin",1,20000.00);
        Set<CryptoInformation> s = new HashSet<>();
        s.add(crypto);
        Cryptocurrencies cryptocurrencies = Cryptocurrencies.of(s, "2023-02-15 11:00:00");
        server.setCryptocurrencies(cryptocurrencies);

        assertEquals("You don't have enough money ", sendRequest("login Petar 123456 && deposit-money 1000" +
                " && buy BTC 1500"), "Invalid buying was expected when investing more money than available");

    }

    @Test
    public void testBuyCryptoUnavailableCryptocurrency() {
        CryptoInformation crypto = new CryptoInformation("BTC", "Bitcoin",1,20000.00);
        Set<CryptoInformation> s = new HashSet<>();
        s.add(crypto);
        Cryptocurrencies cryptocurrencies = Cryptocurrencies.of(s, "2023-02-15 11:00:00");
        server.setCryptocurrencies(cryptocurrencies);

        assertEquals("This cryptocurrency is unavailable at the moment",
                sendRequest("login Petar 123456 && deposit-money 1000" +
                " && buy ETC 500"), "Invalid buying was expected when buying unavailable crypto");
    }

    @Test
    public void testBuyCryptoNotLogged() {
        CryptoInformation crypto = new CryptoInformation("BTC", "Bitcoin",1,20000.00);
        Set<CryptoInformation> s = new HashSet<>();
        s.add(crypto);
        Cryptocurrencies cryptocurrencies = Cryptocurrencies.of(s, "2023-02-15 11:00:00");
        server.setCryptocurrencies(cryptocurrencies);

        assertEquals("You have not logged to your profile",
                sendRequest("buy BTC 500"), "Invalid buying was expected when buying before logging in account");
    }

    @Test
    public void testBuyCryptoInvalidInput() {
        CryptoInformation crypto = new CryptoInformation("BTC", "Bitcoin",1,20000.00);
        Set<CryptoInformation> s = new HashSet<>();
        s.add(crypto);
        Cryptocurrencies cryptocurrencies = Cryptocurrencies.of(s, "2023-02-15 11:00:00");
        server.setCryptocurrencies(cryptocurrencies);

        assertEquals("User's input is invalid, check the help menu",
                sendRequest("buy ETC "), "Invalid buying was expected when input is invalid");

    }

    @Test
    public void testSellCryptoSuccessfully() {
        CryptoInformation crypto = new CryptoInformation("BTC", "Bitcoin",1,20000.00);
        Set<CryptoInformation> s = new HashSet<>();
        s.add(crypto);
        Cryptocurrencies cryptocurrencies = Cryptocurrencies.of(s, "2023-02-15 11:00:00");
        server.setCryptocurrencies(cryptocurrencies);

        assertEquals("You successfully sold BTC", sendRequest("login Petar 123456 && deposit-money 1000" +
                " && buy BTC 500 && sell BTC"), "Successfully selling was expected ");
    }

    @Test
    public void testSellCryptoUnavailableCrypto() {
        CryptoInformation crypto = new CryptoInformation("BTC", "Bitcoin",1,20000.00);
        Set<CryptoInformation> s = new HashSet<>();
        s.add(crypto);
        Cryptocurrencies cryptocurrencies = Cryptocurrencies.of(s, "2023-02-15 11:00:00");
        server.setCryptocurrencies(cryptocurrencies);

        assertEquals("This cryptocurrency is unavailable at the moment",
                sendRequest("login Petar 123456 && deposit-money 1000" +
                " && buy BTC 500 && sell ETC"), "Invalid selling was expected when missing cryptocurrency");
    }

    @Test
    public void testSellCryptoBeforeBuying() {
        CryptoInformation crypto = new CryptoInformation("BTC", "Bitcoin",1,20000.00);
        CryptoInformation ETH = new CryptoInformation("ETC", "Ethereum",1,50.00);
        Set<CryptoInformation> s = new HashSet<>();
        s.add(crypto);
        s.add(ETH);
        Cryptocurrencies cryptocurrencies = Cryptocurrencies.of(s, "2023-02-15 11:00:00");
        server.setCryptocurrencies(cryptocurrencies);

        assertEquals("You can't sell cryptocurrency that you haven't bought ",
                sendRequest("login Petar 123456 && deposit-money 1000" +
                        " && buy BTC 500 && sell ETC"), "Invalid selling was expected when selling before buying");
    }

    @Test
    public void testGetWalletSummarySuccessfully() {
        CryptoInformation crypto = new CryptoInformation("BTC", "Bitcoin",1,20000.00);
        Set<CryptoInformation> s = new HashSet<>();
        s.add(crypto);
        Cryptocurrencies cryptocurrencies = Cryptocurrencies.of(s, "2023-02-15 11:00:00");
        server.setCryptocurrencies(cryptocurrencies);

        StringBuilder builder = new StringBuilder();
        builder.append(MONEY);
        builder.append(0.00);
        builder.append(SPACE);
        builder.append(ACTIVE_INVESTMENTS);
        builder.append(SPACE);
        builder.append(ID);
        builder.append("BTC");
        builder.append(SPACE);
        builder.append(NAME);
        builder.append("Bitcoin");
        builder.append(SPACE);
        builder.append(BOUGHT);
        builder.append(1000.0);
        builder.append(SPACE);
        builder.append(COUNT);
        builder.append(0.05);
        builder.append(SPACE);
        builder.append(SPACE);

        assertEquals(builder.toString(),
                sendRequest("login Petar 123456 && deposit-money 1000" +
                        " && buy BTC 1000 && get-wallet-summary"), "Valid summary was expected");
    }

    @Test
    public void testGetWalletSummaryBeforeLogging() {
        CryptoInformation crypto = new CryptoInformation("BTC", "Bitcoin",1,20000.00);
        Set<CryptoInformation> s = new HashSet<>();
        s.add(crypto);
        Cryptocurrencies cryptocurrencies = Cryptocurrencies.of(s, "2023-02-15 11:00:00");
        server.setCryptocurrencies(cryptocurrencies);

        assertEquals("You have not logged to your profile", sendRequest("get-wallet-summary"),
                "Invalid summary was expected before logging in account");
    }

    @Test
    public void testGetWalletOverallSummaryBeforeLogging() {
        CryptoInformation crypto = new CryptoInformation("BTC", "Bitcoin",1,20000.00);
        Set<CryptoInformation> s = new HashSet<>();
        s.add(crypto);
        Cryptocurrencies cryptocurrencies = Cryptocurrencies.of(s, "2023-02-15 11:00:00");
        server.setCryptocurrencies(cryptocurrencies);

        assertEquals("You have not logged to your profile", sendRequest("get-wallet-overall-summary"),
                "Invalid overall summary was expected before logging in account");
    }

    @Test
    public void testGetWalletOverallSummarySuccessfully() {
        CryptoInformation crypto = new CryptoInformation("BTC", "Bitcoin",1,20000.00);
        Set<CryptoInformation> s = new HashSet<>();
        s.add(crypto);
        Cryptocurrencies cryptocurrencies = Cryptocurrencies.of(s, "2023-02-15 11:00:00");
        server.setCryptocurrencies(cryptocurrencies);

        StringBuilder builder = new StringBuilder();
        builder.append("ActiveInvestments:  ");
        builder.append("ID:");
        builder.append("BTC ");
        builder.append("Name:");
        builder.append("Bitcoin ");
        builder.append("boughtPrice:");
        builder.append("1000.0 ");
        builder.append("currentPrice:");
        builder.append("1000.0 ");
        builder.append("currentProfit:");
        builder.append("0.0  ");
        builder.append("FinishedInvestments: ");
        builder.append("overallProfit:");
        builder.append("0.0  ");

        assertEquals(builder.toString(), sendRequest("login Petar 123456 && deposit-money 1000" +
                        " && buy BTC 1000 && get-wallet-overall-summary"),
                "Successful wallet-overall was expected");
    }

    @Test
    public void testUnknownCommand() {
       assertEquals("Unknown command", sendRequest("check"),"Invalid command was expected");
    }

    @Test
    public void testDisconnect() {
       assertEquals("disconnect", sendRequest("login Petar 123456 && deposit-money 1000" +
               " && buy BTC 1000 && disconnect"), "Successful disconnect was expected");
    }
}


