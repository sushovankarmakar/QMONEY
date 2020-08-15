
package com.crio.warmup.stock.quotes;

import com.crio.warmup.stock.dto.AlphavantageCandle;
import com.crio.warmup.stock.dto.AlphavantageDailyResponse;
import com.crio.warmup.stock.dto.Candle;
import com.crio.warmup.stock.exception.StockQuoteServiceException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.springframework.web.client.RestTemplate;

public class AlphavantageService implements StockQuotesService {

  private RestTemplate restTemplate;

  protected AlphavantageService(RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
  }

  @Override
  public List<Candle> getStockQuote(String symbol, LocalDate from, LocalDate to) 
      throws JsonProcessingException, StockQuoteServiceException {
    if (to.isBefore(from)) {
      to = LocalDate.now();
    }

    String uri = buildUri(symbol);

    ObjectMapper objectMapper = getObjectMapper();
    String result = 
        restTemplate.getForObject(uri, String.class);

    //System.out.println(result);
    AlphavantageDailyResponse response;

    try {
      response = objectMapper.readValue(result, 
        new TypeReference<AlphavantageDailyResponse>() {});
    } catch (JsonProcessingException e) {
      throw new JsonProcessingException("Can't process the Json file") {};
    }
    
    
    //System.out.println("-----------------------");
    //System.out.print(response.getCandles());    

    List<Candle> listOfCandles = new LinkedList<>();    

    if (response != null) {
      try {
        Map<LocalDate, AlphavantageCandle> alphaCandles = response.getCandles();

        for (Map.Entry<LocalDate, AlphavantageCandle> entry : alphaCandles.entrySet()) {

          //System.out.print(entry.getValue());

          LocalDate date = entry.getKey();

          if (date.compareTo(from) >= 0 && date.compareTo(to) <= 0) {
            entry.getValue().setDate(date);
            listOfCandles.add(entry.getValue());
          }
        }
      } catch (Exception e) {
        throw new StockQuoteServiceException(
          "Can't process the response from a third-party service or" 
          + "response contains an error or is otherwise invalid.");
      }

    } else {
      throw new StockQuoteServiceException(
          "Can't process the response from a third-party service or" 
          + "response contains an error or is otherwise invalid.");
    }

    listOfCandles.sort(getComparator());

    //System.out.print(listOfCandles);
    
    return listOfCandles;
  }

  private static Comparator<Candle> getComparator() {
    return Comparator.comparing(Candle::getDate);
  }

  private static String buildUri(String symbol) {
    // String uriTemplate = "https://www.alphavantage.co/" 
    //         + "query?function=TIME_SERIES_DAILY&symbol=$SYMBOL&outputsize=full&apikey=$APIKEY";
    String uriTemplate = "https://www.alphavantage.co/" 
            + "query?function=TIME_SERIES_DAILY&apikey=2LTLXWOB8DGU6MAU&outputsize=full&symbol=" 
            + symbol;        
    //String token = "2LTLXWOB8DGU6MAU";
    //String url = uriTemplate.replace("$SYMBOL", symbol)
    //                        .replace("$APIKEY", token);

    // URI uri = URI.create(uriTemplate);
    // try {
    //   uri = new URI(uriTemplate);
    // } catch (URISyntaxException e) {
    //   e.printStackTrace();
    // }
   
    return uriTemplate;
  }

  private static ObjectMapper getObjectMapper() {
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());
    return objectMapper;
  }

  // TODO: CRIO_TASK_MODULE_ADDITIONAL_REFACTOR
  //  Implement the StockQuoteService interface as per the contracts. Call Alphavantage service
  //  to fetch daily adjusted data for last 20 years.
  //  Refer to documentation here: https://www.alphavantage.co/documentation/
  //  --
  //  The implementation of this functions will be doing following tasks:
  //    1. Build the appropriate url to communicate with third-party.
  //       The url should consider startDate and endDate if it is supported by the provider.
  //    2. Perform third-party communication with the url prepared in step#1
  //    3. Map the response and convert the same to List<Candle>
  //    4. If the provider does not support startDate and endDate, then the implementation
  //       should also filter the dates based on startDate and endDate. Make sure that
  //       result contains the records for for startDate and endDate after filtering.
  //    5. Return a sorted List<Candle> sorted ascending based on Candle#getDate
  // Note:
  // 1. Make sure you use {RestTemplate#getForObject(URI, String)} else the test will fail.
  // 2. Run the tests using command below and make sure it passes:
  //    ./gradlew test --tests AlphavantageServiceTest
  //CHECKSTYLE:OFF
    //CHECKSTYLE:ON
  // TODO: CRIO_TASK_MODULE_ADDITIONAL_REFACTOR
  //  1. Write a method to create appropriate url to call Alphavantage service. The method should
  //     be using configurations provided in the {@link @application.properties}.
  //  2. Use this method in #getStockQuote.

  // TODO: CRIO_TASK_MODULE_EXCEPTIONS
  //   1. Update the method signature to match the signature change in the interface.
  //   2. Start throwing new StockQuoteServiceException when you get some invalid response from
  //      Alphavantage, or you encounter a runtime exception during Json parsing.
  //   3. Make sure that the exception propagates all the way from PortfolioManager, so that the
  //      external user's of our API are able to explicitly handle this exception upfront.
  //CHECKSTYLE:OFF
}

