package bg.sofia.uni.fmi.mjt.cryptowallet.coinapi.dto;

import java.util.Set;

public record Cryptocurrencies(Set<CryptoInformation> cryptocurrencies, String lastUpdateOfInformation) {

    public static Cryptocurrencies of(Set<CryptoInformation> cryptocurrencies, String lastUpdateOfInformation) {
        return new Cryptocurrencies(cryptocurrencies, lastUpdateOfInformation);
    }
}
