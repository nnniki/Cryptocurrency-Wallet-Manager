package bg.sofia.uni.fmi.mjt.cryptowallet.user;

import bg.sofia.uni.fmi.mjt.cryptowallet.coinapi.dto.CryptoInformation;
import bg.sofia.uni.fmi.mjt.cryptowallet.coinapi.dto.Cryptocurrencies;
import bg.sofia.uni.fmi.mjt.cryptowallet.exception.InsufficientAvailabilityException;
import bg.sofia.uni.fmi.mjt.cryptowallet.exception.InvalidSellingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class UserTest {

    private static final String SPACE = " ";
    private static final String ID = "ID:";
    private static final String NAME = "Name:";
    private static final String BOUGHT = "boughtPrice:";
    private static final String MONEY = "Money: ";
    private static final String COUNT = "boughtCount:";
    private static final String ACTIVE_INVESTMENTS = "ActiveInvestments: ";

    private User user;

    @BeforeEach
    public void setUpData() {
        user = new User("niki", "77777");
    }

    @Test
    public void testDepositMoneySuccessfully() {
        user.depositMoney(1500);

        assertEquals(1500, user.getMoney(), "Invalid result when depositing money");
    }

    @Test
    public void testDepositMoneyNegativeAmount() {
        assertThrows(IllegalArgumentException.class, () -> user.depositMoney(-1500),
                "IllegalArgument exception was expected when depositing negative amount of money");
    }

    @Test
    public void testBuyCryptoSuccessfully() throws InsufficientAvailabilityException {
        CryptoInformation info = new CryptoInformation("BTC", "Bitcoin",1,20525.4561);
        user.depositMoney(1500);
        double investingMoney = 1000;
        double boughtCount = 0.048;
        BoughtCryptocurrency bought = new BoughtCryptocurrency(info, investingMoney,boughtCount);
        Set<BoughtCryptocurrency> boughtCryptocurrency = new HashSet<>();
        boughtCryptocurrency.add(bought);

        user.buyCrypto(info,1000);
        var result = user.getBoughtCryptocurrencies();
        for (var curr : result) {
            assertEquals(info, curr.boughtCrypto(), "Invalid crypto information was returned");
            assertEquals(investingMoney,curr.buyingPrice(), "Invalid investing price was returned");
            assertEquals(boughtCount, curr.buyingCount(), 3 , "Invalid buying count was returned");
        }
    }

    @Test
    public void testBuyCryptoNotEnoughMoney() {
        CryptoInformation info = new CryptoInformation("BTC", "Bitcoin",1,20525.4561);
        user.depositMoney(500);
        assertThrows(InsufficientAvailabilityException.class, () -> user.buyCrypto(info, 1000),
                "InsufficientAvailabilityException was expected when you don't have enough money");
    }

    @Test
    public void testBuyCryptoNegativeInvestedMoney() {
        CryptoInformation info = new CryptoInformation("BTC", "Bitcoin",1,20525.4561);
        user.depositMoney(500);
        assertThrows(IllegalArgumentException.class, () -> user.buyCrypto(info, -1000),
                "IllegalArgumentException was expected when you invest negative or zero amount of money");
    }

    @Test
    public void testBuyCryptoNotAvailableForBuying() {
        CryptoInformation info = new CryptoInformation("BTC", "Bitcoin",1,0.0);
        user.depositMoney(1500);
        assertThrows(IllegalArgumentException.class, () -> user.buyCrypto(info, 1000),
                "IllegalArgumentException was expected when you try to buy crypto that isn't available for buying");
    }

    @Test
    public void testSellCryptoSuccessfully() throws InsufficientAvailabilityException, InvalidSellingException {
        CryptoInformation info = new CryptoInformation("BTC", "Bitcoin",1,20525.4561);
        user.depositMoney(1500);
        double investingMoney = 1000;
        double boughtCount = 0.048;
        BoughtCryptocurrency bought = new BoughtCryptocurrency(info, investingMoney,boughtCount);
        Set<BoughtCryptocurrency> boughtCryptocurrency = new HashSet<>();
        boughtCryptocurrency.add(bought);
        user.buyCrypto(info,1000);

        SoldCryptocurrency sold = new SoldCryptocurrency(info, 1000,0.0);
        Set<SoldCryptocurrency> soldCryptocurrencies = new HashSet<>();
        soldCryptocurrencies.add(sold);
        user.sellCrypto(info);
        assertIterableEquals(soldCryptocurrencies, user.getSoldCryptocurrencies(),
                "Invalid result when selling crypto");
        assertEquals(1500, user.getMoney(), "Invalid money after selling crypto");
        assertIterableEquals(new HashSet<>(), user.getBoughtCryptocurrencies(),
                "Bought cryptocurrency should be deleted after selling it");
    }

   @Test
    public void testSellCryptoBeforeBuyingIt() {
       CryptoInformation info = new CryptoInformation("BTC", "Bitcoin",1,20525.4561);

       assertThrows(InvalidSellingException.class, () -> user.sellCrypto(info),
               "InvalidSellingException was expected when selling crypto before buying it");
   }

   @Test
    public void testGetWalletSummaryAfterBuyingSuccessfully() throws InsufficientAvailabilityException {
       CryptoInformation info = new CryptoInformation("BTC", "Bitcoin",1,20000);
       user.depositMoney(1500);
       double investingMoney = 1000;
       double boughtCount = 0.05;
       BoughtCryptocurrency bought = new BoughtCryptocurrency(info, investingMoney,boughtCount);
       Set<BoughtCryptocurrency> boughtCryptocurrency = new HashSet<>();
       boughtCryptocurrency.add(bought);

       user.buyCrypto(info,1000);

       StringBuilder builder = new StringBuilder();

       builder.append(MONEY);
       builder.append(500.0);
       builder.append(SPACE);
       builder.append(ACTIVE_INVESTMENTS);
       builder.append(SPACE);
       builder.append(ID);
       builder.append("BTC ");
       builder.append(NAME);
       builder.append("Bitcoin ");
       builder.append(BOUGHT);
       builder.append(1000.0);
       builder.append(SPACE);
       builder.append(COUNT);
       builder.append(0.05);
       builder.append(SPACE);
       builder.append(SPACE);

       assertEquals(builder.toString(), user.getWalletSummary().toString(), "Invalid wallet summary was returned");
   }

    @Test
    public void testGetWalletOverallSummaryAfterBuyingSuccessfully() throws InsufficientAvailabilityException {
        CryptoInformation info = new CryptoInformation("BTC", "Bitcoin",1,20000);
        user.depositMoney(1500);
        double investingMoney = 1000;
        double boughtCount = 0.05;
        BoughtCryptocurrency bought = new BoughtCryptocurrency(info, investingMoney,boughtCount);
        Set<BoughtCryptocurrency> boughtCryptocurrency = new HashSet<>();
        boughtCryptocurrency.add(bought);

        user.buyCrypto(info,1000);

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


        Set<CryptoInformation> s = new HashSet<>();
        s.add(info);
        Cryptocurrencies crypto = Cryptocurrencies.of(s, "2023-02-14 16:00:00");
        assertEquals(builder.toString(), user.getWalletOverall(crypto).toString(),
                "Invalid overall wallet summary was returned");
    }

    @Test
    public void testGetWalletOverallSummaryAfterBuyingAndSellingSuccessfully() throws InsufficientAvailabilityException,
            InvalidSellingException {

        CryptoInformation info = new CryptoInformation("BTC", "Bitcoin",1,20000);
        user.depositMoney(1500);
        double investingMoney = 1000;
        double boughtCount = 0.05;
        BoughtCryptocurrency bought = new BoughtCryptocurrency(info, investingMoney,boughtCount);
        Set<BoughtCryptocurrency> boughtCryptocurrency = new HashSet<>();
        boughtCryptocurrency.add(bought);

        user.buyCrypto(info,1000);
        user.sellCrypto(info);

        StringBuilder builder = new StringBuilder();

        builder.append("ActiveInvestments:  ");
        builder.append("FinishedInvestments: ");
        builder.append("ID:BTC ");
        builder.append("Name:Bitcoin ");
        builder.append("currentProfit:0.0  ");
        builder.append("overallProfit:");
        builder.append("0.0  ");

        Set<CryptoInformation> s = new HashSet<>();
        s.add(info);
        Cryptocurrencies crypto = Cryptocurrencies.of(s, "2023-02-14 16:00:00");
        assertEquals(builder.toString(), user.getWalletOverall(crypto).toString(),
                "Invalid overall wallet summary was returned");
    }
}
