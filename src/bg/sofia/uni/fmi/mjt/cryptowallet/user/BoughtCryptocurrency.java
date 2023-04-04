package bg.sofia.uni.fmi.mjt.cryptowallet.user;

import bg.sofia.uni.fmi.mjt.cryptowallet.coinapi.dto.CryptoInformation;
import com.google.gson.annotations.Expose;

public record BoughtCryptocurrency(@Expose CryptoInformation boughtCrypto,
                                   @Expose double buyingPrice, @Expose double buyingCount) { }
