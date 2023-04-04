# Cryptocurrency-Wallet-Manager

This is a project made by me for Modern Java Technologies course in Faculty Of Matematics and Informatics at Sofia University St. Kliment Ohridski

In this project i am simulating virtual cryptocurrencies wallet.

I am using free REST API to take information about the current price of the criptocurrencies, which i cache for the next 30 minutes so within this time no requests to
the REST API are required.

You can find more information about the REST API i use here - https://www.coinapi.io/

To use the REST API you will have to take your personal API KEY - https://www.coinapi.io/pricing?apikey

This project is client-server aplication, in which the server must respond to multiple requests from many clients at the same time

The commands my project supports are :
1) help - Print instructions
2) register <username> <password> - Make an account
3) login <username> <password> - Login into your profil
4) list-offerings - Gives you information about the available cryptocurrencies at the moment
5) buy <offering_code> <amount_money> - Buy cryptocurrency, where <offering_code> is the code of the crypto you want to buy, and <amount_money> are the money you are investing
6) sell <offering_code> - Sell cryptocurrency, where <offering_code> is the code of the crypto you want to buy
7) get-wallet-summary - Gives you information about the active investments of the user and the amount of money in his wallet
8) get-wallet-overall-summary - Gives you a full information about the profit/loss of the user's investments
9) disconnect - Exit and save information
