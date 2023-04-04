package bg.sofia.uni.fmi.mjt.cryptowallet.coinapi;

import bg.sofia.uni.fmi.mjt.cryptowallet.coinapi.dto.CryptoInformation;
import bg.sofia.uni.fmi.mjt.cryptowallet.coinapi.dto.Cryptocurrencies;
import bg.sofia.uni.fmi.mjt.cryptowallet.exception.BadRequestToRestApiException;
import bg.sofia.uni.fmi.mjt.cryptowallet.exception.TooManyRequestsException;
import bg.sofia.uni.fmi.mjt.cryptowallet.exception.UnauthorizedException;
import com.google.gson.Gson;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class CoinApiDataTest {

    private static Cryptocurrencies cryptocurrencies;
    private static String cryptocurrenciesToJson;
    private CoinApiData coinApiData;

    @Mock
    private HttpClient httpClientMock;

    @Mock
    private HttpResponse<String> httpResponseMock;


    @BeforeAll
    public static void setUpClassData() {
        CryptoInformation BTC = new CryptoInformation("BTC", "Bitcoin", 1 ,20525.4561);
        CryptoInformation ETC = new CryptoInformation("ETH", "Ethereum", 1, 25.1234);
        Set<CryptoInformation> crypto = new HashSet<>();
        crypto.add(BTC);
        crypto.add(ETC);

        cryptocurrenciesToJson = new Gson().toJson(crypto);
        cryptocurrencies = Cryptocurrencies.of(crypto, "2023-02-14 16:00:00");
    }

    @BeforeEach
    public void setUp() throws IOException, InterruptedException {

        when(httpClientMock.send(Mockito.any(HttpRequest.class), ArgumentMatchers.<HttpResponse.BodyHandler<String>>any()))
                .thenReturn(httpResponseMock);

        coinApiData = new CoinApiData(httpClientMock);
    }

    @Test
    public void testGetCryptocurrenciesInfoSuccessfully() throws TooManyRequestsException, UnauthorizedException,
            BadRequestToRestApiException {

        when(httpResponseMock.statusCode()).thenReturn(HttpURLConnection.HTTP_OK);
        when(httpResponseMock.body()).thenReturn(cryptocurrenciesToJson);

        var result = coinApiData.getCryptocurrenciesInfo();
        assertTrue(result.cryptocurrencies().containsAll(cryptocurrencies.cryptocurrencies()),
                "Invalid cryptocurrencies were returned");
        assertTrue(cryptocurrencies.cryptocurrencies().containsAll(result.cryptocurrencies()),
                "Invalid cryptocurrencies were returned");
    }

    @Test
    public void testGetCryptocurrenciesInfoInvalidRequests() {
        when(httpResponseMock.statusCode()).thenReturn(HttpURLConnection.HTTP_BAD_REQUEST);

        assertThrows(BadRequestToRestApiException.class, () -> coinApiData.getCryptocurrenciesInfo()
        ,"There is a problem with your request, check API documentation");
    }

    @Test
    public void testGetCryptocurrenciesInfoInvalidApiKey() {
        when(httpResponseMock.statusCode()).thenReturn(HttpURLConnection.HTTP_UNAUTHORIZED);

        assertThrows(UnauthorizedException.class, () -> coinApiData.getCryptocurrenciesInfo()
                ,"There is a problem with your API key, check if it is correct");
    }

    @Test
    public void testGetCryptocurrenciesInfoTooManyRequests() {
        when(httpResponseMock.statusCode()).thenReturn(CoinApiData.TOO_MANY_REQUESTS);

        assertThrows(TooManyRequestsException.class, () -> coinApiData.getCryptocurrenciesInfo()
                ,"You have made too many requests for today, try again later");
    }
}
