package bg.sofia.uni.fmi.mjt.cryptowallet.user;

import bg.sofia.uni.fmi.mjt.cryptowallet.coinapi.dto.CryptoInformation;
import bg.sofia.uni.fmi.mjt.cryptowallet.coinapi.dto.Cryptocurrencies;
import bg.sofia.uni.fmi.mjt.cryptowallet.exception.InsufficientAvailabilityException;
import bg.sofia.uni.fmi.mjt.cryptowallet.exception.InvalidSellingException;
import com.google.gson.annotations.Expose;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;

public class User {
    private static final double INITIAL_MONEY = 0.0;
    private static final String SPACE = " ";
    private static final String ID = "ID:";
    private static final String NAME = "Name:";
    private static final String BOUGHT = "boughtPrice:";
    private static final String CURRENT = "currentPrice:";
    private static final String PROFIT = "currentProfit:";
    private static final String MONEY = "Money: ";
    private static final String OVERALL_PROFIT = "overallProfit:";
    private static final String COUNT = "boughtCount:";
    private static final String ACTIVE_INVESTMENTS = "ActiveInvestments: ";
    private static final String FINISHED_INVESTMENTS = "FinishedInvestments: ";
    @Expose
    private final String username;
    private final String password;
    @Expose
    private final byte[] passwordBytes;
    @Expose
    private double money;
    @Expose
    private Set<BoughtCryptocurrency> boughtCryptocurrencies;
    @Expose
    private Set<SoldCryptocurrency> soldCryptocurrencies;

    public User(String username, String password) {
        this.username = username;
        this.password = password;
        passwordBytes = password.getBytes(StandardCharsets.UTF_8);
        this.money = INITIAL_MONEY;
        boughtCryptocurrencies = new HashSet<>();
        soldCryptocurrencies = new HashSet<>();
    }

    public void depositMoney(double money) {
        if (money <= INITIAL_MONEY) {
            throw new IllegalArgumentException("You can't deposit zero or negative amount of money ");
        }

        this.money += money;
    }

    public void buyCrypto(CryptoInformation info, double investingMoney) throws InsufficientAvailabilityException {
        if (investingMoney > this.money) {
            throw new InsufficientAvailabilityException("You don't have enough money ");
        }

        if (investingMoney <= 0.0) {
            throw new IllegalArgumentException("You can not invest negative or zero amount of money ");
        }

        if (info.price() <= 0.0) {
            throw new IllegalArgumentException("This cryptocurrency can't be bought at the moment ");
        }

        this.money -= investingMoney;
        double countBoughtCrypto = investingMoney / info.price();
        BoughtCryptocurrency boughtCryptocurrency = new BoughtCryptocurrency(info, investingMoney, countBoughtCrypto);
        if (boughtCryptocurrencies.contains(boughtCryptocurrency)) {
            BoughtCryptocurrency crypto = new BoughtCryptocurrency(info, investingMoney * 2,
                    countBoughtCrypto * 2);

            boughtCryptocurrencies.remove(boughtCryptocurrency);
            boughtCryptocurrencies.add(crypto);
        }
        else {
            boughtCryptocurrencies.add(boughtCryptocurrency);
        }
    }

    private void deleteSoldCrypto(CryptoInformation info) {
        Iterator<BoughtCryptocurrency> it = boughtCryptocurrencies.iterator();

        while (it.hasNext()) {
            var curr = it.next();
            if (curr.boughtCrypto().assetID().equals(info.assetID())) {
                it.remove();
            }
        }
    }

    public void sellCrypto(CryptoInformation info) throws InvalidSellingException {
        double currentSellingPrice = info.price();
        double countBoughtCrypto = 0.0;
        double givenMoneyBoughtCrypto = 0.0;
        for (var curr : boughtCryptocurrencies) {
            if (curr.boughtCrypto().assetID().equals(info.assetID())) {
                countBoughtCrypto += curr.buyingCount();
                givenMoneyBoughtCrypto += curr.buyingPrice();
            }
        }

        if (countBoughtCrypto == INITIAL_MONEY) {
            throw new InvalidSellingException("You can't sell cryptocurrency that you haven't bought ");
        }
        double sumToEarn = countBoughtCrypto * currentSellingPrice;
        this.money += sumToEarn;
        double profit = sumToEarn - givenMoneyBoughtCrypto;
        SoldCryptocurrency soldCrypto = new SoldCryptocurrency(info, sumToEarn, profit);
        soldCryptocurrencies.add(soldCrypto);
        deleteSoldCrypto(info);
    }

