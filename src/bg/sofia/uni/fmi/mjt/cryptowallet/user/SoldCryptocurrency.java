package bg.sofia.uni.fmi.mjt.cryptowallet.user;

import bg.sofia.uni.fmi.mjt.cryptowallet.coinapi.dto.CryptoInformation;
import com.google.gson.annotations.Expose;

public record SoldCryptocurrency(@Expose CryptoInformation soldCrypto, @Expose double sellingPrice,
                                 @Expose double profit) { }
