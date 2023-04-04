package bg.sofia.uni.fmi.mjt.cryptowallet.coinapi.dto;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public record CryptoInformation(@Expose @SerializedName("asset_id") String assetID,
                                @Expose @SerializedName("name") String assetName,
                                @SerializedName("type_is_crypto") int isCrypto,
                                @Expose @SerializedName("price_usd") double price) {
}
