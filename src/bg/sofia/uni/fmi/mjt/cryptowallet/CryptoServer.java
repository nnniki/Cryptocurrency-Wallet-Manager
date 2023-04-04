package bg.sofia.uni.fmi.mjt.cryptowallet;

import bg.sofia.uni.fmi.mjt.cryptowallet.coinapi.dto.CryptoInformation;
import bg.sofia.uni.fmi.mjt.cryptowallet.coinapi.dto.Cryptocurrencies;
import bg.sofia.uni.fmi.mjt.cryptowallet.exception.InsufficientAvailabilityException;
import bg.sofia.uni.fmi.mjt.cryptowallet.exception.InvalidSellingException;
import bg.sofia.uni.fmi.mjt.cryptowallet.user.User;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Type;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class CryptoServer {
    public static final int SERVER_PORT = 7777;
    private static final String SERVER_HOST = "localhost";
    private static final int BUFFER_SIZE = 10000;
    private static final String CRYPTO_INFO_FILE = "CryptoInformation.txt";
    private static final String USERS_INFO_FILE = "UsersInformation.txt";
    private static final String TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
    private static final String SPACE = " ";
    private static final String ID = "ID:";
    private static final String PRICE = "Price:";
    private static final String NAME = "Name:";
    private static final String REGISTER = "register";
    private static final String INVALID_USERNAME = "Invalid username, choose another one";
    private static final String SUCCESSFUL_REGISTRATION = "User registered successfully";
    private static final String INVALID_LOGGING = "Invalid logging";
    private static final String NOT_LOGGED = "You have not logged to your profile";
    private static final String INVALID_INPUT = "User's input is invalid, check the help menu";
    private static final String UNAVAILABLE_CRYPTO = "This cryptocurrency is unavailable at the moment";
    private static final String LOGIN = "login";
    private static final String SUCCESSFUL_LOGIN = "User logged successfully";
    private static final String LIST_OFFERINGS = "list-offerings";
    private static final String DEPOSIT = "deposit-money";
    private static final String SUCCESSFUL_DEPOSIT = "Money are deposit successfully";
    private static final String BUY = "buy";
    private static final String SUCCESSFUL_BUY = "You successfully bought ";
    private static final String SELL = "sell";
    private static final String SUCCESSFUL_SELL = "You successfully sold ";
    private static final String SUMMARY = "get-wallet-summary";
    private static final String SUMMARY_OVERALL = "get-wallet-overall-summary";
    private static final String DISCONNECT = "disconnect";
    private static final String UNKNOWN_COMMAND = "Unknown command";
    private static final int VALID_MINUTES = 30;
    private static final int INPUT_LENGTH = 3;
    private static final Gson GSON = new Gson();
    private final int port;
    private final ByteBuffer messageBuffer;
    private Selector selector;
    private boolean isStarted = true;
    private Cryptocurrencies cryptocurrencies;
    private Set<User> registeredUsers;
    private Map<SocketChannel, User> userChannels;
    private SaveInformationToFiles saver;

    public CryptoServer(int port) throws IOException {
        this.port = port;
        this.messageBuffer = ByteBuffer.allocate(BUFFER_SIZE);
        userChannels = new HashMap<>();
        saver = new SaveInformationToFiles();
        initializeUsers();
        readCryptocurrenciesInfoFromFile();
    }

    public CryptoServer() {
        this.port = CryptoServer.SERVER_PORT;
        this.messageBuffer = ByteBuffer.allocate(BUFFER_SIZE);
        userChannels = new HashMap<>();
        registeredUsers = new HashSet<>();
    }

    private void initializeUsers() throws IOException {
        File usersFile = new File(USERS_INFO_FILE);
        try (Reader reader = new FileReader(usersFile)) {
            if (usersFile.length() == 0) {
                registeredUsers = new HashSet<>();
            } else {
                Type cryptoListType = new TypeToken<Set<User>>() {
                }.getType();
                Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
                registeredUsers = gson.fromJson(reader, cryptoListType);
            }
        } catch (IOException e) {
            registeredUsers = new HashSet<>();
            saver.saveErrorInfoToFile(e);
        }
    }

    private void readCryptocurrenciesInfoFromFile() throws IOException {
        File cryptoFile = new File(CRYPTO_INFO_FILE);
        try (Reader reader = new FileReader(cryptoFile)) {
            if (cryptoFile.length() == 0) {
                cryptocurrencies = saver.getCryptocurrenciesFromApi();
            } else {
                cryptocurrencies = GSON.fromJson(reader, Cryptocurrencies.class);
            }
        } catch (IOException e) {
            cryptocurrencies = saver.getCryptocurrenciesFromApi();
            saver.saveErrorInfoToFile(e);
        }
    }

    private void updateCryptocurrenciesIfNeeded() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(TIME_FORMAT);
        LocalDateTime dateTime = LocalDateTime.parse(cryptocurrencies.lastUpdateOfInformation(), formatter);

        LocalDateTime validUntilTime = dateTime.plusMinutes(VALID_MINUTES);

        if (validUntilTime.isBefore(LocalDateTime.now())) {
            if (saver != null) {
                cryptocurrencies = saver.getCryptocurrenciesFromApi();
            }
        }
    }

    public void start() throws IOException {
        try (ServerSocketChannel serverSocketChannel = ServerSocketChannel.open()) {
            serverSocketChannel.bind(new InetSocketAddress(SERVER_HOST, port));
            serverSocketChannel.configureBlocking(false);

            selector = Selector.open();
            serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
            while (isStarted) {
                    int readyChannels = selector.select();
                    if (readyChannels == 0) {
                        continue;
                    }

                    Set<SelectionKey> selectedKeys = selector.selectedKeys();
                    Iterator<SelectionKey> keyIterator = selectedKeys.iterator();

                    while (keyIterator.hasNext()) {
                        SelectionKey key = keyIterator.next();

                            if (key.isReadable()) {
                                SocketChannel socketChannel = (SocketChannel) key.channel();
                                messageBuffer.clear();
                                int r = socketChannel.read(messageBuffer);
                                if (r <= 0) {
                                    System.out.println("Client has closed the connection");
                                    socketChannel.close();
                                    continue;
                                }

                                handleKeyIsReadable(key, messageBuffer);

                            } else if (key.isAcceptable()) {
                                handleKeyIsAcceptable(key);
                            }
                            keyIterator.remove();
                    }
            }
        } catch (IOException | InterruptedException e) {
            System.err.println("There is a problem with the server socket: " + e.getMessage());
            if (saver != null) {
                saver.saveErrorInfoToFile(e);
            }
        }
        System.out.println("Server stopped");
    }

    private String registerUser(String username, String password) throws IOException {
        for (var curr : registeredUsers) {
            if (curr.getUsername().equals(username)) {
                return INVALID_USERNAME;
            }
        }

        User newUser = new User(username, password);
        registeredUsers.add(newUser);
        if (saver != null) {
            saver.saveUsersInfoToFile(registeredUsers);
        }
        return SUCCESSFUL_REGISTRATION;
    }

    private String loginUser(String username, String password, SocketChannel channel) {
        for (var currUser : registeredUsers) {
            String currUserPassword = new String(currUser.getPasswordBytes(), StandardCharsets.UTF_8);
            if (username.equals(currUser.getUsername()) && password.equals(currUserPassword)) {
                userChannels.put(channel, currUser);
                return SUCCESSFUL_LOGIN;
            }
        }
        return INVALID_LOGGING;
    }

    private StringBuilder listOfferings() {
        updateCryptocurrenciesIfNeeded();

        StringBuilder builder = new StringBuilder();
        for (var currCrypto : cryptocurrencies.cryptocurrencies()) {
            builder.append(ID);
            builder.append(currCrypto.assetID());
            builder.append(SPACE);
            builder.append(NAME);
            builder.append(currCrypto.assetName());
            builder.append(SPACE);
            builder.append(PRICE);
            builder.append(currCrypto.price());
            builder.append(SPACE);
            builder.append(SPACE);
        }

        return builder;
    }

    private String depositMoney(SocketChannel channel, double amount) throws IOException {
        String response;
        if (!userChannels.containsKey(channel)) {
            response = NOT_LOGGED;
        } else {
            try {
                userChannels.get(channel).depositMoney(amount);
                response = SUCCESSFUL_DEPOSIT;
            } catch (IllegalArgumentException e) {
                response = e.getMessage();
                if (saver != null) {
                    saver.saveUserErrorsToFile(e, channel, userChannels);
                }
            }
        }

        return response;
    }

    private void updateUserInfo(User user) {
        Iterator<User> it = registeredUsers.iterator();
        while (it.hasNext()) {
            var currUser = it.next();
            if (currUser.getUsername().equals(user.getUsername())) {
                it.remove();
                break;
            }
        }
        registeredUsers.add(user);
    }

    private String disconnect(SelectionKey key) throws IOException {
        SocketChannel socketChannel = (SocketChannel) key.channel();
        User user = userChannels.get(socketChannel);
        updateUserInfo(user);
        userChannels.remove(socketChannel);
        if (saver != null) {
            saver.saveUsersInfoToFile(registeredUsers);
        }
        return DISCONNECT;
    }

    private String buyCrypto(String cryptoID, double amount, SocketChannel channel) throws IOException {
        updateCryptocurrenciesIfNeeded();
        String response;
        if (!userChannels.containsKey(channel)) {
            response = NOT_LOGGED;
        } else {
            User user = userChannels.get(channel);
            CryptoInformation cryptoInfo = null;
            for (var currCrypto : cryptocurrencies.cryptocurrencies()) {
                if (currCrypto.assetID().equals(cryptoID)) {
                    cryptoInfo = currCrypto;
                    break;
                }
            }
            try {
                if (cryptoInfo == null) {
                    response = UNAVAILABLE_CRYPTO;
                } else {
                    user.buyCrypto(cryptoInfo, amount);
                    response = SUCCESSFUL_BUY + cryptoID;
                    userChannels.remove(channel);
                    userChannels.put(channel, user);
                }
            } catch (InsufficientAvailabilityException | IllegalArgumentException e) {
                response = e.getMessage();
                if (saver != null) {
                    saver.saveUserErrorsToFile(e, channel, userChannels);
                }
            }
        }
        return response;
    }

    private String sellCrypto(String cryptoID, SocketChannel channel) throws IOException {
        updateCryptocurrenciesIfNeeded();
        String response;

        if (!userChannels.containsKey(channel)) {
            response = NOT_LOGGED;
        } else {
            User user = userChannels.get(channel);
            CryptoInformation cryptoInfo = null;
            for (var currCrypto : cryptocurrencies.cryptocurrencies()) {
                if (currCrypto.assetID().equals(cryptoID)) {
                    cryptoInfo = currCrypto;
                    break;
                }
            }
            try {
                if (cryptoInfo == null) {
                    response = UNAVAILABLE_CRYPTO;
                } else {
                    user.sellCrypto(cryptoInfo);
                    response = SUCCESSFUL_SELL + cryptoID;
                    userChannels.remove(channel);
                    userChannels.put(channel, user);
                }
            } catch (InvalidSellingException e) {
                response = e.getMessage();
                if (saver != null) {
                    saver.saveUserErrorsToFile(e, channel, userChannels);
                }
            }
        }
        return response;
    }

    private StringBuilder getWalletSummary(SocketChannel channel) {
        StringBuilder builder = new StringBuilder();

        if (!userChannels.containsKey(channel)) {
            builder.append(NOT_LOGGED);
        } else {
            User user = userChannels.get(channel);
            builder = user.getWalletSummary();
        }
        return builder;
    }

    private StringBuilder getWalletOverallSummary(SocketChannel channel) {
        updateCryptocurrenciesIfNeeded();
        StringBuilder builder = new StringBuilder();

        if (!userChannels.containsKey(channel)) {
            builder.append(NOT_LOGGED);
        } else {
            User user = userChannels.get(channel);
            builder = user.getWalletOverall(cryptocurrencies);
        }
        return builder;
    }

    private boolean checkNullEmptyBlank(String first, String second) {
        if (first == null || first.isEmpty() || first.isBlank()) {
            return false;
        }
        return second != null && !second.isEmpty() && !second.isBlank();
    }

    private String executeOperations(String[] words, SocketChannel socketChannel, SelectionKey key) throws IOException {
        String response;
        switch (words[0]) {
            case REGISTER -> {
                if (words.length == INPUT_LENGTH && checkNullEmptyBlank(words[1], words[2])) {
                    String username = words[1];
                    String password = words[2];
                    response = registerUser(username, password);
                } else {
                    response = INVALID_INPUT;
                }
            }
            case LOGIN -> {
                if (words.length == INPUT_LENGTH && checkNullEmptyBlank(words[1], words[2])) {
                    String username = words[1];
                    String password = words[2];

                    response = loginUser(username, password, socketChannel);
                } else {
                    response = INVALID_INPUT;
                }
            }
            case LIST_OFFERINGS -> response = new String(listOfferings());
            case DEPOSIT -> {
                if (words.length == 2 && words[1] != null && !words[1].isEmpty() && !words[1].isBlank()) {
                    double amount = Double.parseDouble(words[1]);
                    response = depositMoney(socketChannel, amount);
                } else {
                    response = INVALID_INPUT;
                }
            }
            case BUY -> {
                if (words.length == INPUT_LENGTH && checkNullEmptyBlank(words[1], words[2])) {
                    String cryptoID = words[1];
                    double amount = Double.parseDouble(words[2]);
                    response = buyCrypto(cryptoID, amount, socketChannel);
                } else {
                    response = INVALID_INPUT;
                }
            }
            case SUMMARY -> response = new String(getWalletSummary(socketChannel));
            case SELL -> {
                if (words.length == 2 && words[1] != null && !words[1].isBlank() && !words[1].isEmpty()) {
                    String cryptoID = words[1];
                    response = sellCrypto(cryptoID, socketChannel);
                } else {
                    response = INVALID_INPUT;
                }
            }
            case SUMMARY_OVERALL -> response = new String(getWalletOverallSummary(socketChannel));
            case DISCONNECT -> response = disconnect(key);
            default -> response = UNKNOWN_COMMAND;
        }
        return response;
    }
    private void handleKeyIsReadable(SelectionKey key, ByteBuffer buffer) throws IOException, InterruptedException {
        SocketChannel socketChannel = (SocketChannel) key.channel();
        buffer.flip();

        byte[] clientInputBytes = new byte[buffer.remaining()];
        buffer.get(clientInputBytes);
        String clientInput = new String(clientInputBytes, StandardCharsets.UTF_8);

        String[] words = clientInput.strip().split(SPACE);
        String response = executeOperations(words, socketChannel, key);

        if (response != null) {
            System.out.println("Sending response to client: ");
            response += System.lineSeparator();
            buffer.clear();
            buffer.put(response.getBytes());
            buffer.flip();
            socketChannel.write(buffer);
        }
    }

    private void handleKeyIsAcceptable(SelectionKey key) throws IOException {
        ServerSocketChannel sockChannel = (ServerSocketChannel) key.channel();
        SocketChannel accept = sockChannel.accept();
        accept.configureBlocking(false);
        accept.register(selector, SelectionKey.OP_READ);

        System.out.println("Connection accepted from client " + accept.getRemoteAddress());
    }

    public void setRegisterUser(User user) {
        registeredUsers.clear();
        registeredUsers.add(user);
    }

    public void setCryptocurrencies(Cryptocurrencies crypto) {
        this.cryptocurrencies = crypto;
    }

    public Set<User> getRegisteredUsers() {
        return registeredUsers;
    }

    public void stop() {
        isStarted = false;
    }

    public static void main(String[] args) throws IOException {
        CryptoServer server = new CryptoServer(SERVER_PORT);
        server.start();
    }
}
