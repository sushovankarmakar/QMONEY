package com.crio.warmup.stock.portfolio;

import com.crio.warmup.stock.dto.AnnualizedReturn;
import com.crio.warmup.stock.dto.Candle;
import com.crio.warmup.stock.dto.PortfolioTrade;
import com.crio.warmup.stock.dto.TiingoCandle;
import com.crio.warmup.stock.exception.StockQuoteServiceException;

import com.crio.warmup.stock.quotes.StockQuotesService;
import com.fasterxml.jackson.core.JsonProcessingException;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.springframework.web.client.RestTemplate;

public class PortfolioManagerImpl implements PortfolioManager {

  private RestTemplate restTemplate;
  private StockQuotesService stockQuotesService;
  
  // Caution: Do not delete or modify the constructor, or else your build will break!
  // This is absolutely necessary for backward compatibility

  protected PortfolioManagerImpl(StockQuotesService stockQuotesService) {
    this.stockQuotesService = stockQuotesService;
  }
  
  @Deprecated
  protected PortfolioManagerImpl(RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
  }

  //TODO: CRIO_TASK_MODULE_REFACTOR
  // Now we want to convert our code into a module, so we will not call it from main anymore.
  // Copy your code from Module#3 PortfolioManagerApplication#calculateAnnualizedReturn
  // into #calculateAnnualizedReturn function here and make sure that it
  // follows the method signature.
  // Logic to read Json file and convert them into Objects will not be required further as our
  // clients will take care of it, going forward.
  // Test your code using Junits provided.
  // Make sure that all of the tests inside PortfolioManagerTest using command below -
  // ./gradlew test --tests PortfolioManagerTest
  // This will guard you against any regressions.
  // run ./gradlew build in order to test yout code, and make sure that
  // the tests and static code quality pass.

  //CHECKSTYLE:OFF

  @Override
  public List<AnnualizedReturn> calculateAnnualizedReturn(List<PortfolioTrade> portfolioTrades, LocalDate endDate)
  throws StockQuoteServiceException {
    
    List<AnnualizedReturn> annualizedReturnList = new LinkedList<>();

    for (PortfolioTrade portfolioTrade : portfolioTrades) {
      List<Candle> candles = Collections.emptyList();
      try {
        candles = stockQuotesService.getStockQuote(portfolioTrade.getSymbol(), portfolioTrade.getPurchaseDate(), endDate);
      } 
      catch (StockQuoteServiceException | JsonProcessingException ex) {
        throw new StockQuoteServiceException(
          "Can't process the response from a third-party service or response contains an error or is otherwise invalid.");
      }
      
      if (candles != null) {
        double buyPrice = candles.stream()
                                .filter(candle -> 
                                    candle.getDate().equals(portfolioTrade.getPurchaseDate()))
                                .findFirst().get()
                                .getOpen();

        // double sellPrice = candles.stream()
        //                         .filter(candle -> 
        //                             candle.getDate().equals(endDate)
        //                             || candle.getDate().equals(endDate.minusDays(1)))
        //                         .findFirst().get()
        //                         .getClose();

        double sellPrice = candles.get(candles.size() - 1).getClose();                            
                                
                                
        annualizedReturnList.add(
              calculateAnnualizedReturns(endDate, 
              portfolioTrade, buyPrice, sellPrice));                        
      } else {
        throw new StockQuoteServiceException(
          "Can't process the response from a third-party service or" 
          + "response contains an error or is otherwise invalid.");
      }  
    }

    annualizedReturnList.sort(getComparator());

    return annualizedReturnList;
  }

  @Override
  public List<AnnualizedReturn> calculateAnnualizedReturnParallel(
      List<PortfolioTrade> portfolioTrades,
      LocalDate endDate, int numThreads) throws InterruptedException,
      StockQuoteServiceException {
    
    List<AnnualizedReturn> annualizedReturnList = new LinkedList<>();    
    ExecutorService executorService = Executors.newFixedThreadPool(numThreads);
    
    List<Future<AnnualizedReturn>> futureList = new ArrayList<>();   
     

    for (PortfolioTrade portfolioTrade : portfolioTrades) {
      Callable<AnnualizedReturn> callable = new Mycallable(stockQuotesService, portfolioTrade, endDate);

      Future<AnnualizedReturn> future = executorService.submit(callable);
      futureList.add(future);
    }

    for (Future<AnnualizedReturn> future : futureList) {
      try {
        annualizedReturnList.add(future.get());

      } catch (InterruptedException | ExecutionException e) {
        throw new StockQuoteServiceException(
          "Can't process the response from a third-party service or" 
          + "response contains an error or is otherwise invalid.");
      }
    }

    // shutting down the executor service
    executorService.shutdown();
    try {
      if (!executorService.awaitTermination(800, TimeUnit.MILLISECONDS)) {
        executorService.shutdown();
      }
    } catch(InterruptedException e) {
      executorService.shutdown();
    }
    
    annualizedReturnList.sort(getComparator());
    return annualizedReturnList;
  }

