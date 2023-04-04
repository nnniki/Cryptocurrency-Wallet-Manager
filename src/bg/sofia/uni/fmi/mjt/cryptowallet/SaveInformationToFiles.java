package bg.sofia.uni.fmi.mjt.cryptowallet;

import bg.sofia.uni.fmi.mjt.cryptowallet.coinapi.CoinApiThread;
import bg.sofia.uni.fmi.mjt.cryptowallet.coinapi.dto.Cryptocurrencies;
import bg.sofia.uni.fmi.mjt.cryptowallet.user.User;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.channels.SocketChannel;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

public class SaveInformationToFiles {

    private static final String ERROR_INFO_FILE = "Exceptions.txt";
    private static final String USER_EXCEPTION = "Exception occurred to user ";
    private static final String CRYPTO_INFO_FILE = "CryptoInformation.txt";
    private static final String USERS_INFO_FILE = "UsersInformation.txt";
    private static final Gson GSON = new Gson();

    public void saveErrorInfoToFile(Exception e) throws IOException {
        try (Writer writer = new FileWriter(ERROR_INFO_FILE, true)) {
            writer.write(e.getMessage());
            writer.write(System.lineSeparator());
            writer.flush();
            writer.write(Arrays.toString(e.getStackTrace()));
            writer.write(System.lineSeparator());
            writer.flush();
        }
    }

    public void saveUserErrorsToFile(Exception e, SocketChannel channel, Map<SocketChannel, User> userChannels)
            throws IOException {

        try (Writer writer = new FileWriter(ERROR_INFO_FILE, true)) {
            writer.write(USER_EXCEPTION + userChannels.get(channel).getUsername());
            writer.write(System.lineSeparator());
            writer.write(e.getMessage());
            writer.write(System.lineSeparator());
            writer.flush();
            writer.write(Arrays.toString(e.getStackTrace()));
            writer.write(System.lineSeparator());
            writer.flush();
        }
    }

    public void saveCryptocurrenciesToFile(Cryptocurrencies cryptocurrencies) throws IOException {
        try (Writer writer = new FileWriter(CRYPTO_INFO_FILE)) {
            GSON.toJson(cryptocurrencies, writer);
        }
    }

    void saveUsersInfoToFile(Set<User> registeredUsers) throws IOException {
        try (Writer writer = new FileWriter(USERS_INFO_FILE, false)) {
            Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
            gson.toJson(registeredUsers, writer);
        }
    }

    public Cryptocurrencies getCryptocurrenciesFromApi() {
        Callable<Cryptocurrencies> callable = new CoinApiThread();
        try {
            Cryptocurrencies cryptocurrencies = callable.call();
            saveCryptocurrenciesToFile(cryptocurrencies);
            return cryptocurrencies;
        } catch (Exception e) {
            try {
                System.out.println(e.getMessage());
                saveErrorInfoToFile(e);
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }
        return null;
    }

}
