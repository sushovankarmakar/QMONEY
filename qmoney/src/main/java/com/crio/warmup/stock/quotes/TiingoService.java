
package com.crio.warmup.stock.quotes;

import com.crio.warmup.stock.dto.Candle;
import com.crio.warmup.stock.dto.TiingoCandle;
import com.crio.warmup.stock.exception.StockQuoteServiceException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.time.LocalDate;
import java.util.LinkedList;
import java.util.List;
import org.springframework.web.client.RestTemplate;

public class TiingoService implements StockQuotesService {
  private RestTemplate restTemplate;

  protected TiingoService(RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
  }

  @Override
  public List<Candle> getStockQuote(String symbol, LocalDate from, LocalDate to) 
      throws JsonProcessingException, StockQuoteServiceException {

    if (to.isBefore(from)) {
      to = LocalDate.now();
    }

    ObjectMapper objectMapper = getObjectMapper();

    String uri = buildUri(symbol, from, to);

    String result = restTemplate.getForObject(uri, String.class);

    //System.out.print(result);

    List<Candle> listOfCandles = new LinkedList<>();

    if (result != null) {
      List<TiingoCandle> candles;
      try {
        candles = objectMapper.readValue(result, 
            new TypeReference<List<TiingoCandle>>() {});
      } catch (Exception e) {
        throw new JsonProcessingException("Can't process the Json file") {};
      }
      
      listOfCandles.addAll(candles);
        
    } else {
      throw new StockQuoteServiceException(
          "Can't process the response from a third-party service or" 
          + "response contains an error or is otherwise invalid.");
    }

    return listOfCandles;
  }

  private static String buildUri(String symbol, LocalDate startDate, LocalDate endDate) {
    String uriTemplate = "https://api.tiingo.com/tiingo/daily/$SYMBOL/prices?"
            + "startDate=$STARTDATE&endDate=$ENDDATE&token=$APIKEY";
    String token = "b2e11b78d70e7824e9493801b3c6a07a06ea4f5d";
    String url = uriTemplate.replace("$SYMBOL", symbol)
                            .replace("$STARTDATE", startDate.toString())
                            .replace("$ENDDATE", endDate.toString())
                            .replace("$APIKEY", token);

    // URI uri = URI.create(url);
    // try {
    //   uri = new URI(url);
    // } catch (URISyntaxException e) {
    //   e.printStackTrace();
    // }
   
    return url;
  }

  private static ObjectMapper getObjectMapper() {
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());
    return objectMapper;
  }

  // TODO: CRIO_TASK_MODULE_ADDITIONAL_REFACTOR
  //  Implement getStockQuote method below that was also declared in the interface.

  // Note:
  // 1. You can move the code from PortfolioManagerImpl#getStockQuote inside newly created method.
  // 2. Run the tests using command below and make sure it passes.
  //    ./gradlew test --tests TiingoServiceTest


  //CHECKSTYLE:OFF

  // TODO: CRIO_TASK_MODULE_ADDITIONAL_REFACTOR
  //  Write a method to create appropriate url to call the Tiingo API.





  // TODO: CRIO_TASK_MODULE_EXCEPTIONS
  //  1. Update the method signature to match the signature change in the interface.
  //     Start throwing new StockQuoteServiceException when you get some invalid response from
  //     Tiingo, or if Tiingo returns empty results for whatever reason, or you encounter
  //     a runtime exception during Json parsing.
  //  2. Make sure that the exception propagates all the way from
  //     PortfolioManager#calculateAnnualisedReturns so that the external user's of our API
  //     are able to explicitly handle this exception upfront.

  //CHECKSTYLE:OFF
}
