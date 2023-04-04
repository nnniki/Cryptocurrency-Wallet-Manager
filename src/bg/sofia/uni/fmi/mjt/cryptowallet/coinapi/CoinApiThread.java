package bg.sofia.uni.fmi.mjt.cryptowallet.coinapi;

import bg.sofia.uni.fmi.mjt.cryptowallet.coinapi.dto.Cryptocurrencies;
import bg.sofia.uni.fmi.mjt.cryptowallet.exception.BadRequestToRestApiException;
import bg.sofia.uni.fmi.mjt.cryptowallet.exception.TooManyRequestsException;
import bg.sofia.uni.fmi.mjt.cryptowallet.exception.UnauthorizedException;

import java.net.http.HttpClient;
import java.util.concurrent.Callable;

public class CoinApiThread implements Callable<Cryptocurrencies> {

    @Override
    public Cryptocurrencies call() throws TooManyRequestsException, UnauthorizedException,
            BadRequestToRestApiException {

        HttpClient client = HttpClient.newBuilder().build();
        CoinApiData data = new CoinApiData(client);

        return data.getCryptocurrenciesInfo();
    }
}
