package bg.sofia.uni.fmi.mjt.cryptowallet.coinapi;

import bg.sofia.uni.fmi.mjt.cryptowallet.coinapi.dto.CryptoInformation;
import bg.sofia.uni.fmi.mjt.cryptowallet.coinapi.dto.Cryptocurrencies;
import bg.sofia.uni.fmi.mjt.cryptowallet.exception.BadRequestToRestApiException;
import bg.sofia.uni.fmi.mjt.cryptowallet.exception.TooManyRequestsException;
import bg.sofia.uni.fmi.mjt.cryptowallet.exception.UnauthorizedException;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.stream.Collectors;

public class CoinApiData {

    private static final String API_KEY = null; // Enter your API key
    private static final String API_ENDPOINT_SCHEME = "https";
    private static final String API_ENDPOINT_HOST = "rest.coinapi.io";
    private static final String API_ENDPOINT_PATH = "/v1/assets";
    private static final String API_HEADER_TEXT = "X-CoinAPI-Key";
    private static final String TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
    public static final int TOO_MANY_REQUESTS = 429;
    public static final int MAX_CRYPTOCURRENCIES = 50;
    private static final Gson GSON = new Gson();
    private final HttpClient client;

    public CoinApiData(HttpClient client) {
        this.client = client;
    }

    private HttpResponse<String> sendRequest() throws BadRequestToRestApiException {
        HttpResponse<String> response;

        try {
            URI uri = new URI(API_ENDPOINT_SCHEME, API_ENDPOINT_HOST, API_ENDPOINT_PATH, null);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(uri)
                    .setHeader(API_HEADER_TEXT, API_KEY)
                    .build();

            response = client.send(request, HttpResponse.BodyHandlers.ofString());

            return response;
        } catch (Exception e) {
            throw new BadRequestToRestApiException("There is a problem with your request", e);
        }
    }

    public Cryptocurrencies getCryptocurrenciesInfo() throws BadRequestToRestApiException, TooManyRequestsException,
            UnauthorizedException {

        HttpResponse<String> response = sendRequest();

        if (response.statusCode() == HttpURLConnection.HTTP_OK) {
            Type cryptoListType = new TypeToken<Set<CryptoInformation>>() { }.getType();
            Set<CryptoInformation> userArray = GSON.fromJson(response.body(), cryptoListType);

            Set<CryptoInformation> onlyCryptoInfo = userArray.stream()
                    .filter(e -> e.isCrypto() == 1)
                    .limit(MAX_CRYPTOCURRENCIES)
                    .collect(Collectors.toSet());

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(TIME_FORMAT);
            String formattedTime = LocalDateTime.now().format(formatter);

            return Cryptocurrencies.of(onlyCryptoInfo, formattedTime);

        } else if (response.statusCode() == HttpURLConnection.HTTP_BAD_REQUEST) {
            throw new BadRequestToRestApiException("There is a problem with your request");
        } else if (response.statusCode() == HttpURLConnection.HTTP_UNAUTHORIZED) {
            throw new UnauthorizedException("Your API key is wrong or invalid");
        } else if (response.statusCode() == TOO_MANY_REQUESTS) {
            throw new TooManyRequestsException("You have made too many requests to the server, try again later");
        }

        return null;
    }
}