  static class Mycallable implements Callable<AnnualizedReturn> {
    
    private StockQuotesService stockQuotesService;
    private PortfolioTrade portfolioTrade;
    private LocalDate endDate;

    Mycallable(StockQuotesService stockQuotesService, PortfolioTrade portfolioTrade, LocalDate endDate) {
      this.stockQuotesService = stockQuotesService;
      this.portfolioTrade = portfolioTrade;
      this.endDate = endDate;
    }

    @Override
    public AnnualizedReturn call() throws StockQuoteServiceException {

      List<Candle> candles = Collections.emptyList();
      try {
        candles = stockQuotesService.getStockQuote(portfolioTrade.getSymbol(), portfolioTrade.getPurchaseDate(), endDate);
      } 
      catch (Exception e) {
        throw new StockQuoteServiceException(
          "Can't process the response from a third-party service or" 
          + "response contains an error or is otherwise invalid.");
      }

      if (candles != null) {
        double buyPrice = candles.stream()
                                .filter(candle -> 
                                    candle.getDate().equals(portfolioTrade.getPurchaseDate()))
                                .findFirst().get()
                                .getOpen();

        double sellPrice = candles.get(candles.size() - 1).getClose();                            
                          
        AnnualizedReturn annualizedReturn = calculateAnnualizedReturns(endDate, portfolioTrade, buyPrice, sellPrice);
        return annualizedReturn;

      } else {
        throw new StockQuoteServiceException(
          "Can't process the response from a third-party service or" 
          + "response contains an error or is otherwise invalid.");
      }

    }
  }




  public static AnnualizedReturn calculateAnnualizedReturns(LocalDate endDate, 
          PortfolioTrade trade, Double buyPrice, Double sellPrice) {
    double totalReturns = (sellPrice - buyPrice) / buyPrice;
    long days = ChronoUnit.DAYS.between(trade.getPurchaseDate(), endDate);
    double years = (double) days / 365;
    double annualizedReturns = Math.pow((1 + totalReturns), (1 / years)) - 1;
    return new AnnualizedReturn(trade.getSymbol(), annualizedReturns, totalReturns);
  }


  private static Comparator<AnnualizedReturn> getComparator() {
    return Comparator.comparing(AnnualizedReturn::getAnnualizedReturn).reversed();
  }

  //CHECKSTYLE:OFF

  // TODO: CRIO_TASK_MODULE_REFACTOR
  //  Extract the logic to call Tiingo thirdparty APIs to a separate function.
  //  It should be split into fto parts.
  //  Part#1 - Prepare the Url to call Tiingo based on a template constant,
  //  by replacing the placeholders.
  //  Constant should look like
  //  https://api.tiingo.com/tiingo/daily/<ticker>/prices?startDate=?&endDate=?&token=?
  //  Where ? are replaced with something similar to <ticker> and then actual url produced by
  //  replacing the placeholders with actual parameters.


  public List<TiingoCandle> getStockQuote(String symbol, LocalDate from, LocalDate to)
      throws JsonProcessingException {
    if(to.isBefore(from)) {
      to = LocalDate.now();
    }        
    String url = buildUri(symbol, from, to);
    //URI uri = new URI(url);
    TiingoCandle[] response = restTemplate.getForObject(url, TiingoCandle[].class);

    List<TiingoCandle> listOfCandles = new LinkedList<>();

    if (response != null) {
      listOfCandles.addAll(Arrays.asList(response));
    } else {
      throw new RuntimeException();
    }

    return listOfCandles;
  }

  protected static String buildUri(String symbol, LocalDate startDate, LocalDate endDate) {
      String uriTemplate = "https://api.tiingo.com/tiingo/daily/$SYMBOL/prices?"
            + "startDate=$STARTDATE&endDate=$ENDDATE&token=$APIKEY";

      String token = "b2e11b78d70e7824e9493801b3c6a07a06ea4f5d";
      String uri =  uriTemplate.replace("$SYMBOL", symbol)
                               .replace("$STARTDATE", startDate.toString())
                               .replace("$ENDDATE", endDate.toString())
                               .replace("$APIKEY", token);      
      return uri;
  }
}