    public StringBuilder getWalletSummary() {
        StringBuilder builder = new StringBuilder();

        builder.append(MONEY);
        builder.append(getMoney());
        builder.append(SPACE);
        builder.append(ACTIVE_INVESTMENTS);
        builder.append(SPACE);

        for (var curr : boughtCryptocurrencies) {
            builder.append(ID);
            builder.append(curr.boughtCrypto().assetID());
            builder.append(SPACE);
            builder.append(NAME);
            builder.append(curr.boughtCrypto().assetName());
            builder.append(SPACE);
            builder.append(BOUGHT);
            builder.append(curr.buyingPrice());
            builder.append(SPACE);
            builder.append(COUNT);
            builder.append(curr.buyingCount());
            builder.append(SPACE);
            builder.append(SPACE);
        }

        return builder;
    }

    private StringBuilder getFinishedInvestments() {
        StringBuilder builder = new StringBuilder();

        builder.append(FINISHED_INVESTMENTS);
        for (var currCrypto : soldCryptocurrencies) {
            builder.append(ID);
            builder.append(currCrypto.soldCrypto().assetID());
            builder.append(SPACE);
            builder.append(NAME);
            builder.append(currCrypto.soldCrypto().assetName());
            builder.append(SPACE);
            builder.append(PROFIT);
            builder.append(currCrypto.profit());
            builder.append(SPACE);
            builder.append(SPACE);
        }
        return builder;
    }

    public StringBuilder getWalletOverall(Cryptocurrencies info) {
        StringBuilder builder = new StringBuilder();
        double overallProfit = INITIAL_MONEY;

        builder.append(ACTIVE_INVESTMENTS);
        builder.append(SPACE);
        for (var currCrypto : boughtCryptocurrencies) {
            for (var curr : info.cryptocurrencies()) {
                if (currCrypto.boughtCrypto().assetID().equals(curr.assetID())) {
                    double boughtPrice = currCrypto.buyingPrice();
                    double currPrice = curr.price();
                    double profit = (currPrice * currCrypto.buyingCount()) - boughtPrice;
                    overallProfit += profit;

                    builder.append(ID);
                    builder.append(curr.assetID());
                    builder.append(SPACE);
                    builder.append(NAME);
                    builder.append(curr.assetName());
                    builder.append(SPACE);
                    builder.append(BOUGHT);
                    builder.append(currCrypto.buyingPrice());
                    builder.append(SPACE);
                    builder.append(CURRENT);
                    builder.append(curr.price() * currCrypto.buyingCount());
                    builder.append(SPACE);
                    builder.append(PROFIT);
                    builder.append(profit);
                    builder.append(SPACE);
                    builder.append(SPACE);
                }
            }
        }
        for (var currCrypto : soldCryptocurrencies) {
            overallProfit += currCrypto.profit();
        }
        builder.append(getFinishedInvestments());
        builder.append(OVERALL_PROFIT);
        builder.append(overallProfit);
        builder.append(SPACE);
        builder.append(SPACE);

        return builder;
    }

    public String getUsername() {
        return username;
    }

    public double getMoney() {
        return money;
    }

    public Set<BoughtCryptocurrency> getBoughtCryptocurrencies() {
        return boughtCryptocurrencies;
    }

    public Set<SoldCryptocurrency> getSoldCryptocurrencies() {
        return soldCryptocurrencies;
    }

    public byte[] getPasswordBytes() {
        return passwordBytes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return username.equals(user.username) && password.equals(user.password);
    }

    @Override
    public int hashCode() {
        return Objects.hash(username, password);
    }
}
